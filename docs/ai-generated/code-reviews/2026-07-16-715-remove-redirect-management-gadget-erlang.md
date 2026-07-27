# Erlang Review — 715-remove-redirect-management-gadget

**Date**: 2026-07-16  
**Reviewer**: Erlang (strict independent pre-PR)  
**Scope**: Uncommitted changes on `715-remove-redirect-management-gadget` vs `origin/development`

## Summary

Removes the discontinued **Redirect Management** dashboard gadget from `GadgetRegistry.xml` (issue #715). Legacy `perc_website_config_gadget` assets are already absent from the tree; the registry entry was the remaining product surface that kept the name visible (including under Deprecated after #794). Behavioral unit test asserts the name is no longer registered. Scope correctly does **not** delete the separate redirect-management REST path / `PercRedirectHandler` (page-move redirect rules), which is not the gadget.

## Scope

- Base: `origin/development`
- Head: uncommitted on `715-remove-redirect-management-gadget`
- Files: 2 changed (`GadgetRegistry.xml`, `GadgetRegistryTest.java`)

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Issues

### Issue 1 -- Severity: nit

- File: `WebUI/.../GadgetRegistry.xml` (XML comment)
- Description: In-file comment is useful for archaeology; optional to drop if style prefers no narrative XML comments.
- Suggestion: Keep as-is for #715 traceability.

### Issue 2 -- Severity: suggestion (out of scope / product note)

- Description: Users who already saved the gadget on a personal dashboard may still hold a stale URL in metadata and see a blank chrome until they remove it.
- Suggestion: Accept for this PR unless product wants a metadata cleanup migration (not required by current AC).

## Positive notes

- Test fails on pre-fix registry (would assert Deprecated) and passes post-fix.
- No drive-by deletion of redirect service / REDMGT paths (distinct feature).
- Aligns with historical removal in `c09957048e` and supersedes #794 deprecation-only approach.

## Handoff

Safe to commit and push / open PR against `development`.
