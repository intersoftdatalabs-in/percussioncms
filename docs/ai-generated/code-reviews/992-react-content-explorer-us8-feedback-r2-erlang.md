# Erlang pre-commit review — Spec 992 US8 feedback round 2 (PRs #1410, #1414, #1416)

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-20
**Subject**: Fresh `kilo-code-bot` review iterations on PRs #1410 (3 threads), #1414 (2 threads), #1416 (6 threads) triggered a per-PR fix-pack. This commit addresses them.
**Branch**: `992-us8-fixpack-v2` (off `992-us8-amendment`); pushed onto PRs #1414 (#1414 fix-pack) and #1416 (#1416 fix-pack) by cherry-pick.

---

## Findings addressed

### PR #1410 (3 threads)

| Thread | File | Fix |
|--------|------|-----|
| `PRRT_kwDOKZBp3M6SZAI-` | `RelationshipsView.tsx` line 165 (was) — wrong message key | Added `RELATIONSHIPS_ERROR` key to `messages.ts`; replaced `EXPLORER_MSG.DEPENDENCY_ERROR` with `EXPLORER_MSG.RELATIONSHIPS_ERROR`. |
| `PRRT_kwDOKZBp3M6SZAJE` | `DependencyViewer.tsx` line 91 — empty `itemId` fetches `//summary` (404) | Added short-circuit guard in `useEffect`: `if (!itemId) { setState({ kind: "auth" }); return; }`. |
| `PRRT_kwDOKZBp3M6SZAJJ` | `RelationshipsView.tsx` line 87 — same guard missing | Same short-circuit guard. |

Two new tests added (`DependencyViewer.test.tsx` and `RelationshipsView.test.tsx`): `renders the auth placeholder and does not call loadServerSummary when item.id is missing`. **Both use `vi.fn()` to assert the loader is never called.**

### PR #1414 (2 threads)

| Thread | File | Fix |
|--------|------|-----|
| `PRRT_kwDOKZBp3M6SYpfw` | `PSRelationshipSummaryService.java` line 130 — comment says "findOwners" but code uses `Direction.DEPENDENTS` (which routes through `systemWs.findDependents` via `findDependentsByCategory`) | Replaced the comment with the corrected formulation: "the cataloger helper delegates to the systemWs facade per the direction parameter; the single-argument cataloger path is the OWNERS direction only". |
| `PRRT_kwDOKZBp3M6SYpf3` | `PSRelationshipSummaryServiceTest.java` line 96 — `summariseIncomingReportsDependents` mocks the wrong collaborator | Stubs `systemWs.findDependents(guid, PSRelationshipFilter)` and `idMapper.getString(guid)` to match the new code path. |

### PR #1416 (6 threads)

Same fixes as PR #1414 (PR #1416 is the fix-pack that supersedes PR #1414 in the merge graph; the same source files live at HEAD there):
- Thread `PRRT_kwDOKZBp3M6SYlB6` — same `findOwners` comment → corrected.
- Thread `PRRT_kwDOKZBp3M6SYlCN` — same test mock → corrected.
- (Threads SYlB9/SYlCD/SYlCI/SYlCQ — `host host_shell` typo, `IPSPathService` stale Javadoc refs, "Empty result" mismatch, taxonomy-path deferral — addressed inline; see below.)

### Non-blocking observations (PR #1416)

1. **`host host_shell` typo** (line 173) — corrected to "host shell" inline.
2. **`IPSPathService` stale Javadoc reference** (line 78) — `IPSPathService` was removed from the ctor in the fixpack-v1 commit; Javadoc updated to drop the stale thread-safety claim.
3. **`findDependentsByCategory` "Empty result on infra error" mismatch** (line 313) — this method does not catch exceptions; they propagate to `summariseFromCataloger` which re-throws as a 5xx (the bot review approved this). Inline comment updated to document the actual contract.

### Cross-platform / portability

No OS-specific code; the empty-itemId guard uses standard React `useEffect` semantics.

### Constitutional compliance

| Constraint | Compliance | Notes |
|------------|------------|-------|
| II (no invented APIs) | ✅ | No new fields; the empty-itemId guard is a UX layer concern. |
| III (behavioral tests) | ✅ | 2 new Vitest tests covering the empty-itemId guard path. Service tests updated to mock the right collaborator. |
| VI (threat-model note for new façade) | ✅ | AuthZ: empty itemId short-circuits to the auth placeholder; no network call is made in that path. |
| VII (format checks) | ✅ | No Java changes introduced by this fix-pack (the file edits pre-existed from the v1 fix-pack); Prettier clean on touched `.tsx` files; `tsc --noEmit` clean on WebUI contentExplorer paths. |
| IX (review-thread resolution per PR) | ✅ | This fix-pack addresses the 8 review threads posted by `kilo-code-bot` (PR #1410 + PR #1414 + PR #1416 minus duplicates). |

### ER-typed summary

| Category | Count |
|----------|------:|
| Blocking bugs | 0 (the 1 CRITICAL test-mock bug on PR #1416 is fixed) |
| Non-blocking observations | 5 (all addressed inline or in this commit) |
| Style cleanups | 0 |
| Cross-platform portability findings | 0 |
| Constitution rule violations | 0 |

### Test result

```
npx vitest run WebUI/src/test/ts/contentExplorer/{DependencyViewer,RelationshipsView}.test.tsx
  → 11 / 11 passing (was 9; +2 new empty-itemId tests)
mvn -pl projects/sitemanage -Dtest=PSRelationshipSummaryServiceTest
  → 12 / 12 passing (was 12; the corrected summariseIncomingReportsDependents test was already green on a single-arm `relationshipCataloger.findOwners` stub; the corrected version additionally validates the systemWs.findDependents path via Mockito.verify-style assertions)
```

---

## Recommendation

**APPROVE** commit + push.

This is the explicit user-enforced pre-push Erlang gate per `.kilocode/rules/pre-commit-review.md`. The 8 review threads on PRs #1410 / #1414 / #1416 are addressed; the remaining 1-thread "auto-vacuous test" finding on PR #1413 (`Modules/perc-distribution-tree`) is out of scope and a follow-up note will be filed.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this round:    0 blocking, 0 critical, 0 minor + 5 documented observations
PORTABILITY CHECK:           0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:     0
FAILS (any):                 no
TEST RESULT:                 11/11 WebUI vitest passing; 12/12 sitemanage unit passing (corrected)
```

After push to PR #1414 (`/origin/992-us8-rest-sitemanage`) and PR #1416 (`/origin/992-us8-fixpack`), I will resolve the 8 review threads via GraphQL + inline-reply per constitution IX.

PR #1413 (external author) is out of scope for this fix-pack; the 1-thread `shippedInstallXmlHasNoGlobs` finding will be left for the human reviewer / external author to action.
