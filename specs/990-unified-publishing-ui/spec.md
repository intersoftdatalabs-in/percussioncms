# Feature Specification: Unified Publishing UI

**Feature Branch**: `990-unified-publishing-ui`  
**Created**: 2026-07-18  
**Status**: Draft  
**Input**: Inventory and consolidate legacy Rhythmyx publishing design, publishing runtime, and the modern CMS publishing screen into a new unified publishing UI implemented in the modern React TypeScript stack, with feature parity and ease of use, so an agent can implement the new publishing user interface from the resulting specs and plan.

## Module Scope

- **Primary module(s)**: `WebUI/` (legacy JSF Publishing Design under `ui/publishing`, Publishing Runtime under `ui/pubruntime`, Minuet/jQuery Publish under `cm/app` and mirrors; modern Track B UI under `WebUI/src/main/ts` / frontend Vite pipeline)
- **Secondary / integration modules**: `projects/sitemanage/` (site publish, publish status, pub server REST); `system/` (`services.publisher`, `rx.publisher`, publishing web services); optional `rest/` publishing-server DTOs; product help under `system/Docs/Rhythmyx_Publishing_*_Help` for capability reference only
- **AGENTS files to apply**: root `AGENTS.md`, `WebUI/AGENTS.md` (Track B React modernization; Rhythmyx Publishing marked legacy/retiring)
- **User roles affected**: site publishers and operators (run publish, monitor jobs, review logs); CMS administrators and integrators (publish server configuration; publishing design infrastructure); content authors with publish-now / staging actions on items; ops monitoring long-running jobs
- **Install / upgrade impact**: web UI distribution and navigation rewiring; **no** intentional publishing engine rewrite, package format change, or schema migration unless plan discovers a documented API gap. Upgrade must preserve the ability to publish sites and manage design/runtime configuration for existing customer sites. Deep links and bookmarks to classic design/runtime/publish URLs must map to the unified UI or show a clear moved/unavailable message.

### Supporting inventory

Evidence inventory of current surfaces and capability groups: [`research/inventory.md`](./research/inventory.md). That document is normative for **what exists today** for parity; this specification is normative for **what users must be able to do** in the unified product.

## Clarifications

None yet. Reasonable defaults are recorded under **Assumptions**. Use `/speckit-clarify` if product owners reject a default (especially cutover phasing or design depth).

## User Scenarios & Testing

Each story must be independently testable. Stories are ordered for delivery value: operators first, then design power users, then legacy retirement.

### User Story 1 - Unified Publishing home and site operations (Priority: P1)

A publisher opens a single **Publishing** area in the modern CMS UI (same visual language and session behavior as modern Dashboard/Home). They browse sites (card or list, with filter), open a site, see its publish servers, and start a **full** or **incremental** publish to a chosen server. While a job runs they see progress and can **stop** the job when the product allows stopping today.

**Why P1**: This is the daily path for CM1 site publishing; it replaces the primary Minuet Publish screen and is the spine of the unified experience.

**Acceptance Scenarios**:
1. **Given** a signed-in user with publishing access, **When** they open Publishing from product navigation, **Then** they land in one modern Publishing shell (not a separate legacy JSF app and not the classic Minuet-only page as the primary path once this story ships).
2. **Given** multiple sites exist, **When** the user filters and toggles card/list views, **Then** matching sites appear and selection opens that site’s publishing workspace.
3. **Given** a site with at least one configured publish server, **When** the user runs full publish to that server, **Then** a job starts and appears in status with identifiable site/server context.
4. **Given** items are queued for incremental publish, **When** the user previews the incremental queue (and related items when the product shows them today) and confirms incremental publish, **Then** the incremental job starts using the same business rules as today (including approval flows when required).
5. **Given** a running job that is stoppable, **When** the user chooses Stop, **Then** the system requests stop and status reflects stopping/stopped without leaving the UI in an ambiguous state.
6. **Given** permission or license forbids publish, **When** the user attempts to publish, **Then** they see a clear denial consistent with current product behavior (not a blank failure).

