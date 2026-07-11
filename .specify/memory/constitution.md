<!-- Sync Impact Report:
- Version change: 1.0.0 → 2.0.0 (MAJOR — full rewrite of governance principles)
- List of modified principles:
  - Library-First → I. Module-First Boundaries
  - CLI Interface → II. Evidence Over Invention
  - Test-First (NON-NEGOTIABLE) → III. Test Discipline (NON-NEGOTIABLE)
  - Integration Testing → IV. Contract & Integration Integrity
  - Observability, Versioning & Simplicity → V. Safe Modernization
  - Security Requirements (section) → VI. Security by Default
  - Development Workflow (section) → VII. Build, Platform & Dependency Hygiene
  - (new) VIII. Documentation & Operability
- Added sections: Domain Constraints, Complexity Budget
- Removed sections: Generic "CLI Interface", "Library-First" as product principles
- Templates requiring updates:
  ✅ Updated: .specify/templates/plan-template.md (Constitution Check + Technical Context defaults)
  ✅ Updated: .specify/templates/tasks-template.md (tests mandatory; mono-repo path conventions)
  ✅ Verified: .specify/templates/spec-template.md (user stories / acceptance scenarios remain valid)
  ✅ Verified: .specify/templates/constitution-template.md (upstream scaffold; leave intact)
- Follow-up TODOs: None
- Rationale: Prior constitution was Speckit boilerplate (library-first CLI product). Percussion CMS
  is a long-lived multi-module CMS mono-repo under active modernization; governance must match
  AGENTS.md, module AGENTS files, and operational reality.

- Version change: 2.0.0 → 2.1.0 (MINOR — new principle added)
- List of modified principles:
  - (new) IX. PR Review Comment Resolution (NON-NEGOTIABLE)
- Added sections: none
- Removed sections: none
- Templates requiring updates:
  ✅ Updated: .specify/templates/plan-template.md (Constitution Check now lists principle IX)
  ✅ Updated: .specify/templates/tasks-template.md (no change — principle is enforced by the
     post-implementation PR babysit step, not by the task list itself)
  ✅ Verified: .specify/templates/spec-template.md (no change — principle is operational, not spec-shape)
  ✅ Verified: .specify/templates/constitution-template.md (upstream scaffold; leave intact)
- AGENTS files requiring updates:
  ✅ Updated: ./AGENTS.md (added "PR Review Comment Resolution" section with concrete
     GraphQL/REST command templates so any agent can follow the procedure)
- Follow-up TODOs: None
- Rationale: PR review comments are part of the merge-gate surface. A code-only fix that does
  not also resolve the corresponding review thread leaves the PR in a state that the CI/merge
  gate blocks. Treating comment resolution as part of "fixing the review" — rather than as a
  separate, optional post-step — prevents the false-complete state we hit on PR #1185 (the
  002-jdbc-drivers-cleanup work). See root AGENTS.md "PR Review Comment Resolution" for the
  executable procedure.
-->

# Percussion CMS Constitution

Living governance for specifications, plans, and implementation work in the Percussion CMS
mono-repo (also known as Rhythmyx, CM1, CM System, E2 Server, PercussionCMS).

This constitution binds Speckit artifacts (`spec.md`, `plan.md`, `tasks.md`) and human/agent
contributions. Runtime day-to-day rules live in root `AGENTS.md` and module-level `AGENTS.md` /
`AGENTS.local.md` files. When those conflict with informal habit, **this constitution and the
applicable AGENTS files win**.

## Core Principles

### I. Module-First Boundaries

Percussion CMS is a multi-module mono-repo, not a greenfield single library.

- Every change MUST identify its owning module path (for example `system/`, `rest/`,
  `projects/sitemanage/`, `deliverytiersuite/delivery-tier-suite/`, `WebUI/`,
  `modules/perc-*`).
