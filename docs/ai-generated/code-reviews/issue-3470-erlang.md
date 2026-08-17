# Erlang review — issue #3470 React editor rich controls

**Branch:** `feat/issue-3470-react-editor-widgets`  
**Date:** 2026-08-16  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

## Summary

Maps content-type controls onto React widgets (TinyMCE/HTML, file, image, keyword,
community) on the chrome-less editor host. Scalar/HTML/keyword/community persist
through itemmanagement fields (`sys_communityid` now editable). File/image persist
through new itemmanagement binary GET/PUT. Revision promote form reuses restore
revision REST. No leftover CE HTML requestors.

## Cross-platform path checklist

- [x] Binary field names restricted to `[A-Za-z][A-Za-z0-9_]{0,79}`
- [x] Upload filenames strip `\`/`/` path segments before sibling metadata
- [x] Temp files via `PSPurgableTempFile` + `Files.newOutputStream(temp.toPath())`
- [x] Tests reject `../img` and Windows/Unix path-shaped filenames
- [x] No hardcoded `/tmp` or OS-only separators in new code

## Issues

None blocking. Behavioral Vitest per widget + mapper/binary unit tests + Playwright
surface `editor-rich-controls.spec.js` (2 passed on H2 QA).

Memory patterns hit: change-class companions (WebUI + sitemanage + Playwright +
product-docs), portable path I/O on binary upload, persist only through
itemmanagement.

## Gate

approve
