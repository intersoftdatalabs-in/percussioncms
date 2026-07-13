# Feature Specification: Javadoc Cleanup for Content Explorer Module

**Feature Branch**: `[003-javadoc-cleanup]`
**Created**: 2026-07-11
**Status**: Draft
**Input**: User description: "We need to clear the javadoc errors and warnings in the content explorer module they are slowing down the build."

## Module Scope *(mandatory for this mono-repo)*

<!--
  Percussion CMS Constitution I — Module-First Boundaries.
  Identify owning module(s) before detailing stories.
-->

- **Primary module(s)**: `modules/DesktopContentExplorer/` (`com.percussion:perc-content-explorer`)
- **Secondary / integration modules**: None — work is scoped to the content explorer module's own Java sources. No shared module is modified.
- **AGENTS files to apply**:
  - Root `AGENTS.md` (rules apply: no invented APIs, JDK 21 on `development`, code/test/docs discipline).
  - Module `AGENTS.md` / `AGENTS.local.md` under `modules/DesktopContentExplorer/` — apply any local rules discovered via the Rule Discovery Protocol before plan/task work begins.
- **User roles affected**: Integrators and contributors who build the CMS from source; CI systems that fail the build on javadoc plugin errors. End-user behavior of the running CMS is not changed.
- **Install / upgrade impact**: none — no schema, package (`.ppkg`), config, or distribution tree change. This is a source-level documentation/quality change.

## User Scenarios & Testing *(mandatory)*

<!--
  User stories prioritized as journeys. Each story independently testable.
  -->

### User Story 1 - CI Build Succeeds for Content Explorer Module (Priority: P1)

A CI pipeline runs the full Maven build for the mono-repo. Before this change, the
`mvn-env.sh` invocation of the javadoc goal on the `perc-content-explorer` module either
fails outright (errors) or emits many warnings that pollute logs and, under stricter
configurations, fail the build. After this change, the javadoc generation for the content
explorer module completes with zero errors and the warning count is at or near zero, so
the build pipeline no longer reports a javadoc failure attributable to this module.

**Why this priority**: This is the explicit user-visible problem in the request — the build is "slowing down" because of javadoc issues. Without a green javadoc step the build is unreliable, which blocks every other contributor's work.

**Independent Test**: Run `./mvn-env.sh -pl modules/DesktopContentExplorer -am javadoc:javadoc` (and the module's normal build goal) on JDK 21 and observe exit code 0, zero javadoc errors, and the documented reduced warning count.

**Acceptance Scenarios**:

1. **Given** a clean checkout on the `development` branch with JDK 21, **When** the contributor runs the module's javadoc generation, **Then** the command exits 0 with no javadoc errors attributed to `modules/DesktopContentExplorer`.
2. **Given** the same clean checkout, **When** the contributor runs the module's full build (including `verify` or the parent-aggregated javadoc step), **Then** the build no longer reports the content explorer module as the source of javadoc failures or excessive warnings.

---

### User Story 2 - Public/Internal API Surfaces Have Useful Javadoc (Priority: P2)

A developer working on a sister module (for example `perc-system` or `WebUI`) that
consumes a class from `perc-content-explorer` can hover / look up the symbol and find a
meaningful description in the generated javadoc instead of an "Missing Javadoc" warning
or a stale/copy-pasted block. After the cleanup, classes and members that the javadoc
tool flags as missing documentation have been audited, and either received a real
description or have been marked with the project's documented "no-op" pattern (for
example `@SuppressWarnings("javadoc")` only where the project already uses such a
pattern).

**Why this priority**: The value beyond "build is green" — IDE help and generated docs become trustworthy. This is the reason for fixing the warnings rather than just suppressing them globally.

**Independent Test**: Open the generated javadoc HTML for the module and inspect (a) classes that previously had a "no comment" warning now have descriptive Javadoc, and (b) no warning remains for "missing comment" on the same classes.

**Acceptance Scenarios**:

1. **Given** a class or method that was previously generating a "missing comment" javadoc warning, **When** the javadoc is regenerated, **Then** either the symbol carries a meaningful Javadoc comment or it carries a justification comment explaining why it does not.
2. **Given** a class that previously generated a malformed-HTML javadoc warning, **When** the Javadoc is regenerated, **Then** the warning no longer appears.

---

### User Story 3 - Localized Builds & IDE Inspections Are Quiet (Priority: P3)

A developer running the build on their workstation — or opening the content explorer
module in an IDE that invokes javadoc tooling on save/inspect — sees a clean log with no
content-explorer-attributed javadoc errors. The reduced noise makes it easier to spot
real warnings from other modules.

**Why this priority**: Quality-of-life; not strictly required for CI to pass, but it follows directly from doing P1+P2 properly and improves day-to-day developer experience.

**Independent Test**: On a clean checkout, run a focused javadoc pass on `modules/DesktopContentExplorer` and compare the warning line count against the baseline captured before the cleanup.

**Acceptance Scenarios**:

1. **Given** a developer workstation with the repo checked out, **When** they run javadoc on `perc-content-explorer`, **Then** the warning output for this module is reduced to the documented target level (see Success Criteria).

---

