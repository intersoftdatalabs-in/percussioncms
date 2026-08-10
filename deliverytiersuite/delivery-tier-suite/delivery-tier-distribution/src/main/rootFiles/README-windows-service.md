# Percussion DTS Windows services (Procrun / tomcat11)

Production and Staging DTS install as native Windows services via Apache Commons
Daemon **Procrun** (`tomcat11.exe` / `tomcat11w.exe`).

|         Installer          |   Default service name    |
|----------------------------|---------------------------|
| `DTSProductionService.bat` | `PercussionProductionDTS` |
| `DTSStagingService.bat`    | `PercussionStagingDTS`    |

Scripts live under `Deployment/Server/` after install (same tree as Tomcat).

## Install / remove

Run from an elevated command prompt with the current directory set to
`Deployment\Server` (or rely on the script’s `CATALINA_HOME` discovery):

```bat
cd /d C:\path\to\DTS\Deployment\Server
DTSProductionService.bat install
DTSStagingService.bat install

DTSProductionService.bat remove
```

Optional second argument overrides the Windows service name.

Java home is resolved via `resolve-java-home.bat` at the DTS install root
(`java.properties` written at install time — GH-991). A JRE under
`<InstallRoot>\JRE` is **not** required.

## Where logs go (issue #938)

|                 Stream                  |                                     Path                                     |
|-----------------------------------------|------------------------------------------------------------------------------|
| Application / console (stdout + stderr) | `Deployment\Server\logs\catalina.log`                                        |
| Procrun / commons-daemon service log    | `Deployment\Server\logs\` (LogPath; prefix defaults to service tooling)      |
| Log4j2 Tomcat + DTS app logs            | `Deployment\Server\logs\` (`catalina.log`, `localhost.log`, per-app `*.log`) |

This matches Linux service installers, which set:

```text
CATALINA_OUT=${CATALINA_HOME}/logs/catalina.log
```

Procrun’s `StdOutput=auto` / `StdError=auto` mode would instead create dated
`service-stdout.YYYY-MM-DD.log` files and is **not** used. After reinstalling the
service with a fixed script, restart the service and confirm
`Deployment\Server\logs\catalina.log` updates.

### Existing installs

If the service was installed with an older script:

1. Stop the service.
2. `DTSProductionService.bat remove` (or Staging equivalent).
3. Re-run `… install` from the updated tree.
4. Start the service.

GUI tweaks: `tomcat11w.exe //ES//PercussionProductionDTS` → Logging tab
(StdOutput / StdError / Log path).

## Log4j wiring under the service

Console start (`TomcatStartup.bat` → `catalina.bat` + `setenv.bat`) puts Log4j on
the classpath and sets the JUL bridge. The Windows **service** does not run
`setenv.bat`; Procrun starts `org.apache.catalina.startup.Bootstrap` directly.
The installers therefore set:

* classpath: `bootstrap.jar`, `tomcat-juli.jar`, `log4j2/lib/*`, `log4j2/conf`
* `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
* `-Dlog4j.configurationFile=<CATALINA_BASE>\log4j2\conf\log4j2-tomcat.xml`

Do not point `java.util.logging.config.file` at the Log4j2 XML (that property is
for JUL properties files only).

## Related

* Linux: `README-systemd.md`, `DTSProductionService.sh` / `DTSStagingService.sh`
* CMS Jetty Windows service is separate (`modules/perc-jetty`); do not conflate
  with DTS Tomcat logging.

