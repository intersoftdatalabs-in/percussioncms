# Erlang review: PR #1370 — unified Publishing UI

**Date**: 2026-07-19  
**PR**: https://github.com/intersoftdatalabs-in/percussioncms/pull/1370  
**Branch**: `990-unified-publishing-ui` → `development`  
**Scope**: 132 files, ~+13.7k / −4.7k (React PublishingShell, sitemanage design façade, Minuet cutover, US9 residual)  
**Reviewer persona**: Erlang (strict independent review; Kilo infra failed — substitute)  
**Memory patterns hit**: missing behavioral contract for API request shapes; multi-copy shared WebUI assets; empty/swallowed catch; incomplete dual-tree retirement

## Summary

Large, coherent feature PR that lands a modern React Publishing shell, a thin sitemanage design/runtime JSON façade, dual-tree nav rewire to `publishModern.jsp`, Minuet client deletions under `cm/webapp`, and solid Vitest coverage for pure helpers (approval, sort, filters, validation, deep-link allowlist). JSP query allowlisting for section/siteId/serverId is the right XSS pattern.

**However**, log **purge** and log **details** client request bodies do not match the existing sitemanage DTOs / Minuet contract (`jobids` / `jobid` + root names). That is user-visible broken behavior for OPS-22/23/24 claims. Incomplete packaging cleanup around `static-bundles.json` / dual `war` vs `webapp` Minuet trees is a residual ship risk.

## Recommendation

**`request-changes`**

## Gate

|                  Gate                  |                                         Result                                         |
|----------------------------------------|----------------------------------------------------------------------------------------|
| Bugs (correctness / contract)          | **Fail** — purge + log details body shape                                              |
| Behavioral tests for non-trivial logic | Pass for pure TS helpers + design REST core; **gap** on purge/details request builders |
| Cross-platform path/file I/O           | Pass (N/A for most of this PR; no new non-portable FS joins found)                     |
| May merge as-is                        | **No**                                                                                 |

## Issues

### Bugs (block)

#### B1 — Log purge request body does not match server DTO / Minuet contract

- **Where**: `WebUI/src/main/ts/publishing/sections/LogsSection.tsx` → `purgePublishingLogs({ jobIds: [...] })`; `WebUI/src/main/ts/api/publishing/statusApi.ts`
- **Evidence**: Server `PSSitePublishPurgeRequest` exposes `jobids` (not `jobIds`) and is `@JsonRootName("SitePublishPurgeRequest")`. Minuet `PercPublisherService.purgeJob` posts:

  ```json
  { "SitePublishPurgeRequest": { "jobids": [ ... ] } }
  ```

  Modern client posts `{ "jobIds": [ ... ] }` (wrong property name, no root wrapper).

- **Impact**: Purge selection appears to work in UI but server will not receive job IDs → purge no-ops or 400; breaks OPS-24 / SC-008 diagnose-and-cleanup path.

- **Fix**: Align client with Minuet/DTO: send `jobids` (and root name if mapper requires UNWRAP). Add unit test for request body shape (behavioral contract test).

#### B2 — Log details request body uses `jobId` instead of `jobid`

- **Where**: `LogsSection.onDetails` → `fetchLogDetails({ jobId })`
- **Evidence**: `PSSitePublishLogDetailsRequest` field is `jobid` (`getJobid`/`setJobid`); Minuet posts `{ SitePublishLogDetailsRequest: { jobid: jobId } }`.
- **Impact**: Details panel may always fail or return empty; undercuts OPS-23 “structured details” claim.
- **Fix**: Same as B1 — match DTO + Minuet; unit-test builder.

#### B3 — Minuet sources deleted but minify bundle inventory still lists them

- **Where**: `WebUI/src/main/resources/minify/static-bundles.json` still lists `views/PercPublishMinuetView.js`, `PercPublishStatusMinuetView.js`, `PercPublishLogsMinuetView.js` for `jslibMin/perc_publish.packed.js` (file **not** updated in this PR while webapp Minuet views **were** deleted).
- **Evidence**: `WebUI/scripts/build-legacy-bundles.js` processes `static-bundles.json`. Product tree under `WebUI/src/main/webapp/cm/...` no longer has those views; PR deleted them.
- **Impact**: Legacy bundle build against webapp/source tree fails or ships a broken pack; dual-tree drift vs any remaining `war/` consumers.
- **Fix**: Update or remove `perc_publish` pack entries to remaining consumers only (e.g. `PercPublisherService.js` if still needed for item publish), or point pack exclusively at retained paths; verify `npm run build:legacy` / packaging after cutover.

### Suggestions (should fix before merge if cheap)

#### S1 — Dual packaging / war tree still Minuet while webapp cut over

- `WebUI/war/app/publish.jsp` (tracked) still loads Minuet scripts; not part of this PR’s webapp dual-tree rewire.
- Confirm whether `war/` is still a product packaging input. If yes, apply same redirect/modern mount; if no, document as non-product and exclude from ship.