### User Story 2 - Live status and publishing logs (Priority: P1)

Operators monitor all current publishing jobs and review historical logs: filter by site/server/time window as supported today, open job detail including published items, and purge/delete selected logs with confirmation. Status updates while jobs run without requiring a full page reload mental model (auto-refresh or equivalent).

**Why P1**: Runtime and modern screens both center on status and logs; failures are diagnosed here.

**Acceptance Scenarios**:
1. **Given** one or more jobs are active, **When** the user opens Status, **Then** they see site, status, start time, duration, and progress for current jobs, with stop actions where applicable.
2. **Given** completed jobs exist, **When** the user opens Logs and applies supported filters, **Then** the matching log list loads and each row can open details of published items.
3. **Given** the user selects logs for removal, **When** they confirm purge/delete, **Then** those logs are removed and the list refreshes; cancel leaves logs unchanged.
4. **Given** a job fails or ends abnormally, **When** the user reviews status/logs, **Then** end state and enough detail are visible to begin diagnosis (aligned with today’s job/item log information).

### User Story 3 - Publish server configuration (Priority: P1)

An administrator manages **publish servers** for a site: create, edit, delete, refresh, and designate the default Publish Now server. They configure Production vs Staging, File vs Database targets, and driver-specific settings (Local, FTP, FTPS, SFTP, Amazon S3, and supported database drivers) with validation that prevents saving incomplete or invalid configurations.

**Why P1**: Without server configuration parity, sites cannot be published after leaving the Minuet UI.

**Acceptance Scenarios**:
1. **Given** a selected site, **When** the admin adds a server with a valid Local file configuration, **Then** the server appears in the site’s server list and can be used for publish.
2. **Given** each supported driver family (FTP/FTPS/SFTP, S3, database variants), **When** the admin completes required fields and saves, **Then** properties persist and reload correctly (secrets handled per product norms—never shown in logs).
3. **Given** a server is default for Publish Now, **When** the admin views the server list, **Then** the default is clearly indicated; changing default updates which server is used for publish-now behaviors that depend on it.
4. **Given** invalid or incomplete properties, **When** the admin saves, **Then** the UI blocks save and explains what to fix.
5. **Given** hosted/EC2-related options apply in the environment, **When** the admin configures relevant fields, **Then** available regions/publishing server helpers behave as they do in the current Publish UI.

### User Story 4 - Publishing design (infrastructure) (Priority: P2)

Integrators and admins maintain the **publishing design** infrastructure in the same unified Publishing product area (clearly navigable section, not a separate abandoned JSF application): sites (design-level), editions, content lists (including legacy content list where still supported), contexts, location schemes (modern and legacy), context variables, delivery type registrations, and site-root/path browsing used by schemes. Create, edit, copy, delete, and associate operations available in the current Design UI remain available with equivalent outcomes.

**Why P2**: Design is essential for advanced and upgraded Rhythmyx/CM implementations; it is larger than day-to-day publish and can follow ops parity, but **feature parity is required** before Design JSF can retire.

**Acceptance Scenarios**:
1. **Given** design access, **When** the user opens the Design section of Publishing, **Then** they can navigate the design object hierarchy (sites → editions / content lists / contexts / delivery types as today).
2. **Given** the user creates or edits an edition and associates content lists, **When** they save, **Then** associations persist and the edition can be selected for runtime/ops publish paths that depend on it.
3. **Given** the user maintains a content list (modern or legacy type still supported product-wide), **When** they save, **Then** generators/parameters behave as in current Design for that type.
4. **Given** the user maintains contexts and location schemes (modern or legacy), **When** they save and test path/root browsing where offered today, **Then** schemes resolve and persist as today.
5. **Given** the user registers or edits a delivery type, **When** they save, **Then** it is available to publishing configuration that references delivery types.
6. **Given** copy edition from another site (including optional content list copy), **When** the user completes the flow, **Then** the target site receives the copied edition configuration equivalent to today’s Design behavior.
7. **Given** delete of a design object, **When** the user confirms, **Then** the object is removed subject to the same constraints/warnings as today (e.g. dependent schemes).

