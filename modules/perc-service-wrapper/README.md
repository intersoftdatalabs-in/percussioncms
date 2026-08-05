# perc-service-wrapper

Process start/stop control jar for Percussion CMS and co-located Delivery Tier
Service (DTS) processes. Main class: `com.percussion.wrapper.PSServiceWrapper`.

**Status (8.2.x / GH-2067):** **USED** on supported classic Linux SysV/init.d
service install paths. The jar is staged into shipping CMS and DTS layouts and
invoked by Linux service scripts. It is **not** the modern Jetty native systemd
entry point (that lives under `modules/perc-jetty`). Do **not** delete this
module or its packaging copies until the init.d path is formally deprecated
(see GH-1976; dual-ship policy GH-962 / GH-1978).

## Usage

```text
java -jar perc-service-wrapper.jar [options...] [jetty properties...] [jetty configs...]
```

The wrapper discovers the distribution root from the jar location unless
`-Drxdeploydir` is set. It can start/stop/status:

- Jetty CMS (`JettyStartWrapper`)
- Production DTS (`DtsStartWrapper`)
- Staging DTS (`DtsStartWrapper`)

Extra options are forwarded to Jetty `start.jar` when applicable (for example
`--list-config`). Use `--jettyHelp` for Jetty's own help.

Full CLI flags: `src/main/resources/com/percussion/wrapper/usage.txt`.

## Live entry points (8.2)

|                          Surface                           |                                                                           Role                                                                           |
|------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `system/release/installer/Linux/percussion-service.sh`     | SysV service script; default `SVC_WRAPPER=$PERC_ROOT/perc-service-wrapper.jar`; `start`/`stop`/`status` run `java -jar … --start|--stop|--status`        |
| `system/release/installer/Linux/install-service.sh`        | Installs `/etc/init.d/<ServiceName>` from the percussion-service template and writes `/etc/default/<ServiceName>` with `SVC_WRAPPER` / `SVC_WRAPPER_CMD` |
| `modules/perc-distribution-tree/pom.xml`                   | Dependency + copy to CMS assembly as `perc-service-wrapper.jar` at distribution root                                                                     |
| `deliverytiersuite/.../delivery-tier-distribution/pom.xml` | Stages `perc-service-wrapper.jar` into the DTS distribution layout                                                                                       |

## Not used by (inventory GH-2067)

Static inventory found **no** callers in:

- Modern Jetty systemd dual-ship (`modules/perc-jetty` `install-jetty-service.sh`,
  `percussion-cms.service.in`, `rxjetty.sh`) — preferred Linux service path on
  systemd hosts
- Windows Procrun / Jetty service install under `modules/perc-jetty`
- Docker / `perc-devctl` scripts
- QA automation (`modules/perc-qa-automation`)

Those paths start Jetty (and related processes) without this jar. Coexistence is
intentional while init.d remains dual-shipped.

## Overlap with Jetty systemd (GH-962)

|                Path                |                                  Mechanism                                  | Wrapper jar  |
|------------------------------------|-----------------------------------------------------------------------------|--------------|
| Classic Linux init.d (this module) | `install-service.sh` → init.d → `java -jar perc-service-wrapper.jar`        | **Required** |
| Modern Jetty service               | `install-jetty-service.sh` → systemd unit and/or init helper → `rxjetty.sh` | **Not used** |

Removal of this module would break the classic Linux service install path. Defer
removal until ops signs off init.d deprecation (GH-1976) after dual-ship soak
(GH-1978).

## Building

From this module directory (JDK 21, repo Maven wrapper):

```bash
../../mvnw clean install
```

Windows:

```bat
..\..\mvnw.cmd clean install
```

