# Erlang review — issue #4439 shared-field nested field and control editor

## Summary

Vertical CD-15 slice: Admin SPA add/delete nested fields on Developer → Shared
Fields detail and GET/PUT control properties (omit `choices` unchanged;
`type: none` clears) using existing REST. Playwright surface spec + product-docs.

## Scope

Uncommitted work on `fix/issue-4439-shared-field-nested-editor` vs `origin/main`.
Modules: `WebUI`, `modules/perc-qa-automation`, `product-docs/8.2`,
`projects/sitemanage` (proven contract gap: duplicate/not-found 409/404 after
`loadSharedDefLocked` leaked the request lock; `withLockedWrite` always
releases via save). REST wire paths already shipped.

Memory patterns: WRAP_ROOT (`SharedField`, `SharedFieldControlProperties`);
request lock released on save (adaptor already); path-safe names; no REST/SPA
split; Vitest for wrap/unwrap + panel add/delete/save; Playwright on QA H2.

Cross-platform path review: client `isSafeGroupName` / `SHARED_FIELD_NAME_PATTERN`
reject `..` `/` `\` NUL as **path-injection filters**, not filesystem joins.
Playwright uses `encodeURIComponent` on URL path segments. No new OS path
concatenation.

## Recommendation

approve

## Gate

May commit/push: yes after WebUI `mvnw clean install` and C5 Playwright.

No blocking bugs in the SPA/API layer. Behavioral tests cover field-name
validation, WRAP_ROOT add/delete/controlProperties, duplicate 409, omit vs
`choices.type=none`, in-app field delete confirm.

## Issues

None blocking.
