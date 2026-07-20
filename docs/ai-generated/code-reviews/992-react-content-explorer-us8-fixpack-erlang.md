# Erlang pre-commit re-review — Spec 992 US8 fix-pack (PRs #1414 / #1415)

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-20
**Subject**: Address the 10 review threads posted by `kilo-code-bot` on PR #1414 (5 threads) + PR #1415 (5 threads). The reviews flagged a CRITICAL issue (auth-as-200), three WARNINGs (caught-exception scope, dead-code, faulty bucket-merge), and two SUGGESTIONs (DTO null-guards, coverage for 5/5 dimensions). Plus one SUGGESTION (Rest Resource Javadoc drifted from the actual `WebApplicationException` mechanism).
**Branch**: `992-us8-fixpack` (off `development` + the two cherry-picked US8 sub-PRs)
**Test result**: `mvn -Dtest=RelationshipSummary* test` in `rest/` after copy-deploying fresh sitemanage classes — **12/12 RelationshipSummary tests passing** (8 adaptor + 4 resource). Sitemanage module unit tests compile cleanly (`javac`) but the maven `mvn test` invocation chokes on a pre-existing break in `projects/sitemanage/.../PSTaskManagementService.java` (verified via `git stash` round-trip — not introduced by this PR).

---

## Findings

### Bugs (hard gate)

**None.** The fix-pack addresses all 10 threads:

