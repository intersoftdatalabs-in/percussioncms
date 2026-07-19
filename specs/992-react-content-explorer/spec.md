# Feature Specification: Unified React Content Explorer

**Feature Branch**: `992-react-content-explorer`  
**Created**: 2026-07-19  
**Status**: Draft  
**Input**: Consolidate the main Finder UI (dynatree/fancytree miller-column navigation, heavy iframe usage) and the Desktop Content Explorer UI (Java applet / Swing / AWT / JavaFX) into a single modern React UI that adopts the Content Explorer interaction model. Close capability gaps (folder permissions, ACLs, customizable action menus). Provide a reusable content browser component for dialogs and other features that need navigate / search / locate content.

## Module Scope
- **Primary module(s)**: `WebUI/` (Finder includes, Web Management shell, modern React/TypeScript frontend under `WebUI/src/main/ts` and `WebUI/src/main/frontend`); `modules/DesktopContentExplorer/` (source of UX and capability requirements; retirement target)
- **Secondary / integration modules**: `projects/sitemanage/` (path management, folder helpers, permissions); `system/` (action menus, folder ACLs, content services, content browser dialog JSPs); `rest/` (public REST where applicable); related content browser consumers (Active Assembly / editors that open browse/locate dialogs)
  - **`system/` scope clarification**: `system/` is in scope **only when host server-side wiring is required** to replace a legacy dialog/action JSP. The default modern path is browser-to-REST via `WebUI` + `projects/sitemanage` + `rest`. A `system/` task is added **only when an individual host hard-cut proves a need** (see `tasks.md` US2 per-host tasks). Otherwise `system/` is not touched by this feature.
- **AGENTS files to apply**: root `AGENTS.md`, `WebUI/AGENTS.md` (if present), module AGENTS under Desktop Content Explorer / sitemanage when changing those modules
- **User roles affected**: content contributors and editors (browse, open, create, search, basic folder ops); publishers and site admins (folder security, bulk/content ops); system admins (action menu configuration, ACL/community rules); integrators embedding content pickers in dialogs
- **Install / upgrade impact**: web UI distribution and navigation entry points; possible new or extended REST contracts for actions/ACLs if gaps exist; **no** required DB schema change for first ship if existing folder ACL and action configuration storage is reused. Desktop Content Explorer client distribution becomes optional then removed after cutover. Coordinate with Home Library work in `989-react-cui-widget-builder` (shared browse component).
- **Target product release**: **8.2** — full feature scope (all capability-matrix in-scope rows) is required for 8.2 GA; **functional parity blocks the 8.2 release** (FR-029, SC-012).

## Related work
- **Unified UI Plan** (`docs/ai-generated/tasks/#000-unified-ui-plan/`): Track B strategic React consolidation; Desktop CE listed as web-based replacement; Finder lives in WebUI Legacy layer.
- **989 Home / Widget Builder**: Home **Library** is contributor browse/open only; this feature owns **full** content exploration for Web Management and admin content work, plus a **reusable** browser that Home Library and dialogs can adopt.
- **FancyTree migration** (completed tactical work): does not fix miller-column UX, iframe coupling, or missing CE capabilities—this feature supersedes Finder as the long-term surface.

## Clarifications
### Session 2026-07-19
- Q: How should production handle Finder vs modern explorer during transition? → A: **Hard cut per phase** — when a surface is declared ready, that release ships modern-only for it; no classic Finder fallback in production for that surface.
- Q: What MUST be complete before the primary-navigation hard cut (Finder retired)? → A: **Core navigate only** — tree + list, open/create/move/copy/delete; menus may be a **ReducedAction set** (see FR-010a); ACL and full search may ship after Finder hard cut.
- Q: When must dialog/host content-browser migrations hard-cut relative to Finder primary-nav hard cut? → A: **Independent host phases** — reusable browser can land before/with/after Finder hard cut; each host hard-cuts when migrated; not all hosts required with Finder.
- Q: Minimum bar before Desktop Content Explorer hard cut? → A: **Same as Finder hard cut** — core navigate only; Desktop CE may retire as soon as modern explorer replaces Finder primary nav (menus/ACL/full search not required for CE retirement).
- Q: How should advanced CE-only capabilities be scoped? → A: **All in-matrix post-cutover** — clipboard, site-copy wizards, dependency/IA views are phased work **inside** this feature after retirement (not retirement gates).
- Q: Target product release and release gate? → A: **Everything in this feature is in scope for product release 8.2.** Engineering phases (hard cuts, PR trains) may be ordered within the 8.2 train, but **functional parity** against the capability matrix (core navigate, reusable browser + in-scope hosts, full action menus, folder ACL UI, search, and advanced CE matrix rows) **blocks 8.2 GA**. Incomplete parity MUST NOT ship as 8.2. Work MUST NOT be deferred to a post-8.2 product release.