- Before coding, agents and contributors MUST apply the **Rule Discovery Protocol**:
  1. Resolve the module path.
  2. Read `AGENTS.local.md` if present (highest precedence).
  3. Else read module `AGENTS.md` if present.
  4. Else use root `AGENTS.md`.
- Shared code belongs in intentional shared modules (`modules/utils`,
  `modules/perc-security-utils`, `modules/perc-xml-security`, etc.). Do not create
  organizational-only modules or copy utilities across module trees without a clear owner.
- Ignore module folders that are not referenced (directly or transitively) from the root
  `pom.xml` as child modules unless the task is explicitly inventory/cleanup.

**Rationale**: Work that ignores module ownership creates cross-module coupling, broken
installers, and unmaintainable "shared" code with no home.

### II. Evidence Over Invention

All design and implementation MUST be grounded in the checked-out branch and files present in
the workspace.

- MUST NOT invent third-party APIs, libraries, language features, or Percussion extension
  points that are not documented in official sources (JDK docs for the branch target, MDN,
  Maven coordinates already in this repo, Percussion help, or existing code).
- When requirements or APIs are unclear, MUST ask for clarification rather than guess.
- Specs and plans MUST cite concrete paths, classes, package names, and existing patterns
  (for example REST adaptor pattern, system service locator, package/`.ppkg` deployment).
- Generated scripts MUST live under `./scripts` (repo-wide) or the owning module's script
  directory — never system temp (`%TEMP%`, `$TMPDIR`). Scratch work uses `./tmp`.

**Rationale**: Fluent but invented APIs ship fiction. This codebase is large and historical;
truth lives in the tree and docs, not model memory.

### III. Test Discipline (NON-NEGOTIABLE)

Every behavioral code change MUST ship with corresponding automated tests that pass.

- For new behavior: add tests that fail before the fix/feature, then pass after.
- For edits: update existing tests or add focused regression coverage for the changed
  behavior. No exceptions for "small" fixes.
- Prefer JUnit 5 (`org.junit.jupiter.api.*`) and established project test patterns (Mockito
  where already used). Do not introduce alternate frameworks without explicit approval.
- Tests MUST be runnable via the project Maven wrapper tooling (`./mvn-env.sh` /
  `mvn-env.bat`) on the target branch JDK.
- Integration / contract tests are REQUIRED when changing: public REST or SOAP contracts,
  inter-module service interfaces, package install/upgrade paths, schema/TableFactory
  migrations, authentication/authorization, or CMS ↔ DTS boundaries.
- Speckit task lists MUST include explicit test tasks for each user story; tests are not
  optional decoration.

**Rationale**: Modernization without regression nets is how long-lived CMS installs break
silently for customers.

### IV. Contract & Integration Integrity

Public and semi-public surfaces are products, not implementation details.

- Public REST (`rest/`), internal sitemanage APIs, SOAP/webservices, XML applications, and
  package (`.ppkg`) contents MUST remain backward compatible unless the change is explicitly
  versioned and called out in the plan with a migration path.
- REST changes MUST respect the adaptor pattern (resource → adaptor → system services) and
  Jakarta EE 10 packages (`jakarta.ws.rs.*`, not `javax.ws.rs.*` on JDK 21 tracks).
- Schema and data migrations MUST use established TableFactory / install-upgrade mechanisms;
  ad-hoc SQL against production schemas in application code is forbidden unless already the
  local module pattern and justified.
- DTS (Delivery Tier Suite) and CMS are de-coupled products; do not blur their dependency
  graphs without an architecture decision recorded in the plan.

**Rationale**: Customers upgrade in place. Broken contracts and silent schema drift are
product failures, not cleanup.

### V. Safe Modernization

The codebase is being modernized; do not assume all code follows current best practices.

- Prefer incremental improvement of the code you touch over drive-by refactors of unrelated
  packages.
- Preserve public API signatures unless the task is an explicit breaking change with a
  migration plan.
