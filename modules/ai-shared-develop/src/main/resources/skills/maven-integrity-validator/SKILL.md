---
name: maven-integrity-validator
description: Operate the com.intsof:ai-build-integrity-maven-plugin (v0.13.3) that seals and verifies AI resources (AGENTS.md, SKILL.md, .java, .xml, .yml, ...). Skip it, understand why it fails, regenerate hashes, and keep tests out of the source tree.
version: 1.2.0
---

# Maven Integrity Validator Skill

**Purpose**: Fast-track agents working with the `ai-build-integrity-maven-plugin` (GAV `com.intsof:ai-build-integrity-maven-plugin`, currently pinned at `0.13.3` in the root `pom.xml`). The plugin seals every AI / source file under the repo with a SHA-256 hash at `validate` time, then re-verifies every hash at `test` time. **Any agent that writes into the source tree during a build will break the seal and fail the next module.**

This skill is the **authoritative reference** for that plugin in this repo. Load it before any `mvn` run that exercises `validate` or `test`, and before any change that touches tracked files or adds tests.

## When to load this skill

- A `mvn validate` / `mvn test` / `mvn verify` run fails with `HASH MISMATCH`, `Source file missing for hash:`, `Central hash file not found:`, or `Central hash file is empty.`
- You are about to add or modify a tracked file (especially in `modules/`, `projects/`, root, `AGENTS.md`, `pom.xml`, any `.java`/`.xml`/`.yml`/`.json`/`.properties`).
- You are writing a test that needs to write a file. **Default: write into `target/`, never into `src/`.**
- You are running a partial-reactor build (`-pl`, `-rf`, `-am`) and the plugin over- or under-seals.
- You need to skip the plugin for a one-off local run without editing `pom.xml`.

## 1. Plugin wiring in this repo

Wired in the root `pom.xml` (v0.13.3, `pluginManagement` + active `<plugins>`):

```xml
<plugin>
  <groupId>com.intsof</groupId>
  <artifactId>ai-build-integrity-maven-plugin</artifactId>
  <version>0.13.3</version>
  <configuration>
    <hashFileMode>CENTRAL</hashFileMode>
    <baseDir>${maven.multiModuleProjectDirectory}</baseDir>
    <centralHashFile>${maven.multiModuleProjectDirectory}/target/ai-integrity.sha256</centralHashFile>
    <gitignoreAutoExclude>true</gitignoreAutoExclude>
    <forceIncludes>.env</forceIncludes>
    <includes>**/*.md, **/*.yml, **/*.json, **/*.properties,
            **/*.java, **/*.sh, **/*.bat, **/*.ts, **/*.js,
            **/*.xml, **/*.xsl, **/*.html, **/*.css, **/*.cs,
            **/*.py, **/*.jsp</includes>
  </configuration>
  <executions>
    <execution><id>generate</id><goals><goal>generate-hashes</goal></goals>
      <configuration><executionRootOnly>true</executionRootOnly></configuration>
    </execution>
    <execution><id>verify</id>  <goals><goal>verify-hashes</goal></goals>
    </execution>
    <execution><id>clean</id>  <goals><goal>clean-hashes</goal></goals>
      <configuration><executionRootOnly>true</executionRootOnly></configuration>
    </execution>
  </executions>
</plugin>
```

| Phase       | Goal               | `executionRootOnly` | Effect                                                                 |
|-------------|--------------------|---------------------|------------------------------------------------------------------------|
| `validate`  | `generate-hashes`  | `true`              | Walks the **entire repo** once at the start of the reactor, writes one **central** ledger `${maven.multiModuleProjectDirectory}/target/ai-integrity.sha256`. |
| `test`      | `verify-hashes`    | `false`             | Runs **every module**: re-reads the central ledger and re-hashes every tracked file. If any file changed, fails the build. |
| `clean`     | `clean-hashes`     | `true`              | Deletes the central ledger plus any sidecar `.sha256` files.            |

Audit report is written to `target/ai-integrity-report.json` after verification.

### What gets hashed (CENTRAL mode)

Every file under `baseDir` matching any of the include globs, **except** entries skipped by:

- `skipDirs` (default: `target,.git,node_modules,.tmp`)
- `.gitignore` (because `gitignoreAutoExclude=true`)
- `excludes` (default: `**/*.sha256,**/*.sha384,**/*.sha512`)
- `forceIncludes` (default: none) — and `.env` is force-included so dotfiles aren't lost

