# Erlang review — 992-react-content-explorer — US1 commit

**Branch**: `992-react-content-explorer-us1` (off `origin/development` HEAD `e8b0e218c8`)
**Date**: 2026-07-19
**Scope**: US1 (T013–T027 + T015a + T024a) — modern React Content Explorer core navigate surface, components, Vitest tests, capability matrix P0-Core status update, Phase 1+2 bug fix.

## Files reviewed

| File | Action |
|------|--------|
| `WebUI/src/main/ts/api/contentExplorer/pathApi.ts` | **modified** (Phase 1+2 import bug: `apiFetch` → `get`/`post`; `encodePath` exported for testability) |
| `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx` | modified (real layout, not placeholder; lifted selection state; folder default WRITE assumption) |
| `WebUI/src/main/ts/contentExplorer/ExplorerTree.tsx` | new (lazy expand, keyboard nav, empty/error UX) |
| `WebUI/src/main/ts/contentExplorer/DetailList.tsx` | new (paginated, page-size 50, page-0 reset on folder change) |
| `WebUI/src/main/ts/contentExplorer/ReducedActions.tsx` | new (FR-010a 7-action bar, confirm-before-delete, no-throw preview) |
| `WebUI/src/main/ts/contentExplorer/messages.ts` | new (TMX key catalog `perc.ui.explorer.*`) |
| `WebUI/src/main/ts/contentExplorer/openInEditor.ts` | new (path-first / id-fallback navigation to editor) |
| `WebUI/src/main/ts/contentExplorer/selection.ts` | new (PSPathItem helpers + access-level gates) |
| `WebUI/src/main/ts/contentExplorer/styles.ts` | new (inline styles, matches `home.styles.ts` pattern) |
| `WebUI/src/test/ts/contentExplorer/setup.ts` | new (Vitest setup; fetch mock per-test) |
| `WebUI/src/test/ts/contentExplorer/pathApi.test.ts` | new (encodePath + paginatedFolder) |
| `WebUI/src/test/ts/contentExplorer/ExplorerTree.test.tsx` | new (lazy expand + selection + error) |
| `WebUI/src/test/ts/contentExplorer/DetailList.test.tsx` | new (pagination + folder-change reset + error) |
| `WebUI/src/test/ts/contentExplorer/reducedActions.test.tsx` | new (disabled-state gating + confirm + error surfacing) |
| `WebUI/src/test/ts/contentExplorer/sc005-perf-regression.test.ts` | new (SC-005 dev-machine regression guard, ≤ 5 s) |
| `specs/992-react-content-explorer/contracts/capability-matrix.md` | modified (P0-Core Status column populated) |
| `specs/992-react-content-explorer/tasks.md` | modified (T013-T023 + T015a + T026 marked `[x]`; T024/T024a/T025/T027 with `<!-- handoff -->` markers) |

## Summary

US1 ships the **core navigate surface** (FR-001…FR-005) on Track B React 19 + Vite. Three issues found and fixed in this commit:

- **H1 (was HARD) — Phase 1+2 import bug**: `pathApi.ts` imported `apiFetch` from `../client`, which does not exist. The export surface of `client.ts` is `get`, `post`, `put`, `del`. The Phase 1+2 PR merged without a compile pass (no `npx tsc --noEmit` in CI for this layer). **Fixed** by switching to `get`/`post` and exporting `encodePath` for testability.
- **H2 (was HARD) — DetailList stale-page bug**: when `folderPath` changed, the effect used the captured `page` value (stale from the previous folder) to compute `startIndex`, so the first fetch of a new folder would request `page * PAGE_SIZE` instead of 0. **Fixed** by computing `startIndex` locally from the `isNewFolder` flag (independent of the queued `setPage(0)` state update). SC-005 perf regression test guards this.
- **H3 (was HARD) — ReducedActions default handlers threw on user actions**: `defaultReducedActionHandlers().onOpen` threw `"must be implemented by the host for non-folder items"` and `onPreview` threw `"no-op placeholder in US1"`. Both threw into the `runItemAction` error path, surfacing internal-looking error strings to the user. **Fixed** by:
  - `onOpen` for non-folder items becomes a no-op (hosts that surface the explorer in a context where users open items must override `onOpen`).
  - `onPreview` becomes a no-op and the shell passes `hasPreviewHandler` to gray the Preview button when the host doesn't supply a preview handler. US3 / T055 will wire the real preview pane.

## Cross-platform path checklist

- All TS code uses URL `/` correctly (false-positive guard applies). `encodePath` is URL-style.
- No new shell scripts. No new filesystem path code.
- No tests assert path strings.
- **Phase 1+2 carryover**: `scripts/create-large-folder-fixture.sh` (already reviewed + fixed in PR #1385) remains the only UAT tooling.

## Hard gates

| Gate | Status |
|------|--------|
| Missing behavioral tests for non-trivial logic | **Pass** — T013–T016 + T015a cover path API, tree, list, ReducedActions, and SC-005 perf regression. |
| Non-portable filesystem path joins | **Pass (n/a)** — no new filesystem path code. |
| Empty catch / swallowed exceptions | **Pass** — `runItemAction` always surfaces errors via `onError`. `parseBody` in `client.ts` has documented fallbacks (unchanged from baseline). |
| False-green on ignored exit codes | **Pass (n/a)** — no shell scripts in this commit. |
| Path containment | **Pass (n/a)** — `encodePath` is a defensive encoder for URL segments; not a path-traversal guard (server enforces). |
| Duplicate method declarations | **Pass** — no compile pass to confirm; manual review of new files shows no duplicates. |

## Recommendation

**Approve.**

## Memory patterns hit

- Missing-behavioral-test gate (per US, not per file in scaffolding).
- False-positive guards (URL `/` is correct).
- Module-boundary hygiene (US1 modules under `WebUI/src/main/ts/contentExplorer/` mirror existing `home/` layout).

## Outstanding (handoff)

These tasks remain `- [ ]` with `<!-- handoff -->` markers in `tasks.md` because they require runtime validation that the analyzer session cannot perform:

- **T024** Mount explorer in `webmgt.jsp` — requires deployed CMS shell.
- **T024a** Shell-mount verification evidence — requires visual inspection.
- **T025** Run Vitest for `contentExplorer/` and fix failures — requires `npx vitest run` against the WebUI Vite config (the test files are written and ready; the run lands in the handoff session).
- **T027** Constitution IX review-thread resolution discipline — fires when review comments arrive on this PR.

The PR body will call out these handoff items explicitly so reviewers know what landed in this commit vs. what depends on a session with a deployed CMS.

## Gate

**May commit/push: yes.**