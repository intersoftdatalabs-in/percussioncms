# Erlang review: EditorHost item publish history (#4863)

**Branch:** `fix/issue-4863-editor-publish-history`  
**Base:** `origin/main`  
**Head:** `1f59a8e9184e0fe0a80abdba1a4a5c9edd119ad2`  
**Date:** 2026-09-25  
**Persona:** erlang 0.1.1  
**Persona source:** `/home/nate/.local/share/mkd/agents/erlang`  
**Reviewer:** independent of the implementer (read-only; no product edits)

## Summary

Slice of parent #4532: **Publishing history** on the React Content Editor host for the already-open page or asset. Edit and View show the control. The host opens the existing `PublishingHistoryDialog` with `String(contentId)` and does not call publish or takedown. Promote, folders, templates, and items with no id stay hidden because `resolveEditorPublishKind` returns `none` and `canViewPublishHistoryFromEditor` is false for `none`.

No in-diff bug. Behavioral Vitest covers the visibility predicate, a view-mode row for the open item, empty history, and HTTP 404 as an error (not empty success). Playwright covers the edit-mode row for content id 42, empty history, HTTP 404, no publish call, leftover Content Editor URLs, console cleanliness, and dialog a11y. Product docs note the editor-host behavior. No non-portable filesystem paths.

## Recommendation

**approve**

## Gate

**PASS**

**May commit/push: yes**

Blocking bugs: 0. Missing behavioral tests for new logic: none. Non-portable path/file I/O: none.

## Scope

- Diff: `git diff origin/main...HEAD` (8 files, +446). Working tree clean; no untracked files for a side unified diff.
- `WebUI/src/main/ts/editor/EditorHost.tsx`
- `WebUI/src/main/ts/editor/editorPublish.ts`
- `WebUI/src/main/ts/editor/messages.ts`
- `WebUI/src/test/ts/editor/EditorHost.test.tsx`
- `WebUI/src/test/ts/editor/editorPublish.test.ts`
- `modules/perc-qa-automation/frontend/tests/editor-host-publish-history.spec.js`
- `modules/perc-qa-automation/frontend/tests/helpers/editor-host-publish-history.js`
- `product-docs/8.2/admin/publishing.md`

## CLI report (`mkd-code-review`)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Exit code: 0. Stderr empty. Machine report (full substance):

```markdown
## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._
```

The CLI did not emit an LLM issues section. This file is the independent reading of the same diff.

## In-diff bugs

None.

Checked against the new logic:

- `canViewPublishHistoryFromEditor` is edit or view, and page or asset. Promote is false. Kind `none` is false, and `resolveEditorPublishKind` already returns `none` without an id, without a content type, and for folder/site/template tokens. The host button uses that kind, so unsaved items do not show the control.
- The dialog mounts only when `historyOpen && contentId != null` and passes `itemId={String(contentId)}`. `contentId` is `parsePositiveInt` of the editor query. `itemPubHistoryUrl` builds `…/itemmanagement/item/pubhistory/${encodeURIComponent(id)}` (URL path, not an OS join).
- Empty list renders `item-history-empty`. Non-2xx from `get()` throws `ApiError`; the existing panel maps 404 and 403 to `item-history-error` and clears rows. The new host test and Playwright spec both show 404 as that error, not the empty state.
- Publish and takedown are separate controls. View mode still hides Publish now. The new tests assert `publishItem` is not called and the Playwright publish-route list stays empty.

## Missing behavioral tests

None that block.

Present and exercised (Vitest, this session: 3 passed, 120 skipped in the two files, filter `4863|canViewPublishHistoryFromEditor`):

- `canViewPublishHistoryFromEditor`: edit+page true, view+asset true, promote+page false, edit+none false, view+none false.
- EditorHost view mode: button present, Publish now absent, dialog row contains `prod`, close removes the dialog, `publishItem` not called.
- EditorHost edit mode: empty `ItemPublishingHistory` shows `item-history-empty`; a later 404 shows `item-history-error` and not the empty state.

Playwright (`editor-host-publish-history.spec.js`):

- Edit mode: one `pubhistory/42` GET, row text `prod`, publish route not called, no leftover `checkoutedit.xml` / `contenteditorurls.html` / `view=editor`, no page errors, dialog a11y scope.
- View mode: Publish now absent, empty history, then 404 error with empty state gone, publish route still not called.

Preexisting `itemHistoryErrorMessage` tests already name HTTP 403. The new wiring uses that same catch. A second host assertion for 403 would be redundant with the 404 path.

## Non-portable paths

Clean. New code and tests use CMS/URL slashes (`/services/itemmanagement/item/pubhistory/…`, Playwright URL globs and regexes). The helper comment states those are logical URL paths. No `"/" +` filesystem joins, no `/tmp` or `C:\` roots, no `:`/`;` path-list splits, no Unix-only path assertions.

## Change-class closure

Change class: **WebUI product screen (EditorHost) reusing the existing publishing-history dialog and item pubhistory GET**, plus Playwright and product-docs.

| Companion | Status |
|-----------|--------|
| Visibility predicate + host button + dialog wired to the open content id | present |
| Vitest predicate + host row / empty / HTTP error | present |
| Playwright surface spec + helper | present |
| `product-docs/8.2/admin/publishing.md` editor-host note | present |
| New REST adaptor / sitemanage type | not required (existing GET) |
| Dual-ship `WebUI/war` copy of this TSX | not applicable (canonical source is `WebUI/src/main/ts`) |
| Agent rule files | not in the diff |

## Suggestions (non-blocking)

- Host Vitest matches any `/item/pubhistory/` URL and checks the row text. The content-id assertion lives in Playwright (`/pubhistory/42`). A fetch URL assertion in the host test would pin the same fact without a CMS.
- The predicate comment says folders, templates, and unsaved items stay hidden. The function itself only sees mode and kind; those cases stay hidden because the host passes `resolveEditorPublishKind`, which returns `none`. Callers that pass `"page"` without going through that resolver would show the control.
- Issue #4863 acceptance also asks to update parent #4532 **Agent progress**. That text is the GitHub issue body, not this diff. It does not change the code gate.

## Issues

None blocking.
