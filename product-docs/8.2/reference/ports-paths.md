---
id: reference-ports-paths
title: Ports & paths
description: Common ports and filesystem paths for Percussion CMS 8.2
version: "8.2"
order: 63
tags: [reference, admin]
---

# Ports & paths

Exact ports and paths are **environment-specific**. This page lists common conventions; always
confirm against your install wizard choices and `Version.properties` / Jetty config.

## Ports

| Service | Notes |
|---------|--------|
| CMS HTTP / HTTPS | Chosen at install; developer machines often use non-80 ports |
| DTS HTTP | Separate connectors when DTS is installed |
| Database | External to CMS; firewall as appropriate |
| Reverse proxy | TLS usually terminates at the proxy in production |

Do not hard-code ports in documentation examples as universal truths.

## Install tree highlights

Paths below are relative to the CMS install root unless noted. Separators differ by OS; use the
platform path form.

| Location | Purpose |
|----------|---------|
| `Version.properties` | Installed product version metadata |
| `jetty/` | Embedded Jetty runtime |
| `jetty/base/logs/` | Server and application logs |
| `rxconfig/` (when present) | Classic configuration trees |
| Publish locations | Configured per Site / delivery type — not a single fixed folder |

## Repository (source) paths of interest

| Path | Purpose |
|------|---------|
| `product-docs/` | Git-backed product documentation Virtual Site source |
| `tmp/product-docs-site/` | Default offline docs build output |
| `scripts/build-cms-docs.bat` / `.sh` | Docs build entry points |
| `rest/` | Public REST module |
| `projects/sitemanage/` | UI middleware + REST adaptors |
| `system/` | Core CMS module |
| `modules/perc-qa-automation/` | Playwright E2E |

## Related

- [Server operations](id:admin-server-ops)
- [Installation Overview](id:install-overview)
