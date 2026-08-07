# Erlang review — #2241 Control setting: inline link title field name

**Date:** 2026-08-07  
**Branch:** `fix/issue-2241-inline-link-title-field-setting`  
**Parent:** #946  
**Reviewer persona:** Erlang (pre-commit gate)

## Summary

Adds `InlineLinkTitleField` control parameter to `sys_tinymce` (ControlMeta + CE XSL init), registers TinyMCE option `inlineLinkTitleField` (empty default = product field defaults), and unit tests for control-meta parse + `PSControlRef` XML round-trip of the param.

## Scope

| Path | Change |
|------|--------|
| `system/cms/.../sys_Templates.xsl` | ControlMeta param + XSL variable + `perc_tinymce_init` wire |
| `modules/perc-tinymce/.../rxinline/plugin.js` | `editor.options.register("inlineLinkTitleField")` |
| `modules/perc-tinymce/.../percadvlink/plugin.js` | same registration (CMS insert plugin) |
| `modules/perc-tinymce/.../percadvimage/plugin.js` | same registration |
| `system/src/test/.../controlmeta.xml` | fixture peer param |
| `system/src/test/.../PSInlineLinkTitleFieldControlParamTest.java` | meta + persist round-trip |

**Out of scope (by design, residual slices):** runtime title resolve (#2242), Playwright (#2243).

**Memory patterns:** none hit (no path I/O, no Spring beans, no new REST surface).

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Cross-platform path review

N/A — no new file I/O, path joins, or path assertions. XSL/JS only use existing relative web paths.

## Issues

None (bug/suggestion/nit empty).

### Notes (non-blocking)

- Duplicate `options.register` for `inlineLinkTitleField` in three plugins is intentional peer style so TinyMCE 6 does not strip the option depending on which plugins load; #2242 can `editor.options.get("inlineLinkTitleField")` from percadvlink/image without extra wiring.
- Empty string default (not `displaytitle` string) matches product acceptance: empty/absent → displaytitle/resource_link_title at resolve time (#2242).

## Gates evidence

- `modules/perc-tinymce`: `mvnw clean install` — BUILD SUCCESS  
- `system`: `mvnw clean install -DskipITs` — BUILD SUCCESS (1144 tests, 0 failures; includes `PSInlineLinkTitleFieldControlParamTest` 4/4)  
- Focused: `-Dtest=PSInlineLinkTitleFieldControlParamTest` — 4 tests green  

---

> Co-Authored by Grok Build using grok-4.5 with agent main.
