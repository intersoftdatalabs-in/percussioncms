# T074 — REST / service gaps for the modern Content Explorer's dependency / IA relationship views

**Author**: Kilo (US7 implementer / T074 spike), 2026-07-20.
**Scope**: US7 P-Adv surfaces that depend on **typed relationship lookup** beyond the existing `PSWidgetAssetRelationshipService` AA-link data the DependencyViewer already consumes.
**Status (this revision, 2026-07-20 15:15 ET)**: **policy changed** — see [§"Policy change 2026-07-20"](#policy-change-2026-07-20). The original T074 outcome ("NO new façade required for 8.2, 5/6 dimensions render as unknown with client-side preview") is **superseded** by US8 below.

---

## What this spike originally said (2026-07-20 morning)

US7 ships 6 relationship dimensions for the DependencyViewer:

| # |         Dimension          |                                                                                                Existing server support?                                                                                                |  Client population  |
|---|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| 1 | **outgoing relationships** | No typed REST endpoint; underlying data in `RXRELATIONSHIPS` + `PSRelationshipConfigSet` is reachable via internal Java APIs (`IPSRelationshipCataloger`) but not exposed to /Rhythmyx/rest/.                          | rendered `unknown`  |
| 2 | **incoming relationships** | Same.                                                                                                                                                                                                                  | rendered `unknown`  |
| 3 | **Active Assembly links**  | Yes — `PSWidgetAssetRelationshipService.findOwners(...)` is the source of truth; `aaLinkCount` is supplied by the host shell (P-Addon / P-Adv wiring layer).                                                           | rendered with count |
| 4 | **taxonomy / site edges**  | No typed REST endpoint; underlying data in `PSNode` / `taxonomy` schema reachable via `IPSNodeService` but not exposed.                                                                                                | rendered `unknown`  |
| 5 | **local dependencies**     | No typed REST endpoint; the dimension is component-assembly-style edges (template → widget → asset within a single page); reachable via `IPSWidgetAssetRelationshipService` only for widgets, not by page-level graph. | rendered `unknown`  |
| 6 | **reverse dependencies**   | No typed REST endpoint; `IPSRelationshipCataloger.getParents(...)` is internal Java.                                                                                                                                   | rendered `unknown`  |

The T074 outcome in the morning shipped `DependencyViewer.tsx` + `RelationshipsView.tsx` showing the 5 dimensions as `unknown` with a `clientSidePreview` banner, on the assumption that the missing endpoints were a follow-up `rest` track.

---

## Policy change 2026-07-20

The release has clarified (2026-07-20 15:15 ET):

> **No residuals are allowed out of these spec phases. If rest API work is needed for the UI, the spec must be revised to include that work so the UI can be delivered.**

This invalidates the morning T074 outcome for 8.2. The five unknown dimensions are not OK at GA; the spec must be amended.

This spike records, below, the **revised** scope that brings the missing `rest` work **into** spec 992 as a new user-story (US8, "Dependency API surface for the modern Content Explorer"), so the UI can be delivered complete at 8.2 GA.

---

# US8 — Dependency API surface for the modern Content Explorer (NEW, 2026-07-20)

## Goal

Define a typed REST façade over the relationship, taxonomy, and component-assembly
data that the modern Content Explorer's DependencyViewer and RelationshipsView
need, so all 6 dimensions render authoritative counts (no `unknown`,
no `clientSidePreview` banner) at 8.2 GA.

## In-scope endpoints

Five new endpoints ship in spec 992 US8 (one per currently-unknown
dimension). They live in the `rest/` module so they participate in the
existing `rest` build + OpenAPI surface, and they delegate to existing
sitemanage services where available.

| # |            Endpoint             |   Method   |                                                    Path                                                    |                                    Source service                                    |
|---|---------------------------------|------------|------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| 1 | Get outgoing relationship count | `GET`      | `/Rhythmyx/rest/content-explorer/relationships/{itemId}/outgoing`                                          | new `IPSRelationshipSummaryService` (sitemanage) wrapping `IPSRelationshipCataloger` |
| 2 | Get incoming relationship count | `GET`      | `/Rhythmyx/rest/content-explorer/relationships/{itemId}/incoming`                                          | same service                                                                         |
| 3 | (already shipped, US7)          | (existing) | the AA dimension is sourced via the existing `PSWidgetAssetRelationshipService` count supplied by the host | —                                                                                    |
| 4 | Get taxonomy / site edge count  | `GET`      | `/Rhythmyx/rest/content-explorer/relationships/{itemId}/taxonomy`                                          | new service wrapping `IPSNodeService` / taxonomy schema                              |
| 5 | Get local (page-assembly) edges | `GET`      | `/Rhythmyx/rest/content-explorer/relationships/{itemId}/local`                                             | new service wrapping page / template / widget DAO layer                              |
| 6 | Get reverse-dependency count    | `GET`      | `/Rhythmyx/rest/content-explorer/relationships/{itemId}/reverse`                                           | same service as #1, parent-side aggregation                                          |

A sixth consolidated convenience endpoint is added:

| 7 | Full per-node relationship summary | `GET` | `/Rhythmyx/rest/content-explorer/relationships/{itemId}/summary` | the rest façade composes #1, #2, #4, #5, #6 from the new service in a single response for the DependencyViewer |

## Out-of-scope (explicit)

- AA-link count is already authoritative (US7). US8 does not duplicate it.
- Pull-graph / find-usage UIs that lead to a deep graph traversal (already
  partially covered by the legacy `PSDependencyViewer` Java client) are
  scoped to a follow-up spec, not 992 8.2.
- Bulk / multi-node summaries are scoped to a follow-up spec. US8 ships
  one-node-at-a-time; the DependencyViewer renders one node at a time.

## Delivery surface (constitution II — no invented APIs)

The endpoints are typed 1:1 to the Java DTOs that the underlying services
produce. No server-side data is invented in the TypeScript layer; the
existing relationships / taxonomy / page-assembly schemas are reused.

|  Endpoint  |                                                      Wire format                                                       |                                                         Source DTO                                                         |
|------------|------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| #1, #2, #6 | `{ "PSRelationshipSummary": { "count": number, "byType": [{ "type": "string", "count": number }] } }`                  | new `PSRelationshipSummary` in `rest/src/main/java/com/percussion/share/relationship/` mirroring existing `PSRelationship` |
| #4         | `{ "PSTaxonomySummary": { "count": number, "nodes": ["string"] } }`                                                    | new `PSTaxonomySummary` in `rest/src/main/java/com/percussion/share/relationship/`                                         |
| #5         | `{ "PSLocalDependencySummary": { "count": number, "links": [{ "type": "string", "targetId": "string" }] } }`           | new `PSLocalDependencySummary` in `rest/src/main/java/com/percussion/share/relationship/`                                  |
| #7         | `{ "PSNodeRelationshipSummary": { "outgoing": ..., "incoming": ..., "taxonomy": ..., "local": ..., "reverse": ... } }` | new `PSNodeRelationshipSummary` in `rest/src/main/java/com/percussion/share/relationship/`                                 |

TypeScript mirrors land in `WebUI/src/main/ts/api/contentExplorer/types.ts`'s
`relationship.ts` (new file). Each is consumed by:
- `WebUI/src/main/ts/api/contentExplorer/relationshipsApi.ts` (new) — typed fetch wrappers.
- `WebUI/src/main/ts/contentExplorer/views/dependencyModel.ts` — replaces the morning "unknown + clientSidePreview" branch with the real server summary.
- `WebUI/src/test/ts/contentExplorer/dependencyModel.test.ts` — updated to assert all 6 dimensions are populated for the canonical happy-path item.

## Test strategy (constitution III)

Two-layer tests per the existing 992 convention:

- **Vitest unit / mapper** (`WebUI/src/test/ts/contentExplorer/relationshipsApi.test.ts`): wire encoding, map shapes, default `count=0` for empty summaries, defensive `null` guards. ≥6 tests.
- **Vitest component** (`WebUI/src/test/ts/contentExplorer/DependencyViewer.test.tsx`): mount the viewer in a Vitest with `relationshipsApi` mocked to return a full server summary; assert all 6 dimensions render with the supplied counts and the `clientSidePreview` banner is **gone** (because at least one dimension is authoritative, the banner is replaced with "Sourced from server" affordance, or simply removed — agreed in T087 review).
- **Playwright E2E** (`modules/perc-qa-automation/frontend/tests/us8-dependency.spec.js`): per-PR contribution; mounts the modern pilot at `cm/app/us7AdvancedModern.jsp` (now renamed or expanded if US7 pilot is reused), logs in as Admin, asserts network round-trip to `/Rhythmyx/rest/content-explorer/relationships/.../summary` and that the rendered counts populate. ≥3 tests.

## AuthZ / CSRF (constitution VI)

- All 5 new GETs are read-only; they inherit the existing `pathApi`
  AuthZ envelope (Admin/Write/Read allowed; View allowed for templates
  and folders, gated per `accessLevel`).
- The server-side `IPSRelationshipSummaryService` checks the
  caller's ACL on `itemId` before returning the count; returns
  `PSForbiddenException` (HTTP 403) when the caller has View-only
  access on a private folder's content (the Dimension value
  reflects "no access", not "0").
