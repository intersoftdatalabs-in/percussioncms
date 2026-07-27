# Jetty startup WARN hygiene (GH-1484 – GH-1487)

**Date:** 2026-07-27  
**Issues:** [#1484](https://github.com/intersoftdatalabs-in/percussioncms/issues/1484),
[#1485](https://github.com/intersoftdatalabs-in/percussioncms/issues/1485),
[#1486](https://github.com/intersoftdatalabs-in/percussioncms/issues/1486),
[#1487](https://github.com/intersoftdatalabs-in/percussioncms/issues/1487)  
**Origin:** Windows 8.2 smoke catalog (#1369 / #1464), findings 2.1–2.6.

## Summary

Four classes of Jetty console noise observed on CMS 8.2 Windows smoke are fixed
in `perc-jetty` packaging and defaults (no application Java changes).

| Issue | Symptom | Fix |
|-------|---------|-----|
| **#1484** | Multiple SLF4J providers → NOP logger | `perc-logging` provides `logging\|default` so stock `logging-jetty` / `jetty-slf4j-impl` is not selected |
| **#1485** | Fork second JVM for `[perc-logging, perc]` | Remove `[exec]` from those modules; consolidate JVM system properties in `start.d/jvm.ini` |
| **#1486** | `ShutdownMonitor` + `CookieConfig.setComment` deprecations | Enable Jetty `shutdown` module (`jetty.shutdown.*`); SameSite via `setAttribute` in `Rhythmyx.xml` |
| **#1487** | Empty `jetty.xml` Args + DigesterFactory missing schemas | Named Server constructor Args on assembled `upstream/etc/jetty.xml`; ship W3C DTD/XSD jar for DigesterFactory |

## #1484 — Single SLF4J provider (Log4j2)

**Root cause:** `perc-logging.mod` provided `logging-log4j2|default` but **not**
Jetty’s `logging` capability. Jetty therefore also enabled stock
`logging-jetty` (`jetty-slf4j-impl`). Two `SLF4JServiceProvider` implementations
on the classpath produced “not a subtype” errors and NOP fallback.

**Change:**

- `defaults/modules/perc-logging.mod` → `[provides] logging|default` and
  `logging-log4j2`
- Share server Log4j with webapps via `jetty.webapp.addProtectedClasses` for
  `org.apache.logging.log4j.` (WEB-INF excludes log4j jars; app code needs
  `IoBuilder` etc. from server `lib/perc-logging`)
- Keep server SLF4J hidden via `jetty.webapp.addHiddenClasses` for `org.slf4j.`
  so WEB-INF `slf4j-api` wins for Artemis/Spring JMS

**Follow-up (upgrade smoke):** An earlier draft of this work used
`addHiddenClasses` for Log4j as well. That hid `log4j-iostreams` from the
Rhythmyx webapp and failed startup with
`NoClassDefFoundError: org/apache/logging/log4j/io/IoBuilder` from
`PSConsole` static init. Protected (not hidden) is required for Log4j.

Log4j2 remains the sole SLF4J binding from `lib/perc-logging/**.jar`
(`perc-jetty-logging` assembly, unpacked nested jars).

## #1485 — Avoid module-driven JVM forks

**Root cause:** Jetty `start.jar` forks whenever any enabled module contributes
`[exec]` / `--exec` JVM args. Both `perc-logging` and `perc` declared `[exec]`,
so the console warned:

```text
WARN : Forking second JVM due to forking module(s): [perc-logging, perc]
```

**Change:**

- Remove `[exec]` from `perc-logging.mod` and `perc.mod`
- Move Percussion-specific system properties (SAX factory, `java.library.path`,
  commons-logging factory, etc.) into `defaults/start.d/jvm.ini`
- Drop obsolete `-Dorg.eclipse.jetty.util.log.class=…Log4j2Logger` (depends on
  non-packaged `log4j-appserver`; Jetty 12 logs via SLF4J → Log4j2)

**Note:** `jvm.ini` still uses `--exec`, so start.jar may fork **once** for the
`jvm` module. That is intentional Jetty design. Fully in-process start requires
moving those args onto the `StartJetty` `java` command line and removing
`--exec` (optional follow-up).

## #1486 — ShutdownService + SameSite attribute

### ShutdownMonitor → ShutdownService

**Root cause:** `StartJetty.bat` passed `-DSTOP.PORT` / `-DSTOP.KEY` on the
**server** JVM, activating deprecated `org.eclipse.jetty.server.ShutdownMonitor`.

**Change:**

- Depend on Jetty stock `shutdown` module from `perc.mod`
- `defaults/start.d/shutdown.ini` defaults:
  - `jetty.shutdown.port=50011`
  - `jetty.shutdown.key=SHUTDOWN`
  - `jetty.shutdown.host=127.0.0.1`
- `StartJetty.bat` / `install-jetty-service.bat` pass operator `STOPPORT` /
  `STOPKEY` as Jetty start properties `jetty.shutdown.port` /
  `jetty.shutdown.key` (overrides ini; does **not** activate ShutdownMonitor)
- **Keep** `-DSTOP.PORT` / `-DSTOP.KEY` on `StopJetty.bat` and service
  `PR_STOPPARAMS` as **client** connection params for `start.jar --stop`

**PR #1518 review fix:** Restored operator customization of stop port/key on
Windows service install (`PR_STARTPARAMS=jetty.shutdown.port=%STOPPORT%;…`).
The first iteration had dropped `STOPPORT`/`STOPKEY` from start params entirely.

### CookieConfig.setComment → SameSite attribute

**Root cause:** `Rhythmyx.xml` used the Jetty 9-era
`<Set name="comment">__SAME_SITE_STRICT__</Set>` convention.
`SessionHandler.CookieConfig.setComment` is deprecated for removal.

**Change:** Servlet 6 / Jetty 12 style:

```xml
<Call name="setAttribute">
  <Arg>SameSite</Arg>
  <Arg>Strict</Arg>
</Call>
```

## #1487 — jetty.xml empty Args + DigesterFactory schemas

### Empty `Ignored arg [[]]` (×3)

**Root cause:** Stock `jetty-home` `etc/jetty.xml` passes three unnamed
constructor args (`threadPool`, `scheduler`, `byteBufferPool`). XmlConfiguration
can ignore them when binding fails, logging three WARNs.

**Change:** After unpacking jetty-home into `upstream/`, the perc-jetty antrun
pass rewrites those Args to named parameters matching Jetty’s `@Name`
annotations (`threadpool`, `scheduler`, `bufferPool`).

### DigesterFactory missing XMLSchema.dtd / datatypes.dtd / xml.xsd

**Root cause:** Jetty ee11-apache-jsp embeds Tomcat `DigesterFactory`, which
expects W3C schema resources next to
`org.apache.tomcat.util.descriptor.DigesterFactory`. Mortbay’s apache-jsp jar
does not ship those three files.

**Change:**

- Source files under `src/build/perc-xml-schemas/org/apache/tomcat/util/descriptor/`
  (redistributed from `tomcat-servlet-api` `jakarta.servlet.resources`)
- Assembly builds `defaults/lib/perc/perc-xml-schemas.jar` (picked up by
  `perc.mod` `lib/perc/**.jar`)
- Keep `-Dorg.eclipse.jetty.xml.XmlParser.Validating=false` in `jvm.ini`
  (intentional non-validating Jetty XML parse)

## Tests

`com.percussion.jetty.StartupWarnHygieneTest` asserts:

- `logging|default` provide + no `[exec]` on perc-logging / perc
- `jvm.ini` ownership of moved system properties
- `shutdown.ini` + StartJetty without server-side STOP.PORT
- StopJetty still has stop client props
- Rhythmyx.xml SameSite attribute (no comment)
- DigesterFactory schema source files present

## Operator notes

1. **Stop port/key** remain **50011** / **SHUTDOWN** (unchanged defaults).
2. After upgrade, re-run `install-jetty-service.bat` if the Windows service was
   registered with old start params containing `-DSTOP.PORT`.
3. If operators previously overrode SameSite via cookie **comment** in a custom
   context XML, migrate to `setAttribute` / `SameSite`.
