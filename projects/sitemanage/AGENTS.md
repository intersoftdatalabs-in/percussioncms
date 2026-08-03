This project follows the Universal Code v1.0.0 - read ../../docs/policies/UC-EMBED-v1.0.0.md (vendored; upstream https://github.com/monkeyking-hq/universal-code)

# sitemanage AI Agent Notes

## Module role

`projects/sitemanage` is the CM1 middleware layer: domain services, site/gadget/widget APIs, and the
**apibridge** that implements the public REST adaptor contracts defined in the `rest` module.

Primary consumers:

- **WebUI** (internal/CM1 REST used by the CMS UI)
- **Public REST** (`./rest`) — via Spring beans implementing `com.percussion.rest.*.IXxxAdaptor`
- Installer / runtime packaging (loaded in the Rhythmyx webapp with perc-system)

## Required reading

- This file (module rules — especially **Maven dependency direction** and **apibridge**)
- [rest/AGENTS.md](../../rest/AGENTS.md) — public REST surface, wire DTOs, adaptor interfaces
- [sitemanage README](README.md) — high-level module purpose
- Root [AGENTS.md](../../AGENTS.md) — monorepo rules, JDK 21, cross-platform I/O

## Maven dependency direction (HARD RULE — no reactor cycles)

```text
rest  ──does NOT depend on──▶  sitemanage
  ▲
  │  Maven dependency (required)
  │
sitemanage  ──depends on──▶  rest, perc-system, perc-deployer, ...
```

|       Direction       | Allowed?  |                                             Notes                                              |
|-----------------------|-----------|------------------------------------------------------------------------------------------------|
| **sitemanage → rest** | **Yes**   | Already declared in this module’s `pom.xml`. Needed for `IXxxAdaptor`, wire DTOs, rest errors. |
| **rest → sitemanage** | **Never** | Causes `ProjectCycleException`: `rest → sitemanage → rest`.                                    |

If rest code “needs” a sitemanage type:

1. Move or define the **wire DTO / API type** in **rest**.
2. Keep domain logic and service interfaces used only by the bridge in **sitemanage**.
3. Implement `IXxxAdaptor` here under `com.percussion.apibridge`.

Do not “fix” a rest compile error by adding sitemanage to `rest/pom.xml`.

## apibridge architecture

Package: `com.percussion.apibridge`

This package is the **only** place that should implement public REST adaptor interfaces from
`com.percussion.rest.*`.

### Workbench replacement (HARD RULES)

apibridge is **thin glue**, not a second Workbench. Prefer **design webservices**
(`IPSContentDesignWs`, `IPSUiDesignWs`, `IPSAssemblyDesignWs`, …) when implementing
Developer/Workbench-replacement catalogs — same backends SOAP used. Do **not** wire
partial CM1/sitemanage product REST “because it’s already REST-like.”

Canonical rules: [`docs/developer-module/workbench-rest-and-qa-modes.md`](../../docs/developer-module/workbench-rest-and-qa-modes.md).  
Audit matrix: [`docs/ai-generated/tasks/developer-module-p0/adaptor-design-ws-audit.md`](../../docs/ai-generated/tasks/developer-module-p0/adaptor-design-ws-audit.md).

```text
rest JAX-RS Resource
        │  injects interface
        ▼
rest IXxxAdaptor  (interface + wire DTOs live in rest)
        │  implemented by
        ▼
sitemanage apibridge XxxAdaptor  (@PSSiteManageBean)
        │  calls (Workbench replacement prefer:)
        ▼
IPS*DesignWs / design system APIs  (SOAP reference)
  or documented ALT system owners when no design-WS twin
```

### Conventions

|      Concern      |              Where               |                             Example                              |
|-------------------|----------------------------------|------------------------------------------------------------------|
| HTTP resource     | `rest`                           | `RelationshipSummaryResource`                                    |
| Adaptor interface | `rest`                           | `IRelationshipSummaryAdaptor`                                    |
| Wire DTOs         | `rest`                           | `com.percussion.share.relationship.data.PSRelationshipSummary`   |
| Adaptor impl      | **this module** `apibridge`      | `RelationshipSummaryAdaptor`                                     |
| Domain service    | **this module** (or perc-system) | `IPSRelationshipSummaryService` / `PSRelationshipSummaryService` |

**Adaptor implementation checklist:**

1. Class under `src/main/java/com/percussion/apibridge/`.
2. Annotate with `@PSSiteManageBean` (preferred) — matches other adaptors (`PreferencesAdaptor`,
   `UserAdaptor`, `SitesAdaptor`, …). Avoid putting production adaptor impls in `rest`.
3. `implements` the rest-module interface (`IXxxAdaptor`).
4. Inject domain collaborators (`@Autowired` ctor or fields); map domain results to **rest wire DTOs**.
5. Translate AuthZ / not-found / validation into REST-friendly outcomes (e.g. `Optional.empty()` →
   `WebApplicationException` with 403/404) when the resource expects that contract.
6. Unit tests under `src/test/java/com/percussion/apibridge/` (or next to the domain service tests
   for pure service logic).
7. When the rest resource is new or injects a **new** adaptor interface, the **rest** module also
   needs a Spring test stub (`TestXxxAdaptor` under `rest/.../test/apibridge/`). This is an instance
   of root [AGENTS.md](../../AGENTS.md) → **Change-class completeness** — see also
   [rest/AGENTS.md](../../rest/AGENTS.md). Without the stub, `rest` `MainTest` / `RolesTest` /
   `UsersTest` fail with `No qualifying bean of type '…Adaptor'`.

### What does *not* belong in apibridge

- JAX-RS `@Path` resources (those stay in `rest`)
- OpenAPI-only annotation surfaces with no domain work (stay on rest resources)
- Generic utilities unrelated to bridging rest contracts (prefer `share` / domain packages)

## Domain services vs REST

- **Domain services** (e.g. `com.percussion.share.relationship.service.IPSRelationshipSummaryService`)
  own AuthZ and cataloger/widget/JCR logic. They may return rest wire DTOs when those types live in
  rest (sitemanage already depends on rest).
- **Internal/CM1 REST** hosted in sitemanage (non-`com.percussion.rest` packages) is separate from the
  public rest module; do not conflate the two when adding endpoints.
- Prefer extending existing domain services over duplicating relationship/site/path logic in the
  adaptor.

## Relationship summary (US8) reference layout

Canonical split after the rest↔sitemanage cycle fix:

|           Artifact           |     Module     |                           Location                           |
|------------------------------|----------------|--------------------------------------------------------------|
| Wire DTOs                    | rest           | `rest/src/main/java/com/percussion/share/relationship/data/` |
| Adaptor interface + resource | rest           | `rest/src/main/java/com/percussion/rest/relationsummary/`    |
| Adaptor impl + tests         | **sitemanage** | `apibridge/RelationshipSummaryAdaptor[.java]` + test         |
| Domain service               | **sitemanage** | `share/relationship/service/`                                |

Use this as the template for any new public REST feature that needs sitemanage domain code.

## Building & testing

```bash
# Prefer standalone module builds (from module dir) — see root AGENTS.md Pre-PR Maven gate
cd projects/sitemanage && ../../mvnw clean install
cd rest && ../mvnw clean install   # when rest interfaces/resources/stubs changed too

# Focused tests
cd projects/sitemanage && ../../mvnw test -Dtest=I18nCorrectionsAdaptorImplTest
cd rest && ../mvnw test -Dtest=MainTest,I18nCorrectionsResourceTest
```

Windows: `mvnw.cmd` with the same relative paths.

If Maven reports a **cyclic reference** between `rest` and `sitemanage`, inspect `rest/pom.xml` first —
a sitemanage dependency there is almost always the cause.

### Stubbing `PSServer` / server.properties in unit tests

Instance of root **Change-class completeness** → *match production types in test fakes*.

`PSServer.ms_serverProps` is typed **`com.percussion.util.PSProperties`** (extends
`java.util.Properties`), **not** plain `java.util.Properties`.

Reflective tests that do `propsField.set(null, new Properties())` fail with:

```text
IllegalArgumentException: Can not set static com.percussion.util.PSProperties field
com.percussion.server.PSServer.ms_serverProps to java.util.Properties
```

**Do:**

```java
import com.percussion.util.PSProperties;

propsField.set(null, new PSProperties());
// cast previous / current field values to PSProperties
```

**Do not** assign a bare `java.util.Properties` instance to that field. Reading via
`PSServer.getProperty` / `getServerProps()` still works because `PSProperties` is-a `Properties`.

Always restore the previous static value in `@AfterEach`.

## Cross-platform

Same rules as root AGENTS.md: portable `Path` / `Files` APIs; no Unix-only path assumptions in
production code or tests.

## Documentation

When changing layering, dependency direction, or apibridge conventions:

1. Update **this** `AGENTS.md` and [rest/AGENTS.md](../../rest/AGENTS.md) together.
2. Keep [README.md](README.md) aligned for human-oriented module overview.
3. Add unit tests for new adaptor or domain behaviour (project rule — no exceptions).

