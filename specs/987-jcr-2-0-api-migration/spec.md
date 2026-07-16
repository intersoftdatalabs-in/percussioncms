# Feature Specification: Content Repository API Standard Upgrade (1.0 → 2.0)

**Feature Branch**: `1286-jcr-2-0-api-migration`  
**Created**: 2026-07-16  
**Status**: Draft  
**Input**: "We need to refactor the code from JCR 1.0 API to JCR 2.0 API after this mornings security update for JCR."  
**Related**: Backlog issue #506; dependency version bump merged via #531 (2026-07-16)

## Module Scope
- **Primary module(s)**: `system/` (content manager, assembly, publisher, and related services — majority of content-repository call sites); `modules/utils/` (shared repository helpers)
- **Secondary / integration modules**: `modules/perc-toolkit/`, `projects/sitemanage/`, `modules/p13n-api/`, `modules/segmentation-rx/`, `deployer/`, selected extensions (`extensions-main`, `extensions-nav`, `extensions-sfp`, `extensions-workflow`, `extensions-serverutils`, `extensions-landingpage`), `modules/ContentUI/`, limited delivery-tier touchpoints that depend on repository abstractions
- **AGENTS files to apply**: root `./AGENTS.md`; module AGENTS when present under primary/secondary modules
- **User roles affected**: content editor, publisher, admin, integrator (custom extensions/templates), ops (build, deploy, security posture) — **no intentional change** to day-to-day product UX
- **Install / upgrade impact**: dependency and application compatibility only; **no** intended database schema, package (`.ppkg`), or installer distribution-tree changes. Existing content and standard upgrade paths remain valid.

## Clarifications

### Session 2026-07-16

- Q: Migration depth — compile-only vs deprecation cleanup vs full modernization? → A: Compile-clean + deprecation cleanup (fix build breaks and systematically replace product call sites of deprecated 1.0 methods with 2.0 equivalents where a clear replacement exists; no optional new 2.0 capabilities)
- Q: Custom / third-party extension compatibility? → A: Source rebuild only — custom extensions must recompile against the upgraded product; no binary-compat guarantee for third-party JARs built only against 1.0 repository types
- Q: Regression verification bar for merge? → A: Automated + scripted smoke — designated automated suites green; short documented smoke (create/save, open, preview, one publish) recorded before feature-complete merge; intermediate story PRs may use automated tests only
- Q: Call sites with no clear 2.0 replacement? → A: Documented exceptions allowed for non-critical only — prefer real 2.0 replacement or shared helper; non-critical leftovers may ship with tracked exception (owner, rationale, follow-up); critical editor/publish paths must not use exceptions
- Q: Dependency vs application-code landing? → A: Dependency already updated on development (leads); first deliverable is compile-clean build + PR for that work only; then continue deprecation cleanup and remaining implementation in follow-on PRs

## User Scenarios & Testing
Each story must be independently testable.

### User Story 1 - Editors and publishers keep working after the repository standard upgrade (Priority: P1)

After the content-repository dependency was raised to the supported 2.0 standard for security and maintenance, editors and publishers must continue to create, edit, preview, assemble, and publish content with the same outcomes as before the upgrade. This story is the primary business value: security modernization without operational disruption.

**Why this priority**: Raising the library version without application compatibility leaves the product unbuildable or regressing; users must not see broken content operations.

**Independent Test**: On a product build that uses the updated repository standard, run the **documented scripted smoke** (create/save, open existing item, preview, one publish path) plus designated automated suites; confirm stored content and published output match pre-upgrade expectations (or documented baselines).

**Acceptance Scenarios**:
1. **Given** a CMS instance built with the upgraded repository standard, **When** an editor creates and saves a page or asset using existing content types, **Then** the item persists and reloads with the same fields and relationships as before the upgrade.
2. **Given** content that previously published successfully, **When** a publisher runs preview and full or incremental publish for a representative site, **Then** assembly and delivery complete without new repository-related errors and published results are equivalent to the pre-upgrade baseline.
3. **Given** folder navigation and site hierarchy operations that depend on repository-backed lookups, **When** users browse, search, or list content via existing UIs, **Then** listings and selections return expected items (no silent empty results or lookup failures introduced by the upgrade).
4. **Given** feature-complete work ready to merge to the development line, **When** the scripted smoke checklist is executed and results recorded, **Then** create/save, open, preview, and one publish path all pass with no repository-related defects (broader multi-site UAT is not required for this feature gate).

