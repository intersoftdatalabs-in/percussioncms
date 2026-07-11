# Phase 0 Research: Systemd Linux Service Scripts (Replace init.d)

**Date**: 2026-07-11
**Spec**: [spec.md](./spec.md)
**Plan**: [plan.md](./plan.md)

## Decision 1 — systemd unit `Type=`

**Decision**: `Type=simple`.

**Rationale**: The CMS's runtime entry point is `modules/perc-jetty/src/main/jetty/StartJetty.sh`, which `exec`s the Java process and blocks until the JVM exits. systemd's `simple` type matches this exactly: systemd considers the unit "started" as soon as `ExecStart=` returns from fork, and the service stays "active" while the main PID lives. No `fork()` wrapper is involved, so `forking` would be wrong. `notify` would require Jetty to emit `sd_notify`, which it does not currently do; `Type=simple` avoids that wiring.

**Alternatives considered**:
- `Type=forking` — rejected; would require restructuring the start script to fork and detach.
- `Type=notify` — rejected; requires Jetty support not currently present; tracked as a future enhancement, not v1.
- `Type=oneshot` — rejected; the CMS is a long-running service, not a one-shot task.

---

## Decision 2 — `ExecStart=`, `ExecStop=`, and `PIDFile=`

**Decision**:
- `ExecStart=/bin/bash -c '${PERC_ROOT}/Jetty/base/StartJetty.sh'` (path resolved from `EnvironmentFile=`)
- `ExecStop=/bin/bash -c '${PERC_ROOT}/Jetty/base/StopJetty.sh'`
- `PIDFile=${JETTY_RUN}/rxjetty.pid` (Jetty already writes this; per `install-jetty-service.sh:194`)
- `KillMode=mixed` — `SIGTERM` to the main PID (per PIDFile), `SIGKILL` to remaining cgroup processes after `TimeoutStopSec=`

**Rationale**: Reuses the existing entry points without duplication. The env file (`EnvironmentFile=`) exposes `PERC_ROOT`, `JETTY_RUN`, etc., so the unit template is not hard-coded to a path.

**Alternatives considered**:
- A new `systemd-aware` start script that calls `sd_notify` — rejected for v1 (out of scope per Decision 1).

---

## Decision 3 — Restart policy (already resolved in clarification)

**Decision**: `Restart=on-failure`, `RestartSec=30s`, `StartLimitBurst=5`, `StartLimitIntervalSec=600s`.

**Rationale**: A hard JVM crash should bring the CMS back automatically, but a misconfigured DB / network should not produce a tight restart loop. 5 restarts in 10 minutes is enough to recover from transient failures; after that, systemd leaves the unit in `failed` state and the operator must intervene (matches Story 3 scenario 3).

---

## Decision 4 — Multi-instance detection algorithm

**Decision**: The migration script enumerates existing init.d installations by:

1. Scanning `/etc/init.d/` for entries matching `^[SK]?[0-9]{0,3}(percussion|rx|rxjetty|rhythmyx|perc)` (case-insensitive), excluding `catalina.sh` (Tomcat — not ours).
2. For each candidate, parsing `SERVER_DIR=`, `PERC_ROOT=`, or `/etc/default/<name>` for the install path and existing JVM options.
3. Mapping each to one `percussioncms@<instance>.service` unit, where `<instance>` defaults to `default` for the first install and `instance2`, `instance3`, … for subsequent installs.
4. Writing `/etc/percussion/cms-<instance>.env` from the parsed values; never overwriting an existing env file without prompting (FR-010 idempotency).

**Rationale**: The legacy `install-jetty-service.sh:131` already gates on `/etc/init.d/${SERVICE_NAME}`; the new migration script reuses that check and extends it to enumerate prefixes. Mapping to per-instance env files matches `EnvironmentFile=/etc/percussion/cms-%i.env` on the template unit.

**Alternatives considered**:
- A "let the operator rename" prompt at upgrade time — rejected; too fragile, breaks unattended upgrades.
- A separate `percussioncms-instances` registry file — rejected; the unit template + env file pair is the standard systemd idiom.

---

## Decision 5 — Environment file format & permissions

