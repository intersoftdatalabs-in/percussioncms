# Erlang review — #3377 empty ACL runtime columns + Template shell isolation

**Branch:** `fix/issue-3377-empty-acl-runtime-template-shell`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** isolate SPA section throws (Admin #3195 peer); empty-state tables must keep headers; product-docs companion for operator ACL chrome.

## Summary

Object ACL skipped the permission table when `draftEntries.length === 0`, so Runtime headers only appeared after a draft row. Template (or missing-field) render throws replaced the whole Developer route via `RouteErrorBoundary` ("Unable to load Developer").

This change always renders Design + Runtime headers for a loaded ACL (runtime columns still gated by `shouldShowRuntimeAccessColumns(kind)` without requiring `forceShow` from existing bits), isolates Template/Developer sections with an error boundary peer of Admin, and hardens Template detail apply against envelope/missing fields.

## Cross-platform path checklist

Not applicable — no new filesystem path construction. Playwright URLs use `/` (URL paths). Docker copy in C5 uses container POSIX paths only.

## Issues

None (bug-class). Behavioral tests cover empty ACL Runtime Visible, Template load error remaining in-panel, and Developer shell surviving a TemplatesPanel throw.

## Suggestion (non-blocking)

`DeveloperSectionErrorBoundary` wraps every Developer tab. That is slightly broader than the Template-only acceptance line; it matches Admin isolation and is the right change-class companion for "do not crash Developer."
