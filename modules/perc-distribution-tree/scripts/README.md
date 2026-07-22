# scripts/ — perc-distribution-tree

Utility scripts used to verify, debug, or inspect the assembled Percussion CMS distribution.

The build-time gates (`mvn verify`) are invoked through `exec-maven-plugin:java` so they run identically on Windows, Linux, and macOS. The Python scripts in this directory are the cross-platform operator-facing entry points; the canonical implementations live as Java main classes invoked directly by Maven so the build gate does not depend on a Python interpreter.

Per spec 994 (`specs/994-python-build-scripts/spec.md`): the original `.sh`/`.bat` wrappers around these Java mains have been removed (FR-004). Windows, Linux, and macOS users now invoke the Python entry point identically (`python3 scripts/<name>.py`).

## verify-jdbc-drivers.py

Asserts that the assembled distribution artifact contains a valid, non-empty `jetty/base/lib/jdbc/` directory with real JDBC driver JARs.

**Cross-platform behavior**: the canonical implementation is the Java main `com.percussion.distribution.install.VerifyJdbcDrivers`. The Maven `verify` phase invokes that main class via `exec-maven-plugin:java` (see `modules/perc-distribution-tree/pom.xml` execution `verify-jdbc-drivers`), so the build gate does not depend on Git-Bash, WSL, or `bash` being present on Windows CI. `verify-jdbc-drivers.py` is the cross-platform Python port of the same logic for manual operator runs.

**When to run**: after `mvn package` / `mvn verify` of `modules/perc-distribution-tree`, and as part of CI.

**Invocation (POSIX / Windows / macOS — identical)**:

```sh
python3 modules/perc-distribution-tree/scripts/verify-jdbc-drivers.py
python3 modules/perc-distribution-tree/scripts/verify-jdbc-drivers.py \
    --artifact path/to/perc-distribution-tree.jar \
    --expected-driver-glob 'mariadb-java-client-*.jar,derby-*.jar,derbyclient-*.jar,derbynet-*.jar,mssql-jdbc-*.jar,jtds-*.jar,ojdbc17-*.jar'
```

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

**Tests**: `python3 -m pytest modules/perc-distribution-tree/scripts/test_verify_jdbc_drivers.py -v`

## check-no-glob-deletes.py

Static assertion that the install/upgrade ANT script's `<delete>` block inside `<target name="install_jdbc_drivers">` does not use glob patterns. A glob like `mysql-connector-java-*.jar` would silently purge integrator-supplied drivers whose filenames happen to match a bundled-name pattern; this script guards against that regression.

**Cross-platform behavior**: the canonical implementation is the Java main `com.percussion.distribution.install.CheckNoGlobDeletes`. The Maven `verify` phase invokes that main class directly via `exec-maven-plugin:java` (see `modules/perc-distribution-tree/pom.xml` execution `check-no-glob-deletes`), so the build gate does not depend on a shell. `check-no-glob-deletes.py` is the cross-platform Python port of the same logic for manual operator runs.

**When to run**: automatically by the Maven `verify` phase (see `modules/perc-distribution-tree/pom.xml` execution `check-no-glob-deletes`). Also runnable manually.

**Invocation (POSIX / Windows / macOS — identical)**:

```sh
python3 modules/perc-distribution-tree/scripts/check-no-glob-deletes.py
python3 modules/perc-distribution-tree/scripts/check-no-glob-deletes.py \
    --install-xml path/to/install.xml
```

**Exit codes**:

| Code | Meaning |
|------|---------|
| 0 | install_jdbc_drivers `<delete>` uses exact filenames only |
| 1 | Invocation error (bad args, install.xml not found) |
| 7 | One or more `<include>` entries are glob patterns (contains `*` or `?`) — the failure this script exists to catch |

**Tests**: `python3 -m pytest modules/perc-distribution-tree/scripts/test_check_no_glob_deletes.py -v`

## api-update.py

Consolidated cross-platform replacement for the four legacy Windows batch files `APIUpdate-WEBUI.bat`, `APIUpdate-REST.bat`, `APIUpdate-SiteManage.bat`, and `APIUpdateJars.bat`. Each `.bat` ran the same three-step dance — build the relevant Maven module(s), copy the resulting jar(s) into the assembled distribution, and optionally restart Jetty — but hardcoded Windows-specific process spawning (`start /WAIT cmd /C ...`) and `xcopy` / `copy` syntax.

**Invocation**:

```sh
python3 modules/perc-distribution-tree/scripts/api-update.py --module webui
python3 modules/perc-distribution-tree/scripts/api-update.py --module rest
python3 modules/perc-distribution-tree/scripts/api-update.py --module sitemanage
python3 modules/perc-distribution-tree/scripts/api-update.py --module jars
python3 modules/perc-distribution-tree/scripts/api-update.py --module webui --skip-tests --no-restart
python3 modules/perc-distribution-tree/scripts/api-update.py --module webui --dry-run   # show the build plan
```

**Tests**: `python3 -m pytest modules/perc-distribution-tree/scripts/test_api_update.py -v`

## update-tinymce.py

Cross-platform Python port of `UpdateTinyMCE.bat`. Syncs TinyMCE asset sources from `modules/perc-tinymce/src/main/tinymce/` into the packaged-resources directory `modules/perc-tinymce/src/main/resources/tinymce/` so the next `mvn package` rebuild picks them up.

**Invocation**:

```sh
python3 modules/perc-distribution-tree/scripts/update-tinymce.py
python3 modules/perc-distribution-tree/scripts/update-tinymce.py --source /custom/tinymce --target /custom/tinymce-resources
```

**Tests**: `python3 -m pytest modules/perc-distribution-tree/scripts/test_update_tinymce.py -v`

## Adding a script here

Per `AGENTS.md`, scripts in this module live under `scripts/`. Per the cross-platform hard gate, **every script that CI or an operator runs must be implemented as either cross-platform Python (operator-facing) or a Java main class invoked through `exec-maven-plugin:java` (build-time gate)**. Prefer the Java-main pattern for any new build-time gate so the gate runs identically on every host; add a Python port of the same logic for manual operator runs, mirroring the `verify-jdbc-drivers.{java,py}` / `check-no-glob-deletes.{java,py}` split.