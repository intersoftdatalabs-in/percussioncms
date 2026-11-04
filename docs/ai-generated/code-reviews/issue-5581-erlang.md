<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue #5581

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/5581
- Parent: #1690
- Base: origin/main
- Head: fix/issue-5581-locale-default-timezone
- Reviewed: staged branch diff

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **33** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 5 finding(s); preexisting: 28
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Agent interpretation

- **0 blocking bugs.** The 5 in-diff findings are all `complexity.cognitive`
  *suggestions*, not bugs, and each mirrors the merged sibling slice exactly:
  - `isDefaultTzOnlyWrite` has the same shape and the same cyclomatic count
    (16) as the merged `isMeasurementSystemOnlyWrite` (also 16) — the field
    list is the format model, not incidental branching.
  - `persistFormatIfRequested` grew by one `if` branch for the new
    dedicated write, the same way every prior locale slice (#5549 … #5580)
    grew it.
  - `savedLocaleDefaultTz` mirrors the merged `savedLocaleMeasurementSystem`
    guard chain (omitted-field check plus a fingerprint of the other format
    fields).
  - `handleDefaultTzSave` mirrors `handleMeasurementSave`.
- The 28 preexisting findings are the same complexity rows the earlier locale
  slices already carry; this slice does not widen them.
- Correctness points worth stating:
  - The client sends `format.defaultTz` only. The server routes that body
    through `isDefaultTzOnlyWrite` before the full `applyFormatFields`, so
    label, description, status, language string, and every other format field
    stay.
  - Blank clears: `PSLocaleFormat.setDefaultTz` maps an empty value to null
    and the row is saved without the key.
  - The 64-character bound is enforced in both places — client
    (`LOCALE_DEFAULT_TZ_MAX_LENGTH`, refuses before the write) and server
    (`validateMaxLen("defaultTz", …, 64)` → 400).
  - The accepted value is only shown when the response matches what was sent
    and nothing else changed (`savedLocaleDefaultTz`); otherwise the previous
    value stays and the save is reported as a failure.
- No missing behavioral tests: `localeDefaultTz.test.ts` (9),
  `LocaleDetailPanel.test.tsx` (#5581 block, 2), `LocalesAdaptorDesignWsTest`
  (3 new cases, 63 total in the class), and the H2 Playwright spec cover the
  write, the unchanged case, the overlong refusal, the clear, Cancel, and
  HTTP 400 / 403 / 409.
- No non-portable path/file I/O.
- Gate: PASS for commit/push.
