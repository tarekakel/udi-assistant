/* UDI Assistant — single-page application (no framework, no build step). Served by the approuter on BTP and by
 * Spring locally. Views: devices (data table), audit trail, ask the regulation, knowledge sources, about.
 * Supports EN/DE (udiLang), light/dark (udiTheme) and a collapsible navigation rail (udiNav). */
(() => {
  'use strict';

  // ======================================================================
  // i18n
  // ======================================================================
  const I18N = {
    en: {
      'brand.sub': 'Life-sciences master data',
      'nav.devices': 'Devices', 'nav.audit': 'Audit trail', 'nav.ask': 'Ask the regulation', 'nav.sources': 'Knowledge sources', 'nav.about': 'About', 'nav.ui5': 'SAPUI5 client',
      logout: 'Log out', 'mode.btp': 'SAP BTP · XSUAA', 'mode.local': 'Local · no IdP', 'theme.toggle': 'Switch light / dark', 'nav.toggle': 'Collapse or expand navigation', 'roles.none': 'no roles',
      'devices.sub': '{n} UDI-DI records · every change is recorded in the audit trail with a reason',
      'devices.new': '+ New device', close: 'Close', 'search.ph': 'Search UDI-DI, device, manufacturer',
      status: 'Status', 'filter.title': 'Filter by status', columns: 'Columns', 'columns.title': 'Toggle columns',
      export: 'Export {n} selected (CSV)', refresh: 'Reload from the service',
      'col.udiDi': 'UDI-DI', 'col.name': 'Device', 'col.manufacturer': 'Manufacturer', 'col.riskClass': 'Class', 'col.registrationStatus': 'Status', 'col.version': 'Version', 'col.updatedAt': 'Updated',
      by: 'by {u}', 'devices.empty': 'No devices match.', selected: '{n} of {m} row(s) selected', rowsPerPage: 'Rows per page', range: '{a}–{b} of {n}',
      'act.trail': 'Audit trail', 'act.review': 'Review against regulation', 'act.copy': 'Copy UDI-DI', 'act.changeStatus': 'Change status', 'act.noTransition': 'No transition allowed', 'act.edit': 'Edit details',
      'st.DRAFT': 'Draft', 'st.SUBMITTED': 'Submitted', 'st.REGISTERED': 'Registered', 'st.WITHDRAWN': 'Withdrawn',
      'reason.title': 'Change status of {name}: {from} → {to}', 'reason.sub': 'A reason is required and becomes part of the audit trail (21 CFR Part 11 / Annex 11).',
      'reason.label': 'Reason for change', 'reason.ph': 'e.g. Technical documentation complete; submitted for review', 'reason.confirm': 'Confirm → {to}', cancel: 'Cancel', recordedAs: 'Recorded as {u}',
      'edit.title': 'Edit {name}', 'edit.sub': 'UDI-DI is immutable. Version {v} is sent with the change; a concurrent edit is rejected (optimistic locking).',
      name: 'Name', manufacturer: 'Manufacturer', riskClass: 'Risk class', 'edit.save': 'Save changes', 'edit.reasonPh': 'Why is this record changing?',
      'new.title': 'New device', 'new.sub': 'UDI-DI must be a GTIN-14 with a valid GS1 check digit; the record starts in Draft.', 'new.udi': 'UDI-DI (GTIN-14)', 'new.create': 'Create device',
      'trail.loading': 'Loading audit trail…', 'trail.none': 'No entries.', 'review.loading': 'Reviewing against the regulation corpus…',
      when: 'When', action: 'Action', field: 'Field', change: 'Change', reason: 'Reason', byCol: 'By', device: 'Device',
      'review.none': 'No findings supported by the regulation excerpts.', 'review.consulted': 'model {m} · passages consulted: {p}', none: 'none',
      'n.statusChanged': 'Status changed to {s}.', 'n.updated': 'Device updated.', 'n.created': 'Device {u} created in Draft.', 'n.copied': 'Copied {v}',
      'e.forbidden': 'Not allowed', 'e.forbiddenDetail': 'Your role does not allow this action (Editor scope required).', 'e.notConfigured': 'Assistant not configured', 'e.notConfiguredDetail': 'No OpenAI key is bound to this deployment.', 'e.session': 'Session expired',
      'audit.sub': 'Newest first · append-only · who, what, when and why for every change', 'audit.count': '{n} entries (last 200)', 'audit.loading': 'Loading audit trail…', 'devices.loading': 'Loading devices…', loading: 'Loading…',
      'ask.sub': 'Answers come only from the versioned corpus and cite the passage they rest on. Out-of-scope questions are declined.',
      'ask.ph': 'Ask about UDI-DI, EUDAMED, labelling, risk classes, audit trails…', ask: 'Ask', 'ask.thinking': 'Retrieving passages and asking the model…', 'ask.retrieved': 'model {m} · {n} passage(s) retrieved',
      'ask.suggest': ['What is the difference between UDI-DI and UDI-PI?', 'When does a change require a new UDI-DI?', 'Where must the UDI carrier be placed?', 'What does 21 CFR Part 11 require of an audit trail?'],
      'sources.sub': 'What the assistant can cite: {s} sources, {n} sections, versioned with the application.',
      'about.sub': 'udi-assistant — a GxP-flavoured master-data service for medical devices on SAP BTP',
      'about.what': 'What it demonstrates', 'about.domain': 'Domain', 'about.domainText': 'Devices identified by UDI-DI (GTIN-14), EU MDR risk classes, a registration lifecycle that never deletes records',
      'about.trail': 'Audit trail', 'about.trailText': 'Append-only, written in the same transaction as the change, with the acting user and a mandatory reason (21 CFR Part 11 / Annex 11)',
      'about.security': 'Security', 'about.securityText': 'XSUAA login through the application router; Viewer / Editor scopes enforced at the router and again in the service; the token user lands in the trail',
      'about.assistant': 'Assistant', 'about.assistantText': 'Retrieval-augmented answers over a versioned regulation corpus; citations come from retrieval metadata, out-of-scope questions are declined; structured device review',
      'about.platform': 'Platform', 'about.platformText': 'SAP BTP Cloud Foundry, MTA (service + router + XSUAA), secrets via user-provided services, Flyway-owned schema',
      'about.stack': 'Stack', 'about.service': 'Service', 'about.router': 'Router / UI', 'about.routerText': '@sap/approuter · this page (plain HTML/JS, no build step, EN/DE, light/dark)',
      'about.method': 'Method', 'about.methodText': 'SPEC.md as source of truth · DECISIONS.md for trade-offs and lessons · 34 tests, none calling the model provider',
      'about.session': 'Session', mode: 'Mode', 'about.modeBtp': 'SAP BTP — authenticated by XSUAA', 'about.modeLocal': 'Local — no identity provider; user from the X-User header', user: 'User', scopes: 'Scopes', 'about.source': 'Source on GitHub',
    },
    de: {
      'brand.sub': 'Life-Sciences-Stammdaten',
      'nav.devices': 'Geräte', 'nav.audit': 'Audit-Trail', 'nav.ask': 'Regulierung fragen', 'nav.sources': 'Wissensquellen', 'nav.about': 'Über', 'nav.ui5': 'SAPUI5-Client',
      logout: 'Abmelden', 'mode.btp': 'SAP BTP · XSUAA', 'mode.local': 'Lokal · kein IdP', 'theme.toggle': 'Hell / dunkel umschalten', 'nav.toggle': 'Navigation ein- oder ausklappen', 'roles.none': 'keine Rollen',
      'devices.sub': '{n} UDI-DI-Datensätze · jede Änderung wird mit Begründung im Audit-Trail erfasst',
      'devices.new': '+ Neues Gerät', close: 'Schließen', 'search.ph': 'UDI-DI, Gerät, Hersteller suchen',
      status: 'Status', 'filter.title': 'Nach Status filtern', columns: 'Spalten', 'columns.title': 'Spalten ein-/ausblenden',
      export: '{n} ausgewählte exportieren (CSV)', refresh: 'Vom Service neu laden',
      'col.udiDi': 'UDI-DI', 'col.name': 'Gerät', 'col.manufacturer': 'Hersteller', 'col.riskClass': 'Klasse', 'col.registrationStatus': 'Status', 'col.version': 'Version', 'col.updatedAt': 'Geändert',
      by: 'von {u}', 'devices.empty': 'Keine Geräte gefunden.', selected: '{n} von {m} Zeile(n) ausgewählt', rowsPerPage: 'Zeilen pro Seite', range: '{a}–{b} von {n}',
      'act.trail': 'Audit-Trail', 'act.review': 'Gegen Regulierung prüfen', 'act.copy': 'UDI-DI kopieren', 'act.changeStatus': 'Status ändern', 'act.noTransition': 'Kein Übergang möglich', 'act.edit': 'Details bearbeiten',
      'st.DRAFT': 'Entwurf', 'st.SUBMITTED': 'Eingereicht', 'st.REGISTERED': 'Registriert', 'st.WITHDRAWN': 'Zurückgezogen',
      'reason.title': 'Status von {name} ändern: {from} → {to}', 'reason.sub': 'Eine Begründung ist erforderlich und wird Teil des Audit-Trails (21 CFR Part 11 / Annex 11).',
      'reason.label': 'Begründung der Änderung', 'reason.ph': 'z. B. Technische Dokumentation vollständig; zur Prüfung eingereicht', 'reason.confirm': 'Bestätigen → {to}', cancel: 'Abbrechen', recordedAs: 'Erfasst als {u}',
      'edit.title': '{name} bearbeiten', 'edit.sub': 'Die UDI-DI ist unveränderlich. Version {v} wird mitgesendet; eine gleichzeitige Änderung wird abgelehnt (Optimistic Locking).',
      name: 'Name', manufacturer: 'Hersteller', riskClass: 'Risikoklasse', 'edit.save': 'Änderungen speichern', 'edit.reasonPh': 'Warum ändert sich dieser Datensatz?',
      'new.title': 'Neues Gerät', 'new.sub': 'Die UDI-DI muss eine GTIN-14 mit gültiger GS1-Prüfziffer sein; der Datensatz beginnt als Entwurf.', 'new.udi': 'UDI-DI (GTIN-14)', 'new.create': 'Gerät anlegen',
      'trail.loading': 'Audit-Trail wird geladen…', 'trail.none': 'Keine Einträge.', 'review.loading': 'Prüfung gegen den Regulierungskorpus…',
      when: 'Wann', action: 'Aktion', field: 'Feld', change: 'Änderung', reason: 'Begründung', byCol: 'Von', device: 'Gerät',
      'review.none': 'Keine durch die Regulierungsauszüge gestützten Feststellungen.', 'review.consulted': 'Modell {m} · herangezogene Passagen: {p}', none: 'keine',
      'n.statusChanged': 'Status geändert zu {s}.', 'n.updated': 'Gerät aktualisiert.', 'n.created': 'Gerät {u} als Entwurf angelegt.', 'n.copied': '{v} kopiert',
      'e.forbidden': 'Nicht erlaubt', 'e.forbiddenDetail': 'Ihre Rolle erlaubt diese Aktion nicht (Editor-Berechtigung erforderlich).', 'e.notConfigured': 'Assistent nicht konfiguriert', 'e.notConfiguredDetail': 'An dieses Deployment ist kein OpenAI-Schlüssel gebunden.', 'e.session': 'Sitzung abgelaufen',
      'audit.sub': 'Neueste zuerst · nur anfügend · wer, was, wann und warum für jede Änderung', 'audit.count': '{n} Einträge (letzte 200)', 'audit.loading': 'Audit-Trail wird geladen…', 'devices.loading': 'Geräte werden geladen…', loading: 'Wird geladen…',
      'ask.sub': 'Antworten stammen ausschließlich aus dem versionierten Korpus und zitieren die zugrunde liegende Passage. Fragen außerhalb des Umfangs werden abgelehnt.',
      'ask.ph': 'Fragen zu UDI-DI, EUDAMED, Kennzeichnung, Risikoklassen, Audit-Trails…', ask: 'Fragen', 'ask.thinking': 'Passagen werden abgerufen und das Modell befragt…', 'ask.retrieved': 'Modell {m} · {n} Passage(n) abgerufen',
      'ask.suggest': ['Was ist der Unterschied zwischen UDI-DI und UDI-PI?', 'Wann erfordert eine Änderung eine neue UDI-DI?', 'Wo muss der UDI-Träger angebracht werden?', 'Was verlangt 21 CFR Part 11 von einem Audit-Trail?'],
      'sources.sub': 'Was der Assistent zitieren kann: {s} Quellen, {n} Abschnitte, mit der Anwendung versioniert.',
      'about.sub': 'udi-assistant — ein GxP-orientierter Stammdatenservice für Medizinprodukte auf SAP BTP',
      'about.what': 'Was es zeigt', 'about.domain': 'Domäne', 'about.domainText': 'Geräte, identifiziert über UDI-DI (GTIN-14), EU-MDR-Risikoklassen, ein Registrierungslebenszyklus, der niemals Datensätze löscht',
      'about.trail': 'Audit-Trail', 'about.trailText': 'Nur anfügend, in derselben Transaktion wie die Änderung geschrieben, mit handelndem Benutzer und verpflichtender Begründung (21 CFR Part 11 / Annex 11)',
      'about.security': 'Sicherheit', 'about.securityText': 'XSUAA-Anmeldung über den Application Router; Viewer-/Editor-Scopes am Router und erneut im Service durchgesetzt; der Token-Benutzer landet im Trail',
      'about.assistant': 'Assistent', 'about.assistantText': 'Retrieval-gestützte Antworten über einen versionierten Regulierungskorpus; Zitate stammen aus den Retrieval-Metadaten, Fragen außerhalb des Umfangs werden abgelehnt; strukturierte Geräteprüfung',
      'about.platform': 'Plattform', 'about.platformText': 'SAP BTP Cloud Foundry, MTA (Service + Router + XSUAA), Geheimnisse über User-Provided Services, Schema durch Flyway verwaltet',
      'about.stack': 'Technologie', 'about.service': 'Service', 'about.router': 'Router / UI', 'about.routerText': '@sap/approuter · diese Seite (reines HTML/JS, kein Build-Schritt, EN/DE, hell/dunkel)',
      'about.method': 'Vorgehen', 'about.methodText': 'SPEC.md als Single Source of Truth · DECISIONS.md für Abwägungen und Erkenntnisse · 34 Tests, keiner ruft den Modellanbieter auf',
      'about.session': 'Sitzung', mode: 'Modus', 'about.modeBtp': 'SAP BTP — authentifiziert durch XSUAA', 'about.modeLocal': 'Lokal — kein Identity Provider; Benutzer aus dem X-User-Header', user: 'Benutzer', scopes: 'Berechtigungen', 'about.source': 'Quellcode auf GitHub',
    },
  };
  const LOCALE = { en: 'en-GB', de: 'de-DE' };
  let lang = localStorage.getItem('udiLang') || (navigator.language.startsWith('de') ? 'de' : 'en');
  const t = (key, vars = {}) => {
    let s = I18N[lang][key] ?? I18N.en[key] ?? key;
    if (Array.isArray(s)) return s;
    for (const [k, v] of Object.entries(vars)) s = s.replaceAll(`{${k}}`, v);
    return s;
  };
  const st = (status) => t(`st.${status}`);

  // ======================================================================
  // helpers & state
  // ======================================================================
  const $ = (sel, root = document) => root.querySelector(sel);
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString(LOCALE[lang], { dateStyle: 'medium', timeStyle: 'short' }) : '');
  const STATUSES = ['DRAFT', 'SUBMITTED', 'REGISTERED', 'WITHDRAWN'];
  const CLASSES = ['I', 'IIA', 'IIB', 'III'];
  const isMobile = () => matchMedia('(max-width: 860px)').matches;

  class ApiError extends Error {
    constructor(status, title, detail, errors) {
      super(detail ? `${title}: ${detail}` : title);
      this.status = status; this.title = title; this.detail = detail; this.errors = errors || [];
    }
  }

  const state = {
    mode: 'btp', user: null, scopes: [], canEdit: false,
    devices: [], devicesLoaded: false,
    search: '', searchFocus: false, statusFilter: new Set(), sort: { key: 'name', dir: 'asc' },
    page: 0, pageSize: 10, selected: new Set(),
    columns: { udiDi: true, name: true, manufacturer: true, riskClass: true, registrationStatus: true, version: true, updatedAt: true },
    expanded: null, newDeviceOpen: false,
    qa: [], sources: null,
  };
  const COLUMNS = [
    { key: 'udiDi' }, { key: 'name' }, { key: 'manufacturer' }, { key: 'riskClass' }, { key: 'registrationStatus' }, { key: 'version', num: true }, { key: 'updatedAt' },
  ];

  // ======================================================================
  // API
  // ======================================================================
  async function api(path, options = {}) {
    const headers = { Accept: 'application/json' };
    if (options.body) headers['Content-Type'] = 'application/json';
    if (state.mode === 'local' && state.user) headers['X-User'] = state.user;
    const res = await fetch(path, { ...options, headers });
    const isJson = (res.headers.get('content-type') || '').includes('json');
    if (res.redirected || (!isJson && res.status !== 204)) {
      location.replace('/index.html');                     // expired router session: back to sign-in
      throw new ApiError(401, t('e.session'));
    }
    const body = res.status === 204 ? null : await res.json();
    if (!res.ok) {
      if (res.status === 403) throw new ApiError(403, t('e.forbidden'), t('e.forbiddenDetail'));
      if (res.status === 503) throw new ApiError(503, t('e.notConfigured'), t('e.notConfiguredDetail'));
      throw new ApiError(res.status, body?.title || res.statusText, body?.detail || '', body?.errors);
    }
    return body;
  }

  // ======================================================================
  // chrome: notices, theme, language, navigation
  // ======================================================================
  let noticeTimer;
  function notify(message, kind = 'ok') {
    const el = $('#notice');
    el.textContent = message; el.className = `notice ${kind}`; el.hidden = false;
    clearTimeout(noticeTimer);
    if (kind === 'ok') noticeTimer = setTimeout(() => { el.hidden = true; }, 4000);
  }
  const clearNotice = () => { $('#notice').hidden = true; };
  const fail = (e) => notify(e.message || String(e), 'error');

  function applyTheme(next) {
    if (next) { document.documentElement.dataset.theme = next; localStorage.setItem('udiTheme', next); }
  }
  function toggleTheme() {
    const explicit = document.documentElement.dataset.theme;
    const dark = explicit ? explicit === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches;
    applyTheme(dark ? 'light' : 'dark');
  }

  function applyLang(next) {
    if (next) { lang = next; localStorage.setItem('udiLang', lang); }
    document.documentElement.lang = lang;
    document.querySelectorAll('[data-i18n]').forEach((el) => { el.textContent = t(el.dataset.i18n); });
    document.querySelectorAll('#langSwitch button').forEach((b) => b.classList.toggle('active', b.dataset.lang === lang));
    $('#themeToggle').title = t('theme.toggle');
    $('#menuToggle').title = t('nav.toggle');
    $('#userRoles').textContent = state.scopes.join(' · ') || t('roles.none');
    $('#modePill').textContent = t(state.mode === 'btp' ? 'mode.btp' : 'mode.local');
    document.querySelectorAll('#nav a').forEach((a) => { a.title = t(`nav.${a.dataset.view}`); });
    $('#crumbs').textContent = t(`nav.${currentView()}`);
  }

  function toggleNav() {
    if (isMobile()) { $('#sidebar').classList.toggle('open'); return; }
    const collapsed = $('#shell').classList.toggle('collapsed');
    localStorage.setItem('udiNav', collapsed ? 'collapsed' : 'expanded');
  }

  function renderChrome() {
    $('#userEmail').textContent = state.user;
    $('#userEmail').title = state.user;
    $('#avatar').textContent = state.user.slice(0, 2).toUpperCase();
    $('#avatar').title = state.user;
    $('#modePill').className = `mode-pill ${state.mode}`;
    if (localStorage.getItem('udiNav') === 'collapsed') $('#shell').classList.add('collapsed');
    $('#logout').addEventListener('click', () => {
      if (state.mode === 'btp') { location.href = '/logout'; return; }
      localStorage.removeItem('udiUser'); location.href = '/index.html';
    });
    $('#menuToggle').addEventListener('click', toggleNav);
    $('#themeToggle').addEventListener('click', toggleTheme);
    $('#langSwitch').addEventListener('click', (ev) => { const b = ev.target.closest('button'); if (b) { applyLang(b.dataset.lang); route(); } });
    applyLang();
  }

  // ======================================================================
  // boot & routing
  // ======================================================================
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

  const VIEWS = { devices: renderDevicesView, audit: renderAuditView, ask: renderAskView, sources: renderSourcesView, about: renderAboutView };
  function currentView() { const v = (location.hash || '#/devices').replace('#/', '').split('/')[0]; return VIEWS[v] ? v : 'devices'; }
  async function route() {
    const view = currentView();
    document.querySelectorAll('#nav a').forEach((a) => a.classList.toggle('active', a.dataset.view === view));
    $('#crumbs').textContent = t(`nav.${view}`);
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
      const c = typeof x === 'number' ? x - y : String(x ?? '').localeCompare(String(y ?? ''), LOCALE[lang], { numeric: true });
      return dir === 'asc' ? c : -c;
    });
    return rows;
  }

  async function renderDevicesView() {
    $('#view').innerHTML = `<div class="spinner">${t('devices.loading')}</div>`;
    await loadDevices();
    renderDevices();
  }

  function renderDevices() {
    if (currentView() !== 'devices') return;
    const rows = filteredDevices();
    const pages = Math.max(1, Math.ceil(rows.length / state.pageSize));
    state.page = Math.min(state.page, pages - 1);
    const pageRows = rows.slice(state.page * state.pageSize, (state.page + 1) * state.pageSize);
    const counts = Object.fromEntries(STATUSES.map((s) => [s, state.devices.filter((d) => d.registrationStatus === s).length]));
    const visible = COLUMNS.filter((c) => state.columns[c.key]);
    const allOnPage = pageRows.length > 0 && pageRows.every((d) => state.selected.has(d.id));

    $('#view').innerHTML = `
      <div class="page-head">
        <div><h1>${t('nav.devices')}</h1><p>${t('devices.sub', { n: state.devices.length })}</p></div>
        ${state.canEdit ? `<button class="btn btn-primary" data-action="toggle-new">${state.newDeviceOpen ? t('close') : t('devices.new')}</button>` : ''}
      </div>
      ${state.newDeviceOpen ? newDevicePanel() : ''}
      <div class="toolbar">
        <div class="search"><input id="search" placeholder="${esc(t('search.ph'))}" value="${esc(state.search)}"></div>
        <div class="dropdown" id="dd-status">
          <button class="btn" data-menu="dd-status">${t('status')} ${state.statusFilter.size ? `<span class="chip">${state.statusFilter.size}</span>` : ''} ▾</button>
          <div class="menu">
            <div class="title">${t('filter.title')}</div>
            ${STATUSES.map((s) => `<label><input type="checkbox" class="check" data-filter-status="${s}" ${state.statusFilter.has(s) ? 'checked' : ''}> ${st(s)} <span class="count">${counts[s]}</span></label>`).join('')}
          </div>
        </div>
        <div class="dropdown" id="dd-cols">
          <button class="btn" data-menu="dd-cols">${t('columns')} ▾</button>
          <div class="menu">
            <div class="title">${t('columns.title')}</div>
            ${COLUMNS.map((c) => `<label><input type="checkbox" class="check" data-col="${c.key}" ${state.columns[c.key] ? 'checked' : ''}> ${t(`col.${c.key}`)}</label>`).join('')}
          </div>
        </div>
        <div class="spacer"></div>
        ${state.selected.size ? `<button class="btn" data-action="export">${t('export', { n: state.selected.size })}</button>` : ''}
        <button class="btn btn-ghost" data-action="refresh" title="${esc(t('refresh'))}">↻</button>
      </div>
      <div class="table-wrap">
        <table class="data">
          <thead><tr>
            <th class="col-check"><input type="checkbox" class="check" data-action="select-page" ${allOnPage ? 'checked' : ''}></th>
            ${visible.map((c) => `<th class="sortable ${state.sort.key === c.key ? 'sorted' : ''} ${c.num ? 'num' : ''}" data-sort="${c.key}">${t(`col.${c.key}`)}<span class="dir">${state.sort.key === c.key ? (state.sort.dir === 'asc' ? '▲' : '▼') : '↕'}</span></th>`).join('')}
            <th class="col-actions"></th>
          </tr></thead>
          <tbody>
            ${pageRows.length === 0 ? `<tr><td colspan="${visible.length + 2}" class="empty">${t('devices.empty')}</td></tr>` : ''}
            ${pageRows.map((d) => deviceRow(d, visible)).join('')}
          </tbody>
        </table>
      </div>
      <div class="table-foot">
        <span>${t('selected', { n: state.selected.size, m: rows.length })}</span>
        <div class="spacer"></div>
        <label>${t('rowsPerPage')} <select id="pageSize">${[5, 10, 25, 50].map((n) => `<option ${n === state.pageSize ? 'selected' : ''}>${n}</option>`).join('')}</select></label>
        <span>${t('range', { a: rows.length ? state.page * state.pageSize + 1 : 0, b: Math.min(rows.length, (state.page + 1) * state.pageSize), n: rows.length })}</span>
        <div class="pager">
          <button class="btn" data-action="page" data-page="${state.page - 1}" ${state.page === 0 ? 'disabled' : ''}>‹</button>
          ${pageButtons(pages)}
          <button class="btn" data-action="page" data-page="${state.page + 1}" ${state.page >= pages - 1 ? 'disabled' : ''}>›</button>
        </div>
      </div>`;
    if (state.searchFocus) { const s = $('#search'); s.focus(); s.setSelectionRange(s.value.length, s.value.length); }
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
        case 'name': return `<td><em>${esc(d.name)}</em></td>`;
        case 'riskClass': return `<td><span class="chip">${esc(d.riskClass)}</span></td>`;
        case 'registrationStatus': return `<td><span class="badge ${esc(d.registrationStatus)}">${st(d.registrationStatus)}</span></td>`;
        case 'version': return `<td class="num">${d.version}</td>`;
        case 'updatedAt': return `<td>${fmtDate(d.updatedAt)}<span class="cell-sub">${t('by', { u: esc(d.updatedBy) })}</span></td>`;
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
            <button class="item" data-action="expand" data-id="${d.id}" data-kind="trail">${t('act.trail')}</button>
            <button class="item" data-action="expand" data-id="${d.id}" data-kind="review">${t('act.review')}</button>
            <button class="item" data-action="copy" data-value="${esc(d.udiDi)}">${t('act.copy')}</button>
            ${state.canEdit ? `<hr>
              <div class="title">${t('act.changeStatus')}</div>
              ${d.allowedTransitions.length ? d.allowedTransitions.map((x) => `<button class="item" data-action="expand" data-id="${d.id}" data-kind="reason" data-status="${x}">→ ${st(x)}</button>`).join('') : `<button class="item disabled">${t('act.noTransition')}</button>`}
              <hr>
              <button class="item" data-action="expand" data-id="${d.id}" data-kind="edit">${t('act.edit')}</button>` : ''}
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
        <h3 class="form-title">${t('reason.title', { name: `<em>${esc(d.name)}</em>`, from: st(d.registrationStatus), to: st(x.status) })}</h3>
        <p class="form-sub">${t('reason.sub')}</p>
        <div class="form-grid"><div class="field wide"><label for="reason-${d.id}">${t('reason.label')}</label><input id="reason-${d.id}" name="reason" maxlength="500" placeholder="${esc(t('reason.ph'))}"><div class="err" data-err="reason"></div></div></div>
        <div class="form-actions"><button class="btn btn-primary" type="submit">${t('reason.confirm', { to: st(x.status) })}</button><button class="btn" type="button" data-action="collapse">${t('cancel')}</button><span class="hint">${t('recordedAs', { u: esc(state.user) })}</span></div>
      </form>`;
    if (x.kind === 'edit') return `
      <form data-form="edit" data-id="${d.id}">
        <h3 class="form-title">${t('edit.title', { name: `<em>${esc(d.name)}</em>` })}</h3>
        <p class="form-sub">${t('edit.sub', { v: d.version })}</p>
        <div class="form-grid">
          <div class="field"><label>${t('name')}</label><input name="name" maxlength="200" value="${esc(d.name)}"><div class="err" data-err="name"></div></div>
          <div class="field"><label>${t('manufacturer')}</label><input name="manufacturer" maxlength="200" value="${esc(d.manufacturer)}"><div class="err" data-err="manufacturer"></div></div>
          <div class="field"><label>${t('riskClass')}</label><select name="riskClass">${CLASSES.map((c) => `<option ${c === d.riskClass ? 'selected' : ''}>${c}</option>`).join('')}</select><div class="err" data-err="riskClass"></div></div>
          <div class="field wide"><label>${t('reason.label')}</label><input name="reason" maxlength="500" placeholder="${esc(t('edit.reasonPh'))}"><div class="err" data-err="reason"></div></div>
        </div>
        <div class="form-actions"><button class="btn btn-primary" type="submit">${t('edit.save')}</button><button class="btn" type="button" data-action="collapse">${t('cancel')}</button></div>
      </form>`;
    if (x.kind === 'trail') return x.data ? trailTable(x.data) : `<span class="spinner">${t('trail.loading')}</span>`;
    if (x.kind === 'review') return x.data ? reviewPanel(x.data) : `<span class="spinner">${t('review.loading')}</span>`;
    return '';
  }

  const statusOrValue = (field, value) => (field === 'registrationStatus' && value ? st(value) : (value ?? '—'));

  function trailTable(entries) {
    if (!entries.length) return `<div class="empty">${t('trail.none')}</div>`;
    return `<table class="trail"><thead><tr><th>${t('when')}</th><th>${t('action')}</th><th>${t('field')}</th><th>${t('change')}</th><th>${t('reason')}</th><th>${t('byCol')}</th></tr></thead><tbody>
      ${entries.map((e) => `<tr><td class="mono" style="white-space:nowrap">${fmtDate(e.performedAt)}</td><td>${esc(e.action)}</td><td>${esc(e.field ?? '')}</td>
        <td class="diff">${e.field ? `<span class="old">${esc(statusOrValue(e.field, e.oldValue))}</span><span class="arrow">→</span>${esc(statusOrValue(e.field, e.newValue))}` : ''}</td>
        <td>${esc(e.reason ?? '')}</td><td>${esc(e.performedBy)}</td></tr>`).join('')}
    </tbody></table>`;
  }

  function reviewPanel(r) {
    const findings = r.findings.length
      ? r.findings.map((f) => `<div class="finding"><span class="sev ${esc(f.severity)}">${esc(f.severity)}</span> · <em>${esc(f.rule)}</em><div>${esc(f.message)}</div><div class="cite"><b>[${esc(f.source)} § ${esc(f.section)}]</b></div></div>`).join('')
      : `<div class="finding">${t('review.none')}</div>`;
    return `${findings}<div class="cite">${t('review.consulted', { m: esc(r.model), p: r.citations.map((c) => esc(`${c.source} § ${c.section}`)).join('; ') || t('none') })}</div>`;
  }

  function newDevicePanel() {
    return `<div class="panel"><form data-form="create">
      <h3>${t('new.title')}</h3><p class="sub">${t('new.sub')}</p>
      <div class="form-grid">
        <div class="field"><label>${t('new.udi')}</label><input name="udiDi" class="mono" maxlength="14" placeholder="04012345678901"><div class="err" data-err="udiDi"></div></div>
        <div class="field"><label>${t('name')}</label><input name="name" maxlength="200"><div class="err" data-err="name"></div></div>
        <div class="field"><label>${t('manufacturer')}</label><input name="manufacturer" maxlength="200"><div class="err" data-err="manufacturer"></div></div>
        <div class="field"><label>${t('riskClass')}</label><select name="riskClass">${CLASSES.map((c) => `<option>${c}</option>`).join('')}</select><div class="err" data-err="riskClass"></div></div>
      </div>
      <div class="form-actions"><button class="btn btn-primary" type="submit">${t('new.create')}</button><button class="btn" type="button" data-action="toggle-new">${t('cancel')}</button></div>
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
        notify(t('n.statusChanged', { s: st(form.dataset.status) }));
        await loadDevices(true); await openExpansion(id, 'trail');
      } else if (form.dataset.form === 'edit') {
        const d = state.devices.find((x) => x.id === id);
        await api(`/api/devices/${id}`, { method: 'PUT', body: JSON.stringify({ ...data, version: d.version }) });
        notify(t('n.updated'));
        await loadDevices(true); await openExpansion(id, 'trail');
      } else if (form.dataset.form === 'create') {
        const created = await api('/api/devices', { method: 'POST', body: JSON.stringify(data) });
        notify(t('n.created', { u: created.udiDi }));
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
    const x = ev.target;
    if (x.dataset.filterStatus) { x.checked ? state.statusFilter.add(x.dataset.filterStatus) : state.statusFilter.delete(x.dataset.filterStatus); state.page = 0; renderDevices(); keepOpen('dd-status'); }
    else if (x.dataset.col) { state.columns[x.dataset.col] = x.checked; renderDevices(); keepOpen('dd-cols'); }
    else if (x.dataset.select) { x.checked ? state.selected.add(x.dataset.select) : state.selected.delete(x.dataset.select); renderDevices(); }
    else if (x.dataset.action === 'select-page') { filteredDevices().slice(state.page * state.pageSize, (state.page + 1) * state.pageSize).forEach((d) => (x.checked ? state.selected.add(d.id) : state.selected.delete(d.id))); renderDevices(); }
    else if (x.id === 'pageSize') { state.pageSize = Number(x.value); state.page = 0; renderDevices(); }
  }
  const keepOpen = (id) => { const dd = document.getElementById(id); if (dd) dd.classList.add('open'); };

  function onDocumentClick(ev) {
    if (!ev.target.closest('#search')) state.searchFocus = false;
    const menuBtn = ev.target.closest('[data-menu]');
    document.querySelectorAll('.dropdown.open').forEach((dd) => { if (!dd.contains(ev.target) || menuBtn) dd.classList.remove('open'); });
    if (menuBtn) { document.getElementById(menuBtn.dataset.menu).classList.toggle('open'); return; }
    if (isMobile() && !ev.target.closest('#sidebar') && !ev.target.closest('#menuToggle')) $('#sidebar').classList.remove('open');
    const th = ev.target.closest('th[data-sort]');
    if (th) { const key = th.dataset.sort; state.sort = { key, dir: state.sort.key === key && state.sort.dir === 'asc' ? 'desc' : 'asc' }; renderDevices(); return; }
    const btn = ev.target.closest('[data-action]'); if (!btn || btn.type === 'checkbox') return;
    const a = btn.dataset.action;
    if (a === 'expand') openExpansion(btn.dataset.id, btn.dataset.kind, btn.dataset.status);
    else if (a === 'collapse') { state.expanded = null; renderDevices(); }
    else if (a === 'toggle-new') { state.newDeviceOpen = !state.newDeviceOpen; renderDevices(); }
    else if (a === 'page') { state.page = Number(btn.dataset.page); renderDevices(); }
    else if (a === 'refresh') loadDevices(true).then(renderDevices).catch(fail);
    else if (a === 'copy') navigator.clipboard?.writeText(btn.dataset.value).then(() => notify(t('n.copied', { v: btn.dataset.value })));
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
    $('#view').innerHTML = `<div class="spinner">${t('audit.loading')}</div>`;
    const [entries] = await Promise.all([api('/api/audit-trail'), loadDevices()]);
    const names = Object.fromEntries(state.devices.map((d) => [d.id, d]));
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>${t('nav.audit')}</h1><p>${t('audit.sub')}</p></div></div>
      <div class="table-wrap"><table class="data">
        <thead><tr><th>${t('when')}</th><th>${t('device')}</th><th>${t('action')}</th><th>${t('field')}</th><th>${t('change')}</th><th>${t('reason')}</th><th>${t('byCol')}</th></tr></thead>
        <tbody>${entries.map((e) => { const d = names[e.entityId]; return `<tr>
          <td class="mono" style="white-space:nowrap">${fmtDate(e.performedAt)}</td>
          <td>${d ? `${esc(d.name)}<span class="cell-sub mono">${esc(d.udiDi)}</span>` : `<span class="mono">${esc(e.entityId)}</span>`}</td>
          <td><span class="chip">${esc(e.action)}</span></td><td>${esc(e.field ?? '')}</td>
          <td class="diff">${e.field ? `<span class="old">${esc(statusOrValue(e.field, e.oldValue))}</span><span class="arrow">→</span>${esc(statusOrValue(e.field, e.newValue))}` : ''}</td>
          <td>${esc(e.reason ?? '')}</td><td>${esc(e.performedBy)}</td></tr>`; }).join('')}
        </tbody></table></div>
      <div class="table-foot"><span>${t('audit.count', { n: entries.length })}</span></div>`;
  }

  // ======================================================================
  // Ask the regulation
  // ======================================================================
  async function renderAskView() {
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>${t('nav.ask')}</h1><p>${t('ask.sub')}</p></div></div>
      <form class="ask-form" data-form="ask"><input id="question" name="question" maxlength="1000" placeholder="${esc(t('ask.ph'))}" autocomplete="off"><button class="btn btn-primary" type="submit">${t('ask')}</button></form>
      <div class="suggest">${t('ask.suggest').map((q) => `<button class="btn btn-sm" data-action="suggest" data-q="${esc(q)}">${esc(q)}</button>`).join('')}</div>
      <div class="qa" id="qa"></div>`;
    renderQa();
  }

  function renderQa() {
    const el = $('#qa'); if (!el) return;
    el.innerHTML = state.qa.map((x) => `<div class="panel">
      <div class="q">${esc(x.question)}</div>
      ${x.pending ? `<div class="spinner">${t('ask.thinking')}</div>` : x.error ? `<div class="error">${esc(x.error)}</div>` : `<div class="a">${esc(x.answer.answer)}</div>
        ${x.answer.citations.map((c) => `<div class="cite"><b>[${esc(c.source)} § ${esc(c.section)}]</b> ${esc(c.excerpt).slice(0, 240)}…</div>`).join('')}
        <div class="cite">${t('ask.retrieved', { m: esc(x.answer.model), n: x.answer.citations.length })}</div>`}
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
    $('#view').innerHTML = `<div class="spinner">${t('loading')}</div>`;
    state.sources = state.sources || await api('/api/assistant/sources');
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>${t('nav.sources')}</h1><p>${t('sources.sub', { s: state.sources.length, n: state.sources.reduce((n, s) => n + s.sections.length, 0) })}</p></div></div>
      <div class="panel sources">${state.sources.map((s) => `<div class="src"><h3><span class="mono">${esc(s.source)}</span></h3><ul>${s.sections.map((x) => `<li>${esc(x)}</li>`).join('')}</ul></div>`).join('')}</div>`;
  }

  // ======================================================================
  // About
  // ======================================================================
  async function renderAboutView() {
    const kv = (rows) => `<table class="kv">${rows.map(([k, v]) => `<tr><td>${k}</td><td>${v}</td></tr>`).join('')}</table>`;
    $('#view').innerHTML = `
      <div class="page-head"><div><h1>${t('nav.about')}</h1><p>${t('about.sub')}</p></div></div>
      <div class="panel about">
        <h2>${t('about.what')}</h2>
        ${kv([[t('about.domain'), t('about.domainText')], [t('about.trail'), t('about.trailText')], [t('about.security'), t('about.securityText')], [t('about.assistant'), t('about.assistantText')], [t('about.platform'), t('about.platformText')]])}
        <h2>${t('about.stack')}</h2>
        ${kv([[t('about.service'), 'Java 17 · Spring Boot 4 · Spring Data JPA · Flyway · Spring Security (JWT resource server) · Spring AI 2 · H2 (demo)'], [t('about.router'), t('about.routerText')], [t('about.method'), t('about.methodText')]])}
        <h2>${t('about.session')}</h2>
        ${kv([[t('mode'), t(state.mode === 'btp' ? 'about.modeBtp' : 'about.modeLocal')], [t('user'), esc(state.user)], [t('scopes'), esc(state.scopes.join(', ')) || '—']])}
        <p style="margin-top:14px"><a href="https://github.com/tarekakel/udi-assistant" target="_blank" rel="noopener">${t('about.source')}</a></p>
      </div>`;
  }

  init().catch(fail);
})();
