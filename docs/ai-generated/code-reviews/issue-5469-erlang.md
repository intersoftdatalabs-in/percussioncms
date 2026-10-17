## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
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

## Intent

Folder-only FTP delivery-server PUT. Host, port, user, and password stay. A blank folder clears the stored folder. An overlong folder does not write. Non-FTP drivers, including Local, are rejected before properties change. Vitest covers cancel, a blank clear, and HTTP 400/403/409. The password strings are test fixtures, the same shape as the host, port, and user slices, and are not logged. The remote folder is stored as trimmed text and is not joined with the local filesystem separator. The cognitive finding is preexisting on `updatePubServer`. No in-diff bugs, missing behavioral tests, or non-portable paths. Gate: PASS (advisory, 0 blocking bugs).

