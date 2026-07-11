# Quickstart: Systemd Linux Service Scripts (Replace init.d)

**Date**: 2026-07-11
**Spec**: [spec.md](./spec.md)
**Plan**: [plan.md](./plan.md)

This quickstart is the runnable validation guide for the feature. Each scenario is end-to-end runnable on the supported-platform matrix (Ubuntu 22.04+, Debian 12+, RHEL 9+, Rocky/AlmaLinux 9+, Amazon Linux 2023+).

## Prerequisites

- A clean Linux VM (or container) with systemd as PID 1, JDK 21, and the Percussion CMS distribution tree staged at `${PERC_ROOT}` (or a fresh CMS install built from this branch).
- For container-based testing: `docker run --privileged -v /sys/fs/cgroup:/sys/fs/cgroup:ro -ti ubuntu:22.04 /sbin/init` then `apt-get install -y openjdk-21-jdk` and stage the CMS distribution.
- For CI: the `docker/systemd-test/` harness (added by this feature) runs all scenarios automatically.

## Scenario 1 — Fresh install on a systemd host

**Goal**: Verify FR-001 / FR-002 / Story 1.

```bash
# 1. As root, run the installer
sudo bash modules/perc-distribution-tree/scripts/install-systemd-units.sh \
    --perc-root /opt/percussion/perc-cms \
    --user percussion \
    --instance default

# Expected output includes a "Migrated 1 instance" summary row.

# 2. Verify the unit is installed
systemctl list-unit-files | grep percussion
# Expected: percussioncms@.service    enabled

# 3. Verify it is active
systemctl is-active percussioncms@default
# Expected: active

# 4. Verify the CMS is responding
curl -sf http://localhost:9992/Rhythmyx/Authentication/login.html | head -5
# Expected: HTML output (the CMS login page)

# 5. View logs via journalctl
journalctl -u percussioncms@default -n 50 --no-pager
# Expected: CMS startup banner visible

# 6. Reboot and verify autostart
sudo reboot
# After reboot:
systemctl is-active percussioncms@default
# Expected: active
```

## Scenario 2 — Upgrade from a legacy init.d install

**Goal**: Verify FR-003 / Story 2.

```bash
# 1. Seed a legacy init.d install (simulates an existing customer)
sudo cp system/release/installer/Linux/percussion-service.sh /etc/init.d/PercussionCMS
sudo ln -s /etc/init.d/PercussionCMS /etc/rc2.d/S99PercussionCMS
sudo ln -s /etc/init.d/PercussionCMS /etc/rc3.d/S99PercussionCMS
sudo service PercussionCMS start   # starts via legacy script

# 2. Verify the legacy install is running
service PercussionCMS status
# Expected: legacy start succeeded

# 3. Run the upgrade installer (the same installer used in Scenario 1)
sudo bash modules/perc-distribution-tree/scripts/install-systemd-units.sh \
    --perc-root /opt/percussion/perc-cms \
    --user percussion \
    --instance default

# Expected: "Detected legacy init.d installation for instance 'default'; migrating."

# 4. Verify legacy artifacts are gone
ls /etc/init.d/ | grep -i percussion
# Expected: empty (or no percussion-related entries)

ls /etc/rc?.d/ | grep -i percussion
# Expected: empty

# 5. Verify the systemd unit is now active
systemctl is-active percussioncms@default
# Expected: active

systemctl list-unit-files | grep percussion
# Expected: percussioncms@.service    enabled
```

## Scenario 3 — Operator lifecycle (systemctl + journalctl)

**Goal**: Verify FR-006a / Story 3.

```bash
# Stop
sudo systemctl stop percussioncms@default
# Expected: exits 0 within TimeoutStopSec (120s default)
systemctl is-active percussioncms@default
# Expected: inactive

# Start
sudo systemctl start percussioncms@default
# Expected: exits 0 within TimeoutStartSec (300s default)
systemctl is-active percussioncms@default
# Expected: active

# Restart
sudo systemctl restart percussioncms@default
# Expected: active within TimeoutStartSec + TimeoutStopSec

# Status
systemctl status percussioncms@default
# Expected: shows active (running), recent journal entries, PID

# Disable / re-enable autostart
sudo systemctl disable percussioncms@default
sudo systemctl is-enabled percussioncms@default
# Expected: disabled
sudo systemctl enable percussioncms@default
# Expected: enabled

# Crash and verify auto-restart
sudo kill -9 $(cat /var/run/rxjetty/perc-cms/rxjetty.pid)
# Wait 30s, then:
systemctl is-active percussioncms@default
# Expected: active (RestartSec=30s, Restart=on-failure)
journalctl -u percussioncms@default -n 20
# Expected: "Main process exited" followed by "Scheduled restart job"

# Crash-loop verify
for i in 1 2 3 4 5 6; do sudo kill -9 $(cat /var/run/rxjetty/perc-cms/rxjetty.pid); sleep 2; done
# After StartLimitBurst=5 failures, systemd stops trying:
systemctl is-active percussioncms@default
# Expected: failed
sudo systemctl reset-failed percussioncms@default
# Now restart works again.
```

