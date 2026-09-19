## Summary

Slice 6 of #4531 (issue #4581): PublishingShell site workspace shows a server-driven
item publishing actions menu that consumes the existing sitemanage
`GET …/publish/publishingActions/{id}` endpoint and navigates to the already-shipped
panels (publish now, schedule, takedown, stage). Unavailable actions render disabled;
403/404 surface as errors. No new REST was added; one missing companion was fixed
in sitemanage (unknown item NPE → real 404). WebUI + Vitest + Playwright +
product-docs ship in one PR.

Disclosure: author and reviewer are the same agent session; rigor applied as normal.

## Scope

- Base: origin/main (0cb7ac3064)
- Head: branch fix/issue-4581-publishing-actions (uncommitted)
- Files: 13 changed/added (5 modified, 8 new)
- Prior report: none for this slice
- Memory patterns hit: change-class closure; wrong-type test fakes (checked,
  clean); structural-only tests (checked, behavioral tests present)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion
- File: WebUI/src/main/ts/publishing/components/ItemPublishingActionsMenu.tsx
- Description: The `useEffect` reads `loadedFor` but lists only `[itemId]` in deps
  (with an eslint-disable). A parent re-render that re-sets the same itemId after
  an in-flight load could double-fetch. Idempotent GET, no user-visible defect.
- Suggestion: Include `loadedFor` in deps or use a ref guard if this ever grows
  beyond a read-only menu.
- Status: open

### Issue 2 -- Severity: suggestion
- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java
- Description: Malformed (non-GUID) ids still 500 via shared `PSItemSummaryService`
  (`NumberFormatException`), same as every other item endpoint (publish-now,
  takedown, stage). The slice's 404 covers well-formed unknown ids; the menu
  never sends malformed ids (client-side `mapIdParam` validation, like peers).
- Suggestion: Harden malformed-id handling across item endpoints as its own slice
  if desired; out of scope here.
- Status: open (accepted residual)

### Issue 3 -- Severity: nit
- File: projects/sitemanage/src/test/java/com/percussion/sitemanage/service/impl/PSSitePublishServiceGetPublishingActionsTest.java
- Description: Fully-qualified `jakarta.ws.rs.core.Response` used in two method
  bodies instead of an import.
- Suggestion: Import `jakarta.ws.rs.core.Response`.
- Status: open

## Checks applied

- Behavioral tests: new parse/map/error unit tests, API test (endpoint path +
  blank-id short-circuit + 403 propagation), component tests (disabled rows,
  unknown-name skip, empty, 403, 404, navigation), sitemanage service + adapter
  tests (NPE→404, 200 list entity). No structural-only tests.
- Change-class closure: WebUI + Vitest + surface Playwright + product-docs in one
  PR; sitemanage companion (null guard + 404 Response) with tests; no new rest
  adaptor surface so no Spring stub needed; no public API signature moved
  (service interface unchanged; only the JAX-RS adapter method return widened to
  `Response`, container-called only).
- Shared exception-mapper trap verified by test, not assumed: an explicit
  `WebApplicationException(404)` is flattened to 500 by
  `PSRuntimeExceptionMapper` (existing tests assert WAE→500), so the adapter
  returns a `Response` instead. The shared mapper was deliberately left untouched.
- Cross-platform path review: no issues. Diff touches no filesystem paths; `/`
  appears only in CMS item paths, REST URLs, and JAX-RS paths (correct).
- Secrets: none in diff. QA admin password stayed in shell env.
- Copyright: new files carry 2026 Intersoft headers; legacy Percussion headers untouched.

## Verification (pre-PR gates)

- `cd WebUI && ../mvnw clean install` → BUILD SUCCESS; Surefire 69/0; Vitest 471 files / 4528 tests.
- `cd rest && ../mvnw clean install` → BUILD SUCCESS (1434/0; reinstalled to repair
  ~/.m2 pollution from sibling branch — documented in PR).
- `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS; 2789/0.
- `cd modules/perc-qa-automation && ../../mvnw clean install` → BUILD SUCCESS.
- QA H2: qa-up OK, qa-deploy-webui OK, bundle chunk verified in cell, jars deployed,
  Jetty restarted in-cell, fresh boot `Initialization completed`.
- Surface Playwright (live cell): itemPublishingActions 3 passed; itemStage 3 passed;
  itemPublishNow 3 passed; itemTakedown 2 passed. Zero JS pageerrors.
- server.log ERROR/FATAL frozen across the final 11-test run (real 404s log nothing).