> **Cross-platform note**: traversal uses `java.nio.file.Files.walkFileTree` with normalized `Path` equality. Sidecar files are hidden via `Files.setAttribute("dos:hidden", true)` on Windows and via the leading dot on Unix. The path strings in the ledger are **forward-slash relative** to `baseDir` regardless of OS.

## 2. Skipping the plugin (preferred escape hatch)

The plugin accepts **two** skip flags. Either works; both are equivalent in the plugin's view.

```bash
# Linux / macOS
./mvn-env.sh -Dai.integrity.skip=true   <goal> ...

# Windows (wrapper enforces JDK 21)
mvn-env.bat -Dai.integrity.skip=true   <goal> ...

# Maven-conventional alias (also accepted by every goal)
./mvn-env.sh -Dskip.ai.integrity=true <goal> ...
```

`-Dai.integrity.skip=true` is the form used in this repo's own code reviews and `specs/`; prefer it for consistency.

The plugin also exposes a finer-grained `failOnError=false` parameter on `verify-hashes` (lives in `centralHashFile` configuration) — but **using the skip flag is the supported, idiomatic choice** for local-only skips. Do not edit `pom.xml` to disable the plugin for everyone.

### When to skip vs. when to regenerate

| Situation                                                                | Action                                  |
|--------------------------------------------------------------------------|-----------------------------------------|
| Local debug of a single test class on a module you're not changing       | `-Dai.integrity.skip=true`              |
| Local debug where you've **temporarily** edited a tracked file to test   | `-Dai.integrity.skip=true` **then** undo the edit before commit |
| You **intentionally** changed a tracked file (new skill, new test, etc.) | **Regenerate the ledger** (see §4)      |
| CI / full reactor build / pre-PR                                         | **Never skip.** Let it run.             |

> **Rule of thumb**: if your change is destined for a commit, the ledger must be regenerated **as part of that commit**. Skipping locally is for debugging only.

## 3. Why it fails — the failure catalogue

All of these come from `com.intsof.ai.build.integrity.HashVerifyMojo` / `HashGeneratorMojo`. The exact strings are emitted under a banner of `--------` lines and end with the skip-flavor hint.

### 3.1 `HASH MISMATCH: <file> - file may have been tampered with!`

The most common failure. A file listed in `target/ai-integrity.sha256` no longer hashes to the recorded value. Causes:

- You edited a tracked file after `validate` started (during a long test run, or in another terminal).
- A test wrote into `src/...` instead of `target/...` (see §5). Other modules see the mutation; the seal breaks.
- A code generator / Spotless / formatter rewrote a file mid-build.
- Someone committed without regenerating the ledger.
- CRLF/LF mismatch on a Windows checkout (`git config core.autocrlf=true`).

**Fix**: regenerate the ledger (§4). If you suspect CRLF, run `git config core.autocrlf false` in the repo, then regenerate.

### 3.2 `Source file missing for hash: <file>`

A file in the ledger was deleted (or moved/renamed) after `validate`. Fix: regenerate (§4). If the file should not have been removed, restore it.

### 3.3 `Central hash file not found: .../target/ai-integrity.sha256`

The ledger wasn't generated. Either:

- A previous `mvn clean` removed both `target/` and the ledger. Run `mvn validate` first (it regenerates the seal) **or** skip the plugin for the one-off run with `-Dai.integrity.skip=true`.
- The build was launched from a sub-module directory with `executionRootOnly=true` but the plugin defaulted to a per-module `baseDir`. The fix is to run from repo root **or** pass `-pl <root-module>` so the execution root runs.

### 3.4 `Central hash file is empty.`

