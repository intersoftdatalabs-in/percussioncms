## Summary

Machine analysis found **111** finding(s), **110** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 0 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes (repo-wide preexisting rows only; see verdict below)

## Gate

- Blocking bugs: 110
- May commit/push: yes

## Issues

110 x `secrets.gitleaks` + 1 x `poly.osv`, all in files outside this
diff (test fixtures, `modules/perc-i18n/scripts/cache/i18n_translate.json`,
instruction docs, unrelated Java/XML). **Zero findings reference
`modules/perc-i18n/src/main/resources/i18n/DeveloperUi.tmx` or the
`perc.ui.developer@Hide from menu` TU** (grep-verified against the full
sanitized report).

## In-diff verdict (night-issue-prs #7114)

- Branch diff is exactly one file, +4 lines: an XML comment plus one
  en-us-only `<tu tuid="perc.ui.developer@Hide from menu">` TU, mirroring
  the #7083/#7084 grouping pattern.
- Static scoping note: `mkd-code-review` 0.5.0 could not resolve the
  branch diff in this linked worktree (`diff files absent from checkout;
  falling back to walk`, `--diff <patch>` likewise ignored), so scoping
  was established by grepping the full sanitized report for the diff
  path/key: 0 hits. No in-diff bugs, no behavioral-test gap (data-only
  TMX change; SPA-side key already asserted by
  `WebUI/src/test/ts/developer/messages.i18n.test.ts`), no path handling.
- The change-class gate for TMX work, `PSTmxCatalogIntegrityTest`
  (unique tuid, well-formed XML), passes 5/5 in the `perc-i18n` clean
  install (57 tests, 0 failures).
- **Gate: PASS for this diff.** Preexisting repo-wide rows do not block
  per the work contract.

### Codex review

The added translation unit matches the existing UI message key, uses the canonical en-us locale, and introduces no duplicate key. XML parsing passed; no actionable defects were found.

## Run notes (night-issue-prs #7114, cycle stonemonkey-20261123T163346Z)

- Static: `mkd-code-review analyze --pack percussion --format markdown
  --gate advisory --machine-only` (plus `--git-base origin/main`,
  `--diff <branch-patch>`, and single-file PATHS attempts; all fell back
  to walk/0-files in this worktree — recorded, not blocking).
- Sanitized via `python3 scripts/sanitize-erlang-review.py`.
- Codex: `codex review --base origin/main -c model_reasoning_effort=medium`
  — no P0/P1, no actionable regressions.
