# Erlang pre-commit review: 984-installer-db-targets / issue #949

**Date**: 2026-07-16  
**Scope**: Uncommitted changes on `984-installer-db-targets` (exclude unrelated `org/`)  
**Intent**: CLI new-install database targets via `-Ddbprops` / property file; Oracle; connect validation; samples/docs  
**Base**: `development` (JDK 21)

## Summary

Solid structure: extractable resolver, good unit coverage for dbprops mapping/precedence/validation messages, samples and README, upgrade path gated by `do.install`. **Two correctness bugs block commit/PR**: (1) premature `Class.forName` in connection validation can fail installs that `InstallUtil` could still complete via registered JDBC JARs; (2) `Main.main` does not propagate ANT/`processCode` failure to process exit status, so outer installer can report success after FR-008 validation fails.

## Recommendation

**`request-changes`**

## Gate

| Item | Result |
|------|--------|
| Bugs open | **Yes (2)** |
| Missing behavioral tests for non-trivial new logic | Resolver: **adequate**. Validate action: **partial** (see suggestions). |
| **May commit/push / open PR** | **No** |

---

## Issues

### BUG-1 — Premature `Class.forName` blocks InstallUtil driver loading path

**Severity**: bug  
**Location**: `modules/perc-ant/src/main/java/com/percussion/ant/install/PSValidateRepositoryConnection.java` ~115–134  

**What**: After `registerJdbcDriversFromInstall` (which only populates `InstallUtil` jar URL list), the task calls `Class.forName(driverClass)` and **throws** on `ClassNotFoundException`.

**Why wrong**: Sibling install tasks (`PSExecSQLStmt`, etc.) rely on `InstallUtil.createLoadedConnection` / `createConnection`, which load drivers via system classpath **or** the custom loader fed by `InstallUtil.addJarFileUrl`. On a fresh install, drivers live under `jetty/base/lib/jdbc`; Ant path `ant.deps` may not expose them to the task classloader at resolve time (empty/cached path before copy), while `registerJdbcDriversFromInstall` + `createLoadedConnection` would still work. The hard `Class.forName` gate **fails the install incorrectly** and never reaches the path used by the rest of repository setup.

**Suggestion**: Remove the standalone `Class.forName` fail-fast, or only use it as a soft log. Let `InstallUtil.createLoadedConnection` perform load/connect. On failure, if message indicates missing driver, append guidance to place JARs under `jetty/base/lib/jdbc` (FR-012). Align with `PSExecSQLStmt` patterns.

**Test gap**: Add a unit/integration-style test that registers a jar URL / mocks InstallUtil path if feasible; at minimum, document that “missing driver” is asserted via connection failure message without requiring system Class.forName.

---

### BUG-2 — Outer installer does not exit non-zero when ANT/validation fails

**Severity**: bug (feature contract FR-008 / SC-003 / US3)  
**Location**: `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java` ~193–201  

**What**: `execJar(...)` returns `processCode` and sets `error` when ANT exits non-zero (including `PSValidateRepositoryConnection` `BuildException`). `main` **ignores** the return value, always prints `"Done extracting"`, and does not `System.exit(processCode)`.

**Why wrong**: Connectivity / install failures fail the Ant child but the **preinstall JVM still exits 0**. Unattended automation cannot detect failure. Config errors now correctly `System.exit(1)`, but the more important post-resolve failures (including new validation) do not.

**Suggestion**: After `execJar`, if `processCode != 0` or `Boolean.TRUE.equals(error)`, print a clear failure and `System.exit(processCode != 0 ? processCode : 1)`. Avoid printing a bare success line on failure. Add a small unit test around a package-visible helper if exit is hard to test, or test a method that maps processCode → shouldFail.

**Note**: Ignoring `processCode` is partly pre-existing; shipping FR-008 without fixing it makes the new validation **appear** to fail-fast in logs while the outer process still succeeds — product-visible false green.

---

### SUGGESTION-1 — Password on `-Dperc.db.password=...` process command line

**Severity**: suggestion (security hygiene)  
**Location**: `Main.execJar` loop that adds all `ResolvedDbConfig.systemProperties` as `-D`  

**What**: Repository password is passed as a JVM system property argument to the Ant process (visible via `ps` / process listings). Pre-existing pattern for structured `--db.*`; extended to dbprops path.

**Suggestion**: Prefer writing a temp properties file under the install/work dir (mode 600) and pass only a path, or use env-only for password. Not a gate-alone item if BUG-1/2 fixed first; track as follow-up if out of MVP.

---

### SUGGESTION-2 — Upgrade non-regression test is structural-only

**Severity**: suggestion  
**Location**: `RepositoryPropertiesInstallGuardTest`  

**What**: Asserts `installRepository.xml` contains `do.install` and task names. Good guardrail against accidental deletion of the gate; does not exercise Ant behavior.

**Suggestion**: Acceptable as a canary if BUG-1/2 fixed; optional later: fixture-based propertyfile simulation. Do **not** treat as sole proof of FR-006.

---

### SUGGESTION-3 — Connect-failure path under-tested

**Severity**: suggestion  
**Location**: `PSValidateRepositoryConnectionTest`  

**What**: Covers missing props file and missing driver class (via current Class.forName path). Does not assert unreachable host / SQLException → `BuildException` without password leak once Class.forName is removed.

**Suggestion**: After BUG-1 fix, add test with minimal valid-looking props + bogus host and short timeout; assert message does not contain password.

---

### NIT-1 — Sample `PWD=changeit`

**Severity**: nit  
**Location**: sample `rxrepository.*.properties`  

Acceptable placeholders; clearly documented. Prefer `# CHANGE_ME` comments only if product samples avoid any password-looking tokens in scanners.

---

### NIT-2 — Exclude accidental `org/` from any commit

Unrelated untracked tree under repo root (`org/apache/commons/jexl3/...`). Do not stage.

---

## What looks good

- `DbInstallConfigResolver` extraction + fail-fast on resolve for missing/invalid dbprops (passwords not in exception text) — covered by solid unit tests (12 cases).
- Oracle branch in `installRepository.xml` mirrors mysql/sqlserver; still behind `do.install`.
- Validation task also behind `do.install` (upgrade path not re-validated for rewrite).
- MariaDB default driver class for composed mysql path matches shipped JDBC packaging.
- Spec/plan/contracts under `specs/006-installer-db-targets/` are coherent with implementation.
- antlib registration present.

## Test evidence noted

Author reported (this session):

- `DbInstallConfigResolverTest` / guards / samples / extract: **16** tests green  
- `PSValidateRepositoryConnectionTest`: green under prior reactor run  

Re-run after fixes required.

## Handoff

1. Fix **BUG-1** (Class.forName).  
2. Fix **BUG-2** (Main exit code).  
3. Strengthen **SUGGESTION-3** test if cheap.  
4. Re-run unit tests via `./mvn-env.sh`.  
5. Re-request Erlang review; only then commit/push/PR.

**May commit/push: no**
