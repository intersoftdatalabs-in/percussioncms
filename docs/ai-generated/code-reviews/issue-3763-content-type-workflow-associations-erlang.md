# Erlang review: issue #3763 Content Type workflow associations PUT REST (CD-08)

| Field | Value |
|-------|--------|
| **Date** | 2026-08-23 |
| **Branch** | `feat/issue-3763-content-type-workflow-associations` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | May commit/push: yes |
| **Memory patterns hit** | Change-class closure (rest DTO + interface + resource + Mockito + Spring stub + sitemanage impl/tests + product-docs); typed 409 vs message-substring 409; exact implementors of `IContentTypesAdaptor` |

## Summary

Dedicated Admin-only `PUT /services/contenttypes/{idOrName}/allowedWorkflows` replaces allowed-workflow associations under a **held** design-session lock (`IPSSystemDesignWs.isLocked`), validates workflow ids via `IPSWorkflowService`, saves with `release=false`, and returns `ContentTypeDetail` so GET lists the new set. Generic `PUT /contenttypes/{idOrName}` lock-save-unlock is unchanged. No WebUI.

## Gate

- No bugs found in the diff after standalone `rest` and `sitemanage` `mvnw clean install`.
- Behavioral tests cover persist-with-lock, GET reflects set, empty-list clear, unknown workflow 400, 409 no-lock / other user / other session, 403 not Admin, 404, wildcard 400, typed 409 vs generic 500 (`percBlockquote`).
- Companions complete vs rest/sitemanage AGENTS and peer #3760 enable/disable.
- Cross-platform path checklist: N/A (no path/file I/O).
- Product-docs: `product-docs/8.2/developer/rest.md` CD-08 table + status codes.

## Issues

None.

## Notes (non-blocking)

- `ContentTypeDesignLockException` is duplicated vs open PRs #3748/#3749/#3773; same class, merge should be additive.
- GET may list inclusion ids from the content editor when content-mgr associations are empty so PUT/GET immediately after save still shows the new set.
