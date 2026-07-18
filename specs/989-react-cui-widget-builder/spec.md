# Feature Specification: Migrate Home/Contributor UI and Widget Builder to Modern UI

**Feature Branch**: `989-react-cui-widget-builder`  
**Created**: 2026-07-17  
**Status**: Draft  
**Input**: Migrate CUI Home/Contributor shell and Widget Builder to the modern React UI stack; remove legacy CUI (Knockout) and Widget Builder (Backbone/jQuery) packs. Epics: (1) React Home / Contributor shell replacing CUI iframe + library mode strategy; (2) React Widget Builder keeping existing widget-builder server services; (3) Remove legacy CUI package, Knockout deps, Widget Builder packed bundles, and dead JSP includes.

## Module Scope
- **Primary module(s)**: `WebUI/` (Home JSP shell, CUI SPA under `cm/cui` / `cm/pages/cui`, Widget Builder JSP and client under `cm/widgetbuilder`, modern frontend under `WebUI/src/main/ts` or `WebUI/src/main/frontend`)
- **Secondary / integration modules**: `projects/sitemanage/` (widget builder services and related REST; content/finder APIs used by Home/CUI); shared REST/session/CSRF patterns already used by modern Dashboard
- **AGENTS files to apply**: root `AGENTS.md`, `WebUI/AGENTS.md`, `projects/sitemanage` module AGENTS if present
- **User roles affected**: content contributors (Home / library / create wizards / search); admins and integrators who use Widget Builder (when enabled); all roles using main nav (Home vs Dashboard)
- **Install / upgrade impact**: web UI distribution only (no DB schema, no `.ppkg` format change). Upgrade must preserve navigation entry points and feature flags (e.g. Widget Builder enabled). **No dual-path cutover**: modern Home and Widget Builder ship as the only production path in the same release as US1+US2; US3 legacy removal is part of that switch (big-bang). Home is a **single shell** with sections/routes (not library-mode vs contributor-iframe modes).

## Clarifications
### Session 2026-07-17
- Q: Release cutover strategy for modern vs classic Home/Widget Builder? → A: Big-bang — modern only when US1+US2 ship; no dual-path (US3 same release as switch).
- Q: Deep links and bookmarks for old Home/CUI/Widget Builder URLs? → A: Redirect/map known legacy Home + Widget Builder URLs to modern equivalents; unknown paths get a clear message.
- Q: Home information architecture (library mode vs contributor frame)? → A: Single modern Home shell with sections/routes (Recent, Library, Search, Create)—no dual-mode shell.
- Q: Concurrent Widget Builder edits by two admins? → A: Last write wins; save succeeds; UI shows confirmation; reload shows latest server state.
- Q: Home Library section depth of parity? → A: Contributor browse/open parity (site/folder navigate, open items); advanced finder-only admin actions can stay elsewhere.
- Q: Production removal scope for old UI, product JS, and vendored JS after modern Home/WB? → A: Exclusive-only deletion of CUI SPA, WB client packs, and dead Home/WB includes, **plus** remove orphaned shared vendors (e.g. Backbone/Underscore/Backgrid) only when the post-cutover **manual inventory** proves zero remaining product consumers.
- Q: Fate of classic entry JSPs (`home.jsp`, `widgetBuilder.jsp`) and related includes? → A: Hard cut — delete classic Home/Widget Builder JSPs (no in-file rewrite, no redirect-only stubs); rewire navigation and product config to modern-only modules in the same release.
- Q: Fate of automated tests that only exercise legacy CUI/Widget Builder clients? → A: Same-release replace — remove legacy-only client tests with US3; require equivalent modern UI and/or service-contract coverage (FR-011) to pass CI in that release.
- Q: How must US3 prove retired UI/JS/orphan vendors are gone or intentionally retained? → A: Manual inventory only — documented removal checklist and code review; no required automated absence scan as a CI hard gate.
- Q: Where must the US3 removal inventory live for reviewer sign-off? → A: Feature-docs checklist under `specs/989-react-cui-widget-builder/` (e.g. `checklists/removal-inventory.md`); PR and release review sign it off.
- Q: Localization bar for modern Home and Widget Builder user-visible strings? → A: Catalog-backed via **TMX translation tables** — reuse existing `perc.ui.*` (and related) keys where they fit; add TMX keys for net-new strings. Not English-only React hardcoding as the product bar.
- Q: Translation completeness for net-new TMX keys in this feature? → A: Structural parity — new keys include the same `xml:lang` set as neighboring `perc.ui` units (e.g. en-us + es + hi); non-English may temporarily equal en-us when no translation exists, flagged for later localization.
- Q: Runtime resolution of TMX strings in modern Home/WB React? → A: Existing TMX JS pipeline — modern shell JSPs include `tmx.jsp` with session locale; React/TS resolves keys via `I18N.message` or a thin TypeScript wrapper over that global (no second catalog system).
- Q: What must automated tests prove about i18n? → A: Key presence only — manual/PR checklist that UI strings map to TMX keys (SC-008); no required automated multi-locale or I18N mock assertions in Vitest for this feature.
- Q: How to prevent losing classic Home function while modernizing stack/look? → A: Home capability matrix (`contracts/home-capability-matrix.md`) is normative for Create/open/list MUST rows; redesign preserves classic contributor capabilities (page/asset/blog wizards equal to CUI), not a thinner product.
- Q: Preserve classic My Bookmarks content list on modern Home? → A: **Keep** — required Home section listing bookmarked content (`getMyContent` / item/mycontent) with open-item parity.

