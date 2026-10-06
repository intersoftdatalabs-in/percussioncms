<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5293

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Tool: mkd-code-review 0.1.18 (`analyze --pack percussion --format markdown --gate advisory --git-base origin/main`)
- Models: `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5293
- Base: origin/main (027798ad7ae0265c1a2d446488e0ae1d12abe672)
- Head: 324748b2d48306cbf8479e41517de567b9110d6e
- Files analyzed: 7
- Reviewer is independent of the implementer.
- Ollama `dev-coder` returned HTTP 500 (CUDA out of memory). Machine findings kept. Not a merge block.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open


## Independent reading

Rename posts a name-only `LocationSchemeSummary`. `JSON.stringify` drops undefined fields, and `PSPublishingDesignRestService.updateScheme` applies a non-blank name only. A null generator, description, content type, template, context, or parameter list is left stored (`applySchemeParameters` returns immediately when parameters are null). Blank and over-long names are rejected before the PUT. A failed update, including HTTP 409, does not replace the list. After success the UI prefers the refreshed rows and, if that refresh fails, patches only the name on the previous row. `openSchemeRename` loads the stored scheme for display and resets the shared rename field. Vitest, the H2 Playwright spec, and `product-docs/8.2/admin/publishing.md` are in the diff. No rule-file diff and no filesystem path I/O.

Recommendation: **approve**. May commit/push: yes. In-diff blocking bugs: 0.
