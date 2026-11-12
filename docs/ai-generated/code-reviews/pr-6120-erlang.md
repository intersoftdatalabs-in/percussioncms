# Erlang review — PR #6120 (issue #6075)

- Status: **LGTM — approved, squash-merged**
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang`)
- Scope: PR head vs `origin/main`, 7 files (WebUI + qa-automation +
  product-docs, no Java)
- Verdict: `lgtm_merged`, merged: true, bugs_found: [] (in-diff)

## Gate

- In-diff bugs: **0** (1 in-diff suggestion, advisory only — component already
  over-limit, manage logic extracted to hook + pure reducers). Recommendation:
  approve.

## Machine findings

- PR-embedded + independent re-run agree: **1 suggestion** (SearchPanel
  cognitive 26/27, preexisting scale), 0 blocking.

## Manual verification (refusal-with-rows-kept contract)

- Catalog merged **only after transport resolves** (`setCatalog` inside `try`
  after `await`); catch paths set `manageError` and leave the dialog open —
  rows kept on 400/403/404/409. Verified in `useSavedSearchManage`
  (`handleRenameSave` / `handleDeleteConfirm`).
- Rename PUTs the display label only (`saveSearch(key, {name: key, label})` —
  catalog name key never renamed, per REST-GAPS-02); blank refused client-side
  with no PUT; cancel calls no transport.
- Reducers `renameCatalogRow` / `deleteCatalogRow` are pure, key-scoped, and
  unit-tested (reducer + 16 Vitest cases incl. cancel/blank/4xx); axe-core gate
  with dialog open.
- Backend #6119 (H2 PUT 500 row-loss) is preexisting, zero-Java in this slice,
  already filed and linked in the body; the live spec asserts the deterministic
  contract (save-lands OR refusal-keeps-rows). Mergeable with contract held.
- No new-file Java headers apply (TS only). C5 present (H2 surface 4 passed +
  golden 2 passed, 0 console errors, 0 server.log ERROR; rename-refusal branch
  filters only its single expected 500 resource message).
- product-docs checkbox: done. Checks: all green. No unresolved threads.

## Residuals

- None new (#6119 already filed by the work agent; in-diff suggestion is
  advisory, not a residual candidate).