## User Scenarios & Testing
Each story must be independently testable.

### User Story 1 - Modern Home / Contributor shell (Priority: P1)

Content contributors open **Home** and work in a **single modern Home shell** (no embedded legacy contributor SPA and no separate “library mode vs contributor mode” shells). Within that shell they use **sections/routes**—at minimum **Recent**, **My Bookmarks**, **Library** (finder-style browse), **Search**, and **Create**—covering page/asset/blog (and related) flows that today’s Home/CUI + library mode provide, with the same visual language and session behavior as the modern Dashboard.

**Why P1**: Home is a primary landing surface; retiring the iframe and dual-mode strategy removes Knockout and unifies navigation.

**Acceptance Scenarios**:
1. **Given** a signed-in user with Home access, **When** they open Home from main navigation, **Then** they see one modern Home shell with navigable sections/routes (not an iframe-only or dual-mode layout).
2. **Given** the user is on Home, **When** they open Recent, My Bookmarks, Library, Search, or Create sections, **Then** they complete list/browse, search, and create outcomes for supported roles; **Library** supports site/folder navigation and opening content items (contributor browse/open parity)—not necessarily every advanced admin finder action available elsewhere in Web Management; **My Bookmarks** lists bookmarked content and opens items.
3. **Given** the user starts a page, asset, or blog creation flow from Home Create (via type chooser and wizard steps equivalent in capability to classic CUI), **When** they complete required fields and confirm, **Then** the content item is created and they can open or locate it as they can today (see capability matrix §6).
4. **Given** deep links that previously selected Home `initialScreen` values (library, list, search, newitem), **When** opened after migration, **Then** they map to the corresponding modern section/route (known mappings; no classic UI).
5. **Given** session timeout or permission errors during Home flows, **When** the user acts, **Then** they receive clear recovery (re-login or denied message) without a blank embedded frame.
6. **Given** a user whose session locale is a non-default product language with TMX coverage, **When** they open modern Home, **Then** primary UI chrome strings resolve from TMX catalogs for that locale (not fixed English-only hardcoding).

### User Story 2 - Modern Widget Builder (Priority: P1)

Integrators and admins (when Widget Builder is enabled) define, edit, validate, and package custom widgets using a modern UI that calls the **existing** widget-builder server capabilities (definitions, fields, display, resources, validation, package generation)—without using the legacy Backbone/jQuery Widget Builder screens.

**Why P1**: Widget Builder is a self-contained exclusive tool; server logic stays; UI modernization unblocks deleting a large legacy pack.

