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

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=38 (max 15), cyclomatic=42 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/SftpDeliveryPassphrasePanel.test.tsx:64 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 64)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerSftpPassphrase.test.ts:56 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 56)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

