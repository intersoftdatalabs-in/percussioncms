# Erlang review — issue #4440 system-def field control properties

## Summary

Vertical CD-16 slice: Admin GET/PUT `/services/systemdef/fields/{fieldName}/controlProperties`
(wrap root `SystemDefControlProperties`), sitemanage `SystemDefAdaptor` via
`IPSContentDesignWs` request lock released on save, Developer System Def SPA
control-property editor, Playwright surface spec, product-docs.

## Scope

Uncommitted work on `fix/issue-4440-system-def-field-control-properties` vs
`origin/main`. Modules: `rest`, `projects/sitemanage`, `WebUI`,
`modules/perc-qa-automation`, `product-docs/8.2`.

Memory patterns: adaptor Spring stub + Mockito resource tests; request lock
(not content-type held lock); WRAP_ROOT; path-safe names; no REST/SPA split.

Cross-platform path review: `isSafeFieldName` rejects `..` `/` `\` NUL as
**path-injection filters**, not filesystem joins. Playwright uses
`encodeURIComponent` on URL path segments. No new OS path concatenation.

## Recommendation

approve

## Gate

May commit/push: yes

No blocking bugs. Behavioral tests cover GET/PUT 200/400/403/404/409,
unsafe names, lock release, SPA save. Playwright 3/3 on QA H2 after
matching `perc-system` + rest + sitemanage WAR deploy.

## Issues

None blocking.
