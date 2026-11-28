# Erlang review — PR #7398 (issue #7390, parent #1690)

Status: **approve** — static gate clean (0 blocking bugs; 1 pre-existing complexity
suggestion recorded non-blocking), Codex: no actionable defects.
Erlang persona: 0.1.1. Cycle: stonemonkey-20261128T184051Z. Reviewer: independent erlang-review (not the Work implementer).
Checks at review: QA wiring pass, signed-commits pass (ubuntu + windows); Analyze
(javascript-typescript) + product-docs build smoke pending at review time (see merge note).

## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/TemplateDetailPanel.tsx:673 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleLabelSave` cognitive=9 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

Erlang adjudication: NON-BLOCKING suggestion on a pre-existing function. Verified against
the PR diff: `handleLabelSave` itself is untouched — the slice adds `handleLabelKeyDown` /
`handleDescriptionKeyDown` (Escape-cancel reusing existing cancel guards), edit-button
refs, and two focus-return `useLayoutEffect`s. Splitting the 18-branch save function is
future debt work, not this keyboard-path slice. Filed as tech-debt residual #7399 (p8);
never expanded into this PR per the residual rules.

Change-class closure: production keyboard path + component tests
(`TemplateDetailPanel.test.tsx`) + Playwright surface spec
(`developer-template-set-editors-keyboard.spec.js`) + proof screenshots +
`product-docs/8.2/admin/developer-templates.md` update — full companion set present.
Build evidence (WebUI BUILD SUCCESS, Surefire 84/0, Vitest 10348 passed; Playwright 4
passed on H2, zero console errors, server.log 0 ERROR/FATAL) recorded in the PR body.
No cross-platform path findings (no filesystem path logic in the diff).

### Codex review

Codex (read-only, `codex review --base origin/main`): "No actionable defects were found
in the changes against the specified merge base. Focus restoration and Escape
cancellation reuse the existing save/cancel guards, with component tests, Playwright
coverage, and product documentation included. Static analysis found no blocking bugs;
runtime tests were not run."

Erlang note: the not-run runtime suites are covered by the PR body's recorded evidence
(Playwright 4/4 green on H2), so no gap remains.

Verdict: **approve**. No BLOCK findings. Residuals: #7399 (p8, complexity in
PR-touched file — out-of-scope leftover, does not block merge).
