## Summary

Machine analysis found **6** finding(s) (4 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 4 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/activity/service/impl/PSActivityService.java:400 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 400)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/test/java/com/percussion/activity/service/impl/PSActivityServiceEmptyCellTest.java:121
- Rule: `llm.openrouter-free`
- Tool: `llm`
- Description: The `contentWs.getIdByPath(anyString())` stub in `stubSingleSite` is never exercised by the all-sites branch of `createActivityNodesByPaths` (that branch only maps `siteMgr.findAllSites()` to `PSActivityNode`). Under Mockito's default STRICT_STUBS this raises UnnecessaryStubbingException and fails both new tests.
- Suggestion: Remove the `contentWs.getIdByPath` stub from `stubSingleSite`, or make the test class lenient if the stub is needed elsewhere.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (4)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/activity/service/impl/PSActivityService.java:359` -- *Justification*: : adjudicated intentional (issue #6354, (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/activity/service/impl/PSActivityService.java:364` -- *Justification*: : adjudicated intentional (issue #6354, (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/activity/service/impl/PSActivityService.java:367` -- *Justification*: : adjudicated intentional (issue #6354, (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/activity/service/impl/PSActivityService.java:385` -- *Justification*: : adjudicated intentional (issue #6354, (via in_source_mkd)

</details>

