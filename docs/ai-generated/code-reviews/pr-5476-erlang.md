<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5476

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5476
- Base: origin/main
- Head: 8c26202e16950de6537e09e3e64208500cfd153e
- Files analyzed: 10
- Reviewer: independent Erlang (did not author the PR)
- LLM: CLI markdown has no `llm.error`. Wall time about 5s with empty stderr, consistent with a machine short-circuit (preexisting `handleSave` complexity is severity bug). Residual pass is this reading of the diff. Not a block.

## Independent reading

`applyContentTypeLabel` trims the label. A non-blank value replaces the stored label. A blank value stores the internal name and does not call `setName`, `setDescription`, or `setEnabled`. `updateContentType` still no-ops null fields, null allowed workflows, and null templates, so a `{label}` body does not clear those. The panel sends only `label`, treats an equal trimmed label as no write, and on a rejected response or HTTP failure restores the previous label without a success notice. The lock is not dropped on 400, 403, or 409 (same as the description-save peer; the acceptance text only requires that those statuses not claim success). Vitest, `ContentTypeAdaptorUpdateTest`, the surface Playwright spec, and `product-docs/8.2/admin/developer-content-types.md` are in the diff. No new filesystem path joins. No agent rule files.

In-diff `savedContentTypeLabel` cognitive 18 is a suggestion. The blank-echo, name, description, enabled, and field checks are covered by unit tests. Preexisting `handleSave` complexity is not this slice. Not a block.

Recommendation: **approve**. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1111 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=85 (max 15), cyclomatic=84 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/contentTypeLabel.ts:105 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedContentTypeLabel` cognitive=18 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
