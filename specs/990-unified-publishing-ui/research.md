# Research: Unified Publishing UI

**Feature**: `990-unified-publishing-ui`  
**Date**: 2026-07-18  
**Companion inventory**: [research/inventory.md](./research/inventory.md) (surface-by-surface evidence)

## R1 — UI stack and mount pattern

**Decision**: Use Track B **React 19 + TypeScript + Vite**, `window.PercModernUI.mount(containerId, 'PublishingShell', props)` from `WebUI/src/main/ts/bridge.ts`, register in `registry.ts`. Shell JSP pattern clones `homeModern.jsp` (`modern_shell_head.jsp`, CSRF, `tmx.jsp`, `/cm/modern/assets/perc-modern-ui.js`).

**Rationale**: Production-proven for Dashboard, Home, Widget Builder; WebUI AGENTS strategic Track B; avoids a second SPA framework.

**Alternatives considered**:
- Keep Minuet jQuery and only restyle — rejected (does not retire legacy; fails modernization goal).
- Full React Router SPA replacing all Web Management — rejected (scope creep; ~20 jQuery screens remain).
- Rewrite JSF pages in place — rejected (JSF stack is retiring; Track B is target).

## R2 — Information architecture

**Decision**: One **`PublishingShell`** with sections:

| Section | Primary stories | Default for role |
|---------|-----------------|------------------|
| **Sites & servers** | US1, US3 | Default landing for publishers |
| **Status** | US2 | |
| **Logs** | US2 | |
| **Design** | US4 | Admins/integrators |
| **Runtime / Editions** | US5 | Power users / upgraded editions model |

Routine publish never requires opening Design. Deep links may open a specific section via props/query (see contracts/deep-links.md).

**Rationale**: Spec FR-010 / US7 ease of use; unifies three historic apps without flattening design power.

**Alternatives considered**: Separate nav items for Design vs Publish — rejected (user asked for **unified** UI; sections achieve progressive disclosure).

## R3 — Backend access strategy

**Decision**:

| Capability group | Backend | Client |
|------------------|---------|--------|
| Ops site publish, incremental, item publish | Existing sitemanage `/publish/*` | Typed TS API |
| Status / logs / purge | Existing `/pubstatus/*` | Typed TS API |
| Publish servers CRUD + helpers | Existing `/servers/*` | Typed TS API |
| Design: sites, editions, content lists, edition tasks, demand queue, start job, purge job log | `IPSPublishingWs` + `IPSPublisherService` / `IPSRxPublisherService` | **New thin JSON REST** in sitemanage (or system REST) wrapping these services |
| Design: contexts, location schemes | `IPSSiteManager` scheme/context APIs | Same façade |
| Design: delivery types | `IPSPublisherService` delivery type APIs | Same façade |

**Rationale**: Ops already has JSON. Design JSF talks to server-side beans, not a public JSON API. Façade must **delegate only**—no browser-side engine.

**Alternatives considered**:
- Keep Design in JSF forever — rejected by product modernization and user request.
- Call SOAP from browser — rejected (auth/complexity; not Track B pattern).
- Direct Hibernate from UI — impossible/forbidden.

**Implementation note**: Exact package name (`com.percussion.publishingdesign` vs extension of existing pubserver service) chosen at first Design PR; must stay inside sitemanage or system REST modules already used by WebUI, with OpenAPI/adaptor patterns consistent with project REST style.

## R4 — Ops path constants (authoritative for US1–3)

From `WebUI/.../perc_path_constants.js` (SERVICES.SITEMGT prefix):

