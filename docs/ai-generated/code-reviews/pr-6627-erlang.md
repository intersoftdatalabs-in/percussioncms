## Summary

Machine analysis found **18** finding(s) (11 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- In-diff: 6 finding(s); preexisting: 12
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/perc-devctl.py:761 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 761)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/perc-devctl.py:2360 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 2360)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/unit/resolve-cms-env.test.js:137 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 137)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/unit/resolve-cms-env.test.js:153 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 153)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/unit/resolve-cms-env.test.js:172 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 172)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/unit/resolve-cms-env.test.js:208 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 208)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 7 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/unit/resolve-cms-env.test.js:244 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 244)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (11)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:106` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - subcommand-name literal, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:902` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:903` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:904` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:917` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:918` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:922` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder assertion, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:923` -- *Justification*: : adjudicated FP (issue #6610) - banner label assertion, no credential value. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/unit/resolve-cms-env.test.js:184` -- *Justification*: : adjudicated FP (issue #6610, precedent #4585) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/unit/resolve-cms-env.test.js:196` -- *Justification*: : adjudicated FP (issue #6610, precedent #4585) - test-only placeholder, never a live credential. (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_perc_devctl.py:824` -- *Justification*: : adjudicated (issue #6570, parent #6341) - container-internal HEALTHCHECK path in mocked docker-inspect JSON, never a host path. (via in_source_mkd)

</details>

## Independent Erlang verdict (this run)

- Route: local Ollama pool (openrouter-free key absent in worker env); LLM stage completed on all 12 PRs with no `llm.error`.
- Gate: in-diff bugs only. Result: **no in-diff blockers on this PR**.
- Manual: intended behavior change (#6610 — in-cell passwords authoritative, cell wins over stale env even for Admin); old precedence test replaced, banner + helper tests added. Preexisting open secrets.heuristic bug rows on perc-devctl.py are the same password-vocabulary FP class adjudicated by sibling PR #6634 and already tracked by open residual #6568 — reuse-and-link, no new filing. Preexisting secrets.heuristic suggestion rows in resolve-cms-env.test.js are test placeholders (non-qualifying severity) — recorded only.
- Action: **report-only — no approve, no merge** (host sets `allow_erlang_squash_merge=false`; every PR in this run carries only this session's model + operator identity, and approving/merging own-model-only PRs is hard-banned). No COMMENT posted (nothing blocking).
