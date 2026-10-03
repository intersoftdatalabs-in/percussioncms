<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.
-->

# Erlang review — PR 5111

Independent review of `fix/issue-5101-developer-create-role` (`fc681d137062f257706bc5063dd9ff0fe64c41e8`) vs `origin/main`. Reviewer did not author the change.

`PUT /services/roles/?create=true` always calls `IRoleAdaptor.createRole` and does not fall through to `updateRole`, so a create payload cannot clear members. A missing name without the flag also creates; an existing exact name updates only when `create` is not true. Blank names are HTTP 400 before the adaptor. Non-admin create is HTTP 403. Duplicate names are `PSBeanValidationException` (a `PSValidationException`) mapped to HTTP 400, and `updateRole` is not called.

`PSRole.clone()` copies name, description, homepage, oldName, and users without `super.clone()`, so create still returns the new role when the WAR loads `perc-system`'s `PSAbstractDataObject` (no `clone()`). Both `IRoleAdaptor` implementors (`RoleAdaptor`, `RoleTestAdaptor`) define `roleExists`. Roles panel tests cover a blank name, cancel, the row only after resolve, and HTTP 400/403. Playwright and product-docs are present. No new filesystem path joins. No agent rule files.

`llm.error` is Ollama CUDA out-of-memory while loading `dev-coder`. Machine findings are 0 bugs. Not a product defect.

Gate: PASS. Recommendation: approve. May commit/push: yes.

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 19 analyzed
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