**Decision**: `/etc/percussion/cms-<instance>.env` is a `key=value` shell-sourced file with mode `0640`, owner `root:<runtime-group>`. The unit's `User=`/`Group=` are set to the same runtime user/group (extracted from the legacy `rx_user.id` or the install tree's owner).

**Rationale**: SC-006 requires "no world-writable env files, no log-injection vectors via `EnvironmentFile=`". Mode `0640` with group-restricted read satisfies that. systemd's `EnvironmentFile=` reads `key=value` pairs without shell expansion, so there is no injection vector; but the file MUST not contain `export FOO=$(...)` or backticks, and the migration script strips these defensively.

**Alternatives considered**:
- Dropping the env file and hard-coding values in the unit — rejected; FR-004 forbids hard-coding.
- systemd `LoadCredential=` (encrypted creds) — rejected; v1 only handles install paths / JVM opts, no secrets.

---

## Decision 6 — Hardening directives

**Decision**: Ship the unit template with:

```ini
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
```

**Rationale**: SC-006 (no security regressions). These directives harden the CMS process without breaking Jetty's need for network sockets (allowed by default) and read access to its install path (covered by `ReadWritePaths=` on `PERC_ROOT` and `JETTY_RUN`). Testing in CI will confirm Jetty still starts under these restrictions.

**Alternatives considered**:
- `DynamicUser=yes` — rejected; the runtime needs a fixed UID/GID to own install files.
- `CapabilityBoundingSet=` (zero) — rejected; Jetty needs `CAP_NET_BIND_SERVICE` for privileged ports; covered separately if needed.

---

## Decision 7 — Supported-platform matrix (initial draft)

**Decision**: Ship documentation listing:

- **Supported**: Ubuntu 22.04 LTS+, Debian 12+, RHEL 9+, Rocky Linux 9+, AlmaLinux 9+, Amazon Linux 2023+.
- **Out of support in this release**: RHEL 7 / CentOS 7 / Ubuntu 18.04 (no systemd or systemd too old).
- **Container notes**: PID 1 must be systemd for `systemctl` to work. Standard `docker run --privileged -v /sys/fs/cgroup:/sys/fs/cgroup:ro ubuntu:22.04 /sbin/init` is the documented pattern.

**Rationale**: Matches modern Linux support norms and matches the JDK 21 runtime baseline. Per Q1 (clarification), legacy init.d hosts are out of support in this release.

**Alternatives considered**:
- "Best-effort" support for RHEL 7 — rejected; init.d dropped this release.

---

## Decision 8 — Integration test harness

**Decision**: A new `docker/systemd-test/` harness using `ubuntu:22.04` with `/sbin/init` as PID 1 and the host's cgroups mounted. The test:

1. Installs the distribution tree under `/opt/percussion` (simulated).
2. Runs `install-systemd-units.sh`.
3. Asserts `systemctl is-active percussioncms@default` returns `active` and `journalctl -u percussioncms@default` shows CMS startup logs.
4. Restarts and verifies.
5. For upgrade path: seeds `/etc/init.d/PercussionCMS` symlinks, runs the migration, asserts init.d scripts are gone and the systemd unit is active.

**Rationale**: FR-012 requires an integration test that exercises `systemctl`. A privileged container with `/sbin/init` is the lightest-weight way to get real systemd on CI.

**Alternatives considered**:
- `systemd-nspawn` — considered; works on systemd hosts but less portable for GitHub Actions runners.
- Mocking `systemctl` — rejected; would not exercise the actual lifecycle semantics.

---

## Open Implementation Notes (passed to `/speckit.tasks`)

- Exact `TimeoutStartSec=` / `TimeoutStopSec=` values need empirical tuning against the actual Jetty startup time on a clean VM. Start with `TimeoutStartSec=300`, `TimeoutStopSec=120` and adjust if tests show they are too tight.
- The `/etc/percussion/` directory mode/owner policy (`0755 root:root` for the dir, `0640 root:<group>` for env files) needs cross-check against the runtime user having read access.
- ShellCheck on the new `install-systemd-units.sh` and `install-systemd-jetty-service.sh` scripts — add to CI if not already wired.
- The deletion of `system/release/installer/Linux` and `system/release/installer/unix` may also require updating Maven assembly descriptors / `perc-ant` install XMLs that reference these paths; the tasks phase must grep for references before deletion.