### User Story 5 - Runtime edition control and demand publish (Priority: P2)

Operators run publishing **by edition** when that model is required: list runtime editions, start and stop edition runs, perform demand publish, and reach job/item logs from the runtime path. Capabilities documented for Publishing Runtime (including clear site record / advanced log cleanup when still product-supported) are reachable from the unified UI without the classic Runtime JSF shell.

**Why P2**: Completes parity with Surface B for power users and upgraded systems that still think in editions rather than only “site + server”.

**Acceptance Scenarios**:
1. **Given** editions exist for a site, **When** the user opens Runtime / Editions, **Then** they see runnable editions and can start a run.
2. **Given** an edition is running, **When** the user stops/cancels it, **Then** the job transitions to a stopped/cancelled state consistent with today’s runtime behavior.
3. **Given** demand publish is allowed for the selection, **When** the user submits demand publish, **Then** work is queued/run as today.
4. **Given** the user needs advanced log or site-record cleanup still offered by the product, **When** they use the unified UI entry for that action, **Then** outcomes match current Runtime behavior (including confirmations).

### User Story 6 - Item-level publish actions remain coherent (Priority: P2)

Authors and publishers continue to **publish now**, **take down**, **stage**, **remove from staging**, view **publishing actions** available for an item, manage **schedule** dates where supported, and open **publishing history** for an item—from the content-centric entry points the product uses today (finder, editor, workflow)—with the same outcomes. The unified Publishing area does not remove these actions; if any item history or status deep-links into Publishing, those links land on the modern shell.

**Why P2**: Item actions are part of publishing feature parity even when the primary new shell is site-centric.

**Acceptance Scenarios**:
1. **Given** an item the user is allowed to publish, **When** they invoke Publish Now (page or asset/resource), **Then** publish proceeds under the same rules (default server, multi-site constraints, license) as today.
2. **Given** take down / stage / unstage actions are offered, **When** the user confirms, **Then** results match current item publisher behavior (including linked-item warnings where applicable).
3. **Given** scheduled publish dates are supported for the item, **When** the user sets or clears them, **Then** values persist and affect scheduling as today.
4. **Given** publishing history for an item, **When** the user opens history, **Then** historical entries are visible equivalent to today’s history dialog.

### User Story 7 - Ease of use and information architecture (Priority: P2)

Users who only need “publish this site” are not forced through design-object complexity. The unified UI uses a clear information architecture: e.g. **Sites & servers** (ops), **Status**, **Logs**, **Design**, **Runtime/Editions** (labels may be refined in plan/design), with progressive disclosure, consistent empty states, accessible controls (keyboard, labels), and responsive layout consistent with modern CMS screens. Primary tasks require fewer context switches than today (where design, runtime, and Minuet are separate applications).

**Why P2**: Explicit product goal—parity **and** ease of use.

**Acceptance Scenarios**:
1. **Given** a typical publisher role, **When** they publish a site end-to-end, **Then** they can complete site select → server select → full publish → see status without opening Design or classic Runtime.
2. **Given** an integrator needs design, **When** they switch to Design within Publishing, **Then** they reach design tools without leaving the CMS chrome or re-authenticating beyond normal session rules.
3. **Given** empty sites, no servers, or no logs, **When** the user opens those views, **Then** empty states explain next actions (e.g. add server).
4. **Given** keyboard-only use of primary lists and actions, **When** the user navigates, **Then** focus order and control labels allow completing publish and stop without a pointer (parity with modern UI accessibility expectations).

### User Story 8 - Retire legacy publishing UIs (Priority: P3)

In the release(s) that achieve parity for a surface, operators and developers no longer ship that surface’s classic client as a production path: Minuet Publish client for ops, JSF Design for design, JSF Runtime for runtime. Navigation and known URLs point at the unified UI. Dual-running classic vs modern for the same surface is not the long-term state.

