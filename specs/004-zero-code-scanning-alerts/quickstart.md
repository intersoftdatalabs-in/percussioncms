# Quickstart: Zero Open Code Scanning Alerts for 8.2 Release

**Branch**: `004-zero-code-scanning-alerts` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)

This is the end-to-end validation guide for the feature. It walks a release/security engineer through the canonical sequence of commands and checks that prove the feature works: triage → removal/mitigation/suppression → re-scan → release-readiness report.

For the data shape of every artifact produced, see [data-model.md](./data-model.md). For the exact column and comment formats that must be used, see [contracts/README.md](./contracts/README.md).

---

## Prerequisites

- Repository working tree clean, on the `8.2` release branch (per `./AGENTS.md`).
- JDK 21 active via `./mvnw` (per Constitution VII).
- GitHub CLI (`gh`) authenticated (`gh auth login`) with `security_events:read` scope for the target repository.
- `jq` installed (used by the fetch script).
- Permissions: ability to push branches and open PRs against the target repo.

---

## Phase 1 — Triage

### 1.1 Fetch the current open alerts

```bash
scripts/fetch-gh-code-scanning-alerts.sh percussion/percussioncms
```

Expected output:

- `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` is updated with the current alert list.
- The script exits 0; non-zero means auth or network failure (see script output for specifics).

### 1.2 Author the triage inventory

Create `docs/ai-generated/tasks/gh-codeql-alerts/triage.md` with one row per open alert, using the columns from [contracts/C1](./contracts/README.md#c1-triage-inventory). Sort by severity (critical → note) then module_owner.

### 1.3 Validate the triage inventory

- Row count == number of open alerts in `alerts.md` (no orphans, no duplicates).
- Every `false-positive` and `accepted-risk` row has a non-empty `notes` column.
- Every `module_owner` is a path listed in `./AGENTS.md`.

**Pass condition**: the file satisfies all checks in [contracts/C1](./contracts/README.md#c1-triage-inventory).

---

## Phase 2 — Per-Disposition Closure

Each row in `triage.md` is closed via exactly one of four work flows, depending on its `disposition`. The number of distinct PRs is equal to the number of distinct module/owner groups; do not collapse unrelated fixes into a single PR.

### 2A — Obsolete (delete files)

For each `disposition == obsolete` row:

```bash
# Confirm the file is truly unreferenced
grep -RIn --exclude-dir=target --exclude-dir=build --exclude-dir=node_modules \
  "<file_path-basename>" . | head -50

# Delete the file
git rm <file_path>

# If the file was bundled in a distribution JAR / .ppkg / install tree,
# also update the owning packaging descriptor (modules/perc-ant/install.xml,
# modules/perc-distribution-tree, or module-level pom <resources>).
# Rebuild and verify the artifact does NOT contain the file:
./mvnw -pl modules/perc-distribution-tree -am clean package
unzip -l modules/perc-distribution-tree/target/*.jar | grep <file_path-basename> || echo "OK: absent"

# Run the owning module's tests
./mvnw -pl <module_owner> -am test
```

PR body MUST follow [contracts/C5](./contracts/README.md#c5-pr-closing-comment).

**Pass condition**: re-running `scripts/fetch-gh-code-scanning-alerts.sh` no longer reports the alert, the module's test suite passes, and the rebuilt distribution archive does not contain the deleted file.

### 2B — Valid (mitigate)

For each `disposition == valid` row:

1. Implement the smallest fix that closes the finding (upgrade dep, replace unsafe API, validate/sanitize input, move secret to config).
2. Add or update a regression test that demonstrably fails on the pre-fix code (record the commit hash of the pre-fix test run in the PR body for reviewer verification).
3. Run the module's full test suite:

```bash
./mvnw -pl <module_owner> -am test
```

4. If the fix is a Maven dependency upgrade and the upgrade exceeds a Dependabot exclusion, append a justified entry to `.github/dependabot.yml` (see Constitution VI).

PR body MUST follow [contracts/C5](./contracts/README.md#c5-pr-closing-comment) and cite the regression test.

**Pass condition**: re-scan no longer reports the alert, and the regression test fails on the pre-fix commit and passes on the post-fix commit.

### 2C — False-positive (suppress)

For each `disposition == false-positive` row:

1. Add an inline `// codeql[rule-id]` comment immediately above the flagged construct, with a `justification:` segment that references a concrete code path, guard, or config key (per [contracts/C2](./contracts/README.md#c2-suppression-entry)).
2. Append a row to `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md` (per [contracts/C3](./contracts/README.md#c3-suppression-index)).
3. For path-level exclusions (e.g., a vendored script flagged wholesale), extend `.github/codeql/codeql-config.yml` `paths-ignore` and add the corresponding row in `suppressions.md` with `file_path = .github/codeql/codeql-config.yml`.

**Pass condition**: re-scan does not re-open the alert, the inline comment matches the index row verbatim, and the justification can be located by another reviewer without re-reading history.

### 2D — Accepted-risk (document only)

For each `disposition == accepted-risk` row:

1. Append a row to `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md` (per [contracts/C4](./contracts/README.md#c4-accepted-risk-record)).
2. Do not silence the scanner — the alert remains open. The release notes MUST cite the accepted-risk by alert ID.

**Pass condition**: the row is present, all required columns are non-empty, and the alert is listed by name in the `8.2` release notes.

---

## Phase 3 — Re-scan and Verification

After all per-disposition PRs are merged:

```bash
# Trigger a fresh CodeQL scan (CodeQL Advanced runs on push to development; the merge IS the trigger).
# Wait for the workflow to finish:
gh run watch --workflow=codeql.yml

# Re-fetch the alerts
scripts/fetch-gh-code-scanning-alerts.sh percussion/percussioncms
```

Expected: the count of open alerts equals the count of `accepted-risk` rows in `triage.md` (i.e., zero, or only the accepted-risks).

---

## Phase 4 — Release-Readiness Report

Author `docs/ai-generated/tasks/gh-codeql-alerts/release-readiness-8.2.md` per [contracts/C6](./contracts/README.md#c6-release-readiness-report--docsai-generatedtasksgh-codeql-alertsrelease-readiness-82md) with:

1. Total open alerts (should equal the number of `accepted-risk` rows).
2. Counts by disposition.
3. Counts by severity.
4. The list of accepted risks.
5. Pass/fail decision: `PASS` if zero open alerts; `PASS-WITH-EXCEPTIONS` if all remaining alerts are accepted-risks; otherwise `FAIL`.

Attach the report to the `8.2` release and reference it from the release notes.

---

## End-to-End Pass Conditions

The feature is complete when **all** of the following are true:

- [ ] `triage.md` row count for non-`accepted-risk` rows == 0 (or each remaining row has a merged closing PR linked).
- [ ] CodeQL re-scan reports `0 active code-scanning alerts` not on the accepted-risk list.
- [ ] Every closing PR resolved all of its review threads (Constitution IX).
- [ ] `release-readiness-8.2.md` decision is `PASS` or `PASS-WITH-EXCEPTIONS` and is referenced in the `8.2` release notes.
- [ ] No suppression in `suppressions.md` is older than one release cycle (per FR-007) without a `stale-suppression` note and a re-review date.

