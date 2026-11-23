## Summary

Machine analysis found **10** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 0 finding(s); preexisting: 10
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:669 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 669)
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:820 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 820)
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:831 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 831)
- Status: open

### Issue 4 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:864 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 864)
- Status: open

### Issue 5 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:932 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 932)
- Status: open

### Issue 6 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:950 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 950)
- Status: open

### Issue 7 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:953 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 953)
- Status: open

### Issue 8 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:995 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 995)
- Status: open

### Issue 9 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:106 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `findListedPageViaRest` cognitive=33 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:381 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `openSitesThenPages` cognitive=28 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
### Codex review

The new synchronization logic lacks the behavioral unit coverage explicitly required by the repository's blocking review gate. The added matching-helper tests do not verify the timing-sensitive behavior being repaired.

Review comment:

- [P2] Add behavioral tests for the new list synchronization logic — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/modules/perc-qa-automation/frontend/tests/explorer-content-editor.spec.js:270-275
  The added unit tests cover name matching and tree-node selection, but none exercise the new snapshot classification or navigation synchronization. These introduce substantial branching for stale rows, loading, errors, empty folders, and unchanged-path timeouts—the behavior this patch is intended to repair. Extract the synchronization helpers into a testable module and add deterministic behavioral tests for these states, including delayed row replacement. [AGENTS.md:124](AGENTS.md#L124) explicitly makes missing behavioral unit tests for new non-trivial logic a blocking review issue.
