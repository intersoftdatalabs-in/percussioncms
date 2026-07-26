# Implementation Plan: Zero Open Code Scanning Alerts for 8.2 Release

**Branch**: `004-zero-code-scanning-alerts` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-zero-code-scanning-alerts/spec.md`

## Summary

This feature drives every open code-scanning (CodeQL) alert on the `8.2` release branch to one of four dispositions — **Obsolete (remove)**, **Valid (mitigate)**, **False Positive (suppress with justification)**, or **Accepted Risk (document only)** — and produces a release-readiness report at sign-off. The headline success criterion is `0 active code-scanning alerts` for `8.2` (with accepted-risks explicitly excluded by name in the release notes).

The technical approach, locked in by Phase 0 research, is:

1. **Reuse the existing scanner infrastructure** — `.github/workflows/codeql.yml` (CodeQL Advanced on push to `development` + weekly cron for `actions`, `java-kotlin`, `javascript-typescript`) and `.github/dependabot.yml` (weekly Maven updates gated on `development-8.1.x`). No new CI is required.
2. **Reuse the existing alert-fetch script** — `scripts/fetch-gh-code-scanning-alerts.sh` writes `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` (already wired by prior work). No new helper script is introduced.
3. **Author a triage inventory** at `docs/ai-generated/tasks/gh-codeql-alerts/triage.md` (one row per open alert, format locked in [contracts/C1](./contracts/README.md#c1-triage-inventory)).
4. **Drive each disposition to closure** via the four work flows in [quickstart.md](./quickstart.md) — Obsolete → `git rm` + packaging update + rebuild verification; Valid → fix + regression test that fails on pre-fix code; False Positive → inline `// codeql[rule-id]` suppression with justification + suppression index entry; Accepted Risk → documented entry with owner + target milestone + expiry date.
5. **Re-scan via the existing CodeQL workflow** (triggered automatically by `push` to `development`) and confirm `0 active` alerts (excluding accepted-risks).
6. **Publish the release-readiness report** at `docs/ai-generated/tasks/gh-codeql-alerts/release-readiness-8.2.md` and reference it from the `8.2` release notes.
7. **Per-PR review-comment resolution** per Constitution IX and the procedure in `./AGENTS.md`: every closing PR gets inline replies AND `resolveReviewThread` mutations before it is merge-ready.

## Technical Context

**Language/Version**: Java 21 on `development` (per Constitution VII). The feature itself introduces no new Java source — it is an organizational / remediation workflow that touches existing source in many modules. Any code edits during mitigation follow the project's per-module language stack.

**Primary Dependencies**: Existing repository stack only — no new dependencies. CodeQL scanning via `github/codeql-action@v4` (already in `.github/workflows/codeql.yml`). GitHub CLI (`gh`) for fetching alerts via the existing `scripts/fetch-gh-code-scanning-alerts.sh`. `jq` for JSON processing in the fetch script.

**Storage**: None — this is not an application feature. The feature's "data" lives in versioned markdown files under `docs/ai-generated/tasks/gh-codeql-alerts/` (see [data-model.md](./data-model.md) for the entities and storage locations).

**Testing**: Per-module JUnit 5 + Mockito per `./AGENTS.md` and Constitution III. Every Valid disposition MUST ship a regression test that demonstrably fails on the pre-fix code and passes on the post-fix code (verified by commit hash recorded in the PR body). Removal and suppression dispositions require build + test re-runs but not new test classes.

**Target Platform**: CMS distribution on Jetty (install tree) is the downstream artifact that changes when obsolete files are removed — `modules/perc-distribution-tree`, `modules/perc-ant`, `modules/perc-packages` are the packaging surface that MUST be kept consistent with the source tree.

**Project Type**: Multi-module CMS mono-repo. The coordination lives at repo root, but each concrete mitigation PR is owned by exactly one module per `./AGENTS.md`.

**Performance Goals**: N/A — no runtime path is changed by the triage/suppression process itself. Removed obsolete files may incidentally improve scan time.

**Constraints**: Per Constitution and `./AGENTS.md`: branch JDK via `./mvn-env.sh`; no Spring Boot; PR review-comment resolution per Constitution IX; Dependabot exclusions must be justified in `.github/dependabot.yml` when a CVE-driven bump exceeds a Dependabot exclusion (Constitution VI); generated scripts MUST live under `./scripts`; AGENTS files (root + module-level) MUST be consulted per the Rule Discovery Protocol for every per-module PR.

