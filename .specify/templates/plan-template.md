# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

- **Language/Version**: Java 21 (on `development`) / Java 8 (on `development-8.1.x`)
- **Owning Module(s)**: [e.g., `system/`, `rest/` — required]
- **AGENTS Hierarchy**: [paths of AGENTS.md / AGENTS.local.md read]
- **Dependencies & Storage**: [e.g., Maven modules, Spring, Hibernate, RDBMS]
- **Testing**: [e.g., JUnit 5, Mockito]
- **Scale/Impact**: [e.g., modules touched, user roles, install/upgrade impact]

## Constitution Check

- [ ] **I. Module-First Boundaries** (owning modules identified, AGENTS applied)
- [ ] **II. Evidence Over Invention** (cites existing paths/APIs)
- [ ] **III. Test Discipline** (unit tests planned for all changes)
- [ ] **IV. Contract & Integration Integrity** (REST/schema impacts assessed)
- [ ] **V. Safe Modernization** (no Spring Boot/framework churn)
- [ ] **VI. Security by Default** (AuthZ, XML, upload, logging reviewed)
- [ ] **VII. Build & Dependency Hygiene** (branch JDK, maven/npm deps)
- [ ] **VIII. Documentation & Operability** (README/i18n updates planned)
- [ ] **IX. PR Review Comment Resolution** (inline replies & resolving threads)
- [ ] **Complexity Budget** (any constitution violations listed in Complexity Tracking with justification)

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # Technical plan
├── research.md          # Research findings
└── tasks.md             # Task list
```

### Source Code (affected paths)

```text
[Insert specific module paths that will change]
```

## Complexity Tracking

*(Only if constitution exceptions are justified)*
- **Violation**:
- **Justification & Alternatives**:
