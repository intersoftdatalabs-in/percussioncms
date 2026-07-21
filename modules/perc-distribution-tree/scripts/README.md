# scripts/ — perc-distribution-tree

Utility scripts used to verify, debug, or inspect the assembled Percussion CMS distribution.

The build-time gates (`mvn verify`) are invoked through `exec-maven-plugin:java` so they run identically on Windows, Linux, and macOS. The `.sh` and `.bat` files here are operator-facing wrappers around the same Java main classes.

## verify-jdbc-drivers (.sh / .bat)

Asserts that the assembled distribution artifact contains a valid, non-empty `jetty/base/lib/jdbc/` directory with real JDBC driver JARs.

**Cross-platform behavior**: both wrappers delegate to the canonical Java main
`com.percussion.distribution.install.VerifyJdbcDrivers`. The Maven
`verify` phase already invokes that main class via
`exec-maven-plugin:java` (see `modules/perc-distribution-tree/pom.xml`
execution `verify-jdbc-drivers`), so the build gate does not depend on
Git-Bash, WSL, or `bash` being present on Windows CI.

**When to run**: after `mvn package` / `mvn verify` of `modules/perc-distribution-tree`, and as part of CI.

**Invocation (POSIX)**:

```sh
./scripts/verify-jdbc-drivers.sh
./scripts/verify-jdbc-drivers.sh --artifact path/to/perc-distribution-tree.jar
./scripts/verify-jdbc-drivers.sh --artifact path/to/perc-distribution-tree.jar \
    --expected-driver-glob 'mariadb-java-client-*.jar,derby-*.jar,derbyclient-*.jar,derbynet-*.jar,mssql-jdbc-*.jar,jtds-*.jar,ojdbc17-*.jar'
```

**Invocation (Windows)**:

```bat
scripts\verify-jdbc-drivers.bat
scripts\verify-jdbc-drivers.bat --artifact path\to\perc-distribution-tree.jar ^
  --expected-driver-glob "mariadb-java-client-*.jar,derby-*.jar,derbyclient-*.jar,derbynet-*.jar,mssql-jdbc-*.jar,jtds-*.jar,ojdbc17-*.jar"
```

The `.bat` shim resolves `JAVA_HOME` (or `JAVA_HOME_21`, then PATH) and runs
`java -cp target\perc-distribution-tree.jar com.percussion.distribution.install.VerifyJdbcDrivers`
with the flags forwarded unchanged. Build the artifact first (`mvn package`)
or point `--artifact` at an existing jar.

The `--expected-driver-glob` option is what the Maven `verify` phase uses
(see `modules/perc-distribution-tree/pom.xml` execution `verify-jdbc-drivers`),
so the example above exits 0 against a freshly built artifact. Single-quote
the glob string so the shell does not expand `*` in your interactive shell.
Use `--expected-driver-set` (not recommended) only when you need to pin
specific filenames; that option requires re-editing on every driver version
bump and is not wired into the CI build.

**Exit codes**:

| Code | Meaning |
|------|---------|
| 0 | All checks passed |
| 1 | Invocation error (bad args, artifact not found, missing tool) |
| 2 | `jetty/base/lib/jdbc/` missing or empty |
| 3 | One or more JARs are zero-byte |
| 4 | One or more JARs are not valid Java archives |
| 5 | Artifact could not be unpacked |
| 6 | `--expected-driver-set` or `--expected-driver-glob` does not match what's shipped |

## check-no-glob-deletes (.sh / .bat)

Static assertion that the install/upgrade ANT script's `<delete>` block inside `<target name="install_jdbc_drivers">` does not use glob patterns. A glob like `mysql-connector-java-*.jar` would silently purge integrator-supplied drivers whose filenames happen to match a bundled-name pattern; this script guards against that regression.

**Cross-platform behavior**: both wrappers delegate to the canonical Java main
`com.percussion.distribution.install.CheckNoGlobDeletes`. The Maven
`verify` phase invokes the same main class directly via
`exec-maven-plugin:java`, so the build gate does not depend on a shell.

**When to run**: automatically by the Maven `verify` phase (see `modules/perc-distribution-tree/pom.xml` execution `check-no-glob-deletes`). Also runnable manually.

**Invocation (POSIX)**:

```sh
./scripts/check-no-glob-deletes.sh
./scripts/check-no-glob-deletes.sh --install-xml path/to/install.xml
```

**Invocation (Windows)**:

```bat
scripts\check-no-glob-deletes.bat
scripts\check-no-glob-deletes.bat --install-xml path\to\install.xml
```

**Exit codes**:

| Code | Meaning |
|------|---------|
| 0 | install_jdbc_drivers `<delete>` uses exact filenames only |
| 1 | Invocation error (bad args, install.xml not found) |
| 7 | One or more `<include>` entries are glob patterns (contains `*` or `?`) — the failure this script exists to catch |

## Adding a script here

Per `AGENTS.md`, scripts in this module live under `scripts/`. Per the cross-platform hard gate, **every script that CI or an operator runs must have both a `.sh` and a `.bat` shim** OR be implemented as a Java main class invoked through `exec-maven-plugin:java`. Prefer the latter for any new build-time gate so the gate runs identically on every host.