# Erlang review — issue #4647 PublishingShell design edition save

## Summary

Vertical increment: save publish edition from Design. REST 403/409, JAXB wrap on wire, SPA mapping, Playwright H2, product-docs.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None blocking. Behavioral tests cover 403/409 and SPA save/conflict. Paths are URL/REST only (portable). Fail-closed Admin/Designer check when userService unset. Duplicate edition name is 409.

## Cross-platform path checklist

N/A (no filesystem I/O). Playwright uses `path` helpers already in auth.

Memory patterns hit: explicit HTTP 403/409, JAXB root wrap for sitemanage JSON.
