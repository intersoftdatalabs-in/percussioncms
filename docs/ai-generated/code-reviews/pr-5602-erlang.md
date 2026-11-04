<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5602

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5602
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/5581
- Parent: #1690
- Base: origin/main
- Head: 4d951deb06
- Reviewed: published PR head (worktree `~/.grok/worktrees/intersoft-workspace-percussioncms/slice-5581`)
- Reviewer: independent Erlang pass (not the implementer persona)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **33** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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
  *suggestions*, not bugs, and each one mirrors the merged sibling slice
  (#5580 / PR #5597) exactly:
  - `isDefaultTzOnlyWrite` has the same shape and the same cyclomatic count
    (16) as the merged `isMeasurementSystemOnlyWrite` (also 16). The
    branch count is the format model's field list, not incidental branching.
  - `persistFormatIfRequested` grew by one `if` branch for the new dedicated
    write, exactly the way every prior locale slice (#5549 … #5580) grew it.
  - `savedLocaleDefaultTz` mirrors the merged `savedLocaleMeasurementSystem`
    guard chain (omitted-field check plus a fingerprint of the other format
    fields).
  - `handleDefaultTzSave` mirrors `handleMeasurementSave`.
- The 28 preexisting findings are the complexity rows the earlier locale
  slices already carry; this slice does not widen them.
- Correctness points checked by the reviewer:
  - The client sends `format.defaultTz` only. The server routes that body
    through `isDefaultTzOnlyWrite` / `persistDefaultTzOnly` **before** the
    full `applyFormatFields`, so label, description, status, language string,
    and every other format field stay.
  - Blank clears: `PSLocaleFormat.setDefaultTz` maps an empty value to null,
    and `putIfPresent` then omits the key, so the saved row loses it.
  - The 64-character bound is enforced in both places — client-side
    (`LOCALE_DEFAULT_TZ_MAX_LENGTH`, refuses before the write) and server-side
    (`validateMaxLen("defaultTz", …, 64)` → 400).
  - The accepted value is only shown when the response matches what was sent
    and nothing else changed (`savedLocaleDefaultTz`); otherwise the previous
    value stays and the save is reported as a failure.
  - The language string is immutable: the dedicated body never sends it.
- No missing behavioral tests: `localeDefaultTz.test.ts` (9 Vitest),
  `LocaleDetailPanel.test.tsx` (#5581 block, 2), `LocalesAdaptorDesignWsTest`
  (3 new cases; 63 in the class), and the H2 Playwright spec cover the write,
  the unchanged case, the overlong refusal, the clear, Cancel, and
  HTTP 400 / 403 / 409.
- No non-portable path/file I/O.
- Gate: **PASS**.

## Independent verification (reviewer, not the implementer)

Standalone JDK 21 clean install on the branch tree, run from the review worktree:

- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS.
- `cd rest && ../mvnw clean install` — BUILD SUCCESS.
- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS (Surefire + Vitest green).
- `cd modules/perc-qa-automation && ../../mvnw clean install` — BUILD SUCCESS.
- `cd projects/sitemanage && ../../mvnw test -Dtest=LocalesAdaptorDesignWsTest` — Tests run: 63, Failures: 0, Errors: 0.
- `npx vitest run ../../test/ts/developer/` — Test Files 135 passed, Tests **1792 passed**.

C5 live UI proof on a fresh H2 QA cell with this branch's WebUI SPA and
`sitemanage` / `rest` jars hot-deployed (`TEST_CMS_URL=http://127.0.0.1:9993`):

- `python3 docker/scripts/perc-devctl.py qa-up --skip-image-build --then-qa-deploy-webui` — RESULT:OK
- `qa-deploy-webui` RESULT:OK; `qa-deploy-war-jars --restart-jetty` RESULT:OK
- `qa-health` — RESULT:OK HTTP:200 HEALTH:healthy
- `npm run test:surface -- --path tests/developer-locale-default-tz.spec.js` — **1 passed** (sets the default timezone only after success, keeps language / label / description / status / date-time / separators / currency / text direction / first day / measurement system, and a blank value clears)
- server.log-clean=yes (0 ERROR/FATAL in the QA cell `server.log` for the test window)

Verdict: **LGTM** — independent review, C1 and C5 re-verified.
