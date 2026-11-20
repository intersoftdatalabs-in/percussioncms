## Summary

Machine analysis found **35** finding(s) (29 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- In-diff: 0 finding(s); preexisting: 35
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:204 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 204)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:231 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 231)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:242 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 242)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:958 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 958)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1704 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1704)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1707 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1707)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (29)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/perc-devctl.py:811` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - help-text vocabulary, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/perc-devctl.py:2491` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - QA env-parsing helper; password vocabulary in docstring/key names only, value read live from the cell file, never hardcoded. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:70` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:837` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1113` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1114` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1115` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1116` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1117` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1275` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1276` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1277` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1278` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1279` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1404` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1405` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1406` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1407` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1408` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1676` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:107` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - subcommand-name literal, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:901` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:902` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:903` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:916` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:917` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:921` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder assertion, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:922` -- *Justification*: : adjudicated FP (issue #6610) - banner label assertion, no credential value. (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_perc_devctl.py:823` -- *Justification*: : adjudicated (issue #6570, parent #6341) - container-internal HEALTHCHECK path in mocked docker-inspect JSON, never a host path. (via in_source_mkd)

</details>