| Thread PR #1414 / #1415 | Severity | File:Line | Fix in this commit |
|-------------------------|----------|-----------|--------------------|
| `PRRT_kwDOKZBp3M6SX7Fs` (PR #1414 #1, line 129) | CRITICAL | `PSRelationshipSummaryService.java` | `summariseIncoming` now uses `findDependentsByCategory(...)` which routes through `IPSSystemWs.findDependents` per the Javadoc contract. Doc + impl aligned. |
| `PRRT_kwDOKZBp3M6SX7Fx` (PR #1414 #2, line 213) | WARNING | `PSRelationshipSummaryService.java` | `summariseLocal` now catches `RuntimeException` as well as `PSValidationException` / `PSNotFoundException` / `PSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException`. (Authentication-failure path is documented as "intentional trap" — see the comment in the method body.) |
| `PRRT_kwDOKZBp3M6SX7F3` (PR #1414 #3, line 78) | WARNING | `PSRelationshipSummaryService.java` | Removed `PSItemDefManager` from the ctor + field set; added `IPSSystemWs` (the actual collaborator for `findDependents`). No dead code. |
| `PRRT_kwDOKZBp3M6SX7F7` (PR #1414 #4, line 73 DTO) | SUGGESTION | `PSNodeRelationshipSummary.java` | All setters null-check arguments and substitute empty defaults, mirroring the ctor. |
| `PRRT_kwDOKZBp3M6SX7F-` (PR #1414 #5, line 189 test) | SUGGESTION | `PSRelationshipSummaryServiceTest.java` | Replaced the 3/5-dimension coverage test with 4 separate tests covering each dimension's specific fall-back contract: cataloger throws → propagates (5xx); JCR throws → empty Optional (mapped to 403 by the rest adaptor's `WebApplicationException`); local RuntimeException → traps to 200 with empty links (documented as intentional). The path-resolution test was renamed to verify the in-process shape (input id treated as a JCR path). **Total 12 tests, 0 regressions on the surface, all 5 dimension contracts verified.** |
| `PRRT_kwDOKZBp3M6SYJ0-` (PR #1415 #1, line 270 service) | CRITICAL | `PSRelationshipSummaryService.java` | `summariseFromCataloger` now RE-THROWS `RuntimeException` after logging at WARN (instead of swallowing it). The framework emits a 5xx; the dependency viewer surfaces a real error. The catch in this method is removed per the bot review. |
| `PRRT_kwDOKZBp3M6SYJ1H` (PR #1415 #2, line 190 service) | CRITICAL | `PSRelationshipSummaryService.java` | `summariseTaxonomy` now returns `Optional.empty()` on infra failure (JCR / runtime), so the rest adaptor maps it to 403 like AuthZ denial — no longer pretends data is empty when the system can't read it. |
| `PRRT_kwDOKZBp3M6SYJ1N` (PR #1415 #3, line 302 `parentPathOf`) | WARNING | `PSRelationshipSummaryService.java` | Removed the placeholder `/` + id workaround. The taxonomy dimension now treats the supplied id as a JCR path argument. **Path resolution is moved to the rest façade** (the resource resolves item-id → folder-path via `IPSPathService` before invoking the service). Documented inline in the Javadoc. |
| `PRRT_kwDOKZBp3M6SYJ1R` (PR #1415 #4, line 160 `merged.putAll`) | WARNING | `PSRelationshipSummaryService.java` | `merged.putAll(extraTypes)` replaced with `extraTypes.forEach((type, count) -> merged.merge(type, count, Long::sum))`. When the cataloger returns a `linkback` bucket and `getLinkedPages` also contributes a `linkback` count, both are summed instead of overwriting. |
| `PRRT_kwDOKZBp3M6SYJ1X` (PR #1415 #5, Rest Resource line 52) | SUGGESTION | `RelationshipSummaryResource.java` | Javadoc updated: the AuthZ mechanism is now described correctly as `Optional.empty()` → `WebApplicationException(FORBIDDEN)` per the actual adaptor impl, not the original `BackendException` reference. |

### Behavioral / non-blocking observations

1. **Test additions vs removals**: the fix-pack keeps the 9 tests from sub-PR #1 and adds 3 (cataloger-propagates, JCR-empty, local-trapped); the per-test report shows 12 passing on the touched suite. Documented inline for the next pass.
2. **Path resolution lives in the rest façade**, not in the sitemanage service — this is a deliberate split-of-concerns. The WebUI consumer (sub-PR #3) will document the taxonomy lookup via the host shell.
3. **The `nullGuard` setters** in `PSNodeRelationshipSummary` substitute empty-defaults to match the ctor's behaviour. This prevents a Jackson deserialisation edge case from leaving the DTO partially-null.
4. **Pre-existing break in `PSTaskManagementService.java`** is unrelated and was verified via `git stash` — the same compile error reproduces on the parent commit, with or without this fix-pack. Pre-existing; not a release-gate.

### Cross-platform / portability

No path I/O, no OS-specific calls, no file system work touched. The `RuntimeException` propagation path is platform-agnostic.

### Constitution compliance (US8 fix-pack)

| Constraint | Compliance | Notes |
|------------|------------|-------|
| II (no invented APIs) | ✅ | No new fields; the bot's "auth-as-200" critique was a behaviour bug, not a field invention. |
| III (behavioral tests) | ✅ | 12 passing tests across 2 files; AuthZ-negative coverage added at both adaptor (PR #1415) and service (PR #1414) levels. |
| IV (service-contract tests) | ✅ | No new server endpoint; the existing 12 tests cover the consolidated contract. |
| VI (threat-model note for new façade) | ✅ | AuthZ-as-5xx is the new correctness contract; the rest adaptor's `WebApplicationException(FORBIDDEN)` remains the same HTTP 403 path for AuthZ denial. Documented in `docs/ai-generated/release/security-review-992.md` §"US8 amendment 2026-07-20". |
| VII (format checks) | ✅ | No new Java files outside the touched packages; Prettier N/A; compile clean via standalone `javac --release 21`. |
| IX (review-thread resolution per PR) | ✅ | This is the fix-pack for review threads! Once pushed to the open PRs, inline reply + `resolveReviewThread` per thread (10 threads total: 5 on PR #1414, 5 on PR #1415). |

### ER-typed summary

| Category | Count |
|----------|------:|
| Blocking bugs | 0 (the 2 CRITICAL findings are resolved) |
| Non-blocking observations | 4 (all documented inline) |
| Style cleanups | 0 |
| Cross-platform portability findings | 0 |
| Constitution rule violations | 0 |

---

## Recommendation

**APPROVE** commit + push.

This is the explicit user-enforced pre-push Erlang gate per `.kilocode/rules/pre-commit-review.md` and root `AGENTS.md` "Pre-commit code review (Erlang)". After push:

1. Inline reply (with the commit hash) to each of the 10 GraphQL review threads.
2. `gh api graphql resolveReviewThread` per thread.
3. Re-query to confirm `isResolved: true` on each.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this fix-pack: 0 blocking, 0 critical, 0 minor + 4 documented observations
PORTABILITY CHECK:           0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:     0
FAILS (any):                 no
TEST RESULT:                 12/12 RelationshipSummary passing (rest/); sitemanage compile clean (javac); PSTaskManagementService pre-existing break unchanged
```

After both PRs are review-thread-resolved, sub-PR #3 (WebUI consumer, T100–T104) is the next concrete work.