---

### User Story 2 - The product can be built, tested, and released on the supported standard (Priority: P1)

The repository API dependency is **already** updated to 2.0 on the development line. Engineering must first restore a **compiling** product and open a **PR limited to that compile-clean work**. Deprecation cleanup, broader test hardening, documentation, and scripted smoke follow in subsequent PRs. Obsolete 1.0-only contracts must not block compilation. Automated checks that exercise repository-backed behavior must pass for feature-complete (or be intentionally updated where behavior is specified to change).

**Why this priority**: The dependency already leads; without a green compile, the line cannot progress. Splitting “compile PR first” reduces risk and unblocks parallel follow-on work.

**Independent Test**: Full product build succeeds on the development-line platform with the 2.0 dependency; a compile-focused PR is submitted and reviewable on its own. Later PRs add deprecation cleanup and verification gates.

**Acceptance Scenarios**:
1. **Given** product dependency management already declares the upgraded repository standard only, **When** the product is built with the project’s standard build entry point on the development-line platform after the first-phase changes, **Then** all modules that previously failed to compile against 2.0 build successfully.
2. **Given** the first-phase compile-clean work is complete, **When** a pull request is opened for **that work only** (no requirement to finish deprecation cleanup in the same PR), **Then** the PR is suitable for review and merge on compile success and appropriate automated checks for that scope.
3. **Given** automated tests that cover content loading, repository-backed queries, assembly finders, and shared repository helpers, **When** those tests run at feature-complete, **Then** they pass, or any intentional expectation changes are documented and justified in the change set.
4. **Given** a dependency inventory of the built product, **When** checked for the legacy 1.0 repository API component, **Then** that legacy component is absent from compile and runtime classpaths used by shipping modules.

---

### User Story 3 - Operators retain security and support posture (Priority: P2)

Operations and security stakeholders need confidence that the product no longer relies on an unsupported repository API surface, that the upgrade does not introduce new high-severity vulnerabilities in the repository stack, and that upgrade notes remain accurate.

**Why this priority**: The security-oriented dependency update is incomplete until application behavior and documentation match the supported stack.

**Independent Test**: Dependency and vulnerability review of the repository-related tree shows the supported standard in use; release notes mention the change; no new high-severity repository-stack findings without documented exception.

**Acceptance Scenarios**:
1. **Given** the upgraded product build, **When** operators review dependency and security scan results for the content-repository stack, **Then** the product reports the supported 2.0 standard and no unexplained new high-severity findings from that upgrade alone.
2. **Given** existing installation and upgrade documentation, **When** release notes for this work are published, **Then** they state that the content repository API standard was upgraded and that no content data migration is required for standard deployments (unless a later finding requires otherwise, which must be called out explicitly).

---

### User Story 4 - Integrators and extension authors see stable contracts (Priority: P2)

Partners and internal authors of extensions, finders, and query-driven features that use the product’s content-repository abstractions must continue to function **after recompiling** against the upgraded product. Binary compatibility of third-party JARs compiled only against the 1.0 repository types is **not** required. Where public extension-facing contracts must change, changes are minimized, documented, and discoverable so integrators can rebuild confidently.

**Why this priority**: The product ships many built-in and custom extensions that touch repository types; silent breaks would surface only in customer environments. Rebuild is an accepted upgrade step for this change.

**Independent Test**: Built-in extensions and toolkit helpers that perform repository lookups or content access compile and pass their tests; any public signature changes are listed in documentation aimed at integrators rebuilding extensions.

**Acceptance Scenarios**:
1. **Given** built-in extensions that read or query content during assembly or workflow, **When** those extensions run under the upgraded standard, **Then** they produce the same functional results as before for representative configurations.
2. **Given** a documented list of any public or extension contract changes required by the upgrade, **When** an integrator rebuilds a custom extension against the upgraded product using that guidance, **Then** every breaking change (if any) includes a replacement approach; the preferred outcome is zero breaking changes outside the repository standard types themselves.
3. **Given** a third-party extension JAR compiled only against the 1.0 repository types and not rebuilt, **When** it is deployed on the upgraded product, **Then** load or runtime failure is an accepted outcome (not a product defect); release notes MUST state that custom extensions require rebuild.

---

