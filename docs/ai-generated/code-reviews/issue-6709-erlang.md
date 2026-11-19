# Pre-push local code review — issue #6709

Branch: `fix/issue-6709-m2-snapshot-poisoning` vs `origin/main`.
Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory
--git-base origin/main` (local Ollama pool; no paid fallbacks).
Sanitized with `scripts/sanitize-erlang-review.py` before commit.

## Summary

Machine analysis found **4** finding(s) (2 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- In-diff: 0 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/qa_preflight.py:76 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 76)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/qa_preflight.py:245 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 245)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (2)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/perc-devctl.py:763` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - help-text vocabulary, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/perc-devctl.py:2370` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - QA env-parsing helper; password vocabulary in docstring/key names only, value read live from the cell file, never hardcoded. (via in_source_mkd)

</details>

## Author disposition

Both open rows are **preexisting** (not in-diff): line 76 is the original
`_M2_GROUP + "/" + _M2_ARTIFACT` constant join, and line 245 is the ZIP-entry
`WEB-INF/lib/` match (ZIP paths always use `/` per spec, independent of OS).
Neither blocks this slice per the in-diff gate. No suppression added — both
are out of this slice's scope; noted here for the record.
