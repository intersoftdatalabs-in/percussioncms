# Erlang independent review - PR #6558

Status: independent second pass (not the Work implementer). Persona erlang 0.1.1, OpenCode 2.3.0, model muse-spark-1.3-contributor. Machine findings below are the strict gate input; in-diff bugs would BLOCK. Verdict: Fixes #6341 (slice 8). Gate: 0 blocking; machine rows all preexisting in touched files (secrets.heuristic on password vocabulary; paths.hardcoded_sep on /tmp test argv literals) - filed as residuals #6568/#6569/#6570 (p7). Manual pass: List<IPSFeedDescriptor>->List<PSFeedDescriptor> compiles (diamond infers IPSFeedDescriptor; PSFeedDescriptor implements it) with no wire-shape change; Jackson-3 provider deserialization covered by 2 new tests + fixture; seed script uses pathlib/PurePosixPath with suppression and stdin-only secret transport. Closure: JUnit + node unit + python tests + Playwright + AGENTS.md + product-docs. Checks green. Merged via admin squash.

---

## Summary

Machine analysis found **15** finding(s) (3 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 14 analyzed
- In-diff: 3 finding(s); preexisting: 12
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/perc-devctl.py:716 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 716)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/perc-devctl.py:2276 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 2276)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:98 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 98)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:894 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 894)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:895 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 895)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:896 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 896)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 7 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:386 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 386)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:486 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 486)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:658 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 658)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 10 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:695 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 695)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 11 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:732 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 732)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 12 -- Severity: bug

- File: docker/scripts/test_perc_devctl.py:816 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 816)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (3)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `docker/scripts/qa_seed_widget_fixtures.py:56` -- *Justification*: : adjudicated intentional (issue #6341) — (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/qa_seed_widget_fixtures.py:237` -- *Justification*: : adjudicated intentional (issue #6341) — (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_qa_seed_widget_fixtures.py:185` -- *Justification*: : fake asserting the cell body-file path (issue #6341). (via in_source_mkd)

</details>
