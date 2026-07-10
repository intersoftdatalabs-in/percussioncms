# Repo Scripts

Scripts in this directory are repo-wide utilities (not tied to a single module). Module-specific
scripts should live under that module's own `scripts/` directory instead.

## fast-build.sh / fast-build.bat

Runs a Maven build with slow, non-essential plugins skipped, for a quick local dev iteration
loop (compile/package without waiting on the full quality/verification pipeline).

This reactor binds several plugins to the default lifecycle that are expensive across the
29+ module build:
- A custom `ai-build-integrity` plugin that generates/verifies SHA-256 hashes for 15,000+
  files per module (tens of seconds per module).
- `maven-javadoc-plugin` `attach-javadocs` (runs `javadoc` and packages a `-javadoc.jar` for
  every jar module).
- `maven-enforcer-plugin` rule checks.
- `maven-dependency-plugin` `analyze-only` bytecode analysis.
- Unit tests (`maven-surefire-plugin`).

None of these are needed for a "does it compile/package" iteration loop, so this script skips
them via their documented Maven properties, and runs Maven offline (`-o`) by default since
dependencies are normally already resolved locally.

Usage (Linux/macOS/Git Bash):
```
./scripts/fast-build.sh [--with-tests] [--online] <maven args...>
```

Usage (Windows cmd/PowerShell):
```
scripts\fast-build.bat [--with-tests] [--online] <maven args...>
```

Any arguments other than `--with-tests`, `--online`, and `--help` are passed straight through
to Maven (e.g. `-pl <module>`, `-am`, `install`, `package`, `-P<profile>`).

Flags applied by default:
- `-Dai.integrity.skip=true`
- `-Dmaven.javadoc.skip=true`
- `-Denforcer.skip=true`
- `-Dmdep.analyze.skip=true`
- `-Dcheckstyle.skip=true` (defensive; checkstyle is not bound to the default build today)
- `-DskipTests=true` (omitted if `--with-tests` is passed)

Examples:
```
./scripts/fast-build.sh -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am package
./scripts/fast-build.sh -pl modules/perc-security-utils install
./scripts/fast-build.sh --with-tests -pl system install
./scripts/fast-build.sh --online -pl rest install
```

Notes:
- This is a **dev convenience script only**. Do not use it for release/CI builds — those must
  run the full pipeline including hash integrity verification, javadoc, and enforcer rules.
- Uses `mvn-env.sh` / `mvn-env.bat` under the hood, so JDK selection follows the same rules as
  a normal build (JDK 21 on `development`, JDK 8 on `development-8.1.x`).

## hot-deploy-local.sh

Builds selected CMS modules (`system`, `rest`, `sitemanage`, `webui`) and copies the built
artifacts into a local Jetty-based CMS install for fast local testing, optionally restarting
Jetty afterward. See the script's own header comment for full usage and options.

## resolve-conflicts.sh

Scans the working tree for unresolved Git merge conflicts and reports them.

## gh-preflight.sh

Checks that GitHub CLI (`gh`) and the `origin` git remote are both pointed at the correct
fork/repository (`intersoftdatalabs-in/percussioncms`) before running GitHub-related automation,
with an optional `--fix` to correct misconfiguration.

## fetch-gh-code-scanning-alerts.sh

Fetches GitHub code scanning alerts for a repository via `gh` and writes a markdown report to
`docs/ai-generated/tasks/gh-codeql-alerts/alerts.md`.

## authenticate-sigstore.sh

Authenticates with Sigstore OIDC via `cosign` once per session, to avoid repeated browser
prompts during multi-module Maven builds that perform artifact signing/verification.
