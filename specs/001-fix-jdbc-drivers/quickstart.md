# Quickstart: Fix Missing JDBC Drivers in Percussion Distribution Install

**Branch**: `001-fix-jdbc-drivers` | **Date**: 2026-07-10 | **Spec**: [spec.md](spec.md)

End-to-end validation guide for the fix. This is a **run guide**, not implementation code; full code belongs in `tasks.md` and the implementation phase.

## Prerequisites

- JDK 21 (per root `AGENTS.md` for the `development` branch).
- Maven build wrapper: `./mvn-env.sh` (or `mvn-env.bat` on Windows).
- A clean working tree on the `001-fix-jdbc-drivers` branch.
- POSIX utilities available: `unzip`, `stat`, `find` (the verification script uses them).

## Setup

```sh
# From repo root, on branch 001-fix-jdbc-drivers
git status                 # confirm clean tree
git rev-parse --abbrev-ref HEAD   # confirm branch

# Build the module that produces the distribution artifact
cd modules/perc-distribution-tree
../../mvn-env.sh clean install
```

Expected: a successful Maven build that produces `modules/perc-distribution-tree/target/perc-distribution-tree.jar`.

## Validation Scenarios

### Scenario 1 — Default-repository driver ships (SC-001, SC-002)

```sh
# 1. Locate the produced distribution artifact
ls -la modules/perc-distribution-tree/target/perc-distribution-tree.jar

# 2. Run the verification script
./modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh \
    --artifact modules/perc-distribution-tree/target/perc-distribution-tree.jar
```

Expected outcome:
- Exit code 0.
- Human-readable table listing each driver JAR with non-zero size and "valid: yes".
- At least one JAR — the MariaDB/MySQL connector — is present and listed.

### Scenario 2 — `jetty/base/lib/jdbc/` exists and is non-empty (FR-001, FR-002)

```sh
TMP=$(mktemp -d)
unzip -q modules/perc-distribution-tree/target/perc-distribution-tree.jar \
    -d "$TMP/dist"
ls -la "$TMP/dist/jetty/base/lib/jdbc/"
find "$TMP/dist/jetty/base/lib/jdbc/" -type f -name '*.jar' | wc -l
```

Expected outcome:
- Directory exists.
- At least one `.jar` file is present.

### Scenario 3 — No zero-byte or stub JARs (SC-002)

```sh
TMP=$(mktemp -d)
unzip -q modules/perc-distribution-tree/target/perc-distribution-tree.jar -d "$TMP/dist"
find "$TMP/dist/jetty/base/lib/jdbc/" -type f -name '*.jar' -size 0
```

Expected outcome: no output (no zero-byte files).

```sh
find "$TMP/dist/jetty/base/lib/jdbc/" -type f -name '*.jar' \
    -exec unzip -t {} \; | grep -E "ERROR|warning"
```

Expected outcome: no `ERROR` lines; only the standard "No errors detected" lines from `unzip -t`.

### Scenario 4 — Loud failure when a driver cannot be resolved (FR-003, SC-004)

Simulate by temporarily misnaming a driver coordinate in `modules/perc-distribution-tree/pom.xml` and rebuilding:

```sh
# After intentionally breaking a coordinate, e.g.:
#   <artifactId>mariadb-java-client</artifactId>  →  <artifactId>mariadb-java-client-BROKEN</artifactId>
../../mvn-env.sh clean install
```

Expected outcome: Maven build fails during `maven-dependency-plugin:copy-dependencies` (configured `failOnAnyMissingDependency=true`) with a message identifying the missing artifact coordinate. The build does NOT silently produce an empty `jetty/base/lib/jdbc/`.

Revert the breaking change before continuing.

### Scenario 5 — Legacy `DEVELOPMENT=true` behavior preserved (FR-004)

```sh
DEVELOPMENT=true ../../mvn-env.sh clean install
TMP=$(mktemp -d)
unzip -q modules/perc-distribution-tree/target/perc-distribution-tree.jar -d "$TMP/dist"
ls "$TMP/dist/jetty/base/lib/jdbc/"
```

Expected outcome:
- The full production driver set is still present (MariaDB, Derby, MSSQL, jTDS, Oracle).
- The development MySQL connector from the legacy path is added on top (if the legacy `system/Tools/mysql/` path is restored; otherwise the production set alone is correct, matching today's behavior).
- Build does not regress compared to current `DEVELOPMENT=true` builds.

### Scenario 6 — CI runs the verification automatically

Run:

```sh
../../mvn-env.sh -pl modules/perc-distribution-tree verify
```

Expected outcome: the Maven build runs the verification script as part of the `verify` phase and exits non-zero if the driver set is wrong (wires in via a future Maven `exec` execution; see `tasks.md` for the exact plugin invocation).

## Linking to Other Artifacts

- Functional requirements: [spec.md#requirements](spec.md)
- Success criteria: [spec.md#success-criteria](spec.md)
- Data model (artifact shape): [data-model.md](data-model.md)
- Contracts: [contracts/README.md](contracts/README.md)
- Research / decisions: [research.md](research.md)
- Implementation tasks: `tasks.md` (produced by `/speckit.tasks`, not by `/speckit.plan`)

## Done When

- [x] Scenarios 1–3 pass on a clean build.
- [x] Scenario 4 (forced failure) demonstrates the build fails loudly.
- [x] Scenario 5 (legacy `DEVELOPMENT=true`) preserves prior behavior.
- [x] Scenario 6 (CI integration) is wired and non-flaky.

