# Erlang review — issue #5503

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 1 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:1348 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `toPSPublishServerInfo` cognitive=38 (max 15), cyclomatic=32 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting. This change always sets `isDefault` from `site.getDefaultPubServer()` (classic sample sites are not page-based, so GET/list omitted the flag). No new branches in the mapper.

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDefault.test.ts:58 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 58)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

Disposition: not a bug. The string is a fixture password on a mocked delivery-server object so the default-only body can be asserted not to include it. Same pattern as `deliveryServerRename.test.ts`.

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=23 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting. This change adds a default-flag-only guard that returns before the full replace; it does not expand the full-update body.

## Gate notes

- In-diff bugs: 0
- Missing behavioral tests: none for the new default-only path (`PSDeliveryServerDefaultUpdateTest`, Vitest unit + panel, Playwright `sitesDeliveryServerDefault.spec.js`)
- Non-portable paths: none
- May commit/push: yes
