# Contracts: Zero Open Code Scanning Alerts for 8.2 Release

**Branch**: `004-zero-code-scanning-alerts` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)

This feature does not introduce new public APIs or external interfaces. Its "contracts" are the repo-internal file formats and review conventions the triage, mitigation, and PR closure work must follow. Anyone (human or agent) working on this feature — or auditing it later — must be able to read these contracts and produce/consume artifacts in the exact shapes described.

## C1: Triage Inventory — `docs/ai-generated/tasks/gh-codeql-alerts/triage.md`

Every open code-scanning alert on the `8.2` branch MUST appear as exactly one row in this file with the columns below, in this order. The file is the single source of truth for what has been triaged and what is still un-triaged.

### Required columns

| #  |       Column       |    Type    |                                       Notes                                        |
|----|--------------------|------------|------------------------------------------------------------------------------------|
| 1  | `alert_id`         | integer    | GitHub alert number; matches `Alert.alert_id` in [data-model.md](../data-model.md) |
| 2  | `rule_id`          | string     | CodeQL rule (e.g., `java/sql-injection`)                                           |
| 3  | `severity`         | enum       | `critical` / `high` / `medium` / `low` / `note`                                    |
| 4  | `file_path`        | path       | Repo-relative path to the flagged file                                             |
| 5  | `module_owner`     | path       | One of the modules listed in `./AGENTS.md`                                         |
| 6  | `disposition`      | enum       | `obsolete` / `valid` / `false-positive` / `accepted-risk`                          |
| 7  | `target_action`    | string     | One short sentence describing the concrete fix                                     |
| 8  | `target_milestone` | enum       | `8.2-blocker` / `8.2-must-fix` / `8.2-backlog` / `accepted-risk`                   |
| 9  | `linked_pr`        | int \| "—" | PR number that closes the alert, or `—` while pending                              |
| 10 | `notes`            | string     | Free-form; required non-empty for `false-positive` and `accepted-risk` rows        |

### Validation rules (enforced by review)

- Header row MUST be exactly the column names above, in the given order.
- Every open alert appears exactly once. The total row count MUST equal the GitHub CodeQL dashboard's count of open alerts for `development` at sign-off (per FR-009 / SC-001).
- Every row's `module_owner` MUST be a path listed in `./AGENTS.md`.
- Every `false-positive` and `accepted-risk` row MUST have non-empty `notes`.
- Sorting: rows MUST be sorted by `severity` (critical → note) then by `module_owner` so the table reads as a release-readiness checklist.

## C2: Suppression Entry — Inline `// codeql[rule-id]` Comment

For a `Disposition` of `false-positive` where the suppression must be scoped to a single code line or block, the suppression MUST be applied as a CodeQL inline suppression comment in the source file.

### Required form

```
// codeql[java/rule-id] justification: <one-sentence reason a reviewer can verify>
<flagged line of code, possibly preceded by a single-line block scope>
```

or for a multi-line block:

```
// codeql[java/rule-id] justification: <reason> -- scope: lines N..M
<line N>
<line N+1>
...
<line M>
```

### Validation rules (enforced by review)

- The rule ID in the comment MUST exactly match the CodeQL rule that flagged the finding (verify against `Alert.rule_id`).
- The `justification:` segment MUST be present and MUST contain a concrete reference: a code path, a guard, a config key, a CVE control reference, or a documentation URL — not generic phrases like "false positive" or "safe code".
- The comment MUST be on the line immediately above (or in the same line as) the suppressed construct, per CodeQL inline-suppression syntax.
- The justification text in the comment MUST match the justification in the corresponding `SuppressionRecord` (see C3) so a reader of either side finds the same reason.

## C3: Suppression Index — `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md`

A machine-greppable index of every suppression applied under this feature. Required so a future release engineer can audit suppressions without grepping the entire codebase.

### Required columns

