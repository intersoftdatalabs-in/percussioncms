## Summary

Machine analysis found **17** finding(s) (14 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 14 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1311 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getSaaSSiteNames` cognitive=24 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1508 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getCssFilesCount` cognitive=20 (max 15), cyclomatic=8 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1856 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateListAsset` cognitive=29 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (14)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md:169` -- *Justification*: ` with issue + precedent citation on the sink; the `String` field maps a CMS page reference like its `LOGIN_PAGE`/`REGISTRATION_PAGE`/`RESET_PAGE` siblings. Tests: `PSSiteResetPageColumnMappingTest` (column-name pin, String page-path type, getter/setter round-trip, sibling page-column family). | 2026-11-20 | fix/issue-6728-pssite-secret | 2027-11-20 | (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:325` -- *Justification*: : adjudicated FP (issue #6730) — escaped quotes (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:400` -- *Justification*: : adjudicated FP (issue #6730) — escaped quotes (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:434` -- *Justification*: : adjudicated FP (issue #6730) — escaped quotes (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:719` -- *Justification*: #6730 escaped-quote FP (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:721` -- *Justification*: #6730 escaped-quote FP (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:733` -- *Justification*: #6730 escaped-quote FP (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:735` -- *Justification*: #6730 escaped-quote FP (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1457` -- *Justification*: : adjudicated intentional (issue #6730, (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1516` -- *Justification*: : adjudicated intentional (issue #6730, (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1656` -- *Justification*: #6730 escaped-quote FP (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1658` -- *Justification*: #6730 escaped-quote FP (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1873` -- *Justification*: : adjudicated intentional (issue #6730, (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1876` -- *Justification*: : adjudicated intentional (issue #6730, (via in_source_mkd)

</details>
