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

# Erlang review — PR #5565 delivery-server database name (#5554)

Status: independent overnight review (this agent did not author the PR)

Persona: erlang 0.1.1

Persona source: ~/.local/share/mkd/agents/erlang

CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models …/models.ollama-dev-coder.toml`

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate: **PASS** (in-diff `secrets.heuristic` hits are dummy fixture passwords used to prove the PUT body and UI do not leak/clear stored credentials)

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/DatabaseDeliveryNamePanel.test.tsx:62 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 62)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

Fixture password asserts the Local/Oracle path never renders it. Not a committed live secret.

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabaseName.test.ts:101 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 101)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

`withUpdatedDatabaseName` must leave `password` on the in-memory server. Dummy value.

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=27 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent / change-class closure

MySQL/MSSQL database-name-only PUT; blank/overlong/Oracle/FTP/Local rejected; host/port/userid/password/SID/driver/name/default stay. Companions: helper + panel + SiteWorkspace, sitemanage apply, unit tests, Playwright, product-docs. Peers: database host/port/user slices.

Recommendation: **approve**.
