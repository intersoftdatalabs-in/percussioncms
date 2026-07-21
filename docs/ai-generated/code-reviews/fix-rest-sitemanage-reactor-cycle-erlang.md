# Erlang review — fix/rest-sitemanage-reactor-cycle

**Date:** 2026-07-21  
**Branch:** `fix/rest-sitemanage-reactor-cycle`  
**Base:** `development`  
**Reviewer:** Erlang (pre-commit / pre-PR gate)

## Summary

Breaks the Maven reactor cycle `rest → sitemanage → rest` introduced by US8 relationship-summary work. Wire DTOs move into `rest`; adaptor implementation moves to sitemanage `com.percussion.apibridge` with `@PSSiteManageBean`; forbidden dependency removed from `rest/pom.xml`. Agent docs updated so the layering is hard to re-break.

## Scope

- `rest/pom.xml` — remove sitemanage dependency
- DTO moves: `share/relationship/data/*` sitemanage → rest (package unchanged)
- `RelationshipSummaryAdaptor` + test: rest → sitemanage apibridge (package `com.percussion.apibridge`)
- Docs: `rest/AGENTS.md`, `projects/sitemanage/AGENTS.md` (new), root `AGENTS.md`, `rest/README.md`
- Prior memory: US8 reviews under `docs/ai-generated/code-reviews/992-react-content-explorer-us8-*.md`; pattern “missing behavioral tests / wrong layer” applicable
- Cross-platform path review: **clean** (no file I/O or path construction in this diff)

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| bug findings | none |
| behavioral tests for moved/changed logic | retained (adaptor test moved with impl; resource + service tests remain) |
| non-portable path/file I/O | n/a — not touched |
| May commit/push | **yes** |

## Issues

_None._

### Notes (non-blocking)

1. **suggestion** — Reactor validate already SUCCESS after dep removal. Prefer also green focused unit tests before merge (`RelationshipSummaryResourceTest`, `RelationshipSummaryAdaptorTest`, `PSRelationshipSummaryServiceTest`).
2. **nit** — Wire DTO package remains `com.percussion.share.relationship.data` under the `rest` module (intentional for import stability / US8 task path). Documented in both AGENTS files; future cleanup could move to `com.percussion.rest.relationsummary` if desired.
3. **nit** — Sample in older rest docs used `@Component` for adaptors; AGENTS now requires `@PSSiteManageBean` in sitemanage apibridge (matches `PreferencesAdaptor` et al.).

## Memory patterns hit

- Layering / wrong-module dependency (rest must not depend on sitemanage)
- Behavioral tests present for AuthZ-empty → 403 adaptor path (tests relocated, not dropped)

## Follow-up fixes included before PR

1. **bug (compile):** `PSRelationshipSummaryService.summariseLocal` multi-catch listed `PSNotFoundException` with `RuntimeException` (subclass) — illegal multi-catch; fixed by catching `RuntimeException` only (covers `PSNotFoundException`).
2. **bug (pre-existing compile, blocks sitemanage):** `PSTaskManagementService` used wrong schedule API names (`NOTIFICATION_TEMPLATE`, `setBody`/`getBody`, nested `NotifyWhen`, Boolean `setNotify`, etc.). Aligned to `SCHEDULE_NOTIFICATION_TEMPLATE`, `setTemplate`/`getTemplate`, `PSNotifyWhen`, `String setNotify`, `getProblemDesc`/`getServer`.

## Test evidence

- `rest`: `RelationshipSummaryResourceTest` 4/4
- `sitemanage`: `RelationshipSummaryAdaptorTest` 8/8 + `PSRelationshipSummaryServiceTest` 12/12
- Reactor validate: no cycle (`rest` then `sitemanage`)