- When modernizing legacy code, follow local markers and lists (for example system module
  refactor notes) and avoid redoing completed work.
- YAGNI applies: do not introduce new frameworks, Spring Boot, alternate build systems, or
  parallel architectures "while you're here."
- This is **not** a Spring Boot application — MUST NOT add Spring Boot dependencies.

**Rationale**: Big-bang rewrites stall; safe modernization keeps the product shippable.

### VI. Security by Default

Content management systems hold credentials, content, and integration secrets.

- Security-relevant code MUST reuse shared modules (`perc-security-utils`,
  `perc-xml-security`) rather than ad-hoc crypto, XML parsers, or ACL checks.
- Changes to authentication, authorization, session handling, file upload/extract, XML/XSLT
  processing, redirects, cryptography, or logging of request data MUST include threat
  notes in the plan and tests for abuse cases (zip-slip, XXE, open redirect, secret
  leakage, etc.) when those surfaces are touched.
- Follow project security scanning practices (CodeQL, OWASP dependency checks as configured)
  and the public vulnerability process in `SECURITY.md` (responsible disclosure; do not file
  public issues for unfixed vulns).
- MUST NOT log secrets, passwords, tokens, or full sensitive payloads. Prefer structured,
  minimal error messages to clients; keep diagnostics in server logs with appropriate
  redaction.
- Dependency upgrades that fix CVEs are preferred; new exclusions in Dependabot config
  require justification in `.github/dependabot.yml` for the affected branch.

**Rationale**: CMS compromise is high impact; shared security libraries exist so modules do
not re-implement danger.

### VII. Build, Platform & Dependency Hygiene

- Target branch dictates JDK: `development` → JDK 21; `development-8.1.x` → JDK 8.
  Always build and test with `./mvn-env.sh` / `mvn-env.bat`.
- Java dependencies are managed in Maven POMs (parent `pluginManagement` / dependency
  management first). Front-end packages use npm via the Maven frontend plugin where already
  established.
- Format Java with Spotless as required by the module (`spotless:apply` / `spotless:check`).
- Feature work SHOULD live on task/feature branches and land via PR review against the
  appropriate base branch. Do not force-push shared branches or rewrite published history
  without explicit owner approval.
- Generated or one-off tooling scripts require README updates in the owning script
  directory.

**Rationale**: Wrong JDK, rogue dependencies, and unformatted diffs burn CI and reviewers.

### VIII. Documentation & Operability

- Code changes MUST update the nearest durable docs: module README, Maven site markdown,
  package notes, or inline Javadoc for public APIs — whichever the module already uses.
- Specs MUST describe user-visible behavior in plain language (editor, publisher, admin,
  integrator) and acceptance scenarios that can be demonstrated on a running CMS/DTS where
  applicable.
- Logging and failure modes MUST be diagnosable from server logs (for example Jetty base
  logs under a deployed install) without requiring a debugger for common failures.
- Internationalization: user-facing strings in CMS UI paths MUST follow existing i18n/TMX
  patterns (`perc-i18n`) rather than hard-coded English-only UI text when the surrounding
  code is localized.

**Rationale**: A CMS is operated by people who were not in the PR; docs and logs are part of
the feature.

### IX. PR Review Comment Resolution (NON-NEGOTIABLE)

A PR review comment is "addressed" only when **both** of the following are true:

1. An inline reply has been posted on the comment citing the commit hash that fixes it, a
   short description of what changed, and a pointer to tests/docs that back the fix.
2. The corresponding review thread has been resolved via the GitHub `resolveReviewThread`
   GraphQL mutation.

A code-only fix that does not also resolve the corresponding thread is incomplete from the
merge-readiness perspective — the CI/merge gate blocks a PR with unresolved threads, and a
bare resolve is not a substitute for a documented fix.

