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
| **Supported RDBMS** (production) | Use the database drivers and schemas documented for your release; evaluation/dev often uses H2. |
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

### Docker / evaluation

Repository `docker/` and compose files support evaluation and QA-style environments (including H2
QA mode for automated testing). Prefer documented `perc-devctl` / compose flows for agent and
developer QA rather than one-off container recipes.

## First verification checklist

- [ ] Process is running; no fatal errors in server logs under the install tree.
- [ ] Login page loads over the configured host/port.
- [ ] Admin user can authenticate.
- [ ] At least one Site is visible (sample or newly created).
- [ ] Version information matches 8.2 (About box or `Version.properties` under the install root).

## Build from source (developers)

To compile the monorepo instead of installing binaries, see
[Build from source](id:developer-build-source). Building is not required for ordinary operator installs.

## Related

- [Upgrade Overview](id:upgrade-overview)
- [Server operations](id:admin-server-ops)
- [Ports & paths](id:reference-ports-paths)