## Problem statement
Today content exploration is split across two incompatible UIs:

| Surface | Stack | Strengths | Weaknesses |
|---------|--------|-----------|------------|
| **Finder** (Web Management / Home library mode) | jQuery + FancyTree, miller columns, deep iframe coupling | In-browser, no desktop install | Mac-like column navigation confuses many users; limited admin actions; weak/missing folder permission & ACL UX; hard to embed cleanly; maintenance cost |
| **Desktop Content Explorer** | Swing/AWT + JavaFX WebView, SOAP + HTTP | Explorer tree + detail list; powerful **server-driven action menus**; folder properties/security (ACL); search; clipboard; mature content admin workflows | Desktop/JavaFX runtime; separate from modern web shell; hard to support; not usable inside web dialogs |

Users and support staff repeatedly prefer the Content Explorer **mental model** (tree + list, rich context actions) over Finder’s miller columns. The product needs **one** modern web surface that preserves CE’s strengths and kills both legacy stacks over time.

## User Scenarios & Testing
Each story must be independently testable.

### User Story 1 - Explorer-style content workspace (Priority: P1)

Content managers open the product’s primary content navigation surface and work in an **explorer-style** layout: a navigable folder/site tree on one side and a detail list (or equivalent multi-column list) of the selected folder’s children on the other—not miller-column “Mac Finder” columns as the primary paradigm. They expand folders, select items, open content for edit/preview, create folders/items where permitted, and move/copy/delete with clear feedback. The workspace is a first-class modern web view in the CM shell (same session, chrome, and visual language as Dashboard / modern Home), without embedding the legacy Finder column chrome or the desktop Content Explorer application.

**Why P1**: Replaces the primary pain of Finder UX and establishes the interaction model for all later stories.

**Acceptance Scenarios**:
1. **Given** a signed-in user with content access, **When** they open the primary content exploration entry (Web Management content area / successor of Finder), **Then** they see an explorer layout (tree + detail list) with sites/folders they are allowed to see—not miller-column navigation as the only/default mode.
2. **Given** the user expands folders in the tree and selects a folder, **When** the detail pane loads, **Then** they see that folder’s children with identity and key metadata (name, type, and other columns appropriate to display format), and can open an item for edit or preview consistent with product rules.
3. **Given** the user has create rights in a folder, **When** they create a folder or supported content type via the workspace actions, **Then** the new item appears after refresh/navigation without requiring the desktop CE client.
4. **Given** the user moves, copies, or deletes an item they are allowed to change, **When** the action completes, **Then** the tree and list reflect the result; on failure they see a clear, non-blank error.
5. **Given** a deep or large folder, **When** they navigate, **Then** the UI remains usable (pagination or virtualization as needed; no unresponsive blank iframe state characteristic of legacy Finder edge cases).
6. **Given** session timeout or insufficient permission, **When** the user attempts navigation or an action, **Then** they receive re-login or access-denied messaging without a hung embedded frame.

### User Story 2 - Reusable content browser component (Priority: P1)

Authors and editors invoke a **content browser** from dialogs and other features (for example: pick a page/asset/folder, locate content for linking, choose a target folder) and complete navigate / search / select / confirm without leaving the host dialog flow. The same browser capability is available as an embeddable component (not a one-off page), so product features share one navigate-and-locate experience instead of forking Finder miller columns, Dojo/legacy content browser dialogs, or desktop CE.

Host migrations are **independent phases**: the browser capability may ship before, with, or after the Finder primary-nav hard cut. Each host (or host group) hard-cuts when it is migrated—**not** all hosts need to move in the same release as Finder.

**Why P1**: Unblocks editors and AA/dialog flows incrementally; prevents a third parallel browser design as hosts modernize.

**Acceptance Scenarios**:
1. **Given** a host feature opens the content browser in select mode, **When** the user navigates or searches and confirms a selection, **Then** the host receives the selected item identity (and path or other agreed selection payload) and the dialog closes or returns focus cleanly.
2. **Given** the browser is configured for folder-only or type-filtered selection, **When** the user tries to select a disallowed type, **Then** confirm is disabled or rejected with a clear message.
3. **Given** multi-select is enabled by the host, **When** the user selects multiple allowed items and confirms, **Then** the host receives the full selection set.
4. **Given** the same user opens the full Content Explorer workspace and a dialog browser, **When** they navigate, **Then** both use the same navigation/search behaviors and permission rules (single product model).
5. **Given** a specific host is declared ready for hard cut, **When** that release ships, **Then** that host uses the modern browser (not miller-column Finder and not desktop CE) with **no** production classic fallback for that host; other unmigrated hosts MAY still use their prior mechanism until their own hard-cut phase.

