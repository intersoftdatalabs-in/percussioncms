# Erlang review — 991-system-java-home US2 (DTS parity)

## Summary

US2 propagates the Phase 2 Java home resolution contract to the Delivery
Tier Service: dual-platform `resolve-java-home.{sh,bat}` copies land in
`delivery-tier-distribution/src/main/rootFiles/`, `TomcatStartup.{sh,bat}`
and `TomcatShutdown.{sh,bat}` source/call the helper instead of `cd JRE`
heuristics, and `DTSProductionService.{sh,bat}` / `DTSStagingService.{sh,bat}`
write the resolved home into `/etc/default/<service>` and the Windows Procrun
`--JavaHome`. Eleven tests in
`DtsJavaHomeScriptTest` plus the pre-existing `DtsServiceInstallScriptTest`
pass on JDK 21; module README documents the operator migration. No blocking
bugs; one nit on Tomcat* script working-directory assumption.

## Scope

- Base: `development`
- Head: `991-system-java-home` (uncommitted at review time, second PR)
- Files: 9 changed (3 new, 6 modified)
- Prior report: `docs/ai-generated/code-reviews/991-system-java-home-phase2-us1-erlang.md`
- Memory patterns hit: `cross-platform.dual-shell-bat (paired)`, `cross-platform.sourced-bash (sourced not executed)`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Issues

### Issue 1 -- Severity: nit

- File: `delivery-tier-distribution/src/main/rootFiles/TomcatStartup.sh:6`, `TomcatShutdown.sh:6`
- Description: Switched the installer-root derivation from `CURDIR=$(pwd)` to
  `SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"` so the
  install root is stable regardless of where the operator invokes the
  script from. This is consistent with the Jetty side and avoids a
  relative-to-cwd assumption.
- Suggestion: None — kept as documentation of intent. Behavior verified.
- Status: closed

## Cross-platform path review

- DTS resolver copies are byte-identical to the CMS Jetty copies (per the
  contract requirement to "avoid silent drift"). A future CI lint or shared
  packaging step could compare hashes; deferred to US5/Polish.
- `TomcatStartup.sh` / `TomcatShutdown.sh`: now use `BASH_SOURCE[0]`
  derivation of install root, portable on Linux/macOS.
- `TomcatStartup.bat` / `TomcatShutdown.bat`: use `%SCRIPT_DIR%\..` (the
  installer root) consistently.
- DTSProductionService.sh / .bat / StagingService.sh / .bat: call the
  helper at the same canonical location
  (`%CATALINA_HOME%\..

  call "%~dp0..\..\resolve-java-home.bat" "%~dp0..\.."`) — i.e., the
  install root that contains the resolve helper script. Producers and
  service installers agree on the install-root path.

- Tests: structural assertions only. No raw OS-path string checks.

## Non-portable pattern hits: none

No `C:\`, `/tmp`, `/var`, `/usr`, `/bin/sh` introduced into product code or
tests for paths.

## Behavioral test coverage

- `DtsJavaHomeScriptTest` — 6 structural tests covering the DTS resolve
  helper copies, Tomcat startup/shutdown sources/calls the helper, both
  service-install scripts source/call the helper, and Procrun `--JavaHome`
  is wired through to the resolved Java home.
- `DtsServiceInstallScriptTest` (existing) — 5 tests still pass.

## Follow-ups

- Add a `scripts/check-resolver-parity.sh` lint (US5/Polish) that fails when
  the Jetty and DTS resolve-java-home.{sh,bat} diverge from the contract.
- Behavioral tests for the resolve scripts (US5/Polish follow-up): the
  current structural tests are sufficient for CI but a host with a real
  Java 21 install could exercise the full precedence end-to-end.

