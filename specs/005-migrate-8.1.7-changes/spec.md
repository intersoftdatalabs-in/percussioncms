# Feature Specification: Migrate v8.1.7 Changes to 8.2 Development Branch

**Feature Branch**: `[005-migrate-8.1.7-changes]`
**Created**: 2026-07-11
**Status**: Draft
**Input**: User description: "We recently released version tagged v8.1.7 with a number of bug fixes, some minor enhancements, and security updates. This is based on the development-8.1.x line wich is still targetting java 1.8. The 8.2 development branch has diverged significantly from development-8.1.x we need to ensure that we have migrated the issues that were included in 8.1.7 to the current development branch. We can ignore dependabot pull requests, but all other PRs may be in scope. We need to first analyze all of the issues adress in 8.1.7 and then for each issue, verify that the change (if relevant) have also been made on the 8.2 branch."

## Clarifications

### Session 2026-07-11

- Q: Verdict rule for security/dependency-version PRs where `development` already runs a patched version → A: Classify by functional intent — `already-present` when dev already runs the patched version, `needs-migration` (priority P0) when not, `not-applicable` only when the PR targets a bug specific to the dev's version pairing.
- Q: Ownership boundary — does this spec own the porting PRs, or just the audit + backlog? → A: Audit and prioritized backlog only. Porting PRs are downstream work products; the backlog is the handoff. SC-006 is reframed to assert that each backlog item records `testCoverageIn817` so the porter knows what regression coverage to bring forward, not that every backlog item has been ported by spec completion.
- Q: How should PRs that resolve the same GitHub issue be presented (cluster vs separate)? → A: Separate rows per PR in the backlog — each PR carries its own per-PR verdict and evidence per Constitution Principle II. Add an "Issue clusters" appendix to `migration-backlog.md` that groups PRs by GitHub issue number and recommends a canonical PR for cherry-pick, so the porter does not have to discover the relationship manually.

## Module Scope *(mandatory for this mono-repo)*

- **Primary module(s)**: Cross-cutting — touches every module that received v8.1.7 fixes (e.g. `system/`, `WebUI/`, `projects/sitemanage/`, `rest/`, `deliverytiersuite/delivery-tier-suite/` per-service, `modules/perc-i18n`, `modules/perc-security-utils`, `deployer`, installer modules). No single module "owns" this work; it is a **migration audit + selective back-port** task.
- **Secondary / integration modules**: `modules/perc-ant`, `modules/perc-distribution-tree` (release/build infrastructure referenced by v8.1.7 release notes), `modules/perc-tinymce` (CMS editor affected by accessibility fixes).
- **AGENTS files to apply**: `./AGENTS.md` (root). No single module-local `AGENTS.md` / `AGENTS.local.md` can govern the full set; module-specific files must be consulted per-issue as part of step 6 of User Story 1.
- **User roles affected**: CMS editor (UI fixes), publisher (publishing fixes), admin (admin console / categories), integrator (REST contract fixes), site visitor (accessibility / metadata fixes on delivered pages).
- **Install / upgrade impact**: dependency-version changes (Apache Shiro 2.1.0, Tomcat 9.0.115, Jetty Maven Plugin 9.4.58.v20250814, PDFBox 2.0.36, etc.) and `package/.ppkg` content changes are in scope; **however** the spec assumes JDK 21 on the 8.2 branch, so Java-8-only fixes are out of scope (see Assumptions).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Inventory all non-dependabot PRs merged into v8.1.7 (Priority: P1)

A maintainer wants a complete, deduplicated inventory of every non-dependabot PR that landed in v8.1.7 so each one can be triaged for back-port to the 8.2 branch.

**Why this priority**: This is the foundation for every downstream step. Without a complete inventory, gaps in the migration cannot be detected, and the audit cannot be repeated on a future release without re-deriving the list from scratch.

**Independent Test**: Run the analysis script against the `v8.1.7` tag and verify (a) every non-dependabot PR merged into the v8.1.7 lineage between v8.1.6 and v8.1.7 appears in the output exactly once, and (b) every dependabot PR is excluded. Spot-check 5 random PR numbers against `gh pr view`.

**Acceptance Scenarios**:

