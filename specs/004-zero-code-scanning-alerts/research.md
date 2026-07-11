# Research: Zero Open Code Scanning Alerts for 8.2 Release

**Branch**: `004-zero-code-scanning-alerts` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)

## Decisions

### D1: Reuse the existing fetch script (`scripts/fetch-gh-code-scanning-alerts.sh`) as the canonical data source for triage.

- **Rationale**: The script exists and queries the correct GitHub REST endpoint. **However**, the original implementation used flat field names (`.rule_id`, `.rule_severity`, `.state`) that the GitHub API does not return — those are nested under `.rule.*` and at the top level. Fixed 2026-07-11 by switching to `.rule.id`, `.rule.security_severity_level`, and adding `most_recent_instance.location.path` + `start_line` so the output is actionable for triage.
- **Alternatives considered**:
  - Use the GitHub Security tab UI directly. Rejected because the output is not committable and cannot be diffed in PR review.
  - Add a new `scripts/fetch-alerts-*.sh`. Rejected — duplication.
  - Use a third-party tool (e.g., `gh-codesearch`, `osv-scanner`). Rejected — adds a dependency for no functional gain over the existing GitHub-native scanner already wired via `.github/workflows/codeql.yml`.

### D2: Adopt a single triage spreadsheet (Markdown table) as the authoritative inventory, with one row per alert.

- **Rationale**: Markdown tables diff cleanly in PRs, are reviewable in plain GitHub UI, and can be sorted/grouped by any field without a database. The project already uses this pattern in `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md`.
- **Alternatives considered**:
  - CSV file under `docs/`. Rejected — loses formatting in GitHub UI and harder to review in PR.
  - GitHub Project board. Rejected — not committable, not searchable from the repo, and lives outside the source of truth.
  - Spreadsheet (Google Sheets / Excel). Rejected — external to the repo.

### D3: Suppression mechanism = per-file `// codeql[rule-id] disable-next-line …` comments AND a query filter file (`.github/codeql/codeql-config.yml` additions) for file-level exclusions.

- **Rationale**: CodeQL Advanced supports both inline suppression comments (`// codeql[java/cleartext-log-injection]`) for path-precise exclusions, and `paths-ignore` / `query-filters` in the config for build/artifact/vendor directories. Using both lets each false-positive get the narrowest possible suppression, minimizing future maintenance. Inline comments must cite the justification string inline (per FR-004 / SC-005). The existing `.github/codeql/codeql-config.yml` already declares `paths-ignore` and is the natural extension point.
- **Alternatives considered**:
  - Global suppression via a single SARIF `notApplicable` upload. Rejected — clobbers all alerts, not just the ones we want, and obscures the justification.
  - `# noqa`-style generic comments. Rejected — CodeQL does not honor them; would silently fail to suppress.

### D4: Dependency-version CVE fixes are handled by direct version bumps in the owning module's `pom.xml`, with a tracking note in the Dependabot exclusion list (`.github/dependabot.yml`) only when the version bump is blocked.

- **Rationale**: Constitution principle VI ("Dependency upgrades that fix CVEs are preferred") and principle VII ("Java dependencies are managed in Maven POMs"). The existing `.github/dependabot.yml` already contains a long list of exclusions gated on the `development-8.1.x` target branch with justification comments — adding to this list is the established pattern when a CVE fix would force a JDK-breaking bump.
- **Alternatives considered**:
  - Repackage the affected library as an in-tree fork. Rejected — adds long-term maintenance burden without proportionate value for a security fix.
  - Switch to a different library. Rejected — out of scope for a remediation pass; would be its own feature.

### D5: Removal of obsolete code reuses the existing PR + CodeQL re-scan loop. Verification is a fresh scan of the `8.2` branch after the PR is merged.

- **Rationale**: `.github/workflows/codeql.yml` already runs on `push` to `development` and on a weekly cron. Removal work lands via PR; the merged result automatically produces a new scan whose results feed the next iteration of `alerts.md`. No new CI is required.
- **Alternatives considered**:
  - Add a per-PR CodeQL run (currently commented out in `codeql.yml`). Rejected for this feature — weekly + push-to-development cadence is sufficient for a remediation drive, and per-PR scans would slow every unrelated PR. May be revisited as a separate hardening feature.

### D6: Triage / coordination scripts live under `./scripts` and the inventory lives under `./docs/ai-generated/tasks/gh-codeql-alerts/`.

- **Rationale**: Constitution principle II ("Generated scripts MUST live under `./scripts` … Scratch work uses `./tmp`") and principle VIII (Documentation & Operability). The folder is already established for exactly this purpose (see `gh-codeql-alerts/README.md`); extending it is cheaper than creating a new top-level location.

