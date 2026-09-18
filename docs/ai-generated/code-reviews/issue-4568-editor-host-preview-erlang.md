# Erlang review: EditorHost assembled Preview (#4568)

**Branch:** `fix/issue-4568-editor-host-preview`  
**Base:** `origin/main`  
**Date:** 2026-09-18  
**Persona:** Erlang (independent of implementer)

## Summary

Slice 4 of parent #4532: **Preview** on the React Content Editor host for the already-open page or asset. Reuses Explorer `openPreviewItem` (Page Management render for pages; asset view URL for assets). View and edit may preview; edit with a dirty form confirms that the last saved revision is assembled. HTTP 403/404 is not success. Promote stays on the restore form. No jQuery/AA overlay.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral Vitest covers kind/path-item construction, dirty detection, page probe-then-open, 403/404 not opening a window, missing id/kind, asset view-url fetch. EditorHost tests cover view (no confirm), dirty confirm/cancel, FORBIDDEN banner, promote hide, percRichText as asset. Playwright surface spec covers assembled popup (not editor host), unsaved confirm, 403/404, cancel, asset view URL, leftover CE blocklist, console-clean, a11y. Product-docs updated on content-explorer editor controls and getting-started.

## Change-class closure

Change class: **WebUI product screen (EditorHost) + existing Explorer preview helpers + Playwright + product-docs**. No new REST adaptor.

| Companion | Status |
|-----------|--------|
| `editorPreview.ts` + EditorHost `data-testid=editor-preview` | present |
| Vitest (`editorPreview.test.ts`, EditorHost host tests) | present |
| Playwright `editor-host-preview.spec.js` + helper + unit helper tests | present |
| product-docs admin content-explorer + getting-started | present |
| Date widgets (#4569) / new-copy (#4570) / AA overlay | out of scope |

## Cross-platform path checklist

CMS/URL/classpath paths only (`/` in `/Sites/`, `/Assets/`, `/pagemanagement/render/page/`). Playwright helpers document URL slashes, not OS joins. **Outcome: clean.**

Memory patterns hit: behavioral tests for forbidden-not-success; Playwright companion for WebUI screen; product-docs for user-visible chrome; no THRASH_PATH overlap with the open publishing-takedown PR (`paths.ts`, `developer/messages.ts`, `rest.md`, `admin/index.md` untouched). `frontend/package.json` only appends the new unit-test path.

## Issues

None blocking.

**suggestion:** Page preview GETs assembled HTML to probe 403/404, then `window.open` loads it again. Acceptable for the forbidden/unknown-id gate; a HEAD probe would be lighter if the render servlet honors it later.

## Re-review

Playwright stubs moved from `page.route` to `page.context().route` so popup `window.open` does not hit live Page Management / assembly (first C5 pass leaked `page.does.not.exist` / assembly ERROR for stub ids 42/99). Surface spec re-run 6 passed; no new server.log ERROR in the second window. Gate unchanged.
