# Erlang review — `fix/audit-log-truncate-nonstring`

**Reviewer:** Erlang Shen (independent; did not author this change)  
**Date:** 2026-08-15  
**Scope:** uncommitted vs `HEAD` on `fix/audit-log-truncate-nonstring`.  
**Memory patterns hit:** structural/token tests vs behavior; UI crash from untyped wire values.

## Summary

`SecurityAuditLogViewer.truncate` assumed `string` and called `.trim()`. Live wire `target` is sometimes a number (content-id) or wrapped object. That throws `e.trim is not a function` and `AdminSectionErrorBoundary` replaces the tool even though REST `AUDIT_VIEW` succeeded.

Fix: `auditCellText(unknown)` coerces primitives / `{value,text,name,label}`; `truncate` uses it. Table cells and detail rows go through the same helper. Vitest covers number/`{value}` and a render that must not throw. Playwright asserts `admin-section-error` is absent.

## Recommendation

approve

## Gate

- **May commit/push: yes** (feature branch)
- Bugs: none remaining for this throw
- Behavioral tests: Vitest + Playwright assertion
- Playwright companion: updated `admin-security-audit-log.spec.js`
- Agent rule files: none
- Cross-platform: **N/A** (no path I/O)

## Tests

`npm run test -- SecurityAuditLogViewer` (WebUI frontend): 10 passed.