### D7: Module ownership follows the file path → module mapping declared in `./AGENTS.md`. Cross-module files (e.g., shared `install.xml` or distribution ANT scripts) are assigned to the primary owner named there and listed secondarily.

- **Rationale**: Constitution principle I (Module-First Boundaries) and the Rule Discovery Protocol in `./AGENTS.md`. Cross-cutting files like `modules/perc-ant/install.xml` and `modules/perc-distribution-tree/*` are explicitly listed there; the spec's "Module Owner" entity already reflects this rule.

### D8: "Accepted-risk" findings get a documented entry in a new `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md` file with owner, rationale, and target milestone.

- **Rationale**: An accepted risk needs to be visible to operators and auditors at any future point. Putting it in a versioned markdown file inside the repo satisfies the docs-in-tree principle and is searchable from the scanner dashboard's "linked files" column.

## Unknowns Resolved

- **Scanner identity**: Confirmed via `.github/workflows/codeql.yml` — CodeQL Advanced running on push to `development` and weekly cron for `actions`, `java-kotlin`, `javascript-typescript`. Dependabot weekly for Maven (gated on `development-8.1.x`).
- **Existing alert fetch tool**: `scripts/fetch-gh-code-scanning-alerts.sh` + `docs/ai-generated/tasks/gh-codeql-alerts/` (now fixed; output is properly populated with rule/severity/path/line).
- **Suppression mechanism**: Inline `// codeql[rule-id]` comments + `paths-ignore` / query-filter extensions in `.github/codeql/codeql-config.yml`.
- **Distribution artifact impact**: `modules/perc-distribution-tree`, `modules/perc-ant`, `modules/perc-packages` are the install/upgrade surface; removal tasks MUST update these so obsolete files are not re-shipped.
- **Module ownership rule**: Per `./AGENTS.md` module list; cross-module files → primary owner named there + secondary listed for packaging impact.
- **Test framework**: JUnit 5 + Mockito per `./AGENTS.md` (also Constitution III).

## Initial enumeration (seeded 2026-07-11)

After fixing the fetch script, the open-alert state of the `development` branch was enumerated. Full table is in `docs/ai-generated/tasks/gh-codeql-alerts/triage.md` (866 rows) with raw fetch in `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md`.

- **866 open alerts, 38 distinct rules, 338 distinct files.**
- **Severity**: 13 critical, 535 high, 318 medium.
- **Language split**: ~650 `js/*` (WebUI / JSP), ~215 `java/*` (server-side).
- **Long-tail concentration**: the top 10 rules account for 742 of 866 alerts (~86%).
- **Top concentrations by module_owner**:
  - `WebUI/` — 498 alerts (387 obsolete vendored JS libs; 111 valid in custom UI code).
  - `system/` — 133 alerts (67 obsolete `ApplicationFiles/` JS, 66 valid Java).
  - `projects/sitemanage/` — 128 alerts (100 valid Java, 28 obsolete `src/test` samples).
  - `modules/perc-packages/` — 35 valid Java.
  - `deliverytiersuite/.../p13n-ds/` — 14 valid + 3 obsolete JS.

This data drives the candidate dispositions seeded in `triage.md` and the initial milestone assignments (`8.2-blocker` for critical/valid Java security findings; `8.2-must-fix` for high/medium). Final dispositions and milestone assignments are the module owner's call per FR-001.

## Best-Practice Notes

- **Inline suppression justification text**: Per CodeQL docs, the inline suppression comment should reference the rule ID and a brief justification. The justification must be sufficient for an independent reviewer to verify without re-reading history (satisfies FR-004 / SC-005).
- **Re-scanning after removal/fix**: CodeQL runs on `push` to `development`. After a removal PR merges, the next push (or the weekly cron) re-scans. For a tight remediation loop, run the script locally (`scripts/fetch-gh-code-scanning-alerts.sh`) to confirm the alert closed without waiting for CI.
- **Dependabot vs direct CVE bump**: If the CVE is in a Maven dep that Dependabot already covers, prefer letting Dependabot open the PR; if Dependabot is excluded for that coordinate, do the bump directly in `pom.xml`.
- **Stale suppressions**: A suppression is "stale" when the underlying code path or the scanner rule has changed enough that the justification no longer applies. Constitution principle VI plus the spec's FR-007 require these to be re-reviewed at release sign-off.
- **Cross-module artifacts**: When a removal affects `modules/perc-distribution-tree` or `modules/perc-ant` (packaging), rebuild the installer locally with `./mvn-env.sh -pl modules/perc-distribution-tree -am package` and inspect the produced `.ppkg` archive listing to confirm the removed file is absent.