| Constant | Path suffix |
|----------|-------------|
| SITE_PUBLISH | `/publish/{site}/{server}` |
| INCREMENTAL_LIST | `/publish/incremental/content/...` |
| INCREMENTAL related | `/publish/incremental/relatedcontent/...` |
| INCREMENTAL_PUBLISH | `/publish/incremental/publish/...` |
| PUBLISH_CURRENT_STATUS | `/pubstatus/current` (+ `/{siteId}`) |
| PUBLISH_LOGS | `/pubstatus/logs` |
| PUBLISH_PURGE | `/pubstatus/purge` |
| PUBLISH_LOGS_DETAILS | `/pubstatus/details` |
| Servers | `/servers/...` (see PSPubServerRestService) |
| Item paths | PAGE_PUBLISH, RESOURCE_PUBLISH, takedown, staging variants |

Full table: [contracts/ops-publish-api.md](./contracts/ops-publish-api.md).

## R5 — Cutover strategy

**Decision**: **Phased by surface**, big-bang **within** each surface after UAT:

1. Ops (US1–3) → rewire `views.put("publish", …)` → remove Minuet publish exclusive clients  
2. Design (US4) → map `/ui/publishing/*` → remove JSF design pages from product path  
3. Runtime (US5) → map `/ui/pubruntime/*` → remove JSF runtime pages  

**Rationale**: Spec Assumptions; Design is larger than ops; still ends at one UI (FR-001, SC-006).

**Alternatives considered**: Single release for all three — higher risk, longer time-to-value for publishers.

## R6 — Dual webapp trees and war/

**Decision**: Treat `WebUI/src/main/webapp/cm/app` and `cm/pages/app` as **both** requiring rewire/delete. Treat `war/` as packaging output—ensure build does not reintroduce retired trees. Same lesson as feature 989 research R6.

## R7 — Item-level publish

**Decision**: **No forced rewrite** of finder/editor jQuery publish actions in P1. US6 is regression-first; optional later migration of history dialog into React. Site shell remains site-centric.

**Rationale**: Spec assumption; item actions already REST-backed; largest user pain is three separate *admin* apps.

## R8 — i18n

**Decision**: Reuse `perc.ui.publish.*` keys from Minuet templates; add `perc.ui.publish.design.*` / `runtime.*` as needed. Structural locale parity for new keys. Manual i18n checklist, not multi-locale Vitest hard gate (align 989).

## R9 — Testing strategy

**Decision**:
- Vitest: shell routing, form validation, progress helpers, API error mapping, purge confirmation  
- JUnit: any new REST façade methods (happy path + AuthZ + not-found)  
- Manual UAT: capability matrix rows per cutover milestone  
- Cross-platform: no Unix-only paths in new code/tests  

## R10 — Security

**Decision**: All mutating REST via existing CSRF client; never log password/privateKey fields; mask secrets in React state dumps/dev tools where product already masks; AuthZ failures map to FORBIDDEN messaging already used by Minuet (`PUBLISHER_JOB_STATUS_FORBIDDEN`, etc.).

### T015 security surface assessment (implement)

| Surface | Risk | Mitigation |
|---------|------|------------|
| CSRF on mutations | Session CSRF | Ops clients use `api/client.ts` which attaches OWASP CSRFGuard header on GET/POST/PUT/DELETE |
| AuthZ errors | Silent failure | Map 403 / FORBIDDEN body tokens in `publishActions.ts`; clear i18n messaging |
| Secrets (server passwords/keys) | Log leakage | Server editor (US3) must never put password/privateKey into error serialization (see `serverSecrets` tests); no console.log of server props |
| Query props XSS | Reflected XSS | `publishModern.jsp` allowlists `section` and restricts `siteId`/`serverId` to safe charset; `deepLinkMap.mapIdParam` double-checks |
| Design façade (US4+) | Privilege escalation | Must enforce same design/admin roles as JSF; JUnit AuthZ cases at façade |

## Resolved unknowns

| Unknown | Resolution |
|---------|------------|
| React vs other | Track B React (R1) |
| One vs multi shell | One PublishingShell (R2) |
| Design without REST | Thin façade (R3) |
| Cutover | Phased by surface (R5) |
| Item actions scope | Regression-first (R7) |

No remaining NEEDS CLARIFICATION for plan phase.
