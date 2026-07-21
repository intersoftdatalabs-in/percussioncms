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

| Direction | Allowed? | Notes |
|-----------|----------|--------|
| **sitemanage → rest** | **Yes** | Already declared in this module’s `pom.xml`. Needed for `IXxxAdaptor`, wire DTOs, rest errors. |
| **rest → sitemanage** | **Never** | Causes `ProjectCycleException`: `rest → sitemanage → rest`. |

If rest code “needs” a sitemanage type:

1. Move or define the **wire DTO / API type** in **rest**.
2. Keep domain logic and service interfaces used only by the bridge in **sitemanage**.
3. Implement `IXxxAdaptor` here under `com.percussion.apibridge`.

Do not “fix” a rest compile error by adding sitemanage to `rest/pom.xml`.

## apibridge architecture

Package: `com.percussion.apibridge`

This package is the **only** place that should implement public REST adaptor interfaces from
`com.percussion.rest.*`.

```text
rest JAX-RS Resource
        │  injects interface
        ▼
rest IXxxAdaptor  (interface + wire DTOs live in rest)
        │  implemented by
        ▼
sitemanage apibridge XxxAdaptor  (@PSSiteManageBean)
        │  calls
        ▼
sitemanage / perc-system domain services
```

### Conventions

| Concern | Where | Example |
|---------|--------|---------|
| HTTP resource | `rest` | `RelationshipSummaryResource` |
| Adaptor interface | `rest` | `IRelationshipSummaryAdaptor` |
| Wire DTOs | `rest` | `com.percussion.share.relationship.data.PSRelationshipSummary` |
| Adaptor impl | **this module** `apibridge` | `RelationshipSummaryAdaptor` |
| Domain service | **this module** (or perc-system) | `IPSRelationshipSummaryService` / `PSRelationshipSummaryService` |

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

| Artifact | Module | Location |
|----------|--------|----------|
| Wire DTOs | rest | `rest/src/main/java/com/percussion/share/relationship/data/` |
| Adaptor interface + resource | rest | `rest/src/main/java/com/percussion/rest/relationsummary/` |
| Adaptor impl + tests | **sitemanage** | `apibridge/RelationshipSummaryAdaptor[.java]` + test |
| Domain service | **sitemanage** | `share/relationship/service/` |

Use this as the template for any new public REST feature that needs sitemanage domain code.

## Building & testing

```bash
# From repo root — always use the env wrapper (JDK 21)
./mvn-env.sh -pl projects/sitemanage -am test

# Focused tests
./mvn-env.sh -pl projects/sitemanage -Dtest=PSRelationshipSummaryServiceTest,RelationshipSummaryAdaptorTest test
./mvn-env.sh -pl rest -Dtest=RelationshipSummaryResourceTest test
```

Windows: `mvn-env.bat` with the same `-pl` arguments.

If Maven reports a **cyclic reference** between `rest` and `sitemanage`, inspect `rest/pom.xml` first —
a sitemanage dependency there is almost always the cause.

## Cross-platform

Same rules as root AGENTS.md: portable `Path` / `Files` APIs; no Unix-only path assumptions in
production code or tests.

## Documentation

When changing layering, dependency direction, or apibridge conventions:

1. Update **this** `AGENTS.md` and [rest/AGENTS.md](../../rest/AGENTS.md) together.
2. Keep [README.md](README.md) aligned for human-oriented module overview.
3. Add unit tests for new adaptor or domain behaviour (project rule — no exceptions).
