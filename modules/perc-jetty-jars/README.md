# perc-jetty-jars

This module assembles the bundles of third-party and CMS utility jars that the Jetty
distribution loads from `Jetty/defaults/lib/perc/` and `Jetty/defaults/lib/perc-logging/`.
It contains no Java sources of its own (it ships as `packaging=pom`); its only artifact is a
run-time `jar-with-dependencies` produced by the `maven-assembly-plugin` from the dependencies
declared in `pom.xml`.

## What it bundles

- CMS utility bundles: `perc-jetty-logging`, `utils` (without Guava, since Jetty provides its own).
- Database drivers: Derby (`derby`, `derbyclient`, `derbynet`), jTDS, Microsoft `mssql-jdbc`, and
  Oracle `ojdbc17`.
- Connection pooling: `HikariCP`.
- XML parsing: `xercesImpl` + `xml-apis`.
- JASPIC: `geronimo-jaspi` (Jakarta Authentication SPI for EE11).

`servlet-utils` is intentionally **not** on this assembly. It depends on the Jakarta Servlet
API, which lives on the EE11 child classloader; placing `servlet-utils` on the server
classloader caused `NoClassDefFoundError: jakarta/servlet/ServletResponse` during
`PSContextLoaderListener` startup. `servlet-utils` is therefore packaged into the `Rhythmyx`
WAR via an explicit WebUI dependency instead.

## Relationship to perc-jetty

`perc-jetty` (the shipping assembly module) unpacks this artifact's output jar into
`Jetty/defaults/lib/perc/` at build time, alongside the unpacked `jetty-home` distribution.
See `perc-jetty/pom.xml` (`unpack-jetty-distribution` execution) and
`perc-jetty/src/main/jetty/defaults/modules/perc.mod` for how these jars are exposed to
the Jetty server.

## Building

```
mvn clean install
```

The output is `target/perc-jetty-jars-8.2.0-SNAPSHOT.jar` (and its `jar-with-dependencies`
classifier when the assembly descriptor runs), which is then unpacked into the Jetty
distribution by `perc-jetty`.

## See also

- `perc-jetty` — the Jetty distribution assembler that consumes this artifact.
- `perc-jetty-logging` — the Log4j2/perc-logging jar bundled by this module.
- `utils` — the CMS utility jar bundled by this module.

