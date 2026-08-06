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

1. Pre-fix: `qa-up` failed mid-install: `failed to create task or type PSGenerateRepositoryPassword` (antlib missing taskdef).
2. Post-fix: silent install completed; Jetty connector up on freeport; `/Rhythmyx/login` returned **HTTP 503** because Rhythmyx webapp Spring context failed: `Cannot locate BeanDefinitionParser for element [logging]` in `sitemanage-beans.xml` (CXF nested `<logging>`).
3. `var/config/generated/passwords` only had `cmdb=` (no `Admin=`).

**Residual:** product readiness of H2 matrix cell after install (Spring CXF logging + Admin password emission) — track as follow-up issues; not expanded in this PR.

## Verdict

**Ship** golden smoke + antlib registration. **Partial** vs full acceptance (live green login+explorer deferred on residual stack defects).