## Scenario 4 — Multi-instance (FR-004a)

**Goal**: Verify two CMS instances on one host.

```bash
# 1. Stage a second instance under a different PERC_ROOT
sudo mkdir -p /opt/percussion/perc-cms-2
# (copy or symlink the install tree; the actual second-instance provisioning is out of scope
# for v1 of this feature — for now, a copy of the install tree is sufficient)

# 2. Register the second instance
sudo bash modules/perc-distribution-tree/scripts/install-systemd-units.sh \
    --perc-root /opt/percussion/perc-cms-2 \
    --user percussion \
    --instance instance2

# 3. Start both
sudo systemctl start percussioncms@default
sudo systemctl start percussioncms@instance2

# 4. Verify both are active and on different ports
systemctl list-units --type=service | grep percussion
# Expected: both percussioncms@default.service and percussioncms@instance2.service active
```

## Scenario 5 — DTS staging and production

**Goal**: Verify FR-008 / Story 4.

```bash
# 1. Install DTS (separate distribution)
sudo bash deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/scripts/install-systemd-units.sh \
    --instance staging
sudo bash deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/scripts/install-systemd-units.sh \
    --instance production

# 2. Verify
systemctl list-units --type=service | grep dts
# Expected: percussiondts-staging@staging.service and percussiondts-production@production.service active
```

## Scenario 6 — Idempotency (FR-010)

**Goal**: Verify the installer is safe to re-run.

```bash
# Re-run the installer
sudo bash modules/perc-distribution-tree/scripts/install-systemd-units.sh \
    --perc-root /opt/percussion/perc-cms \
    --user percussion \
    --instance default
# Expected: "Instance 'default' already migrated; no action taken." Exit 0.

# Re-run after manually deleting the unit (simulates partial failure)
sudo rm /etc/systemd/system/percussioncms@default.service
sudo systemctl daemon-reload
sudo bash modules/perc-distribution-tree/scripts/install-systemd-units.sh ...
# Expected: re-creates the unit, restarts, exits 0.
```

## Scenario 7 — Out-of-support host (negative test)

**Goal**: Verify the installer refuses non-systemd hosts with a clear error.

```bash
# In a chroot or non-systemd container:
bash modules/perc-distribution-tree/scripts/install-systemd-units.sh ...
# Expected: exit 2, "systemd is not PID 1; this installer does not support non-systemd hosts. Upgrade is required via a supported distribution."
```

## Validation summary (maps to Success Criteria)

| Scenario | SC verified |
|----------|-------------|
| 1 | SC-001 (install time within 10% of baseline), SC-003 (lifecycle commands), SC-004 (no init.d artifacts) |
| 2 | SC-002 (upgrade without intervention; init.d absent after) |
| 3 | SC-003 (full lifecycle), SC-006 (security hardening) |
| 4 | SC-003 (multi-instance) |
| 5 | SC-003 (DTS coverage) |
| 6 | SC-002 (idempotency), FR-010 / FR-011 |
| 7 | SC-004 (supported-platform matrix enforced) |

## Troubleshooting

- `systemd-analyze verify` warnings — run with `--man=true` for human-readable output; check for missing `EnvironmentFile=` keys.
- `Failed to read environment file: Permission denied` — env file mode is not `0640`, or owner group does not match the runtime group.
- `start request repeated too quickly` — see the journal for the underlying failure; usually a bad `JAVA_OPTIONS` or a missing `JETTY_HOME`.
- Multi-instance port conflict — the operator must configure per-instance ports via `JAVA_OPTIONS` or `rxconfig/Server/`. This feature does not auto-assign ports.

## Reference

- [plan.md](./plan.md) — implementation plan
- [research.md](./research.md) — Phase 0 research
- [data-model.md](./data-model.md) — entities
- [contracts/unit-template.md](./contracts/unit-template.md) — unit template contract
- [contracts/env-file.md](./contracts/env-file.md) — env file contract
- [contracts/installer-script.md](./contracts/installer-script.md) — installer script contract