**Why P3**: Retirement is gated on parity and UAT, but is a required outcome of consolidation—not an optional cleanup.

**Acceptance Scenarios**:
1. **Given** ops parity (Stories 1–3) is accepted, **When** the product is installed/upgraded with ops cutover, **Then** primary Publish navigation does not load the classic Minuet publish page as the main UI.
2. **Given** design parity (Story 4) is accepted, **When** design cutover ships, **Then** production does not require JSF Design pages for design tasks.
3. **Given** runtime parity (Story 5) is accepted, **When** runtime cutover ships, **Then** production does not require JSF Runtime pages for runtime tasks.
4. **Given** a known classic URL for a retired surface, **When** a user opens it, **Then** they are routed to the equivalent unified view or receive a clear moved/unavailable message.
5. **Given** retirement inventory is signed off, **When** reviewers check the feature checklist, **Then** exclusive classic clients for retired surfaces are listed as removed and any retained shared libraries are justified.

### Edge Cases

- User has access to view status/logs but not to start publish or edit servers.
- Site has zero publish servers; publish actions disabled with guidance.
- Concurrent jobs for multiple sites; status list remains usable and stop targets the correct job.
- Incremental queue empty; preview shows empty state; publish may no-op or warn per today’s rules.
- Default Publish Now server missing or misconfigured when item publish now is invoked.
- Database or remote (FTP/S3) target unreachable; user sees actionable error, not silent hang.
- Session expires mid-configuration; dirty form does not corrupt server definitions; user can re-authenticate and recover.
- Design object delete blocked by dependencies; user sees warning and object remains.
- Legacy content list or legacy location scheme still present on upgraded systems; modern UI can open and save them without forced migration in this feature.
- Very large log volumes; list remains paginated or limited as today without browser lockup for typical admin use.
- Hosted vs on-prem differences (EC2/regions); UI only offers environment-appropriate options.

## Requirements

### Functional Requirements

- **FR-001**: The product MUST provide a single modern **Publishing** user experience that consolidates capabilities of (a) modern CMS Publish, (b) Rhythmyx Publishing Design, and (c) Rhythmyx Publishing Runtime, within the modern CMS chrome.
- **FR-002**: Users MUST be able to list, filter, and open sites for publishing operations using card and list presentations equivalent in outcome to the current Publish UI.
- **FR-003**: Users MUST be able to create, update, delete, and refresh **publish servers** for a site, including Production/Staging, File/Database types, and all driver/property combinations supported by the current Publish UI.
- **FR-004**: Users MUST be able to run **full** and **incremental** site publishes to a selected server, preview incremental queues (and related items when currently shown), support incremental-with-approval when the product requires it, and **stop** stoppable jobs.
- **FR-005**: Users MUST be able to monitor **current job status** (including progress and timing fields available today) with timely refresh while jobs run.
- **FR-006**: Users MUST be able to query **publishing logs**, open **job/item details**, and **purge/delete** logs with confirmation, covering outcomes available in modern Publish and Runtime log UIs.
- **FR-007**: Users with design authority MUST be able to perform design lifecycle operations for **sites (design)**, **editions**, **content lists** (modern and still-supported legacy), **contexts**, **location schemes** (modern and legacy), **context variables**, **delivery types**, **edition copy from other site**, and **site root/path browsing** with outcomes equivalent to current Design UI.
- **FR-008**: Users MUST be able to **start/stop editions**, perform **demand publish**, and perform remaining Runtime-only operations still supported by the product (including clear site record / advanced log cleanup if still offered) with equivalent outcomes.
- **FR-009**: Item-level **publish now**, **takedown**, **stage**, **remove from staging**, **publishing actions**, **schedule dates**, and **publishing history** MUST remain available with equivalent outcomes; content entry points MUST NOT regress when the unified Publishing shell ships.
- **FR-010**: The unified UI MUST use progressive disclosure so routine site publish does not require design-object expertise, while Design and Runtime sections remain fully reachable for authorized users.
- **FR-011**: Authorization, license, and configuration gates that apply today to publish, design, or runtime actions MUST be enforced in the unified UI with clear user messaging.
- **FR-012**: User-visible strings in the unified Publishing UI MUST use the product’s catalog-backed localization approach (TMX / existing message pipeline), reusing keys where appropriate and adding keys for net-new strings with structural language parity for neighboring catalogs.
- **FR-013**: Publishing MUST continue to use the existing server-side publishing engine and services as the system of record; the feature MUST NOT reimplement assembly, edition execution, or delivery inside the browser.
- **FR-014**: Known deep links and bookmarks to classic Publish, Design, and Runtime entry points MUST map to unified equivalents or show a clear moved/unavailable message after the corresponding surface is cut over.
- **FR-015**: After accepted parity for a surface (ops/design/runtime), that surface’s classic primary client MUST be removable from production navigation and packaging per the retirement story; retirement proof is a durable inventory checklist under this feature directory, signed off in PR/release review.
- **FR-016**: Forms that hold publish server secrets (passwords, keys) MUST NOT expose secrets in logs, analytics, or error reports; display/storage of secrets MUST follow existing product security norms.
- **FR-017**: Automated tests MUST cover new/changed client behavior for non-trivial logic, MUST preserve or replace service-contract coverage, and MUST include automated Playwright end-to-end tests in `modules/perc-qa-automation` verifying all core UI screens and workflows; tests MUST be cross-platform (Windows/Linux/macOS) per project rules.
- **FR-018**: Primary Publishing workflows MUST be operable with keyboard and provide accessible names/labels for critical controls (site list, server actions, publish, stop, log purge).
- **FR-019**: Empty, error, and in-progress states MUST be explicit for sites, servers, jobs, logs, and design lists so users always know the next safe action or recovery step.
- **FR-020**: Concurrent use of the Publishing UI by multiple operators MUST not corrupt server or design definitions; last-write-wins is acceptable where the product has no merge UI today, with success/failure clearly shown.

