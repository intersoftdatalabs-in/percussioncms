# Erlang review — issue 5323

## Summary

Machine analysis found **6** finding(s), **2** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- In-diff: 0 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 2
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/perc-devctl.py:559 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 559)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/perc-devctl.py:1917 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1917)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: docker/scripts/hot-deploy-rhythmyx-war-jars.py:303
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The script does not handle the case where the 'utils' jar is missing the 'perc-noscript' shield.
- Suggestion: Add a check to ensure that the 'utils' jar contains the 'perc-noscript' shield before copying it. If not, log an error and exit.
- Status: open

### Issue 4 -- Severity: bug

- File: docker/scripts/perc-devctl.py:591
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The documentation for the '--qa-deploy-war-jars' command does not mention the jsoup 'noscript' shield.
- Suggestion: Update the documentation to include information about the jsoup 'noscript' shield and how it is handled by the script.
- Status: open

### Issue 5 -- Severity: suggestion

- File: docker/scripts/hot-deploy-rhythmyx-war-jars.py:489 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `_remove_artifact_jars` cognitive=23 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: docker/scripts/hot-deploy-rhythmyx-war-jars.py:721 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `deploy` cognitive=19 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Interpreter note (night-issue-prs)

Machine scope line is authoritative for the gate: **in-diff findings: 0**. The two `secrets.heuristic` bugs and both complexity rows are preexisting and do not block.

LLM rows 3 and 4 are false positives on this diff:

- `resolve_module_jars` returns `EXIT_MARKER_MISSING` when the target `utils` jar lacks `perc-noscript` and Maven local has no shielded jar. `deploy` checks `jar_has_noscript_shield` again before copy. Covered by `test_missing_noscript_shield`.
- `--then-qa-deploy-webui` and `qa-deploy-war-jars` help text cite `#5323` and copying `utils`; `qa-deploy-war-jars` help says the copy is so cells can clean `<noscript>`.

Gate applied here: no in-diff bug, no missing behavioral test, no non-portable path. May open the PR.