**Acceptance Scenarios**:
1. **Given** Widget Builder is enabled for the user, **When** they open Widget Builder from navigation, **Then** they see a modern builder UI listing existing widget definitions (or empty state).
2. **Given** a user creates or edits a widget definition (metadata, fields, display, resources), **When** they save, **Then** changes persist via existing server services and reappear after reload; concurrent saves use **last write wins** with success confirmation (no required merge UI).
3. **Given** invalid definition data, **When** they validate or save, **Then** validation messages from the server (or equivalent client checks) prevent bad packages and explain what to fix.
4. **Given** a valid definition, **When** they generate/export a package as today, **Then** a package is produced with the same server-side rules as the current builder.
5. **Given** Widget Builder is disabled, **When** the user tries to open it, **Then** access is denied or the entry is hidden, consistent with current product behavior.
6. **Given** a user whose session locale is a non-default product language with TMX coverage, **When** they use modern Widget Builder chrome, **Then** primary UI strings resolve from TMX catalogs for that locale.

### User Story 3 - Remove legacy Home/CUI and Widget Builder clients (Priority: P2)

In the **same release** that ships modern Home (US1) and modern Widget Builder (US2), operators and developers no longer ship or load the obsolete contributor SPA, its Knockout/RequireJS client stack for that surface, Widget Builder packed client bundles, or dead JSP includes that only served those UIs. There is **no** multi-release dual-path of classic vs modern for these surfaces.

**Removal scope (exclusive + proven orphans):**
1. **Must remove (exclusive):** CUI SPA trees and entry wiring (e.g. `cm/cui`, `cm/pages/cui`); Widget Builder client packs (product JS, templates, CSS, packed min bundles); CUI-local vendored Knockout/RequireJS (and other libs only under those trees); **classic entry JSPs** for Home and Widget Builder (e.g. `home.jsp`, `widgetBuilder.jsp` and includes that exist only to serve those classic UIs)—**hard cut**: delete them; do **not** rewrite them in place or leave redirect-only JSP stubs.
2. **Must rewire:** Main navigation, view/route config, and any product wiring that previously targeted classic Home/Widget Builder JSPs MUST point at modern-only modules/entry points in the same release.
3. **May remove (orphans only):** Shared legacy vendors such as Backbone, Underscore, and Backgrid **only after** the post-cutover **manual inventory** proves zero remaining product consumers outside retired surfaces. If any other screen still references a library, that library **stays**.
4. **Must keep:** Shared platform jQuery and other libs still required by remaining Web Management admin screens; Widget Builder **server** services; non-Home/non-WB product UI.
5. **Deep links (already locked):** Known legacy Home/Widget Builder **URLs** still MUST map to modern destinations (server or app-level mapping—not via retained classic JSP stubs). Unknown retired paths still get a clear unavailable/moved message.
6. **Proof of removal:** Durable inventory at `specs/989-react-cui-widget-builder/checklists/removal-inventory.md` (or equivalent under that feature dir); signed off via PR/release review—not an automated CI absence-scan hard gate.

**Why P2 (same-release gate)**: Deletion is gated on US1/US2 parity and UAT, but must land with the switch—not as a later optional cleanup—per big-bang cutover.

**Acceptance Scenarios**:
1. **Given** the release that introduces modern Home and Widget Builder, **When** the product is installed/upgraded, **Then** production pages for Home and Widget Builder do not load the retired contributor SPA or packed Widget Builder client bundles (no classic fallback entry for those screens).
2. **Given** that production build, **When** the documented removal inventory is reviewed against source/distribution, **Then** retired CUI client package, Widget Builder packed artifacts, and classic Home/Widget Builder entry JSPs are absent (hard cut—no classic JSP rewrite or redirect stubs left behind).
3. **Given** main nav and product view config after cutover, **When** a user opens Home or Widget Builder (when enabled), **Then** they land on modern-only modules rewired in config/nav—not on deleted classic JSPs.
4. **Given** automated or manual smoke of main nav (Home, Dashboard, Widget Builder when enabled), **When** tests run, **Then** all pass without depending on Knockout CUI or Backbone Widget Builder scripts.
5. **Given** exclusive removals are complete, **When** the manual orphan-vendor inventory is completed, **Then** any library with zero remaining product consumers is removed from production distribution; any library still referenced by other screens remains and is listed with its consumers.
6. **Given** US3 lands with modern Home/WB, **When** CI runs, **Then** no job depends on deleted legacy CUI/WB client scripts; critical flows are covered by modern UI and/or service-contract tests (legacy-only client tests removed and replaced in the same release).

