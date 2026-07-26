# Data Model — Spec 005: v8.1.7 → 8.2 Migration Audit

The audit produces four primary data artifacts. All are plain text / JSON / Markdown so they diff cleanly in a PR review.

## Entity: PRRecord

One non-dependabot PR merged into the v8.1.7 lineage.

|      Field       |      Type       |                                                                                                   Source                                                                                                    |                                                                                                                                                    Notes                                                                                                                                                     |
|------------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `number`         | int             | `gh pr view --json number`                                                                                                                                                                                  | PR number (unique key)                                                                                                                                                                                                                                                                                       |
| `title`          | string          | `gh pr view --json title`                                                                                                                                                                                   | Full title; truncated to 90 chars in inventory table                                                                                                                                                                                                                                                         |
| `author`         | string          | `gh pr view --json author.login`                                                                                                                                                                            | GitHub login; `dependabot[bot]` is excluded                                                                                                                                                                                                                                                                  |
| `mergedAt`       | ISO-8601 string | `gh pr view --json mergedAt`                                                                                                                                                                                | Merge timestamp; used for the v8.1.6 cutoff filter                                                                                                                                                                                                                                                           |
| `baseRef`        | string          | `gh pr view --json baseRefName`                                                                                                                                                                             | Always `development-8.1.x` for this audit                                                                                                                                                                                                                                                                    |
| `mergeCommitSha` | string          | `gh pr view --json mergeCommit.oid`                                                                                                                                                                         | Resolved on `development-8.1.x`                                                                                                                                                                                                                                                                              |
| `modulePaths`    | list of string  | derived from `gh api repos/.../pulls/N/files --paginate`                                                                                                                                                    | Top-level path segments only (`system`, `modules/perc-packages`, `WebUI`, `deliverytiersuite/delivery-tier-suite/<service>`, `projects`, `rest`, `deployer`, etc.). Files outside any module (`pom.xml`, `CHANGES.md`, `mvnw`, `.github/`, `docs/`) are dropped from the column but still counted in totals. |
| `dependabotFlag` | bool            | `author.login` matches `dependabot` (case-insensitive) OR label contains `dependencies`                                                                                                                     | `true` ⇒ excluded from inventory; logged for audit                                                                                                                                                                                                                                                           |
| `jdk8OnlyFlag`   | bool            | diff heuristic: scan PR diff for `sun.misc.`, `javax.ws.rs.`, `javax.persistence.`, `javax.xml.bind.`, `com.sun.`, `javax.annotation.`, etc.                                                                | `true` ⇒ likely `not-applicable` on development unless an equivalent fix is present                                                                                                                                                                                                                          |
| `securityFlag`   | bool            | diff heuristic: scan title/body/files for `CVE-`, `security`, `shiro`, `tomcat`, `jetty`, `csp`, `authentication`, `authorization`, `xss`, `csrf`, dependency version bumps in security-critical components | `true` ⇒ surfaced to top of backlog (FR-004)                                                                                                                                                                                                                                                                 |

Validation rules:
- `number` must be unique across the inventory.
- `mergedAt >= 2026-01-02` (v8.1.6 commit timestamp) OR the PR is referenced in `git log v8.1.6..v8.1.7 --merges` — disjunction is allowed because cherry-picks from `development` may not carry the exact merge timestamp.
- `dependabotFlag == true` ⇒ PR must NOT appear in inventory file (logged separately).
- `modulePaths` may be empty for meta-only PRs (release-prep, build-number bumps, etc.); in that case the PR is recorded with `modulePaths: []` and is not assigned to a module owner.

State transitions: none (PRRecord is immutable once captured).

## Entity: PRVerdict

Per-PR classification result produced by the comparison phase.

|       Field        |  Type  |                                                                                      Notes                                                                                      |
|--------------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `prNumber`         | int    | FK → PRRecord.number                                                                                                                                                            |
| `verdict`          | enum   | One of: `already-present`, `needs-migration`, `not-applicable`, `superseded`, `conflicts-with-newer-design`                                                                     |
| `evidenceCommit`   | string | Commit hash on `development` that contains the equivalent fix (omit if `not-applicable` or `conflicts-with-newer-design`)                                                       |
| `evidenceFilePath` | string | Path on `development` HEAD where the fix was verified (omit if conflict / not found)                                                                                            |
| `evidenceNote`     | string | Free-text explanation: e.g. "no `normalizePath` method in `rest/src/main/java/com/percussion/rest/pages/PagesResource.java`; all four `p.matcher(path)` calls remain unwrapped" |
| `jdk8Only`         | bool   | Mirror of PRRecord.jdk8OnlyFlag; `true` ⇒ default verdict is `not-applicable` unless an equivalent fix exists                                                                   |
| `securityFlag`     | bool   | Mirror of PRRecord.securityFlag; affects backlog ordering                                                                                                                       |