### User Story 3 - Context actions and customizable menus (Priority: P2)

Users act on folders and items through **context menus and menu bars** driven by the product’s action configuration (the Content Explorer model): available actions depend on selection type, permissions, and workflow state. Menus are richer and more consistent than Finder’s limited button set. Administrators continue to configure or extend actions using existing product action configuration capabilities where those already control CE; the modern UI consumes those definitions rather than hard-coding a tiny fixed set of buttons only.

**Why P2**: CE’s action system is a primary reason to prefer CE over Finder. Full menus are **not** required for an intermediate Finder/CE hard-cut commit *within* the 8.2 train, but they **are** required for **8.2 GA functional parity** (FR-029).

**Acceptance Scenarios**:
1. **Given** a user right-clicks (or uses the keyboard menu equivalent) on a folder or content item, **When** the menu opens, **Then** they see actions appropriate to that selection and their rights (including open, edit, workflow-related actions where applicable—not a generic empty menu).
2. **Given** an action that CE exposes for the same selection today (e.g. open properties, force check-in, transition), **When** that action is in the modern menu configuration for the user’s role, **Then** invoking it performs the equivalent server-backed operation and refreshes the view.
3. **Given** the user has no permission for an action, **When** the menu is built, **Then** that action is hidden or disabled consistently with security rules (not merely failing after click without explanation).
4. **Given** product action configuration already customizes menus for CE, **When** an administrator relies on that configuration, **Then** the modern explorer reflects those customizations for equivalent contexts without a separate parallel menu product.
5. **Given** keyboard-only use, **When** the user opens the context menu and chooses an action, **Then** the flow is operable without a mouse (accessibility bar aligned with other modern CM UI).

### User Story 4 - Folder permissions and ACLs (Priority: P2)

Site admins and content admins manage **folder security** from the modern explorer: view and edit folder permission levels and ACL entries (users/roles/communities as supported by the product today in Content Explorer), including warnings when a change would lock the current user out. Contributors without security rights can still see that they lack permission when an action is denied; they do not need the desktop CE security dialogs.

**Why P2**: Explicit gap vs Finder; required for CE replacement credibility for admin users.

**Acceptance Scenarios**:
1. **Given** a user with folder admin rights, **When** they open folder properties/security for a folder, **Then** they can view current permission/ACL configuration and save allowed changes.
2. **Given** a change that would remove the current user’s own access, **When** they attempt to save, **Then** the product warns before applying (CE-equivalent safeguard).
3. **Given** a user without security rights, **When** they open folder properties, **Then** security editing controls are absent or read-only per product rules.
4. **Given** ACL/permission changes are saved, **When** another user session refreshes navigation, **Then** visible folders and allowed actions match the new rules.
5. **Given** invalid or conflicting ACL input, **When** save is attempted, **Then** the user sees a clear validation or server error and the previous configuration remains until a successful save.

### User Story 5 - Search and locate at scale (Priority: P2)

Users search for content from the explorer workspace and from the reusable browser (simple and advanced criteria consistent with product search capabilities used by CE/Finder for content locate), open results, and navigate to an item’s folder location. Saved or catalogued searches that CE exposes for everyday content work remain available or have a documented modern equivalent for the same user roles.

**Why P2**: Locate-by-search is essential when trees are large; both legacy UIs offer search. Post-cutover relative to Finder/CE retirement hard cuts.

**Acceptance Scenarios**:
1. **Given** the user runs a content search from the explorer, **When** results return, **Then** they can open an item or reveal it in the tree/list.
2. **Given** the reusable browser is in search-capable mode, **When** the user searches and selects a result, **Then** the host receives the selection as in US2.
3. **Given** no results or a server error, **When** search completes, **Then** empty and error states are explicit (not a blank panel).
4. **Given** the user lacks rights to an item in results, **When** they try to open it, **Then** access is denied cleanly.

### User Story 6 - Retire Finder and Desktop Content Explorer (Priority: P3)

After each surface’s readiness bar is met, operators ship a product release with a **hard cut** for that surface: **no classic production fallback**.

**Shared minimum bar for Finder primary-nav and Desktop CE retirement *within the 8.2 train*:** modern explorer **core navigate** only—tree + detail list; open/preview; create/rename/copy/move/delete where permitted; session/permission errors handled. A **ReducedAction set** (FR-010a) is acceptable for that intermediate hard cut. **Full** server-driven action menus (US3), folder ACL UI (US4), full search parity (US5), advanced CE tools (US7), and in-scope host browser migrations (US2) remain **required before 8.2 GA** as functional parity (FR-029)—they may complete after intermediate hard cuts but **must** complete in the same product release train (8.2).

