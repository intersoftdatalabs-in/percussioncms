<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4827 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--diff` of uncommitted 4827 files
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4827
- Branch: `fix/issue-4827-history-contrast` (HEAD == `origin/main` `6c0a958036`; all work is uncommitted)
- Files reviewed:
  - `WebUI/src/main/ts/publishing/publishing.styles.ts` (modified)
  - `WebUI/src/test/ts/publishing/publishing.styles.test.ts` (modified)
- `--git-base origin/main` is empty here (no commits vs main). Machine pass used a unified diff with `a/` `b/` prefixes so both tracked files were in scope.
- Memory: `~/.agents/skills/erlang/PATTERNS.md` and repo `erlang-review/patterns.md`
- Prior report: none for this ticket
- Cross-platform path review: no filesystem path I/O in this slice

## This-diff behavior

Cycle-verify Playwright `tests/explorer-action-dispatch.spec.js` failed axe `color-contrast` (serious) on `button[type="submit"][data-testid="item-history-lookup"]`. That control uses shared `primaryButtonStyle`. White (`#fff`) on `#0b6` is **2.53:1**, below WCAG AA 4.5:1 for normal text.

This slice darkens the shared token to `background: "#146c43"` and `borderColor: "#0f5132"`, leaving `color: "#fff"`. Independent WCAG relative-luminance check: **#fff on #146c43 = 6.45:1** (AA pass). `ItemPublishingHistoryPanel` still applies `primaryButtonStyle` at the lookup submit button; other Publish-shell primaries share the same token (intentional, matching the previous “not a one-surface restyle” contract).

Vitest `publishing.styles.test.ts` asserts the new hex pair and a WCAG 2 relative-luminance contrast ≥ 4.5. Playwright companion already exists: `expectNoSeriousA11yViolations` scoped to `[data-testid="explorer-publishing-history-dialog"]` in `modules/perc-qa-automation/frontend/tests/explorer-action-dispatch.spec.js` (the failing acceptance test). CSS-only token change; no selector/flow change. Product-docs N/A (operator procedure unchanged).

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 2 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Erlang interpretation

Gate counts in-diff bugs only. Residual LLM added no extras (no `llm.error`). Machine pass: 2 files, 0 findings.

Independent review of the token and test:

- Production change is two color literals on an existing inline-style object. Spreading `buttonStyle` then setting `borderColor` already produced the old `#0a5` border in the axe HTML; the new `#0f5132` follows the same React inline-style merge.
- Contrast math in the Vitest helper matches WCAG 2 relative luminance (independent check 6.45:1 vs comment ~6.5:1). Hex identity plus ratio ≥ 4.5 is behavioral coverage for this token; the helper is test-local, not a new production API.
- Change-class closure: shared Publish primary token + Vitest on that token. Live axe already lives on the History dialog in `explorer-action-dispatch.spec.js`. No new Playwright spec required for this CSS-only fix (`WebUI/AGENTS.md` CSS-only exception). This review did not re-run the QA cell.
- Remaining `#0b6` uses (`navButtonStyle` active border, `RuntimeSection` / `ServerList` selected borders) are outside this hunk and are not white-on-fill text. Preexisting, not blockers.

## Recommendation

approve

## Issues

_No issues._

Gate: PASS
May commit/push: yes

> Co-Authored by Grok Build using grok-4.6 with agent Erlang Shen.
