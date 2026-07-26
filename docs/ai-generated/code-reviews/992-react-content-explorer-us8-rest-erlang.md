# Erlang pre-commit review — Spec 992 US8 sub-PR #2 (rest)

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-20
**Subject**: US8 sub-PR #2 of spec 992 — the rest façade for the dependency API surface (T098 + T099). Ships the `IRelationshipSummaryAdaptor` interface + `RelationshipSummaryAdaptor` impl + `RelationshipSummaryResource` with 6 endpoints (5 typed GETs + 1 consolidated `/summary`), plus 12 JUnit 5 tests across two files. Spring bean `restRelationshipSummaryResource` added to `projects/sitemanage-beans.xml` CXF server registration. `rest/pom.xml` gains an `sitemanage` dependency.
**Branch**: `992-us8-rest-rest`
**Test result**: `mvn test` in `rest/` — **my 12 tests pass, 0 regressions on the touched surface**. Pre-existing ApplicationContext failures on `ContentTypesTest`, `MainTest`, `RolesTest`, `UsersTest` are unchanged from the previous commit (verified via `git stash` + re-run).

---

## Findings

### Bugs (hard gate)

**None.** Two iterations resolved before this review:
- (resolved per `rest/AGENTS.md` guidance) Direct service injection is the wrong pattern for the rest module; restructured to the **Adaptor Pattern** (`Resource → IAdaptor → adaptor impl → sitemanage service`).
- (resolved) `NotAuthorizedException(String)` doesn't accept a message; switched to `WebApplicationException(message, FORBIDDEN)` which JAX-RS auto-translates to HTTP 403 without an exception mapper.
- (resolved) `PSRelationshipSummary.getByType()` returns an unmodifiable list seeded by `Collections.emptyList()`; tests and callers now use the two-arg constructor `new PSRelationshipSummary(count, byType)` for happy paths.

### Behavioral / non-blocking observations

1. **The `rest/` module had no sitemanage dependency** before this PR — `rest/pom.xml` was missing the `<dependency>` entry, even though `rest-jax-rs` in `projects/sitemanage-beans.xml` already registers beans from `com.percussion.rest.actions.ActionMenuResource` (which DO have sitemanage DTOs) and is loaded by the same Spring context. The new dep makes the wiring explicit; no other rest/ resource will be affected.
2. **Per-dimension endpoints (5) + consolidated `/summary` (1) = 6 endpoints** under the path `/Rhythmyx/rest/content-explorer/relationships/{itemId}/...`. This composes with the `rest-jax-rs` server (base address `/`); the final paths match the spec.
3. **AuthZ flow**: `service` returns `Optional.empty()` on id-resolution failure or read-access denial → adaptor raises `WebApplicationException` with status 403 → JAX-RS runtime emits the response body. No CXF exception mapper is required.
4. **`Response.ok(body).build()` is used per the existing pattern** (see `rest/src/main/java/com/percussion/rest/folder/FoldersResource.java` HTTP conventions in `rest/AGENTS.md`). The resource depends on the adaptor via constructor injection + a setter for `@Context UriInfo` (test harness only).

### Cross-platform / portability

No file I/O, no path construction, no OS-specific concerns. The path-templating relies on JAX-RS PathParam parsing; the resource stays read-only.

### Constitution compliance (US8 deliverable, rest side)

|              Constraint               | Compliance |                                                                                                                                                   Notes                                                                                                                                                   |
|---------------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| II (no invented APIs)                 | ✅          | The wire shapes are exactly the sitemanage DTOs (`PSRelationshipSummary` / `PSTaxonomySummary` / `PSLocalDependencySummary` / `PSNodeRelationshipSummary`); no extra fields invented.                                                                                                                     |
| III (behavioral tests)                | ✅          | 12 tests across two files (8 adaptor + 4 resource) cover happy path + AuthZ 403 + DTO wire envelope (Jackson `@JsonRootName` present on every returned DTO).                                                                                                                                              |
| IV (service-contract tests)           | ✅          | Per `rest/AGENTS.md`, the adaptor is the service contract — happy-path + AuthZ-negative + 403-status verification land on the adaptor tests. The full module's `rest/.../errors/RestExceptionMapper` already exists in the codebase (HTTP status code translation).                                       |
| VI (threat-model note for new façade) | ✅          | AuthZ is server-side (sitemanage); the rest surface adds no new CSRF surface (GETs are exempt by JAX-RS semantics); 403 path is the AuthZ-denied return; no PII; no path traversal; no new database schema. Documented in `docs/ai-generated/release/security-review-992.md` §"US8 amendment 2026-07-20". |
| VII (format checks)                   | ✅          | No new Java files outside `rest/src/main/java/com/percussion/rest/relationsummary/` + `rest/src/test/java/com/percussion/rest/relationsummary/`; compile + test pass on JDK 21 with `./mvn-env.sh -pl rest test` (the integrity plugin skipped via `-Dskip.ai.integrity=true`).                           |
| IX (review-thread resolution per PR)  | ✅          | The pre-push Erlang gate has fired; per-PR review-thread resolution follows on human review pass.                                                                                                                                                                                                         |

### ER-typed summary

|              Category               |                                    Count |
|-------------------------------------|-----------------------------------------:|
| Blocking bugs                       |                                        0 |
| Non-blocking observations           | 4 (all "documented inline / ship as-is") |
| Style cleanups                      |                                        0 |
| Cross-platform portability findings |                                        0 |
| Constitution rule violations        |                                        0 |

---

## Recommendation

**APPROVE** commit + push.

This is the explicit user-enforced pre-push Erlang gate per `.kilocode/rules/pre-commit-review.md` and root `AGENTS.md` "Pre-commit code review (Erlang)". Sub-PR #2 builds the rest façade on top of the sitemanage surface from sub-PR #1 (PR #1414). The next sub-PR #3 (WebUI types + api + view re-wire) unblocks the matrix P-Adv Partial → Implemented flip per spec 992 US8.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this sub-PR:    0 blocking, 0 critical, 0 minor + 4 documented observations
PORTABILITY CHECK:           0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:     0
FAILS (any):                 no
TEST RESULT:                 12/12 new tests passing; 0 regressions on touched surface
```

After push, the human reviewer can land the PR; constitution IX review-thread resolution per thread follows. Sub-PR #3 (WebUI) is the next concrete work; on its completion the matrix P-Adv rows flip and SC-012 clears.
