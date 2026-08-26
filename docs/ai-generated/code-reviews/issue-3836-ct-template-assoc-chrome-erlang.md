# Erlang review — #3836 CT template-assoc chrome (Cycle Verify residual)

**Date:** 2026-08-26  
**Branch:** `fix/issue-3836-ct-template-assoc-chrome`  
**Scope:** uncommitted vs `HEAD` (WebUI ContentTypeDetailPanel, Vitest, Playwright, product-docs)  
**Memory patterns hit:** change-class closure (Vitest + Playwright for WebUI screen); always-mount chrome before GET (peer #3834 lock toolbar); no path I/O.

## Summary

Cycle Verify on cluster tip #3833 failed Playwright
`developer-content-type-template-associations.spec.js`: template add-name was
enabled while unlocked, and Admin lock/replace timed out waiting for
`developer-ct-detail-name`. Cluster union gated name / templates / lock
toolbar on GET `detail` and placed Lock after the percPage fields table.

This residual always mounts sticky Lock/Save/Unlock/Enabled, the type name
(`detail?.name || idOrName`), and allowed-template add/remove at the top of
the panel. `canEdit` is `heldLock && !busy && detail != null`. Unlocked (and
still-loading) template add is `disabled` + `readOnly` + `aria-disabled`.
409 lock does not steal or enable editors. Save still uses dedicated
`PUT .../allowedTemplates` and does not release the lock.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

None blocking.

### Companions (checked)

| Artifact | Present |
|----------|---------|
| Always-mounted name + templates + sticky toolbar | yes |
| Vitest: loading disabled + 409 no steal | yes |
| Playwright surface spec waits for lock enabled / add disabled | yes |
| product-docs/8.2/admin/developer-content-types.md | yes |
| Cross-platform path I/O | N/A (no filesystem paths) |

### Tests

- Vitest: WebUI `ContentTypeDetailPanel` loading toolbar/templates; 409 template add stays disabled; existing CD-12 PUT/GET cases.
- Playwright: `npm run test:surface -- --path tests/developer-content-type-template-associations.spec.js` — 2 passed on H2 QA.

## Hard bans

- No exploits / malware.
- No rule-file commits.
- No non-portable path construction.
