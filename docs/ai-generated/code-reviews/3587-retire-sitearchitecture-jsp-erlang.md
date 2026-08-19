# Erlang review — #3587 retire leftover siteArchitecture.jsp host

**Branch:** `fix/issue-3587-retire-sitearchitecture-jsp-host`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** missing behavioral tests (covered); WebUI Playwright companion; product-docs companion; dual-tree lockstep; URL paths correctly use `/`

## Summary

Classic Architecture JSP hosts were already redirect stubs (#3099). This slice removes them from the shipped WebUI WAR and keeps `/cm/app/siteArchitecture.jsp` and `?view=arch` bookmarks landing on SPA Navigation.

`*.jsp` on Jetty still maps missing JSP files to the JSP servlet (404). An **exact servlet mapping** (`PSRetiredJspRedirectServlet`, view=`arch`) wins over the extension mapping and 301s via `PSLegacyViewRedirect` + context prefix `/Rhythmyx`. Path-filter 301 is retained as defense in depth. Dual-tree JSPs and the residual `WebUI/war/app` copy are deleted.

## Issues

None blocking.

## Notes

- Location sanitization reuses `PSLegacyViewRedirect` (drops markup / CR/LF; forces `view=arch`).
- `withContextPath` is public so the servlet can prefix `/Rhythmyx` (Playwright proof: unprefixed `/cm/app/` 404s the CMS error page).
- Cross-platform path checklist: N/A for filesystem I/O; URL/WAR paths correctly use `/`. Vitest `read()` already normalizes CRLF.
- Playwright `architecture-legacy-redirect.spec.js`: 2 passed on H2 QA after servlet deploy; pageerror/console-error gates empty.
- Server.log `No content type info found for content type id: 315` appears on stock Navigation load (including `?view=arch`); not introduced by this redirect.

## Tests / companions

- Java: `PSWebUiSpaFallbackFilterTest` retired-JSP + context prefix
- Vitest: `spaCutover.test.ts` asserts JSP hosts gone + servlet mapping in `web.xml`
- Playwright: bookmark URL + `?view=arch` → `perc-architecture-shell`
- product-docs: host-retirement row **Available**
