# Plan: Address CRITICAL review comment on PR #1536 (PostgreSQL 42.7.7 → 42.7.12 lockstep update)

## Goal

Make the source-of-truth files in `modules/perc-distribution-tree` consistent with the
PostgreSQL driver version bump (`42.7.7 → 42.7.12`) that PR #1536 already applied to
`pom.xml` and `deliverytiersuite/delivery-tier-suite/pom.xml`, so the bundled-driver
verify phase passes and field upgrades purge the prior version's JAR.

## Context — what the review comment says (kilo-code-bot, CRITICAL)

PR #1536 (head: `dependabot/maven/maven-55f3996f77`, base: `development`) bumps
`org.postgresql:postgresql` from `42.7.7` to `42.7.12`. The two changed lines are
correct, but the module's documented lockstep contract
(`modules/perc-distribution-tree/README.md:178-189`) requires the bump to be
propagated to **three** places in the same commit:

1. `pom.xml` version property (done in PR)
2. `deliverytiersuite/delivery-tier-suite/pom.xml` version property (done in PR)
3. **`BundledJdbcDrivers` constants** (NOT done — `EXACT_FILENAMES` still
   has `postgresql-42.7.7.jar` at line 124; `PRIOR_FILENAMES` is missing
   `postgresql-42.7.7.jar`).
4. **`install.xml` `<delete>` block** (NOT done — line 198 still has
   `postgresql-42.7.7.jar` in the CURRENT list, and the PRIOR list does
   not contain it).

Without (3) and (4):

- `InstallXmlDeleteSetTest.deleteSetContainsAllBundledFilenames` will fail
  (it asserts the `<delete>` set equals the union of
  `BundledJdbcDrivers.EXACT_FILENAMES ∪ PRIOR_FILENAMES`).
- `VerifyJdbcDrivers` verify-phase gate will see a missing
  `postgresql-42.7.12.jar` in `jetty/base/lib/jdbc/`.
- Field upgrades from a distribution that shipped 42.7.7 will leave the
  stale `postgresql-42.7.7.jar` on the Jetty classpath (no longer in
  either the CURRENT or the PRIOR delete set).

The PR also brings in two upstream security fixes
(`CVE-2026-42198` SCRAM PBKDF2 cap; `CVE-2026-54291` SCRAM channel-binding
downgrade) — the version bump itself is desired and should stay.

## Target branch

The fix should land on the PR's own head branch
`dependabot/maven/maven-55f3996f77` so PR #1536 picks it up. The repo
`no-force-push-development` rule applies only to `origin/development`; the
dependabot branch is a regular feature branch owned by this PR, so
amending/force-pushing the dependabot branch is allowed (and is the
normal Dependabot flow). Per AGENTS.md, the implementer may also push
follow-up commits without rewriting history; either approach is fine.

## Files to change (in `modules/perc-distribution-tree`)

### A. `src/test/java/com/percussion/distribution/jdbc/BundledJdbcDrivers.java`

Move `postgresql-42.7.7.jar` from `EXACT_FILENAMES` to `PRIOR_FILENAMES` and
add the new current version to `EXACT_FILENAMES`. Preserve insertion order
and the existing comment on line 89.

In the `EXACT_FILENAMES` static block (lines 113-125), change line 124:

```java
filenames.add("postgresql-42.7.7.jar");
```

to:

```java
filenames.add("postgresql-42.7.12.jar");
```

In the `PRIOR_FILENAMES` static block (lines 133-138), add a new entry that
matches the pattern of the other entries:

```java
prior.add("postgresql-42.7.7.jar");
```

Insert it as the **last** entry in the `PRIOR_FILENAMES` set, so the file
remains in chronological (most-recent-bump-last) order. Update the
prose comment immediately above the `PRIOR_FILENAMES` set
(lines 127-132) to add `postgresql` to the list of drivers whose
version bumped in the recent history, so the comment stays accurate.

### B. `src/main/resources/distribution/rxconfig/Installer/install.xml`

Mirror the move in the `<delete>` block of the `install_jdbc_drivers`
target (lines 184-208). Change the CURRENT list entry on line 198 from:

```xml
<include name="postgresql-42.7.7.jar" />
```

to:

```xml
<include name="postgresql-42.7.12.jar" />
```

And add a matching entry to the PRIOR list (lines 199-206) as the **last**
include in that group:

```xml
<include name="postgresql-42.7.7.jar" />
```

Preserve the existing comment structure ("CURRENT (matches
BundledJdbcDrivers.EXACT_FILENAMES)" on line 186 and the PRIOR block
prose on lines 199-202). No other XML attributes change; the `<delete
failonerror="false" verbose="true">` wrapper, the fileset dir, and the
trailing `<copy>` are untouched.

No other files in the PR need to change:

- The version properties in `pom.xml` and
  `deliverytiersuite/delivery-tier-suite/pom.xml` are already bumped by
  the Dependabot commit.
- `modules/perc-distribution-tree/pom.xml` does **not** pin a postgresql
  version — it uses the parent's `${postgresql.version}` via
  `dependencyManagement` (verified at root `pom.xml:1482-1484`), so no
  edit is needed there.
