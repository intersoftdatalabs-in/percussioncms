# Erlang review: fix DTS widget surface specs (#6244)

Independent Erlang review (did not author the change): **approve**.
Machine findings: 0 blocking bugs. Gate: may commit/push.

```text
residual model=dev-coder:latest id=ollama-dev-coder outcome=started
residual model=dev-coder:latest id=ollama-dev-coder outcome=completed findings=0
## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 14 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: deliverytiersuite/delivery-tier-suite/tomcat-common/src/main/java/com/percussion/tomcat/filters/PSAddResponseHeaderFilter.java:66 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `doFilter` cognitive=20 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
```

Reviewer notes (human-readable):

- The single finding is preexisting (cognitive complexity of the
  untouched `doFilter` structure) and explicitly out of the in-diff
  gate. The in-diff edit *reduces* coupling (drops the Spring import
  that caused a `ClassNotFoundException` on every DTS request from the
  shared classloader) without adding branches.
- The four `beans.xml` edits are additive `<context:exclude-filter>`
  elements that remove duplicate Spring bean definitions (the Jersey
  `SpringComponentProvider` skipped all four REST resources as
  ambiguous); no wiring values changed.
- The two `security.xml` edits wire up an existing (previously
  unreferenced) CSRF matcher bean (polls) and add one ignored path
  (comments `addcomment`, matching the openly served forms collect
  path); no roles, guarantees, or URL patterns changed.
- Playwright specs + helper follow the established US2/US3 patterns
  (closed inventory, two-level skip contract, `node:test` unit file
  registered in `test:unit`); no new env vars, no default-suite weight.
```

> Co-Authored by OpenCode 2.3.0 using muse-spark-1.3-contributor with agent night-issue-prs.