1. **Given** the v8.1.7 tag exists on `origin`, **When** the analysis script runs, **Then** it produces a JSON/CSV report listing every non-dependabot PR with number, title, author, merge date, modules touched, and labels.
2. **Given** a dependabot PR (e.g. `build(deps): Bump …`), **When** the analysis script runs, **Then** the PR is excluded from the report and the exclusion is logged for auditability.
3. **Given** multiple PRs that resolve the same issue (e.g. PRs #924, #872, #897 all touching issue #867), **When** the report is generated, **Then** the report keeps each PR as a separate row in the backlog (each PR carries its own per-PR verdict and evidence per Constitution Principle II) AND emits an "Issue clusters" appendix in `migration-backlog.md` that groups PRs by GitHub issue number and recommends the canonical PR for cherry-pick.

---

### User Story 2 - For each v8.1.7 PR, determine whether the change is already present on the `development` branch (Priority: P1)

A maintainer wants, for every v8.1.7 PR, a per-PR verdict of "already on 8.2", "needs migration", "not applicable (Java 8 only)", or "superseded by a different fix", so the migration backlog is bounded.

**Why this priority**: This is the decision surface. Without a per-PR verdict, "verify that the change has also been made on the 8.2 branch" cannot be answered for any individual issue.

**Independent Test**: For any single PR in the inventory, run the comparison check and verify the verdict is reproducible (same input → same output) and cites concrete evidence (commit hashes, file paths, or absence thereof).

**Acceptance Scenarios**:

1. **Given** a v8.1.7 PR with a fix in `system/src/main/java/…/Foo.java`, **When** the comparison runs against the `development` branch HEAD, **Then** the report shows either (a) the same code change present in `development` (commit hash cited), or (b) the code change absent (path checked, not found), or (c) an equivalent fix already in `development` via a different PR.
2. **Given** a v8.1.7 PR that is Java-8-specific (e.g. a `sun.misc.*` workaround, or a dependency pinned for Java 8 compatibility), **When** the comparison runs, **Then** the PR is classified "not applicable" with a one-sentence reason and is excluded from the migration backlog.
3. **Given** a v8.1.7 PR that conflicts with a newer 8.2 design (e.g. a UI widget that was replaced on the 8.2 line), **When** the comparison runs, **Then** the PR is classified "superseded" with a pointer to the 8.2 replacement.

---

### User Story 3 - Surface the actionable migration backlog to maintainers (Priority: P1)

A maintainer wants a single document that lists only the PRs that need action, ordered by user-visible impact and security severity, so the team can plan the actual porting work.

**Why this priority**: The end deliverable of this audit is a prioritized backlog. Without it, the discovery work in stories 1–2 produces data but no action.

**Independent Test**: A new maintainer can open the backlog file and, for any item, (a) read the one-line description, (b) find the source v8.1.7 PR, (c) find the target module on the `development` branch, and (d) know whether it is a cherry-pick candidate or requires manual rework.

**Acceptance Scenarios**:

1. **Given** the verdict from User Story 2, **When** the backlog is generated, **Then** only PRs classified "needs migration" appear, grouped by module, with security PRs (Shiro, Tomcat, etc.) and publishing / REST contract fixes surfaced first.
2. **Given** a backlog item, **When** a maintainer opens it, **Then** the item lists: v8.1.7 PR link, target module path, suggested migration strategy (cherry-pick / back-port / re-implement / skip), and any blocker discovered during analysis.
3. **Given** the backlog is reviewed by two maintainers, **When** they disagree on a verdict, **Then** the report's per-PR evidence (commit hashes, file paths) is sufficient for them to resolve the disagreement without re-running analysis.

---

### User Story 4 - Migrate a backlog item to the 8.2 branch with tests (Priority: P2)

A developer takes one item from the backlog and produces a `development`-branch PR that ports the fix, with regression tests, per Constitution Principle III (Test Discipline).

**Why this priority**: The backlog is the input to actual porting work; without a repeatable porting workflow, the audit findings sit idle.

**Independent Test**: Pick a backlog item, follow the workflow, and verify (a) the v8.1.7 fix is functionally present on the 8.2 branch, (b) the change compiles on JDK 21, (c) tests pass via `./mvn-env.sh`, (d) any JDK-8-isms in the original fix have been translated to JDK 21 equivalents.

**Acceptance Scenarios**:

1. **Given** a "needs migration" backlog item with a clean cherry-pick target, **When** the developer runs the porting workflow, **Then** a PR is opened against `development` that cherry-picks the commit, resolves any minor conflicts, and adds/updates tests per Constitution Principle III.
2. **Given** a backlog item that depends on JDK-8-only code, **When** the developer runs the porting workflow, **Then** the JDK-8 idiom is translated to its JDK 21 / Jakarta EE 10 equivalent and the change is documented in the PR description.

---

### User Story 5 - Maintain the audit as a repeatable process for future 8.1.x releases (Priority: P3)

A release engineer wants to re-run the same audit when v8.1.8 (or v8.2.0) is cut, without re-deriving the analysis logic each time.

**Why this priority**: The mono-repo will keep cutting 8.1.x point releases for Java-8 customers while the 8.2 line evolves independently. Without a reusable pipeline, every release requires a manual re-analysis.

**Independent Test**: Tag a hypothetical new release, run the pipeline against the new tag range, and verify the same script produces a comparable inventory and verdict without code changes.

**Acceptance Scenarios**:

1. **Given** the audit script lives in `./scripts/release-audit/`, **When** a release engineer points it at a different tag range, **Then** the script produces an inventory and verdict for the new range without modification.

### Edge Cases

- What happens when a v8.1.7 PR's commit does not exist on `development` but an equivalent functional fix is present under a different commit? (Detected as "superseded"; verdict must cite the equivalent commit.)
- What happens when a v8.1.7 PR was squash-merged and the commit message does not reference the PR number? (Falls back to scanning PR-merge commits and `Fixes #NNN` patterns in commit messages.)
- What happens when a v8.1.7 PR touches code that was deleted on the `development` branch (e.g. a legacy widget replaced by a new component)? (Classified "not applicable / deleted"; the report flags it so the maintainer can decide whether the deletion was intentional.)
- What happens when the `origin` remote is unreachable during the audit? (Script fails fast with a clear message; no partial report is written.)
- What happens when the same PR number exists in both the 8.1.x and 8.2 lineages with different content? (Report disambiguates by base ref.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST produce a complete inventory of every non-dependabot PR merged into the `v8.1.7` lineage between the previous release tag (`v8.1.6`) and the `v8.1.7` tag, including PR number, title, author, merge date, and the set of module paths the PR touched.
- **FR-002**: System MUST exclude dependabot PRs from the inventory by default, log each exclusion with PR number and reason, and allow a maintainer to override the exclusion with an explicit flag.
- **FR-003**: System MUST, for each inventoried PR, classify the change against the `development` branch HEAD as one of: `already-present`, `needs-migration`, `not-applicable`, `superseded`, or `conflicts-with-newer-design`, and MUST cite the evidence (commit hash, file path, or "not found at path") supporting the classification.
- **FR-004**: System MUST surface security-relevant PRs (CVE fixes, dependency upgrades addressing vulnerabilities, authentication/authorization changes) at the top of the migration backlog regardless of merge date.
- **FR-005**: System MUST group the migration backlog by module path so that work can be assigned per module owner.
- **FR-005a**: System MUST emit an "Issue clusters" appendix in `migration-backlog.md` that maps GitHub issue numbers to the set of v8.1.7 PRs that reference them and recommends a canonical PR for cherry-pick. Each PR still appears as its own row in the backlog with its own per-PR verdict and evidence (per FR-003 and Constitution Principle II); the appendix is read-only and does not collapse per-PR rows.
- **FR-006**: System MUST detect Java-8-only idioms in v8.1.7 PRs (e.g. `sun.misc.*`, `javax.ws.rs.*`, `javax.persistence.*`, `com.sun.*`, removed/refactored JDK 8 APIs) and classify those PRs as `not-applicable` on the JDK 21 / Jakarta EE 10 `development` branch unless an equivalent fix is already present.
- **FR-006a**: For security/dependency-version PRs (e.g. CVE upgrades for Shiro, Tomcat, CSP), the verdict MUST be derived from the functional intent of the PR against the `development` branch's current dependency state: (a) `already-present` if the `development` branch already runs the patched version of the same component, citing the dev pom version as evidence; (b) `needs-migration` (priority P0) if the `development` branch does not yet run the patched version; (c) `not-applicable` only if the PR addresses a bug specific to the `development` branch's version pairing (e.g. PR #915 PDFBox downgrade — dev runs PDFBox 3.0.6 + Tika 3.2.3, which does not exhibit the trigger). The `not-applicable` case is reserved for version-pair-specific workarounds, not for security upgrades that the dev branch simply hasn't applied yet.
- **FR-007**: System MUST persist the inventory, per-PR verdict, and migration backlog to files under `./tmp/release-audit/v8.1.7/` (and committed to `./scripts/release-audit/` once stable) so the output is reviewable in a PR.
- **FR-008**: System MUST be re-runnable against a different tag range (e.g. `v8.1.6..v8.1.8`) without code changes; tag range MUST be a CLI argument.
- **FR-009**: Per Constitution Principle III, each ported fix MUST land on the `development` branch with updated or new automated tests; the migration backlog MUST record, per item, whether tests exist in the v8.1.7 source PR so the porter knows what regression coverage to bring forward.
- **FR-010**: System MUST produce a Markdown summary report (`v8.1.7-to-8.2-migration-report.md`) suitable for posting to a GitHub issue or PR description, listing total PRs analyzed, counts per verdict, top-priority items, and a link to the per-PR evidence file.
- **FR-011**: System MUST NOT modify any source file as part of the audit itself; porting is a separate, follow-on workflow that produces PRs against `development`.

### Key Entities *(include if feature involves data)*

- **PRRecord**: One PR from the v8.1.7 lineage. Attributes: number, title, author, merge date, base ref, head SHA, files touched (module paths), labels, dependabot flag.
- **PRVerdict**: Per-PR classification result. Attributes: PR number, verdict (`already-present` | `needs-migration` | `not-applicable` | `superseded` | `conflicts-with-newer-design`), evidence (commit hash / file path / reason string), security flag, jdk8-only flag.
- **MigrationBacklogItem**: One actionable item. Attributes: PR number, target module path, suggested migration strategy (`cherry-pick` | `back-port` | `re-implement` | `skip`), blocker notes, links to v8.1.7 PR and to any 8.2 replacement commit.
- **AuditRun**: One execution of the audit pipeline. Attributes: tag range, run timestamp, inventory file path, verdict file path, backlog file path, summary report path, total PRs analyzed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Within the first run, 100% of non-dependabot PRs merged into v8.1.7 are inventoried (verifiable by cross-checking the inventory file against `gh pr list --state merged --base development-8.1.x`).
- **SC-002**: 0 dependabot PRs appear in the inventory when the script is run with default settings (verifiable by grepping the inventory file for `dependabot[bot]`).
- **SC-003**: 100% of inventoried PRs receive one of the five verdict classifications, and each verdict cites concrete evidence (commit hash, file path, or explicit "not found at path" reason).
- **SC-004**: The migration backlog lists every `needs-migration` PR grouped by module path, with security PRs and REST contract changes in the first section.
- **SC-005**: Re-running the audit with a different tag range (e.g. `v8.1.6..v8.1.8`) produces a comparable inventory and verdict with no script changes.
- **SC-006**: For every `needs-migration` backlog item, the backlog records `testCoverageIn817` (which tests the v8.1.7 PR shipped) so the downstream porter knows what regression coverage to bring forward per Constitution Principle III. SC-006 does NOT assert that every backlog item has been ported by spec completion — porting is downstream work tracked via the backlog as the handoff artifact.
- **SC-007**: The Markdown summary report is reviewable in under 10 minutes by a maintainer who has not seen the v8.1.7 release notes (verifiable by a maintainer reading the report and correctly answering "what is the migration backlog?" without opening any other file).

## Assumptions

- The v8.1.7 tag on `origin` is authoritative for "what is in v8.1.7"; the `v8.1.6` tag is the lower bound for the inventory range. If the lower bound needs adjustment (e.g. v8.1.6 is on a different lineage), the script accepts an explicit `--from-tag` argument.
- "Development branch" means the currently checked-out `development` branch (JDK 21 / Jakarta EE 10). The script's `--target-branch` argument defaults to `development` but accepts overrides for ad-hoc verification against other branches.
- The GitHub CLI (`gh`) is available and authenticated for PR metadata queries; PR data not available via `gh` is fetched via `git log` and `git show` against the local clone.
- Dependabot PRs are identified by author login matching `dependabot[bot]` or by PR labels containing `dependencies`; either signal alone is sufficient to exclude.
- "Java 8 only" is detected heuristically by scanning the PR's diff for `sun.misc.`, `javax.ws.rs.`, `javax.persistence.`, `javax.xml.bind.`, `com.sun.`, or imports removed from JDK 11+; the porter is expected to confirm before classification is final.
- The migration backlog is advisory — actual porting is a separate workflow that opens PRs against `development`. This spec owns the audit and the backlog, not the porting PRs themselves, except where User Story 4 is exercised as a representative example.
- The audit script writes outputs under `./tmp/release-audit/v8.1.7/` per the repo's `./tmp` convention (AGENTS.md); once stable, the script and its README are promoted to `./scripts/release-audit/` per AGENTS.md rule "ALWAYS add generated scripts to repo script dir".
- All file outputs are plain text / JSON / Markdown so they diff cleanly in a PR review.
- Per Constitution Principle II (Evidence Over Invention), the script cites concrete commit hashes and file paths for every verdict; it does not invent classifications without evidence.
- Per Constitution Principle VII, JDK is fixed by branch: `development` is JDK 21, so any v8.1.7 fix that pins a dependency to a Java-8-only version is a "needs migration" item (upgrade to a JDK 21-compatible version), not a "not applicable" item, except where the dependency was already moved forward on `development`.

