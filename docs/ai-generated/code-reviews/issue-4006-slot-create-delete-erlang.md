# Erlang review — issue #4006 REST AS-01 slot create/delete

**Branch:** `feat/issue-4006-slot-create-delete`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class completeness (rest resource + adaptor interface + Spring test stub + sitemanage impl + adaptor tests + product-docs); Admin 403 / duplicate 409 peers from content-type POST/DELETE

## Summary

Admin REST `POST /services/slots` and `DELETE /services/slots/{idOrName}` via existing `IPSAssemblyDesignWs.createSlots` / `saveSlots` / `deleteSlots`. No new SOAP. `SLOT_CREATE_DELETE` retired; remainder gap `SLOT_FINDER_RELATIONSHIP_WRITE`. No SPA; no finder/relationship write.

## Gate

- Bugs: none found
- Behavioral tests: rest Mockito resource (32) + Spring `TestSlotsAdaptor` stub + sitemanage adaptor create/delete (17)
- Cross-platform paths: N/A (no filesystem I/O)
- Change-class companions: complete

## Issues

None.

## Build

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 769, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1729, Failures: 0
- Downstream: `ISlotsAdaptor` added methods; only implementers are `SlotsAdaptor` and `TestSlotsAdaptor` (both updated); sitemanage clean install green
