# Quickstart — v8.1.7 → 8.2 Migration Audit

This is the validation guide for the audit pipeline. It does not include implementation code; it documents the runnable scenarios that prove the audit (and the porting follow-on) work end-to-end.

## Prerequisites

- Repository checked out on the `development` branch (JDK 21).
- `gh` CLI authenticated: `gh auth status` returns `Logged in to github.com`.
- `git` has `origin` reachable: `git ls-remote origin v8.1.7 v8.1.6` returns commit SHAs.
- Java 21 on PATH (per `./mvn-env.sh`).
- Working directory: `/home/nate/workspaces/intersoft-workspace/percussioncms`.

The audit script lives at `./scripts/release-audit/release-audit.sh` once promoted; during development it lives at `./tmp/release-audit/release-audit.sh`. It writes outputs under `./tmp/release-audit/v8.1.6..v8.1.7/`.

## Scenario 1 — First-run audit (the canonical run)

**Goal**: Produce the inventory, verdicts, backlog, and summary for `v8.1.6..v8.1.7` against `development`.

**Run**:

```bash
./scripts/release-audit/release-audit.sh \
  --from-tag v8.1.6 --to-tag v8.1.7 \
  --target-branch development \
  --output-dir ./tmp/release-audit/v8.1.6..v8.1.7
```

**Expected outcomes**:
- Exit code `0`.
- File `./tmp/release-audit/v8.1.6..v8.1.7/inventory.json` exists; contains 141 PRRecord entries.
- File `./tmp/release-audit/v8.1.6..v8.1.7/dependabot-excluded.json` exists; contains 229 PRs, all with `author` containing `dependabot`.
- File `./tmp/release-audit/v8.1.6..v8.1.7/verdicts.json` exists; contains 141 PRVerdict entries; every entry has a non-empty `evidenceNote`.
- File `./tmp/release-audit/v8.1.6..v8.1.7/migration-backlog.md` exists; backlog length ≤ 141 (only `needs-migration` rows).
- File `./tmp/release-audit/v8.1.6..v8.1.7/v8.1.7-to-8.2-migration-report.md` exists; reviewable in <10 min.

**Validation checks** (run after the script):

```bash
# SC-001: 100% of non-dependabot PRs inventoried
test "$(jq 'length' tmp/release-audit/v8.1.6..v8.1.7/inventory.json)" = "141"

# SC-002: 0 dependabot PRs in inventory
test "$(jq '[.[] | select(.author | test("dependabot"; "i"))] | length' tmp/release-audit/v8.1.6..v8.1.7/inventory.json)" = "0"

# SC-003: 100% of inventoried PRs have a verdict
test "$(jq 'length' tmp/release-audit/v8.1.6..v8.1.7/verdicts.json)" = "141"
test "$(jq '[.[] | select(.evidenceNote == "" or .evidenceNote == null)] | length' tmp/release-audit/v8.1.6..v8.1.7/verdicts.json)" = "0"

# SC-004: backlog contains only needs-migration; security flagged first
grep -c "^### " tmp/release-audit/v8.1.6..v8.1.7/migration-backlog.md  # > 0
```

## Scenario 2 — Per-PR spot-check (sample of 5)

**Goal**: Manually verify that verdict classifications are reproducible and evidence is concrete.

**Run** (one PR per row from the sample in `research.md`):

```bash
# PR #763 (UI, verdict: needs-migration)
gh pr view 763 --repo intersoftdatalabs-in/percussioncms --json files --jq '.files[].path'
git show development:system/cms/content/applications/sys_resources/ApplicationFiles/css/perc_decoration.css | grep -n "vspan_"
# Expect: no `!important` override for vspan_ rules → confirms verdict

# PR #827 (UI, verdict: already-present)
git grep -n "navLabel" development -- 'modules/perc-packages/src/main/resources/Packages/**/percNavBar.xml'
# Expect: at least one hit on development → confirms verdict

# PR #794 (Refactor, verdict: conflicts-with-newer-design)
git ls-tree -r --name-only development | grep "GadgetRegistry.xml"
# Expect: zero hits in source tree (only docker runtime copy) → confirms verdict

# PR #851 (Publishing, verdict: superseded)
git show development:system/services/src/com/percussion/services/pubserver/data/PSPubServer.java | grep -A2 "public.*getPublishServer"
# Expect: `Optional<String>` return type → confirms verdict

# PR #825 (Security, verdict: needs-migration)
git show development:deliverytiersuite/delivery-tier-suite/feeds/src/main/java/webapp/WEB-INF/perc-security.properties | grep -i "data: blob:"
# Expect: no hit → confirms verdict
```

