# Erlang review — issue #3786 Content Type control property values REST (CD-07)

**Branch:** `fix/issue-3786-content-type-control-property-values`  
**Date:** 2026-08-25  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + wire DTOs + Spring stub + Mockito resource tests + sitemanage apibridge + adaptor tests + product-docs); no path I/O.

## Summary

Thin REST GET/PUT for Content Type field control property **values** and choice catalogs. PUT requires a held design-session lock (409 without). Companions match rest/sitemanage AGENTS.md. Standalone `clean install` green on `rest` and `projects/sitemanage`.

## Issues

None blocking.

## Cross-platform path checklist

N/A — no filesystem path construction.

## Tests

- rest: Mockito GET/PUT/404/409/400; Spring stubs implement new methods; JSON includes `controlProperties` values
- sitemanage: mapping helpers, GET values/choices, PUT persist, 409 lock, 400 invalid catalog, 404 field
