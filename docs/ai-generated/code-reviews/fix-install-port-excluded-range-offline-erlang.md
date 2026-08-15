## Summary

`InstallUtil.portAvailable` (used as the inverse of “CMS/DTS/Derby appears
running”) treated every failed TCP bind as a live listener. On Windows hosts
whose dynamic TCP range overlaps Hyper-V **excluded port ranges**,
`ServerSocket(0)` can issue a port that binds once and then refuses every later
bind. Offline install gates then aborted as if DTS were up. The same bind-only
probe also still lost the GH-2779 UDP race when both bind phases failed.

The patch adds a third phase: if exclusive and `SO_REUSEADDR` binds both fail,
confirm a TCP listener with a short connect to loopback and other local
interface addresses. Bind failures that are not accepts (excluded ranges,
residual UDP races) report available. Tests retry port allocation until a
post-close rebind succeeds, and cover the new listen-confirm helper.

## Scope

- Base: `origin/main` @ `dbae733efb`
- Head: working tree (install-port files only; `patterns.md` not in this change)
- Files: 3 (`modules/utils/src/main/java/com/percussion/install/InstallUtil.java`,
  `modules/utils/src/test/java/com/percussion/install/InstallUtilRunningServerTest.java`,
  this report)
- Prior report: `fix-install-port-tcp-detection-erlang.md` (GH-2779 TCP-only bind)
- Memory patterns hit: installer port-detection false positive; tests must
  exercise behavior not only happy-path bind success

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

### Notes

- **Call-site semantic** — `checkTomcatServerRunning`, `checkServerRunning`, and
  `isDerbyRunning` all use `!portAvailable` as “process is listening”.
  `isBindableTcpPort` is a separate bind-only helper and is unchanged. Phase 3
  aligns `portAvailable` with the running-gate meaning without changing the
  bind-to-choose-a-port path.
- **LAN-only listeners** — connect probes walk up, non-link-local addresses so a
  Tomcat bound only to a NIC IP is still detected. Loopback is tried first
  (typical `0.0.0.0` / dual-stack product bind).
- **Behavioral coverage** — `isLocalTcpPortAccepting_tracksListener` proves the
  new helper; `findDistinctFreePorts_remainAvailableAfterRelease` plus the
  existing placeholder/literal offline tests cover the original false-positive;
  `portAvailable_roundTrip` and bound-connector tests still cover the positive
  (running) direction.
- **Build evidence** — `cd modules/utils && ..\..\mvnw.cmd clean install` →
  **BUILD SUCCESS**. `InstallUtilRunningServerTest`: Tests run: 11, Failures: 0.
  Focused class rerun 8/8 green. No new compiler warnings on the touched files
  (pre-existing javadoc warning baseline on the module is unchanged).
- **Cross-platform path review** — N/A. No filesystem path joins, temp hardcodes,
  or path-string assertions added. Sockets use `InetSocketAddress` / NIO-unrelated
  `java.net`.
- **Product documentation** — N/A. Internal install probe; no operator-facing
  procedure, config key, or UI change. Fewer false “DTS is running” blocks is a
  bugfix of existing gate behavior.

### Non-blocking

- Phase 3 may spend up to `200ms` per local address when a NIC blackholes SYNs
  instead of RST. Install checks a handful of ports; acceptable. Do not cache
  interface lists across process lifetime without invalidation (adapters come
  and go).
