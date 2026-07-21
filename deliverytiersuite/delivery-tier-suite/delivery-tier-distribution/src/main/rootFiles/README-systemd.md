# Percussion DTS Linux services (systemd)

Production and Staging DTS installers prefer a **native systemd unit** when
systemd is available (same approach as CMS Jetty / GH-962).

| Installer | Default service name |
|-----------|----------------------|
| `DTSProductionService.sh` | `PercussionProductionDTS` |
| `DTSStagingService.sh` | `PercussionStagingDTS` |

## Install

```bash
# From the DTS install root (directory containing this script and bin/catalina.sh)
sudo ./DTSProductionService.sh [ServiceName] install
sudo ./DTSStagingService.sh [ServiceName] install

# Flags (optional):
#   --systemd   require systemd
#   --initd     force classic SysV/init.d registration only
```

On systemd hosts:

```bash
sudo systemctl start PercussionProductionDTS
sudo systemctl status PercussionProductionDTS
journalctl -u PercussionProductionDTS -n 100 --no-pager
```

`TimeoutStartSec=1800` (30 minutes). Override with `systemctl edit <name>`.

Application logs: `$CATALINA_BASE/logs/` (and Log4j2 configs under `conf/perc/`).

## Uninstall

```bash
sudo ./DTSProductionService.sh [ServiceName] uninstall
sudo ./DTSStagingService.sh [ServiceName] uninstall
```

## Migration from init.d-only

1. Uninstall the old service  
2. Re-run install (picks systemd when available)  
3. `systemctl start <ServiceName>`

## Windows

`.bat` installers are unchanged (Tomcat Windows service).
