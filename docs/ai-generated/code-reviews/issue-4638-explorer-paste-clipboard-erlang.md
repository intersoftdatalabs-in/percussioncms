# Erlang review — #4638 Explorer paste clipboard into destination

Independent of implementer. Skill: erlang-review.

## Verdict

**PASS** (no hard-gate bugs / missing behavioral tests / non-portable path I/O in this diff).

## Scope

Clipboard paste now posts copy/move into the **selected destination folder** (`target.path`), not `item.path` as both source and dest. REST `copy/folder` rethrows 403/404/409 instead of HTTP 200 Status 404. Playwright H2 surface `explorer-paste-clipboard.spec.js`.

## Checks

- Behavioral tests: `clipboardApi.test.ts` dest envelope; `FoldersTest` copyFolder 403/404/409/400; ClipboardPanel dest label.
- Paths: REST/JS use existing `Path`/`paths.ts` URL roots; no new OS separator joins.
- Page paste uses `/folders/copy/item` so dest is applied (page/copy by id ignored dest).

## Residual

None required for this slice.
