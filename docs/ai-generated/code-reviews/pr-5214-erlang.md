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

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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

- File: WebUI/src/main/ts/publishing/design/DeliveryTypesPanel.tsx:51 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DeliveryTypesPanel` cognitive=26 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open


## Erlang gate

Independent of the author. Recommendation: **approve**. In-diff bugs: 0.

`DeliveryTypesPanel` cognitive complexity is preexisting and does not block.

Manual read: rename PUT sends `name` only. Omitted `unpublishingRequiresAssembly` does not clear the stored flag; an explicit false still applies. Blank and over-long names are rejected before the request. Cancel and HTTP 400/403/409 keep the previous list row. Jackson test covers the wire flag. No filesystem path joins. No agent-rule diff.