### Edge Cases
- Content with large binary or multi-value fields, or deep folder hierarchies, continues to load and save without truncation or path errors.
- Existing content queries used by generators, finders, and lists continue to execute or fail with the same business-level outcomes as before; no new silent empty result sets.
- Concurrent edit workflows and publish jobs that touch many items do not introduce new session or save-order failures.
- Personalization, segmentation, and list-builder features that issue repository-backed queries still return expected segments and lists.
- Package install and design-object import paths that touch repository types complete without new repository API errors.
- Rolling upgrades: documentation states that CMS nodes in a cluster should run the same product build (no mixed application generations in one cluster for this change).
- If a specific call site has no compatible equivalent under the new standard: prefer a real 2.0 replacement or a product-owned shared helper; if neither is viable, a **documented tracked exception** (owner, rationale, follow-up) is allowed only for **non-critical** paths. Critical editor/publish paths MUST NOT rely on exceptions. Undocumented workarounds are forbidden.

## Requirements
### Functional Requirements
- **FR-001**: The product MUST build and run using only the supported content-repository API standard version 2.0 (no remaining compile or runtime dependency on the 1.0-only API component for shipping modules). The 2.0 dependency pin is **already present** on the development line; this feature completes application compatibility against that pin.
- **FR-002**: Application and extension code that previously relied on 1.0-only repository contracts MUST be updated so that equivalent content operations (read, write, query, and session lifecycle as used today) succeed under the 2.0 standard. This includes (a) resolving all compile/link failures against the 2.0 API (including new methods required on product types that implement repository interfaces) and (b) systematically replacing product call sites that use deprecated 1.0 methods with their documented 2.0 equivalents where a clear replacement exists. Adoption of optional new 2.0-only capabilities is out of scope unless required to replace a removed or non-viable 1.0 contract.
- **FR-014**: Delivery MUST be phased: **Phase 1** restores a compiling build against the existing 2.0 dependency and is submitted as its **own pull request** (compile-clean scope only). **Phase 2+** continues deprecation cleanup, tests, documentation, exception tracking, and scripted smoke through follow-on PRs until feature-complete (FR-010, FR-012, FR-013).
- **FR-003**: Existing persisted content, identifiers, folder paths, and relationships MUST remain readable and writable after the upgrade without a content data migration step for standard deployments.
- **FR-004**: Core content operations MUST preserve pre-upgrade behavior for create, update, delete, query, assembly finding, and publish selection unless a change is explicitly documented as intentional.
- **FR-005**: Automated tests covering repository-backed behavior MUST pass on the development-line platform target after the refactor; new or updated tests MUST cover critical flows touched by the migration (session use, create/read/update/delete-style access, queries used by content lists and finders, and any versioning or binary access paths still in product use).
- **FR-006**: Dependency management MUST keep the repository API version centralized so modules do not reintroduce the 1.0 component.
- **FR-007**: Security and maintenance posture MUST be verifiable: a post-change dependency or vulnerability review of the repository-related tree MUST show the upgraded standard in use and MUST not introduce unexplained new high-severity findings attributable to this work.
- **FR-008**: Documentation MUST record the upgrade (changelog or release notes and any developer-facing notes on extension impact), including that standard installs require no content data migration.
- **FR-009**: Public product interfaces (HTTP APIs, legacy remote APIs, internal UI services, and package formats) MUST remain backward compatible; repository-standard types may appear only where they already appeared in public signatures, and any such change MUST be minimized and documented.
- **FR-011**: Custom / third-party Java extensions MUST be expected to **recompile** against the upgraded product. The product MUST NOT treat binary incompatibility of extension JARs built only against 1.0 repository types as a defect. Release notes MUST state the rebuild requirement; documentation MUST list any repository-facing signature changes needed for a successful rebuild.
- **FR-010**: The migration MUST be complete enough that continuous integration on the development line succeeds with the upgraded dependency (no permanent waiver of repository-related build or test failures).
- **FR-012**: Feature-complete merge to the development line MUST require (a) designated automated repository-backed test suites green and (b) a short **documented scripted smoke** covering create/save, open existing item, preview, and one publish path, with results recorded (e.g., PR comment, checklist, or test log). Intermediate story PRs MAY merge on automated tests alone. A full multi-site or multi-content-type UAT cycle is NOT required as a merge gate for this feature.
- **FR-013**: When a call site lacks a clear 2.0 replacement, implementers MUST prefer (1) a documented 2.0 equivalent or (2) a product-owned shared helper. If neither is viable by feature-complete, a **tracked exception** (owner, rationale, follow-up work item) MAY remain only on **non-critical** paths. Critical editor, assembly, and publish paths MUST have a real replacement or helper—no exceptions. Undocumented workarounds are forbidden.