Finder and Desktop CE hard cuts MAY land in the same or sequential **feature-branch / integration** drops within 8.2 once the shared core bar is met. Dialog browse hosts hard-cut **independently** when migrated (FR-008a), still all before 8.2 GA. Known entry points and deep links are rewired or redirected; docs stop presenting retired surfaces as supported.

**Why P3**: Intermediate retirement can proceed on core navigate to kill classic UI early in the train; **8.2 customer release** still requires full functional parity. Permanent dual primary UIs and production classic fallbacks are forbidden once a surface hard-cuts.

**Acceptance Scenarios**:
1. **Given** the release that declares Finder retired for primary navigation, **When** users open Web Management content exploration, **Then** they land on the modern explorer only—**no** miller-column Finder chrome and **no** production fallback toggle/URL to classic Finder for that surface.
2. **Given** that release, **When** a removal/cutover inventory is reviewed, **Then** production navigation for that phase does not ship classic Finder as a supported alternate; dialog hosts migrate under their own hard-cut phase(s).
3. **Given** desktop CE is retired for in-scope workflows (same core-navigate bar; may be same or later release than Finder hard cut), **When** install/upgrade docs and distribution are checked, **Then** CE is marked deprecated/removed for those workflows and support points users to the modern web explorer—without requiring full menus/ACL/search first.
4. **Given** bookmarks to old Finder-centric or CE launch URLs, **When** opened after the relevant hard cut, **Then** known URLs map to modern destinations; unknown ones show a clear moved/unavailable message (not classic UI).
5. **Given** automated tests that only exercised miller-column Finder or desktop CE UI automation for in-scope flows, **When** that surface’s hard cut lands, **Then** they are replaced by modern UI and/or service-contract coverage (not permanently skipped).

### User Story 7 - Advanced CE capabilities (Priority: P3) — required for 8.2 parity

After core-navigate retirement of Finder and/or Desktop CE *within the train*, implementers deliver remaining Content Explorer–grade capabilities tracked in the capability matrix, including at minimum: multi-item **clipboard** (copy/paste across folders), multi-step **site and subfolder copy wizards**, **dependency viewer**, and **IA/relationship** views beyond basic open/edit. Ordering is defined in the matrix; these do not block intermediate US6 hard cuts, but **incomplete rows block 8.2 GA** (FR-029, SC-012).

**Why P3**: Full CE depth stays inside this feature and inside **8.2**; matrix-driven delivery prevents silent scope loss while allowing ordered implementation.

**Acceptance Scenarios**:
1. **Given** the capability matrix after planning, **When** reviewed for advanced CE rows, **Then** clipboard, site/subfolder copy wizards, dependency viewer, and IA/relationship views each have a phase label and acceptance criteria (no unlabeled omit).
2. **Given** a post-cutover phase for clipboard is delivered, **When** a user copies items and pastes into an allowed folder, **Then** the operation completes under permission rules without desktop CE.
3. **Given** a post-cutover phase for a site/subfolder copy wizard is delivered, **When** an authorized user completes the wizard, **Then** the copy outcome matches product rules documented for that matrix row.
4. **Given** dependency or IA/relationship views are delivered for their phase, **When** a user opens them from a selection, **Then** they can inspect relationships/dependencies without desktop CE.
5. **Given** this feature is proposed as complete, **When** SC-011 is checked, **Then** all advanced matrix rows are Done or explicitly scheduled with owners (not silently dropped).

### Edge Cases
- Multi-site repositories and empty site lists (admin vs contributor messaging).
- Folders with thousands of children (performance, pagination, sort).
- Concurrent rename/move of the same folder by two users.
- Mixed selection (folder + item) and multi-select action availability.
- Items checked out by another user; workflow state blocking transitions.
- Community filtering and folder ACLs combining to hide nodes.
- Iframe-heavy legacy editor still open beside modern explorer (session/CSRF shared; no cross-frame Finder assumptions).
- Dialog browser opened while full explorer is also open (independent selection state).
- Keyboard and screen-reader paths for tree, list, menus, and security forms.
- Locale/TMX: UI chrome follows product localization patterns used by other modern CM surfaces.
- Network failure mid-action: partial failure messaging and safe refresh.
- Upgrade with users still running desktop CE against a server that prefers modern UI (server remains compatible until CE retirement story completes; no silent data corruption).

## Requirements
### Functional Requirements

