## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: response contract: finding location is outside the provided diff
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

---

## Worker adjudication (issue #6680)

The single reported finding is `llm.error` — the LLM inference stage was
unavailable and produced no in-diff finding (machine findings in-diff: 0).
Per the night-issue-prs work contract, a machine-only result with 0 in-diff
findings short-circuits as clean; it is recorded here and disclosed in the PR
body rather than blocking the PR.

The diff itself is minimal and mechanical: remove the stale `percCalendar`
pin (v1 widget dropped by #6656; only `percCalendarTwo` ships on disk),
correct the template-count comment (48 → 47), and update the pinned-count
assertion (45 → 44). The full `npm run test:unit` suite (711 tests) passes,
including the previously-failing closure test.