### Key Entities
- **Content repository session**: The authenticated unit of work through which the CMS reads and writes repository-backed content; lifecycle and save semantics must remain correct after the upgrade.
- **Content node / item projection**: Repository-backed representation of CMS content used by assembly, finders, and services; identity and property access must remain stable for callers.
- **Repository query**: Statement and result used by content lists, finders, segmentation, and related features; result behavior must remain functionally equivalent for existing product queries.
- **Shared repository helpers**: Utilities in shared modules that wrap repository access for the rest of the product; preferred place for any compatibility adaptations so call sites stay consistent.

## Success Criteria
### Measurable Outcomes
- **SC-001**: Every shipping module that used the content-repository API builds successfully against the 2.0 standard, and the legacy 1.0 API component is removed from product dependency management.
- **SC-007**: Product call sites of deprecated 1.0 repository methods that have a documented 2.0 replacement are migrated (inventory-driven). Remaining exceptions are listed with owner, rationale, and follow-up; **zero** exceptions remain on critical editor/publish paths; unjustified exceptions in shipping modules are not allowed.
- **SC-002**: All automated tests in the suites designated for repository-backed modules pass on the development-line platform after the migration (preferred: zero open, reviewed exceptions).
- **SC-003**: Before feature-complete merge, a documented scripted smoke (create/save page, open existing page, preview, one publish path) records **zero** new defects attributable to repository API incompatibility compared to the pre-migration baseline. Intermediate story PRs are not required to re-run this smoke.
- **SC-004**: Dependency and security review finds **no** remaining 1.0-only repository API component on shipping classpaths and **no** new high-severity vulnerabilities introduced solely by this upgrade (or each is documented with severity, owner, and mitigation).
- **SC-005**: Release documentation for the change is available at merge time, and support or ops can confirm from that documentation that (a) no standard content data migration is required and (b) custom Java extensions require rebuild against the upgraded product.
- **SC-006**: Continuous integration on the feature work returns to a normal green state (build and required tests not blocked by repository API failures).
- **SC-008**: Phase 1 is evidenced by a submitted (and ideally merged) pull request whose primary scope is restoring compile success against the already-updated 2.0 dependency, before or independent of full deprecation cleanup.

## Assumptions
- The 2026-07-16 security-oriented dependency update (#531: content-repository API library 1.0 → 2.0 on `development`) is **already landed**; this feature completes **application compatibility** against that pin (dependency leads; code catches up).
- Scope is **compile-clean + deprecation cleanup**: fix all 2.0 build breaks and replace deprecated 1.0 call sites with clear 2.0 equivalents. Not a full modernization into optional new 2.0-only product capabilities unless required to replace removed or non-viable 1.0 contracts.
- **Delivery order**: (1) get the build compiling and open a PR for compile-clean work only; (2) continue implementation (deprecation cleanup, tests, docs, smoke) in subsequent PRs until feature-complete.
- Target line is **development** (current platform per project rules); the 8.1.x line is out of scope unless explicitly added later.
- Content storage remains the existing CMS content store; this is not a rewrite onto a different repository product.
- Related helper libraries already on a modern 2.x line are expected to remain compatible unless a conflict is found during planning.
- “No user-visible change” is the default success definition for editors/publishers; any unavoidable behavior change requires product-owner acknowledgment and documentation.
- Integrator impact is limited to **source rebuild** of custom Java extensions against the upgraded product; binary compatibility of pre-built 1.0-era extension JARs is out of scope.
- Related tracking issue #506 remains the product backlog reference for this upgrade theme.
- Incremental delivery by module cluster is acceptable during implementation as long as the development line does not stay broken; FR-001 and FR-010 define the done state for the overall feature.
- Merge verification: intermediate story PRs may rely on automated tests; feature-complete merge additionally requires the documented scripted smoke (FR-012 / SC-003). Full multi-site UAT is out of scope as a merge gate.
- Hard-case call sites: non-critical tracked exceptions allowed; critical editor/publish paths require real replacements or shared helpers (FR-013).
- Approximately 200+ source files across the mono-repo import repository types today; planning should inventory call sites rather than assume a single-module fix.