#### Navigation & workspace
- **FR-001**: Product MUST provide a primary **web** content exploration workspace whose default navigation paradigm is **explorer-style** (hierarchical tree + detail list of children), not miller-column folder navigation.
- **FR-002**: The workspace MUST allow users to expand/collapse folders, select folders and items, and open items for edit/preview according to existing content rules and permissions.
- **FR-003**: The workspace MUST support create, rename, copy, move, and delete for folders and content items where the user has rights, with confirmation for destructive actions.
- **FR-004**: The workspace MUST honor session authentication, CSRF, community context, and folder permission checks already enforced by the CMS; UI MUST NOT show actions the server will always reject without a product reason.
- **FR-005**: The workspace MUST integrate with the modern CM navigation shell (reachable from main product navigation alongside Dashboard/Home as appropriate) and MUST NOT require the Desktop Content Explorer runtime for in-scope web workflows. Shell-mount integration (entry-point registration, chrome consistency, no Finder-only assumptions) MUST be verified as part of US1 acceptance evidence — see `tasks.md` US1 shell-mount task.

#### Reusable content browser
- **FR-006**: Product MUST provide a reusable **content browser** capability for embed/host use in dialogs and other features supporting at least: navigate hierarchy, search/locate, single-select, optional multi-select, type/folder filters, cancel/confirm.
- **FR-007**: The content browser MUST return a stable selection result to the host (item id and path or equivalent product identifiers already used by CMS integrations).
- **FR-008**: Host features that today open legacy content browser or Finder-based pickers for in-scope locate flows MUST be migratable to the reusable browser; the feature MUST document the host integration contract for implementers. The concrete **in-scope host IDs** for 8.2 (`host-asset-picker`, `host-page-picker`, `host-aa-contentbrowser-dialog`, `host-folder-picker`, optional `host-home-library`) are enumerated in `contracts/capability-matrix.md` P-Host rows; per-host implementation tasks are T045a–T045f.
- **FR-008a**: Host migrations are **independent hard-cut phases within the 8.2 train**. The reusable browser MAY land before, with, or after Finder primary-nav hard cut in the development/integration sequence. Product MUST NOT require all in-scope hosts to migrate in the same intermediate drop as Finder. When a host is declared ready, that drop hard-cuts **that host only** (no production classic fallback for that host). **All in-scope hosts MUST be hard-cut before 8.2 GA** (FR-029).
- **FR-009**: The full explorer workspace and the reusable browser MUST share the same permission and visibility rules for nodes (no “see in dialog but not in explorer” inconsistency for the same user/session).

#### Actions & menus
- **FR-010**: Product MUST eventually present **context menus** (and primary menu/toolbar actions where applicable) for the current selection, driven by the CMS **action configuration** model used by Content Explorer—not a Finder-only fixed button strip as the **long-term** sole action surface. Full action-configuration-driven menus (US3) are a **post-retirement capability phase**, not a gate for Finder or Desktop CE hard cut.
- **FR-010a (ReducedAction set at intermediate hard cut)**: At Finder primary-nav hard cut, product MUST expose a **ReducedAction set** sufficient for open/preview and create/rename/copy/move/delete (and related confirms). That set MAY be product-defined rather than fully configuration-driven. The ReducedAction set is the **intermediate** action surface for FR-019b and is **superseded** (not duplicated) by FR-010 once full configuration-driven menus ship. The concrete ReducedAction entries are enumerated in `data-model.md` § *ReducedAction* and the ≥10 high-value full-action list for FR-010 is enumerated in `contracts/capability-matrix.md` P-Menu rows.
- **FR-011**: When full action menus are enabled, available actions MUST vary by object type, selection, workflow state, and user authorization consistent with server rules.
- **FR-012**: Invoking a supported action MUST execute the corresponding CMS operation and refresh affected tree/list state (or open the appropriate editor/dialog).
- **FR-013**: Product MUST support keyboard access to open context actions and activate menu items on the primary explorer surfaces for actions that are present in the current phase.

#### Folder permissions & ACLs
- **FR-014**: Product MUST allow authorized users to view and edit **folder permissions and ACL entries** from the modern explorer (parity with Content Explorer folder security capabilities for the permission model the product already stores).
- **FR-015**: Product MUST warn before saving ACL changes that would remove the current user’s access to that folder.
- **FR-016**: Users without folder security rights MUST NOT be able to modify ACLs via the modern UI.

#### Search
- **FR-017**: Product MUST provide content search from the explorer workspace sufficient to locate items by criteria comparable to everyday CE/Finder content search usage, and to open or reveal results in context. Full search is a post-retirement capability phase relative to Finder/CE hard cuts unless delivered earlier.
- **FR-018**: The reusable browser MUST support search-based locate when the host enables it.

