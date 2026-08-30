# Erlang review — issue #4015 REST AS-01 slot finder/relationship write

**Branch:** `fix/issue-4015-slot-finder-relationship-write`  
**Base:** `origin/main`  
**Date:** 2026-08-30  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Admin REST write of slot `finderName`, `relationshipName`, and `finderArguments` on existing
`PUT /services/slots/{idOrName}`, plus `POST .../lock` and `POST .../unlock` so the held-lock
contract is usable. Persistence is `IPSAssemblyDesignWs` load/save (no new SOAP). Invalid
finder is 400, unknown slot 404, unlocked/other locker 409, non-Admin 403. GET detail
round-trips. `SLOT_FINDER_RELATIONSHIP_WRITE` is retired. Product-docs 8.2 Developer REST
updated.

Memory patterns hit: change-class closure (rest resource + adaptor interface + Spring stub +
sitemanage impl + adaptor tests + product-docs); behavioral tests for success/403/409/400;
no path I/O.

## Recommendation

approve

## Gate

**May commit/push: yes**

No bugs found. Behavioral tests cover finder write, lock/unlock, invalid finder, unknown
relationship, unlocked 409, other locker 409, non-Admin 403. Spring test stub updated.
Standalone `rest` and `projects/sitemanage` `clean install` BUILD SUCCESS.

Cross-platform path checklist: N/A (no filesystem path construction).

## Issues

None (hard-gate).

## Notes (non-blocking)

- Label-only PUT still acquires+releases a lock in one request (existing behavior). Finder
  write requires a previously held lock and keeps it (`saveSlots(..., release=false)`).
- SPA slot-editor chrome remains out of scope (parent #1690 remainder).
- `ISlotsAdaptor` gained `lockSlot` / `unlockSlot`; implementors grepped (SlotsAdaptor +
  TestSlotsAdaptor only). Downstream `sitemanage` clean install green.
