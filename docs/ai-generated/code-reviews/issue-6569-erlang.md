## Summary

Machine analysis found **10** finding(s) (4 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- In-diff: 4 finding(s); preexisting: 6
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:386 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 386)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:486 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 486)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:658 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 658)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:695 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 695)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:732 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 732)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:816 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 816)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (4)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:98` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - subcommand-name literal, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:894` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:895` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:896` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)

</details>
