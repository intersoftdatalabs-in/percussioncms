# Pre-push local code review — issue #7161

Static gate (`mkd-code-review analyze --pack percussion --format markdown
--gate advisory --git-base origin/main --machine-only`): **approve**,
0 blocking bugs, may commit/push yes. LLM stage skipped per operator
direction (no Ollama; OpenRouter key not required).

## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
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

- File: modules/perc-i18n/scripts/test_i18n_translate.py:248 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 248)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

Adjudication: verified false positive. The flagged literals are the
*subjects* of the `PathSafetyTest.test_no_os_path_import` guard rail,
which asserts those exact substrings are absent from
`i18n_translate.py`. Same pattern as sibling #7160 (left unsuppressed
there); preexisting row, not in-diff, does not block.

### Codex review

First pass found one real in-diff [P2]: the new absolute-path rejection
fixtures used the POSIX-only literal `/abs/path.tmx`, which is not
absolute on Windows (`Path.is_absolute()` false without a drive), so
`test_absolute_path_rejected` would fail on Windows. Mitigated by
building the fixture from `Path.cwd() / 'abs-path.tmx'` (absolute on
every supported platform). Re-ran Codex after the fix:

> The refactoring preserves the existing translation, retry, TMX scanning, and CLI behavior. All 62 translation-tool tests passed; no actionable regressions were identified.
