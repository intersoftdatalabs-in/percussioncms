# Erlang review — #3195 Admin System Tools

**Scope:** uncommitted `fix/issue-3195-admin-system-tools` vs `origin/main`  
**Change class:** WebUI product screen (Admin → System Tools) + Playwright companion + product-docs  
**Recommendation:** approve  
**Gate:** May commit/push: yes (after WebUI `clean install`)  
**Memory patterns hit:** Jackson WRAP_ROOT list envelopes / `.map is not a function`; WebUI Playwright companion; product-docs for operator surface; isolate errors so one tab cannot blank the route

## Summary

Admin → System Tools was blanking the Admin route via `RouteErrorBoundary` (`Unable to load Admin`). The likely class matches #3202: Jackson WRAP_ROOT / one-item list envelopes treated as arrays (`TypeError: map is not a function`), plus possible Instant-shaped `eventTime` rendered as a React child.

This change:

- Coerces audit-log `entries` (and consistency `issues`) to arrays before `.map`
- Unwraps `{ SystemAuditLogEntry: [...] }` / nested `SystemAuditLogPage` envelopes
- Formats `eventTime` as a string always
- Isolates **System Tools** (and each tool) in `AdminSectionErrorBoundary` so a tool crash cannot replace Admin chrome. Administration tabs (workflow/roles/users/categories) are **not** rewritten (PR #3229)

## Cross-platform path checklist

N/A — no new filesystem path I/O. Playwright uses existing `BASE_URL` helpers.

## Issues

None (hard-gate). Behavioral tests added for unwrap envelopes, viewer non-array entries, ConsistencyChecker list coerce, boundary isolation, and Playwright `bug-3195-admin-system-tools.spec.js`.

## Notes

- `AdminSectionErrorBoundary` is extracted so #3229 can share it; this PR wraps only the tools tab in `AdminShell`.
- Viewer still guards `Array.isArray(page.entries)` if a mock/bypass skips unwrap.
