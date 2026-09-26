# Erlang review — issue 4912

## Summary

Machine analysis found **8** finding(s), **0** bug(s) in the advisory gate (preexisting path rows are not blocking).

## Scope

- Base: origin/main
- Head: fix/issue-4912-approve-queue-item
- Files: 11 analyzed
- In-diff: 2 finding(s); preexisting: 6
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- In-diff bugs, missing behavioral tests, non-portable paths: none blocking

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:550 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 550)
- Status: open (preexisting; not in this diff)

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:757 (preexisting)
- Rule: `paths.hardcoded_sep`
- Status: open (preexisting)

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:778 (preexisting)
- Rule: `paths.hardcoded_sep`
- Status: open (preexisting)

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:805 (preexisting)
- Rule: `paths.hardcoded_sep`
- Status: open (preexisting)

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:807 (preexisting)
- Rule: `paths.hardcoded_sep`
- Status: open (preexisting)

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:1081 (preexisting)
- Rule: `paths.hardcoded_sep`
- Status: open (preexisting)

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/ts/publishing/incrementalQueue.ts:71 (in-diff)
- Rule: `complexity.cognitive`
- Description: Function `extractQueueItems` cognitive=16 (max 15). Preexisting function; this change only added helpers above it.
- Status: open (not a bug)

### Issue 8 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:953 (in-diff)
- Rule: `complexity.cognitive`
- Description: Function `approveQueuedIncrementalContent` cognitive=15, cyclomatic=16 (max 15).
- Status: open (suggestion; behavior covered by unit tests)

## Intent

Approve-one keeps the item on the incremental queue, reloads the list, and surfaces 400/403/404 on the queue panel. Cancel does not call the server. Companions: sitemanage service + adapter, WebUI shell, Vitest, Playwright surface spec, product-docs publishing note.
