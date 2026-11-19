---
id: admin-server-ops
title: Server Operations
description: Start, stop, logs, and operational basics for Percussion CMS 8.2
version: "8.2"
order: 44
tags: [admin, operations]
---

# Server Operations

Day-to-day control of the CMS process, logs, and configuration surfaces.

## Process control

Installations typically register a platform service:

| Platform | Common control |
|----------|----------------|
| Windows | Windows Service for the CMS (and DTS if installed) |
| Linux | systemd unit and/or classic init scripts shipped under installer `Linux` trees |
| Developer hosts | Foreground Jetty start scripts under the install tree |

Always stop services cleanly before upgrades or invasive config edits.

## Logs

Primary server logs live under the install’s Jetty base, commonly:

- `jetty/base/logs/` (server and application logs)

Delivery Tier (DTS) services each write their own log file under the Tomcat
base, so a failing widget (poll, feed, metadata, form, comments, membership)
can be diagnosed without tailing console output:

- `Deployment/Server/logs/<service>.log` — one file per service, e.g.
  `polls.log`, `metadata.log`, `comments.log`, `feeds.log`, `forms.log`,
  `membership.log`
- `Deployment/Server/logs/catalina.log` / `localhost.log` — container-level
  Tomcat output (startup, connector, deployment messages)

When filing bugs, attach relevant log excerpts (not secrets) and note CMS version from
`Version.properties` or the About dialog.

## Configuration surfaces

| Area | Examples |
|------|----------|
| Server properties | Feature flags, runtime toggles under server config |
| Jetty | Connectors, TLS, thread pools |
| Database | JDBC settings established at install / reconfig |
| Packages | Deployed `.ppkg` components and their configs |
| Custom extensions | JARs and registrations under extension paths |

Prefer change control: edit → restart if required → smoke test → document.

## JVM security defaults (CVE-2026-114938)

**CVE-2026-114938** ("Java4Shell") is an out-of-bounds heap write in the HotSpot JVM's
AVX-512 VBMI-accelerated `java.util.Base64.Decoder` intrinsic (`_base64_decodeBlock`). It
affects OpenJDK, Eclipse Temurin, Oracle Java SE, and Corretto **17.0.6–17.0.21**,
**21.0.0–21.0.13**, **25.0.0–25.0.5**, and **27.0.0–27.0.1** on **x86-64** processors
with AVX-512 VBMI support (`avx512vbmi` + `bmi2`). ARM64 systems and older x86-64 CPUs
without AVX-512 VBMI are not affected.

### Permanent fix

Upgrade the host (or container) JDK/JRE runtime used by CMS and DTS to a patched release
— **JDK `21.0.13.1` or later** (for example Eclipse Temurin `21.0.13.1+2`), or `17.0.21.1+`,
`25.0.5.1+`, or `27.0.1.1+` — then restart the CMS and DTS services.

### Shipped defense-in-depth flag (`-XX:-UseBASE64Intrinsics`)

`-D` Java system properties cannot disable HotSpot JIT intrinsics. Starting with the
fix for GH-6664, 8.2 ships **`-XX:-UseBASE64Intrinsics`** by default across CMS and DTS:

- `-XX:-UseBASE64Intrinsics` is a standard HotSpot `{product}` flag accepted on both
  x86-64 and ARM64 (no `-XX:+UnlockDiagnosticVMOptions` required) and forces safe
  fallback to the non-vectorized Java decoder.
- Avoid `-XX:UseAVX=2` in portable configs (causes JVM startup failure on ARM64), and
  never pass `-XX:DisableIntrinsic=_base64_decodeBlock` without
  `-XX:+UnlockDiagnosticVMOptions` (causes JVM startup failure).

| Surface | Default configuration location | Notes for existing / un-upgraded installs |
|---------|-------------------------------|-------------------------------------------|
| **CMS (Jetty)** — Windows service, Linux systemd/init.d, `StartJetty` | `jetty/defaults/start.d/jvm.ini` (under `--exec`) | Add `-XX:-UseBASE64Intrinsics` on its own line below `--exec` in `jetty/base/start.d/jvm.ini`. Do **not** put it only in `/etc/default/PercussionCMS` `JAVA_OPTIONS` or outer `StartJetty`, which apply only to the `start.jar` bootstrap JVM rather than the forked server JVM. Once on JDK `21.0.13.1+`, operators may re-enable acceleration with `-XX:+UseBASE64Intrinsics` in `jetty/base/start.d/jvm.ini`. |
| **DTS (Tomcat)** — Linux service & foreground `TomcatStartup` | `Deployment/Server/bin/setenv.sh`, `setenv.bat`, `TomcatStartup.sh`, `TomcatStartup.bat`, `/etc/default/<ServiceName>` (`CATALINA_OPTS`) | Ensure `CATALINA_OPTS` includes `-XX:-UseBASE64Intrinsics` (before any operator override) and restart DTS. |
| **DTS (Tomcat)** — Windows service (`Procrun`) | `Deployment/Server/DTSProductionService.bat`, `DTSStagingService.bat` (`++JvmOptions`) | Procrun runs in `--StartMode jvm` and bypasses `setenv.bat`. For existing services, append the flag in place and restart: `tomcat11.exe //US//PercussionProductionDTS ++JvmOptions "-XX:-UseBASE64Intrinsics"` (and `PercussionStagingDTS`). |
| **Container images** | `docker/cms/Dockerfile`, `docker/matrix/Dockerfile` (`JDK_JAVA_OPTIONS`) | Defense-in-depth if a cached image layer precedes Temurin `21.0.13.1+2`. |

### Verification

Because CMS ships with `-XX:+DisableAttachMechanism` in `jetty/defaults/start.d/jvm.ini`,
`jcmd <pid> VM.flags` is refused on the CMS process. Verify `-XX:-UseBASE64Intrinsics` on
the running server JVM command line instead (after restarting the service):

- **Linux / macOS:** `ps -ef | grep UseBASE64Intrinsics`
- **Windows (PowerShell):** `Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='tomcat11.exe'" | Select-Object ProcessId, CommandLine`

## Health checks

- Login page responds.
- Authenticated navigation works.
- Representative content open/save succeeds.
- Publish job completes to a staging location.
- Disk space for repository, temp, and publish targets remains healthy.

## Ports & paths

See [Ports & paths reference](id:reference-ports-paths) for common locations. Exact ports are
**install-time choices** — do not assume a single fixed port across all environments.

## Related

- [Authentication, MFA, and Recovery Readiness](id:admin-authentication-mfa)
- [Installation Overview](id:install-overview)
- [Upgrade Overview](id:upgrade-overview)