#### Advanced CE (matrix; 8.2 parity)
- **FR-028**: Advanced Content Explorer capabilities—including multi-item **clipboard** (copy/paste), multi-step **site/subfolder copy wizards**, **dependency viewer**, and **IA/relationship** views beyond basic open/edit—MUST be inventoried in the capability matrix and delivered as ordered phases **within this feature and within 8.2**. Product MUST NOT treat them as permanent drops or post-8.2 deferrals solely because they are not intermediate retirement gates. Phase ordering and acceptance for each advanced capability are defined in the matrix during planning.

#### Cutover & retirement
- **FR-019**: Product MUST plan work in phases **within the 8.2 release train**, for example: (1) modern explorer **core navigate** ready → Finder primary-nav hard cut and/or Desktop CE hard cut (same intermediate bar), (2) dialog/browser **host hard cut(s)** on independent schedules (FR-008a), (3) full action menus / ACL / search, (4) advanced CE tools (FR-028). Phases MAY land in sequential PRs/merges **inside 8.2** and need not be strictly ordered beyond documented dependencies (e.g. a host needs the browser capability). Phases MUST **not** be deferred to a product release after 8.2. For each **retirement/host hard-cut** phase, the integration drop that declares that surface ready MUST apply a **hard cut**: production MUST NOT ship a classic fallback (toggle, alternate URL, or dual primary path) for that surface. The detailed no-fallback-in-shipped-builds policy and the intermediate core-navigate bar are stated in FR-019a and FR-019b respectively (no duplication here).
- **FR-019a (no classic fallback in shipped builds)**: Feature-branch and pre-release QA MAY exercise modern and classic side by side. **Shipped customer builds** (including 8.2 GA) for a completed hard-cut surface MUST be modern-only for that surface (no supported classic Finder or dual primary path in production for that surface).
- **FR-019b (intermediate core-navigate bar)**: The readiness bar for **Finder primary-navigation hard cut** and **Desktop Content Explorer hard cut** *as intermediate train milestones* is the **same: core navigate only** (FR-001–FR-005 and FR-003 ops): tree + list; open/preview; create/rename/copy/move/delete with confirmations; clear permission/session errors. Product MAY use a **ReducedAction set** (FR-010a) for those ops at that intermediate hard cut. Product MUST NOT require US3/US4/US5/US7 as gates for those intermediate retirement milestones. Product MUST still complete US2 (in-scope hosts), US3, US4, US5, and US7 (matrix) for **8.2 GA** (FR-029).
- **FR-029**: **Target product release is 8.2.** All in-scope work for this feature—including core explorer, Finder and Desktop CE hard cuts, reusable content browser, all in-scope host migrations, full action menus, folder ACL UI, search, and advanced CE capability-matrix rows—MUST be complete for **8.2 GA**. **Functional parity** (defined as: capability matrix in-scope rows **Done** with acceptance criteria met; SC-001–SC-011 applicable outcomes green for 8.2 scope) **blocks the 8.2 release**. Product MUST NOT ship 8.2 with incomplete functional parity, dual primary content explorers, or matrix rows left as “later release.”
- **FR-020**: When Finder is retired for primary navigation, production MUST NOT load miller-column Finder as the content exploration UI (not merely “not default”—classic is absent as a supported path).
- **FR-021**: When Desktop Content Explorer is retired for in-scope workflows (same core-navigate bar as FR-019b), product docs and distribution MUST NOT require it for those workflows; known launch entry points MUST map to the modern web explorer or a clear unavailable message—not launch classic CE as a fallback.
- **FR-022**: Implementers MUST maintain a durable **capability and cutover inventory** under `specs/992-react-content-explorer/` (e.g. `contracts/capability-matrix.md` and `checklists/cutover-inventory.md`) listing CE capabilities, Finder touchpoints, dialog hosts, keep/drop decisions, and test replacements. PR/release review signs off inventories for each retirement phase.
- **FR-023**: Automated tests MUST cover critical explorer navigation, browser selection, at least one ACL save path (authorized), and representative action invocations at modern UI and/or service-contract level with passing CI on the target JDK/branch.
- **FR-024**: Legacy-only tests that solely exercise miller-column Finder or desktop CE for in-scope flows MUST be replaced (not permanently skipped) when those surfaces are removed.