### Edge Cases

- **What happens when a class is auto-generated or vendored?** Such classes should be excluded from javadoc checks via the existing module configuration (Maven javadoc plugin `sourcepath` / `excludePackageNames` or `@SuppressWarnings("all")` only where the project already follows that convention) — never with one-off hacks that diverge from neighboring modules.
- **How does the cleanup behave when the javadoc tool version differs?** The plan must rely only on flags and behavior documented for the javadoc tool bundled with the JDK 21 baseline on `development`; no undocumented options.
- **What if a class intentionally has no public surface (package-private / internal)?** Internal classes should not be documented to public-javadoc standards. The cleanup must not invent public Javadoc on symbols whose visibility is internal, and must not widen visibility to "fix" the warning.
- **What if a warning is reproducible only on a non-default locale or doclint setting?** Stick to the project's established javadoc plugin configuration in the parent POM; do not introduce per-module override files that diverge from sibling modules.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Maven javadoc goal for `modules/DesktopContentExplorer` (artifact `perc-content-explorer`) MUST exit 0 with zero javadoc errors on JDK 21 using `./mvn-env.sh`.
- **FR-002**: The total number of javadoc warnings emitted for `modules/DesktopContentExplorer` MUST be reduced from the pre-cleanup baseline by at least 80% (Success Criteria SC-001). The remaining warnings, if any, MUST be documented in the plan with a justified reason per warning.
- **FR-003**: The cleanup MUST fix root causes (missing/incorrect Javadoc comments, malformed HTML tags, broken `{@link}` references, wrong parameter/return descriptions) before resorting to suppression annotations. `@SuppressWarnings("javadoc")` is allowed only where the module already uses such suppressions or where an established project pattern justifies it; each instance MUST carry a brief justification comment.
- **FR-004**: The cleanup MUST NOT change any public type signature, method signature, runtime behavior, or visibility of any class or member in `modules/DesktopContentExplorer`. Pure documentation changes only.
- **FR-005**: The cleanup MUST NOT add new Maven dependencies, new modules, new frameworks, or new build plugins. It MUST use the javadoc plugin configuration already established by the parent POM.
- **FR-006**: A baseline report of the pre-cleanup javadoc errors and warnings for `modules/DesktopContentExplorer` MUST be captured into the spec's `checklists/` directory or a referenced artifact before changes are made, so the improvement is measurable.
- **FR-007**: After the changes, a follow-up report MUST be captured the same way showing the post-cleanup count and the delta.
- **FR-008**: The README or developer notes for the content explorer module MUST be updated only if the module already documents javadoc conventions; otherwise no new documentation file is created (YAGNI per Constitution VIII).
- **FR-009**: No automated tests are added or modified by this feature (the feature is documentation-only). Any test task that appears during planning MUST be justified in the Complexity Tracking table; absent justification, no test code is changed.

### Key Entities *(include if feature involves data)*

This feature does not introduce or modify runtime data entities. The only "entity" is the
**JavadocReport**: a textual artifact (captured before/after) listing per-symbol
errors/warnings with file path and line number. It is an internal verification artifact,
not a runtime concept.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Javadoc warnings for `modules/DesktopContentExplorer` are reduced by at least 80% versus the captured baseline, measured by `mvn-env.sh` javadoc output on a clean checkout on `development` (JDK 21). Baseline and post-cleanup counts are stored as artifacts referenced from `checklists/requirements.md`.
- **SC-002**: Javadoc errors for `modules/DesktopContentExplorer` are reduced to zero; running the javadoc goal on the module exits 0 (build no longer "slowed down" by this module's javadoc issues).
- **SC-003**: The full module build (`./mvn-env.sh -pl modules/DesktopContentExplorer -am verify` or the closest equivalent used by CI) succeeds for this module without javadoc-related failures.
- **SC-004**: Zero public type or method signatures in `modules/DesktopContentExplorer` are changed (verifiable by `git diff --stat` on non-comment, non-whitespace lines matching zero non-javadoc files modified).

## Assumptions

- The user description refers to the `modules/DesktopContentExplorer` module (artifact `perc-content-explorer`), consistent with `AGENTS.md`'s `perc-content-explorer` entry. The phrase "content explorer module" is not ambiguous in this repo.
- The base branch is `development` (JDK 21) per the current checkout state; javadoc behavior follows the JDK 21 javadoc tool, not JDK 8.
- The Maven javadoc plugin configuration is inherited from the parent POM and is the canonical configuration for this module; per-module plugin overrides are not introduced by this feature.
- Warnings are emitted by the same toolchain that the project's CI uses; no special flags need to be invented for the cleanup to be effective.
- "Slowing down the build" refers to javadoc-related failures / excessive warnings during the build, not literal wall-clock performance. The cleanup unblocks the build rather than optimizing build time.
- The cleanup is purely a source-code quality change. No schema, package, distribution, or runtime behavior is in scope.
- Suppressing warnings is acceptable only as a last resort and only where the module already follows that pattern; otherwise root-cause fixes (writing the missing Javadoc) are required.
- The feature is a one-time cleanup, not an ongoing enforcement policy. No new CI gate / quality rule is introduced as part of this spec.