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

# Erlang review — issue #5523 (database delivery-server port)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Cwd: worktree `fix/issue-5523-db-delivery-server-port`.
CLI: mkd-code-review. Reviewer did not author this change as a nested agent.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: staged branch diff
- Files: 10 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- Gate: advisory (in-diff bugs only)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- May merge: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/DatabaseDeliveryPortPanel.test.tsx:62 (in-diff)
- Rule: `secrets.heuristic`
- Description: Possible secret or credential material
- Status: open — dummy `PASSWORD` fixture matching `DatabaseDeliveryHostPanel` / FTP port tests; not a live credential.

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabasePort.test.ts:100 (in-diff)
- Rule: `secrets.heuristic`
- Description: Possible secret or credential material
- Status: open — same dummy password fixture; tests assert it is not rendered or sent.

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Description: Function `updatePubServer` cognitive=25 (max 15)
- Status: open — preexisting; this slice only adds a port-only dispatch peer of folder-only.

## Gate notes

- In-diff bugs: none.
- Behavioral tests: `PSDatabaseDeliveryPortUpdateTest` port-only detection, trim/replace port, leave host/user/database/sid/driver/password, blank/non-numeric/overlong no-write, local/FTP driver reject, password not in errors; Vitest `deliveryServerDatabasePort` + `DatabaseDeliveryPortPanel`; Playwright `sitesDeliveryServerDatabasePort.spec.js` (2 passed).
- Change-class companions: sitemanage helper + `PSPubServerService` port-only branch (stored driver selects database vs FTP, peer of folder-only), WebUI panel, Vitest, Playwright, `product-docs/8.2/admin/publishing.md` (not an index).
- Non-portable filesystem paths: none.
- Port-only PUT is gated before full property replace so omitted host/user/database/password are not cleared.

Gate: **PASS**. Recommendation: approve.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs.
