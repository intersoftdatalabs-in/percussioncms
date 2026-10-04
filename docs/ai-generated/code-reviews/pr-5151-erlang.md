<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5151

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5151
- Branch: fix/issue-5141-role-home-page
- Issue: #5141
- Base: origin/main
- Head: acdefc55f6f1affe0c6d669a466a6654ad33d8df
- Reviewer: independent Erlang pass (not the author). Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **4** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 21 analyzed
- In-diff: 0 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/role/service/impl/PSRoleService.java:368 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 368)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/RoleAdaptor.java:396 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `browseRoles` cognitive=31 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/role/service/impl/PSRoleService.java:520 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getUserHomepage` cognitive=18 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open


## Interpreter

Independent read of the diff (not the author). PUT /services/roles/?homePage=true changes only the stored home page. Description and members are copied from the stored role. A blank value deletes the metadata key. An unknown non-blank value is HTTP 400 before write. A missing role is 404. A non-admin is 403. Description update still copies the stored homepage, so it does not clear it. readStoredHomepage does not invent Home; getUserHomepage still resolves an empty set to Home. The row shows the new value only after reload. Cancel does not send the home-page request. Companions are present: RolesResource tests, RoleTestAdaptor, RoleAdaptor tests, PSRoleServiceHomepageWriteTest, Vitest, surface Playwright, product-docs admin and REST. Preexisting paths.hardcoded_sep at PSRoleService.java:368 is a user-name sentence, not a filesystem join, and it is outside the homepage hunk. browseRoles complexity is preexisting. Neither blocks. Ollama dev-coder failed to load (CUDA out of memory); machine findings kept. In-diff bugs: 0. Recommendation: approve. May commit/push: yes.