**Expected outcomes**: Each spot-check confirms the verdict in `verdicts.json`. If any disagrees, the verdict entry is wrong and must be regenerated.

## Scenario 3 — Re-runnability on a different tag range

**Goal**: Confirm SC-005 — the same script works against a different tag range with no code changes.

**Setup**: This scenario is forward-looking and may not be runnable today (no v8.1.8 tag exists). Use the v8.1.5..v8.1.6 range as a substitute dry-run.

**Run**:

```bash
./scripts/release-audit/release-audit.sh \
  --from-tag v8.1.5 --to-tag v8.1.6 \
  --target-branch development
ls -la ./tmp/release-audit/v8.1.5..v8.1.6/
```

**Expected outcomes**: Same five files appear under `./tmp/release-audit/v8.1.5..v8.1.6/`. Verdict counts vary but structure matches.

## Scenario 4 — Single-item porting (representative backlog item)

**Goal**: Validate User Story 4 by porting one `needs-migration` item from the backlog to `development` with tests (Constitution Principle III).

**Setup**: Pick PR #894 from the backlog — "GH-891: Support leading `Sites/` in Page by Path REST resource". The change is contained to `rest/` and has clear tests in v8.1.7.

**Run**:

```bash
# 1. Create a feature branch from development
git switch -c 005-migrate-891-rest-leading-sites development

# 2. Cherry-pick the v8.1.7 merge commit (resolve any conflicts)
git cherry-pick -x 0a4e8cd8a2b9133089530323966cca52eda4b940

# 3. Verify the change compiles on JDK 21
./mvn-env.sh -pl rest -am compile

# 4. Run the v8.1.7 test class on JDK 21
./mvn-env.sh -pl rest test -Dtest=PagesTest

# 5. Run Spotless if the module requires it
./mvn-env.sh -pl rest spotless:check

# 6. Open a PR against `development` referencing #894 and the v8.1.7 PR
gh pr create --base development --head 005-migrate-891-rest-leading-sites \
  --title "Migrate PR #894: leading Sites/ prefix in Page by Path REST resource" \
  --body "Cherry-pick of intersoftdatalabs-in/percussioncms#894 from v8.1.7. Resolves <this-spec's-backlog-item>."
```

**Expected outcomes**:
- Cherry-pick applies cleanly (or with one trivial conflict to resolve).
- `./mvn-env.sh -pl rest test -Dtest=PagesTest` passes.
- `./mvn-env.sh -pl rest spotless:check` passes.
- The opened PR has at least one new/changed test in its diff.
- Per Constitution Principle IX, when review comments arrive, the agent replies inline AND resolves each review thread (see root `AGENTS.md` "PR Review Comment Resolution").

## Scenario 5 — Dependabot override (negative test)

**Goal**: Confirm FR-002 override works.

**Run**:

```bash
./scripts/release-audit/release-audit.sh \
  --from-tag v8.1.6 --to-tag v8.1.7 \
  --include-dependabot \
  --output-dir ./tmp/release-audit/v8.1.6..v8.1.7-with-deps
jq '[.[] | select(.dependabotFlag == true)] | length' \
  ./tmp/release-audit/v8.1.6..v8.1.7-with-deps/inventory.json
```

**Expected outcomes**: Inventory contains 370 PRs (141 + 229). Inventory entries with `dependabotFlag == true` are flagged but not excluded. The default run (without `--include-dependabot`) is unaffected.

## Where to look next

- Audit outputs and evidence: `research.md` (sample of 20 PRs already analyzed; full audit populates the four output files per Scenario 1).
- Data shapes: `data-model.md`, `contracts/audit-output-schemas.md`.
- Migration backlog (the actionable list): `migration-backlog.md` once Scenario 1 has been run end-to-end.
- Constitution compliance: see plan.md Constitution Check.
- Module ownership and per-module AGENTS lookup: required per-module at porting time (US4 step 1).

