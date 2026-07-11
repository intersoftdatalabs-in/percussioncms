# Implementation Plan: Migrate v8.1.7 Changes to 8.2 Development Branch

**Branch**: `005-migrate-8.1.7-changes` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-migrate-8.1.7-changes/spec.md`

## Summary

Produce a deterministic, re-runnable audit that inventories every non-dependabot PR merged into v8.1.7, classifies each against the `development` branch as `already-present` / `needs-migration` / `not-applicable` / `superseded` / `conflicts-with-newer-design`, and emits a prioritized migration backlog plus a reviewable Markdown summary. The audit itself is read-only; the resulting backlog is then worked through as a sequence of porting PRs against `development`, each landing with regression tests (Constitution Principle III).

The audit pipeline is the deliverable for User Stories 1–3 and 5; User Story 4 (porting individual items) is a follow-on workflow that consumes the backlog and is out of scope for this spec's implementation tasks beyond a representative example.

## Technical Context

**Language/Version**: Bash 4+ for the audit driver; output files are JSON/Markdown only (no runtime code on the audit path). Porting PRs produced downstream use Java 21 on `development` (per `./mvn-env.sh`); no Java 8 source is added.

**Primary Dependencies**:
- `gh` CLI v2.x (authenticated against github.com) — for PR metadata (`gh pr view`, `gh api repos/.../pulls/N/files`)
- `git` with `origin` reachable — for tag resolution, diff inspection, file-existence checks against `development`
- `jq` — for parsing JSON output of `gh` and the audit's own intermediate files
- `bash` — script interpreter (no POSIX-only constraint; the script targets `bash` because the repo's `./mvn-env.sh` and many other scripts already use bash)

**Storage**: None for the audit itself; audit outputs are plain files on disk (per contracts/audit-output-schemas.md). No database, no remote storage.

**Testing**: The audit script is tested by re-running it against known inputs (Scenario 1 in quickstart.md) and asserting exit codes and file contents. No new framework introduced. Per-module regression tests for ported items use the existing JUnit 5 + Mockito stack on the target module.

**Target Platform**: Audit's host environment is any Linux/macOS dev machine with `gh`, `git`, `jq`, `bash`. Porting PRs target the `development` branch running JDK 21 on the existing CMS module structure (`system/`, `WebUI/`, `projects/sitemanage/`, `rest/`, `deliverytiersuite/delivery-tier-suite/`, `modules/perc-*`).

**Project Type**: Cross-cutting audit + selective back-port. The audit script itself is a repo-wide tool (`./scripts/release-audit/`). Porting PRs are per-module and inherit each target module's existing structure.

**Performance Goals**: A full audit run (141 PRs, files-changed fetch + 5 verdict heuristics per PR) MUST complete in under 10 minutes on a developer laptop; per-PR verdicting dominates. Per the research.md sample, a 20-PR sample finishes in roughly 5 minutes of manual investigation; an automated script should match or beat that.

**Constraints**:
- Read-only on `development` (FR-011).
- Per Constitution Principle VII, output paths are under `./tmp/release-audit/` while in development and promoted to `./scripts/release-audit/` once stable, with README per AGENTS.md.
- Per Constitution Principle II (Evidence Over Invention), every verdict MUST cite a concrete commit hash or file path; no verdict without evidence.
- Per Constitution Principle V (Safe Modernization), porting must translate JDK 8 idioms (`javax.ws.rs.`, `javax.persistence.`, `javax.xml.bind.`, etc.) to Jakarta EE 10 equivalents on JDK 21 — never preserve them.
- Branch JDK is fixed: `development` is JDK 21; v8.1.7 PRs that pinned Java-8-only dependency versions are `needs-migration` items (upgrade dependency), not `not-applicable` (skip), except where `development` already runs a different version pair (e.g. PDFBox 3.0.6 + Tika 3.2.3 makes PR #915 `not-applicable`).

**Scale/Scope**: 141 non-dependabot v8.1.7 PRs to classify; estimated 40–70 `needs-migration` items (extrapolating from the 14/20 = 70% rate in the research.md sample); modules touched include `system/`, `WebUI/`, `projects/sitemanage/`, `rest/`, `deliverytiersuite/delivery-tier-suite/*` (8 services), `modules/perc-packages/`, `modules/utils/`, and possibly `modules/perc-ant` / `modules/perc-distribution-tree` for dependency upgrade items.

**Owning module(s)**: The audit script is a repo-wide tool living in `./scripts/release-audit/` (no Maven module owns it). Per-porting-PR ownership follows the target module from `inventory.json[].modulePaths[0]`; the Rule Discovery Protocol is applied per-porting-PR per Constitution Principle I.

**AGENTS hierarchy applied**: `./AGENTS.md` (root) governs the audit script and the cross-cutting workflow. Module-local `AGENTS.md` / `AGENTS.local.md` files apply only during per-module porting (User Story 4) and must be consulted per-item by the porter.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*
*Source: `.specify/memory/constitution.md` v2.1.0*

- [x] **I. Module-First Boundaries**: Audit script is a repo-wide tool (`./scripts/release-audit/`), not a new Maven module; per-porting-PR ownership resolves via the inventory's `modulePaths` field. Rule Discovery Protocol cited in Technical Context for porting work. PASS.
- [x] **II. Evidence Over Invention**: Every verdict cites a concrete commit hash or file path (FR-003 + contracts/audit-output-schemas.md PRVerdict schema). Audit is read-only (FR-011). No invented APIs; PR diff inspection is grounded in `gh api` + `git show` output. PASS.
- [x] **III. Test Discipline**: Audit script is validated by re-running it against known inputs (Scenario 1 quickstart) — that is the test surface for the audit itself. Per-porting-PR regression tests are mandatory under FR-009 and Constitution Principle III; backlog records `testCoverageIn817` per item. PASS.
- [x] **IV. Contract & Integration Integrity**: REST contract changes (PR #929 mapping validation exceptions to `BAD_REQUEST`; PR #894 leading `Sites/` prefix; PR #886 friendly orphaned-page error) are explicitly surfaced via the `securityFlag` + `priority` logic — REST contract PRs surface as P1 in the backlog. Schema/package migrations are out of scope for this audit but flagged when present in inventory. PASS.
- [x] **V. Safe Modernization**: No new frameworks, no Spring Boot. Audit script uses `gh`, `git`, `jq`, `bash` — all already standard tools in this repo. Per-porting-PR workflow translates JDK 8 idioms to JDK 21 / Jakarta EE 10 equivalents (Assumption in spec). PASS.
- [x] **VI. Security by Default**: FR-004 surfaces CVE/security PRs (Shiro 2.1.0, Tomcat 9.0.115, CSP, auth) at the top of the backlog regardless of merge date. The audit script does not handle credentials, XML parsing, or auth surfaces itself — it only inspects diffs. The porting PRs that touch `perc-security-utils`, `perc-xml-security`, REST auth, or CSP MUST cite threat notes and abuse-case tests per Principle VI's own gate. PASS (audit), follow-up required (porting PRs).
- [x] **VII. Build, Platform & Dependency Hygiene**: Audit script runs on bash with no Maven dependency; porting PRs run on the target module's existing build. JDK is fixed by branch (development = JDK 21); script promotes to `./scripts/release-audit/` with README per AGENTS.md. Spotless runs per module on porting PRs, not on the audit script. PASS.
- [x] **VIII. Documentation & Operability**: contracts/audit-output-schemas.md and quickstart.md are the durable docs. Audit outputs are reviewable from `v8.1.7-to-8.2-migration-report.md` (SC-007: <10 min review). Failure modes: `gh` not authenticated ⇒ exit 4; tag range unresolvable ⇒ exit 3; partial failure ⇒ exit 2 with errors logged. PASS.
- [x] **IX. PR Review Comment Resolution**: Applies to porting PRs (User Story 4), not the audit itself. The plan documents this in quickstart.md Scenario 4 step 6 and in the per-porting-PR pattern; porter MUST follow root `AGENTS.md` "PR Review Comment Resolution" procedure. N/A for the audit deliverable.
- [x] **Complexity Budget**: No new top-level Maven modules (audit script is a bash tool under `./scripts/`). No new cross-cutting frameworks. No breaking public REST/SOAP contracts introduced by the audit (porting PRs that DO break contracts must be flagged in the backlog with a migration plan). No dual implementations. PASS.
- [x] **Governance**: This Constitution Check is re-affirmed after Phase 1 design (no changes). AGENTS.md remains the runtime guide for daily workflow.

**Re-check after Phase 1 design**: All gates still pass. The contracts/audit-output-schemas.md does not introduce new public APIs (it's an internal JSON schema); the data-model.md does not introduce new persistent entities; the quickstart.md does not propose new tooling. No new violations.

## Project Structure

### Documentation (this feature)

```text
specs/005-migrate-8.1.7-changes/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output — 141-PR inventory + 20-PR sample verdicts
├── data-model.md        # Phase 1 output — PRRecord, PRVerdict, MigrationBacklogItem, AuditRun
├── quickstart.md        # Phase 1 output — 5 runnable scenarios
├── contracts/
│   └── audit-output-schemas.md  # Phase 1 output — JSON/MD schemas + CLI surface
├── checklists/
│   └── requirements.md  # Created by /speckit.specify
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository modules)

```text
scripts/release-audit/                    # NEW: audit script (promoted from tmp/ once stable)
├── release-audit.sh                      # main driver
├── lib/
│   ├── inventory.sh                      # PRRecord collection
│   ├── verdicts.sh                       # PRVerdict classification
│   ├── backlog.sh                        # MigrationBacklogItem ordering + Markdown
│   └── report.sh                         # v8.1.7-to-8.2-migration-report.md generator
└── README.md                             # usage, prerequisites, examples

tmp/release-audit/v8.1.6..v8.1.7/         # GENERATED: audit outputs (gitignored)
├── inventory.json
├── dependabot-excluded.json
├── verdicts.json
├── migration-backlog.md
└── v8.1.7-to-8.2-migration-report.md
```

**Per-porting-PR structure** (representative; downstream of this spec, not part of `tasks.md` for the audit itself):

```text
# One PR per backlog item; structure follows the target module
# Example: PR for #894 → rest/src/main/java/com/percussion/rest/pages/PagesResource.java
#                                 rest/src/test/java/com/percussion/rest/pages/PagesTest.java
#                                 rest/src/test/java/com/percussion/rest/pages/PageTestAdaptor.java
```

**Structure Decision**: The audit lives at `./scripts/release-audit/` per AGENTS.md rule "ALWAYS add generated scripts to repo script dir" — it is a repo-wide tool, not a per-module artifact. The audit script is intentionally NOT a new Maven module: introducing one would require modifying the root `pom.xml` modules list, dependabot configuration, and CI matrix, which violates the Complexity Budget for a tool that produces no compiled artifact. The generated outputs live under `./tmp/release-audit/` per AGENTS.md rule "NEVER read and write to %TEMP% or $TMPDIR" and are gitignored.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |

No constitution violations require justification. The audit script stays within the existing tool surface (`bash`, `gh`, `git`, `jq`) and introduces no new frameworks, modules, or contracts. Porting PRs are downstream and must clear their own Constitution Check at PR time.