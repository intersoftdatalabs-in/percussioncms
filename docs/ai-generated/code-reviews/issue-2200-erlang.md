# Erlang pre-push review — issue #2200 (TableFactory/perc-ant javac slice)

- Date: 2026-11-10 (UTC)
- Branch: `fix/issue-2200-javac-javadoc-warnings` vs `origin/main`
- Persona: erlang 0.1.1 (`~/.local/share/mkd/agents/erlang`)
- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models models.ollama-dev-coder.toml`
- Verdict mapping: 0 in-diff findings → gate clean; 3 preexisting complexity rows do not block per work-scoping rules.

## Summary

Machine analysis found **3** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcStatementFactory.java:668 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getInsertStatement` cognitive=45 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcStatementFactory.java:856 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getUpdateStatement` cognitive=39 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcStatementFactory.java:373 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getCopyTableDataStatement` cognitive=19 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Author notes (change-class closure)

- The 3 complexity rows are preexisting method-size findings in functions this
  slice did not restructure (generics-only parameterisation). Refactoring those
  methods is out of scope for a warnings-cleanup slice and is tracked under the
  #2200 umbrella.
- No new behavioral logic was added (type arguments + `serialVersionUID` +
  removal of now-redundant casts + one `PSCollection<?>` wildcard), so no new
  behavioral tests were required; existing suites cover the touched paths:
  TableFactory 52 tests, perc-ant 49 tests, perc-i18n 35 tests, all green.
- Change-class completeness: javac-slice companions are the module clean
  installs (evidence in PR body). `product-docs` N/A — no operator-, admin-,
  or integrator-visible behavior changed. Javadoc output is byte-identical
  before/after (modulo line shifts); residual TableFactory javadoc
  errors/warnings are filed as a follow-up issue linked from the PR.