### Edge Cases
- User has Home access but not Widget Builder (or the reverse).
- Multi-site vs no-site empty states on Home (admin vs non-admin messaging).
- Slow network / large content lists in recent/library views.
- Library section: advanced finder-only admin actions (beyond site/folder browse and open) may remain outside Home.
- Concurrent edit of the same widget definition: **last write wins**; UI confirms save; reload shows server state (no client merge/version block required for this feature unless server already returns conflicts).
- Upgrade mid-session with mixed browser tabs open to old bookmarks (deep-link policy applies; no classic fallback UI).
- Feature flag off for Widget Builder; Home must still work.
- Deep link to retired CUI or old Widget Builder paths: **known** product URLs MUST map to modern equivalents (without classic JSP stubs); **unknown** retired paths MUST show a clear unavailable/moved message (no classic UI).
- Direct request for a deleted classic entry JSP path after upgrade: treated under deep-link policy (map if known; clear message if not)—not a silent 404 without guidance if the path was a documented product entry.
- Accessibility: keyboard and screen-reader paths for create wizards and builder forms.
- CSRF/session expiry during multi-step wizard or long builder edit.
- Missing TMX key: product behavior should remain diagnosable (fallback to key or English per existing TMX/client patterns)—must not blank the UI.
- Server/API error payloads: display server message when provided; do not require inventing TMX keys for every backend exception string in this feature unless the product already catalogs them.

