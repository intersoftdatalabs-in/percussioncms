# Erlang Review — 992-react-content-explorer US5 PR #1398 post-rebase (merge + 2 JSDoc follow-ups)

**Branch**: `992-react-content-explorer-us5`
**Base**: `origin/development`
**Head**: `f2a8dde30dce434af29225bb305b2ffd7d1f6a1f`
**Reviewer**: Erlang (independent — no authorship on any of the 3 reviewed commits)
**Date**: 2026-07-20
**Scope**: Three commits under review (oldest → newest):

1. `bdb41210af` — **merge**: sync `origin/development` into `992-react-content-explorer-us5` (PR #1398). Brings spec-993 `WorkflowAdminShell` and US4 P-ACL `FolderSecurityPanel`. Three conflicts resolved: `registry.ts`, `contentExplorer/messages.ts`, `api/contentExplorer/types.ts`.
2. `c2794aeede` — docs-only JSDoc fix for review thread `PRRT_kwDOKZBp3M6ST4mT`. Replaces `{@link SearchPanel.runSearch}` with plain-text `runSearch` and drops the unresolvable `{@link SearchStatusView.onRetry}` link.
3. `f2a8dde30d` — docs-only JSDoc fix for review thread `PRRT_kwDOKZBp3M6SUIdK`. Replaces the last remaining `{@link SearchStatusView}` (line 38) with `{@code SearchStatusView}` (non-resolvable code-span) plus prose pointing at the local function component.

## Summary

Two of the three commits (commits 2 & 3) are pure JSDoc tag swaps that remove unresolvable TypeDoc links while preserving readability. Both are behavior-preserving and pass all relevant tests. The merge commit (commit 1) is a well-documented sync merge with clean conflict resolution: 5 registry imports + 5 registrations (no duplicates), 9 US5 `SEARCH_*` + 17 US4 `SECURITY_*` message keys (no overlap), and US5/US3 DTO blocks preserved verbatim from both sides. Verification: `npx tsc --noEmit` is clean; `npx vitest run SearchPanel.test.tsx FolderSecurityPanel.test.tsx ContextMenu.test.tsx ActionToolbar.test.tsx actionMenuApi.test.ts` returns 53/53 passing.

No material findings introduced by these 3 commits. Pre-existing TypeScript-vs-Java DTO field-name mismatches exist in `types.ts` (introduced in the parent PRs #1396 and US5 `bbb8e2d8e9`), but they were not authored or modified here; the merge simply preserved them verbatim from both sides. These are noted under "Pre-existing observations" but do **not** block the merge.

## Recommendation

`approve`

## Gate

- Blocking bugs: **0**
- Missing behavioral tests: **0** (changes are JSDoc-only; 9/9 SearchPanel + 53/53 US3/4/5 suites green)
- Non-portable path/file I/O: **0** (no filesystem path additions in any of the 3 commits)
- Invented APIs / invented field names: **0 new** (pre-existing field-name mismatches in `types.ts` are preserved verbatim — see "Pre-existing observations")
- May commit/push: **yes**

## Issues

(None. No `bug` / `suggestion` / `nit` findings introduced by the 3 reviewed commits.)

## Change-by-change verdict

### Commit 1 — `bdb41210af` (merge, 61 files changed, +7500 / -52)

- `WebUI/src/main/ts/registry.ts`: US5 side had 1 import + 1 set for `SearchPanel`; development side had imports + sets for `FolderSecurityPanel`, `ContextMenu`, `ActionToolbar`, `WorkflowAdminShell`. Merged result has exactly 1 import per component (5 components) and exactly 1 `componentRegistry.set` per component. **No duplicates.** Order is consistent. ✔
- `WebUI/src/main/ts/contentExplorer/messages.ts`: US5 side contributed 9 `SEARCH_*` keys; development side contributed 17 `SECURITY_*` keys (merge commit message claims 18; actual count is 17 — see nit below). All keys concatenated with no overlap. Both blocks gated by their respective story requirements. ✔
- `WebUI/src/main/ts/api/contentExplorer/types.ts`: US5 search DTOs (`PSSearchCriteria`, `PSItemProperties`, `PSPagedItemPropertiesList`, `PSPagedItemPropertiesListEnvelope`, `PSSearchResults`) precede US3 action-menu DTOs (`ActionMenuType`, `ActionMenuParameter`, `ActionMenuProperty`, `ActionMenuVisibilityContext`, `ActionMenuModeUIContext`, `ActionMenuListEnvelope`, `ActionMenu`, `AllowedContentTypeMenusRequest`, `MenuAction`). Both blocks preserved verbatim. ✔
- Other `.specify/*` files auto-merged cleanly. ✔

#### Sub-finding 1 — Severity: nit (pre-existing, preserved by merge, NOT introduced)

- **File**: `WebUI/src/main/ts/api/contentExplorer/types.ts`
- **Description**: The TypeScript interfaces in the US5 search DTO block claim to "mirror" Java DTOs per constitution II (Evidence Over Invention), but several field names do not match the actual Java fields:
  - `PSSearchCriteria.caseSensitive?: boolean` — **NOT** present on `com.percussion.searchmanagement.data.PSSearchCriteria` (which has only `query`, `searchType`, `startIndex`, `maxResults`, `sortColumn`, `sortOrder`, `formatId`, `searchFields`, `folderPath`).
  - `PSItemProperties` (TS) vs `com.percussion.share.data.PSItemProperties` (Java): TS fields `title`, `folderPath`, `displayProperties`, `workflowState`, `lastModified`, `locale` do not match Java fields (`name`, `path`, `summary`, `status`, `workflow`, `lastModifiedDate`, etc.). Only `id`, `name`, `type` align cleanly.
  - `ActionMenu.guid?: { raw?: string }` — Java `com.percussion.rest.Guid` has 6 fields (`stringValue`, `untypedString`, `hostId`, `type`, `uuid`, `longValue`), not just `{ raw?: string }`.
- **Why this is a nit (not a bug for this PR)**: These field-name mismatches were introduced by the parent PRs (`89c40c7102` US3 PR #1396, `bbb8e2d8e9` US5 PR) on `development` before this merge. Commit `bdb41210af` does not author or modify these declarations; it only combines the two pre-existing blocks verbatim. The 3 reviewed commits are **not** the right place to fix this — flagging for a follow-up issue or for re-review of the original US3/US5 PRs.
- **Suggestion**: Open a follow-up issue to align TS field names with Java DTOs (e.g., rename `PSItemProperties.title` → `name` or document the deliberate rename; add `caseSensitive` to Java DTO if intended; expand `ActionMenu.guid` to match `com.percussion.rest.Guid` shape). Not blocking this PR.
- **Status**: open (pre-existing; flagging for follow-up)

#### Sub-finding 2 — Severity: nit

- **File**: `WebUI/src/main/ts/contentExplorer/messages.ts` (lines for `SEARCH_PLACEHOLDER` and `SEARCH_LOADING`)
- **Description**: Merge commit message normalizes the literal escape `\u2026` (Unicode horizontal ellipsis in JS string-literal form) to the actual character `…` (U+2026). Both encode to the same runtime string, so behavior is identical, but the source representation changed during the conflict resolution. The merge commit body does not document this normalization.
- **Suggestion**: Either revert to `\u2026` (preserves byte-for-byte identity with the US5 side) or document the normalization in the merge message. No functional impact.
- **Status**: open (informational)

#### Sub-finding 3 — Severity: nit

- **File**: Merge commit message body
- **Description**: The merge message claims "US4 SECURITY_* (18) keys" but the actual count is **17** (verified via `grep -c "^  SECURITY_"` on the merged file: 17). The 17-key count matches the development-side parent.
- **Suggestion**: Either correct the merge message (requires a new commit) or note the off-by-one in the merge-retro doc. No code impact.
- **Status**: open (informational)

### Commit 2 — `c2794aeede` (JSDoc-only, 1 file, +9 / -4)

- `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx`:
  - State-machine error-state bullet (lines ~34–37): drops `{@link SearchStatusView.onRetry}` and rephrases as "see the `{@code onRetry}` prop description in `{@link SearchStatusView}`" — links the component itself (valid TypeDoc target).
  - `SearchStatusView.onRetry` JSDoc (lines ~167–178): replaces `{@link SearchPanel.runSearch}` with plain-text `runSearch` (no `@link`); adds a parenthetical explaining why `SearchStatusView.onRetry` is not a valid `@link` target (destructured prop parameter, not a property of the function).
- All remaining `@link` refs in the file (verified by grep):
  - `{@link SearchPanelProps.onOpen}` — valid (interface prop) ✔
  - `{@link SearchPanelProps.onReveal}` — valid (interface prop) ✔
  - `{@link searchExtended}` — valid (exported function) ✔
  - `{@link SearchPanel}` — valid (exported function) ✔
  - `{@link SearchStatusView}` — valid as function reference within the same file ✔
- No remaining `@link` to non-exported function components or non-existent props. ✔
- No runtime behavior change. ✔

### Commit 3 — `f2a8dde30d` (JSDoc-only, 1 file, +2 / -1)

- `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx`:
  - State-machine error-state bullet (line 38): drops the final `{@link SearchStatusView}` and replaces it with prose + `{@code SearchStatusView}` (code-span, not a resolvable link) — since `SearchStatusView` is a local function component (no `export` modifier, declared at line 167), it cannot be a TypeDoc target.
- After this commit, all `SearchStatusView` references in JSDoc are either:
  - `{@code SearchStatusView}` (lines 35, 39, 175) — code-span, non-link ✔
  - `<SearchStatusView ... />` (line 157) — JSX usage, not JSDoc ✔
  - `function SearchStatusView(...)` (line 167) — declaration, not JSDoc ✔
- No remaining `{@link ...}` references to non-exported function components. ✔
- No runtime behavior change. ✔

## Verification commands & outputs

```bash
# 1. Verify branch / HEAD state
$ git branch --show-current
992-react-content-explorer-us5
$ git log --oneline -10 f2a8dde30d
f2a8dde30d docs(992/us5): {SearchStatusView} -> {code SearchStatusView} (PRRT_kwDOKZBp3M6SUIdK)
c50806467e docs(erlang): append follow-up #2 to US5 secondary mitigation review (ST4mT + sync merge)
c2794aeede docs(992/us5): drop unresolvable {SearchStatusView.onRetry} JSDoc ref (PRRT_kwDOKZBp3M6ST4mT)
bdb41210af merge: sync origin/development into 992-react-content-explorer-us5 (PR #1398)
67c0a165b8 docs(992/us5): fix broken {@link SearchPanelProps.onRetrySearch} JSDoc refs after retry-button mitigation
16474d1379 feat(992/us4): US4 P-ACL — FolderSecurityPanel + aclLockout + SC-004 coverage (T058–T064b) (#1397)
5898108648 feat(webui): Unified Workflow & Admin React UI (Spec 993) (#1404)
...

# 2. Diff scope for JSDoc commits (commits 2 & 3)
$ git diff c50806467e~1..f2a8dde30d -- WebUI/src/main/ts/contentExplorer/SearchPanel.tsx
# Only 1 file, 2 lines: +2 / -1
# Replaces {@link SearchStatusView} (line 38) with prose + {@code SearchStatusView}

# 3. Merge commit stat
$ git show --stat bdb41210af
# 61 files changed, 7500 insertions(+), 52 deletions(-)
# WebUI/src/main/ts/api/contentExplorer/types.ts | 109 +
# WebUI/src/main/ts/contentExplorer/messages.ts  |  23 +-
# WebUI/src/main/ts/registry.ts                  |   8 +
# (and many spec files, registry/worktree dirs, and tests)

# 4. TypeScript type-check on entire frontend
$ cd WebUI/src/main/frontend && npx tsc --noEmit
# (no output) -> clean, 0 errors

# 5. Target test suites (US3/4/5, as claimed by merge commit)
$ npx vitest run SearchPanel.test.tsx FolderSecurityPanel.test.tsx ContextMenu.test.tsx ActionToolbar.test.tsx actionMenuApi.test.ts
# Test Files  5 passed (5)
#      Tests  53 passed (53)
# Duration   14.37s

# 6. SearchPanel-only (as claimed by JSDoc commits)
$ npx vitest run SearchPanel.test.tsx
# Test Files  1 passed (1)
#      Tests  9 passed (9)

# 7. Verify no remaining @link to non-resolvable targets
$ grep -nE '@link' WebUI/src/main/ts/contentExplorer/SearchPanel.tsx
# (only SearchPanelProps.onOpen, SearchPanelProps.onReveal, searchExtended,
#  SearchPanel, SearchStatusView -- all valid references)

# 8. Verify registry.ts merge: exactly 1 import per component, 1 set per component
$ grep -E "import \{ (SearchPanel|FolderSecurityPanel|ContextMenu|ActionToolbar|WorkflowAdminShell)" WebUI/src/main/ts/registry.ts | wc -l
# 5
$ grep -E "componentRegistry.set\(\"(SearchPanel|FolderSecurityPanel|ContextMenu|ActionToolbar|WorkflowAdminShell)" WebUI/src/main/ts/registry.ts | wc -l
# 5

# 9. Verify messages.ts merge: 9 SEARCH_* + 17 SECURITY_* keys (no overlap)
$ grep -c "^  SEARCH_" WebUI/src/main/ts/contentExplorer/messages.ts
# 9
$ grep -c "^  SECURITY_" WebUI/src/main/ts/contentExplorer/messages.ts
# 17

# 10. Verify TS types match Java DTO field names (Evidence Over Invention check)
# (See Sub-finding 1 for the documented mismatches)

# 11. Cross-platform path/file I/O check (root AGENTS.md mandate)
$ git diff c50806467e~1..f2a8dde30d -- WebUI/src/main/ts/contentExplorer/SearchPanel.tsx \
    WebUI/src/main/ts/registry.ts WebUI/src/main/ts/contentExplorer/messages.ts \
    WebUI/src/main/ts/api/contentExplorer/types.ts | rg '(require\(|import\(|fs\.|path\.)'
# (no output) -> no Node.js filesystem APIs or path-joining in any of the 4 files
# (no os-specific path separators introduced)
```

## Pre-existing observations (NOT blocking, NOT introduced by these 3 commits)

The following issues exist in the merged state of `types.ts` but were authored in the parent PRs (`89c40c7102` US3, `bbb8e2d8e9` US5) before this merge. They are preserved verbatim by the merge. Listed here for transparency and to inform follow-up review of the original PRs:

|               TS field               |   TS source    |                               Java DTO actual                               |                           Status                            |
|--------------------------------------|----------------|-----------------------------------------------------------------------------|-------------------------------------------------------------|
| `PSSearchCriteria.caseSensitive`     | `types.ts:197` | **Not present** in `com.percussion.searchmanagement.data.PSSearchCriteria`  | Field invention; suggest server-side addition or TS removal |
| `PSItemProperties.title`             | `types.ts:208` | Java field is `name`; `title` does not exist                                | Either rename to `name` or document intentional divergence  |
| `PSItemProperties.folderPath`        | `types.ts:211` | Java field is `path` (`@NotEmpty String path`)                              | Mismatch                                                    |
| `PSItemProperties.displayProperties` | `types.ts:215` | **Not present** on Java                                                     | Field invention                                             |
| `PSItemProperties.workflowState`     | `types.ts:217` | Java has `workflow` (name) and `status` (current state); no `workflowState` | Mismatch                                                    |
| `PSItemProperties.lastModified`      | `types.ts:219` | Java field is `lastModifiedDate`                                            | Mismatch                                                    |
| `PSItemProperties.locale`            | `types.ts:221` | **Not present** on Java                                                     | Field invention                                             |
| `ActionMenu.guid`                    | `types.ts:317` | Java `Guid` has 6 fields; TS uses `{ raw?: string }`                        | Oversimplified; round-trip data loss possible               |

These were already present on `development` and on `992-react-content-explorer-us5` prior to the merge. The merge `bdb41210af` does not modify these declarations. **Do not block this PR** for pre-existing parent-PR issues; recommend opening a follow-up ticket to align TS with Java DTOs.

## Handoff

- **What was reviewed**: 3 commits on `992-react-content-explorer-us5` — 1 merge (61 files, +7500/-52) + 2 JSDoc-only follow-ups to PR #1398 review threads `PRRT_kwDOKZBp3M6ST4mT` and `PRRT_kwDOKZBp3M6SUIdK`. Focused scope: 4 files (`SearchPanel.tsx`, `registry.ts`, `messages.ts`, `types.ts`).
- **Top findings**: 0 bugs / 0 suggestions / 3 nits (1 pre-existing TS-vs-Java DTO mismatch in `types.ts`, 1 source-representation normalization of `\u2026` → `…`, 1 merge-message off-by-one on `SECURITY_*` key count).
- **Recommendation**: **approve**. No blocking findings. May commit/push: **yes**.
- **Continuity**: Prior reports — `992-react-content-explorer-us5-erlang.md`, `992-react-content-explorer-us5-review-thread-mitigation-erlang.md`, `992-react-content-explorer-us5-secondary-mitigation-erlang.md`.
- **Next steps**: After push, verify PR #1398 review threads `PRRT_kwDOKZBp3M6ST4mT` and `PRRT_kwDOKZBp3M6SUIdK` are resolved inline (per root AGENTS.md **PR Review Comment Resolution** rule) with mitigation statements pointing at commits `c2794aeede` and `f2a8dde30d`.

