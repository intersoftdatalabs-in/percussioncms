<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5461

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 6c976185095e70c7d501abd92858ef0c52ad0ac2
- Branch: fix/issue-5439-ftp-delivery-host
- Recommendation: approve (in-diff behavioral bugs: 0)

## Pre-push local code review

## Summary

Machine analysis found **7** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 17 analyzed
- In-diff: 2 finding(s); preexisting: 5
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/dao/impl/PSSitePublishDao.java:2306 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2306)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: system/business/src/com/percussion/sitemanage/impl/PSSitePublishDaoHelper.java:110 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 110)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:1137 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `toPSPublishServerInfo` cognitive=39 (max 15), cyclomatic=33 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/FtpDeliveryHostPanel.test.tsx:61 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 61)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerFtpHost.test.ts:81 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 81)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=17 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:1379 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `setFolderFlags` cognitive=19 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang intent

Blocking bugs: 0. Preexisting `paths.hardcoded_sep` and `toPSPublishServerInfo` complexity are outside this change and do not block. The in-diff `secrets.heuristic` rows are fixture passwords in unit tests (`s3cret-password`, `secret-pass`), not production credentials. Severity is suggestion.

Host-only PUT requires a single `serverip` and no name, type, description, or default flag. `apply` rejects a non-FTP driver, a blank host, and a host longer than 255 before `addProperty`, which replaces `serverip` and leaves port, user, folder, and password. `PSValidationException` maps to HTTP 400. The panel sends only `serverip`, does not render the password, and keeps the previous host on cancel and on HTTP 400, 403, and 409. `getProperties()` returns the live list so CXF can replace it and server assembly can append; `PSPublishServerInfoJsonReadTest` covers that. `isSiteDefaultServer` does not unbox a null default id. The folder-flag helpers change the full update and response path when both flags are false; the typed-folder case is unit-tested, and the H2 spec creates an FTP server then saves the host without dropping port, user, or folder. No new filesystem path joins. No agent-rule files.

Recommendation: approve.

> Co-Authored by Grok Build 1.0.60 using grok-4.7 with agent night-issue-prs-erlang.
