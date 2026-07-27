# GH Code Scanning Alerts (CodeQL)

This folder holds the raw alert fetch, the triage inventory, and the per-disposition indexes used by the `004-zero-code-scanning-alerts` feature (`specs/004-zero-code-scanning-alerts/spec.md`).

> **2026-07-21 sign-off**: 0 active code-scanning alerts on `development`. See `release-readiness-8.2.md` for the per-phase closure log + accepted-risks register.

## Files

|              File               |                                                                                                                    Purpose                                                                                                                    |           Owner           |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| **`codeql-pr-playbook.md`**     | **Operational playbook** — analyzer of record, default-setup off, model pack, sink-line suppressions, disposition ladder, PR checklist. **Read this before any CodeQL/security PR work.**                                                     | Human-edited              |
| `alerts.md`                     | Raw fetch — one section per open alert, produced by `scripts/fetch-gh-code-scanning-alerts.sh`. Regenerated on every release-readiness refresh; 0 rows as of 2026-07-21.                                                                      | Generated                 |
| `triage.md`                     | Triage inventory — one row per closed alert with disposition, module owner, target action, target milestone, `linked_pr`. Retained as historical audit trail (866 rows; 192 ready-to-close + 674 historical).                                 | Human-edited (historical) |
| `triage.archived-2026-07-21.md` | Archive of the 674 historical triage rows at the spec 004 sign-off; preserved per Constitution V (no silent deletion of audit trail).                                                                                                         | Generated (preserved)     |
| `suppressions.md`               | Index of inline `// codeql[rule-id]` suppressions for `false-positive` dispositions. See `specs/004-zero-code-scanning-alerts/contracts/README.md` C3. Updated 2026-07-21 to remove runtime-fix entries for closed alerts (#796, #638, #639). | Human-edited              |
| `accepted-risks.md`             | Accepted-risk register for findings that cannot be remediated in `8.2`. 8 rows (T047/T048 PSAesCBC legacy-decrypt + T054 SimpleXmlView + T054 PSWebdavConfigValidator). See `specs/004-zero-code-scanning-alerts/contracts/README.md` C4.     | Human-edited              |
| `release-readiness-8.2.md`      | Per-release sign-off report. **PASS** as of 2026-07-21 (0 active alerts; 8 accepted-risks; SC-001 met). See `specs/004-zero-code-scanning-alerts/contracts/README.md` C6.                                                                     | Generated at sign-off     |

## Analyzer of record (do not re-enable default setup / Code Quality)

- **Workflow**: `.github/workflows/codeql.yml` (push + **pull_request** to `development` + schedule + `workflow_dispatch`)
- **Languages**: **Java** + **JavaScript/TypeScript** only
- **Config**: `.github/codeql/codeql-config.yml` (`paths-ignore`, Java `packs`, `query-filters`)
- **Models**: `.github/codeql/models/` (custom Java sanitizer barriers)
- **Default setup**: must remain `not-configured` — verify with  
  `gh api repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup --jq .state`
- **Code Quality**: must stay **disabled** (Settings → Code quality). Dynamic workflow `dynamic/github-code-scanning/codeql` ignores advanced config and can wipe default-branch alerts.
- **Gate scripts**: `scripts/verify-codeql-analyzer-of-record.sh` (asserts the policy above); `scripts/verify-suppressions.sh` (asserts every suppression row has a `// codeql[rule-id]` anchor in source); `scripts/verify-pr-review-resolution.sh` (asserts every closing PR has 0 unresolved review threads).

## Re-scan cadence

Weekly `scripts/fetch-gh-code-scanning-alerts.sh intersoftdatalabs-in/percussioncms open` followed by `scripts/verify-triage-inventory.sh`. Alerts.json and triage.md are committed under `docs/ai-generated/tasks/gh-codeql-alerts/`. Any regression (new open alert) re-opens the corresponding row in `triage.md` and triggers a new closure PR per the playbook.
