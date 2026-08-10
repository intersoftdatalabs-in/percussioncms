---
id: admin-server-ops
title: Server operations
description: Start, stop, logs, and operational basics for Percussion CMS 8.2
version: "8.2"
order: 44
tags: [admin, operations]
---

# Server operations

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

- [Installation Overview](id:install-overview)
- [Upgrade Overview](id:upgrade-overview)
