# Research: Linux systemd Service Management (988)

## R1 — Why init.d fails under systemd

**Decision**: Treat auto-generated LSB units from init.d as **insufficient** for CMS; ship a **native** unit.

**Rationale**: systemd’s `systemd-sysv-generator` wraps SysV scripts. For forking Java/Jetty starts that take a long time (post-upgrade DB/package work), default start timeouts and weak readiness semantics produce `start operation timed out` while the process continues, empty-ish journals, and broken stop/status — matching #962.

**Alternatives considered**:
- Only raise timeouts via drop-in for generated LSB unit — fragile, still poor journal/readiness.
- Type=oneshot — wrong for long-running server.

## R2 — Service type and PID

**Decision**: `Type=forking` with `PIDFile=` set to the same path as `JETTY_PID` in `/etc/default/<service>` (today: under `/var/run/rxjetty/.../rxjetty.pid`).

**Rationale**: Existing `rxjetty.sh` / Jetty scripts already fork and write a PID file; matches Jetty upstream sample unit pattern and current install script.

**Alternatives considered**:
- `Type=simple` + `ExecStart` foreground — requires start script changes and may break existing operators.
- `Type=notify` — needs sd_notify from the JVM or a wrapper; higher complexity, deferred.

## R3 — Start timeout for upgrades

**Decision**: Default `TimeoutStartSec=1800` (30 minutes). Document that sites with longer upgrades can use a systemd drop-in to increase further. `TimeoutStopSec` remain moderate (e.g. 120s) unless evidence needs more.

**Rationale**: #962 upgrade path exceeds default ~90s; 30 minutes covers typical package/DB upgrade windows without infinite hang masking.

**Alternatives considered**:
- `TimeoutStartSec=infinity` — hides permanent hung starts.
- Keep 90s — fails SC-002 / FR-005.

## R4 — Journal vs file logs

**Decision**: Set `StandardOutput=journal` and `StandardError=journal` on the unit; retain `JETTY_START_LOG` / Log4j file logs for detailed app output. Installer/docs tell operators to use both `journalctl -u <service>` and `JETTY_BASE/logs/`.

**Rationale**: Addresses “nothing in the journal” while preserving existing log4j rotation (see #939).

## R5 — Installer detection and coexistence

**Decision**:
```text
if systemd is active (systemctl available && /run/systemd/system exists)
  → install unit to /etc/systemd/system/<name>.service
  → daemon-reload, enable --now optional (enable without start by default; document start)
  → do NOT also chkconfig/update-rc.d the init.d script
else
  → existing init.d path
optional: install-jetty-service.sh ... install --initd  # force SysV
optional: install-jetty-service.sh ... install --systemd  # force systemd or fail
```

**Rationale**: Avoids dual-start; supports FR-003 fallback.

## R6 — Template shipping location

**Decision**: Place unit template under `modules/perc-jetty/src/main/jetty/service/` (alongside install scripts) and ensure perc-jetty assembly/distribution copies `service/` as today.

**Rationale**: Single ops package location; install script already roots from Jetty install layout.

## R7 — Testing without root systemd in CI

**Decision**: Structural tests parse the unit template for required keys (`Type=`, `PIDFile=`, `TimeoutStartSec=`, `EnvironmentFile=`, `ExecStart=`, `User=`). Installer logic extracted or asserted via file content / dry-run flags that print planned actions without writing to `/etc`.

**Rationale**: Constitution test discipline without privileged CI.

## R8 — DTS

**Decision**: Out of scope for 988; follow-up issue if needed.

**Rationale**: #962 narrative and comments center on PercussionCMS Jetty.
