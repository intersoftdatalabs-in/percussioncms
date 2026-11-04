# Erlang review — issue #5555 (PublishingShell Oracle delivery-server SID)

Persona: erlang 0.1.1  
CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main`  
Models: `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`  
Scope: branch `fix/issue-5555-publishing-oracle-sid` vs `origin/main` (10 files).

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD (staged slice files)
- Files: 10 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

Hard gate (bugs, missing behavioral tests, non-portable paths): **PASS**. Preexisting `updatePubServer` complexity is out of slice scope. In-diff `secrets.heuristic` hits are fixture passwords in Vitest (same pattern as `DatabaseDeliveryNamePanel` / host/port peers; values are not live secrets and are asserted **not** to appear in PUT bodies or the DOM).

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/DatabaseDeliverySidPanel.test.tsx:63 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 63)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open (accepted peer fixture; not a live credential)

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabaseSid.test.ts:101 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 101)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open (accepted peer fixture; not a live credential)

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=28 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open (preexisting; this slice only adds a SID-only dispatch sibling)

## Change-class closure (persona)

Vertical slice matches database-name (#5554) peers:

- `PSDatabaseDeliverySidUpdate` + unit tests
- `PSPubServerService` SID-only PUT dispatch
- Vitest unit + panel
- Playwright `sitesDeliveryServerDatabaseSid.spec.js`
- `product-docs/8.2/admin/publishing.md`
- Wired in `SiteWorkspace`
- No rest module change (existing publishmanagement PUT)
- No rule files
- No new filesystem path joins

## Gate verdict

**PASS** — no in-diff bugs, behavioral tests present, portable I/O not applicable.
