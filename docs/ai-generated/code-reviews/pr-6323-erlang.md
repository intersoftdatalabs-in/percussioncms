# Erlang review — PR #6323 (issue #6067): unassign one slot from a template region with confirm

Independent reviewer: **Erlang Shen** (third eye; did not author the change).
Persona: `erlang 0.1.1` (`~/.agents/skills/erlang/`).
Engine: `mkd-code-review 0.4.0` — `--pack percussion --format markdown --gate advisory --git-base origin/main`
with `--models config/models.ollama-dev-coder.toml` (Ollama `dev-coder:latest`, `localhost:11434`).
LLM residual stage: `outcome=completed findings=0`.
Mode: in-diff bugs gate the merge; preexisting rows never block (recorded as residuals instead).

## Verdict

**LGTM — APPROVE.** 0 in-diff findings, 0 blocking bugs. One preexisting
complexity suggestion in a touched file (out of scope here; filed as a
tech-debt residual, never a blocker). CI is green. May merge.

Manual read-through (independent of the machine report):

- `TemplateDetailPanel.tsx`: the unassign flow is a clean extraction, not a
  bolt-on. `slotPayloadFor()` / `templateSavePayload()` / `applySavedTemplate()`
  are factored out of `handleSave` and reused by `handleUnassignConfirm`, so
  the save path actually *loses* ~60 duplicated lines. Guard order is right:
  lock → clear stale messages → stage `pendingUnassign`; confirm re-checks
  lock/detail/key, bails on any inflight edit, no-ops when the key is already
  gone, and on `400/403/409` closes the dialog with the error surfaced while
  the slot mapping stays rendered (slots untouched in catch).
- State-staleness check (reviewer-traced, not a finding): the `handleSave`
  catch restores `initialSlotKeys`, which is derived from `detail.slots` —
  and `applySavedTemplate` calls `setDetail(saved)` on every success — so the
  restore target is always the last-*saved* server state, never a stale
  load-time snapshot. A failed save after an earlier successful unassign
  restores the post-unassign membership. Correct.
- `CatalogConfirmDialog` reuse (existing component), `data-testid` per slot
  key, `aria-label` on the unassign button, confirm message strips the
  `name:`/`guid:` key prefix for humans. `pendingUnassign` clears on both
  success and failure; `cancelUnassign` writes nothing while busy-guard holds.
- `messages.ts`: new keys only; unit test + Playwright spec + product-docs
  (`admin/developer-templates.md`) + 2 proof PNGs in the same PR — full
  change-class closure for a WebUI screen (Vitest 8841 per body, H2 1+2 with
  proofs). Copyright headers on new files correct (2026 Intersoft).
- Cross-platform: no filesystem path I/O in the diff (URL/route strings only);
  Node `path.join` used in the spec artifact write. Checklist: pass.

## Residual (out of scope, filed separately)

- Preexisting `complexity.cognitive` suggestion —
  `WebUI/src/main/ts/developer/TemplateDetailPanel.tsx:729`
  `handleDescriptionSave` (cognitive 9, cyclomatic 18 > max 15), untouched by
  this diff → tech-debt residual (p8), link recorded in
  `scratch/erlang-this-run.json`. Not a merge blocker.

## Full machine report

```text
## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/TemplateDetailPanel.tsx:729 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDescriptionSave` cognitive=9 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
```
