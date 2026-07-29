# Percussion CMS Constitution

Living governance for specifications, plans, and implementation work in the Percussion CMS mono-repo.
Conflicts are resolved in favor of this constitution and applicable `AGENTS.md` files.

## Core Principles

### I. Module-First Boundaries

- Apply Rule Discovery Protocol (resolve module path, read local/module `AGENTS.md`, fall back to root `AGENTS.md`) before changing code.
- Shared code belongs in `modules/utils` or `modules/perc-security-utils`; do not create organizational-only modules or copy utilities.

### II. Evidence Over Invention

- Do not invent APIs, libraries, language features, or extension points.
- Cite concrete paths, classes, and patterns in plans. Write scripts under `./scripts` or module script paths, never system temp.

### III. Test Discipline (NON-NEGOTIABLE)

- Every behavioral code change MUST have passing tests runnable via `./mvnw` / `mvnw.cmd` on the target JDK.
- Integration/contract tests are required for REST/SOAP changes, schema migrations, and authentication updates.

### IV. Contract & Integration Integrity

- Keep REST, SOAP, sitemanage, XML apps, and package (`.ppkg`) formats backward compatible.
- Database schema changes must use TableFactory; do not use ad-hoc SQL in application code.

### V. Safe Modernization

- Focus on incremental, localized refactoring; preserve API signatures.
- Never add Spring Boot dependencies or unneeded third-party frameworks.

### VI. Security by Default

- Reuse shared security modules (`perc-security-utils`, `perc-xml-security`).
- Never log secrets, passwords, or tokens. Threat-test security changes for common exploits (e.g. zip-slip, XXE).

### VII. Build & Dependency Hygiene

- Target correct JDK (JDK 21 for `development`, JDK 8 for `development-8.1.x`) via `./mvnw`.
- Manage dependencies in Maven parent/module POMs. Format Java with Spotless.

### VIII. Documentation & Operability

- Update nearest README, Maven site, or Javadoc. Ensure failures are diagnosable from server logs.
- Follow localized i18n (`perc-i18n`) patterns for user-visible strings.

### IX. PR Review Comment Resolution (NON-NEGOTIABLE)

- Resolving review comments requires:
  1. An inline reply citing the fix commit, a description of the change, and test details.
  2. Resolving the thread using the GitHub `resolveReviewThread` GraphQL mutation.

---

## Domain Constraints

- **Core CMS**: `system/` (Spring, Hibernate, Artemis, XML/XSL).
- **UI**: `WebUI/` + backend `projects/sitemanage/`.
- **Public API**: `rest/` (JAX-RS / CXF, adaptor pattern).
- **Delivery**: `deliverytiersuite/delivery-tier-suite/` microservices.
- **Packaging/Installer**: `.ppkg` via `deployer` / `modules/perc-packages`.

## Complexity Budget

- Exceptions (e.g., new top-level modules, breaking contract changes) must be justified in the plan's Complexity Tracking section.

## Development Workflow

- Sequence: 1. Specify -> 2. Plan -> 3. Task -> 4. Implement -> 5. Verify -> 6. Review.
- Story Checkpoint: Implement, commit, and submit a PR for each story individually. Monitor Kilo Code checks, resolve review comments, and verify the PR is merged to the base branch before starting the next story.

## Governance

- This constitution supersedes informal practices. Plans must complete the Constitution Check before research and after design.
- Amendments: Bump version (MAJOR = rewrite; MINOR = new principle; PATCH = clarification), sync template checklist.

**Version**: 2.3.0 | **Ratified**: 2026-03-25 | **Last Amended**: 2026-07-13

