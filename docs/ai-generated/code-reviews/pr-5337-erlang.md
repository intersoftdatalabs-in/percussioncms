/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

# Erlang review PR #5337 (issue #5310)

Status: cli-ok (mkd-code-review 0.1.18)

Persona: erlang 0.1.1
Persona source: ~/.local/share/mkd/agents/erlang

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 17 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/RoleAdaptor.java:250 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addRoleUser` cognitive=18 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/RoleAdaptor.java:307 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `removeRoleUser` cognitive=20 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Interpreter

Independent read of `origin/main...HEAD` (not the author session). `removeRoleUser` copies the stored description and home page and drops only the named member. Blank, multiple, unknown, and non-member names are HTTP 400 and do not call `update`. A strand (`validateDeleteUsersFromRole`) or removing the caller from Admin is HTTP 409 and does not call `update`. The client sends one user and does not treat a 400, 403, or 409 body as success. `IRoleAdaptor` is implemented by `RoleAdaptor` and `RoleTestAdaptor` only. Vitest, adaptor tests, resource tests, product-docs, and the H2 Playwright surface are present. No new filesystem path joins. No agent rule files. In-diff complexity on `removeRoleUser` is a suggestion, not a defect. Preexisting `addRoleUser` complexity does not block. Ollama failed to allocate a CUDA buffer; machine findings stand.

Recommendation: approve. May merge: yes.
