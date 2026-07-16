# Jetty 12 `jetty.home` / `jetty.base` System Property Setup

**Date:** 2026-07-10
**Module affected:** `modules/perc-distribution-tree`
**File changed:** `modules/perc-distribution-tree/pom.xml` (the `setup-home`
`exec-maven-plugin` execution, lines 646-685)
**Severity:** Build-blocking on Windows (`InvalidPathException: Illegal char
<:>`); silent runtime failure on Linux

## Summary

The `setup-home` `exec-maven-plugin` invocation was passing `jetty.home`
and `jetty.base` as **command-line arguments** to
`org.eclipse.jetty.start.Main`:

```xml
<arguments>
    <argument>jetty.home=${assembly-directory}/jetty/upstream</argument>
    <argument>jetty.base=${assembly-directory}/jetty/base</argument>
    ...
</arguments>
```

Jetty's `StartArgs` does parse these as `key=value` and forwards them to
the property map, but only **after** `BaseHome` has been initialised. By
the time the `.mod` files are read, `BaseHome` has already fallen back
to the JVM working directory as both `jetty.home` and `jetty.base`. Any
`basehome:` URI in a module file is then resolved against the wrong
location, and on Windows the `:` triggers
`java.nio.file.InvalidPathException: Illegal char <:>`.

## Symptom

```
[INFO] --- exec:3.5.0:java (setup-home) @ perc-distribution-tree ---
WARN  : Use module compression-gzip instead.
java.nio.file.InvalidPathException: Illegal char <:> at index 8: basehome:lib/jdbc/
        at java.base/sun.nio.fs.WindowsPathParser.normalize(WindowsPathParser.java:204)
        at org.eclipse.jetty.start.PathMatchers.asPath(PathMatchers.java:67)
        at org.eclipse.jetty.start.PathMatchers.getSearchRoot(PathMatchers.java:184)
        at org.eclipse.jetty.start.PathMatchers.isAbsolute(PathMatchers.java:213)
        at org.eclipse.jetty.start.BaseHome.getPaths(BaseHome.java:333)
        at org.eclipse.jetty.start.StartArgs.expandModules(StartArgs.java:413)
```

## Fix

Pass `jetty.home` and `jetty.base` as **JVM system properties** instead
of command-line arguments. `exec-maven-plugin` forwards these to the
forked JVM as `-Djetty.home=…` and `-Djetty.base=…`, so they are visible
to `BaseHome` during its very first initialisation step (before any
`StartArgs` parsing or `.mod` file reading).

```xml
<systemProperties>
    <systemProperty>
        <name>jetty.home</name>
        <value>${assembly-directory}/jetty/upstream</value>
    </systemProperty>
    <systemProperty>
        <name>jetty.base</name>
        <value>${assembly-directory}/jetty/base</value>
    </systemProperty>
</systemProperties>
<arguments>
    <argument>--include-jetty-dir=${assembly-directory}/jetty/defaults</argument>
    <argument>--create-start-d</argument>
    <argument>--create-files</argument>
    <argument>--modules=perc-logging</argument>
    <argument>--add-modules=perc</argument>
    <argument>--approve-all-licenses</argument>
</arguments>
```

The previous `jetty.home=…` and `jetty.base=…` entries were **removed
from `<arguments>`** to avoid double-parsing of the `=` sign, which on
Windows can interact poorly with absolute paths containing `:` (e.g.
`D:\…`).

## Why this also fixes `perc.mod:45-46` and `perc-logging.mod:21`

Both of those `.mod` files contain valid Jetty 12 `[files]` `basehome:`
URIs:

```ini
[files]
basehome:etc/login.conf|etc/login.conf
basehome:etc/installation.properties|etc/installation.properties
```

and

```ini
[files]
basehome:modules/perc-logging
```

Once `jetty.home` and `jetty.base` are set as system properties, Jetty
12 resolves the `basehome:` prefix against the correct installation
directory and these `[files]` entries work as documented.

## Cross-platform behaviour

| OS      | Before                                                                 | After                                                                 |
|---------|------------------------------------------------------------------------|-----------------------------------------------------------------------|
| Windows | `Path.of("basehome:lib/jdbc/…")` throws `InvalidPathException: Illegal char <:>`. Build fails. | `jetty.home`/`jetty.base` are set, `basehome:` resolves, build proceeds. |
| Linux   | `Path.of("basehome:lib/jdbc/…")` returns a literal path that does not exist. Build "succeeds" but JDBC drivers are missing. | `jetty.home`/`jetty.base` are set, `basehome:` resolves, JDBC drivers load. |

## Verification

- `mvn -pl modules/perc-distribution-tree -DskipTests package` should
  now run the `setup-home` exec without the `InvalidPathException`.
- The generated `target/jetty/.../base/start.d/` and `modules/` layout
  should be populated correctly.
- The downstream packaging step (creating the `perc-distribution-tree.jar`
  uber-jar) should succeed.

## Related

- The `setup-home` invocation also emits a deprecation warning
  `WARN  : Use module compression-gzip instead.` This is unrelated and
  is a follow-up item for a future Jetty 12.1.x compatibility pass.
- `perc-logging.mod:21` (`basehome:modules/perc-logging` without the
  required `|dest` separator) is still a latent bug. It is harmless
  while the destination directory is empty, but should be fixed in a
  follow-up.
