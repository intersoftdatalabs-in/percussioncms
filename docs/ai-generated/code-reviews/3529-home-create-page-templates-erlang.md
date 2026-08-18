# Erlang review — #3529 Home Create Page template dropdown

**Scope:** uncommitted vs `HEAD` on `fix/issue-3529-create-page-template-dropdown`  
**Persona:** independent pre-commit (Erlang)  
**Memory patterns hit:** Jackson ArrayList `{empty:false}` bean (#3368); WebUI Playwright companion; product-docs for user-visible Home Create; unwrap envelope + field aliases.

## Summary

Home → Create Page left Template on “Select…” because `fetchTemplatesForSite` only accepted a `TemplateSummary` root and mapped empty `id`/`name` when Jackson 3 serialized `PSTemplateSummaryList` as an ArrayList bean. The change unwraps real envelopes (array, `TemplateSummary` / `TemplateSummaryList`, rest `templateId`/`templateName`), drops empty-bean rows, falls back to `loadPageTemplates` (`percPage` allowed templates), and marks the sitemanage list as `@JsonFormat(ARRAY)` (peer of `SiteList`).

## Recommendation

**approve**

## Gate

**May commit/push: yes**

- Behavioral unit tests: `unwrapTemplateSummaries` / `fetchTemplatesForSite` envelopes; PageWizard selects a site and lists options from the live envelope; empty-bean falls back to allowed templates; `PSTemplateSummaryListJacksonTest` asserts array wire, not `{empty:false}`.
- Playwright: `tests/bugs/bug-3529-home-create-page-templates.spec.js` (`@home` `@page-wizard`) passed on H2 QA after hot-deploy.
- Product-docs: `product-docs/8.2/admin/index.md` Home → Create page template step.
- Cross-platform path checklist: no new filesystem path construction (N/A). URLs use `/` correctly.

## Issues

None that block commit.

### Notes (not gates)

- `qa-health` still reports FastForward `PSDbStorageService` import ERRORs and search-index date parse noise on H2 silent install. Those predate this change; HTTP 200 / `HEALTH:healthy`; no `sitetemplates` / `TemplateSummary` ERRORs in the test window.
- `perc-qa-automation` is spec-only (no Maven source change); Playwright is the required companion for this WebUI screen.