The ledger exists but is empty. Almost always means `validate` was skipped on the execution root (e.g. `-pl foo -rf bar` and the root wasn't in the resumed reactor). Run `mvn validate -N` at the root first.

### 3.5 `Base directory does not exist: <path>`

`baseDir` resolved to a path that doesn't exist. Check `${maven.multiModuleProjectDirectory}` — usually means you're running outside a Maven checkout or the wrapper is pointing at the wrong JDK/project root.

### 3.6 `Partial reactor detected` / `Reactor-scoped sealing`

**Not a failure — informational.** The plugin detected a partial reactor (`-pl`, `-rf`, child-module build) and switched to seal-root-only walking. Resumed builds re-seal automatically. Do not panic at this log line.

### 3.7 `Skipping HashGeneratorMojo execution in non-root project.`

Informational. Generated when a child module runs while the execution root also runs. Safe to ignore.

### Failure advice footer (verbatim)

The plugin prints a recovery footer after every mismatch:

```
------------------------------------------------------------------------
If these changes were intentional, re-seal the project by regenerating hashes:
  mvn validate
To temporarily skip integrity verification for this build:
  -Dai.integrity.skip=true
  (also accepted: -Dskip.ai.integrity=true)
------------------------------------------------------------------------
```

Heed the advice. The `mvn validate` in the footer intentionally runs from the reactor root so the seal scope is correct (§4).

## 4. Regenerating the central ledger

The ledger lives at `<repo-root>/target/ai-integrity.sha256`. It is **gitignored** in the sense that it is inside `target/`, but it is **regenerated every `validate`**. After any intentional change to a tracked file, regenerate before you commit:

```bash
# Linux / macOS — from repo root
./mvn-env.sh validate

# Windows — from repo root
mvn-env.bat validate

# Or as part of a normal build (clean install also re-seals)
./mvn-env.sh clean install
```

`generate-hashes` has `executionRootOnly=true`, so it fires **once** at reactor root. The script walks the whole `baseDir` (the entire repo) and writes one central ledger. There is no per-module generation in this repo's configuration.

### Regenerate for a single module / partial reactor

`generate-hashes` is wired with `executionRootOnly=true` and writes **one repo-wide ledger**. If you run a partial build, the plugin uses `reactorScope=AUTO` (default) and re-seals only the touched seal roots. Flow:

1. `mvn -pl <module> validate` — the root execution runs (because `<root>` is in the reactor when `-pl` is used from the root), generates the ledger.
2. `mvn -pl <module> test` — verifies. Files outside the partial reactor are still in the ledger from the previous full seal; the verify step re-hashes them but expects the same value, so as long as nothing changed, the build passes.

> **If a partial build keeps failing with `HASH MISMATCH` on files you did not touch**, your checkout may have CRLF/LF drift. Force LF: `git config core.autocrlf false`, then `git rm --cached -r . && git reset --hard` is **not** the right fix — instead run `git add --renormalize .` and regenerate the ledger.

### When to regenerate via `clean install` vs `validate`

- `mvn validate` — fastest, just regenerates the ledger. Use this when only manifest / AI / config files changed.
- `mvn clean install` — regenerates and goes all the way to packaging. Use when artifacts / Java classes also changed.
- Do **not** use `mvn clean` alone — it deletes `target/` (including the ledger) but does not regenerate it. The next `validate` will rebuild it.

### Auditing the ledger

After a verify, examine `target/ai-integrity-report.json` (relative to repo root). It is a JSON bill of materials with `VERIFIED`, `TAMPERED`, and `MISSING` entries — useful when a CI failure points to a file you didn't know was tracked.

## 5. Test discipline — never write into the source tree

> **The single most common failure mode of this plugin is tests writing into `src/`.**

The `verify-hashes` step runs **every module at `test` phase**. If a test in module A writes a file into `projects/foo/src/main/resources/...`, the next module to test (or the same module's later verify) sees the file change and `HASH MISMATCH` fires.

### The rule

Tests must write to **`target/`** (preferred) or the **JVM temp dir** (acceptable for truly ephemeral data). The existing repository pattern is documented in `system/src/test/java/com/percussion/install/PSMigrationScaleFixtureTest.java:155`:

```java
/**
 * Resolve the timing-log markdown file path under the build {@code target/} directory.
 * This keeps test output out of tracked source files and avoids breaking the
 * ai-build-integrity seal.
 */
static Path resolveTimingLogFile() {
  return Path.of("target", "migration-timing.md").toAbsolutePath().normalize();
}
```

### Good vs. bad patterns

```java
// GOOD — write under target/. The plugin excludes target/ via skipDirs.
Path out = Path.of("target", "test-output.json");
Files.createDirectories(out.getParent());
Files.writeString(out, json);

// GOOD — JVM temp dir for throwaway scratch work.
Path tmp = Files.createTempFile("percussion-test-", ".json");

// GOOD — copy a resource out of src/test/resources to target for mutation.
Path src = Path.of("src/test/resources/fixture.xml");        // read-only input
Path dst = Path.of("target", "fixture-mutated.xml");         // mutable copy
Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);

// GOOD — use junit @TempDir for per-test isolation.
@TempDir
Path tmp;   // injected by JUnit 5

// BAD — writes into the source tree.
Files.writeString(Path.of("src/main/resources/live-config.json"), "{...}");

// BAD — uses absolute path or hardcoded /tmp (Unix-only).
Files.writeString(Path.of("/tmp/percussion", "out.txt"), "...");
//   ^ fails on Windows AND, if the path were instead
//     "C:\\Users\\...\\AppData\\Local\\Temp\\..." it would only fail outside the repo.

// BAD — concatenates with a hardcoded separator.
String p = base + "/out.txt";   // OS-dependent; use base.resolve("out.txt")
```

### Checklist before opening a PR

- [ ] `grep -R "Files\\.write\\|FileWriter\\|FileOutputStream" src/test/java` — every hit writes to `target/`, a `@TempDir`, or `Files.createTempFile(...)`. Nothing writes to `src/`.
- [ ] No test creates a file under `src/main/`, `src/test/`, `src/site/`, or any tracked resource directory.
- [ ] No test uses `System.getProperty("user.dir")` plus a relative path under `src/`.
- [ ] No test invokes `touch` / `sed -i` / `>` on a file under `src/` via subprocess.
- [ ] If a test needs to verify a "file written to disk" behavior, it either writes to `target/` and asserts there, or uses an in-memory `ByteArrayOutputStream` / Mockito `MockedConstruction`.

> **Why this is a hard gate**: a developer running `mvn -pl <module> test` locally sees the failure in their own module, but the same failure can blow up a sibling module later in the reactor — making the bug look unrelated to the test that caused it. Erlang treats it as a defect.

## 6. Examples

### 6.1 Local one-off test — skip the plugin

```bash
# Linux / macOS
cd projects/sitemanage
../../mvn-env.sh -Dai.integrity.skip=true -Dtest=PSSiteDataServiceTest test

# Windows
cd projects\sitemanage
..\mvn-env.bat -Dai.integrity.skip=true -Dtest=PSSiteDataServiceTest test
```

### 6.2 You changed a tracked file — regenerate the ledger

```bash
# Linux / macOS — from repo root
./mvn-env.sh validate

# Then run the focused build
./mvn-env.sh -pl modules/perc-legacy -Dtest=PSAesTest test
```

### 6.3 Pre-PR clean install (the spec's hard gate)

```bash
# From repo root — full reactor seal
./mvn-env.sh clean install

# Or per-module standalone (faster, see AGENTS.md "Pre-PR Maven verification")
cd projects/sitemanage
../../mvn-env.sh clean install
```

### 6.4 Partial reactor with `-rf`

```bash
# Resume from a specific module. The plugin auto-detects and re-seals.
./mvn-env.sh -rf :perc-distribution-tree verify
```

If the resume target is not in the original reactor, you may need to pass
`-Dai.integrity.resumeFromModule=:perc-distribution-tree` (or the bare `artifactId`).

### 6.5 Spotless / format check after editing a manifest

```bash
# Spotless doesn't touch hashes, but the underlying file it formats does.
# Regenerate after the format run.
./mvn-env.sh -pl projects/sitemanage spotless:apply
./mvn-env.sh validate   # re-seal the whole repo
```

> Note: Spotless is bound to a phase that runs **after** `validate` in many modules. If your `validate` regenerated the ledger and then Spotless reformatted a file, the follow-up `test` invoke will fail. Run validate **after** the formatter alone, then run tests separately.

### 6.6 Why the AGENTS.md / SKILL.md edit broke the build

You added a new skill under `modules/ai-shared-develop/src/main/resources/skills/...`:

```bash
# edit modules/ai-shared-develop/src/main/resources/skills/foo/SKILL.md
git add modules/ai-shared-develop/src/main/resources/skills/foo/SKILL.md
./mvn-env.sh validate   # seals the new file
git add target/ai-integrity.sha256   # (note: target/ is gitignored — see §7)
./mvn-env.sh -pl modules/ai-shared-develop test
```

Actually, **do not** commit `target/ai-integrity.sha256` — it is regenerated by every `validate` and lives inside `target/`. The PR fix is **just the new skill file**; the next CI run regenerates the ledger server-side from your committed source files.

## 7. Repository bookkeeping

- `target/ai-integrity.sha256` is **not** committed. It is in `target/`, which is gitignored.
- `target/ai-integrity-report.json` is also not committed. It is regenerated on every `verify-hashes`.
- The plugin's own `.sha256` sidecars are written next to each hashed file (default `hideHashFiles=true`). On Windows the file is hidden via `dos:hidden`; on Unix the leading dot is enough. If you `git status` and see a `.sha256` file alongside your tracked file, the plugin still wrote it. The default `excludes` (`**/*.sha256,**/*.sha384,**/*.sha512`) keeps it out of subsequent sealing, and standard `.gitignore` patterns should hide it. If it shows up, add `*.sha256` to the nearest `.gitignore`.

## 8. Cross-platform notes

- The wrapper to use depends on the OS:
  - Windows: `mvn-env.bat` (enforces JDK 21).
  - Linux / macOS: `./mvn-env.sh`.
- Paths in the ledger are **forward-slash, relative** to `baseDir`. Don't try to parse them as OS paths; use `Path.of(baseDir, ledgerEntry.replace('/', File.separatorChar))` (or simpler: `Paths.get(ledgerEntry)` against a `FileSystem` rooted at `baseDir`).
- Line endings: set `core.autocrlf=false` for this repo, or the ledger will diverge between Windows and Unix contributors. The plugin supports `<normalizeLineEndings>true</normalizeLineEndings>` if you must allow CRLF — but the canonical seal is LF.
- Hide flag: `hideHashFiles=true` is the default, so generated sidecars are hidden on both Windows (`dos:hidden`) and Unix (dotfile). Don't rely on the sidecar being visible in `ls`/`Get-ChildItem`.

## 9. Quick reference card

```bash
# Skip for a one-off local run
./mvn-env.sh -Dai.integrity.skip=true <goal> ...

# Regenerate the central ledger (repo-wide)
./mvn-env.sh validate

# Pre-PR clean install (JDK 21 enforced)
./mvn-env.sh clean install

# Pre-commit seal refresh after editing tracked files
./mvn-env.sh validate && git status   # target/ regenerated; nothing to commit

# Inspect the audit report
cat target/ai-integrity-report.json | jq '.[] | select(.status=="TAMPERED")'

# HASH MISMATCH on a file you didn't touch? CRLF drift.
git config core.autocrlf false
./mvn-env.sh validate
```

## 10. Common pitfalls (and the one-line fix)

| Symptom                                                            | Fix                                                              |
|--------------------------------------------------------------------|------------------------------------------------------------------|
| `mvn validate` from a sub-module does not create the ledger        | Run from repo root, or pass `-pl <root>` so the `executionRootOnly` goal fires. |
| `HASH MISMATCH` on a file you edited intentionally                 | `./mvn-env.sh validate` (regenerate).                            |
| `HASH MISMATCH` on a file you did not touch                        | CRLF drift or stray test write — see §5.                         |
| `Source file missing for hash:` after a `git rm`                   | `./mvn-env.sh validate` to drop the entry from the ledger.       |
| `Central hash file not found` after `mvn clean`                    | Run `mvn validate` first; `clean` deletes the ledger.            |
| `Central hash file is empty.`                                     | The root execution didn't run. Use `-N` from repo root or include the root in `-pl`. |
| Partial reactor `-pl foo -am` keeps re-sealing the whole repo      | That is intentional — `generate-hashes` walks the whole `baseDir`. Use `reactorScope=REACTOR` only if you really need module-scoped. |
| Spotless reformatted a file after `validate` ran                   | Re-run `validate` after the format pass.                         |
| Test fails with `Files.writeString` writing into `src/`            | Move the write to `target/` or use `@TempDir`. See §5.            |

## 11. Re-seal workflow after a tracked-file change

```text
1. Edit tracked file(s).
2. Run: ./mvn-env.sh validate
   - This regenerates target/ai-integrity.sha256.
3. Run your modules / tests:
   - ./mvn-env.sh -pl <module> test
4. If you only changed AI / config / manifest files, no `mvn install` is needed.
   If you changed Java, run `mvn install` so downstream modules pick up the new
   artifact (the integrity ledger does not affect artifact resolution).
5. Commit your source changes; do NOT commit target/ai-integrity.sha256.
```

## 12. References

- Plugin source: `com.intsof:ai-build-integrity-maven-plugin:0.13.3` (Maven Central).
- Root `pom.xml` lines 2658-2709 (pluginManagement) and 2713-2717 (active plugin).
- Failure advice: `com.intsof.ai.build.integrity.IntegrityFailureAdvice` — banner + skip-flag hint.
- Pre-commit review: `docs/ai-generated/code-reviews/` (use `/erlang-review` in Kilo).
- Cross-platform rules: repo-root `AGENTS.md` → "Cross-Platform File I/O & Paths".
- Test write-target convention: `system/src/test/java/com/percussion/install/PSMigrationScaleFixtureTest.java:155`.