### Key Entities

- **Site (publishing)**: A publishable site shown in ops lists and/or design hierarchy; has servers, editions, and logs.
- **Publish server**: Named delivery configuration for a site (type, driver, properties, default/publish-now flag, production/staging).
- **Edition**: Design/runtime unit that groups content lists and can be executed as a publish job.
- **Content list**: Definition of which items are selected for an edition run (query, incremental, selected items, legacy variants).
- **Context / location scheme**: Rules for generating published locations/links (modern and legacy scheme forms).
- **Delivery type**: Registered delivery mechanism used by publishing infrastructure.
- **Publishing job**: A running or completed execution with status, progress, timing, and stoppability.
- **Publishing log / log item**: Historical record of a job and per-item outcomes.
- **Publish action (item)**: Allowed item-level publish/takedown/stage operations and scheduling metadata.
- **Incremental queue**: Set of items (and related items) pending incremental publish for a site/server.

## Success Criteria

### Measurable Outcomes

- **SC-001**: An automated Playwright test verifies a publisher can complete **site select → full publish → visible job in status** in under **2 minutes** on a reference environment with a preconfigured server (excluding actual job runtime).
- **SC-002**: **100%** of capability groups listed in `research/inventory.md` §7 that are marked in-scope for a cutover milestone have passing Playwright E2E automated test scripts before that surface’s classic UI is removed.
- **SC-003**: In usability validation with at least five representative publishers, or via automated keyboard/accessibility checks using Playwright/A11y tools, **≥80%** successfully complete full publish **without** opening Design documentation or Design UI.
- **SC-004**: Status for active jobs reflects progress updates at least as often as the current Publish status experience (no worse operational visibility).
- **SC-005**: Zero regressions on item publish-now / takedown / stage paths, enforced by automated Playwright E2E integration tests added to the test suite for those APIs.
- **SC-006**: After final retirement milestone, production packaging does not require classic Minuet Publish **and** JSF Design **and** JSF Runtime clients for users to perform their respective tasks; known classic URLs resolve per FR-014.
- **SC-007**: Critical user-visible Publishing chrome strings resolve from localization catalogs (not fixed single-locale hardcoding as the product bar).
- **SC-008**: Diagnostic flows for failed publishes are verified via automated Playwright test scripts simulating failures, ensuring the unified Status/Logs experience provides sufficient detail to diagnose representative failure cases.

