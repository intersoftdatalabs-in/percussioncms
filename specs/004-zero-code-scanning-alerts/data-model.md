# Data Model: Zero Open Code Scanning Alerts for 8.2 Release

**Branch**: `004-zero-code-scanning-alerts` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)

This feature is an organizational / remediation workflow, not new application functionality. The "data" of the feature is the inventory of open code-scanning alerts and the per-finding remediation records. This document defines the shape of those records so the triage sheet, suppression entries, and per-PR closing comments are all consistent.

## Entities

### Alert

A single code-scanning finding as reported by the GitHub CodeQL dashboard for the `8.2` branch.

|    Field     |                           Type                            |                Description / Validation                |
|--------------|-----------------------------------------------------------|--------------------------------------------------------|
| `alert_id`   | integer (GitHub alert number)                             | Immutable identifier from the scanner; primary key     |
| `rule_id`    | string                                                    | CodeQL query ID (e.g., `java/cleartext-log-injection`) |
| `severity`   | enum: `critical` \| `high` \| `medium` \| `low` \| `note` | From scanner output                                    |
| `tool`       | string (default `CodeQL`)                                 | From scanner output                                    |
| `file_path`  | path (relative to repo root)                              | Where the finding was detected                         |
| `line_range` | integer range (optional)                                  | May be omitted if rule is path-level                   |
| `message`    | string                                                    | From scanner `most_recent_instance.message.text`       |
| `url`        | URL                                                       | GitHub alert URL for traceability                      |
| `state`      | enum: `open` \| `closed` \| `suppressed`                  | From scanner, mirrored                                 |
| `created_at` | ISO-8601 timestamp                                        | From scanner                                           |

### Disposition

The categorization assigned during the triage pass. Every open `Alert` has exactly one `Disposition`; closed alerts may keep their last `Disposition` for audit.

|       Field        |                                   Type                                    |                               Description / Validation                                |
|--------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `alert_id`         | integer                                                                   | Foreign key → `Alert.alert_id`                                                        |
| `disposition`      | enum: `obsolete` \| `valid` \| `false-positive` \| `accepted-risk`        | Required; one of four values                                                          |
| `module_owner`     | path                                                                      | Module path from `./AGENTS.md` (e.g., `system/`, `rest/`, `modules/perc-ant/`)        |
| `target_action`    | string                                                                    | Concrete next step (e.g., "delete file", "upgrade x.y.z", "apply inline suppression") |
| `target_milestone` | enum: `8.2-blocker` \| `8.2-must-fix` \| `8.2-backlog` \| `accepted-risk` | Release-readiness bucket                                                              |
| `triaged_by`       | GitHub handle                                                             | Release/security engineer who classified                                              |
| `triaged_at`       | ISO-8601 timestamp                                                        | When classification was assigned                                                      |
| `linked_pr`        | integer \| null                                                           | PR that closes the alert (set when known)                                             |
| `notes`            | string                                                                    | Free-form justification (required for `false-positive` and `accepted-risk`)           |

### SuppressionRecord

A documented justification + scanner-native suppression entry for a `Disposition` of `false-positive`.

|      Field      |     Type      |                                    Description / Validation                                     |
|-----------------|---------------|-------------------------------------------------------------------------------------------------|
| `alert_id`      | integer       | Foreign key → `Alert.alert_id`                                                                  |
| `rule_id`       | string        | CodeQL rule being suppressed                                                                    |
| `file_path`     | path          | File containing the inline comment (or config file for path-level filter)                       |
| `line`          | integer       | Line number of the inline suppression comment                                                   |
| `justification` | string        | Concise reason another reviewer can verify (must reference a code path, control, or config key) |
| `applied_on`    | ISO-8601 date | Date the suppression was applied                                                                |
| `review_by`     | ISO-8601 date | Latest acceptable review date (= `applied_on` + 1 release cycle)                                |
| `applied_by`    | GitHub handle | Reviewer who authorized the suppression                                                         |

### AcceptedRisk

A documented exception for an `Alert` that cannot be mitigated before `8.2` but is consciously accepted rather than ignored.

|         Field          |     Type      |                           Description / Validation                           |
|------------------------|---------------|------------------------------------------------------------------------------|
| `alert_id`             | integer       | Foreign key → `Alert.alert_id`                                               |
| `rationale`            | string        | Why the finding cannot be fixed in this release (e.g., requires JDK upgrade) |
| `compensating_control` | string        | Existing control that reduces residual risk (or "none" with justification)   |
| `owner`                | GitHub handle | Named individual accountable                                                 |
| `target_milestone`     | string        | When remediation is committed to happen (e.g., `8.3`, `9.0`)                 |
| `expires_at`           | ISO-8601 date | Hard date by which the accepted-risk MUST be re-reviewed                     |

## Relationships

```
Alert (1) ── (0..1) Disposition ── (0..1) SuppressionRecord   [if disposition == false-positive]
Alert (1) ── (0..1) Disposition ── (0..1) AcceptedRisk       [if disposition == accepted-risk]
Alert (1) ── (0..*) PR                                        [closing PRs; usually one]
```

## Storage / Artifact Locations

The entities above do not live in a database. They are materialized as:

|       Entity        |                                                Primary location                                                 |                                Format                                |
|---------------------|-----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `Alert` (raw)       | `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md`                                                            | Markdown list produced by `scripts/fetch-gh-code-scanning-alerts.sh` |
| `Disposition`       | `docs/ai-generated/tasks/gh-codeql-alerts/triage.md`                                                            | Markdown table — one row per open alert                              |
| `SuppressionRecord` | (a) inline comment in the suppressed file; (b) `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md` index | (a) `// codeql[rule-id]` comment; (b) Markdown row                   |
| `AcceptedRisk`      | `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md`                                                    | Markdown table                                                       |
| `PR` closing record | GitHub PR description + linked closing commit                                                                   | PR body citing the alert ID                                          |

## Validation Rules

- An `Alert` with `state == open` MUST have a `Disposition` row in `triage.md`.
- A `Disposition` of `false-positive` MUST have a `SuppressionRecord` whose `justification` field is non-empty and references either a code line or a config key.
- A `Disposition` of `accepted-risk` MUST have an `AcceptedRisk` row whose `owner`, `target_milestone`, and `expires_at` are all non-empty.
- A `SuppressionRecord` with `applied_on` older than the prior release cut date MUST be flagged in `triage.md` with a `stale-suppression` note (per FR-007 / SC-007).
- A `Disposition.module_owner` MUST be one of the module paths declared in `./AGENTS.md`; an unrecognized owner is a triage error and must be re-assigned.

## State Transitions

```
[un-triaged open alert]
        │  (triage pass)
        ▼
   (Disposition set) ─────────────────────┐
        │ disposition = obsolete          │ disposition = valid
        ▼                                 ▼
  [PR: delete file]                [PR: fix + regression test]
        │                                 │
        ▼                                 ▼
   [Alert closed by scanner]        [Alert closed by scanner]
        │
        ▼
  (state == closed; row archived from triage.md, retained in audit log)

   (Disposition) ── disposition = false-positive ──▶ (SuppressionRecord applied)
        │                                                 │
        ▼                                                 ▼
   [Alert suppressed]                              [Alert closed / suppressed in scan]
        │
        ▼
   (state == closed; suppression tracked in suppressions.md; review-by date honored)

   (Disposition) ── disposition = accepted-risk ──▶ (AcceptedRisk row created)
        │                                                 │
        ▼                                                 ▼
   [Alert remains open, excluded by name in release notes]
                                                       │
                                                       ▼
                                              (expires_at reached → re-triage)
```