- The Maven `verify` phase argument list at
  `modules/perc-distribution-tree/pom.xml:791` uses the glob
  `postgresql-*.jar`, which already matches `42.7.12`.
- `VerifyJdbcDrivers` and `CheckNoGlobDeletes` consume the constants
  and the install.xml — they do not need their own edits.
- `StagingCleanupAntScriptTest` consumes `BundledJdbcDrivers.GLOB_TO_ARTIFACT_ID`,
  which is glob-based and unaffected.

## Validation steps (pre-PR build gate)

Per the root `AGENTS.md` Pre-PR Maven verification (HARD GATE), after
editing the two files, from the repo root:

1. `cd modules/perc-distribution-tree`
2. `..\mvn-env.bat clean install` (Windows) or `../../mvn-env.sh clean install` (Unix)
3. Confirm `BUILD SUCCESS` and that
   `InstallXmlDeleteSetTest.deleteSetContainsAllBundledFilenames`,
   `deleteSetContainsNoGlobPatterns`,
   `deleteSetPreservesIntegratorFilenames`, and
   `deleteSetOmitsLegacyAndNonShippedGlobs` all pass.
4. Confirm the verify-phase `VerifyJdbcDrivers` main does not log a
   missing-driver failure for `postgresql-42.7.12.jar`.
5. Confirm zero new compiler / surefire / enforcer warnings on
   `perc-distribution-tree` (a diff against a baseline `mvn install` on
   `development` is acceptable per AGENTS.md).

A reactor build is **not** required: the diff is confined to
`perc-distribution-tree` plus the two poms already on the branch, so
the per-module standalone build is sufficient and the AGENTS.md default
applies.

## Resolve the review thread (per AGENTS.md PR Review Comment Resolution)

After the build is green and the commits are pushed to the PR head:

1. Locate the review thread on PR #1536 (the `kilo-code-bot` summary
   comment posted today; the only currently-unresolved thread on the PR
   per the GraphQL query).
2. Reply inline on the comment databaseId with a `**Mitigation (commit
   <sha>):**` statement that cites:
   - The commit hash on the PR head that contains the fix.
   - The two files changed (`BundledJdbcDrivers.java`,
     `install.xml`) and the specific edit (move `postgresql-42.7.7.jar`
     from CURRENT to PRIOR; add `postgresql-42.7.12.jar` to CURRENT).
   - The `InstallXmlDeleteSetTest` and `VerifyJdbcDrivers` test
     evidence (build status, test counts) from the standalone
     `mvn-env` clean install run in `modules/perc-distribution-tree`.
3. Resolve the review thread via the GraphQL `resolveReviewThread`
   mutation (NOT the inline reply alone). The thread id is the
   `id` field from the `reviewThreads` query, not the comment
   `databaseId`.
4. Re-verify with a follow-up GraphQL query that `isResolved: true` on
   the thread.

Skip the inline-reply/resolve step only if the human explicitly asks
for a doc-only skip (not the case here — this is a code fix that lands
on a PR that has already received a human approve from
`natechadwick-intsof`).

## Out of scope (explicit)

- Bumping any other JDBC driver (`mariadb`, `mysql-connector-j`, `derby`,
  `mssql-jdbc`, `jtds`, `ojdbc17`) — they are unchanged in PR #1536 and
  their CURRENT/PRIOR entries are already correct.
- Changing the `STAGING_GLOBS` or `GLOB_TO_ARTIFACT_ID` arrays — they
  are glob-based and unaffected by the version bump.
- Touching `scripts/verify-jdbc-drivers.py` or `scripts/check-no-glob-deletes.py`
  — the Java mains wired into the Maven `verify` phase are the source
  of truth; the Python ports are no longer required.
- Editing `CHANGELOG.md` or `docs/ai-generated/...` for the version
  bump itself — the Dependabot PR body documents the upstream changelog.
- Force-pushing to `origin/development` — explicitly forbidden by
  `.kilo/rules/no-force-push-development.md`.

## Risks / things to double-check

- **Order of entries in `PRIOR_FILENAMES`.** The block already orders
  entries by when the prior-version bump happened (most recent first
  in the file but the linkedH set is insertion-ordered). The new
  `postgresql-42.7.7.jar` entry must be inserted as the last entry in
  that block to keep the file diff minimal and follow the existing
  pattern (see lines 134-137 for the derby/mssql precedent).
- **`InstallXmlDeleteSetTest` is a strict set equality test.** It
  compares `EXACT_FILENAMES ∪ PRIOR_FILENAMES` to the literal
  `<include>` names in `install.xml`. Any deviation (typo in the JAR
  name, missing include, extra include) fails the build — so a clean
  `mvn install` is the proof of correctness and the validation step
  above is non-optional.
- **H2 driver exclusion.** Note that `H2` is not in
  `BundledJdbcDrivers.EXACT_FILENAMES` or `install.xml` delete list,
  even though it has a staging glob — this is the existing behavior
  and must not be "fixed" as part of this PR (per
  `#548 default embedded engine` comment).
- **Dependabot branch protection.** The
  `dependabot/maven/maven-55f3996f77` branch is owned by Dependabot;
  pushing follow-up commits to it is the standard Dependabot flow and
  will be picked up by the PR. There is no need to recreate the PR or
  force-push to `development`.

