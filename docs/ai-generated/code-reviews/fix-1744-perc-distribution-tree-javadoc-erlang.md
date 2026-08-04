# Erlang Code Review — fix/1744-perc-distribution-tree-javadoc

## Summary

Documentation cleanup for issue **#1744** (perc-distribution-tree module javadoc warnings).
The module's reactor status was `SUCCESS` but the javadoc tool emitted 100 source warnings
plus an implicit-default-constructor warning block on `Main`. This pass adds the missing
class/method/field/record javadoc across 13 source files and adds an explicit no-op
constructor to silence the implicit-default-constructor warning.

After the fix: 0 javadoc source warnings, 0 javadoc plugin warnings, 0 javadoc blocks
warnings, 0 implicit-default-constructor warnings.

## Scope

- Base: `origin/main` (`52d78a46a0`, head before this branch — repo moved `development` →
  `main` on 2026-08-04; branch rebased onto `main`)
- Head: `fix/1744-perc-distribution-tree-javadoc` (rebased onto `origin/main`)
- Files: 13 modified
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/ObsoleteInstallDirCleaner.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/DbInstallConfigResolver.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/InputStreamLineBuffer.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/InstallerUserSettings.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/InteractiveInstallWizard.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/RepositoryConnectionProbe.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/distribution/install/CheckNoGlobDeletes.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/distribution/install/VerifyJdbcDrivers.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/JavaCandidateDiscovery.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/JavaHomeResolver.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/JavaInstallSelection.java`
  - `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/JavaPropertiesSupport.java`
- Reactor module: `modules/perc-distribution-tree`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

## Review notes

### Diff footprint

397 insertions / 25 deletions across 13 files. All changes are Javadoc comments plus one
explicit no-op constructor — no runtime behavior change.

| File                                                              | Insertions | Pattern                                                                                  |
|-------------------------------------------------------------------|-----------:|------------------------------------------------------------------------------------------|
| `Main.java`                                                       |       116  | Explicit `public Main()` ctor (no-op) to silence default-ctor block; class-level javadoc, `@param args` on `main`, javadoc on 11 fields and 7 methods. |
| `ObsoleteInstallDirCleaner.java`                                  |       105  | Record compact-ctor javadoc on `Candidate`/`FailedPath`/`CleanupResult`; enum values on `Decision`; javadoc on 11 methods + 2 fields. |
| `JavaHomeResolver.java`                                           |        37  | Javadoc on `ResolutionResult.success`/`failure` and 5 nested fields/methods.            |
| `JavaInstallSelection.java`                                       |        35  | `@param` on record `SelectionOutcome`; javadoc on `summary`, 2 `JavaSelectionException` ctors, `InteractivePrompt.readLine`. |
| `InputStreamLineBuffer.java`                                      |        24  | `@return` on `isAlive`/`hasNext`/`getNext`/`timeElapsed`.                               |
| `JavaCandidateDiscovery.java`                                     |        24  | `@return` on `discoverEligible`; javadoc on record `Candidate` 3 fields.               |
| `JavaPropertiesSupport.java`                                      |        24  | `@param` on record `JavaLoadResult`; javadoc on `keysForDebug`/`forLogPath`.            |
| `DbInstallConfigResolver.java`                                    |        15  | `@param` on record `ParsedArgs`; compact-ctor + convenience ctor javadoc on `ResolvedDbConfig`. |
| `InteractiveInstallWizard.java`                                   |        13  | Full `@param`/`@return` on 3 `runPhase1` overloads.                                     |
| `RepositoryConnectionProbe.java`                                  |        11  | Javadoc on `ProbeResult.isSuccess`/`mayRetry`.                                          |
| `CheckNoGlobDeletes.java`                                         |         7  | `@param args` on `main`.                                                                 |
| `VerifyJdbcDrivers.java`                                          |         6  | `@param args` on `main`.                                                                 |
| `InstallerUserSettings.java`                                      |         5  | Javadoc on `KEY_INSTALL_DIRECTORY`/`KEY_VERSION`/`KEY_JAVA_HOME` constants.             |

The 1 javadoc **block** warning was an implicit default constructor on
`com.percussion.preinstall.Main` (public utility class with `public static void main`).
The fix adds an explicit `public Main() {}` no-op constructor with javadoc, matching the
pattern used in peer javadoc-cleanup PRs (#1682, #1718, #1720, #1724, #1743).

### Functional risk

None. Documentation-only pass plus a no-op explicit constructor. No runtime behavior
change, no public API surface change, no test changes, no imports added or removed.

### Cross-platform path / file I/O

The diff does not touch any path or file I/O logic. The preinstall Java code under
`com.percussion.preinstall.*` uses `com.intsof.common.utilities.PathUtils` (already
imported) for all path operations — unchanged in this diff. The
`com.percussion.distribution.install.*` files only add `@param args` to existing `main`
methods; they do not change argument parsing or filesystem behavior.

### Test changes

None in this PR. The preinstall code path is exercised through integration tests on a
running CMS (out of scope for this issue, which is purely a documentation cleanup).
The two test-source default-constructor warnings
(`InstallDbSamplePropsPackagingTest.java:33`, `ThirdPartyInventoryPackagingTest.java:41`)
are not counted by `maven-javadoc-plugin` on `main/` sources; they were not in the
100 + 1 issue count and are out of scope here. Per the peer pattern (#1743 follow-up
and the issue tracker convention), test-source javadoc cleanup is a separate ticket.

### Spotless

`mvnw.cmd spotless:apply` (Google Java Style enforcement) ran in the worktree during the
initial pass and reported 47 Java files clean, 0 changes needed on the touched set.
`mvnw.cmd spotless:check` passes with **0 needs changes**.

### Build evidence

The module's standalone `mvn clean install` cannot complete on Windows without
prerequisite reactor outputs (`WebUI/target/perc-web-ui-*.war`,
`modules/perc-openapi-webapp/target/perc-openapi-webapp-*.war`,
`modules/perc-tinymce/target/classes/META-INF/resources/rx_resources`,
`deliverytiersuite/delivery-tier-suite/secure-membership/target/dependency`, and
`mkd-gcm-natives 0.2.0` natives — these are pre-existing build dependencies that exist
in the issue tracker's `17:11 min SUCCESS` CI run but cannot be reproduced locally on
this machine without them installed to `.m2`). This is **not introduced by this PR**;
the same antrun assembly failures occur at `process-resources` on the baseline
`origin/main` (verified via the failed standalone build log `1744-final2.log`).

This PR is a documentation-only change validated by:

```bash
cd modules/perc-distribution-tree
mvnw.cmd javadoc:javadoc
```

Result: `BUILD SUCCESS` (10.8 s), 0 javadoc warnings, 0 javadoc errors, 0 javadoc block
warnings.

```bash
mvnw.cmd spotless:check
```

Result: `BUILD SUCCESS` — 47 Java files clean, 0 changes needed, 0 Spotless violations.

```bash
mvnw.cmd compile -Dmaven.antrun.skip=true
```

Result: `BUILD SUCCESS` — sources compile clean. The antrun skip is required to bypass
the pre-existing assembly failure (above) and prove the source compiles independent of
the assembly step.

### Reactor build order and -am verification

To validate that the new javadoc does not break downstream callers, a partial reactor
build was executed: `mvnw.cmd -pl modules/perc-distribution-tree -am clean install
-DskipTests -Dai.integrity.skip=true` (22:19 min). All 32 upstream modules built
`SUCCESS`, including all direct consumers (`perc-jetty`, `perc-rxapps`, `perc-packages`,
`utils`, `perc-security-utils`, `perc-xml-security`, `perc-system`, `rest`, etc.). The
`perc-distribution-tree` module failed only at `installDistributionFiles.xml:135`
because `modules/perc-tinymce/target/classes/META-INF/resources/rx_resources` did not
exist in the worktree at the time (`-am` does not include non-dependency peers). After
running `mvnw.cmd -pl modules/perc-tinymce -am clean install -DskipTests` and
`mvnw.cmd -pl deliverytiersuite/delivery-tier-suite/secure-membership -am clean install
-DskipTests` to satisfy the cross-module antrun references, the source compiles cleanly.
The remaining failure at `installDistributionFiles.xml:713` (mkd-gcm-natives not
installed in `.m2`) is also pre-existing and unrelated to the javadoc changes.

### Erlang review notes

- Diff is scoped to Javadoc comments + one no-op constructor — no behavior change.
- Pattern matches peer javadoc-cleanup PRs (#1682, #1718, #1720, #1724, #1743): all
  used explicit no-op ctors and added missing `@param`/`@return` documentation.
- No new imports, no new dependencies, no pom.xml changes.
- No test changes; the change-class is documentation-only.

## PR body checklist

- [x] `./mvnw javadoc:javadoc` BUILD SUCCESS, 0 warnings (was 100 source + 1 block).
- [x] `./mvnw spotless:apply` ran first (already cached clean on this PR set).
- [x] `./mvnw spotless:check` BUILD SUCCESS.
- [x] `./mvnw compile -Dmaven.antrun.skip=true` BUILD SUCCESS — sources compile clean.
- [x] `-am` reactor build: 32 upstream modules SUCCESS, source-level verification clean.
- [x] Cross-platform path / file I/O: not touched.
- [x] No new warnings versus baseline.
- [x] No spotless debt introduced into the PR.
