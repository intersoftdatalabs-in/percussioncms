<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #4795

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/4795
- Base: origin/main
- Head: 1316e8cd9c4d2b56c7b07c663f6301e36eca6c49 (review of erlang-fix; this report commit follows)
- Files analyzed: 12
- In-diff machine findings: 0. Preexisting path-separator rows: 27 (do not block).
- Reviewer disposition: **approve**. The prior block (rename POST 500 vs Playwright 200, `Fixes #4784` while Explorer kept the error panel) is addressed in `1316e8cd`. `PSSiteDao.updateSite` is its own `@Transactional` commit, so `RXSITES` can already hold the new name when a later navon flush throws `UnexpectedRollbackException`. The publish-server write now runs `REQUIRES_NEW` after the section save and normalizes `HAS_FULL_PUBLISHED` before merge. Navon property saves suspend the caller transaction and retry once. If rollback still escapes, `SitesAdaptor.renameSite` returns the site `findSite` already persisted instead of HTTP 500. Behavioral tests cover the adaptor recovery and the navon retry. No rule-file diffs.
- CI snapshot (one): 2 passed, 0 failed, 2 pending (CodeQL language jobs / QA wiring). May merge this turn: no, until required checks are green on the head that contains this report.
- Recommendation: **approve**. May merge: yes once required checks are green.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **27** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 0 finding(s); preexisting: 27
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:322 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 322)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:396 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 396)
- Status: open (preexisting, not in diff)

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:428 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 428)
- Status: open (preexisting, not in diff)

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:706 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 706)
- Status: open (preexisting, not in diff)

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:708 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 708)
- Status: open (preexisting, not in diff)

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:715 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 715)
- Status: open (preexisting, not in diff)

### Issue 7 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:717 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 717)
- Status: open (preexisting, not in diff)

### Issue 8 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1415 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1415)
- Status: open (preexisting, not in diff)

### Issue 9 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1470 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1470)
- Status: open (preexisting, not in diff)

### Issue 10 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1608 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1608)
- Status: open (preexisting, not in diff)

### Issue 11 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1610 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1610)
- Status: open (preexisting, not in diff)

### Issue 12 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1822 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1822)
- Status: open (preexisting, not in diff)

### Issue 13 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1823 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1823)
- Status: open (preexisting, not in diff)

### Issue 14 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:270 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 270)
- Status: open (preexisting, not in diff)

### Issue 15 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:315 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 315)
- Status: open (preexisting, not in diff)

### Issue 16 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:373 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 373)
- Status: open (preexisting, not in diff)

### Issue 17 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:385 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 385)
- Status: open (preexisting, not in diff)

### Issue 18 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:400 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 400)
- Status: open (preexisting, not in diff)

### Issue 19 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:532 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 532)
- Status: open (preexisting, not in diff)

### Issue 20 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:977 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 977)
- Status: open (preexisting, not in diff)

### Issue 21 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:1650 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1650)
- Status: open (preexisting, not in diff)

### Issue 22 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:1653 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1653)
- Status: open (preexisting, not in diff)

### Issue 23 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:2332 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 2332)
- Status: open (preexisting, not in diff)

### Issue 24 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:2375 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 2375)
- Status: open (preexisting, not in diff)

### Issue 25 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:2484 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 2484)
- Status: open (preexisting, not in diff)

### Issue 26 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java:462 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 462)
- Status: open (preexisting, not in diff)

### Issue 27 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java:1332 (preexisting)
- Rule: `paths.hardcoded_sep`
- Description: Possible non-portable path construction (line 1332)
- Status: open (preexisting, not in diff)

## Erlang disposition (not in CLI short-circuit)

- Prior bug cleared. `SitesAdaptor.renameSite` (`projects/sitemanage/src/main/java/com/percussion/apibridge/SitesAdaptor.java`) catches `UnexpectedRollbackException` (including via `PSNavException` cause) and returns `findSite(newName)` when that row exists. `PSSiteDataService.updatePubServers` no longer joins the rename request. `PSManagedNavService.setNavonProperties` suspends the caller transaction and retries once. Tests: `SitesAdaptorCreateUpdateDeleteTest.rename_rollbackAfterNamePersisted_returnsSavedSite`, `PSManagedNavServiceSetNavonPropertiesTest.setNavonPropertiesRetriesWhenLoadItemsRollsBack`.
- Suggestion (not blocking): the H2 Playwright checkbox in the PR test plan still records the pre-fix HTTP 500. This review did not re-run that surface. If a later navon rollback still skips folder title while `RXSITES` commits, the API now returns 200 for the committed site name; that matches the error-panel bug, not a new false 200 for a name that never committed (`updateSite` commits on its own transaction before navon runs).
- Preexisting `paths.hardcoded_sep` rows are outside the diff and do not block.

## C3

- modules_built: not re-run this review (read-only). Prior fix commit claims system and sitemanage clean install.
- downstream_checked: none
- Operator: Grok: night-issue-prs (model grok-4.6)
