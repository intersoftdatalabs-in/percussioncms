# Erlang review — #4628 PublishingShell incremental Playwright + TMX

Scope: uncommitted vs HEAD on `fix/issue-4628-incremental-publish-playwright-tmx`.

## Summary

Removes the disabled-Incremental early return in Playwright, stubs publish servers so confirm/job/status always run, adds CmsUi.tmx companions for two MSG keys, unit-tests helper matchers, and a one-line product-docs update. No production path I/O.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (bugs / missing behavioral tests / non-portable paths).

Memory patterns hit: Playwright skip-as-pass; i18n change-class TMX companions.

Cross-platform path checklist: N/A (URL regexes, no filesystem joins).