## Requirements
### Functional Requirements
- **FR-001**: Product MUST provide a modern Home / contributor experience that fulfills the primary tasks of today’s Home (browse/recent, library-style access, search, create page/asset/blog and related add flows) without requiring the legacy embedded contributor SPA for those tasks. Modernization is of **presentation and stack**, not a reduction of classic Home/CUI contributor capabilities listed as MUST in the [Home capability matrix](./contracts/home-capability-matrix.md).
- **FR-001a**: Home **Create** MUST implement **page**, **asset**, and **blog post** creation with **equal capability** to classic CUI wizards (type chooser; site/template/folder or type/folder or site/blog selection as applicable; required title/name fields; authorization messaging; persist via existing create semantics; after create, open or locate the item). Free-text-only forms that omit product pickers are **not** sufficient. Detail rows: [home-capability-matrix.md](./contracts/home-capability-matrix.md) §6.
- **FR-001b**: Opening a content item from Home Recent, Library, or Search MUST use the product’s existing path-based open/navigation behavior (classic equivalent: open path item into the editor)—not an incomplete ad-hoc URL that fails for normal contributor items.
- **FR-002**: Product MUST implement Home as a **single shell** with sections/routes including at least **Recent**, **My Bookmarks**, **Library**, **Search**, and **Create**. Product MUST NOT retain separate library-mode vs contributor-iframe shells for the Home nav tab.
- **FR-002b**: Home **My Bookmarks** MUST list the current user’s bookmarked content (classic CUI My Bookmarks / `getMyContent`) and allow opening an item with the same open behavior as Recent (FR-001b).
- **FR-002a**: Home **Library** MUST support site/folder navigation and opening content items for contributor workflows. Full parity with every advanced Web Management finder affordance is NOT required on Home; those may remain on other screens.
- **FR-003**: Modern Home MUST preserve role-appropriate empty states and messaging (e.g. no sites / create site for admins).
- **FR-004**: Modern Home MUST honor existing session, CSRF, and **user locale** (e.g. `sys_lang` / session locale used by product TMX) consistent with other CM screens.
- **FR-021**: User-visible strings on modern Home and Widget Builder (labels, buttons, section titles, empty states, client-side validation chrome, moved/unavailable message, and similar UI copy) MUST be sourced from product **TMX translation tables** (message catalogs). Implementers MUST **reuse** existing catalog keys (e.g. `perc.ui.*`) when semantics match and **add** new TMX keys for net-new copy (typically in `modules/perc-i18n` product UI TMX such as `CmsUi.tmx`). Hardcoded English-only React strings are NOT acceptable as the production localization bar for those surfaces (server-returned error/detail text may still display as provided by the API when it is already localized or is technical detail—see edge cases).
- **FR-022**: Net-new TMX units added for this feature MUST include **structural locale parity** with neighboring `perc.ui` entries in the same catalog (the same `xml:lang` set, e.g. `en-us`, `es`, `hi` as used today). Correct **en-us** source text is required. For other locales, human translations SHOULD be used when available; when not available, the non-English `tuv` MAY temporarily equal the en-us text and MUST be noted for follow-on localization (e.g. in PR notes). Full professional translation of every string is NOT a merge blocker for this feature.
- **FR-023**: Modern Home and Widget Builder thin shell JSPs MUST load the product TMX JavaScript catalog for the user’s session locale using the existing WebUI pattern (e.g. `/Rhythmyx/tmx/tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=…`). React/TypeScript UI copy MUST resolve message keys through the existing client API **`I18N.message(...)`** or a thin TypeScript wrapper that delegates to that global. Product MUST NOT introduce a separate React i18n framework or build-time-only catalog as the sole source of runtime strings for these surfaces.
- **FR-024**: i18n acceptance for this feature is proven primarily by **SC-008 key-presence review** (manual/PR checklist of primary chrome strings → TMX keys). Automated Vitest suites are **not** required to mock `I18N.message` or load real multi-locale TMX solely to satisfy i18n (functional UI tests under FR-011 remain required for behavior).
- **FR-005**: Product MUST provide a modern Widget Builder UI for list/create/edit/validate/package flows currently offered by the legacy Widget Builder screens, gated by the same enablement rules.
- **FR-006**: Widget Builder server-side definition, validation, and package generation capabilities MUST remain the system of record (no parallel server rewrite required for this feature); the modern UI MUST use those capabilities.
- **FR-015**: For concurrent edits of the same widget definition, product MUST apply **last-write-wins** semantics (successful save from each editor accepted in order received); modern UI MUST confirm successful save and show current server state after reload. Product is NOT required to implement client-side merge or mandatory version preconditions unless the existing server already enforces them.
- **FR-007**: Modern Home and Widget Builder MUST be reachable from main navigation after cutover. Navigation and product view/route configuration MUST be rewired to modern-only modules (documented successors of today’s `home` and `widgetbuilder` views if names change).
- **FR-008**: Product MUST NOT ship a dual-path (classic + modern) for Home or Widget Builder in any **shipped release**. The product release that enables modern Home (US1) and modern Widget Builder (US2) MUST also complete US3 legacy removal in that **same release**. Feature-branch PRs may stack US1→US2→US3, but US1/US2 MUST NOT merge alone to a shippable branch (`development` / release line) without US3 in the same train—classic entry files must not remain requestable in a customer build.
- **FR-017**: Classic Home and Widget Builder entry JSPs (e.g. `home.jsp`, `widgetBuilder.jsp`) and includes that exist only for those classic UIs MUST be **deleted** in the same product release (hard cut; US3 owns the delete work). Product MUST NOT leave those files as rewritten modern shells or as redirect-only JSP stubs. Known legacy URL mapping (FR-013) MUST be implemented without retaining classic entry JSPs.
- **FR-009**: Product MUST remove production dependencies on the legacy contributor client stack (RequireJS/Knockout/widgel package used only for CUI) for Home in the **same product release** that ships modern Home (US1+US2+US3 together). Physical deletion of exclusive CUI SPA trees, CUI-local vendors, and classic Home entry wiring is owned by **US3** (not required inside the US1 implementation PR). This includes exclusive CUI SPA trees, CUI-local vendors, and classic Home entry wiring that only served the embedded contributor SPA.
- **FR-010**: Product MUST remove production dependencies on legacy Widget Builder client packs (concatenated/minified builder bundles, product JS/CSS/templates, and dead includes) in the **same product release** that ships modern Widget Builder (US1+US2+US3 together). Physical deletion is owned by **US3** (not required inside the US2 implementation PR).
- **FR-016**: After exclusive Home/CUI and Widget Builder client removal, implementers MUST complete a **documented manual inventory and code review** of remaining production WebUI entry points for candidate orphaned vendors (at minimum Backbone, Underscore, and Backgrid when they were only pulled for Widget Builder). Libraries with **zero** remaining product consumers MUST be removed from production distribution in the same release. Libraries still referenced by non-retired screens MUST be retained (with remaining consumers noted in the inventory). Full jQuery retirement across all remaining admin screens is NOT required by this feature. An automated absence scan is **not** a required CI hard gate for this feature (optional tooling may assist the inventory).
- **FR-011**: Automated tests MUST cover critical Home contributor flows and Widget Builder create/edit/validate paths at least at the modern UI and/or service contract level with passing CI on the target JDK/branch.
- **FR-018**: Legacy-only automated tests that exercise the retired CUI or Widget Builder client stacks (e.g. Backbone/jQuery Widget Builder unit tests) MUST be **removed** in the same release as US3. They MUST be **replaced** (not indefinitely soft-skipped) by equivalent modern UI and/or service-contract tests so CI does not depend on deleted client scripts and FR-011 remains green.
- **FR-019**: US3 MUST maintain a durable **removal inventory** under `specs/989-react-cui-widget-builder/` (recommended path: `checklists/removal-inventory.md`) listing exclusive deletes (CUI SPA, WB client packs, classic entry JSPs), nav/config rewires, candidate orphan vendors with keep/drop decisions and remaining consumers, and test replacements. PR and release review MUST sign off that inventory (manual gate; not a CI absence-scan hard gate).
- **FR-012**: Accessibility for primary Home actions and Widget Builder forms MUST meet the same bar as other modern CM UI (keyboard operable, labeled controls).
- **FR-013**: Product MUST map known legacy Home initial-screen and Widget Builder entry URLs/bookmarks to modern equivalents. Unmapped or obsolete paths MUST present a clear “moved/unavailable” message (no silent blank page, no classic UI)—via a dedicated product surface (dispatcher message, thin shell, or modern component), not only console/log output.
- **FR-020**: Before shipping, implementers MUST complete a **main-nav smoke checklist** covering Dashboard and other non-Home/non-WB tabs used in production (SC-005), recorded with the release/PR evidence.
- **FR-014**: Dashboard modernization cleanup (if still dual-path) is **out of scope** except where shared shell/nav is required for Home mounting—Dashboard gadgets remain a separate effort unless already modern-only.

