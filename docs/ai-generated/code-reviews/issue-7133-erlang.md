## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: .github/workflows/h2-qa-playwright.yml:214 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 214)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Codex review
The new CI invocation inherits the login-only surface filter and fails before executing static coverage. This was reproduced using the workflow's environment settings.

Review comment:

- [P1] Isolate the static run from inherited surface filters — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-lane-a/.github/workflows/h2-qa-playwright.yml:130-130
  On every PR run, this step inherits `SURFACE_PATH=tests/login.spec.js`. `run-surface.js` retains that environment path even when `--tag static` is supplied, producing `playwright test tests/login.spec.js --grep @static`. This selects zero tests and exits with `No tests found`, failing the wiring job and blocking its dependent live job. Clear all `SURFACE_*` filters for this invocation or invoke Playwright directly with `--grep @static`.

**Mitigation (this branch):** split the static run into its own CI step with no
`SURFACE_*` env (new _Static pins_ step in `h2-qa-playwright.yml`). Verified locally via `--print-only`: inherited env produced `playwright test tests/login.spec.js --grep @static` (zero tests → red); clean env produces `playwright test --grep @static` (27 tests, green, no CMS). README documents the clean-env requirement. Codex P1 addressed; no residual.
