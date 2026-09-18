# Erlang review: EditorHost Publish now (#4540)

**Branch:** `fix/issue-4540-editor-publish-now`  
**Base:** `origin/main`  
**Date:** 2026-09-17  
**Persona:** Erlang (independent of implementer)

## Summary

Slice 2 of parent #4532: **Publish now** on the React Content Editor host for the already-open page or asset. Reuses sitemanage demand-publish GETs via `itemPublishPaths` and `mapPublishResponse` (same contract as Explorer `publishSelectedItem`). Confirm before fire. View/promote stay read-only. HTTP 200 `FORBIDDEN`/`BADCONFIG` is not success.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral Vitest covers kind classification, page vs resource GET, HTTP 200 preflight failure, confirm/cancel, view-mode hide. Playwright surface spec covers confirm, FORBIDDEN, cancel, view, percRichText resource path, leftover CE blocklist, console-clean, a11y. Product-docs updated on the editor host page.

## Change-class closure

Change class: **WebUI product screen (EditorHost) + existing sitemanage publish GET + Playwright + product-docs**. No new REST adaptor; no `itemPublish.ts` edits (avoids overlap with open Explorer schedule-dates PR).

| Companion | Status |
|-----------|--------|
| `editorPublish.ts` + EditorHost button/confirm | present |
| Vitest (`editorPublish.test.ts`, EditorHost host tests) | present |
| Playwright `editor-host-publish-now.spec.js` + helper | present |
| product-docs admin content-explorer + publishing | present |
| Workflow transitions (#4539) | not re-implemented (out of scope) |

## Cross-platform path checklist

CMS/URL paths only (`/`). Playwright helpers document URL slashes, not OS joins. **Outcome: clean.**

Memory patterns hit: behavioral tests for preflight-not-success; Playwright companion for WebUI screen; product-docs for user-visible chrome; no THRASH_PATH overlap (`paths.ts`, `developer/messages.ts`, `rest.md`, `admin/index.md`).

## Issues

None blocking.
