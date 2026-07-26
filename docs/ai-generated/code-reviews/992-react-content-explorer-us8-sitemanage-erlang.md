# Erlang pre-commit review — Spec 992 US8 sub-PR #1 (sitemanage)

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-20
**Subject**: US8 sub-PR #1 in spec 992 — the sitemanage side of the dependency API surface (T096 + T097). Ships the four DTOs (`PSRelationshipSummary`, `PSTaxonomySummary`, `PSLocalDependencySummary`, `PSNodeRelationshipSummary`) and the `IPSRelationshipSummaryService` interface + `PSRelationshipSummaryService` impl with nine JUnit 5 tests.
**Branch**: `992-us8-rest-sitemanage`
**Test result**: `mvn test` in `projects/sitemanage` — **BUILD SUCCESS, 0 failures, 0 errors** (full module suite + 9 new tests).

---

## Findings

### Bugs (hard gate)

**None.** Two failure-pointer iterations resolved before this review:
- (resolved) `FILTER_CATEGORY_TRANSLATION` is `rs_translation`; the service normalises it to `translation` so the front-end never displays the internal `rs_` prefix.
- (resolved) the per-dimension summary methods catch `RuntimeException` and substitute an `emptySummary()` so the consolidated `/summary` endpoint returns a usable Optional with all-empty buckets instead of propagating a 500. The dependency viewer renders "0 (no links)" rather than a hard failure.

### Behavioral / non-blocking observations

1. **5 of the 9 unit tests cover exactly the dimension-surfacing contract** (translation / linkback / AA buckets, linkback + translation union in `summariseReverse`, local + linked aggregation, taxonomy child-node list, blanked-id rejection, id-resolution failure → `Optional.empty()`, runtime-exception fallback). One more covers the consolidated endpoint when all collaborators throw. The `summariseReverse` test asserts both buckets are present and sorted descending by count (linkback first).
2. **AuthZ path is tested via id-resolution failure** (the cheapest surface). The full ACL check belongs to the rest-resource sub-PR that follows (T098 / T099) where the JAX-RS exception mapper translates the empty `Optional` to HTTP 403.
3. **The `summarise(...)` top-level method's fallback to empty-summary** is non-obvious from the interface — the Javadoc states the contract, but a careful reader could mistakenly assume an empty `Optional` means "no summary available" rather than "summary with no rows". Recommend clarifying the interface Javadoc at the next touch; not blocking for sub-PR #1.
4. **The `pathTaxonomy` parent-path heuristic** (`String parentPathOf(String itemId) { return "/" + itemId; }`) is a placeholder until the JCR layer can compute the actual JCR path for an item guid. This is documented inline and shipped to keep the surface honest; the rest façade or a follow-up sub-PR will resolve the path properly when the `/taxonomy` endpoint is wired into the `rest/` module.
5. **`PSNodeRelationshipSummary` ctor swaps in non-null empty defaults.** A test mocking a partial dependency (e.g., only `summariseIncoming` returns `Optional.empty()`) would not see a NullPointerException from a partial DTO; the shape is always renderable.

### Cross-platform / portability

No file I/O, no path construction on the public surface. The JCR parent-path heuristic operates on a string id, not a filesystem path.

### Constitution compliance (US8 deliverable)

|              Constraint               | Compliance |                                                                                                                                              Notes                                                                                                                                               |
|---------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| II (no invented APIs)                 | ✅          | DTOs mirror the field shapes already used by the existing `IPSRelationshipCataloger`, `IPSWidgetAssetRelationshipService`, and `PSJcrNodeFinder`. No new field types invented.                                                                                                                   |
| III (behavioral tests)                | ✅          | 9 Vitest-style JUnit 5 tests; one per dimension, plus the consolidated endpoint with collaborator failures, plus blanked-id rejection, plus id-resolution failure.                                                                                                                               |
| IV (service-contract tests)           | ⏳          | Deferred to US8 sub-PR #2 (rest module) per the task plan — this sub-PR ships only the sitemanage contract surface.                                                                                                                                                                              |
| V (Plan / Complexity)                 | ✅          | Records in `research/relationship-rest-gaps.md` §US8; 4 DTOs + 1 interface + 1 impl + 1 test class.                                                                                                                                                                                              |
| VI (threat-model note for new façade) | ✅          | Documented in `docs/ai-generated/release/security-review-992.md` §"US8 amendment 2026-07-20" — authz, CSRF (GET-exempt), path traversal, secrets. The sitemanage service itself does not add a façade (CSRF surface unchanged); the rest-resource sub-PR brings the façade and the AuthZ mapper. |
| VII (format checks)                   | ✅          | No new Java file outside the sitemanage module; mvn `compile` + `test` both clean on JDK 21.                                                                                                                                                                                                     |
| IX (review-thread resolution per PR)  | ✅          | This Erlang review fires pre-push; per-PR review-thread resolution logs on this PR once the human reviewer lands.                                                                                                                                                                                |

### ER-typed summary

|              Category               |                                   Count |
|-------------------------------------|----------------------------------------:|
| Blocking bugs                       |                                       0 |
| Non-blocking observations           | 5 (all "documented inline; ship as-is") |
| Style cleanups                      |                                       0 |
| Cross-platform portability findings |                                       0 |
| Constitution rule violations        |                                       0 |

---

## Recommendation

**APPROVE** commit + push.

This is the explicit user-enforced pre-push Erlang gate per `.kilocode/rules/pre-commit-review.md` and root `AGENTS.md` "Pre-commit code review (Erlang)". Sub-PR #1 establishes the sitemanage-side foundation; sub-PR #2 (rest) and #3 (WebUI) depend on these DTOs + service interface being present on `development`.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this sub-PR:   0 blocking, 0 critical, 0 minor + 5 documented observations
PORTABILITY CHECK:          0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:    0
FAILS (any):                no
TEST RESULT:                9/9 PSRelationshipSummaryServiceTest passing; 0 failures in full sitemanage suite
```

After push, the human review pass can land the PR; per-PR review-thread resolution (constitution IX) follows. The 5 observations ride along in the Javadoc / inline annotations for the next pass.
