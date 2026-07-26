# Contract: Percussion CMS systemd unit template

**Consumer**: Linux operators, `install-jetty-service.sh`  
**Provider**: `modules/perc-jetty` service packaging  
**Related**: FR-001, FR-004, FR-005, FR-007, SC-005

## Required unit keys (must appear in shipped template or generated unit)

|          Key          |                                      Constraint                                      |
|-----------------------|--------------------------------------------------------------------------------------|
| `[Unit] Description=` | Non-empty                                                                            |
| `After=`              | Includes `network.target` (or network-online as product chooses)                     |
| `[Service] Type=`     | `forking`                                                                            |
| `PIDFile=`            | Non-empty path; must match env `JETTY_PID` after install                             |
| `EnvironmentFile=`    | `-` optional prefix allowed; path `/etc/default/%N` or concrete service default file |
| `ExecStart=`          | Invokes product start (init script or jetty.sh start)                                |
| `ExecStop=`           | Invokes product stop                                                                 |
| `TimeoutStartSec=`    | Integer ≥ `900` (15 min) recommended; default **1800**                               |
| `User=`               | Present or documented as set by installer substitution                               |
| `StandardOutput=`     | `journal`                                                                            |
| `StandardError=`      | `journal`                                                                            |
| `[Install] WantedBy=` | `multi-user.target`                                                                  |

## Installer contract

|     Behavior      |                                                  Requirement                                                   |
|-------------------|----------------------------------------------------------------------------------------------------------------|
| systemd detected  | Install unit under `/etc/systemd/system/<name>.service`, `daemon-reload`, `enable` (start optional/documented) |
| systemd path      | MUST NOT also enable init.d via chkconfig/update-rc.d                                                          |
| no systemd        | Use init.d path (existing)                                                                                     |
| uninstall systemd | `disable --now` (if active), remove unit file, `daemon-reload`                                                 |
| custom name       | All artifacts use the same `SERVICE_NAME`                                                                      |

## Non-goals

- Kubernetes probes
- Windows service contract

