<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5419

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5419
- Base: origin/main
- Head: df33cea6ca14052cdab96d4dcf7ca9fa9e3f2532
- Files analyzed: 13
- In-diff machine findings: 5 (all `complexity.cognitive` suggestions)
- Reviewer disposition: **approve**. In-diff complexity rows are suggestions, not bugs. The preexisting `paths.hardcoded_sep` hit at `TemplateDetailJsonReaderTest.java:62` is string concatenation inside a JSON literal (`guid.stringValue` `0-2-1`), not a filesystem join. It does not block.
- Independent read: a description-only PUT omits bindings and slots. `TemplateDetailJsonReader` stores null for an omitted list, and `TemplateAdaptor.applyMutableTemplateUpdates` skips a null list, so bindings and slots are not cleared. An empty array still replaces. Create with omitted lists still starts from an empty `PSAssemblyTemplate`. The panel sends only `{description}`, treats an unchanged value and Cancel as no PUT, and does not show success when the response changes the label or drops bindings. HTTP 400, 403, and 409 keep the previous description. Companions present: Vitest, `TemplateDetailJsonReaderTest`, `tests/developer-template-description.spec.js`, and `product-docs/8.2/admin/developer-templates.md`. No new filesystem path joins. No agent rule files.
- Recommendation: **approve**. May merge: yes, when the check snapshot is green.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **7** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed
- In-diff: 5 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/templates/TemplateDetailJsonReaderTest.java:62 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 62)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/TemplateDetailPanel.tsx:646 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=26 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/templateDescription.ts:169 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedTemplateDescription` cognitive=20 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: rest/src/main/java/com/percussion/rest/templates/TemplateDetailJsonReader.java:94 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parse` cognitive=14 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: rest/src/main/java/com/percussion/rest/templates/TemplateDetailJsonReader.java:152 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `bindingsFromNode` cognitive=16 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: rest/src/main/java/com/percussion/rest/templates/TemplateDetailJsonReader.java:192 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `slotsFromNode` cognitive=16 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: rest/src/main/java/com/percussion/rest/templates/TemplateDetailJsonReader.java:232 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `refsFromNode` cognitive=16 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
