# Erlang Code Review — fix/1681-delivery-tier-distribution-javadoc-cleanup

## Summary

Documentation cleanup for issue #1681 (delivery-tier-distribution module javadoc warnings). The
module's reactor status was `SUCCESS` but the javadoc tool emitted ~100 source warnings plus
implicit-default-constructor warnings on three utility classes. This pass adds the missing
class/method/record-component/exception javadoc, adds an explicit private constructor to each
utility class so the implicit-default-constructor warning is silenced, and adds the missing
`serialVersionUID` on the one checked exception.

Standalone module build after the fix: 77/0/0 tests, 0 javadoc warnings, 0 javadoc plugin
warnings, 0 javadoc blocks warnings, 0 implicit-default-constructor warnings, BUILD SUCCESS in
~6 minutes (a real `clean install`, not an incremental compile).

## Scope

- Base: `origin/development` (`7f2d787acc`, head before this branch)
- Head: `fix/1681-delivery-tier-distribution-javadoc-cleanup`
- Files: 8 modified (10 main-source Java files touched), 0 added, 0 removed
- Reactor module: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution`
- Pre-PR Erlang review recorded at this file (issue #1681 is the tracking item, not a separate
  PR review).

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

## Review notes

### Diff footprint

|                File                |                                                                                                                                                                                                Change                                                                                                                                                                                                 |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AntJobFailedException.java`       | Class-level javadoc + constructor javadoc.                                                                                                                                                                                                                                                                                                                                                            |
| `MainDTSPreInstall.java`           | Class-level javadoc; private utility constructor (silences "use of default constructor, which does not provide a comment"); javadoc on `main`, `extractArchive`, `execJar`; `@param` for `ParsedArgs` record components.                                                                                                                                                                              |
| `java/JavaCandidateDiscovery.java` | `@return` on `discover`, `eligible`, `discoverEligible`; `@param rawCandidates` on `eligible`; javadoc on `Candidate` constructor, `path`, `versionDisplay`, `eligible`.                                                                                                                                                                                                                              |
| `java/JavaHomeResolver.java`       | `@param`/`@return` on `resolve`, `inferHomeFromLauncher`, `parseMajorVersion`, `isSupportedMajor`, `launcherName`, `renderFailure`, `success`/`failure` factories, `success`/`javaHome`/`source`/`attempts` accessors; `@param` for `Attempt` record components; javadoc on `JavaHomeProbe` interface methods, `ResolutionSource` enum constants, `DefaultProbe` (with explicit default constructor). |
| `java/JavaInstallSelection.java`   | `@param` on constructor; `@return`/`@throws` on `selectAndPersist`; `@param` for `SelectionOutcome` record components; `serialVersionUID` on `JavaSelectionException`; javadoc on `summary`, both `JavaSelectionException` constructors, and the `InteractivePrompt.readLine` functional-method.                                                                                                      |
| `java/JavaPropertiesSupport.java`  | Private utility constructor; `@param`/`@return`/`@throws` on `load`, `readJavaHome`, `readJava`, `write`, `mergePreserving`; javadoc on `WRITTEN_BY_VALUE`; `@param` for `JavaLoadResult` record components; javadoc on `keysForDebug`, `summary`, `forLogPath`.                                                                                                                                      |
| `InteractiveDtsInstallWizard.java` | `@param` for `WizardResult` record components; javadoc on `proceed`/`abort` factories.                                                                                                                                                                                                                                                                                                                |
| `RepositoryConnectionProbe.java`   | Javadoc on each `ProbeStatus` enum constant; javadoc on `ProbeResult` record + `@param`/`isSuccess` accessor.                                                                                                                                                                                                                                                                                         |

### Functional risk

None. This is pure documentation and one no-op change (a private constructor on a class that
already exposes only static methods). No runtime behavior, no public API surface, no test changes.

### Cross-platform path / file I/O

The diff does not touch any path or file I/O logic — the keystore warning reported by the build
(`Keystore file '.../target/tomcat-ssl.keystore' doesn't exist.`) is emitted by the
`gen-keystore` Ant task before it creates the keystore; it is informational, not an error, and
was already present on `origin/development`. No change here.

The pre-existing "Unused declared dependencies found" warnings (maven-dependency-plugin:analyze-only)
are also baseline on `origin/development` (verified via `git show origin/development:pom.xml` and
the baseline build log) — they reflect runtime / packaging-only dependencies declared in this
module's `pom.xml` that aren't directly referenced by Java sources (they are picked up by the
Ant-style installer wiring). Out of scope for the javadoc cleanup; the AGENTS.md rule for this
module only forbids introducing **new** warnings on changed modules.

### Spotless

`mvn spotless:apply` reformatted 6 of the 10 touched files (Google Java Style enforcement):
MainDTSPreInstall, InteractiveDtsInstallWizard, RepositoryConnectionProbe, JavaCandidateDiscovery,
JavaHomeResolver, JavaInstallSelection, JavaPropertiesSupport. After `spotless:apply`,
`mvn spotless:check` passes on all touched files. No new Spotless violations introduced.

### Build evidence

```
cd deliverytiersuite/delivery-tier-suite/delivery-tier-distribution
mvnw.cmd clean install -B -Dai.integrity.skip=true
```

Result: `BUILD SUCCESS` in ~6 min.

```
[INFO] Tests run: 77, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] delivery-tier-distribution-8.2.0-SNAPSHOT.jar
[INFO] delivery-tier-distribution-8.2.0-SNAPSHOT-javadoc.jar
```

`mvnw.cmd spotless:check` after `spotless:apply`:

```
[INFO] Spotless.Java is keeping 24 files clean - 0 needs changes to be clean
[INFO] BUILD SUCCESS
```

### Notes for the PR body

- Resolves #1681 (the tracking issue; not a PR-review thread).
- Issue-reported baseline was `JavadocSrcWarn=100, JavadocBlocks=1, OtherWarn=46`; my
  standalone-module `mvnw clean install` baseline matched JavadocSrcWarn and JavadocBlocks (the
  46 OtherWarn entries correspond to the "Unused declared dependencies" warnings plus the
  keystore warning, which are baseline on `origin/development` and out of scope).
- After the fix, the standalone-module javadoc-tool output contains **0** source warnings and
  **0** plugin warnings. No new compiler warnings introduced on the changed module.

### Alternatives considered

- **Suppress the implicit-default-constructor warnings via `@SuppressWarnings("javac")`** —
  rejected; the explicit private constructor is one line and is the idiomatic Java fix that
  also makes the utility-only intent explicit to future readers.
- **Move the warning suppression to the Maven javadoc-plugin `<quiet>` / `<doclint>` config
  in `pom.xml`** — rejected; the warnings are real documentation gaps, not noise. Fixing the
  docs is the right answer for a public-API consumer.

