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

# Erlang pre-push review — issue #5692 (slice 126: copy a delivery server)

Raw `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main` output:

## Summary

Machine analysis found **3** finding(s), **3** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 3
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerCopy.spec.js:182
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The test 'cancel the name prompt posts nothing and keeps the list' does not verify that the list of servers remains unchanged after cancelling the name prompt.
- Suggestion: Add assertions to check that the list of servers has not changed after cancelling the name prompt.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerCopy.spec.js:236
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The test 'HTTP 400 keeps the list and shows the mapped error' does not verify that the list of servers remains unchanged after receiving a 400 status.
- Suggestion: Add assertions to check that the list of servers has not changed after receiving a 400 status.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerCopy.spec.js:290
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The test 'HTTP 403 keeps the list and shows the mapped error' does not verify that the list of servers remains unchanged after receiving a 403 status.
- Suggestion: Add assertions to check that the list of servers has not changed after receiving a 403 status.
- Status: open

---

## Adjudication (worker, 2026-11-07, persona-independent)

All 3 LLM findings are **false positives** — each cited test already asserts the
server list is unchanged:

- **Issue 1 (cancel):** the test asserts `getByRole("button", { name: /^KeepMe/ })`
  `toHaveCount(1)` (exactly one server row remains), `posted` is `false`
  (request listener proves no POST was issued), and
  `getByTestId("publish-action-message")` `toHaveCount(0)` (no error chrome).
- **Issue 2 (HTTP 400):** the test asserts the mapped alert
  (`/required/i`), the rejected name never appears
  (`{ name: /^Bad Name/ }` `toHaveCount(0)` — `loadServers` is not re-run on
  failure, so the list cannot gain a row), and `KeepMe` still has count 1.
- **Issue 3 (HTTP 403):** same shape as Issue 2 (`/forbidden/i`,
  `{ name: /^NopeSrv/ }` count 0, `KeepMe` count 1).

Machine-only analysis of the same diff reported **0 bugs / approve**; the only
machine suggestion (cognitive complexity on the pre-existing route mock) was
addressed by extracting `fulfillJson` / `handleCreatePost` helpers. The 6-test
surface spec passes live against the H2 QA cell (6 passed), with zero JS console
errors and zero `server.log` ERROR/FATAL.

**Verdict: LGTM — no blocking findings (own-model PR: review only, no
APPROVE/merge).**
