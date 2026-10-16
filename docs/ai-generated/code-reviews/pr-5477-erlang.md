<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5477

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5477
- Base: origin/main
- Head: f2ee2ddfce4ce1abf7bb919537fce4293f3c320d
- Files analyzed: 11
- Reviewer: independent Erlang (did not author the PR)
- LLM: CLI markdown has no `llm.error`. Wall time about 5s with empty stderr, consistent with a machine short-circuit. Residual pass is this reading of the diff. Not a block.

## Independent reading

`isPortOnly` matches the host-only peer: one property, key `port`, and no name, type, description, or default. `PSPublishServerInfo.properties` is initialized, so `size()` is not a null dereference. `apply` rejects a non-FTP driver (including Local, FTPS, and SFTP), a blank port, a non-numeric port, and a port longer than 255 before `addProperty`. `addProperty` replaces an existing port and does not clear host, user, folder, or password. `updateFtpPortOnly` returns inside the site-lock `try`, so `finally` still unlocks. It does not base64-decode a password and does not call `setProperties`. The panel validates before PUT, Cancel does not write, and HTTP 400, 403, and 409 stay on the form without a success state. The UI driver check is FTP only. Vitest, `PSFtpDeliveryPortUpdateTest`, the surface Playwright spec, and `product-docs/8.2/admin/publishing.md` are in the diff. No new filesystem path joins. No agent rule files.

`secrets.heuristic` on the two tests flags the word password in fixture titles and assertions that the password is not sent. Those are not credentials. Not a block. Preexisting `updatePubServer` complexity is not this slice. A numeric port outside 1–65535 is out of the issue acceptance text (blank or non-numeric). Not a block.

Recommendation: **approve**. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

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

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/FtpDeliveryPortPanel.test.tsx:61 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 61)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerFtpPort.test.ts:87 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 87)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=18 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
