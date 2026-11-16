## Summary

Machine analysis found **5** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 1 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/contenttypes/ContentTypesResource.java:1108 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1108)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1367 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=95 (max 15), cyclomatic=89 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/ContentTypeAdaptor.java:2413 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldUpdates` cognitive=25 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/ContentTypeAdaptor.java:2608 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `assignParentFieldSequences` cognitive=22 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/ContentTypeAdaptor.java:2714 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `stampChildSequences` cognitive=20 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Erlang adjudication (reviewer addendum, 2026-11-16)

- Verdict: **approve** — 0 in-diff bugs. Behavioral tests present (Mockito
  `ContentTypeAdaptorLocalFieldTest` + Vitest panel tests + surface
  Playwright `developer-content-type-field-child-order.spec.js` with C5 live
  proof); product-docs updated; change-class closure complete.
- Preexisting rows in PR-touched files filed as capped tech-debt residuals:
  #6525 (`paths.hardcoded_sep`, p7), #6526 (`handleSave` complexity, p7),
  #6527 (field-sequence complexity incl. in-diff suggestion, p8). No duplicates
  found (searched open issues + parent #1690 links).
