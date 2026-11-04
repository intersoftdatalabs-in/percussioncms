# Erlang review — issue #5574 Explorer move-destination blank

**Scope:** branch `fix/issue-5574-explorer-blank-move-dest` vs `origin/main`.

Persona: erlang 0.1.1

Persona source: ~/.local/share/mkd/agents/erlang

CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models …/models.ollama-dev-coder.toml`

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD (working tree + untracked Playwright spec included in this write-up)
- Files: 10 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate: **PASS** (in-diff bugs: 0; missing behavioral tests: 0; non-portable path I/O: 0)

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/folders/FoldersResource.java:857 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 857)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open (preexisting, not in this slice's added lines)

## Intent / change-class closure

Slice 124 of parent #4530: Content Explorer **Move** destination that is blank or whitespace-only stays in the move-destination dialog and is not POSTed. A non-blank path still moves. Cancel and HTTP 400/403/409 are not success.

| Layer | Companion |
| --- | --- |
| Production | `moveDestIsBlank` in `moveDest.ts`; `MoveDestinationPickerDialog` refuses before `onPick`; `MOVE_DEST_BLANK` i18n key; OK stays enabled so the error is reachable |
| REST | `FoldersResource.rejectMoveDestBlank` after NUL check; `move/item` and `move/folder` HTTP 400 |
| REST tests | `FoldersTest` (`moveFolderItem_blankDestIsBadRequest`, `moveFolder_blankDestIsBadRequest`, `pathIsBlank_isTrueForEmptyAndWhitespace`) |
| Vitest | `moveDest.test.ts`, `MoveDestinationPickerDialog.test.tsx`, `reducedActions.test.tsx`, `ContentExplorerShell.move-item.test.tsx` |
| Playwright | `modules/perc-qa-automation/frontend/tests/explorer-move-dest-blank.spec.js` |
| Product docs | `product-docs/8.2/admin/content-explorer.md` reduced-actions **Move** and move-item table row |

Cross-platform: no filesystem path construction; move dest is a CMS finder path string. Blank check uses `String.trim().length === 0` and `StringUtils.isBlank`.

C5: `npm run test:surface -- --path tests/explorer-move-dest-blank.spec.js` — 1 passed (5.1s); `npm run test:golden` — 2 passed; console-clean=yes (spec asserts); server.log-clean=yes (no ERROR/FATAL in the QA cell `server.log` for the test window).

No THRASH_PATHS. No move-destination NUL (#5552). No copy dest. No Assembly host (#5519). No new `package.json` registrations.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs.
