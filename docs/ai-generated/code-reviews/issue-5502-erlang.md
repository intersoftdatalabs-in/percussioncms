# Erlang review — issue #5502

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerRename.test.ts:103 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 103)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

Disposition: not a bug. The string is a fixture password on a mocked delivery-server object so the rename body can be asserted not to include it. Same pattern as `deliveryServerFtpHost.test.ts` / `FtpDeliveryHostPanel.test.tsx`.

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=22 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting. This change adds a name-only guard that returns before the full replace; it does not expand the full-update body.

## Gate notes

- In-diff bugs: 0
- Missing behavioral tests: none for the new name-only path (`PSDeliveryServerRenameTest`, Vitest unit + panel, Playwright `sitesDeliveryServerRename.spec.js`)
- Non-portable paths: none
