# Erlang review — 989 startup / login / UI hardening

| Field | Value |
|-------|-------|
| **Branch** | `989-react-cui-widget-builder` |
| **Scope** | Uncommitted package-install, login, audit, role catalog, WebUI path, modern API fixes |
| **Recommendation** | **approve** (with follow-up for incomplete modern Home/WB UI) |
| **May commit/push** | yes |

## Summary

Hardens package install (filters, deploy TX, PSConfig version, file-asset column migration, category lock release) and login path (audit file reuse, encodeURL, redirect sanitize, role Attribute skip). WebUI fixes for lang3 JSP imports, `/cm/app` asset roots, modern client `text/plain` + context-aware `/services`. Behavioral unit tests added for migration, locks, catalog attributes, FileCreator, login sanitize.

## Gate

- **Bugs:** none remaining in this diff that block commit  
- **Tests:** present for non-trivial logic (filter utils, file asset migration, category lock, backend cataloger attributes, FileCreator reuse, login redirect sanitize)  
- **Cross-platform paths:** FileCreator/category marshaller use `Path`/`Files`; deploy prop path uses `Path.of`; OK  

## Follow-up (not gate)

Modern Home + Widget Builder React shells still incomplete product-wise (header chrome, full dashboards, polish). Spec/feature follow-up recommended.

## Memory patterns hit

- Hibernate @Version / UnexpectedRollbackException on package install  
- Outer-join empty XML shells must not hard-fail catalogs  
- Servlet encodeURL (not encodeUrl) under Jakarta  
- text/plain REST flags must not force JSON.parse  