#### Quality, a11y, i18n
- **FR-025**: Primary explorer and browser surfaces MUST meet the same accessibility bar as other modern CM UI (keyboard operable, labeled controls, operable menus).
- **FR-026**: User-visible chrome strings MUST follow product localization practice used by other modern CM surfaces (TMX / existing client message resolution)—not English-only hardcoding as the production bar.
- **FR-027**: Display of list columns SHOULD respect product display formats where CE uses them for folder contents; if a reduced default column set ships first, the capability matrix MUST mark full display-format parity as a follow-on with explicit status.

### Key Entities
- **Content Explorer workspace**: Primary web UI for browsing and managing the content repository hierarchy.
- **Content browser (component)**: Embeddable navigate/search/select experience for dialogs and host features.
- **Folder / path item**: Node in the site/folder tree with permissions and children.
- **Content item**: Page, asset, or other CMS object listed under a folder.
- **Action / menu definition**: Configured operation available for a selection (open, edit, transition, properties, etc.).
- **Folder ACL / permission**: Access control entries and permission level on a folder.
- **Search query / result**: Criteria and matching items for locate flows.
- **Selection result**: Payload returned to a host dialog (ids, paths, types).
- **Cutover inventory**: Documented map of legacy surfaces → modern replacements.
- **Capability matrix**: Phased inventory of CE capabilities (core, menus, ACL, search, advanced tools) with acceptance labels.

## Success Criteria
### Measurable Outcomes
- **SC-001**: In scripted UAT for the **Finder primary-nav hard cut**, trained content users complete navigate → open item → create folder → move or copy → delete (where permitted) **entirely in the modern web explorer** with **100%** of checklist steps passed and **zero** steps requiring miller-column Finder. Full CE menus, ACL UI, and full search are **not** required for this gate.
- **SC-002**: In scripted UAT for **each browser host hard-cut phase**, that host’s dialog flow completes navigate or search → select → confirm using the **reusable content browser**, returning a valid selection to the host, **100%** of checklist steps passed (other hosts may still be unmigrated).
- **SC-003**: For a checklist of high-value CE actions (minimum **10** actions spanning open/edit, folder ops, and at least two workflow or properties actions), each action is available from the modern UI for an authorized user and completes successfully in UAT (**100%** of list)—gate for **full menu / CE-parity phase**, not for Finder primary-nav hard cut.
- **SC-004**: Authorized admin completes view → edit → save of folder ACL/permissions on a test folder in the modern UI; a second user session reflects access changes on refresh (**pass/fail** scripted scenario)—gate for **ACL phase**, not Finder primary-nav hard cut.
- **SC-005**: Explorer remains usable on a folder with a large child set used in UAT (product-agreed fixture, target **≥ 500** children): user can scroll/page and open an item within **10 seconds** of selecting the folder on a standard office network in the test environment (applies at Finder primary-nav hard cut).
- **SC-006**: After Finder primary-navigation hard cut, production Web Management content entry loads **zero** miller-column Finder chrome and offers **no** production classic fallback; cutover inventory items for that phase are **100%** signed off; SC-001 and SC-005 pass.
- **SC-007**: After Desktop CE retirement for in-scope workflows, UAT and docs confirm core-navigate content admin workflows are completed on modern web only; CE not required (**pass** on retirement checklist)—same core-navigate bar as SC-001/SC-006; full menus/ACL/search not required for this gate.
- **SC-008**: CI is green for automated coverage required by the phase under test (core navigate at minimum for Finder hard cut; FR-023 expands as menus/ACL/browser land); no permanent skip of replaced legacy-only tests for retired surfaces.
- **SC-009**: Accessibility spot-check for surfaces in the phase under test (at Finder hard cut: primary tree and list at minimum) are keyboard-completable in UAT checklist (**100%** of that phase’s a11y steps).
- **SC-010**: Prefer-CE usability signal: in structured feedback from at least **5** internal users familiar with both UIs, majority rate modern explorer as **clearer for daily folder navigation** than miller-column Finder (document scores in UAT notes)—measured at or before Finder primary-nav hard cut.
- **SC-011**: Capability matrix lists advanced CE items (clipboard, site/subfolder copy wizards, dependency viewer, IA/relationship views) with phase labels; **100%** of in-scope matrix rows (including advanced) are **Done** with acceptance met before 8.2 GA (no unlabeled omit; “scheduled for post-8.2” is **not** allowed).
- **SC-012**: **8.2 release gate** — 8.2 GA is blocked until functional parity is proven: (1) Finder and Desktop CE retired for in-scope workflows, (2) in-scope content-browser hosts hard-cut, (3) full menus + ACL UI + search complete per matrix, (4) advanced CE matrix rows Done, (5) SC-001–SC-011 applicable checks pass for the 8.2 build. A build missing any of the above MUST NOT be labeled or shipped as 8.2 GA.

