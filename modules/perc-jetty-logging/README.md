# perc-jetty-logging

This module assembles the Log4j2 + SLF4J jar bundle that the Jetty distribution loads from
`Jetty/defaults/lib/perc-logging/`. It contains no Java sources of its own (it ships as
`packaging=pom`); its only artifact is a run-time `jar-with-dependencies` produced by the
`maven-assembly-plugin` from the dependencies declared in `pom.xml`.

## What it bundles

The assembly is the **server-side** logging stack. It is loaded by the Jetty server classloader
and owns the `logging|default` Jetty capability (see `perc-jetty/src/main/jetty/defaults/modules/perc-logging.mod`)
so that webapps inherit it.

|            Dependency            |  Scope   |                                                                 Purpose                                                                 |
|----------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `log4j-api`, `log4j-core`        | compile  | The Log4j2 implementation that the CMS configures (see `perc-jetty/src/main/jetty/defaults/modules/perc-logging/resources/log4j2.xml`). |
| `log4j-1.2-api`                  | compile  | Log4j 1.2 bridge so legacy log4j-1.x API calls are routed to Log4j2.                                                                    |
| `log4j-jul`, `log4j-jcl`         | compile  | `java.util.logging` and Apache Commons Logging bridges.                                                                                 |
| `log4j-slf4j2-impl`              | compile  | SLF4J 2.x binding that routes SLF4J calls to Log4j2.                                                                                    |
| `log4j-iostreams`                | compile  | `Logger` wrappers for `System.out` / `System.err` (used by `IoBuilder`-style patterns).                                                 |
| `disruptor`                      | compile  | LMAX Disruptor — required for the lock-free async log4j2 appender.                                                                      |
| `slf4j-api`                      | compile  | SLF4J facade exposed to webapps.                                                                                                        |
| `jul-to-slf4j`, `jcl-over-slf4j` | compile  | JUL / JCL bridges that re-route to SLF4J.                                                                                               |
| `commons-logging`                | provided | Optional bridge; declared provided because Spring historically prefers the real artifact over a bridge.                                 |

## Why a separate module?

The Jetty server classloader must be able to fully configure and own the logging stack before any
webapp classloader is built. Putting the Log4j2 jars in this module (and not on the webapp
classpath) lets the server hand a configured `LoggerContext` to webapps that need it. Webapps see
Log4j2 through `jetty.webapp.addProtectedClasses+=,org.apache.logging.log4j.` in `perc-logging.mod`
(GH-1484 / GH-1485); they do **not** see SLF4J (it is in `addHiddenClasses+=,org.slf4j.`).

## Relationship to perc-jetty

`perc-jetty` (the shipping assembly module) unpacks this artifact's output jar into
`Jetty/defaults/lib/perc-logging/` at build time. See `perc-jetty/pom.xml`
(`unpack-jetty-distribution` execution) and `perc-jetty/src/main/jetty/defaults/modules/perc-logging.mod`
for how the bundle is exposed to the Jetty server.

## Building

```
mvn clean install
```

The output is `target/perc-jetty-logging-8.2.0-SNAPSHOT.jar` (and its `jar-with-dependencies`
classifier when the assembly descriptor runs), which is then unpacked into the Jetty
distribution by `perc-jetty`.

## See also

- `perc-jetty` — the Jetty distribution assembler that consumes this artifact.
- `perc-jetty-jars` — the broader run-time jar bundle (drivers, XML parsing, JASPIC, CMS
  utilities).
- `modules/perc-jetty/src/main/jetty/defaults/modules/perc-logging.mod` — the Jetty module
  descriptor that wires this jar into the server capability model.

