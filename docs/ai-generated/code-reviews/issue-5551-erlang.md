# Erlang review — issue #5551 Explorer copy-destination NUL

**Scope:** branch `fix/issue-5551-explorer-copy-dest-nul` vs `origin/main`.

**Persona:** erlang 0.1.1 (`~/.local/share/mkd/agents/erlang`)

**CLI:** `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models …/models.ollama-dev-coder.toml`

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate: **PASS**

## Issues

_No issues._

## Intent / change-class closure

Slice 121 of parent #4530: Content Explorer **Copy** destination path containing U+0000 stays in the copy-destination dialog and is not POSTed. Ordinary path still copies. Cancel and HTTP 400/403/409 are not success.

Companions present:

| Kind | Artifact |
|------|----------|
| Production | `copyDest.ts`, `CopyDestinationPickerDialog.tsx` (refuse before `onPick`), `COPY_DEST_NUL` i18n key |
| Vitest | `copyDest.test.ts`, `CopyDestinationPickerDialog.test.tsx`, `reducedActions.test.tsx`, `ContentExplorerShell.copy-item.test.tsx` |
| Playwright | `modules/perc-qa-automation/frontend/tests/explorer-copy-dest-nul.spec.js` |
| Product docs | `product-docs/8.2/admin/content-explorer.md` (reduced-actions Copy, not an index) |

No THRASH_PATHS. No move-destination NUL (#5552). No blank required dest (#5553). No Assembly host (#5519). No new `package.json` registrations.

Cross-platform: no filesystem path construction; copy dest is a CMS finder path string. NUL check uses `String.includes("\u0000")`.

## Evidence (not a CLI finding)

- WebUI: `cd WebUI && rtk mvn clean install` — BUILD SUCCESS; Vitest 786 files / 7630 tests; Surefire Tests run: 69, Failures: 0
- perc-qa-automation: `cd modules/perc-qa-automation && rtk mvn clean install` — BUILD SUCCESS (No tests to run)
- C5: `perc-devctl qa-up` TEST_CMS_URL=http://127.0.0.1:9993; qa-health RESULT:OK HEALTH:healthy; hot-copy `WebUI/target/generated-webui/cm/modern/assets/` into perc-matrix-cms-h2 Rhythmyx WAR; qa-health again OK
- Playwright: `npm run test:surface -- --path tests/explorer-copy-dest-nul.spec.js` — 1 passed (7.9s); `npm run test:golden` — 2 passed; console-clean=yes (spec asserts); server.log-clean=yes for the feature (seed check-in conflict + `#3667` copy/item rollback-only WARN are pre-existing helper/server noise)
- Product docs smoke: `scripts/ci-smoke-product-docs.sh` OK
