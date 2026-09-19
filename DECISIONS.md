# Decisions and lessons

A running log of non-obvious decisions, and of places where a tool, a library, or an AI-generated suggestion was wrong and what was done about it. Newest first.

## 2026-09-19 — XSUAA plan visible in the marketplace but not provisionable: re-sync the entitlement

**Symptom.** `cf deploy` failed creating the XSUAA instance: `Service broker error: service plan not found or not
accessible`. `cf marketplace -e xsuaa` listed the `application` plan; the Cockpit showed the entitlement assigned with
one unit; creating the instance from the Cockpit failed with the same message; `xs-security.json` was not involved
(a bare `cf create-service xsuaa application probe` failed too).

**Cause.** The Cloud Foundry org held a stale plan mapping: `cf marketplace` reflects what the broker advertises to the
region, while `provision` is checked against the subaccount entitlement as the org knows it. In this trial subaccount
the two had drifted apart.

**Fix.** Cockpit → Entitlements → remove the `application` plan → Save → add it back → Save. About a minute later
`cf create-service` succeeded and the deployment went through. The descriptor now also lists redirect URIs for the
`us10` trial region so the same archive can be deployed there if a region is ever the problem.

**Lesson.** On BTP, "the plan is in the marketplace" and "the org may provision the plan" are two different facts.
When the broker says *not accessible* and the entitlement looks right, re-saving the entitlement is the first thing
to try, before rebuilding anything.

## 2026-09-19 — Standard `maven` builder in mta.yaml; real Maven and GNU make on Windows

**Symptom.** `mbt build` on Windows failed three different ways: `make` not found; then `mvnw.cmd` not found although
the working directory was correct (mbt execs commands without a shell, and `cmd /c` did not resolve the wrapper from
the current directory either); then the backslash in `.\mvnw.cmd` was stripped between the generated Makefile and
the shell.

**Decision.** Stop building around the Maven wrapper. `mta.yaml` uses the default `builder: maven` (`mvn -B package`),
which is what every SAP example and CI pipeline expects, and the machine gets a proper Maven 3.9 install plus GNU make
(`winget install GnuWin32.Make`). The wrapper stays in `srv/` for local development.

**Lesson.** A custom build command in `mta.yaml` is a portability hazard; the default builders exist for a reason.
When a tool "cannot find" a file that is demonstrably there, check whether it is spawning a shell at all.

## 2026-09-19 — Plain Spring Security resource server instead of SAP's Spring XSUAA starter

**Context.** The service runs Spring Boot 4.1 / Spring Security 7. SAP's `resourceserver-security-spring-boot-starter`
targets Boot 3 / Security 6 and relies on auto-configuration that may not survive the major upgrade.

**Decision.** Validate XSUAA tokens with the standard `spring-boot-starter-oauth2-resource-server`:
`NimbusJwtDecoder` against `<xsuaa.url>/token_keys`, default timestamp validators, plus a small audience validator
(`aud` or `client_id` must match the bound instance). Scopes `xsappname.Editor` become authorities `Editor` through
a ten-line converter. Credentials come from `VCAP_SERVICES`, which Boot still maps to `vcap.services.*` properties
(`CloudFoundryVcapEnvironmentPostProcessor` is present in Boot 4.1 — verified in the jar).

**Consequence.** No dependency on SAP library release cadence, and the token handling is visible in the codebase
rather than hidden in a starter. If multi-tenancy is added later, the decoder must resolve keys per tenant
(`zid` claim / per-subdomain JWK set) — that is the point at which the SAP library earns its place.

## 2026-09-19 — Scope checks at the router *and* in the service

The approuter enforces `Viewer`/`Editor` per HTTP method (`xs-app.json`). The service repeats the check. Defence in
depth: an internal caller, a misconfigured route, or a second router must not widen access. `csrfProtection` is off on
the API route because the API is called with bearer tokens by non-browser clients as well; the UI5 client (step 5)
will fetch an `x-csrf-token` and the flag can then be turned on.

## 2026-09-19 — Disable java-cfenv injection in the Cloud Foundry Java buildpack

**Symptom.** First `cf push` of the Spring Boot 4.1 service crash-looped on BTP with
`NoSuchMethodError: io.micrometer.common.util.internal.logging.WarnThenDebugLogger.isEnabled()`.
Locally the same jar started fine.

**Cause.** `java_buildpack` v4.77 automatically adds `java-cfenv-3.4.0.jar` to every application. That jar
shades an older copy of `WarnThenDebugLogger`; the JVM loaded it ahead of the one in `micrometer-commons-1.17.1`
that Boot 4 depends on. Classpath conflict, not an application bug.

**Decision.** Set `JBP_CONFIG_JAVA_CF_ENV: '{ enabled: false }'` in `manifest.yml`. With java-cfenv off the
buildpack falls back to the deprecated *Spring Auto Reconfiguration* framework and warns about it, so
`JBP_CONFIG_SPRING_AUTO_RECONFIGURATION: '{ enabled: false }'` is set as well. The app does not use either;
service bindings are read explicitly (XSUAA via the SAP security library, secrets via environment) rather
than through buildpack auto-configuration.

**Lesson.** Platform buildpacks inject code you did not ask for. When a cloud start-up failure names a class
that is not in your `pom.xml`, check `BOOT-INF/lib` in the failure report for jars the buildpack added.

## 2026-09-19 — Spring Boot 4 splits the H2 console into its own module

`spring.h2.console.enabled=true` had no effect: Boot 4 moved the console auto-configuration to
`spring-boot-h2console`, which is not pulled in by the JPA or H2 dependencies. Added it with `runtime` scope.
Documentation and most AI-generated snippets still describe the Boot 3 behaviour.

## 2026-09-19 — Audit trail as an observer of domain events, written before commit

Alternatives considered: JPA entity listeners (`@PreUpdate`) and Hibernate Envers.
Both were rejected because neither can carry the *reason for change*, which a regulated audit trail needs,
and Envers snapshots whole rows rather than field-level transitions. The application service publishes an
`EntityChangedEvent`; the audit module records it in `TransactionPhase.BEFORE_COMMIT` so the change and its
trail are atomic. CREATE writes one row, UPDATE one row per changed field, a no-op update writes nothing.

## 2026-09-19 — No DELETE endpoint

Regulated master data is withdrawn through the status lifecycle (`REGISTERED -> WITHDRAWN`), never removed.
Deliberate omission, documented in `SPEC.md` R5 and the controller Javadoc.

## 2026-09-19 — Schema owned by Flyway, Hibernate set to `validate`

`ddl-auto=update` is convenient locally and dangerous everywhere else. The SQL migration is the single
schema definition and runs unchanged against PostgreSQL or SAP HANA Cloud later.
