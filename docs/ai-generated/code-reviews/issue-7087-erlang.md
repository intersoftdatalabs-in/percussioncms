## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

### Codex review

The publishing Playwright spec has a verified syntax error. The accessibility changes also undermine their intended loading announcements and browser focus transfer.

Full review comments:

- [P1] Close the existing test before declaring the new test — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-lane-c/modules/perc-qa-automation/frontend/tests/publishing/itemPublishNow.spec.js:281-284
  The new `test(...)` starts inside the preceding test callback, whose closing delimiters were removed. `node --check` reports `SyntaxError: Unexpected end of input`, so Playwright cannot collect this spec and none of its publishing regressions run. Restore the preceding test's assertions and closing delimiters before declaring the new sibling test.

- [P2] Capture focus ownership before disabling the submit button — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-lane-c/WebUI/src/main/ts/publishing/components/ItemPublishNowPanel.tsx:165-168
  When a keyboard user submits from the focused button, rendering `submitting=true` disables that button before this effect runs. Browsers can consequently move focus to `body`, making `focusInsideAtSubmit` false and preventing the promised result-focus transfer. The same ordering affects `ItemTakedownPanel`. Capture focus ownership synchronously in the submit/retry handler before setting submitting state; the jsdom tests do not reproduce browser focus loss on disabling.

- [P2] Keep the loading announcement outside the busy subtree — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-lane-c/WebUI/src/main/ts/publishing/components/ItemTakedownPanel.tsx:454-454
  During lookup or submission, the new loading `role="status"` is inside this `aria-busy=true` section. Assistive technology can defer descendant live-region updates until busy clears, but the loading element is removed in that same render, so users may never hear the in-progress announcement. This also affects the publish-now panel. Place the loading live region outside the busy subtree, or scope `aria-busy` to content that excludes the announcer.

### Mitigations for the Codex [P0]/[P1] findings (author)

- [P1] spec syntax (`itemPublishNow.spec.js`): restored the preceding test's closing assertions/delimiters; `node --check` passes on both publishing specs (and both specs collect green in the QA cell).
- [P2] focus capture ordering: `noteSubmitFocus()` now runs synchronously in `runBatch`/`onPublish`/`onTakeDown` before `setSubmitting(true)` disables the trigger; the completion effect only honours that recorded intent.
- [P2] live region inside `aria-busy`: `aria-busy` moved off the `<section>` onto an inner `item-*-busy` region that excludes the `role="status"` loading row (loading/error/success are direct section children outside the busy subtree). Vitest + Playwright assertions updated to the scoped region.
- Post-review delta (spec-only): hermetic route stubs for sibling-panel endpoints (`services/sites/Demo`, `publishingActions`, `pubhistory`) inside the two new Playwright tests so the console-clean assertion is not polluted by pre-existing fixture 404s; final Erlang re-run on this diff: 0 findings, approve.
