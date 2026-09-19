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

- **UI:** http://localhost:8080/ — the same single-page app the approuter serves on BTP (Spring serves
  `approuter/resources` as static content locally). There is no identity provider locally, so the sign-in page asks for
  a name; it is sent as `X-User` and recorded in the audit trail.
- API: http://localhost:8080/api/devices (20 seeded devices with lifecycle history)
- Audit trail: http://localhost:8080/api/audit-trail (all devices, newest first) and `/api/devices/{id}/audit-trail`
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:udi`, user `sa`, no password)
- Health: http://localhost:8080/actuator/health

Secrets go in `srv/.env` (copy `srv/.env.example`).

## The UI

Plain HTML/CSS/JS in `approuter/resources` — no framework and no build step, so the approuter serves it as static
files and Spring serves the same files locally.

| Page | What it does |
|---|---|
| Sign in (`/index.html`, public) | On BTP: hands over to XSUAA ("Sign in with SAP BTP"). Locally: pick the name to record. |
| Devices | Data table with search, status filter with counts, sortable columns, column visibility, row selection with CSV export, pagination. Row actions: audit trail, review against the regulation, copy UDI-DI, status transitions and edit (Editor only). Every write asks for a reason **inline**, never in a browser popup. |
| Audit trail | Last 200 entries across all devices: when, device, action, field, old → new, reason, by. |
| Ask the regulation | Question box with suggested questions; each answer shows its citations and the retrieved passages. |
| Knowledge sources | The corpus the assistant can cite, by source and section. |
| About | What the project demonstrates, the stack, and the current session (mode, user, scopes). |

A SAPUI5 (TypeScript) client for the same API is the planned next UI step.

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
| Open the router URL | Sign-in page → XSUAA login → the device table with your identity and scopes in the sidebar |
| Row menu → a status transition, enter a reason | The row's audit trail opens with the new entry and your e-mail as `performedBy` |
| Remove the Editor role collection, sign out and in | Transitions and edit disappear from the row menu; a direct API write returns 403 |
| Ask the regulation → "What is the capital of France?" | "Not covered by the regulation excerpts available to me." with no citations |
| Row menu → Review against regulation (OrthoFix bone screw) | A CRITICAL finding citing `eudamed § Registration before placing on the market` |
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

## MCP server: the same data for AI agents

The service is also a [Model Context Protocol](https://modelcontextprotocol.io) server at `/mcp` (stateless streamable
HTTP). An agent such as Claude Desktop, Cursor or a Spring AI application can call four **read-only** tools:

| Tool | Returns |
|---|---|
| `findDevice(udiDi)` | Master data, registration status and the transitions allowed next |
| `searchDevices(text?, status?, limit?)` | Devices matching free text over UDI-DI, name and manufacturer |
| `getAuditTrail(udiDi)` | Who changed what, when and why, oldest first |
| `searchRegulation(question)` | The most relevant regulation passages with source and section, no generated answer |

Agents read and cite; they do not write. Changing regulated master data stays a human action with a recorded reason.
The tools call the same services as the REST API and return the same JSON shapes, so an error such as an unknown
UDI-DI comes back as a readable tool error.

**Locally** (service running on port 8080, no token needed), for Claude Desktop add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "udi-assistant": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/mcp", "--transport", "http-only"]
    }
  }
}
```

Then ask, for example: *"Which submitted devices does Hersfeld Biomaterials have, and what does the regulation say
about labelling them?"* The agent will call `searchDevices` and `searchRegulation` and cite the sections.

**On BTP** the endpoint is protected by XSUAA like the API. An agent is a technical client: create a service key on the
XSUAA instance, fetch a client-credentials token, and pass it as a bearer header. The token carries the scopes the
application declares, so `Viewer` is satisfied.

```powershell
cf create-service-key udi-assistant-uaa mcp-client
cf service-key udi-assistant-uaa mcp-client      # clientid, clientsecret, url
$token = (curl -s -u "<clientid>:<clientsecret>" "<url>/oauth/token" -d "grant_type=client_credentials" | ConvertFrom-Json).access_token
curl -s https://<srv-url>/mcp -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
     -H "Accept: application/json, text/event-stream" `
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

For Claude Desktop against BTP, add `"--header", "Authorization: Bearer ${MCP_TOKEN}"` to the `mcp-remote` arguments
and put the token in the `env` block of the server entry. A production setup would let `mcp-remote` run the OAuth
flow against XSUAA instead of pasting a token.

## Security model

- The approuter authenticates the user with XSUAA (authorization code flow) and checks scopes per HTTP method
  before forwarding; the JWT travels to the service in `Authorization: Bearer`.
- The service is a stateless resource server: signature via XSUAA's `token_keys`, timestamp checks, an audience check,
  and the same scope rules again (`GET` → Viewer or Editor, anything else → Editor; `/mcp` → Viewer or Editor).
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
assistant with a stubbed `ChatModel` and index to pin down the prompt contract and citation handling, and connect a
real MCP client (the official SDK) to the running server over HTTP to check the tool list, results and tool errors.
No test calls OpenAI.
