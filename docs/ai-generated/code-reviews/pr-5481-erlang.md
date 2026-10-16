<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang review — PR 5481

## Summary

Machine analysis found **4** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 2 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1550 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleLabelSave` cognitive=6 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1639 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDescriptionSave` cognitive=6 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1743 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleRequiredSave` cognitive=13 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/ContentTypeAdaptor.java:2190 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldUpdates` cognitive=25 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Independent reading

Calling agent: night-issue-prs-erlang (Grok Build 1.0.60, grok-4.7). Persona erlang 0.1.1. CLI mkd-code-review 0.1.18, pack percussion, gate advisory, `--git-base origin/main`. No `llm.error`.

`applyLocalFieldRequired` changes occurrence only when the boolean differs from the current required dimension, and only on a local field. An unchanged flag, including one-or-more and an unchanged system or shared field, does not rewrite occurrence. A system or shared change throws before `saveContentTypes` (HTTP 400). The UI body is only `fields: [{ name, required }]`. A 200 that changes the label, the description, enabled, or another field's required flag is not treated as success. Cancel does not call update. HTTP 400, 403, and 409 leave the previous flag. Vitest, `ContentTypeAdaptorUpdateTest`, Playwright, and `product-docs/8.2/admin/developer-content-types.md` are in the diff. No new filesystem path joins. No rule files.

The in-diff cognitive rows on `handleRequiredSave` and `applyFieldUpdates` are suggestions, not wrong behavior. Preexisting cyclomatic rows on label and description save do not block.

Gate: PASS. Recommendation: approve. May merge: yes.
