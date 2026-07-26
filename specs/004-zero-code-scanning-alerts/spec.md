# Feature Specification: Zero Open Code Scanning Alerts for 8.2 Release

**Feature Branch**: `[004-zero-code-scanning-alerts]`
**Created**: 2026-07-11
**Status**: Draft
**Input**: User description: "We have a large number of open code scanning security alerts on this repository. They generally fall into a few categories: 1. legacy code that is obsolete but is still present on the repository file system (obsolete code / 3rd party scripts should be removed to mitigate). 2. A valid finding that needs mitigation. 3. A false positive. I need a concrete plan that results in assignable tasks. Our goal is 0 active code scanning alerts for the 8.2 release if possible."

## Module Scope *(mandatory for this mono-repo)*

- **Primary module(s)**: Repository-wide — the alerts span every active module under `./`, including `system/`, `rest/`, `projects/sitemanage/`, `WebUI/`, `deliverytiersuite/delivery-tier-suite/*`, `modules/*`, `deployer/`, and `PCM-PkgMgtUI/`. The triage/coordination process is owned at the repo root, but each concrete mitigation task belongs to exactly one module owner.
- **Secondary / integration modules**: `modules/perc-packages`, `modules/perc-distribution-tree` (distribution-tree rebuild impact), `modules/perc-jetty*`, any module that ships third-party scripts or vendored JARs that contribute to alerts.
- **AGENTS files to apply**: `./AGENTS.md` (root — authoritative). Per-module `AGENTS.md` / `AGENTS.local.md` consulted during task assignment for each finding.
- **User roles affected**: integrator / release engineer (primary — owns triage and PR), module maintainers (assignees for individual fixes), security reviewer (validates closure), site visitor (downstream — reduced exposure to known CVEs).
- **Install / upgrade impact**: `package .ppkg` (distribution tree) — removing obsolete scripts/jars MUST be reflected in the installer/distribution so shipped artifacts match the source tree.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Triage Every Open Alert into a Categorized Backlog (Priority: P1)

A security/release engineer opens the code-scanning dashboard for the `8.2` release branch and exports (or queries) every currently open alert. For each alert they record: alert ID, scanner rule, severity, module/file path, and one of three dispositions — **Obsolete (remove)**, **Valid (mitigate)**, or **False Positive (suppress with justification)**. The result is a single, sortable triage sheet that can be split into per-module assignable tickets.

**Why this priority**: Without a complete, categorized inventory nothing else can happen — removal tasks need a file list, mitigation tasks need a defect per finding, and false positives need a documented justification to be closed. This is the gating step for the entire effort.

**Independent Test**: After the triage pass, the count of "untriaged" alerts in the dashboard equals zero, and each remaining open alert has a written disposition with an owner module and a target action.

**Acceptance Scenarios**:

1. **Given** a snapshot of all open code-scanning alerts on the `8.2` branch, **When** the release engineer completes the triage pass, **Then** every alert has exactly one of three dispositions recorded (Obsolete / Valid / False Positive) with a module owner and a target action.
2. **Given** the completed triage sheet, **When** the release engineer filters by disposition, **Then** each disposition bucket totals the same as the count of alerts assigned to that disposition (no orphans, no duplicates).
3. **Given** a high-severity alert, **When** it is triaged, **Then** its target action is recorded with an explicit target milestone (e.g., `8.2-blocker`, `8.2-must-fix`, `8.2-backlog`, or `accepted-risk`) so release readiness can be measured.

---

### User Story 2 - Remove Obsolete Code and Third-Party Scripts (Priority: P1)

A module maintainer receives a "remove obsolete" ticket listing specific files, directories, or vendored third-party scripts that the scanner flagged only because they are unreachable dead code. They confirm (a) the files are not referenced by the build, (b) they are not loaded at runtime by any live code path, and (c) they are not shipped in the distribution. They then delete the files and update the distribution tree / packaging descriptors so the artifacts stop being shipped. The alert is closed when the scanner no longer detects the pattern on a fresh scan of the branch.

**Why this priority**: Removal is the cheapest, safest mitigation for the largest category of alerts in a legacy mono-repo. It eliminates the finding without introducing regressions and shrinks the attack surface at the same time.

**Independent Test**: For each removal ticket, the scanner re-scan on the `8.2` branch reports zero open alerts whose `file path` matches any of the removed files, and the build + module smoke tests still pass for that module.

**Acceptance Scenarios**:

1. **Given** a file or directory listed as obsolete, **When** the maintainer deletes it and updates the distribution/packaging descriptors, **Then** the corresponding scanner alert is no longer reported on the next scan of the `8.2` branch.
2. **Given** a removed obsolete file that was previously bundled into a `.ppkg` or distribution JAR, **When** the installer is rebuilt, **Then** the resulting package no longer contains the removed file (verified by archive listing).
3. **Given** a removed obsolete file, **When** the module's unit test suite is executed, **Then** all existing tests still pass (no compile, link, or runtime references to the removed file).

