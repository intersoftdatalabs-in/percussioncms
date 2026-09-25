<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang Code Review — PR #4882 Explorer Create Page (#4874)

Independent review. This session did not author the product diff.

**Gate: PASS.** Blocking bugs in-diff: 0. May commit/push: yes. Recommendation: approve.

Preexisting `paths.hardcoded_sep` rows on `PSFolderHelper` (lines 315–592) are outside this diff and do not block. The only in-diff row is a cognitive-complexity suggestion on `applyPageMobilePreview` (not a behavior bug). Folder-root leaf match, create-page dialog validation, Vitest, Playwright, and product-docs companions are present. `findSite` avoids the transactional `loadSite` rollback. `isSilentRollback` only changes log level; the handler still returns without post-processing, same as before.

Status: cli 0.1.18

## Pre-push local code review

## Summary

Machine analysis found **8** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 15 analyzed
- In-diff: 1 finding(s); preexisting: 7
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSFolderHelper.java:315 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 315)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSFolderHelper.java:429 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 429)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSFolderHelper.java:543 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 543)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSFolderHelper.java:561 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 561)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSFolderHelper.java:568 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 568)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSFolderHelper.java:592 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 592)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSPageChangeHandler.java:114 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `pageChanged` cognitive=19 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSFolderHelper.java:1352 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyPageMobilePreview` cognitive=17 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

