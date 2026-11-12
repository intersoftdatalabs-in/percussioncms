## Summary

Machine analysis found **5** finding(s) (4 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
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

- File: WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:366 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `buildSpaForwardPath` cognitive=51 (max 15), cyclomatic=49 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (4)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:268` -- *Justification*: : adjudicated intentional (issue #6145, (via in_source_mkd)
- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:349` -- *Justification*: : adjudicated intentional (issue #6145, (via in_source_mkd)
- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:394` -- *Justification*: : adjudicated intentional (issue #6145, (via in_source_mkd)
- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:402` -- *Justification*: : URL prefix join, not a filesystem path — see adjudication above (issue #6145). (via in_source_mkd)

</details>

