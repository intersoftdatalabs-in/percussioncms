# Erlang self-review — #2065 golden unattended Playwright smoke

**Branch:** `feat/issue-2065-golden-unattended-smoke`  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (pre-commit gate)

## Change class

| Class | Artifacts |
|-------|-----------|
| Playwright golden smoke (QA mode) | `tests/golden-unattended-smoke.spec.js`, `npm run test:golden`, README + workbench one-shot docs |
| Installer antlib task registration | `perc-ant` antlib.xml + unit test (unblock silent H2 `qa-up`) |

## Checklist

| Gate | Result |
|------|--------|
| Bugs | No logic bugs found in golden path; reuses `loginAsAdmin` + stable `data-testid` explorer selectors |
| Portable paths | Docs use `path`-agnostic `python docker/scripts/...`; Windows cmd uses `\` only in examples |
| Secrets | No passwords committed; placeholders only |
| Behavioral tests | `AntlibTaskRegistrationTest`; existing `PSGenerateRepositoryPasswordTest`; frontend `test:unit` 48 pass |
| Module clean install | `perc-ant` BUILD SUCCESS; `perc-qa-automation` BUILD SUCCESS |
| AGENTS rule commits | None (issue forbids; human-review gate) |
| Scope | No full suite / surface matrix / CI workflow |

## Live H2 evidence (session)

1. Pre-fix: `qa-up` failed mid-install: `failed to create task or type PSGenerateRepositoryPassword` (antlib missing taskdef) → fixed in this PR.
2. After antlib only: silent install completed; Jetty up; `/Rhythmyx/login` **HTTP 503** — Spring `Cannot locate BeanDefinitionParser for element [logging]` from legacy `<cxf:logging/>` in `sitemanage-beans.xml` → fixed via `LoggingFeature` bean + `cxf-rt-features-logging` (WebUI WAR repackage required for installer).
3. Modern React login: auth helper updated for `data-testid` form (no native `select[name=j_locale]`).
4. **Live green:** `RESULT:OK STEP:qa-up` → `TEST_CMS_URL=http://127.0.0.1:9993` → `npm run test:golden` → **2 passed** (env resolve + Admin login + Content Explorer shell) → `qa-down`.

## Verdict

**Ship** golden smoke + antlib + CXF login stack fixes. **Acceptance met** for live H2 golden path.
