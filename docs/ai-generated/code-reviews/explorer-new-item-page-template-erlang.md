# Erlang review — feat/explorer-new-item-page-template

**Date**: 2026-08-15  
**Scope**: uncommitted vs `origin/main` (`9927d427c3` New Item #3453)  
**Reviewer**: Erlang  
**Prior report**: `docs/ai-generated/code-reviews/explorer-new-item-erlang.md` (suggestion: percPage via `contentItemDao` may fail without a template — this increment addresses that)  
**Memory patterns hit**: change-class completeness (sitemanage page create + WebUI picker/dispatch + Playwright + product-docs); behavioral tests for template pick / no-template / page-service save; CMS repository paths use `/` (not OS I/O)

## Summary

Explorer New Item for `percPage` loads allowed templates (content type, then site), auto-selects a single template, or opens **Choose a page template**. Create POSTs `templateId` and `PSItemService` saves through `IPSPageService` instead of a bare `contentItemDao` item.

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Cross-platform path review

CMS folder helpers (`toRepositoryFolderPath`, `siteNameFromFolderPath` Java/TS) treat repository paths as `/` URLs and normalize `\`. No OS filesystem joins. Clean.

## Change-class closure

| Companion | Status |
|-----------|--------|
| sitemanage create + page save | yes |
| WebUI picker + dispatch | yes |
| Vitest / JUnit behavioral tests | yes |
| Playwright (`explorer-content-editor.spec.js`) | yes |
| product-docs 8.2 admin + REST | yes |

## Issues

None blocking.

### Suggestion

`loadPageTemplates` swallows content-type catalog errors and falls through to site templates. Intentional; a log would help operators when the type GET fails for a real reason.

Playwright skips when **New** has no `percPage` child. Acceptable for catalog variance; live H2 with Demo should list it.

`IPSPageService` is `@Autowired(required = false)` to avoid a setter cycle. Missing bean fails create with an explicit message (covered).

## Tests run

- Focused Vitest: 49 passed (`actionDispatch`, `TemplatePickerDialog`, `pageTemplates`)
- Focused sitemanage: `PSItemCreateSupportTest` 5, `PSItemServiceCreatePageTest` 3
- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS; Tests run: 1266, Failures: 0, Skipped: 125
- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS; Vitest 2540 passed
- Playwright live QA stack not started this increment

## Re-review (PR #3455 kilo threads)

**Date**: 2026-08-15  
**Scope**: review-fix pack vs `347fb964b8`

Kilo CRITICAL/WARNING/SUGGESTION on orphaned picker Promise, dialog keyboard, Playwright happy path, catalog-error fallback, silent catch, initial focus.

### Recommendation

`approve`

### Gate

May commit/push: **yes**

### Issues

None remaining. Prior suggestion (silent catch) now `console.warn`s and has a throw → site-template unit test. Picker session replace/settle is unit-tested so a second open or unmount cancels the waiter. Dialog uses `useDialogEscape` + Tab trap + select focus. Playwright posts `templateId` on OK.
