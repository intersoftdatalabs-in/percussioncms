## Summary

Machine analysis found **30** finding(s) (2 suppressed), **5** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 46 analyzed
- In-diff: 12 finding(s); preexisting: 18
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 5
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSApplicationDependencyHandler.java:99 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getChildDependencies` cognitive=97 (max 15), cyclomatic=31 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSApplicationDependencyHandler.java:671 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformIds` cognitive=145 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSContentRelationDependencyHandler.java:620 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `processFieldSet` cognitive=75 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSContentTypeDependencyHandler.java:122 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getCEChildDependencies` cognitive=49 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSFolderObjectDependencyHandler.java:230 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getChildDependencies` cognitive=43 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSLocaleDefDependencyHandler.java:504 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformTmxIds` cognitive=77 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSMenuActionObjectDependencyHandler.java:71 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getChildDependencies` cognitive=36 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSRelationshipDefDependencyHandler.java:306 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformIds` cognitive=39 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSearchObjectDependencyHandler.java:359 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformIds` cognitive=45 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSharedGroupDependencyHandler.java:219 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=46 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSlotDefDependencyHandler.java:736 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformIds` cognitive=36 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 12 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSTranslationSettingsDefDependencyHandler.java:452 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformIds` cognitive=59 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 13 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/PSAppTransformer.java:1011 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformDataMapper` cognitive=27 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 14 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSApplicationDependencyHandler.java:577 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getIdTypes` cognitive=26 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSApplicationDependencyHandler.java:817 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformDbms` cognitive=29 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSContentRelationDependencyHandler.java:275 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=21 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 17 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSFolderTranslationsDependencyHandler.java:139 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=16 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 18 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSKeywordDependencyHandler.java:204 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `doInstallDependencyFiles` cognitive=19 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 19 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSLocSchemeDefDependencyHandler.java:227 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `doInstallDependencyFiles` cognitive=16 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 20 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSLocaleDefDependencyHandler.java:278 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=28 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 21 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSMenuActionDefDependencyHandler.java:222 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installActions` cognitive=30 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 22 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSMenuActionObjectDependencyHandler.java:288 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformIds` cognitive=23 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 23 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSearchObjectDependencyHandler.java:79 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getChildDependencies` cognitive=24 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 24 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSearchObjectDependencyHandler.java:509 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getSystemFieldDeps` cognitive=20 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 25 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSearchObjectDependencyHandler.java:650 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `transformSystemFieldIds` cognitive=26 (max 15), cyclomatic=8 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 26 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSlotDefDependencyHandler.java:144 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getChildDependencies` cognitive=30 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 27 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSTemplateDefDependencyHandler.java:216 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getChildDependencies` cognitive=20 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 28 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSTemplateDefDependencyHandler.java:454 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=20 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (2)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/webdav/test/util/PSServletRequesterTest.java:162` -- *Justification*: : FOLDER_PATH logged here only; CMS logical path, see adjudication above (issue #5978). (via in_source_mkd)
- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/webdav/test/util/PSServletRequesterTest.java:172` -- *Justification*: : FOLDER_PATH logged here only; CMS logical path, see adjudication above (issue #5978). (via in_source_mkd)

</details>


## Author adjudication (work agent, 2026-11-10)

Machine gate verdict above is `request-changes` on 5 `complexity.cognitive`
findings; all 3 `paths.hardcoded_sep` findings are suppressed inline as
verified false positives, and the gate output states `May commit/push: yes`.

1. **`paths.hardcoded_sep` (PSServletRequesterTest, 3 sites, suppressed)** —
   verified false positive. The flagged `writeln(...)` lines contain no path
   separator literals; the only `/` literal in the method is the CMS logical
   folder path `"//Folders/TestFolder"` passed to
   `PSWebdavUtils.getComponentByPath` (product convention: CMS folder paths
   use `/`, cf. `PATH_SEP = "/"` in `PSFolderObjectDependencyHandler`), not an
   OS filesystem join. Suppressed inline per the AGENTS.md suppression
   protocol with issue #5978 cited; re-verified by re-running the CLI
   (findings moved to suppressed).
2. **`complexity.cognitive` (5 in-diff sites: PSContentRelationDependencyHandler
   `processFieldSet`, PSFolderObjectDependencyHandler `getChildDependencies`,
   PSSharedGroupDependencyHandler `installDependencyFiles`, PSSlotDefDependencyHandler
   `transformIds`, PSTranslationSettingsDefDependencyHandler `transformIds`)** —
   pre-existing complexity in untouched logic. This diff changed only generic
   type arguments, casts, and annotations inside these methods; no control
   flow, branching, or call targets were altered (verify via
   `git diff -U0`, which shows type-only hunks). Extracting helpers from
   packaging/install flows is behavior-risky scope creep for a warnings
   slice and is explicitly out of scope; per night-gates, pre-existing rows
   do not block. Left for a dedicated refactoring slice.
3. **Remaining pre-existing findings (complexity/suggestions outside the
   diff)** — untouched, out of scope.

No behavioral tests were added because no behavior was changed: every edit
is a type-argument, cast, annotation, comment, or constant-form change that
compiles to identical bytecode semantics, covered by the existing 316-test
module suite (green).
