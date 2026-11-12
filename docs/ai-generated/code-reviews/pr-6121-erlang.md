# Erlang review — PR #6121 (issue #6076)

- Status: **LGTM — approved, squash-merged**
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang`)
- Scope: PR head vs `origin/main`, 8 files (WebUI + qa-automation +
  product-docs, no Java)
- Verdict: `lgtm_merged`, merged: true, bugs_found: [] (in-diff)

## Gate

- In-diff bugs: **0** (machine: 0 findings). Recommendation: approve.

## Machine findings

- PR-embedded + independent re-run agree: **0 findings, 0 blocking bugs**.

## Manual verification

- `editorAutosave.ts` pure helpers (`writeAutosavedEditorDraft`,
  `clearAutosavedEditorDraft`, `autosavedDraftDiffers`,
  `shouldOfferAutosaveRestore`) unit-tested (11 new tests across 2 files);
  storage seam injectable, `browserAutosaveStorage` null-guards non-DOM.
- Writes are best-effort (quota/private-mode swallowed, never breaks editing);
  successful save clears the entry; View/Promote modes never show the notice
  (`role="status"`, informational).
- Restore fills the **local draft only, never a fields PUT** — a newer server
  save cannot be clobbered; only explicit Save writes. Staged binaries excluded
  (noted in code + docs).
- Key `perc-editor-autosave-<contentId>` content-scoped; revision carried for
  future staleness use without changing the no-clobber guarantee.
- C1 evidence present (surefire 71/0, vitest 896 files / 8587 tests, no new
  warnings); C5 present (H2 surface 3 passed incl. restore-without-PUT-then-save,
  0 console/page errors, no new server.log FATAL — only preexisting fake-id-42
  stub noise). Overlap with #6112 is disjoint regions; branched from current main.
- product-docs checkbox: done (row next to "Revert unsaved edits").
- Checks: all green. No unresolved review threads.

## Residuals

- None.
