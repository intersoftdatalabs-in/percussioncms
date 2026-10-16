<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang review — PR 5482

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/FtpDeliveryUserPanel.test.tsx:62 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 62)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerFtpUser.test.ts:81 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 81)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=19 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Independent reading

Calling agent: night-issue-prs-erlang (Grok Build 1.0.60, grok-4.7). Persona erlang 0.1.1. CLI mkd-code-review 0.1.18, pack percussion, gate advisory, `--git-base origin/main`. No `llm.error`.

`isUserOnly` takes the early path only for a single `userid` property and does not run the full property replace. `apply` rejects a blank user id, a user id longer than 255 characters, and any driver other than FTP before `addProperty`. Host, port, folder, and password stay. The validation message does not include the password. `updateFtpUserOnly` saves the loaded server. The panel cancel path does not PUT. HTTP 400, 403, and 409 leave the loaded user id. The password is not rendered. Vitest, `PSFtpDeliveryUserUpdateTest`, Playwright, and `product-docs/8.2/admin/publishing.md` are in the diff. No new filesystem path joins. No rule files.

`secrets.heuristic` on the Vitest files is the fixture password `secret-pass`, the same shape as the host and port slices. It is not a committed credential. `updatePubServer` cognitive complexity is preexisting. Neither blocks.

Gate: PASS. Recommendation: approve. May merge: yes.
