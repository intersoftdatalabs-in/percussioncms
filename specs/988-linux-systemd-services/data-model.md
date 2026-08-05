# Data Model: Linux systemd Service Management

This feature is **ops configuration**, not a CMS domain/database model. Entities are filesystem artifacts.

## ServiceIdentity

|           Field            |                      Description                      |
|----------------------------|-------------------------------------------------------|
| `serviceName`              | Unit/script basename (default `PercussionCMS`)        |
| `rxDir`                    | CMS install root                                      |
| `runAsUser` / `runAsGroup` | Process identity from `rx_user.id` / installer prompt |

## ServiceEnvironment (`/etc/default/<serviceName>`)

|                    Field                     |          Description          |
|----------------------------------------------|-------------------------------|
| `JAVA_HOME`, `JAVA`                          | JVM location                  |
| `JETTY_HOME`, `JETTY_BASE`, `JETTY_DEFAULTS` | Jetty layout                  |
| `JETTY_RUN`, `JETTY_PID`                     | Runtime dir and PID file path |
| `JETTY_USER`                                 | User for forking start        |
| `JETTY_ARGS`, `JAVA_OPTIONS`                 | Start arguments               |
| `JETTY_START_LOG`                            | Optional start log file path  |

## SystemdUnit (`/etc/systemd/system/<serviceName>.service`)

|           Directive            |                Maps to                 |
|--------------------------------|----------------------------------------|
| `Description`                  | Human label                            |
| `Type=forking`                 | Jetty fork model                       |
| `PIDFile=`                     | `JETTY_PID`                            |
| `EnvironmentFile=`             | `/etc/default/<serviceName>`           |
| `User=` / `Group=`             | `JETTY_USER` (or explicit)             |
| `ExecStart=` / `ExecStop=`     | Product start/stop commands            |
| `TimeoutStartSec=`             | Long upgrade-safe start (default 1800) |
| `StandardOutput/Error=journal` | Journal visibility                     |

## InitdScript (fallback)

Existing `/etc/init.d/<serviceName>` generated from the CMS Jetty start-helper template
(`jetty/defaults/bin/rxjetty.sh` at install time via `install-jetty-service.sh`) —
unchanged **role** when systemd path not used (helper still backs `ExecStart` on the
systemd path).

> **Inventory note (#1975):** the source template path
> `modules/perc-jetty/src/main/jetty/defaults/bin/rxjetty.sh` is **missing on main**
> (deleted with legacy `system/Tools/jetty` in Jetty 12 restructure). See
> [inventory.md](./inventory.md) Gap A. DTS generates its init helper from
> `catalina.sh` instead of a separate product template.

## State transitions (operator)

```text
[not installed] --install(systemd)--> [installed, disabled]
[installed] --enable--> [enabled]
[enabled|installed] --start--> [active]
[active] --stop--> [inactive]
[installed] --uninstall--> [not installed]
```

