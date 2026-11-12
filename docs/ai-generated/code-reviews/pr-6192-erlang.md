# Erlang review — PR #6192

## Summary

Machine analysis found **7** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 21 analyzed
- In-diff: 0 finding(s); preexisting: 7
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercRevisionDialog.js:38 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `openDialog` cognitive=32 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/webapp/cm/plugins/PercRevisionDialog.js:38 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `openDialog` cognitive=32 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: bug

- File: WebUI/src/main/webapp/cm/widgets/PercInlineEditDataTable.js:75 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `init` cognitive=151 (max 15), cyclomatic=86 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercPublishingHistoryDialog.js:37 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `openDialog` cognitive=19 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/webapp/cm/app/js/legacy/views/PercEditRegionPropertiesDialog.js:594 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveRegionProperties` cognitive=16 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/webapp/cm/plugins/PercPublishingHistoryDialog.js:37 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `openDialog` cognitive=16 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/webapp/cm/views/PercEditRegionPropertiesDialog.js:594 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveRegionProperties` cognitive=16 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Independent Erlang verdict (this review, PR #6192)

- Fresh `mkd-code-review` run on PR head `468b92f1a9` (percussion pack, advisory, origin/main, Ollama dev-coder, persona erlang 0.1.1): **7 findings, 0 in-diff, all preexisting complexity**; recommendation approve. Matches the embedded pre-push report (21 files analyzed).
- Manual review of the migration: `DataTable()` constructor + `isDataTable` pre-destroy guard; `row()/cell()/draw()/destroy()` receivers are 1.10+ API instances (init returns `oTable` — PercRegionCSSHandler `destroy()`/`rows()` calls verified against the API type); `tryRestoreRow` preserves swallow-and-continue semantics with zero behavior change; `fnRender(oObj)` -> `render(data)` preserves 1.x all-types (sort uses rendered markup, as before); `oApi._fnGetTrNodes` -> `this.api().column(iColumn, {order:"index"}).nodes()` in data-index order; `new Number` -> `Number` (wrapper-object fix); `table.$` scoped selector intact on the API instance. No non-portable filesystem paths (URL/asset joins only). `WebUI/war/**` content-freeze disclosed (stale snapshot, deletion parity only).
- Tests / closure: 2 new core-swap Vitest pins (`buildLegacyBundles.test.js`) + new Playwright `classic-datatables-core.spec.js`; canonical + `app/js/legacy` twins migrated lockstep; no JSP/HTML/config changes needed (stable URLs preserved). Product-docs N/A accepted (parity migration, no operator-visible change).
- Checks snapshot: CodeQL pass, Analyze (javascript-typescript) pass, Detect language changes pass, QA wiring pass; Analyze (java-kotlin) + H2 live skipping by path-filtered design (no Java changes). C1 (WebUI BUILD SUCCESS 75 surefire + 8682 vitest; perc-qa-automation BUILD SUCCESS) and C5 (core spec 2 passed + prune peer 2 passed; server.log zero ERROR/FATAL) accepted from body evidence; missing_build_evidence: full clean installs not re-run locally (trust-but-verify).
- Residuals (preexisting complexity in PR-touched files, p8): #6197, #6198, #6199. bugs_found (in-diff): none.
- Verdict: **LGTM** (independent Erlang). Fixes #6002 close-gate holds (last open slice of parent #5663).
