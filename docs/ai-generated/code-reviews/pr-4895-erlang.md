<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang review — PR #4895 Open a related item from EditorHost (#4889)

Status: cli ok

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Base: origin/main
- Head: bd26441ef5f95edaa1c1170fa85d00595230b0a9
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/4895
- CLI: mkd-code-review 0.1.18 --pack percussion --format markdown --gate advisory --git-base origin/main --models models.ollama-dev-coder.toml

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope (CLI)

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

- File: WebUI/src/main/ts/editor/editorRelatedContent.ts:188 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `flattenRelatedContent` cognitive=27 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent (reviewer)

Related-row **Open** uses `relatedRowCanOpen` / `parseExplorerContentId`, reserves a window on the click, then `openEditorHost` with `relatedItemOpenMode` (view and promote → view; otherwise edit). Failure (`false`) closes the reserved window and shows `RELATED_OPEN_FAILED`. Vitest covers mode, missing id, and failed window. Playwright `editor-related-open.spec.js` and `product-docs/8.2/admin/content-explorer.md` are in the diff. No new filesystem path joins. No agent rule files. Preexisting `flattenRelatedContent` complexity is out of this hunk and does not block.

No material bugs.

## Recommendation (reviewer)

approve

May commit/push: yes
May merge: yes
