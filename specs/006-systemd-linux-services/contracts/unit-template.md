# Contract: percussioncms@.service Unit Template

**Date**: 2026-07-11
**Spec**: [spec.md](./spec.md)
**Data model**: [data-model.md](./data-model.md)

This contract specifies the rendered form of the systemd unit template shipped at `modules/perc-distribution-tree/src/main/resources/systemd/percussioncms@.service.template`. The template uses `${VAR}` syntax expanded at install time by the installer; the rendered file is a valid systemd unit consumable by `systemctl`.

## Rendered form (per-instance `<instance>` is `%i`)

```ini
[Unit]
Description=Percussion CMS (%i)
Documentation=https://percussioncmshelp.intsof.com/
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
EnvironmentFile=/etc/percussion/cms-%i.env
WorkingDirectory=${PERC_ROOT}
ExecStart=${JAVA} ${JAVA_OPTIONS} -jar ${PERC_ROOT}/Jetty/base/start.jar ${JETTY_ARGS}
ExecStop=${PERC_ROOT}/Jetty/base/stop.sh
PIDFile=${JETTY_PID}
Restart=on-failure
RestartSec=30s
StartLimitBurst=5
StartLimitIntervalSec=600s
TimeoutStartSec=300
TimeoutStopSec=120
User=${JETTY_USER}
Group=${JETTY_USER}
KillMode=mixed
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
PrivateDevices=true
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true
RestrictSUIDSGID=true
LockPersonality=true
RestrictRealtime=true
RestrictNamespaces=true
ReadWritePaths=${PERC_ROOT} ${JETTY_RUN}
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

*Note: The exact `ExecStart=` will be verified against the actual Jetty 12 entry point at implementation time. Two candidates exist today: `StartJetty.sh` and `start.jar` (via `start.jar`). The tasks phase picks the working one on a clean VM and freezes the contract.*

## Validation

A unit is "valid" when:

1. `systemd-analyze verify /etc/systemd/system/percussioncms@<instance>.service` exits 0.
2. `systemctl daemon-reload` does not emit warnings for the unit.
3. `systemctl start percussioncms@<instance>.service` returns 0 within `TimeoutStartSec`.
4. `systemctl is-active percussioncms@<instance>.service` returns `active`.
5. `journalctl -u percussioncms@<instance>.service -n 50` returns non-empty output containing CMS startup banner.
6. `systemctl stop percussioncms@<instance>.service` returns 0 within `TimeoutStopSec`.
7. `systemctl status percussioncms@<instance>.service` shows `enable` state set by the install (`enabled` in `systemctl is-enabled`).

## Versioning

- The unit template is part of the distribution; versioned with the CMS release.
- Backward-compat rule: a unit shipped with release N MUST work with a runtime installed by release N or any later release on the supported-platform matrix.
- Forward-compat rule: a unit shipped with release N+1 MUST NOT be installed over a release-N runtime without the operator running the upgrade installer.

## Failure modes (must be diagnosable via journal)

| Symptom | Likely cause | Journal hint |
|---------|--------------|--------------|
| `start request repeated too quickly` | JVM crash loop | `Main process exited, code=exited, status=1/FAILURE` |
| `Failed at step GROUP spawning ...` | `User`/`Group` missing on host | `nss-systemd` / `getpwnam` errors |
| `Failed to read environment file` | Env file missing or wrong mode | `openat(2) ... Permission denied` |
| Unit stays `activating` past `TimeoutStartSec` | Jetty hang on startup | `Jetty server starting...` stalls at a module |
| `Service hold-off time over, scheduling restart` | Repeated failures (expected) | `Scheduled restart job` |

## Migration contract (consumed by `install-systemd-units.sh`)

The migration script's input is the legacy init.d state of a host; its output is the systemd state defined by this contract. The contract is:

**Input probes**:
- `/etc/init.d/<name>` files matching the legacy patterns in [data-model.md](./data-model.md#entity-legacy-initd-script-deleted-in-this-release)
- `/etc/rc?.d/[SK]??<name>` symlinks
- `/etc/default/<name>` env files
- `chkconfig --list` / `update-rc.d --list` (whichever exists)

**Output guarantees**:
- All input scripts and symlinks are removed.
- `chkconfig --del <name>` (or `update-rc.d -f <name> remove`) has been run.
- `/etc/systemd/system/percussioncms@<instance>.service` exists and passes `systemd-analyze verify`.
- `/etc/percussion/cms-<instance>.env` exists with mode `0640`.
- `systemctl daemon-reload` has been run.
- `systemctl enable --now percussioncms@<instance>.service` has been run and returned 0.
- `systemctl is-active percussioncms@<instance>.service` returns `active`.
- The exit code of the migration script reflects the final state: 0 = all instances migrated, 1 = at least one instance failed (partial-migration recovery is then possible per FR-011 on the next run).
