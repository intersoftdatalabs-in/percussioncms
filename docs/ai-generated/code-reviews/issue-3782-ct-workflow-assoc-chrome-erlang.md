# Erlang review: issue #3782 Developer Content Type workflow associations chrome (CD-08)

| Field | Value |
|-------|--------|
| **Date** | 2026-08-26 |
| **Branch** | `feat/issue-3782-ct-workflow-assoc-chrome` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | May commit/push: yes |
| **Memory patterns hit** | Change-class closure (WebUI API + panel + Vitest + Playwright + product-docs); behavioral tests for dedicated PUT / 409 / unlocked save; REST URL `/` not filesystem paths |

## Summary

Developer Content Type detail consumes held-lock `PUT /services/contenttypes/{idOrName}/allowedWorkflows` (Jackson `ContentTypeWorkflows`) instead of stuffing `allowedWorkflows` onto the generic content-type PUT. Save does not acquire or release the lock. Unlocked editors and Save stay disabled. Template association chrome is unchanged (still bulk PUT when templates are dirty). REST internals are not reimplemented.

## Gate

- No bugs found in the diff after standalone `WebUI` and `modules/perc-qa-automation` `mvnw clean install`.
- Behavioral tests: wrap helper, dedicated PUT (no lock/unlock), panel lock → CD-08 save, 409 clears lock, unlocked save blocked, helper empty-list / default flags.
- Companions: Playwright `developer-content-type-workflows.spec.js`; product-docs Developer Content Types + REST CD-08 SPA note.
- Cross-platform path checklist: N/A for filesystem joins (REST/URL `/` only; Playwright `encodeURIComponent` on type name).
- C2 reverse-deps: none (no Java public type/signature change).

## Issues

None.

## Notes (non-blocking)

- Generic `updateContentTypeDetail` no longer wraps lock→PUT→unlock; lock chrome (#3744) is the session owner. Mixed saves call CD-08 first, then bulk PUT without `allowedWorkflows`.
- Playwright fails closed if the catalog is empty or both stock workflow names are already associated.
