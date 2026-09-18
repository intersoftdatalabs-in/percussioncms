# Erlang review — #4547 Explorer schedule publish dates

**Scope:** `fix/issue-4547-explorer-schedule-dates` vs `origin/main` (uncommitted WebUI / perc-qa-automation / product-docs).
**Persona:** independent pre-PR review (Erlang).
**Memory patterns hit:** change-class closure (WebUI screen → Vitest + Playwright + product-docs); HTTP 200 application-level `FORBIDDEN`/`BADCONFIG`/`INVALID` must not look like success; URL paths correctly use `/`.

## Summary

Content Explorer injects a **Schedule** action for page/asset selection (same `resolvePublishKind` as Publish Now / Take Down / Stage). The SPA loads classic `GET /services/itemmanagement/item/getitemdates/{id}`, opens a React dialog (set/clear dates + comments), confirms, then `POST …/setitemdates` with `{ ItemDates: { itemId, startDate, endDate, comments } }`. Paths are Explorer-local (not `itemPublishPaths`) to avoid colliding with PublishingShell #4537. Folders/non-publishable types stay hidden/unavailable. `mapPublishResponse` treats HTTP 200 preflight statuses as failures.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Change-class closure

| Companion | Status |
|-----------|--------|
| SPA action enablement + dispatch | Present |
| Behavioral Vitest (parse/convert, GET/POST, FORBIDDEN/BADCONFIG/INVALID, folders, dialog clear/same-dates, shell error chrome) | Present |
| Playwright `@explorer-schedule` on explorer-action-dispatch | Present |
| `product-docs/8.2/admin/content-explorer.md` + publishing pointer | Present |
| rest/sitemanage façade | N/A — existing sitemanage item dates APIs |

## Cross-platform path checklist

- No new filesystem path joins. Service URLs use `/` (correct for URI).
- Date conversion uses local `Date` calendar fields (classic Finder contract), not OS paths.

## Issues

None blocking.

### Suggestion (non-blocking)

Server `dateValidation` also rejects start/end in the past. The SPA does not preflight that (classic dialog was similar). Operators still see the REST error in Server actions chrome.

## Tests

- `itemScheduleDates.test.ts` — JAXB unwrap, datetime round-trip, range validation, GET URL, POST envelope, HTTP 200 FORBIDDEN/BADCONFIG/INVALID, folder no-op
- `ScheduleDatesDialog.test.tsx` — save, clear, same-dates block, cancel, Escape, a11y
- `actionEnablement` / `actionDispatch` / `ContentExplorerShell` peers updated for Schedule
- Playwright: Schedule hidden until page selected; POST FORBIDDEN → `explorer-server-actions-error`