---

### User Story 3 - Mitigate Valid Findings with a Concrete Fix (Priority: P2)

A module maintainer receives a "mitigate" ticket for a finding the scanner identified as a real vulnerability in code that is actually executed or shipped (e.g., a known CVE in a transitive dependency, an unsafe deserialization call, a SQL-injection sink, a path traversal, a hard-coded secret). They implement the smallest fix that closes the finding — upgrade the dependency to a patched version, replace the unsafe API call with a safe alternative, validate/sanitize the input, or move the secret to a config-driven source. They add or update unit/integration tests that exercise the fixed code path, and update any CHANGELOG / release notes entries the project requires.

**Why this priority**: Valid findings are real risk to deployed customers. Mitigation is more expensive than removal and requires per-finding engineering judgment, so it follows the easier P1 removals but is mandatory for release.

**Independent Test**: After the fix lands, a re-scan reports the alert as resolved, and the new/updated tests fail on the pre-fix code and pass on the post-fix code (proving the test actually exercises the vulnerability).

**Acceptance Scenarios**:

1. **Given** a valid finding in `module X`, **When** the maintainer lands the fix, **Then** the scanner re-scan on the `8.2` branch reports the alert as resolved (closed or removed from the open list).
2. **Given** the fix, **When** the maintainer's regression test is run against the pre-fix code, **Then** the test fails (demonstrates it actually exercises the vulnerability).
3. **Given** the fix, **When** the module's full test suite is run, **Then** all tests pass and no previously-passing test has regressed.
4. **Given** a CVE-class finding, **When** the fix is implemented by upgrading a dependency, **Then** the upgraded version is the lowest version that addresses the CVE AND is compatible with the module's current API surface.

---

### User Story 4 - Document and Suppress False Positives (Priority: P3)

A module maintainer or security reviewer receives a "false positive" ticket for a finding the scanner reported but that, on inspection, does not represent an exploitable risk in this codebase (e.g., a pattern detected in test code, a path that is only reachable with admin/elevated credentials and is gated by an upstream control, a transitive dep that is not actually loaded at runtime). They write a short justification citing the specific reason the finding does not apply, attach any supporting evidence (code references, configuration snapshots), and apply the documented suppression per the scanner's approved mechanisms (e.g., `# codeql[rule-id]` comment, SARIF `notApplicable`, or scanner-native suppression file). The alert is closed with the suppression in place.

**Why this priority**: False-positive suppression prevents noise from blocking the release, but it must be the last resort — every suppression carries a long-term maintenance cost (the suppression must be revisited when the scanner or code changes). It is intentionally lower priority than real fixes.

**Independent Test**: After the suppression is applied, a re-scan does not re-open the alert, and the suppression entry references a concrete justification string that another reviewer can locate and verify.

**Acceptance Scenarios**:

1. **Given** a finding classified as a false positive, **When** the maintainer applies the suppression, **Then** the next scan of the `8.2` branch reports the alert as closed/suppressed and does not re-open it on subsequent scans.
2. **Given** the suppression entry, **When** a security reviewer reads it, **Then** they can locate a concrete justification (file + lines or config key + value) that explains why the pattern is not exploitable in this codebase.
3. **Given** a false positive in test code or build-time-only code, **When** the suppression is applied, **Then** the production runtime code paths are unaffected (verified by the suppression being scoped to the test/build artifact only).

---

### Edge Cases

- An alert references a file that is shared across multiple modules (e.g., a script in `modules/perc-jetty` loaded by both `system` and `rest`). The triage sheet records the primary owning module and lists secondary modules that may also need packaging updates — ownership does not duplicate.
- A removal or mitigation requires a dependency upgrade that itself pulls in a new transitive alert. The new alert must be triaged in the same pass; it is not allowed to silently re-open previously-closed work.
- A false-positive suppression expires or breaks when the scanner rule is updated. The release engineer flags any suppression older than one release for re-review before signing off the `8.2` release.
- An alert references a file path that no longer exists in git (stale scanner cache). Triage confirms the file is truly gone (or re-adds it to the active list if it was reverted) before closing.
- A valid finding has no available fix on the supported JDK / dependency version (e.g., requires a major upgrade). The mitigation ticket is escalated to "accepted-risk" with an explicit owner and target milestone — it does not silently block the release, nor is it closed without a documented decision.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The release/security engineer MUST produce a complete triage inventory of every open code-scanning alert on the `8.2` release branch, with each alert assigned one of three dispositions (Obsolete / Valid / False Positive), a module owner, a target action, and a target milestone.
- **FR-002**: For every alert classified as Obsolete, the owning module maintainer MUST delete the flagged files (or extract them from packaging), update the distribution/packaging descriptors so the files are no longer shipped, and confirm the module's test suite still passes.
- **FR-003**: For every alert classified as Valid, the owning module maintainer MUST implement a fix that closes the finding, add or update a regression test that fails on the pre-fix code, and confirm the module's full test suite passes.
- **FR-004**: For every alert classified as False Positive, the responsible reviewer MUST apply an approved suppression scoped to the specific finding, with a written justification that another reviewer can locate and verify.
- **FR-005**: The triage inventory MUST be reviewable by a second person (security reviewer or release lead) before any ticket is closed, so that no finding is dismissed without an independent check.
- **FR-006**: The triage inventory MUST be re-runnable on demand (script or documented procedure) against the current `8.2` branch state so that progress can be reported at any point in the release cycle.
- **FR-007**: Any suppression older than one release (i.e., applied before the previous release cut) MUST be flagged for re-review before the `8.2` release is signed off.
- **FR-008**: A finding that cannot be mitigated or safely suppressed MUST be escalated to "accepted-risk" status with a named owner, a written rationale, and a target milestone for future remediation — it MUST NOT be silently closed.
- **FR-009**: The release engineer MUST publish a release-readiness report stating, for the `8.2` branch: total open alerts, count per disposition, count per severity, count of accepted-risks, and a pass/fail decision against the "0 active alerts" goal.
- **FR-010**: Each per-module mitigation PR MUST follow the project's PR workflow (including review-comment resolution per `./AGENTS.md`) so that no closed alert leaves an unresolved review thread.

