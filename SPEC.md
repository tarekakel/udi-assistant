# udi-assistant — Specification

Single source of truth for the service. Code, tests and docs are derived from this file; when they disagree, this file wins and the others are regenerated.

## 1. Purpose
A small, GxP-flavoured master-data service for medical devices identified by UDI-DI, running on SAP BTP (Cloud Foundry). It exists to demonstrate end-to-end engineering on the p36 stack: Java/JPA/SQL, BTP security, an AI assistant (RAG) and MCP integration.

## 2. Domain
- **Device** — aggregate root. Attributes: `udiDi` (GTIN-14, immutable, unique), `name`, `manufacturer`, `riskClass` (EU MDR: I, IIA, IIB, III), `registrationStatus`.
- **RegistrationStatus** lifecycle: `DRAFT -> SUBMITTED -> REGISTERED -> WITHDRAWN`; `SUBMITTED -> DRAFT` (rejection). `WITHDRAWN` is terminal. Records are never deleted.
- **AuditEntry** — append-only line: entity, action (`CREATE | UPDATE | STATUS_CHANGE`), field, old value, new value, reason, performed by, performed at.

## 3. Rules
- R1 `udiDi` must be 14 digits with a valid GS1 mod-10 check digit.
- R2 `udiDi` is unique across all devices (409 on conflict).
- R3 Every update and status change requires a non-blank `reason`.
- R4 Updates carry the `version` the editor saw; a mismatch is rejected with 409 (optimistic locking).
- R5 Status may only change along the lifecycle above (409 otherwise).
- R6 Every business change produces audit entries in the same transaction as the change: one entry for CREATE, one per changed field otherwise. An update that changes nothing produces no entry.
- R7 The acting user is resolved by a pluggable strategy: `X-User` header locally, XSUAA JWT on BTP; background work runs as `system`.
- R8 Errors are RFC 9457 problem details; validation errors list the offending fields.

## 4. API (`/api`)
| Method | Path | Body | Result |
|---|---|---|---|
| GET | `/devices?status=&page=&size=&sort=` | – | page of devices |
| GET | `/devices/{id}` | – | device incl. `allowedTransitions` |
| POST | `/devices` | udiDi, name, manufacturer, riskClass | 201 + Location |
| PUT | `/devices/{id}` | name, manufacturer, riskClass, version, reason | 200 |
| POST | `/devices/{id}/status` | status, reason | 200 |
| GET | `/devices/{id}/audit-trail` | – | entries, oldest first |

## 5. Non-functional
- Schema owned by Flyway migrations; Hibernate validates, never generates.
- H2 in-memory for local/demo; PostgreSQL or SAP HANA Cloud in production (same migrations).
- `/actuator/health` for platform health checks.
- Secrets via `.env` locally (git-ignored), environment / user-provided service on BTP.

## 6. Security
- S1 Two scopes: `Viewer` (read devices and trails) and `Editor` (create, update, change status). `Editor` implies `Viewer`.
- S2 The application router authenticates users against XSUAA and enforces scopes per HTTP method before forwarding.
- S3 The service validates every request's JWT itself (signature via XSUAA `token_keys`, expiry, audience) and applies the same scope rules — bypassing the router gains nothing.
- S4 The audit-trail user is the token's `user_name` (fallback `email`, then `client_id`).
- S5 `/actuator/health` is public; everything else requires a valid token.
- S6 Locally (no `cloud` profile) the API is open and the user comes from the `X-User` header; that mode never runs on BTP.

## 7. Roadmap
1. Devices + audit trail — done
2. XSUAA + approuter, `Viewer` / `Editor` scopes, JWT user strategy — done
3. "Ask the regulation": RAG over MDR/UDI guidance (OpenAI, in-memory vector store; HANA Cloud Vector Engine in prod)
4. MCP server exposing `findDevice` and `searchRegulation`
5. One SAPUI5 (TypeScript) view replacing the plain demo page served by the router