### Key Entities
- **Home / contributor workspace**: Landing experience for content work (lists, search, create wizards).
- **Content item (page, asset, blog post)**: Created or opened from Home flows.
- **Widget definition**: Metadata, fields, display rules, and resources authored in Widget Builder.
- **Widget package**: Generated deployable package from a valid definition.
- **Navigation entry**: Main nav targets for Home, Dashboard, Widget Builder.
- **Feature enablement**: Product flag controlling Widget Builder availability.

## Success Criteria
### Measurable Outcomes
- **SC-001**: Contributors complete the top Home tasks (open Home; use Recent; use Library to browse a site/folder and open an item; run Search; create at least one of page/asset/blog) without the legacy embedded contributor SPA, in a scripted UAT checklist with 100% of checklist items pass.
- **SC-008**: For a checklist of primary Home and Widget Builder chrome strings (section labels, primary actions, empty-state titles, unavailable message), each string is backed by a TMX catalog key (reuse or newly added), verified by **manual/PR review** (key-presence checklist)—not a required Vitest multi-locale suite. When a non-default product locale is available in the test environment, a spot-check SHOULD show catalog resolution rather than source-hardcoded English only (FR-021). Automated unit tests are **not** required to assert `I18N.message` mocks or load real TMX for this feature.
- **SC-002**: When Widget Builder is enabled, an admin can create, save, reload, and package a simple widget definition end-to-end on the modern UI in one continuous session (target: under 15 minutes for a trained tester with a scripted scenario).
- **SC-003**: After US3, production Home and Widget Builder entry points load zero retired CUI Knockout/RequireJS app scripts and zero legacy Widget Builder packed client bundles, verified by the feature **removal inventory** under `specs/989-react-cui-widget-builder/` (100% of inventory checklist items signed off in PR/release review). Candidate orphan vendors (e.g. Backbone/Underscore/Backgrid) are either removed when the inventory shows zero consumers or retained with documented remaining consumers. No mandatory CI absence-scan gate.
- **SC-004**: Automated test suite for the new Home and Widget Builder surfaces is green in CI on the development branch JDK; legacy-only CUI/WB client tests are gone and do not soft-skip forever.
- **SC-005**: No regression on main nav for users who only use Dashboard or other non-Home tabs—**main-nav smoke checklist** (minimum: Dashboard open; at least one other non-Home tab such as editor/Web Management or Design if available to the tester; Home and WB when enabled still reachable) 100% pass, recorded with PR/release evidence (FR-020).
- **SC-006**: Disabled Widget Builder remains inaccessible (no UI leak) consistent with pre-migration behavior.
- **SC-007**: A checklist of documented legacy Home/Widget Builder URLs (minimum: main `home`/`widgetbuilder` views and Home `initialScreen` values library, list, search, newitem) resolves to modern destinations; a sample unmapped legacy path shows a clear **on-page** moved/unavailable message (UAT 100% of checklist pass).