**Scale/Scope**: Repository-wide scan — every active module under `./` is potentially in scope (per the Module Scope section of [spec.md](./spec.md)). Distribution tree / installer impact: `modules/perc-distribution-tree`, `modules/perc-ant`, `modules/perc-packages` MUST be updated whenever a removed file was previously bundled. Cross-cutting files (e.g., shared `install.xml`) are assigned to the primary owner named in `./AGENTS.md` with secondary modules listed for packaging impact only.

**Owning module(s)**: Repo root for triage/coordination (this feature's process); per-module ownership for each concrete mitigation PR — primary owner is the path of the flagged file under `./AGENTS.md`'s module list.

**Per-module inventory for in-flight US3 clusters** (added 2026-07-16 per analyze finding C1, updated as clusters close):

| Task |                              Rule                              |                      Module(s)                       |                                                                          Primary file                                                                           |
|------|----------------------------------------------------------------|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T055 | `java/stack-trace-exposure`                                    | `modules/TableFactory/`                              | `PSJdbcTableFactoryException.java`                                                                                                                              |
| T056 | `java/implicit-cast-in-compound-assignment` (US4 reclassified) | `deliverytiersuite/.../feeds/`, `system/HTTPClient/` | `PSFeedServicePerformanceTest.java`, `BufferedInputStream.java`, `RespInputStream.java`                                                                         |
| T057 | `java/insecure-cookie`                                         | `modules/p13n-api/`                                  | `CookieGenerator.java` (note: p13n-api pom has 3 missing-version transitive deps; per `AGENTS.md` ignore rule, build verification falls back to CodeQL re-scan) |
| T058 | `js/polynomial-redos` (US2 obsolete-tracking)                  | `WebUI/`                                             | `components/requirejs-text/text.js` (US2 task T026b pending)                                                                                                    |
| T059 | `js/redos`                                                     | `deliverytiersuite/.../p13n-ds/`                     | `perc_p13n_profile.js` (fix applied for #1040); `lib-js/jquery-treeview/lib/jquery.js` (US2 task T026c pending for #1038, #1039)                                |

**AGENTS hierarchy applied**: `./AGENTS.md` (root — authoritative for the triage process, the PR review-comment procedure, the JDK/branch policy, the Dependabot config location, and the module list used to assign ownership). Module-level `AGENTS.md` / `AGENTS.local.md` files consulted on a per-finding basis during mitigation PRs.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*
*Source: `.specify/memory/constitution.md` (v2.1.0)*

- [x] **I. Module-First Boundaries**: Every mitigation PR owns to exactly one module per `./AGENTS.md`; cross-cutting files (install.xml, distribution tree) get primary + secondary owners. Rule Discovery Protocol applied per-module at PR time.
- [x] **II. Evidence Over Invention**: Reuses existing scanner (`codeql.yml`), existing fetch script (`scripts/fetch-gh-code-scanning-alerts.sh`), existing artifact location (`docs/ai-generated/tasks/gh-codeql-alerts/`), and CodeQL-native suppression syntax. No invented APIs or extension points.
- [x] **III. Test Discipline (NON-NEGOTIABLE)**: Every Valid disposition requires a regression test (fail-on-pre-fix, pass-on-post-fix) verified by commit hash in PR body. Removal and suppression dispositions require build + module-test re-runs. Task list (in `/speckit.tasks`) will enumerate test tasks per user story.
- [x] **IV. Contract & Integration Integrity**: Public REST (`rest/`), SOAP (`modules/webservices`), package (`.ppkg`) impacts assessed per-finding. Removal PRs that touch packaged artifacts MUST update the distribution tree + verify the rebuilt archive listing. No backward-incompatible contract changes proposed at the spec level; per-finding contract review is part of the per-PR workflow.
- [x] **V. Safe Modernization**: No drive-by refactoring. Scope is strictly: remove obsolete code, fix real findings, suppress false positives, document accepted risks. No new frameworks, no Spring Boot, no parallel architectures.
- [x] **VI. Security by Default**: The feature IS a security-remediation drive; every mitigation closes a real CVE-class risk. Dependency upgrades preferred for CVE fixes; Dependabot exclusion additions (if required) are recorded in `.github/dependabot.yml` with justification (Constitution VI). Shared security modules (`modules/perc-security-utils`, `modules/perc-xml-security`) are the preferred reuse path for any new safe-API introduced during mitigation.
- [x] **VII. Build, Platform & Dependency Hygiene**: JDK 21 via `./mvn-env.sh`; no new Maven or npm deps; existing Spotless / module formatting rules apply to any edited source. Generated scripts live under `./scripts` (or already do). PR workflow per `./AGENTS.md`.
- [x] **VIII. Documentation & Operability**: New markdown artifacts (`triage.md`, `suppressions.md`, `accepted-risks.md`, `release-readiness-8.2.md`) live alongside the existing `alerts.md` and inherit the existing `README.md` in that directory. Any per-module fixes update that module's nearest durable docs (README, Javadoc) per the module's existing convention.
- [x] **IX. PR Review Comment Resolution (NON-NEGOTIABLE)**: Every closing PR follows the procedure in `./AGENTS.md` "PR Review Comment Resolution" — inline reply citing the commit hash AND `resolveReviewThread` GraphQL mutation. This is called out explicitly in [contracts/C5](./contracts/README.md#c5-pr-closing-comment) and in the [quickstart.md](./quickstart.md) closing checklist (SC-007). **Per-PR enforcement (added 2026-07-11 per analyze C1)**: tasks.md T078b adds a per-PR pre-merge gate with `Review-resolution-gate: passed` checkbox in the PR template + `scripts/verify-pr-review-resolution.sh` validation — the gate is enforced on every closing PR, not only at Phase 7 audit (T078).
- [x] **III. Test Discipline (NON-NEGOTIABLE) — removal extensions**: Per analyze C2, the removal work itself (tasks.md T021–T031) is a behavioral change under Constitution III. T019b requires a pre-removal baseline capture (`./mvn-env.sh -pl <module> -am test` GREEN on the pre-removal commit) so the post-removal test run completes the fail-then-pass loop. Each removal PR cites the baseline log path in its PR body per contracts/C5.
- [x] **Complexity Budget**: No constitution violations to justify. The feature deliberately reuses existing infrastructure rather than introducing new modules or frameworks.

## Project Structure

### Documentation (this feature)

```text
specs/004-zero-code-scanning-alerts/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output — reuses existing infra decisions
├── data-model.md        # Phase 1 output — Alert / Disposition / SuppressionRecord / AcceptedRisk
├── quickstart.md        # Phase 1 output — end-to-end validation guide
├── contracts/           # Phase 1 output — triage / suppression / accepted-risk / PR contracts
│   └── README.md
├── checklists/
│   └── requirements.md  # Spec quality checklist (created by /speckit.specify)
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created here)
```

### Feature artifacts (in-repo, produced by the work)

```text
docs/ai-generated/tasks/gh-codeql-alerts/
├── alerts.md              # Already exists — produced by scripts/fetch-gh-code-scanning-alerts.sh
├── README.md              # Already exists — directory docs
├── triage.md              # NEW — one row per open alert (contracts/C1)
├── suppressions.md        # NEW — index of inline suppressions (contracts/C3)
├── accepted-risks.md      # NEW — accepted-risk register (contracts/C4)
└── release-readiness-8.2.md  # NEW — release sign-off report (contracts/C6)
```

### Source Code (repository modules touched)

No new source code is introduced by this feature in aggregate. Per-disposition PRs touch the existing source tree of the module that owns the flagged file. Examples of modules likely to be touched:

```text
# Possible per-PR module surfaces (each one a separate PR, owner = module):
system/                                       # Core CMS — many findings likely here
rest/                                         # Public REST API
projects/sitemanage/                          # UI backend
WebUI/                                        # UI front end (JS/TS)
deliverytiersuite/delivery-tier-suite/        # DTS services
modules/perc-ant/                             # Installer / upgrade script (often packaging surface)
modules/perc-distribution-tree/               # Distribution tree (often packaging surface)
modules/perc-packages/                        # Package assembly (often packaging surface)
modules/perc-security-utils/                  # Shared security helpers (preferred reuse target)
modules/perc-xml-security/                    # Shared XML security helpers (preferred reuse target)
modules/utils/                                # Shared utilities (preferred reuse target)
```

```text
# Configuration surface (one PR per change, owner = the file's module):
.github/codeql/codeql-config.yml              # paths-ignore / query-filter additions for false positives
.github/dependabot.yml                        # Justified exclusion entries for blocked CVE bumps
```

**Structure Decision**: The triage/coordination layer is centralized at the repo root (this feature's process). The mitigation layer is decentralized — exactly one PR per finding (or per cluster of findings owned by the same module), with the module listed in `./AGENTS.md` as the owner. No new Maven module is created; no cross-cutting refactor is introduced.

## Complexity Tracking

> **No constitution violations to justify.** The feature deliberately reuses existing scanner infrastructure, the existing fetch script, the existing artifact directory, and CodeQL-native suppression syntax. No new dependencies, no new frameworks, no parallel architectures, no drive-by refactors.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| (none)    | —          | —                                    |

