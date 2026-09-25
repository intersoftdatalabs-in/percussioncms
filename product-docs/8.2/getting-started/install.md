---
id: install-overview
title: Installation Overview
description: How to install Percussion CMS 8.2
version: "8.2"
order: 20
tags: [install, admin]
---

# Installation Overview

This page summarizes installing Percussion CMS 8.2 on supported platforms. Prefer official
installers from the project **Releases** page when operating a production or QA host.

## Supported platforms

Percussion CMS is a **cross-platform** product. Install, run, and administer on:

- **Windows** (server and developer workstations)
- **Linux**
- **macOS** (typically developer and evaluation hosts)

File paths, scripts, and services differ by OS; use the installer packages and service wrappers
shipped for your platform rather than hard-coding Unix-only paths.

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| **JDK 21** | Runtime and toolchain for the 8.2 line. Set `JAVA_HOME` to a JDK 21 installation. |
| **Supported RDBMS** (production) | Use the database drivers and schemas documented for your release. **New installs** default the product-managed embedded repository to multiuser **H2** (not Derby). External MySQL / SQL Server remain supported. Upgrades from product-managed Derby: see [Upgrade Overview](id:upgrade-overview). |
| **Disk & ports** | Enough space for install tree, content repository, and publish targets; free HTTP(S) ports (defaults vary by install; common developer CMS UI ports include the install-time configured Jetty ports). |
| **Permissions** | Installer/service account needs write access under the install directory and configured data/publish paths. |

## Obtain packages

1. Open the [GitHub Releases](https://github.com/intersoftdatalabs-in/percussioncms/releases) page for this repository.
2. Download the CMS distribution/installer for your platform and the matching **Delivery Tier Service (DTS)** packages if you use dynamic widgets (comments, forms, membership, metadata, polls, and related services).
3. Verify checksums when provided on the release.

Commercial support customers may also receive packages through Intersoft Data Labs channels.

## Install steps (high level)

Exact wizard screens differ by platform, but the flow is consistent:

1. **Stop** any previous CMS instance that would bind the same ports or install path.
2. Run the **CMS installer** (GUI or silent, as documented for that package).
3. Choose install directory, database connection, ports, and admin credentials.
4. Complete installation and start the CMS service (Windows service, Linux systemd/init scripts, or the platform service wrapper shipped with the product).
5. Optionally install and configure the **DTS** against the same or related environment.
6. Open the Web UI URL printed by the installer and sign in.

### Linux services (systemd and init.d)

Linux packages still ship **both** a native systemd unit and the classic init.d helpers.
Do not remove the init.d path until operations has signed off a live install on that host.

| Role | Install script (under the install tree) | Default unit name |
|------|-----------------------------------------|-------------------|
| CMS Jetty | `jetty/service/install-jetty-service.sh` | `PercussionCMS` |
| DTS Production | `Deployment/Server/DTSProductionService.sh` | `PercussionProductionDTS` |
| DTS Staging | `Deployment/Server/DTSStagingService.sh` | `PercussionStagingDTS` |

Run the script as root. With no flag, a host that has systemd gets a native unit
(`TimeoutStartSec=1800`, so a long upgrade start is not failed early by the unit timeout)
and the init.d file is kept only as the start/stop helper — it is **not** also enabled with
chkconfig. `--initd` registers only the classic SysV path and does not write a unit file.
Do not pass `--systemd` and `--initd` together.

After a systemd install:

```bash
systemctl enable --now PercussionCMS
systemctl status PercussionCMS
journalctl -u PercussionCMS -n 100 --no-pager
```

Uninstall with the same script's `uninstall` action. That removes the unit, `/etc/default`
entry, init.d helper, and SysV links for that service name. To move a host from init.d-only
to systemd, uninstall first, then install again without `--initd`.

The CMS script's on-host notes are `jetty/service/README-systemd.md`. A developer
user-namespace check (not a substitute for the commands above on a real host) lives in
the source tree as `scripts/linux-service-namespace-soak.sh`.

### Docker / evaluation

Repository `docker/` scripts and the root `docker-compose.yml` support evaluation and QA-style environments (including H2
QA mode for automated testing). Prefer documented `perc-devctl` / compose flows for agent and
developer QA rather than one-off container recipes.

H2 QA cells (`perc-devctl qa-up`) bind-mount `modules/perc-distribution-tree/target/perc-distribution-tree.jar`.
Rebuild that jar with `perc-devctl qa-rebuild-chain` (or `--dist-only` when WAR SNAPSHOTs are already fresh).
The chain packages **TinyMCE** (`modules/perc-tinymce`) before the dist tree so installer copy of
`rx_resources` / `sys_resources` does not fail on a missing `target/classes` directory. If
`qa-up` reports a missing or empty dist jar, run `qa-rebuild-chain` first, then `qa-health`.

## First verification checklist

- [ ] Process is running; no fatal errors in server logs under the install tree.
- [ ] Login page loads over the configured host/port.
- [ ] Admin user can authenticate.
- [ ] At least one Site is visible under **Explorer → Sites** (sample sites from installer
      **Install sample sites** / silent `--demo-sites`, or a site you create via
      **Content → Create Site** — see [Sites & content structure](id:admin-sites)).
- [ ] Version information matches 8.2 (About box or `Version.properties` under the install root).

## Build from source (developers)

To compile the monorepo instead of installing binaries, see
[Build from source](id:developer-build-source). Building is not required for ordinary operator installs.

## Related

- [Upgrade Overview](id:upgrade-overview)
- [Server operations](id:admin-server-ops)
- [Ports & paths](id:reference-ports-paths)
