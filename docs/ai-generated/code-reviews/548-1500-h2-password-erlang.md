# Erlang-style self-review: Embedded H2 DB password (interactive prompt + random fallback) + StartJetty launcher path fix

**Scope:** Two related fixes that get a fresh interactive install of Percussion CMS to first-boot.

1. **H2 password** (issue #548 / #1500 matrix smoke): embedded H2 was opening the on-disk
   `Repository/CMDB.mv.db` with whatever credentials `rxrepository.properties` reported. An empty
   PWD= on a fresh DB worked, but the moment the file existed with non-empty credentials
   (operator-supplied, or stale from a prior install), the runtime opened it with `sa`/empty and
   H2 returned `[28000-232] Wrong user name or password`. Fixed by always materialising a
   non-empty H2 DB password:

   - Interactive install: wizard prompts and confirms a CMS DB password (8+ chars recommended;
     re-prompt on mismatch; abort after 5 failed confirmations; reject empty).
   - Silent install: ANT generates a cryptographically random base64url password (24 bytes
     entropy via `SecureRandom`).
   - The H2 password is consumed plaintext by both `rxrepository.properties` and
     `perc-ds.properties` (encrypted by Jetty's `JettyDatasourceConfigurationAdapter` only when
     non-empty — H2 password is intentionally plaintext to avoid the install-time-vs-runtime
     secure-dir key-mismatch class of failures).

   **Operator-supplied interactive passwords are NOT persisted to
   `var/config/generated/passwords`.** That file is reserved for system-generated
   credentials only (the random cmdb password in silent installs, plus Admin / Editor /
   Contributor demo defaults managed by `PSUserService`). Operator-typed secrets live only
   in `rxconfig/Installer/rxrepository.properties` and the encrypted
   `jetty/base/etc/perc-ds.properties`.

2. **StartJetty.bat launcher path** (separate, surfaced while smoke-testing the H2 fix):
   `java.properties` was written with `Properties.store()` escapes (`C\:\Program
   Files\\Microsoft\\jdk-21.0.12.8-hotspot\\bin\\java.exe`). The bat read those raw and passed
   them to `java -jar start.jar` as a launcher arg; Jetty parsed the unescaped colon as a
   `--module` arg separator and printed `launcher missing: C\:\...` instead of starting. Fixed
   by unescaping `\\`, `\:`, `\=` inside `:try_config` before validating or using the path.
   Also fixed a stray `SHIFT REM comments in …` comment marker that cmd.exe was parsing as a
   SHIFT command, printing `Invalid parameter to SHIFT command` on startup.

**Diff:**

```
docs/ai-generated/code-reviews/548-1500-h2-password-erlang.md   (new; this file)
modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/DbInstallConfigResolver.java
modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/InteractiveDbConfigCollector.java
modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/InteractiveInstallWizard.java
modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java
modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml
modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DbInstallConfigResolverTest.java
modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/DefaultEmbeddedH2PackagingTest.java
modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/InteractiveDbConfigCollectorTest.java
modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/InteractiveInstallWizardTest.java
modules/perc-ant/src/main/java/com/percussion/ant/install/PSGenerateRepositoryPassword.java   (new)
modules/perc-ant/src/test/java/com/percussion/ant/install/PSGenerateRepositoryPasswordTest.java   (new)
modules/perc-jetty/src/main/jetty/resolve-java-home.bat
modules/perc-jetty/src/test/java/com/percussion/jetty/java/ResolveJavaHomeScriptTest.java
modules/utils/src/main/java/com/percussion/utils/container/adapters/JettyDatasourceConfigurationAdapter.java
system/src/main/java/com/percussion/install/PSGeneratedPasswords.java                              (new)
system/src/test/java/com/percussion/install/PSGeneratedPasswordsTest.java                        (new)
```

## Recommendation: **approve**

## Findings

### Bugs

- **None blocking.** Cross-platform path handling uses `java.nio.file.Path.resolve` /
  `toAbsolutePath().normalize()` throughout — no hardcoded `File.separator` literals in
  filesystem path construction. Tests assert the file is written under `var/config/generated`
  on Windows and Linux alike (`PSGeneratedPasswordsTest.passwordsFileIsUnderVarConfigGenerated`).
- **None on Linux/macOS.** `PSGeneratedPasswords.VAR_CONFIG_GENERATED` uses forward slashes
  intentionally (documented and asserted in `varConfigGeneratedConstantUsesForwardSlashes`).
- **SHIFT parsing trap** documented and locked down by
  `ResolveJavaHomeScriptTest.batScriptDoesNotInvokeShiftCommand`.
- **Properties-store unescape** locked down by
  `ResolveJavaHomeScriptTest.batScriptUnescapesPropertiesStoreEscapes`. Manually verified
  end-to-end with a real `java.properties` containing a Microsoft JDK path with spaces and
  colon; the parsed `JAVA_HOME` is the literal `C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot`.

### Behavioral coverage

- **Random generation uniqueness** — `generateRandomPasswordProducesUniqueValues` checks 256
  unique values; would catch a regression that swaps to a non-cryptographic PRNG.
- **Round-trip preserves unrelated keys** — `writePreservesUnrelatedKeys` and
  `existingPasswordsArePreserved` (ANT-side) guarantee the Admin/Editor/Contributor entries
  written by `PSUserService` are not clobbered.
- **Empty vs missing key** — `emptyValueIsDistinguishableFromMissingKey` documents and tests
  the explicit blank vs absent semantics.
- **Upgrade fallback** — `readOnMissingFileReturnsNull` and `readOnMissingKeyReturnsNull`
  ensure legacy installs with no `var/config/generated/passwords` file keep their existing
  `rxrepository.properties` password untouched.
- **Interactive prompt+confirm** — `h2PasswordMismatchRecoversOnLaterAttempt`,
  `h2PasswordMismatchRetriesThenAborts`, and `h2EmptyPasswordIsRejected` exercise the
  retry/abort contract; `dbH2PasswordKeyClearedOnReentry` covers the re-entry path.
- **Wizard summary redacts the password** — `interactiveH2SummaryReferencesRxrepositoryProperties`
  asserts the interactive summary points at `rxrepository.properties` (not
  `var/config/generated/passwords`); `silentH2SummaryReferencesGeneratedPasswordsFile`
  asserts the silent-install summary points at `var/config/generated/passwords`. Both
  assert the password value is never echoed.
- **Resolver wiring** — `structuredH2SurfacesCmdbPasswordAsSystemProperty` and
  `structuredH2WithoutPasswordDoesNotEmitCmdbPassword` document the silent-path
  delegation to `PSGenerateRepositoryPassword` (random mode).
- **ANT contract** — `PSGenerateRepositoryPasswordTest.randomModeGeneratesAndPersistsPassword`,
  `explicitValueModeStoresOperatorSuppliedPassword`, `existingPasswordsArePreserved`,
  `randomModeProducesNonEmptyUniqueValuesAcrossCalls`.
- **Packaging defaults** — `installRepositoryH2BranchWritesNonEmptyPassword` parses the
  ANT XML, extracts the H2 branch only, and asserts `PSMakeLasagna` is absent from that
  block (still present elsewhere) plus `PWD_ENCRYPTED` is written explicitly as `N`.

### Cross-platform / file I/O

- All filesystem path construction uses `java.nio.file.Path` APIs.
- The `var/config/generated` literal uses forward slashes (asserted by
  `varConfigGeneratedConstantUsesForwardSlashes`).
- Tests do not assert Unix-only path shapes.
- The bat unescape preserves the cross-platform property-file contract:
  - `Properties.store()` writes `C\:\Program Files\\Microsoft\\…` — opaque to non-Windows
    consumers, but Java `Properties.load()` round-trips correctly.
  - cmd.exe needs to unescape those characters before using the value as a path or launcher
    arg. The unescape order (`\\` first, then `\:`, then `\=`) matches the escape order to
    avoid double-unescape.
- `batScriptIsAsciiSafe` already enforces non-ASCII-safe constraints for cmd.exe OEM code pages.

### Security

- `SecureRandom` for password generation.
- No password is logged: `PSLogger.logInfo` only prints the file path; ANT property exposure
  is intentional for downstream targets.
- Summary printing redacts the password value; only the file path is shown.
- The password is stored plaintext in `perc-ds.properties` for embedded H2 — matches the
  pre-existing matrix-smoke contract (issue #548 / #1500) and avoids the encrypt/decrypt
  cwd-mismatch class of failures.

### Backward compatibility

- **Upgrade path is unaffected.** `PSJdbcDbmsDef.loadRxRepositoryProperties` still reads
  `PWD=` from `rxrepository.properties`; if `var/config/generated/passwords` has no `cmdb`
  key (legacy install), nothing changes.
- **External backends** still encrypt via `PSMakeLasagna` (only the embedded H2 branch
  skips encryption, and that branch is new behavior for fresh installs).
- **DTS installer** unaffected — it does not consume the new `cmdb.password` ANT property.
- **`resolve-java-home.bat` SHIFT-token fix**: prior installs with the stray `SHIFT REM`
  comment marker got `Invalid parameter to SHIFT command` at startup on Windows only.
  Linux/macOS `resolve-java-home.sh` did not contain the marker.

### Risks / notes

- **CMS DB password must be supplied by every interactive H2 install.** The wizard prompts
  and confirms; if the operator cancels after 5 confirmation failures, the wizard exits with
  `EXIT_DB_CONFIG` (1). Operators should back up `rxconfig/Installer/rxrepository.properties`
  (and `var/config/generated/passwords` for the silent-install cmdb key).
- **Silent matrix / CI smoke** rely on `PSGenerateRepositoryPassword` (random mode). The
  generated password is unique per install; test infrastructure for matrix CI must read the
  file if it ever needs DB-level access beyond the HTTP smoke probe (see
  `docker/scripts/perc-devctl.py:640` for the existing pattern).
- **Re-runnable installs.** Because the random password is regenerated every time the H2
  block runs, operators who re-run the installer against an existing install root will
  receive a new H2 password and the existing `CMDB.mv.db` will need to be wiped. This
  matches the existing fresh-install contract (the ANT `do.install` target already sets
  `clean.install` when applicable); no behavior change for upgrades.
- **`PSX_OBJECTACL` SYSID collision on first boot** is a separate, pre-existing bug not
  caused by this change. Surfaced during smoke testing; out of scope for this PR. Recommend
  a separate ticket to add a uniqueness retry / explicit transaction isolation in
  `PSFolderHelper.setDefaultPermissions`.

## Evidence

- `./mvnw -pl system -am clean install` → `BUILD SUCCESS`, 857 tests, 0 failures.
- `./mvnw -pl modules/utils -am clean install` → `BUILD SUCCESS`, 214 tests, 0 failures.
- `./mvnw -pl modules/perc-ant -am clean install` → `BUILD SUCCESS`, all tests pass.
- `./mvnw -pl modules/perc-jetty -am clean install` → `BUILD SUCCESS`,
  13 `ResolveJavaHomeScriptTest` cases pass.
- `./mvnw -pl modules/perc-distribution-tree -am clean install` → `BUILD SUCCESS`,
  171 tests, 0 failures.
- `mvn spotless:apply -pl system,modules/utils,modules/perc-ant,modules/perc-jetty,modules/perc-distribution-tree`
  → `BUILD SUCCESS` for changed source files. (Pre-existing spotless violations in
  `JettyDatasourceConfigurationAdapterTest` / `PSJdbcUtilsTest` exist on the baseline;
  not introduced by this change.)
- Manual cmd.exe reproduction of the unescape (see
  `C:\Users\Nate\AppData\Local\Temp\kilo\test_real_java_parse.bat`): a `java.properties`
  containing `JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot` parses to the
  literal expected path.
- Manual cmd.exe reproduction of the SHIFT fix: `SHIFT REM comments are bad` in a bat
  file produces `Invalid parameter to SHIFT command`. Removed from
  `resolve-java-home.bat`.

