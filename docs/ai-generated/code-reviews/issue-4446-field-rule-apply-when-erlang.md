# Erlang review — issue #4446 field-rule apply-when write

Independent of the implementer. Scope: GET/PUT `.../fields/{fieldName}/ruleExpressions` apply-when, SPA field-rules chrome, Playwright, product-docs.

## Verdict

Pass for commit/PR. No bug, missing behavioral tests, or non-portable path I/O in this diff.

## Change class

Workbench-replacement vertical slice: public REST DTO + resource docs, sitemanage adaptor persist, WebUI chrome, Playwright, `product-docs/8.2`.

## Findings

None that block. Apply-when is on `PSFieldValidationRules` (not per validation `PSRule`). Empty `applyWhen` on PUT clears; omit preserves. Invalid operators / `type=reference` are 400. Unknown field PUT is 404. `CT_FIELD_RULE_APPLY_WHEN` gap removed. Item-exit apply-when and locale format-profile untouched.

## Tests / docs

Adaptor write/clear/omit/reject tests; rest resource 404 + Jackson applyWhen; SPA parse/PUT/lock tests; Playwright set/clear GET round-trip on QA H2 (3/3). Product-docs REST + Developer Content Types updated.

## Cross-platform

REST paths are URI `/` (correct). Playwright uses `page.request` URLs. No new filesystem joins.
