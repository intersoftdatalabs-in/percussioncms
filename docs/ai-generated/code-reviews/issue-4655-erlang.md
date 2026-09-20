# Erlang review — #4655 Explorer rename/item HTTP 500

**Scope:** uncommitted branch `fix/issue-4655-explorer-rename-item-500` vs `origin/main`  
**Persona:** Erlang (independent of implementer)  
**Date:** 2026-09-20

## Summary

H2 `POST /rest/folders/rename/item` 500 was `UnexpectedRollbackException` on `loadItems` after `prepareForEdit` (save never ran). Persist now loads without binaries for text assets, then checkout/save/check-in inside `PROPAGATION_REQUIRES_NEW`. `saveItems` uses `checkin=false`. Rollback-only after save is treated like copy/item NewCopy (HTTP 200). Playwright `explorer-rename-item.spec.js` passed on H2 QA.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (bugs).

## Tests

`FolderAdaptorRenameFolderItemTest` asserts `saveItems(..., false, false, folderId)`, page still sets `filename`, simple text asset does not, rollback still maps to `BackendException`, `releaseFromEdit` still called.

## Cross-platform path checklist

N/A — no filesystem path I/O.

## Memory patterns hit

UnexpectedRollbackException after nested workflow JDBC / double check-in (site-create / recycle peers).

## Change-class closure

Server persist + unit tests + product-docs REST row. Playwright spec already exists (`explorer-rename-item.spec.js`); C5 live proof required before PR.

> Co-Authored by Grok Build 1.0.34 using grok-4.6 with agent night-issue-prs.