#### S2 — Root `WebUI/vite.legacy.config.ts` still maps `perc_publish.packed.min` → missing `perc_publish.bundle.js`

- `src/main/frontend/vite.legacy.config.ts` dropped the entry; root config still references a bundle that does not exist on the branch.
- Align both configs so whichever build entry is used does not fail.

#### S3 — Design façade surface vs tests

- `PSPublishingDesignRestService` is large (~1.2k LOC). Tests cover editions/content-list happy paths and 400/404, but weak coverage for copy-with-content-lists, scheme parameters, demand-publish edge cases.
- Not a full block if UAT covers design, but add at least copy + associate content list behavior tests before calling design “done”.

#### S4 — Swallowed exception in runtime job status

- `PSPublishingRuntimeSupport.listRuntimeEditions`: catch on `getPublishingJobStatus` sets status `"running"` without log.
- Prefer debug log; avoid lying about state when status fetch fails.

#### S5 — `showDesign` always true in shell

- Progressive disclosure prop exists but JSP does not pass role-based `showDesign=false`.
- Document as residual or wire a real capability flag before claiming US7 complete.

### Nits

- `createContext_requiresName` test accepts 400 **or** 503 — too loose; prefer explicit construction with site manager mock for 400-only.
- `mapIdParam` allows only `[A-Za-z0-9_-]{1,128}` — fine for numeric siteIds; document that site **names** with dots/spaces are not deep-linkable via query (ids only).

## Cross-platform path checklist

- [x] No new OS filesystem path joins with hardcoded separators in product Java for this feature
- [x] URL/classpath paths correctly use `/`
- [x] Tests do not assert Unix-only absolute FS paths for new logic
- N/A: installer/package path handling

## Positives (do not lose in rework)

- Query allowlist in `publishModern.jsp` (section + siteId + serverId) — solid XSS control
- Dual `cm/app` + `cm/pages` index rewire to modern shell
- Vitest for approval payload, status sort, logs filter, queue PagedItemList shape, server validation, deep links
- JUnit/Mockito for design REST and runtime support core paths
- Secret redaction helper for server property dumps
- Spring registration of `publishingDesignRestService` in jaxrs serviceBeans
- Honest residual tracking for faces packaging / UAT via #1371 / #1372

## Suggested fix order

1. B1 + B2 (+ tests for request builders)
2. B3 packaging/minify inventory
3. S1/S2 dual-tree/config alignment
4. S3–S5 as capacity allows

## Handoff

Author should not merge until B1–B3 are fixed and re-reviewed (or B3 explicitly accepted with proven packaging path that never builds the dead pack). Re-run Vitest publishing suite + a smoke purge/details call against a live/stack mock of pubstatus endpoints.

## Re-review (2026-07-19 — mitigation commit)

|     Finding     |  Status   |                                                                             Mitigation                                                                              |
|-----------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| B1 purge body   | **Fixed** | `logRequestBodies.buildPurgeRequestBody` → `{ SitePublishPurgeRequest: { jobids } }`; `purgePublishingLogs(jobIds)`                                                 |
| B2 details body | **Fixed** | `buildLogDetailsRequestBody` → `{ SitePublishLogDetailsRequest: { jobid } }`; also wrapped log list POST                                                            |
| B3 Minuet pack  | **Fixed** | Removed three exclusive Minuet views from `static-bundles.json` perc_publish pack; dropped root `vite.legacy` perc_publish entry; structural test asserts pack list |
| Tests           | **Added** | `logRequestBodies.test.ts`, purge gate pairs with payload, `publishNavRewire` B3 assert                                                                             |

**Vitest**: 79 publishing tests passed.  
**Residual (suggestions, not re-opened as bugs)**: S1 `war/app/publish.jsp` still Minuet; S3–S5 design test depth / showDesign / swallowed status — follow-up OK.

## Re-review (2026-07-19 — follow-up S1–S5)

|           Finding           |         Status         |                          Mitigation                          |
|-----------------------------|------------------------|--------------------------------------------------------------|
| S1 war Minuet publish       | **Fixed**              | `WebUI/war/app/publish.jsp` → 301 redirect to modern shell   |
| S2 vite.legacy perc_publish | **Fixed** earlier (B3) | root config entry removed                                    |
| S3 design tests             | **Fixed**              | `copyEdition` + `associateContentList` happy-path unit tests |
| S4 false "running" status   | **Fixed**              | debug log; status `"unknown"` not `"running"`; unit test     |
| S5 showDesign               | **Fixed**              | Admin/Designer gate in `publishModern.jsp` (app + pages)     |

**Java**: DesignRestServiceTest 14 + RuntimeSupportTest 11 = 25 passed.  
**Commit**: `a8941ce733`
