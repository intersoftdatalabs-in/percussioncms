## Summary

Machine analysis found **25** finding(s) (19 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 1 finding(s); preexisting: 24
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:210 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 210)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:237 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 237)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:248 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 248)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:964 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 964)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1710 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1710)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1713 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1713)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (19)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:76` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:843` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1119` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1120` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1121` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1122` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1123` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1281` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1282` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1283` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1284` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1285` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1410` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1411` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1412` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1413` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1414` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1682` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:2174` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)

</details>
