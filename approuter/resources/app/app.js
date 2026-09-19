/* UDI Assistant — single-page application (no framework, no build step). Served by the approuter on BTP and by
 * Spring locally. Views: devices (data table), audit trail, ask the regulation, knowledge sources, about. */
(() => {
  'use strict';

  // ---------- helpers ----------
  const $ = (sel, root = document) => root.querySelector(sel);
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '');
  const STATUSES = ['DRAFT', 'SUBMITTED', 'REGISTERED', 'WITHDRAWN'];
  const CLASSES = ['I', 'IIA', 'IIB', 'III'];
  const SUGGESTIONS = [
    'What is the difference between UDI-DI and UDI-PI?',
    'When does a change require a new UDI-DI?',
    'Where must the UDI carrier be placed?',
    'What does 21 CFR Part 11 require of an audit trail?',
  ];

  class ApiError extends Error {
    constructor(status, title, detail, errors) {
      super(detail ? `${title}: ${detail}` : title);
      this.status = status; this.title = title; this.detail = detail; this.errors = errors || [];
    }
  }

  // ---------- state ----------
  const state = {
    mode: 'btp', user: null, scopes: [], canEdit: false,
    devices: [], devicesLoaded: false,
    search: '', statusFilter: new Set(), sort: { key: 'name', dir: 'asc' },
    page: 0, pageSize: 10, selected: new Set(),
    columns: { udiDi: true, name: true, manufacturer: true, riskClass: true, registrationStatus: true, version: true, updatedAt: true },
    expanded: null,        // { id, kind: 'reason'|'edit'|'trail'|'review', status?, data? }
    newDeviceOpen: false,
    qa: [], sources: null, audit: null, busy: false,
  };

  const COLUMNS = [
    { key: 'udiDi', label: 'UDI-DI' },
    { key: 'name', label: 'Device' },
    { key: 'manufacturer', label: 'Manufacturer' },
    { key: 'riskClass', label: 'Class' },
    { key: 'registrationStatus', label: 'Status' },
    { key: 'version', label: 'Version', num: true },
    { key: 'updatedAt', label: 'Updated' },
  ];

  // ---------- API ----------
  async function api(path, options = {}) {
    const headers = { Accept: 'application/json' };
    if (options.body) headers['Content-Type'] = 'application/json';
    if (state.mode === 'local' && state.user) headers['X-User'] = state.user;
    const res = await fetch(path, { ...options, headers });
    const isJson = (res.headers.get('content-type') || '').includes('json');
    if (res.redirected || (!isJson && res.status !== 204)) {
      // The approuter answers an expired session with a redirect to the identity provider.
      location.replace('/index.html');
      throw new ApiError(401, 'Session expired');
    }
    const body = res.status === 204 ? null : await res.json();
    if (!res.ok) {
      const title = res.status === 403 ? 'Not allowed' : res.status === 503 ? 'Assistant not configured' : (body?.title || res.statusText);
      const detail = res.status === 403 ? 'Your role does not allow this action (Editor scope required).' : (body?.detail || '');
      throw new ApiError(res.status, title, detail, body?.errors);
    }
    return body;
  }

  // ---------- notices ----------
  let noticeTimer;
  function notify(message, kind = 'ok') {
    const el = $('#notice');
    el.textContent = message; el.className = `notice ${kind}`; el.hidden = false;
    clearTimeout(noticeTimer);
    if (kind === 'ok') noticeTimer = setTimeout(() => { el.hidden = true; }, 4000);
  }
  const clearNotice = () => { $('#notice').hidden = true; };
  const fail = (e) => notify(e.message || String(e), 'error');

  // ---------- boot ----------
  async function init() {
    try {
      const me = await api('/user-api/currentUser');
      state.mode = 'btp';
      state.user = me.email || me.name || me.displayName || 'unknown';
      state.scopes = (me.scopes || []).filter((s) => s.includes('.')).map((s) => s.split('.').pop());
    } catch (e) {
      if (e.status !== 404) return;                       // redirected to sign-in
      state.mode = 'local';
      state.user = localStorage.getItem('udiUser');
      if (!state.user) { location.replace('/index.html'); return; }
      state.scopes = ['Viewer', 'Editor'];
    }
    state.canEdit = state.scopes.includes('Editor');
    renderChrome();
    window.addEventListener('hashchange', route);
    document.addEventListener('click', onDocumentClick);
    $('#view').addEventListener('submit', onSubmit);
    $('#view').addEventListener('input', onInput);
    $('#view').addEventListener('change', onChange);
    route();
  }

  function renderChrome() {
    $('#userEmail').textContent = state.user;
    $('#userEmail').title = state.user;
    $('#userRoles').textContent = state.scopes.join(' · ') || 'no roles';
    $('#avatar').textContent = state.user.slice(0, 2).toUpperCase();
    const pill = $('#modePill');
    pill.textContent = state.mode === 'btp' ? 'SAP BTP · XSUAA' : 'Local · no IdP';
    pill.className = `mode-pill ${state.mode}`;
    $('#logout').addEventListener('click', () => {
      if (state.mode === 'btp') { location.href = '/logout'; return; }
      localStorage.removeItem('udiUser'); location.href = '/index.html';
    });
    $('#menuToggle').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
  }

  // ---------- routing ----------
  const VIEWS = { devices: renderDevicesView, audit: renderAuditView, ask: renderAskView, sources: renderSourcesView, about: renderAboutView };
  const TITLES = { devices: 'Devices', audit: 'Audit trail', ask: 'Ask the regulation', sources: 'Knowledge sources', about: 'About' };
  function currentView() { const v = (location.hash || '#/devices').replace('#/', '').split('/')[0]; return VIEWS[v] ? v : 'devices'; }
  async function route() {
    const view = currentView();
    document.querySelectorAll('#nav a').forEach((a) => a.classList.toggle('active', a.dataset.view === view));
    $('#crumbs').textContent = TITLES[view];
    $('#sidebar').classList.remove('open');
    clearNotice();
    try { await VIEWS[view](); } catch (e) { fail(e); }
  }

  // ======================================================================
  // Devices
  // ======================================================================
  async function loadDevices(force = false) {
    if (state.devicesLoaded && !force) return;
    const page = await api('/api/devices?size=200&sort=name');
    state.devices = page.content;
    state.devicesLoaded = true;
    for (const id of [...state.selected]) if (!state.devices.some((d) => d.id === id)) state.selected.delete(id);
  }

  function filteredDevices() {
    const q = state.search.trim().toLowerCase();
    let rows = state.devices.filter((d) =>
      (!q || [d.udiDi, d.name, d.manufacturer].some((v) => String(v).toLowerCase().includes(q))) &&
      (state.statusFilter.size === 0 || state.statusFilter.has(d.registrationStatus)));
    const { key, dir } = state.sort;
    rows = [...rows].sort((a, b) => {
      const x = a[key], y = b[key];
      const c = typeof x === 'number' ? x - y : String(x ?? '').localeCompare(String(y ?? ''), undefined, { numeric: true });
      return dir === 'asc' ? c : -c;
    });
    return rows;
  }

  async function renderDevicesView() {
    $('#view').innerHTML = '<div class="spinner">Loading devices…</div>';
    await loadDevices();
    renderDevices();
  }

  function renderDevices() {
    const rows = filteredDevices();
    const pages = Math.max(1, Math.ceil(rows.length / state.pageSize));
    state.page = Math.min(state.page, pages - 1);
    const pageRows = rows.slice(state.page * state.pageSize, (state.page + 1) * state.pageSize);
    const counts = Object.fromEntries(STATUSES.map((s) => [s, state.devices.filter((d) => d.registrationStatus === s).length]));
    const visible = COLUMNS.filter((c) => state.columns[c.key]);
    const allOnPage = pageRows.length > 0 && pageRows.every((d) => state.selected.has(d.id));

    $('#view').innerHTML = `
      <div class="page-head">
        <div><h1>Devices</h1><p>${state.devices.length} UDI-DI records · every change is recorded in the audit trail with a reason</p></div>
        ${state.canEdit ? `<button class="btn btn-primary" data-action="toggle-new">${state.newDeviceOpen ? 'Close' : '+ New device'}</button>` : ''}
      </div>
      ${state.newDeviceOpen ? newDevicePanel() : ''}
      <div class="toolbar">
        <div class="search"><input id="search" placeholder="Search UDI-DI, device, manufacturer" value="${esc(state.search)}"></div>
        <div class="dropdown" id="dd-status">
          <button class="btn" data-menu="dd-status">Status ${state.statusFilter.size ? `<span class="chip">${state.statusFilter.size}</span>` : ''} ▾</button>
          <div class="menu">
            <div class="title">Filter by status</div>
            ${STATUSES.map((s) => `<label><input type="checkbox" class="check" data-filter-status="${s}" ${state.statusFilter.has(s) ? 'checked' : ''}> ${s} <span class="count">${counts[s]}</span></label>`).join('')}
          </div>
        </div>
        <div class="dropdown" id="dd-cols">
          <button class="btn" data-menu="dd-cols">Columns ▾</button>
          <div class="menu">
            <div class="title">Toggle columns</div>
            ${COLUMNS.map((c) => `<label><input type="checkbox" class="check" data-col="${c.key}" ${state.columns[c.key] ? 'checked' : ''}> ${c.label}</label>`).join('')}
          </div>
        </div>
        <div class="spacer"></div>
        ${state.selected.size ? `<button class="btn" data-action="export">Export ${state.selected.size} selected (CSV)</button>` : ''}
        <button class="btn btn-ghost" data-action="refresh" title="Reload from the service">↻</button>
      </div>
      <div class="table-wrap">
        <table class="data">
          <thead><tr>
            <th class="col-check"><input type="checkbox" class="check" data-action="select-page" ${allOnPage ? 'checked' : ''}></th>
            ${visible.map((c) => `<th class="sortable ${state.sort.key === c.key ? 'sorted' : ''} ${c.num ? 'num' : ''}" data-sort="${c.key}">${c.label}<span class="dir">${state.sort.key === c.key ? (state.sort.dir === 'asc' ? '▲' : '▼') : '↕'}</span></th>`).join('')}
            <th class="col-actions"></th>
          </tr></thead>
          <tbody>
            ${pageRows.length === 0 ? `<tr><td colspan="${visible.length + 2}" class="empty">No devices match.</td></tr>` : ''}
            ${pageRows.map((d) => deviceRow(d, visible)).join('')}
          </tbody>
        </table>
      </div>
      <div class="table-foot">
        <span>${state.selected.size} of ${rows.length} row(s) selected</span>
        <div class="spacer"></div>
        <label>Rows per page <select id="pageSize">${[5, 10, 25, 50].map((n) => `<option ${n === state.pageSize ? 'selected' : ''}>${n}</option>`).join('')}</select></label>
        <span>${rows.length ? state.page * state.pageSize + 1 : 0}–${Math.min(rows.length, (state.page + 1) * state.pageSize)} of ${rows.length}</span>
        <div class="pager">
          <button class="btn" data-action="page" data-page="${state.page - 1}" ${state.page === 0 ? 'disabled' : ''}>‹</button>
          ${pageButtons(pages)}
          <button class="btn" data-action="page" data-page="${state.page + 1}" ${state.page >= pages - 1 ? 'disabled' : ''}>›</button>
        </div>
      </div>`;
    const s = $('#search'); if (document.activeElement !== s && state.searchFocus) { s.focus(); s.setSelectionRange(s.value.length, s.value.length); }
  }

  function pageButtons(pages) {
    const out = [];
    for (let p = 0; p < pages; p++) {
      if (pages > 7 && Math.abs(p - state.page) > 2 && p !== 0 && p !== pages - 1) { if (out[out.length - 1] !== '…') out.push('…'); continue; }
      out.push(`<button class="btn ${p === state.page ? 'current' : ''}" data-action="page" data-page="${p}">${p + 1}</button>`);
    }
    return out.map((x) => (x === '…' ? '<span class="btn btn-ghost" style="pointer-events:none">…</span>' : x)).join('');
  }

  function deviceRow(d, visible) {
    const cell = (c) => {
      switch (c.key) {
        case 'udiDi': return `<td><span class="mono">${esc(d.udiDi)}</span></td>`;
        case 'name': return `<td><strong style="font-weight:500">${esc(d.name)}</strong></td>`;
        case 'riskClass': return `<td><span class="chip">${esc(d.riskClass)}</span></td>`;
        case 'registrationStatus': return `<td><span class="badge ${esc(d.registrationStatus)}">${esc(d.registrationStatus)}</span></td>`;
        case 'version': return `<td class="num">${d.version}</td>`;
        case 'updatedAt': return `<td>${fmtDate(d.updatedAt)}<span class="cell-sub">by ${esc(d.updatedBy)}</span></td>`;
        default: return `<td>${esc(d[c.key])}</td>`;
      }
    };
    const selected = state.selected.has(d.id);
    const exp = state.expanded && state.expanded.id === d.id ? `<tr class="expansion"><td colspan="${visible.length + 2}">${expansion(d)}</td></tr>` : '';
    return `<tr class="${selected ? 'selected' : ''}" data-row="${d.id}">
      <td><input type="checkbox" class="check" data-select="${d.id}" ${selected ? 'checked' : ''}></td>
      ${visible.map(cell).join('')}
      <td class="col-actions">
        <div class="dropdown right" id="dd-row-${d.id}">
          <button class="btn btn-ghost btn-icon" data-menu="dd-row-${d.id}" aria-label="Actions">⋯</button>
          <div class="menu">
            <button class="item" data-action="expand" data-id="${d.id}" data-kind="trail">Audit trail</button>
            <button class="item" data-action="expand" data-id="${d.id}" data-kind="review">Review against regulation</button>
            <button class="item" data-action="copy" data-value="${esc(d.udiDi)}">Copy UDI-DI</button>
            ${state.canEdit ? `<hr>
              <div class="title">Change status</div>
              ${d.allowedTransitions.length ? d.allowedTransitions.map((t) => `<button class="item" data-action="expand" data-id="${d.id}" data-kind="reason" data-status="${t}">→ ${t}</button>`).join('') : '<button class="item disabled">No transition allowed</button>'}
              <hr>
              <button class="item" data-action="expand" data-id="${d.id}" data-kind="edit">Edit details</button>` : ''}
          </div>
        </div>
      </td>
    </tr>${exp}`;
  }

  // ---------- expansion panels (inline, never a browser popup) ----------
  function expansion(d) {
    const x = state.expanded;
    if (x.kind === 'reason') return `
      <form data-form="status" data-id="${d.id}" data-status="${x.status}">
        <h3 style="margin:0 0 2px;font-size:14px">Change status of <em>${esc(d.name)}</em>: ${esc(d.registrationStatus)} → ${esc(x.status)}</h3>
        <p class="sub" style="color:var(--muted);font-size:13px;margin:0 0 10px">A reason is required and becomes part of the audit trail (21 CFR Part 11 / Annex 11).</p>
        <div class="form-grid"><div class="field wide"><label for="reason-${d.id}">Reason for change</label><input id="reason-${d.id}" name="reason" maxlength="500" placeholder="e.g. Technical documentation complete; submitted for review"><div class="err" data-err="reason"></div></div></div>
        <div class="form-actions"><button class="btn btn-primary" type="submit">Confirm → ${esc(x.status)}</button><button class="btn" type="button" data-action="collapse">Cancel</button><span class="hint">Recorded as ${esc(state.user)}</span></div>
      </form>`;
    if (x.kind === 'edit') return `
      <form data-form="edit" data-id="${d.id}">
        <h3 style="margin:0 0 2px;font-size:14px">Edit <em>${esc(d.name)}</em></h3>
        <p class="sub" style="color:var(--muted);font-size:13px;margin:0 0 10px">UDI-DI is immutable. Version ${d.version} is sent with the change; a concurrent edit is rejected (optimistic locking).</p>
        <div class="form-grid">
          <div class="field"><label>Name</label><input name="name" maxlength="200" value="${esc(d.name)}"><div class="err" data-err="name"></div></div>
          <div class="field"><label>Manufacturer</label><input name="manufacturer" maxlength="200" value="${esc(d.manufacturer)}"><div class="err" data-err="manufacturer"></div></div>
          <div class="field"><label>Risk class</label><select name="riskClass">${CLASSES.map((c) => `<option ${c === d.riskClass ? 'selected' : ''}>${c}</option>`).join('')}</select><div class="err" data-err="riskClass"></div></div>
          <div class="field wide"><label>Reason for change</label><input name="reason" maxlength="500" placeholder="Why is this record changing?"><div class="err" data-err="reason"></div></div>
        </div>
        <div class="form-actions"><button class="btn btn-primary" type="submit">Save changes</button><button class="btn" type="button" data-action="collapse">Cancel</button></div>
      </form>`;
    if (x.kind === 'trail') return x.data ? trailTable(x.data) : '<span class="spinner">Loading audit trail…</span>';
    if (x.kind === 'review') return x.data ? reviewPanel(x.data) : '<span class="spinner">Reviewing against the regulation corpus…</span>';
    return '';
  }

  function trailTable(entries) {
    if (!entries.length) return '<div class="empty">No entries.</div>';
    return `<table class="trail"><thead><tr><th>When</th><th>Action</th><th>Field</th><th>Change</th><th>Reason</th><th>By</th></tr></thead><tbody>
      ${entries.map((e) => `<tr><td class="mono" style="white-space:nowrap">${fmtDate(e.performedAt)}</td><td>${esc(e.action)}</td><td>${esc(e.field ?? '')}</td>
        <td class="diff">${e.field ? `<span class="old">${esc(e.oldValue ?? '—')}</span><span class="arrow">→</span>${esc(e.newValue ?? '')}` : ''}</td>
        <td>${esc(e.reason ?? '')}</td><td>${esc(e.performedBy)}</td></tr>`).join('')}
    </tbody></table>`;
  }

  function reviewPanel(r) {
    const findings = r.findings.length
      ? r.findings.map((f) => `<div class="finding"><span class="sev ${esc(f.severity)}">${esc(f.severity)}</span> · <strong style="font-weight:500">${esc(f.rule)}</strong><div>${esc(f.message)}</div><div class="cite"><b>[${esc(f.source)} § ${esc(f.section)}]</b></div></div>`).join('')
      : '<div class="finding">No findings supported by the regulation excerpts.</div>';
    return `${findings}<div class="cite">model ${esc(r.model)} · passages consulted: ${r.citations.map((c) => esc(`${c.source} § ${c.section}`)).join('; ') || 'none'}</div>`;
  }

  function newDevicePanel() {
    return `<div class="panel"><form data-form="create">
      <h3>New device</h3><p class="sub">UDI-DI must be a GTIN-14 with a valid GS1 check digit; the record starts in DRAFT.</p>
      <div class="form-grid">
        <div class="field"><label>UDI-DI (GTIN-14)</label><input name="udiDi" class="mono" maxlength="14" placeholder="04012345678901"><div class="err" data-err="udiDi"></div></div>
        <div class="field"><label>Name</label><input name="name" maxlength="200"><div class="err" data-err="name"></div></div>
        <div class="field"><label>Manufacturer</label><input name="manufacturer" maxlength="200"><div class="err" data-err="manufacturer"></div></div>
        <div class="field"><label>Risk class</label><select name="riskClass">${CLASSES.map((c) => `<option>${c}</option>`).join('')}</select><div class="err" data-err="riskClass"></div></div>
      </div>
      <div class="form-actions"><button class="btn btn-primary" type="submit">Create device</button><button class="btn" type="button" data-action="toggle-new">Cancel</button></div>
    </form></div>`;
  }

  // ---------- device interactions ----------
  async function openExpansion(id, kind, status) {
    state.expanded = { id, kind, status, data: null };
    renderDevices();
    try {
      if (kind === 'trail') state.expanded.data = await api(`/api/devices/${id}/audit-trail`);
      if (kind === 'review') state.expanded.data = await api(`/api/devices/${id}/review`, { method: 'POST' });
    } catch (e) { state.expanded = null; fail(e); }
    if (state.expanded && state.expanded.id === id) renderDevices();
    const row = document.querySelector(`tr[data-row="${id}"]`); if (row) row.scrollIntoView({ block: 'nearest' });
  }

  function showFieldErrors(form, e) {
    form.querySelectorAll('.err').forEach((el) => { el.textContent = ''; });
    (e.errors || []).forEach((err) => { const el = form.querySelector(`[data-err="${err.field}"]`); if (el) el.textContent = err.message; });
    if (!e.errors || !e.errors.length) fail(e);
  }

  async function onSubmit(ev) {
    const form = ev.target.closest('form[data-form]'); if (!form) return;
    ev.preventDefault();
    const data = Object.fromEntries(new FormData(form).entries());
    const id = form.dataset.id;
    try {
      if (form.dataset.form === 'status') {
        await api(`/api/devices/${id}/status`, { method: 'POST', body: JSON.stringify({ status: form.dataset.status, reason: data.reason }) });
        notify(`Status changed to ${form.dataset.status}.`);
        await loadDevices(true); await openExpansion(id, 'trail');
      } else if (form.dataset.form === 'edit') {
        const d = state.devices.find((x) => x.id === id);
        await api(`/api/devices/${id}`, { method: 'PUT', body: JSON.stringify({ ...data, version: d.version }) });
        notify('Device updated.');
        await loadDevices(true); await openExpansion(id, 'trail');
      } else if (form.dataset.form === 'create') {
        const created = await api('/api/devices', { method: 'POST', body: JSON.stringify(data) });
        notify(`Device ${created.udiDi} created in DRAFT.`);
        state.newDeviceOpen = false; state.search = ''; state.statusFilter.clear();
        await loadDevices(true); renderDevices();
      } else if (form.dataset.form === 'ask') {
        await ask(data.question);
      }
    } catch (e) { showFieldErrors(form, e); }
  }

  function onInput(ev) {
    if (ev.target.id === 'search') { state.search = ev.target.value; state.page = 0; state.searchFocus = true; renderDevices(); }
  }

  function onChange(ev) {
    const t = ev.target;
    if (t.dataset.filterStatus) { t.checked ? state.statusFilter.add(t.dataset.filterStatus) : state.statusFilter.delete(t.dataset.filterStatus); state.page = 0; renderDevices(); keepOpen('dd-status'); }
    else if (t.dataset.col) { state.columns[t.dataset.col] = t.checked; renderDevices(); keepOpen('dd-cols'); }
    else if (t.dataset.select) { t.checked ? state.selected.add(t.dataset.select) : state.selected.delete(t.dataset.select); renderDevices(); }
    else if (t.dataset.action === 'select-page') { filteredDevices().slice(state.page * state.pageSize, (state.page + 1) * state.pageSize).forEach((d) => (t.checked ? state.selected.add(d.id) : state.selected.delete(d.id))); renderDevices(); }
    else if (t.id === 'pageSize') { state.pageSize = Number(t.value); state.page = 0; renderDevices(); }
  }
  const keepOpen = (id) => { const dd = document.getElementById(id); if (dd) dd.classList.add('open'); };

  function onDocumentClick(ev) {
    if (!ev.target.closest('#search')) state.searchFocus = false;   // only re-focus the search box while typing in it
    const menuBtn = ev.target.closest('[data-menu]');
    document.querySelectorAll('.dropdown.open').forEach((dd) => { if (!dd.contains(ev.target) || menuBtn) dd.classList.remove('open'); });
    if (menuBtn) { document.getElementById(menuBtn.dataset.menu).classList.toggle('open'); return; }
    const th = ev.target.closest('th[data-sort]');
    if (th) { const key = th.dataset.sort; state.sort = { key, dir: state.sort.key === key && state.sort.dir === 'asc' ? 'desc' : 'asc' }; renderDevices(); return; }
    const btn = ev.target.closest('[data-action]'); if (!btn || btn.type === 'checkbox') return;
    const a = btn.dataset.action;
    if (a === 'expand') openExpansion(btn.dataset.id, btn.dataset.kind, btn.dataset.status);
    else if (a === 'collapse') { state.expanded = null; renderDevices(); }
    else if (a === 'toggle-new') { state.newDeviceOpen = !state.newDeviceOpen; renderDevices(); }
    else if (a === 'page') { state.page = Number(btn.dataset.page); renderDevices(); }
    else if (a === 'refresh') loadDevices(true).then(renderDevices).catch(fail);
    else if (a === 'copy') navigator.clipboard?.writeText(btn.dataset.value).then(() => notify(`Copied ${btn.dataset.value}`));
    else if (a === 'export') exportCsv();
    else if (a === 'suggest') { $('#question').value = btn.dataset.q; ask(btn.dataset.q).catch(fail); }
  }

  function exportCsv() {
    const rows = state.devices.filter((d) => state.selected.has(d.id));
    const cols = ['udiDi', 'name', 'manufacturer', 'riskClass', 'registrationStatus', 'version', 'updatedAt', 'updatedBy'];
    const csv = [cols.join(','), ...rows.map((d) => cols.map((c) => `"${String(d[c] ?? '').replace(/"/g, '""')}"`).join(','))].join('\n');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' })); a.download = 'devices.csv'; a.click();
    URL.revokeObjectURL(a.href);
  }

  // ======================================================================
  // Audit trail (all devices)
  // ======================================================================
  async function renderAuditView() {
    $('#view').innerHTML = '<div class="spinner">Loading audit trail…</div>';
    const [entries] = await Promise.all([api('/api/audit-trail'), loadDevices()]);
    const names = Object.fromEntries(state.devices.map((d) => [d.id, d]));
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>Audit trail</h1><p>Newest first · append-only · who, what, when and why for every change</p></div></div>
      <div class="table-wrap"><table class="data">
        <thead><tr><th>When</th><th>Device</th><th>Action</th><th>Field</th><th>Change</th><th>Reason</th><th>By</th></tr></thead>
        <tbody>${entries.map((e) => { const d = names[e.entityId]; return `<tr>
          <td class="mono" style="white-space:nowrap">${fmtDate(e.performedAt)}</td>
          <td>${d ? `${esc(d.name)}<span class="cell-sub mono">${esc(d.udiDi)}</span>` : `<span class="mono">${esc(e.entityId)}</span>`}</td>
          <td><span class="chip">${esc(e.action)}</span></td><td>${esc(e.field ?? '')}</td>
          <td class="diff">${e.field ? `<span class="old">${esc(e.oldValue ?? '—')}</span><span class="arrow">→</span>${esc(e.newValue ?? '')}` : ''}</td>
          <td>${esc(e.reason ?? '')}</td><td>${esc(e.performedBy)}</td></tr>`; }).join('')}
        </tbody></table></div>
      <div class="table-foot"><span>${entries.length} entries (last 200)</span></div>`;
  }

  // ======================================================================
  // Ask the regulation
  // ======================================================================
  async function renderAskView() {
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>Ask the regulation</h1><p>Answers come only from the versioned corpus and cite the passage they rest on. Out-of-scope questions are declined.</p></div></div>
      <form class="ask-form" data-form="ask"><input id="question" name="question" maxlength="1000" placeholder="Ask about UDI-DI, EUDAMED, labelling, risk classes, audit trails…" autocomplete="off"><button class="btn btn-primary" type="submit">Ask</button></form>
      <div class="suggest">${SUGGESTIONS.map((q) => `<button class="btn btn-sm" data-action="suggest" data-q="${esc(q)}">${esc(q)}</button>`).join('')}</div>
      <div class="qa" id="qa"></div>`;
    renderQa();
  }

  function renderQa() {
    const el = $('#qa'); if (!el) return;
    el.innerHTML = state.qa.map((x) => `<div class="panel">
      <div class="q">${esc(x.question)}</div>
      ${x.pending ? '<div class="spinner">Retrieving passages and asking the model…</div>' : x.error ? `<div class="err" style="color:var(--danger)">${esc(x.error)}</div>` : `<div class="a">${esc(x.answer.answer)}</div>
        ${x.answer.citations.map((c) => `<div class="cite"><b>[${esc(c.source)} § ${esc(c.section)}]</b> ${esc(c.excerpt).slice(0, 240)}…</div>`).join('')}
        <div class="cite">model ${esc(x.answer.model)} · ${x.answer.citations.length} passage(s) retrieved</div>`}
    </div>`).join('');
  }

  async function ask(question) {
    question = (question || '').trim();
    if (!question) { $('#question').focus(); return; }
    const entry = { question, pending: true };
    state.qa.unshift(entry); renderQa(); $('#question').value = '';
    try { entry.answer = await api('/api/assistant/ask', { method: 'POST', body: JSON.stringify({ question }) }); }
    catch (e) { entry.error = e.message; }
    entry.pending = false; renderQa();
  }

  // ======================================================================
  // Knowledge sources
  // ======================================================================
  async function renderSourcesView() {
    $('#view').innerHTML = '<div class="spinner">Loading…</div>';
    state.sources = state.sources || await api('/api/assistant/sources');
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>Knowledge sources</h1><p>What the assistant can cite: ${state.sources.length} sources, ${state.sources.reduce((n, s) => n + s.sections.length, 0)} sections, versioned with the application.</p></div></div>
      <div class="panel sources">${state.sources.map((s) => `<div class="src"><h3><span class="mono">${esc(s.source)}</span></h3><ul>${s.sections.map((x) => `<li>${esc(x)}</li>`).join('')}</ul></div>`).join('')}</div>`;
  }

  // ======================================================================
  // About
  // ======================================================================
  async function renderAboutView() {
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>About</h1><p>udi-assistant — a GxP-flavoured master-data service for medical devices on SAP BTP</p></div></div>
      <div class="panel about">
        <h2>What it demonstrates</h2>
        <table class="kv">
          <tr><td>Domain</td><td>Devices identified by UDI-DI (GTIN-14), EU MDR risk classes, a registration lifecycle that never deletes records</td></tr>
          <tr><td>Audit trail</td><td>Append-only, written in the same transaction as the change, with the acting user and a mandatory reason (21 CFR Part 11 / Annex 11)</td></tr>
          <tr><td>Security</td><td>XSUAA login through the application router; Viewer / Editor scopes enforced at the router and again in the service; the token user lands in the trail</td></tr>
          <tr><td>Assistant</td><td>Retrieval-augmented answers over a versioned regulation corpus; citations come from retrieval metadata, out-of-scope questions are declined; structured device review</td></tr>
          <tr><td>Platform</td><td>SAP BTP Cloud Foundry, MTA (service + router + XSUAA), secrets via user-provided services, Flyway-owned schema</td></tr>
        </table>
        <h2>Stack</h2>
        <table class="kv">
          <tr><td>Service</td><td>Java 17 · Spring Boot 4 · Spring Data JPA · Flyway · Spring Security (JWT resource server) · Spring AI 2 · H2 (demo)</td></tr>
          <tr><td>Router / UI</td><td>@sap/approuter · this page (plain HTML/JS, no build step)</td></tr>
          <tr><td>Method</td><td>SPEC.md as source of truth · DECISIONS.md for trade-offs and lessons · 34 tests, none calling the model provider</td></tr>
        </table>
        <h2>Session</h2>
        <table class="kv"><tr><td>Mode</td><td>${state.mode === 'btp' ? 'SAP BTP — authenticated by XSUAA' : 'Local — no identity provider; user from the X-User header'}</td></tr><tr><td>User</td><td>${esc(state.user)}</td></tr><tr><td>Scopes</td><td>${esc(state.scopes.join(', ')) || '—'}</td></tr></table>
        <p style="margin-top:14px"><a href="https://github.com/tarekakel/udi-assistant" target="_blank" rel="noopener">Source on GitHub</a></p>
      </div>`;
  }

  init().catch(fail);
})();
