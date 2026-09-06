# Erlang review: #4359 WebUI App.test.tsx publish-shell timeout

**Branch:** `fix/issue-4359-webui-publish-shell-timeout`  
**Scope:** uncommitted `WebUI/src/test/ts/app/App.test.tsx` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Vitest `it()` default 5s vs RTL `findBy` wait; do not skip flaky UI tests.

## Summary

Cycle-verify residual: `shows publish nav for designer and loads PublishingShell` failed with `Test timed out in 5000ms` at the `it()` while `findByTestId('publishing-shell')` used `SHELL_TIMEOUT=8000`. That is a test-budget mismatch, not a PublishingShell product hang.

The change raises the App-shell suite timeout (`describe` options + explicit third-arg timeout on the publish case) to `SHELL_TIMEOUT + 4000` so RTL waits can complete under full-suite load. No product TS/Java/WAR dual-ship.

## Issues

None (bugs).

### Nits

None that block. Other `it()`s in the same describe inherit the suite timeout; only the cycle-failing case also sets the third argument.

## Cross-platform path checklist

N/A — no file I/O or path construction.

## Change-class companions

Test-only Vitest timeout. Peers: same file already used `SHELL_TIMEOUT` on `findBy`. No Playwright/product-docs (no user-visible change).

## Evidence

- Focused: `npx vitest run ../../test/ts/app/App.test.tsx` — 13 passed
- C1: `cd WebUI && ../mvnw clean install` (JDK 21) — BUILD SUCCESS; Vitest 439 files / 4078 tests passed
