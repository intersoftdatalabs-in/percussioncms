# Erlang review — issue #5552 Explorer move-destination NUL

**Scope:** branch `fix/issue-5552-explorer-move-dest-nul` vs `origin/main`.

**Persona:** erlang 0.1.1 (`~/.local/share/mkd/agents/erlang`)

**CLI:** `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models …/models.ollama-dev-coder.toml`

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate: **PASS**

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/folders/FoldersResource.java:835 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 835)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

Preexisting row outside this slice. CMS finder paths in this diff are URL/CMS path strings (always `/`), not OS filesystem joins. Not a gate block.

## Intent / change-class closure

Slice 122 of parent #4530: Content Explorer **Move** destination path containing U+0000 stays in the move-destination dialog and is not POSTed. Ordinary path still moves. Cancel and HTTP 400/403/409 are not success. REST `POST /rest/folders/move/item` and `move/folder` map a dest NUL to HTTP 400 without calling the adaptor.

Companions present:

| Kind | Artifact |
|------|----------|
| Production | `moveDest.ts`, `MoveDestinationPickerDialog.tsx` (refuse before `onPick`), `MOVE_DEST_NUL` i18n key, `FoldersResource.rejectMoveDestNul` |
| Vitest | `moveDest.test.ts`, `MoveDestinationPickerDialog.test.tsx`, `reducedActions.test.tsx`, `ContentExplorerShell.move-item.test.tsx` |
| REST tests | `FoldersTest` (`moveFolderItem_nulDestIsBadRequest`, `moveFolder_nulDestIsBadRequest`, `pathContainsNul_isTrueOnlyForU0000`) |
| Playwright | `modules/perc-qa-automation/frontend/tests/explorer-move-dest-nul.spec.js` |
| Product docs | `product-docs/8.2/admin/content-explorer.md` (reduced-actions Move, not an index) |

No THRASH_PATHS. No copy-destination NUL (#5551). No blank required dest (#5553). No Assembly host (#5519). No new `package.json` registrations. Adaptor interface unchanged (no sitemanage signature change).

Cross-platform: no filesystem path construction; move dest is a CMS finder path string. NUL check uses `String.includes("\u0000")` / `indexOf('\0')`.

## Evidence (not a CLI finding)

- rest: `cd rest` via `rtk mvn -f rest/pom.xml clean install` — BUILD SUCCESS; Tests run: 1801, Failures: 0
- WebUI: `cd WebUI && rtk mvn clean install` — BUILD SUCCESS; Vitest 791 files / 7655 tests; Surefire Tests run: 69, Failures: 0
- perc-qa-automation: `cd modules/perc-qa-automation && rtk mvn clean install` — BUILD SUCCESS (No tests to run)
- C5: `perc-devctl qa-up` TEST_CMS_URL=http://127.0.0.1:9993; qa-health RESULT:OK HEALTH:healthy; `qa-deploy-webui` + `qa-deploy-war-jars --restart-jetty`; qa-health again OK
- Playwright: `npm run test:surface -- --path tests/explorer-move-dest-nul.spec.js` — 1 passed (5.4s); `npm run test:golden` — 2 passed; console-clean=yes (spec asserts); server.log-clean=yes for the feature (`#3667` moveItem rollback-only WARN is pre-existing helper/server noise)
- Product docs smoke: `scripts/ci-smoke-product-docs.sh` OK