### Key Entities *(include if feature involves data)*

- **Alert**: A single code-scanning finding. Attributes: scanner rule ID, severity, file path, line range (where applicable), module owner, disposition (`obsolete` | `valid` | `false-positive` | `accepted-risk`), target milestone, linked PR/ticket, suppression justification (if suppressed).
- **Triage Inventory**: The complete set of open alerts for the `8.2` branch, each carrying an `Alert` record. Owned by the release/security engineer for the release.
- **Module Owner**: The module (under `./AGENTS.md` module-first boundaries) responsible for fixing a given alert. One alert has exactly one module owner; cross-cutting files are assigned to the primary owner with secondary modules listed as packaging-impact only.
- **Suppression Record**: A documented justification + scanner-native suppression entry for a false positive, scoped to a specific rule + path, with an applied-on date so it can be re-reviewed when stale.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The `8.2` release branch reports **0 active code-scanning alerts** on the official scanner dashboard at release sign-off (with any remaining items documented as `accepted-risk` and explicitly excluded by name in the release notes).
- **SC-002**: 100% of open alerts on the `8.2` branch have a recorded disposition (Obsolete / Valid / False Positive / Accepted-Risk) and a named module owner before any mitigation work begins.
- **SC-003**: For every Valid finding, a regression test exists that demonstrably fails on the pre-fix code and passes on the post-fix code (verified by a documented run of the test against the pre-fix revision).
- **SC-004**: For every Obsolete finding, the removed file is no longer present in the rebuilt distribution artifact (verified by archive listing of the rebuilt `.ppkg` / JAR / WAR).
- **SC-005**: For every False Positive, a written justification exists that an independent reviewer can locate and confirm without re-reading the full code history.
- **SC-006**: A release-readiness report is published that lists total open alerts, counts by disposition and severity, the list of accepted-risks (if any), and an explicit pass/fail decision against the `0 active alerts` goal.
- **SC-007**: No closed alert leaves an unresolved PR review thread on its closing PR.

## Assumptions

- The "code scanning alerts" referenced are produced by the GitHub-native code-scanning product (CodeQL / Dependabot) which is already enabled on the repository; if a different scanner is in use, the procedure in this spec still applies but the suppression mechanism references must be updated to match that scanner.
- "Module owner" can be reliably inferred from the file path under `./AGENTS.md`'s module list; cross-module files are assigned to the primary owner listed there and listed secondarily for packaging impact.
- The `8.2` release branch already exists (or will be cut from `development` per the project's branch policy in `./AGENTS.md`); this spec does not propose a branching strategy of its own.
- Existing module test infrastructure (JUnit 5 + Mockito per `./AGENTS.md`) is sufficient to write regression tests for the Valid-finding fixes without introducing new test frameworks.
- The project's PR review-comment resolution procedure (per `./AGENTS.md`) is mandatory for every closing PR — this spec inherits that requirement rather than overriding it.
- Removal of an obsolete file is considered safe if (a) no source file references it, (b) no build descriptor includes it, and (c) no runtime code path loads it. Anything more ambiguous is escalated to Valid or to the module maintainer's judgment call rather than auto-removed.
- The project tolerates a small number of "accepted-risk" findings for the `8.2` release if (and only if) they are explicitly documented with a target milestone and an owner — but the explicit goal is zero active alerts, so accepted-risk is the exception path, not the default.
- The release engineer has authority to assign tickets across modules for the duration of this effort; routine module ownership boundaries still apply but cross-module coordination for the triage pass is centralized.

