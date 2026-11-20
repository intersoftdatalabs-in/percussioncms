# Erlang review — issue #6704 (parallel QA cell isolation)

> Sanitized `mkd-code-review analyze --pack percussion` report (advisory gate, origin/main). Raw CLI stdout redacted via `scripts/sanitize-erlang-review.py --check` clean.

## Summary

Machine analysis found **40** finding(s) (11 suppressed), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 2 finding(s); preexisting: 38
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:70 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 70)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:837 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 837)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1088 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1088)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1089 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1089)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1090 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1090)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1091 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1091)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 7 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1092 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1092)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 8 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1250 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1250)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 9 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1251 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1251)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 10 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1252 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1252)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 11 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1253 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1253)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 12 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1254 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1254)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 13 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1379 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1379)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 14 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1380 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1380)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 15 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1381 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1381)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 16 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1382 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1382)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 17 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1383 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1383)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 18 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1651 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1651)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 19 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:204 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 204)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 20 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:231 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 231)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 21 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:242 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 242)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 22 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:933 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 933)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 23 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1679 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1679)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 24 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1682 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1682)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 25 -- Severity: bug

- File: docker/scripts/matrix-install-smoke.py:1225 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `wait_for_http` cognitive=75 (max 15), cyclomatic=32 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 26 -- Severity: bug

- File: docker/scripts/perc-devctl.py:2705 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `cmd_qa_health` cognitive=41 (max 15), cyclomatic=31 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 27 -- Severity: suggestion

- File: docker/scripts/matrix-install-smoke.py:1374 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `run_cell` cognitive=17 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 28 -- Severity: suggestion

- File: docker/scripts/matrix-install-smoke.py:1657 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `main` cognitive=24 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 29 -- Severity: suggestion

- File: docker/scripts/perc-devctl.py:2497 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `cmd_qa_up` cognitive=23 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (11)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/perc-devctl.py:807` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - help-text vocabulary, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/perc-devctl.py:2485` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - QA env-parsing helper; password vocabulary in docstring/key names only, value read live from the cell file, never hardcoded. (via in_source_mkd)
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
