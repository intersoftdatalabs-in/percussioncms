# Contract: install-systemd-units.sh

**Date**: 2026-07-11

This contract specifies the behavior of the installer script `install-systemd-units.sh` shipped in `modules/perc-distribution-tree/scripts/`. The script handles fresh install, upgrade-from-init.d, and idempotent re-run.

## Synopsis

```text
install-systemd-units.sh [--perc-root PATH] [--user USER] [--instance NAME] [--dry-run] [--force] [--help]
```

- `--perc-root PATH`: install root. Default: detected from `objectstore.properties` or `/etc/default/<name>`.
- `--user USER`: runtime user. Default: detected from `rx_user.id` or `/etc/default/<name>`.
- `--instance NAME`: instance name. Default: `default` for a single-instance install, `instance2` for the second, etc.
- `--dry-run`: print what would be done; do not modify the system.
- `--force`: overwrite existing env file (default: prompt).
- `--help`: print usage.

## Phases (in order)

1. **Probe**: read existing state — `/etc/init.d/*`, `/etc/rc?.d/*`, `/etc/default/*`, `/etc/systemd/system/*`, `/etc/percussion/*`.
2. **Detect**: build the in-memory `Instance Record` list per [data-model.md](./data-model.md#entity-instance-record-in-memory-only).
3. **Validate**: for each instance, verify `PERC_ROOT` exists, `rxconfig/Server/objectstore.properties` exists, `rx_user.id` exists (or `--user` was supplied).
4. **Render**: write the env file (`0640 root:<group>`) and the rendered unit file (under `/etc/systemd/system/`).
5. **Migrate (upgrade only)**: stop the legacy init.d service (`service <name> stop` or `/etc/init.d/<name> stop`), deregister (`chkconfig --del` / `update-rc.d -f remove`), remove the symlinks, remove the legacy script.
6. **Enable**: `systemctl daemon-reload && systemctl enable percussioncms@<instance>.service`.
7. **Start**: `systemctl start percussioncms@<instance>.service`. Wait up to `TimeoutStartSec` for `is-active` to return `active`.
8. **Report**: print a summary table — per instance: detected source, env file path, unit path, start status.

## Idempotency rules (FR-010)

- Re-running with no changes: detects the existing systemd state, validates, prints "already migrated", exits 0.
- Re-running after partial migration (init.d removed, no unit installed): re-creates the env file and unit, starts, exits 0.
- Re-running with conflicting state (unit exists, env file missing): recreates the env file, restarts the unit, exits 0.
- Re-running with `--force` over an existing env file: backs up the existing file to `cms-<instance>.env.bak.<timestamp>`, writes the new one, restarts the unit.

## Exit codes

| Code | Meaning |
|------|---------|
| 0 | All instances migrated successfully (or already migrated). |
| 1 | At least one instance failed migration; others may have succeeded. Summary table shows which. |
| 2 | Invalid arguments or missing prerequisites (e.g. not running as root, systemd not PID 1, no `PERC_ROOT`). |
| 3 | Idempotency violation: legacy init.d state and systemd state both exist for the same instance. Operator must run with `--force` to resolve. |

## Pre-conditions

- Must be run as root (or via `sudo`).
- `systemctl` MUST be on `PATH`.
- PID 1 MUST be `systemd` (verified via `pidof systemd` or `cat /proc/1/comm`).

## Post-conditions

- For each detected instance:
  - Legacy init.d script and run-level symlinks: absent.
  - `/etc/percussion/cms-<instance>.env`: present, mode `0640`.
  - `/etc/systemd/system/percussioncms@<instance>.service`: present, passes `systemd-analyze verify`.
  - `systemctl is-active percussioncms@<instance>.service`: `active`.
  - `journalctl -u percussioncms@<instance>.service -n 5`: non-empty.

## Failure recovery (FR-011)

If the script exits non-zero partway through, the operator re-runs it (with or without `--force`). The probe phase re-derives state from disk and converges to the post-condition set.

## Logging

All actions are logged to `journalctl -t install-systemd-units` (using `systemd-cat` or `logger -t`). The summary table is also printed to stdout.