- CSRF: GETs are exempt; the convenience endpoint #7 is also GET.

## Service-contract integration test (constitution III/IV)

- A `JAX-RS integration test` lands in `rest/src/test/java/com/percussion/rest/relationship/` to assert happy path + AuthZ negative + JSON wire envelope (mirror of the existing rest module's
  test convention; see `rest/src/test/java/com/percussion/rest/actions/`).

## Threat-model note (T089 carry-over)

- **AuthZ negative** — confirmed server-side; client-side `aclLockout.ts` semantics untouched.
- **Open redirect** — N/A; endpoints accept only `itemId` (an integer or GUID, validated server-side).
- **Secrets in logs** — N/A; no PII handled by the summary endpoint; server logs only item-id + actor + count.
- **CSRF** — GETs are CSRF-exempt; no body.
- **DoS** — the consolidated `#7` endpoint is bounded by per-node work;
  `IPSRelationshipSummaryService` is in-process and bounded by
  `RXRELATIONSHIPS` row scan; an Admin-on-View-only-folder attempt
  returns 403 before row scan.

## Plan / Complexity Tracking (constitution V)

- New file count (sitemanage): `IPSRelationshipSummaryService` interface + impl = 2
- New file count (rest): `RelationshipSummaryResource` + 5 DTO classes + 5 tests = 11
- New file count (WebUI): `relationship.ts` types + `relationshipsApi.ts` client + updated `dependencyModel.ts` + updated component tests + 1 Playwright spec = 5
- Total: ~18 new files, 3 modified (DependencyViewer.tsx, dependencyModel.ts,
  relationshipsView.tsx stub references in the registry).

This is a multi-week undertaking; the matrix preamble "no post-8.2
deferral" + the policy this spike records supersedes the morning
acceptance of the partial. The 992 train resumes only after the US8
implementation lands (separate PR train; Phase 11 of the spec).

---

## Status (post-policy-change)

- ❌ **Original morning outcome (5/6 dimensions unknown, clientSidePreview banner)** — superseded.
- ✅ **New policy**: DependencyViewer / RelationshipsView must ship all 6 dimensions authoritative at 8.2 GA.
- 🚧 **Implementation in US8** — separate PR train; tasks T091–T104 added to `tasks.md` on the same date.
- 📋 **Required reviewers / sign-off** — release-manager (T090 SC-012) confirms that US8 merge trains before labeling 8.2 GA.

---