Validation rules:
- Verdict MUST be one of the five enumerated values.
- For `verdict == "already-present"` or `verdict == "superseded"`, `evidenceCommit` and `evidenceFilePath` are required.
- For `verdict == "not-applicable"`, `evidenceNote` is required and must explain why.
- For `verdict == "conflicts-with-newer-design"`, `evidenceNote` must cite the `development` commit that removed/replaced the target surface.
- For `verdict == "needs-migration"`, `evidenceNote` must state "not found at path" with the searched path.

Verdict selection rules (decision tree, applied per PR):
1. If `jdk8Only == true` AND no equivalent fix found on `development` ⇒ `not-applicable`.
2. Else if the target file path on `development` HEAD contains the same code change ⇒ `already-present` (cite commit).
3. Else if a different file/function on `development` provides equivalent functional behavior ⇒ `superseded` (cite the divergent commit).
4. Else if the target file path was deleted or wholly refactored on `development` ⇒ `conflicts-with-newer-design` (cite the deletion commit).
5. Else ⇒ `needs-migration`.

## Entity: MigrationBacklogItem

One actionable row in the migration backlog (subset of PRVerdict where verdict == `needs-migration`).

|        Field        |  Type  |                                            Notes                                            |
|---------------------|--------|---------------------------------------------------------------------------------------------|
| `prNumber`          | int    | FK → PRVerdict.prNumber                                                                     |
| `modulePath`        | string | Primary owning module from PRRecord.modulePaths (first non-empty entry wins)                |
| `title`             | string | Mirror of PRRecord.title                                                                    |
| `mergeCommitSha`    | string | v8.1.7 merge commit for cherry-pick reference                                               |
| `v817PrUrl`         | string | `https://github.com/intersoftdatalabs-in/percussioncms/pull/<N>`                            |
| `strategy`          | enum   | One of: `cherry-pick`, `back-port`, `re-implement`, `skip`                                  |
| `strategyRationale` | string | One-sentence reason for the strategy choice                                                 |
| `testCoverageIn817` | string | Notes on tests shipped in the v8.1.7 PR (per FR-009)                                        |
| `blockerNotes`      | string | Free-text, may be empty                                                                     |
| `priority`          | enum   | `P0` (security), `P1` (REST contract / publishing), `P2` (UI fix), `P3` (cosmetic / gadget) |

Validation rules:
- A backlog item exists ONLY for `verdict == "needs-migration"`; other verdicts are excluded.
- `strategy` defaults to `cherry-pick` and is promoted to `back-port` if the diff touches Java 8 idioms that need translation; `re-implement` if the dev API surface diverged; `skip` if a maintainer has triaged it out.
- Items are sorted by: `priority` ascending (P0 first), then `securityFlag` descending, then `mergedAt` descending (most recent first).

## Entity: AuditRun

One execution of the audit pipeline.

|       Field        |       Type        |                                              Notes                                              |
|--------------------|-------------------|-------------------------------------------------------------------------------------------------|
| `tagRange`         | string            | e.g. `v8.1.6..v8.1.7`; CLI argument, default per spec                                           |
| `targetBranch`     | string            | `development` by default; CLI argument                                                          |
| `runTimestamp`     | ISO-8601 string   | Generated at script start                                                                       |
| `inventoryFile`    | path              | `./tmp/release-audit/<tagRange>/inventory.json`                                                 |
| `verdictFile`      | path              | `./tmp/release-audit/<tagRange>/verdicts.json`                                                  |
| `backlogFile`      | path              | `./tmp/release-audit/<tagRange>/migration-backlog.md`                                           |
| `summaryFile`      | path              | `./tmp/release-audit/<tagRange>/v8.1.7-to-8.2-migration-report.md`                              |
| `totalPrsAnalyzed` | int               | Count of PRRecord entries in inventory file                                                     |
| `verdictCounts`    | map of enum → int | `{ already-present, needs-migration, not-applicable, superseded, conflicts-with-newer-design }` |

State transitions: none. AuditRun is the unit of re-runnability — re-running with the same tag range overwrites the four output files atomically.