## Assumptions
- Strategic UI direction is the existing modern CM web stack (React/TypeScript in `WebUI`, Track B); this feature does not introduce a second SPA framework.
- **Explorer paradigm wins**: tree + detail list is the default; miller columns are not retained as the primary UX (optional advanced layout is out of scope unless later justified).
- Content Explorer’s **server-backed action menus**, **folder security**, **search**, and **tree/list** model define target UX; Finder’s miller columns and iframe-heavy shell do not.
- Existing CMS storage for folder ACLs and action configuration is reused; first ship avoids schema redesign unless a gap is proven during planning.
- SOAP-only CE operations will be replaced by existing or new REST/JSON (or equivalent web) contracts during planning; no permanent browser→SOAP dependency from the modern UI.
- Home Library (`989`) may consume the reusable browser for contributor browse; full admin explorer remains this feature’s responsibility.
- Desktop CE retirement means **web replacement**, not a rewritten JavaFX shell.
- **Target product release is 8.2.** All in-scope capability-matrix work is **8.2 scope**; functional parity **blocks 8.2 GA** (FR-029, SC-012).
- Phased delivery is **within the 8.2 train** (PR/story order), not across product releases after 8.2. **Hard cut per phase** (no production classic fallback once a surface hard-cuts) still applies.
- Pre-release/feature-branch dual exercise is allowed for QA; it MUST NOT ship as a supported production dual path for a completed hard-cut surface, and MUST NOT appear in 8.2 GA.
- **Finder primary-nav hard cut and Desktop CE hard cut share one intermediate bar: core navigate only** (tree/list + open + create/rename/copy/move/delete; ReducedAction set OK per FR-010a). They MAY land in sequential integration drops within 8.2 once that bar is met.
- Full action-configuration menus, folder ACL UI, full search, in-scope host browsers, and advanced CE tools MAY land **after** intermediate hard cuts in the train but **MUST complete before 8.2 GA**.
- **Dialog/host browser migrations are independent hard-cut phases** from Finder/CE retirement within the train; not all hosts need to move with the first hard cut—**all in-scope hosts must complete before 8.2 GA**.
- **Advanced CE-only capabilities** (multi-item clipboard, multi-step site/subfolder copy wizards, dependency viewer, IA/relationship views, and similar) are **in scope for this feature and for 8.2**, tracked in the capability matrix—not intermediate retirement gates, not silent drops, and **not post-8.2 deferrals**.
- Active Assembly editor internals remain separate; only browse/locate dialogs and shared session concerns are in scope for this feature’s browser component.
- Eclipse Workbench, JSF admin/publishing, and Package Manager remain separate Track B efforts.

## Out of Scope
- Rewriting the full content editor, Active Assembly canvas, or template designers.
- Migrating JSF Admin/Publishing, GWT Package Manager, or Eclipse Workbench.
- Replacing DTS public-site UIs.
- Re-implementing Desktop CE as a shipped desktop app on a new toolkit.
- Retaining miller-column Finder as a long-term alternate primary UI.
- Global removal of all jQuery from Web Management beyond Finder and its exclusive dependencies (other admin screens may still use jQuery until their own migrations).
- Redesigning the product’s underlying permission model or action-definition schema (consume and surface what exists; extend APIs only as needed for web access).
- Offline/disconnected desktop content administration.
- Shipping advanced CE tools (clipboard, site-copy wizards, dependency/IA views) **as intermediate hard-cut gates**—they are matrix phases after intermediate retirement, still **required for 8.2 GA** (FR-029).
- Deferring any in-scope functional parity work to a product release after 8.2.

## Dependencies
- Modern CM shell, session/CSRF, and i18n patterns already used by Dashboard / modern Home.
- Path/folder services and permission utilities in sitemanage/system.
- Action configuration and folder ACL services currently used by Desktop Content Explorer.
- Coordination with `989-react-cui-widget-builder` for shared browser adoption on Home Library (non-blocking for explorer P1 if Library keeps interim browse).
- REST/API gap analysis during planning for any CE capability still SOAP-only.

## Risks
- **API gaps**: CE features still on SOAP/desktop-only paths may block parity; mitigate with early gap analysis and phased matrix.
- **Action menu complexity**: Dynamic menus are powerful but easy to get wrong for security; require server-side enforcement + UI tests.
- **Finder embedding depth**: Many WebUI plugins call `$.perc_finder()`; cutover needs a systematic inventory (FR-022).
- **User training**: Users only trained on miller columns need brief guidance to tree+list (mitigate with familiar CE patterns and docs).
- **Scope creep**: CE has decades of edge dialogs; strict capability matrix and Out of Scope prevent unbounded porting.
