<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5485

Independent pre-merge review. Author of the feature is not this review. Persona: erlang 0.1.1. Persona source: /home/nate/.local/share/mkd/agents/erlang.

## Pre-push local code review

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

- File: WebUI/src/test/ts/publishing/FtpDeliveryFolderPanel.test.tsx:62 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 62)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerFtpFolder.test.ts:87 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 87)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=20 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang

Independent reading: `secrets.heuristic` matches the test fixtures `s3cret-password` and `secret-pass`. Those are not live credentials. `isFolderOnly` stays off host-only, port-only, user-only, and password bodies. `PSPubServer.addProperty` replaces an existing folder and does not clear host, port, user, or password. A blank folder clears only the folder. A non-FTP driver, including Local, throws before `savePubServer`. Over 255 characters is rejected. The panel does not render the password. Vitest, `PSFtpDeliveryFolderUpdateTest`, Playwright, and `product-docs/8.2/admin/publishing.md` are present. The folder value is a remote path and is not joined with `File.separator`. No rule files. `updatePubServer` complexity is preexisting.

Gate: PASS. Recommendation: approve. May commit/push: yes.
