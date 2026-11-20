# Erlang review — issue #6778 (test_matrix_install_smoke.py `secrets.heuristic` cluster)

Adjudication slice: `docker/scripts/test_matrix_install_smoke.py` — 18
`secrets.heuristic` rows on test placeholder vocabulary (env-parse fixture,
installer-argv assertion, `.env.compose.example` stubs). Verified false
positives before adjudicating: every flagged `*_PASSWORD=` value belongs to
the explicit inert set (`from-file`, `quoted`, `test-db-password`,
`test-local-only`); whole-file scan shows no PEM blocks, no gh_/AKIA/sk-
tokens, no long base64 blobs; the subject script
(`matrix-install-smoke.py`) carries no hardcoded secret (single
`DB_PASSWORD=` f-string fed from runtime env). Same FP class as
#6569/#6610/#6341 (precedent inline suppressions in `test_perc_devctl.py`).

Command (models path redacted per sanitize policy):

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base "origin/main" \
  --models <models-dir>/models.ollama-dev-coder.toml
```

## Sanitized report

models config warn: models.ollama-dev-coder.toml
## Summary

Machine analysis found **25** finding(s) (19 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 19 finding(s); preexisting: 6
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

- File: docker/scripts/test_matrix_install_smoke.py:933 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 933)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1679 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1679)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: docker/scripts/test_matrix_install_smoke.py:1682 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1682)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (19)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:70` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:837` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1088` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1089` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1090` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1091` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1092` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1250` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1251` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1252` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1253` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1254` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1379` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1380` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1381` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1382` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1383` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1651` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md:117` -- *Justification*: ` with issue + precedent citation on the sink; the `String` field maps a CMS page reference like its `LOGIN_PAGE`/`REGISTRATION_PAGE`/`RESET_PAGE` siblings. Tests: `PSSiteResetPageColumnMappingTest` (column-name pin, String page-path type, getter/setter round-trip, sibling page-column family). | 2026-11-20 | fix/issue-6728-pssite-secret | 2027-11-20 | (via in_source_mkd)

</details>
