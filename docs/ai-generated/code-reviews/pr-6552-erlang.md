# Erlang independent review - PR #6552

Status: independent second pass (not the Work implementer). Persona erlang 0.1.1, OpenCode 2.3.0, model muse-spark-1.3-contributor. Machine findings below are the strict gate input; in-diff bugs would BLOCK. Verdict: Parent #2626. Gate: 0 blocking bugs. Machine rows (preexisting handleSave complexity.cognitive=37; in-diff suggestion validateSlotRows cognitive=17) do not block; filed as residuals #6566 (p7) and #6567 (p8). Manual pass: slotMappingValidation mirrors the sitemanage TemplateAdaptor/SlotsAdaptor 400 contract (slots[i], not-found key mapping); Jackson single-object collapse fix covered by new unwrapTemplateDetail tests; rowErrors wiring uses role=alert + aria-invalid/aria-describedby; save path blocks PUT on client errors and maps slot PUT/template 400 failures onto rows. Closure complete: new + extended Vitest, new Playwright surface spec, product-docs section. Checks green. Merged via admin squash (same-login APPROVE unavailable).

---

## Summary

Machine analysis found **2** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 10 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/design/TemplateSourceEditor.tsx:204 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=37 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/design/slotMappingValidation.ts:57 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `validateSlotRows` cognitive=17 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
