# udi-assistant

A small, GxP-flavoured master-data service for medical devices identified by UDI-DI, running on **SAP BTP (Cloud Foundry)**.
Built to demonstrate end-to-end engineering on one stack: Java 17 / Spring Boot 4 / JPA / SQL, XSUAA security behind an
application router, an AI assistant (RAG) and MCP integration.

The specification in [SPEC.md](SPEC.md) is the source of truth; [DECISIONS.md](DECISIONS.md) records the non-obvious choices
and the places where tooling or generated code was wrong.

## Layout

```
udi-assistant/
  mta.yaml            multitarget application: service + router + XSUAA instance
  xs-security.json    scopes (Viewer, Editor), role templates, role collections
  srv/                Spring Boot service (Java 17)
  approuter/          @sap/approuter: login, per-method scope checks, JWT forwarding, demo page
```

## Run locally

```powershell
cd srv
.\mvnw.cmd spring-boot:run
```

- API: http://localhost:8080/api/devices (five seeded devices)
- Audit trail: http://localhost:8080/api/devices/{id}/audit-trail
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:udi`, user `sa`, no password)
- Health: http://localhost:8080/actuator/health

Locally there is no login; pass `X-User: <name>` to have the audit trail record you. Secrets go in `srv/.env`
(copy `srv/.env.example`).

## Deploy to SAP BTP

Prerequisites: JDK 17+, Maven 3.9+ and Node 20+ on the PATH; [cf CLI](https://github.com/cloudfoundry/cli/releases)
with `cf install-plugin multiapps`; `npm install -g mbt` (on Windows `mbt` also needs GNU make, e.g.
`winget install GnuWin32.Make`, and `C:\Program Files (x86)\GnuWin32\bin` on the PATH); `cf login` targeting a space.

```powershell
mbt build
cf deploy mta_archives\udi-assistant_0.1.0.mtar
```

Then, once per user, in the BTP Cockpit: **Security → Users → your user → Assign Role Collection →**
`udi-assistant-Editor` (or `udi-assistant-Viewer`). Open the approuter route printed by `cf apps`.

| Try | Expect |
|---|---|
| Open the router URL | XSUAA login, then the device list with your identity and scopes |
| Advance a status, enter a reason | New audit row with your e-mail as `performedBy` |
| Remove the Editor role collection, try again | 403 from the router (and from the service if called directly) |
| `cf env udi-assistant-srv` | `VCAP_SERVICES.xsuaa[0].credentials` — the binding the service validates tokens against |

## Regulation assistant (RAG)

`POST /api/assistant/ask` answers questions about UDI/MDR requirements from a small, versioned corpus
(`srv/src/main/resources/regulation/*.md`) and returns the retrieved passages as citations. `POST /api/devices/{id}/review`
checks a device record against the same passages and returns structured findings. `GET /api/assistant/sources` shows
what the assistant knows.

```powershell
curl -X POST http://localhost:8080/api/assistant/ask -H "Content-Type: application/json" `
     -d '{"question":"When does a change require a new UDI-DI?"}'
```

The assistant is optional. Without a key the application starts normally and these endpoints return 503.

| Where | How the key is provided | Notes |
|---|---|---|
| Local | `OPENAI_API_KEY=sk-...` in `srv/.env` | An `OPENAI_API_KEY` OS environment variable takes precedence over `.env` |
| BTP | `cf cups openai -p '{"apiKey":"sk-..."}'` then bind (`mta.yaml` requires the `openai` user-provided service) | Read from `VCAP_SERVICES`, never stored in the descriptor or git |

Models: `gpt-4o-mini` for chat, `text-embedding-3-small` for embeddings (`app.openai.*`). Swapping the Spring AI starter
switches the provider (Azure OpenAI, SAP AI Core) without code changes. The vector index is in-memory; production would
use SAP HANA Cloud Vector Engine or pgvector behind the same `RegulationIndex` interface.

## Security model

- The approuter authenticates the user with XSUAA (authorization code flow) and checks scopes per HTTP method
  before forwarding; the JWT travels to the service in `Authorization: Bearer`.
- The service is a stateless resource server: signature via XSUAA's `token_keys`, timestamp checks, an audience check,
  and the same scope rules again (`GET` → Viewer or Editor, anything else → Editor).
- The authenticated `user_name` becomes the audit-trail user through the `CurrentUserProvider` strategy; locally the
  `X-User` header plays that role.

## Tests

```powershell
cd srv
.\mvnw.cmd test
```

Unit tests cover the GTIN-14 check digit, the status lifecycle, change detection, scope mapping, the audience check and
corpus parsing. Integration tests boot the service against H2 for the API and the audit trail, boot the `cloud` profile
with a stubbed `JwtDecoder` to prove scope enforcement and that the token user lands in the trail, and boot the
assistant with a stubbed `ChatModel` and index to pin down the prompt contract and citation handling. No test calls
OpenAI.
