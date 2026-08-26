# Erlang review — issue #3781 Developer Content Type enable/disable chrome (CD-13)

**Scope:** uncommitted work on `feat/issue-3781-ct-enable-disable-chrome-2` vs `origin/main`.
**Change class:** WebUI product screen (Developer Content Type detail) consuming existing REST CD-13 `PUT /services/contenttypes/{idOrName}/enabled`.
**Memory patterns hit:** incomplete change-class closure (Playwright + product-docs); behavioral tests for new helpers; URL `/` paths are URI not filesystem (false-positive guard).

## Summary

The SPA already showed an Enabled checkbox on Content Type detail but saved it on the bulk `PUT /contenttypes/{id}` and `updateContentTypeDetail` still wrapped lock → PUT → unlock (leftover from before lock chrome). Live H2 QA showed dedicated PUT `ContentTypeEnabled.enabled=false` returning `enabled:true` because design save always passed `enable=true` and item-def load never copied the application flag. This change:

1. Adds `setContentTypeEnabled` + `wrapContentTypeEnabledForWire` (`ContentTypeEnabled` Jackson root).
2. Saves enabled only via dedicated PUT after a held lock; bulk PUT omits `enabled`.
3. Stops wrapping lock/unlock on bulk PUT so save keeps the design lock.
4. Design save passes `PSItemDefinition.isEnabled()` into `saveContentType` (CD-13 persist).
5. `loadItemDef` copies `ceApp.isEnabled()`; adaptor GET falls back to object-store after cache miss.
6. Extends Vitest, Playwright lock-save spec (fail closed, GET persist), and product-docs.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (hard-gate).

### Notes (non-blocking)

- Combined save (enabled PUT first, then bulk) can still leave enabled persisted if bulk then fails. Chrome surfaces the error, keeps the description draft, and syncs the Enabled checkbox from the dedicated PUT. No shared rollback.
- Playwright enable test restores the flag in `finally` so a failed assert does not leave `percPage` disabled on a shared H2 QA cell.

## Re-review (PR #3828 Kilo threads)

**Scope:** uncommitted review-response vs `HEAD` on `feat/issue-3781-ct-enable-disable-chrome-2`.
**Change class:** same CD-13 chrome; review-thread mitigations only.
**Memory patterns hit:** behavioral tests for new/changed non-trivial logic; swallowed exceptions must log.

### Summary

Kilo threads on PR #3828:
1. Object-store fallback on `resolveItemDef` is now opt-in (`includeDisabledFromStore`). Only GET detail and `setContentTypeEnabled` pass `true`. Templates / item exits / control properties keep cache-only 404.
2. Combined save runs enabled PUT first so a failed enable cannot leave bulk fields persisted. Bulk failure after enabled success syncs the checkbox from the dedicated PUT and keeps local drafts.
3. `loadItemDefFromObjectStore` debug log includes exception class, message, and throwable stack.

### Recommendation

approve

### Gate

May commit/push: yes

### Issues

None (hard-gate).

### Cross-platform path checklist

- No new filesystem path joins.
- No temp-dir or line-ending assertions.

## Cross-platform path checklist

- No new filesystem path joins (`/` or `\\`).
- New URLs use `/` for REST paths (correct).
- Tests use `encodeURIComponent` for idOrName; Playwright GET uses `BASE_URL` + `/Rhythmyx/services/...` (URI).
- No temp-dir or line-ending assertions.

## Companions

| Companion | Status |
|-----------|--------|
| Vitest API helper (`setContentTypeEnabled`, wrap, no lock wrap on bulk PUT) | yes |
| Vitest panel: disabled until lock, dedicated PUT, combined save order, 409 | yes |
| Playwright surface spec extended; empty catalog fails closed | yes |
| `product-docs/8.2/admin/developer-content-types.md` + rest note | yes |
| REST / sitemanage re-implement | out of scope (CD-13 already shipped) |

## Tests

- `WebUI/src/test/ts/api/developer/contentTypesApi.test.ts`
- `WebUI/src/test/ts/developer/ContentTypeDetailPanel.test.tsx`
- `WebUI/src/test/ts/developer/DeveloperShell.test.tsx` (bulk body omits enabled)
- `modules/perc-qa-automation/frontend/tests/developer-content-type-lock-save.spec.js`
