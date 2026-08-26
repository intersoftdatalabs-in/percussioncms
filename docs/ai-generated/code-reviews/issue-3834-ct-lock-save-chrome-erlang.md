# Erlang review — issue 3834 CT lock/save chrome

**Scope:** uncommitted + `fix/issue-3834-ct-lock-save-chrome` vs cluster tip
`cluster/night-issue-20260826-ct-chrome` @ `8229288b11` (stacked; parent #3781 / epic #1690).
**Recommendation:** approve
**Gate:** May commit/push: yes
**Memory patterns hit:** behavioral tests for changed chrome; Playwright companion for WebUI screens; product-docs for operator-facing toolbar placement.

## Summary

Cycle Verify residual: H2 Playwright `developer-content-type-lock-save.spec.js` failed because lock toolbar / Enabled chrome were not in the first paint (toolbar lived after the fields table, gated on GET `detail`). 409 other-user lock could appear to leave Enabled interactive if the checkbox was missing or not lock-gated on the loaded bundle.

This change always mounts `developer-ct-lock-toolbar` (Lock / Save / Unlock / Enabled) at the top of Content Type detail, sticky, as soon as the panel shell mounts. Enabled stays `disabled` until `heldLock && !busy && detail != null`. 409 lock keeps `heldLock` false; onChange is no-op without `canEdit`. Playwright helper waits for toolbar + enabled-disabled + Lock enabled. Vitest covers loading-state chrome and 409 Enabled stays disabled. Product-docs note the top-of-panel toolbar.

## Issues

None (hard-gate).

## Cross-platform path checklist

Not applicable: no filesystem path construction, installers, or path assertions. Playwright URLs and REST paths correctly use `/`.

## Companions

- Vitest: loading toolbar/enabled; 409 lock does not enable Enabled
- Playwright surface spec helper waits for lock chrome
- product-docs/8.2/admin/developer-content-types.md (toolbar placement / 409 Enabled)
- Maven: `WebUI` and `modules/perc-qa-automation` standalone clean install
- C5: `perc-devctl qa-up --skip-image-build` TEST_CMS_URL=http://127.0.0.1:60026; qa-health RESULT:OK HTTP:200 HEALTH:healthy; docker cp rest + perc-system + sitemanage + WebUI `cm/modern/assets`; in-cell StopJetty/StartJetty; qa-health again RESULT:OK HTTP:200 HEALTH:healthy; `npm run test:surface -- --path tests/developer-content-type-lock-save.spec.js` **3 passed**; console-clean=yes (spec pageerror/console); server.log-clean=yes (no ERROR/FATAL in test window)
