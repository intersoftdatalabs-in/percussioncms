# scripts/ — perc-distribution-tree

Utility scripts used to verify, debug, or inspect the assembled Percussion CMS distribution.

## verify-jdbc-drivers.sh

Asserts that the assembled distribution artifact contains a valid, non-empty `jetty/base/lib/jdbc/` directory with real JDBC driver JARs.

**When to run**: after `mvn package` / `mvn verify` of `modules/perc-distribution-tree`, and as part of CI.

**Invocation**:

```sh
./scripts/verify-jdbc-drivers.sh
./scripts/verify-jdbc-drivers.sh --artifact path/to/perc-distribution-tree.jar
./scripts/verify-jdbc-drivers.sh --artifact path/to/perc-distribution-tree.jar \
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

## Adding a script here

Per `AGENTS.md`, scripts in this module live under `scripts/`. Add a new `.sh` (POSIX shell preferred) or `.bat` (Windows) entry, and document it above.