## Assumptions

- **Parity means outcomes, not pixel clones**: Layouts may improve for ease of use; every user-visible capability and business rule from the three surfaces remains available unless explicitly deprecated in a later clarify/plan decision.
- **Phased cutover by surface is allowed**: Ops (Stories 1–3) may ship and retire Minuet Publish before Design/Runtime JSF retirement, provided navigation remains coherent and dual entry points are temporary and documented. Final state is one unified Publishing UI.
- **Engine reuse**: Existing sitemanage publish/status/server services and system publisher/rx.publisher remain backend of record; plan phase may add thin adapters only where Design JSF has no REST equivalent today—without inventing undocumented engine behavior.
- **Track B stack**: Implementation targets the existing React + TypeScript + Vite modern UI pipeline and mount/registry patterns already used for Dashboard/Home/Widget Builder (see WebUI AGENTS Track B).
- **Item actions stay content-adjacent**: Item publish-now need not be fully redesigned into the site shell in P1, but must not regress; deeper embedding can follow.
- **Legacy design objects remain editable**: No forced migration of legacy content lists or location schemes in this feature.
- **Desktop Content Explorer** publish-related actions are out of primary scope unless plan identifies a hard dependency; web Publishing UI is the consolidation target.
- **Big-bang within a surface**: Once a surface’s modern parity is accepted, that surface does not remain dual-path long term (aligns with modernization features such as 989).
- **Help docs** are capability references; online help rewrite may trail UI but in-app guidance (empty states, labels) ships with UI.
- **Default for ambiguous design APIs**: Prefer existing web services / server services discoverable in-repo; if a Design-only operation lacks any server API, plan documents the gap and tasks include a minimal server adapter—not a browser-side reimplementation.
- **Design “sites” mean publishable site list + design metadata (context variables), not full CMS site lifecycle CRUD** (create site from URL, delete site, etc. remain site admin).
- **Post-implement residual (US9)**: After baseline shell cutover, remaining Minuet parity polish is tracked as User Story 9 (incremental approval UI, status sort, log filters/details, optional JSF deep-page packaging). Entry-path retirement does not require every faces JSP file deleted in the same release if redirects cover product navigation.

### User Story 9 - Ops parity residual and packaging hygiene (Priority: P2)

Publishers need full Minuet-equivalent incremental **approval** when related items require it, sortable status columns, and logs filters/details sufficient for day-to-day diagnosis without classic Minuet. Optionally, residual deep JSF faces pages are removed from packaging once entry redirects are proven.

**Why P2**: Baseline modern shell ships value first; these close matrix rows OPS-18/20/22/23 and RET-06 without blocking ops primary path.

**Acceptance Scenarios**:
1. **Given** related items require approval, **When** the user confirms incremental publish, **Then** the product uses the same approval/publish rules as Minuet (including `publishIncrementalWithApproval` when required).
2. **Given** multiple status rows, **When** the user sorts by a column, **Then** order changes predictably for that column.
3. **Given** historical logs, **When** the user filters by supported site/server/time window, **Then** the list matches those criteria; opening a row shows structured item-level detail (not raw JSON only).
4. **Given** residual deep faces packaging cleanup is in scope for the release, **When** packaging is reviewed, **Then** exclusive deep design/runtime faces are absent or explicitly justified.

## Out of Scope

- Rewriting the publishing engine, assembly pipeline, or edition task framework for its own sake.
- Delivery Tier Service (DTS) microservice redesign.
- Full Desktop Content Explorer modernization.
- Changing publish package (`.ppkg`) formats or unrelated admin JSF areas.
- New publishing cloud products or net-new delivery drivers beyond parity with what the product already supports in the three surfaces.
- Full CMS site create/delete/copy inside the Publishing Design panel (site administration).

