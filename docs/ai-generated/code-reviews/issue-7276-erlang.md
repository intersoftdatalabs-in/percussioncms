## Summary

Machine analysis found **4** finding(s) (3 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 2 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: modules/perc-common-ui-bundle/src/test/js/views/PercRegistrationViewRefactorTest.test.js:154 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 154)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (3)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `modules/perc-common-ui-bundle/src/main/js/views/PercRegistrationView.js:157` -- *Justification*: : adjudicated FP (issue #7275, precedent #7257/#6777) — reset-page input name string, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-common-ui-bundle/src/main/js/views/PercRegistrationView.js:190` -- *Justification*: : adjudicated FP (issue #7275, precedent #7257/#6777) — client-side registration form field read, no secret in source. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-common-ui-bundle/src/test/js/views/PercRegistrationViewRefactorTest.test.js:88` -- *Justification*: : adjudicated false positive (issue #7276) — "s3cret" is a fake jsdom fixture value, not a real credential. (via in_source_mkd)

</details>

### Codex review

The refactor preserves the existing registration behavior, with no actionable regressions identified. All 13 added regression tests pass.
