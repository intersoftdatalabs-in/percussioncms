## Summary

Machine analysis found **2** finding(s) (1 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 20 analyzed
- In-diff: 0 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/PSItemDefinition.java:157 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 157)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (1)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `rest/src/main/java/com/percussion/rest/contenttypes/ContentTypesResource.java:1112` -- *Justification*: : adjudicated intentional (issue #6525) — (via in_source_mkd)

</details>
