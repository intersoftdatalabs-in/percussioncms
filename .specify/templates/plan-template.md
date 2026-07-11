# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  Replace placeholders with concrete values for this feature. Defaults below reflect
  Percussion CMS mono-repo reality — delete or override per plan.
-->

**Language/Version**: Java 21 on `development` (Java 8 only if targeting `development-8.1.x`)
**Primary Dependencies**: Maven multi-module; Spring (not Boot); Hibernate; JAX-RS/CXF as applicable; existing module stack
**Storage**: CMS RDBMS via existing persistence; TableFactory for schema/data migration when needed
**Testing**: JUnit 5, Mockito; module unit tests required; integration/contract tests when APIs or CMS↔DTS boundaries change
**Target Platform**: CMS on Jetty (install tree); DTS services as separate deployables when in scope
**Project Type**: Multi-module CMS mono-repo (core `system/`, UI `WebUI/` + `projects/sitemanage/`, public API `rest/`, DTS under `deliverytiersuite/`)
**Performance Goals**: [domain-specific or N/A — state if editorial UI latency, publish throughput, or API SLAs apply]
**Constraints**: Branch JDK via `./mvn-env.sh`; no Spring Boot; backward-compatible public contracts unless explicitly versioned; module AGENTS hierarchy
**Scale/Scope**: [modules touched, user roles, install/upgrade impact]
**Owning module(s)**: [e.g., `system/`, `rest/`, `projects/sitemanage/` — required]
**AGENTS hierarchy applied**: [paths of AGENTS.md / AGENTS.local.md read]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*
*Source: `.specify/memory/constitution.md` (v2.x)*

- [ ] **I. Module-First Boundaries**: Owning module(s) identified; Rule Discovery Protocol applied; no orphan shared code
- [ ] **II. Evidence Over Invention**: Approach cites existing paths/APIs; no invented libraries or extension points
- [ ] **III. Test Discipline**: Unit tests planned for every behavioral change; fail-then-pass strategy noted
- [ ] **IV. Contract & Integration Integrity**: REST/SOAP/package/schema impacts assessed; compatibility or migration plan
- [ ] **V. Safe Modernization**: Scope limited to needed change; no Spring Boot / drive-by framework churn
- [ ] **VI. Security by Default**: AuthZ, XML, upload, crypto, redirect, and logging surfaces reviewed; shared security modules preferred
- [ ] **VII. Build, Platform & Dependency Hygiene**: Correct branch JDK; Maven/npm deps via existing management; Spotless if module requires
- [ ] **VIII. Documentation & Operability**: README/site/Javadoc/i18n updates planned; diagnosable failures
- [ ] **Complexity Budget**: Any principle violations listed in Complexity Tracking with justification
- [ ] **Governance**: Plan will re-check this list after Phase 1; AGENTS.md remains runtime guide

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository modules)

<!--
  ACTION REQUIRED: Replace with the concrete module paths for this feature.
  Delete unused sections. Do not leave generic Option labels in the delivered plan.
-->

```text
# Core CMS (typical)
system/
├── services/src/main/java/...
├── src/main/java/...
└── **/src/test/java/...

# Public REST API
rest/src/main/java/...
rest/src/test/java/...

# Primary UI backend + front end
projects/sitemanage/src/main/java/...
WebUI/

# Delivery Tier (only if DTS in scope)
deliverytiersuite/delivery-tier-suite/<service>/

# Shared libraries (only if intentionally shared)
modules/perc-security-utils/
modules/utils/
modules/perc-xml-security/
```

**Structure Decision**: [List real directories that will change and why those modules own the work]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., new Maven module] | [current need] | [why existing module insufficient] |
| [e.g., breaking REST field] | [specific problem] | [why additive change insufficient] |
