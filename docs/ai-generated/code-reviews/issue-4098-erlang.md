# Erlang review — issue #4098 (UI-05 display format allowed communities)

**Branch:** `feat/issue-4098-df-communities`  
**Scope:** uncommitted UI-05 write path vs `HEAD` (cluster + #4107 persist).  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** incomplete change-class closure (REST list bind + JDBC persist companions); CXF UNWRAP empty-list omit; JAXB one-element list unwrap.

## Summary

Admin can set allowed communities on a **user** display format in Developer Display Format detail and persist via existing PUT. Packaged formats stay read-only. Empty array / All communities is one persist state (`sys_community=-1`). No new REST resource.

Companions present: rest DTO + JsonReader + jaxrs provider; sitemanage adaptor (400/403); system JDBC property replace + GET hydrate; SPA editor + Vitest; Playwright H2 surface; product-docs 8.2 admin + REST.

## Issues

None that block commit.

### Notes (not gates)

- Live CXF GET still unwraps a one-element `allowedCommunities` list to a single object; SPA/Playwright normalize that (same pattern as columns).
- GET all-communities may omit the field; clients treat omit/empty as All (documented one persist state).
- `DF_GAP_COMMUNITIES` remains in `messages.ts` but is unused on this panel path.

## Cross-platform path checklist

- JDBC persist uses `PreparedStatement` / `?` binds; no filesystem path joins.
- Beans.xml registration test uses `Path.of` / `Files.readString`.
- Playwright / QA scripts already have Windows counterparts.

## Tests / C5

- `system` clean install BUILD SUCCESS (persist tests include restrict/replace/load).
- `rest` clean install BUILD SUCCESS (JsonReader empty array vs omit).
- `projects/sitemanage` clean install BUILD SUCCESS (write 400/403/empty/sentinel).
- `WebUI` clean install BUILD SUCCESS (3687 Vitest).
- Playwright `tests/developer-display-format-communities.spec.js`: 2 passed on H2 QA (`TEST_CMS_URL=http://127.0.0.1:9993`). console-clean=yes. server.log: no feature ERROR/FATAL in the test window.

## Re-review

N/A (first durable report for this slice).
