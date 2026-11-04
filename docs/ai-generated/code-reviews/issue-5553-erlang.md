# Erlang review — issue #5553 Explorer copy-destination blank

**Scope:** branch `fix/issue-5553-explorer-blank-copy-dest` vs `origin/main`.

Persona: erlang 0.1.1

Persona source: ~/.local/share/mkd/agents/erlang

CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models …/models.ollama-dev-coder.toml`

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD (working tree + untracked Playwright spec included in this write-up)
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate: **PASS** (in-diff bugs: 0; missing behavioral tests: 0; non-portable path I/O: 0)

## Issues

_No issues._

## Intent / change-class closure

Slice 123 of parent #4530: Content Explorer **Copy** destination that is blank or whitespace-only stays in the copy-destination dialog and is not POSTed. A non-blank path still copies. Cancel and HTTP 400/403/409 are not success.

| Layer | Companion |
| --- | --- |
| Production | `copyDestIsBlank` in `copyDest.ts`; `CopyDestinationPickerDialog` refuses before `onPick`; `COPY_DEST_BLANK` i18n key; OK stays enabled so the error is reachable |
| REST | Already `StringUtils.isBlank(targetFolderPath)` → HTTP 400 (`copyFolderItem` / `copyFolder`); no REST production change |
| Vitest | `copyDest.test.ts`, `CopyDestinationPickerDialog.test.tsx`, `reducedActions.test.tsx`, `ContentExplorerShell.copy-item.test.tsx` |
| Playwright | `modules/perc-qa-automation/frontend/tests/explorer-copy-dest-blank.spec.js` |
| Product docs | `product-docs/8.2/admin/content-explorer.md` reduced-actions **Copy** |

Cross-platform: no filesystem path construction; copy dest is a CMS finder path string. Blank check uses `String.trim().length === 0`.

C5: `npm run test:surface -- --path tests/explorer-copy-dest-blank.spec.js` — 1 passed (4.7s); `npm run test:golden` — 2 passed; console-clean=yes (spec asserts); server.log-clean=yes for the feature (seed check-in conflict is pre-existing helper/server noise, same as #5551).

No THRASH_PATHS. No copy-destination NUL (#5551). No move dest. No Assembly host (#5519). No new `package.json` registrations.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs.
