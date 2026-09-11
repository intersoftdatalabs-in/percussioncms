# Erlang review — issue #4447 item-exit apply-when write

Independent of the implementer. Scope: GET/PUT `.../itemExits` apply-when on
item-level translations/validations, SPA item-exits chrome, Playwright,
product-docs.

## Verdict

Pass for commit/PR. No bug, missing behavioral tests, or non-portable path I/O
in this diff.

## Change class

Workbench-replacement vertical slice: REST DTO (`ContentTypeItemExit.applyWhen`),
sitemanage adaptor persist on `PSConditionalExit`, WebUI chrome, Playwright,
`product-docs/8.2`.

## Findings

None that block. Apply-when is on `PSConditionalExit` (input/output translations
and validations). Empty `applyWhen` on PUT clears; omit preserves matching GET
rows. Invalid operators / empty required conditionals / `type=reference` are 400.
Pipe pre/post exits ignore `applyWhen`. `CT_ITEM_EXIT_CONDITIONS` gap removed.
Field-rule apply-when and locale format-profile untouched.

## Tests / docs

Adaptor write/clear/reject tests; rest Jackson + resource 400; SPA parse/PUT/lock
tests; Playwright GET round-trip + SPA apply-when. Product-docs REST + Developer
Content Types updated.

## Cross-platform

REST paths are URI `/` (correct). Playwright uses `page.request` URLs. No new
filesystem joins.