## Assumptions
- Strategic UI direction is the existing modern CM UI (Track B); this feature does not introduce a second SPA framework.
- Server widget-builder services (`PSWidgetBuilderService` and related) remain authoritative; UI migration reuses them.
- Home’s dual mode (library finder vs contributor frame) is replaced by one modern shell with Recent / Library / Search / Create sections (not two modes).
- Home Library targets **contributor browse/open** parity (site/folder + open item), not a full clone of every Web Management finder admin action.
- CUI-exclusive blog wizard and create flows must be preserved or explicitly replaced with equal capability (see [home-capability-matrix.md](./contracts/home-capability-matrix.md)).
- Classic Home was limited in surface area but **product-complete** for listed contributor tasks; the React redesign MUST NOT drop MUST-matrix capabilities in exchange for a thinner UI.
- Classic **My Bookmarks** content list is a **required** Home section (product decision: keep); implemented via `item/mycontent` alongside Recent / Library / Search / Create.
- Widget Builder remains optional via existing enablement; not all sites use it.
- **No dual-path beta**: classic Home/CUI and classic Widget Builder UIs are removed when modern equivalents ship (US1+US2+US3 same release after UAT). Pre-release QA may use feature branches/builds, not a supported classic toggle in production.
- Classic `home.jsp` / `widgetBuilder.jsp` are **not** reused as modern hosts; cutover deletes them and rewires nav/config to modern modules.
- Legacy-only CUI/WB client automated tests are deleted and replaced with modern coverage in the same release (no permanent soft-skip).
- Full content editor, Active Assembly, template designer, and JSF admin screens are **out of scope**.
- No schema or packaging format change is required solely for this UI migration.
- i18n for modern Home and Widget Builder **MUST** use product **TMX translation tables** (reuse + add keys); not English-only React hardcoding as the bar.
- Runtime delivery uses existing **`tmx.jsp` + `I18N.message`** (or thin TS wrapper); structural locale parity for new keys per FR-022.

## Out of Scope
- Migrating remaining ~20 jQuery WebUI admin screens (users, roles, publishing, templates, etc.).
- Dojo Active Assembly Track A work (separate).
- Desktop Content Explorer and Eclipse Workbench.
- Rewriting widget package generation algorithms on the server.
- DTS public site UIs.
- Full retirement of jQuery from the entire product (only Home/CUI and Widget Builder clients plus **proven orphan** vendors from the US3 **manual removal inventory** in this feature).
- Replacing the entire Web Management content finder on non-Home screens (Home Library is scoped to contributor browse/open).