This applies to **all** review comments on a PR the agent owns, including comments that
arrive after the initial submission (late feedback). Outdated threads (where the diff no longer
contains the offending line) still need an inline reply AND a `resolveReviewThread` call; the
`isOutdated: true` flag does not auto-resolve.

The executable procedure (GraphQL query to list threads, REST endpoint for inline reply,
GraphQL mutation to resolve) is documented in root `AGENTS.md` under "PR Review Comment
Resolution".

**Rationale**: A false-complete state — code changed but the PR is still unmergeable — is
exactly the trap we hit on the 002-jdbc-drivers-cleanup review cycle. The fix to a review
comment and the resolution of the review thread are the same deliverable from the PR
author's perspective.

## Complexity Budget

## Domain Constraints

These product facts constrain every Speckit plan:

| Domain | Constraint |
|--------|------------|
| Product shape | Multi-site CMS with workflow, permissions, packaging, and de-coupled delivery (DTS) |
| Core CMS | `system/` (XML application server + content services) |
| Primary UI | `WebUI/` + backend `projects/sitemanage/` |
| Public API | `rest/` (JAX-RS / CXF, adaptor pattern) |
| Delivery | `deliverytiersuite/delivery-tier-suite` microservices |
| Install/upgrade | `modules/perc-distribution-tree`, `modules/perc-ant`, packages under `modules/perc-packages` |
| Packaging | Deployable units are `.ppkg` (zip) managed by `deployer` |
| Stack | Java (branch JDK), Spring (not Boot), Hibernate, Artemis, React/JSP/jQuery as present, XML/XSL, JUnit 5 |

Specs that propose greenfield frameworks, replacing Jetty/Tomcat wholesale, or merging CMS
and DTS into one process require an explicit architecture exception in Complexity Budget.

## Complexity Budget

Simplicity is preferred. Record every constitution violation in the plan's Complexity Tracking
table with: violation, why needed, and simpler alternative rejected.

Complexity that usually needs justification:

- New top-level Maven modules
- New cross-cutting frameworks or dependency stacks
- Breaking public REST/SOAP/package contracts
- Dual implementations of the same feature (legacy + new) without a removal plan
- Cross-talk between CMS and DTS beyond existing APIs

## Development Workflow

1. **Specify**: Capture user scenarios, acceptance criteria, and module ownership.
2. **Plan**: Run Constitution Check; fix or justify every failure before research/design.
3. **Task**: Break work by user story; include test and docs tasks as first-class items.
4. **Implement**: Small, reviewable commits; module-local changes preferred.
5. **Verify**: Unit (and required integration) tests pass via `mvn-env`; Spotless clean where
   applicable; no invented APIs; AGENTS hierarchy respected.
6. **Review**: PRs MUST be checkable against this constitution (tests, boundaries, security
   surface, compatibility).

Base branch for new work is `development` (JDK 21) unless the task explicitly targets
`development-8.1.x` or another maintained line.

## Governance

- This constitution supersedes informal Speckit defaults and ad-hoc agent habits for this
  repository.
- Day-to-day executable rules remain in `AGENTS.md` (root and module). If AGENTS and this
  file diverge on a point of fact (JDK, module ownership), update both in the same change.
- **Amendments**: Change this file, bump version (MAJOR = principle remove/redefine; MINOR =
  new principle/section; PATCH = clarification), update dependent Speckit templates' Constitution
  Check lists, and note the change in the Sync Impact Report comment at the top of this file.
- **Compliance**: Speckit plans MUST complete Constitution Check before Phase 0 research and
  re-check after Phase 1 design. Unjustified failures block implementation.
- **Runtime guidance**: Use root `AGENTS.md`, module `AGENTS.md` / `AGENTS.local.md`,
  `CONTRIBUTING.md`, and `SECURITY.md`. Skills index:
  `modules/ai-shared-develop/src/main/resources/skills/SKILLS.md`.

**Version**: 2.1.0 | **Ratified**: 2026-03-25 | **Last Amended**: 2026-07-11
