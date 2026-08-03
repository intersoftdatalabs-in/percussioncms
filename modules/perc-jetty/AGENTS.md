This project follows the Universal Code v1.0.0 — read docs/policies/UC-EMBED-v1.0.0.md (vendored; upstream https://github.com/monkeyking-hq/universal-code)

# perc-jetty AI Agent Notes

## Required reading

- Read modules/perc-jetty/README.md before making changes in this module.

## Linux service install (systemd / init.d)

- Scripts: `src/main/jetty/service/install-jetty-service.sh`, unit template
  `percussion-cms.service.in`, ops notes `README-systemd.md`
- GH-962 / specs `988-linux-systemd-services`: prefer **native systemd** unit;
  init.d is fallback (`--initd`). Do not dual-register chkconfig when systemd is used.
- Default `TimeoutStartSec=1800` for long post-upgrade starts; journal for stdout/stderr.
- Tests: `src/test/java/com/percussion/jetty/service/*Test.java`

## Java home resolution (GH-991 / issue #1340)

- Scripts at `src/main/jetty/resolve-java-home.{sh,bat}` implement the shared
  precedence (java.properties > env > optional install-dir JRE|JRE64 > PATH)
  and require major version **21+**. Operators do **not** need `<InstallDir>/JRE`.
- `StartJetty.sh`, `StartJetty.bat`, `StopJetty.bat`, and
  `service/install-jetty-service.{sh,bat}` source/call the helper and **hard-fail**
  on resolve failure (no soft-fail into unvalidated JRE/JRE64).
- Spec/contracts live in `specs/991-system-java-home/contracts/`; tests live
  under `src/test/java/com/percussion/jetty/java/` and
  `src/test/java/com/percussion/jetty/service/`.
- Re-point Java: edit `<InstallDir>/java.properties`, restart console; for
  services re-run `install-jetty-service.sh install` or update the Procrun
  service JavaHome.

## Logging (perc-logging / Log4j2)

- Config: `src/main/jetty/defaults/modules/perc-logging/resources/log4j2.xml`
- GH-939: size rotate **10 MB**, keep **10** rolled files, **Delete** older dated
  archives via `IfAccumulatedFileCount` (do not rely on `max` alone with `%d` patterns).
- Tests: `src/test/java/com/percussion/jetty/logging/PercLoggingLog4j2ConfigTest.java`
  (surefire is explicitly bound because this module is `packaging=pom`).

## Embedded Messaging

- Embedded JMS broker uses Apache Artemis, not ActiveMQ Classic.
- Broker configuration file: modules/perc-jetty/src/main/jetty/defaults/etc/artemis/broker.xml
- Jetty JNDI wiring file: modules/perc-jetty/src/main/jetty/defaults/etc/perc-mq.xml
- In-VM broker endpoint: vm://0

## Jetty and Servlet Specification

- Current Jetty version: 12.1.7
- Current Servlet API: Jakarta 6.1.0
- Servlet Environment: ee11 (Jakarta EE 11)
- Source of truth for Jetty version: root pom.xml property "jetty.version"
- Source of truth for Servlet API: root pom.xml property "jakarta.servlet.api.version"

### Important: Jetty EE Modules

Jetty 12.x provides multiple servlet environments via module loading:

- `ee8-*` modules → javax.servlet (legacy, NOT used)
- `ee9-*` modules → Jakarta Servlet 5.0
- `ee10-*` modules → Jakarta Servlet 6.0+ (legacy in this repository)
- `ee11-*` modules → Jakarta Servlet 6.1+ ✅ (REQUIRED)

The `perc.mod` file MUST use `ee11-*` modules because the project requires Jakarta Servlet 6.1.0:

- `ee11-deploy`
- `ee11-servlets`
- `ee11-annotations`
- `ee11-cdi`
- `ee11-jstl`

**CRITICAL**: Do not use `ee8-*` modules. All servlet filters and listeners must implement `jakarta.servlet.*` interfaces, not `javax.servlet.*`.

### How to update Jetty

1. Update the root pom.xml property:
   - <jetty.version>NEW_VERSION</jetty.version>
2. Verify the Jetty version supports the required Jakarta Servlet version
3. Update `perc.mod` if EE modules change (e.g., ee10 → ee11):
   - modules/perc-jetty/src/main/jetty/defaults/modules/perc.mod
4. Rebuild perc-jetty to refresh the assembled distribution:
   - ./mvnw clean install -pl modules/perc-jetty -DskipTests
5. Validate module names and dependencies under:
   - modules/perc-jetty/src/main/jetty/defaults/modules/

## Example: adding a new Jetty module

1. Create a module descriptor:
   - modules/perc-jetty/src/main/jetty/defaults/modules/example.mod
2. Enable by default (optional):
   - modules/perc-jetty/src/main/jetty/defaults/start.d/example.ini
   - Include: --module=example
3. Add related configuration files if needed:
   - modules/perc-jetty/src/main/jetty/defaults/etc/
4. If the perc module depends on it, add to:
   - modules/perc-jetty/src/main/jetty/defaults/modules/perc.mod
5. Build to verify:
   - ./mvnw clean install -pl modules/perc-jetty -DskipTests

## AI-Generated Worklogs and Change Logs

AI-generated worklogs, change notes, and implementation details should be placed in:

- Location: `modules/perc-jetty/src/site/markdown/worklog/`
- Format: Markdown (.md files)
- Naming: Use descriptive names (e.g., `jetty-12-compatibility-fixes.md`)

### Adding worklog entries to the site menu

1. Create the markdown file in `src/site/markdown/worklog/`
2. Update `modules/perc-jetty/src/site/site.xml`:
   - Add item under the "Work / Change Log" menu
   - Reference the file with `.html` extension (Maven converts .md to .html)
   - Example:

   ```xml
   <menu name="Work / Change Log">
       <item name="Jetty 12 Migration" href="./worklog/jetty-12-compatibility-fixes.html"/>
   </menu>
   ```

## Module Documentation

Module-specific documentation (README, guides, architecture, etc.) should be placed in:

- Location: `modules/perc-jetty/src/site/markdown/`
- Format: Markdown (.md files)
- Purpose: Overview, building, configuration, and usage guides

### Adding documentation to the site Overview menu

1. Create the markdown file in `src/site/markdown/`
2. Update `modules/perc-jetty/src/site/site.xml`:
   - Add item under the "Overview" menu
   - Example:

   ```xml
   <menu name="Overview">
       <item name="Overview" href="./index.html"/>
       <item name="Architecture" href="./architecture.html"/>
   </menu>
   ```

