# Erlang review — EditorHost check-out / check-in (#4603)

Independent of implementer. Scope: EditorHost lock chrome, itemmanagement checkIn/checkOut status mapping, Playwright surface, product-docs.

## Verdict

**Pass** for commit/PR of this slice.

## Bugs

None found. 403/409 are mapped in the host and not treated as success. Fields stay view-only when the session user does not hold the lock. Check-in REST maps `PSItemWorkflowServiceException` to HTTP 409.

## Tests

- Vitest: `editorCheckout.test.ts`, `EditorHost.test.tsx` lock/403/409 cases.
- JUnit: `PSItemWorkflowServiceCheckInConflictTest`.
- Playwright: `editor-host-checkout.spec.js` (QA H2 surface).

## Paths

Playwright helpers use `/` only in CMS URL path regexes. No OS filesystem concatenation.

## Companions

WebUI + sitemanage + perc-qa-automation + product-docs/8.2. REST public JAX-RS surface unchanged (sitemanage itemmanagement).

## Notes

`GET checkOut` still returns 200 with `ItemUserInfo` when another user holds the lock (legacy WorkflowActionsPanel). The editor treats that as view-only, not success.