| # |     Column      |  Type   |                               Notes                                |
|---|-----------------|---------|--------------------------------------------------------------------|
| 1 | `alert_id`      | integer | GitHub alert number                                                |
| 2 | `rule_id`       | string  | CodeQL rule                                                        |
| 3 | `file_path`     | path    | File containing the inline comment                                 |
| 4 | `line`          | integer | Line number of the comment                                         |
| 5 | `justification` | string  | Verbatim copy of the justification segment from the inline comment |
| 6 | `applied_on`    | date    | ISO `YYYY-MM-DD`                                                   |
| 7 | `applied_by`    | handle  | GitHub handle of the reviewer who authorized the suppression       |
| 8 | `review_by`     | date    | ISO `YYYY-MM-DD`; latest acceptable re-review date                 |

### Validation rules (enforced by review)

- One row per inline suppression; one row per `paths-ignore` / `query-filter` entry in `.github/codeql/codeql-config.yml` introduced by this feature.
- `justification` MUST equal the comment in the source file (enforces C2 ↔ C3 consistency).
- `review_by` defaults to the date of the next release cut after `applied_on`; suppressions older than one release MUST be flagged `stale` per FR-007.

## C4: Accepted-Risk Record — `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md`

Required for every `Disposition` of `accepted-risk`.

### Required columns

| # |         Column         |  Type   |                                   Notes                                    |
|---|------------------------|---------|----------------------------------------------------------------------------|
| 1 | `alert_id`             | integer | GitHub alert number                                                        |
| 2 | `rule_id`              | string  | CodeQL rule                                                                |
| 3 | `file_path`            | path    | Flagged file                                                               |
| 4 | `rationale`            | string  | Why the finding cannot be fixed in this release                            |
| 5 | `compensating_control` | string  | Existing control reducing residual risk, or `"none"` with justification    |
| 6 | `owner`                | handle  | GitHub handle of the accountable individual                                |
| 7 | `target_milestone`     | string  | Future release in which remediation is committed                           |
| 8 | `expires_at`           | date    | ISO `YYYY-MM-DD`; hard date by which the accepted-risk MUST be re-reviewed |

### Validation rules (enforced by review)

- `rationale`, `compensating_control`, `owner`, `target_milestone`, and `expires_at` MUST all be non-empty.
- `expires_at` MUST be ≤ one release cycle after `applied_on`.
- Every accepted-risk row MUST be cited by name in the `8.2` release notes so operators and auditors can find it from outside the repo.

## C5: PR Closing Comment

Every PR that closes one or more alert IDs MUST include a closing comment block in the PR body (or in the final commit message) listing each alert ID it closes.

### Required form (in the PR body)

```
Closes: #<alert-id-1>, #<alert-id-2>, ...
Disposition(s): obsolete | valid | false-positive | accepted-risk
Module(s): <module-owner-1>, <module-owner-2>
Verification:
- [ ] Scanner re-scan no longer reports the alert(s) above.
- [ ] Module test suite (`./mvnw -pl <module> test`) passes.
- [ ] If disposition == valid: regression test that fails on the pre-fix code is referenced.
- [ ] If obsolete file was previously bundled: rebuilt distribution archive listing does not contain it.
```

### Validation rules (enforced by review)

- `Closes:` MUST use GitHub's `Closes #N` keyword form OR reference the alert ID directly. The closing action MUST be performed via the GitHub UI/API so the alert state transitions to `closed`.
- The PR author MUST follow the PR review-comment resolution procedure in `./AGENTS.md` (Constitution principle IX): every review comment gets an inline reply AND the thread is resolved via the `resolveReviewThread` GraphQL mutation before the PR is merge-ready.

## C6: Release-Readiness Report — `docs/ai-generated/tasks/gh-codeql-alerts/release-readiness-8.2.md`

A short markdown report published at release sign-off, required by FR-009 / SC-006.

### Required sections

1. **Total open alerts** (int) — must match `triage.md` row count for `disposition != accepted-risk`.
2. **Counts by disposition**: a table with columns `disposition` and `count`.
3. **Counts by severity**: a table with columns `severity` and `count`, considering open + accepted-risk together.
4. **Accepted risks**: bullet list of `alert_id` + 1-line `rationale` (cross-references `accepted-risks.md`).
5. **Pass/fail decision** against the goal of `0 active code-scanning alerts for 8.2`. If any `accepted-risk` is present, the decision is recorded as `PASS-WITH-EXCEPTIONS` and lists the exceptions.

