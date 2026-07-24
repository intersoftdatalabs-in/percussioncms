# perc-jetty

This Module creates Jetty install/deployment jar including dependencies, configurations and script files.

## Runtime Baseline

- Jetty: 12.1.7
- Jakarta Servlet API: 6.1.0
- Servlet environment modules: ee11
- Embedded JMS broker: Apache Artemis (in-vm `vm://0`)

The module descriptor at src/main/jetty/defaults/modules/perc.mod is the source of truth for enabled Jetty ee11 modules.

## Embedded Messaging Configuration

- Artemis broker XML: `src/main/jetty/defaults/etc/artemis/broker.xml`
- Jetty JNDI resources: `src/main/jetty/defaults/etc/perc-mq.xml`
- Artemis config folder is exposed on classpath via `src/main/jetty/defaults/modules/perc-mq.mod`

## Building

Run: ../../mvn-env.sh -pl modules/perc-jetty clean install -DskipTests

## Linux service (systemd) — GH-962

Native systemd install is provided under `src/main/jetty/service/`:

- `install-jetty-service.sh` — prefers systemd when available; `--initd` forces SysV
- `percussion-cms.service.in` — unit template (`Type=forking`, `TimeoutStartSec=1800`, journal)
- `README-systemd.md` — operator install / migrate / journal notes

```bash
sudo ./install-jetty-service.sh PercussionCMS install
sudo systemctl start PercussionCMS
journalctl -u PercussionCMS -n 50 --no-pager
```

## Java home resolution (GH-991 / issue #1340)

Operators **do not** need to place a JRE under `<InstallDir>/JRE`. At CMS
install time the preinstall selects a system JDK/JRE (or honors
`-Dperc.java.home=...`) and writes `<InstallDir>/java.properties`. Start,
stop, and service install scripts **must** resolve Java through that contract
(`specs/991-system-java-home/contracts/`):

1. `<InstallDir>/java.properties` (`JAVA_HOME`, optionally `JAVA`) — **primary**
2. Process `JAVA_HOME` environment variable
3. Optional legacy `<InstallDir>/JRE` then `<InstallDir>/JRE64` (if still present)
4. `java` discoverable on `PATH`
5. **Hard fail** with major **21+** and sources tried — never soft-fail into an
   unvalidated JRE/JRE64 path

Resolve helpers (`resolve-java-home.sh` / `.bat`) ship next to the Jetty
scripts and are sourced by `StartJetty.sh`, `StartJetty.bat`, `StopJetty.bat`,
and `install-jetty-service.{sh,bat}` (all hard-fail on resolve failure). After
install, change which Java home CMS uses by editing `java.properties` and either
restarting the console, or re-running `install-jetty-service.sh` so
`/etc/default/<service>` (or Procrun `--JavaHome`) reflects the new path.
See `specs/991-system-java-home/quickstart.md` (Smoke A and D).

## Logging retention (GH-939)

Application logs are configured by Log4j2 in:

`src/main/jetty/defaults/modules/perc-logging/resources/log4j2.xml`

| Policy | Value |
|--------|--------|
| Rotate size | **10 MB** (`SizeBasedTriggeringPolicy`) |
| Rolled file count | **10** (`DefaultRolloverStrategy max` + `Delete` / `IfAccumulatedFileCount exceeds="10"`) |

`max="10"` alone only caps the `%i` counter within a date window when `filePattern`
includes `%d{yyyy-MM-dd}`. The `Delete` action removes older **dated** archives so
disk use stays bounded. Same retention idea as DTS `log4j2-tomcat.xml`.

Unit tests: `src/test/java/com/percussion/jetty/logging/PercLoggingLog4j2ConfigTest.java`
