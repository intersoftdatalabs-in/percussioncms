---
id: developer-build-source
title: Build from source
description: Build Percussion CMS 8.2 and product-docs from the monorepo
version: "8.2"
order: 54
tags: [developer, build]
---

# Build from source

Instructions for developers building Percussion CMS 8.2 from this monorepo.

## Prerequisites

| Tool | Notes |
|------|--------|
| **JDK 21** | `JAVA_HOME` must point at JDK 21 |
| **Maven wrapper** | Use repo-root `mvnw` / `mvnw.cmd` (do not require a global Maven version fight) |
| **Git** | Clone `intersoftdatalabs-in/percussioncms` |
| **Node.js** | Required for WebUI / TinyMCE-related modules when those modules are built |
| **Windows long paths** | Enable long path support when building Node-heavy modules on Windows |

## Full reactor build

```bash
# Linux / macOS
./mvnw clean install

# Windows
mvnw.cmd clean install
```

Full monorepo builds are large. For feature work, prefer **standalone module builds**:

```bash
cd rest
../mvnw clean install
```

```bat
cd projects\sitemanage
..\..\mvnw.cmd clean install
```

Build producers before consumers when both change (install the upstream SNAPSHOT first).

## Product docs build

<a id="product-docs-build"></a>

After `system` compiles successfully:

```bat
scripts\build-cms-docs.bat
```

```bash
scripts/build-cms-docs.sh
```

Optional arguments: `[siteRoot] [outputRoot]`. Defaults:

- site root: `product-docs/`
- output: `tmp/product-docs-site/`

The CLI exits non-zero if link checks fail (missing `id:` targets or broken relative `.md` links).

## Contribution basics

- Follow root `AGENTS.md` / `CONTRIBUTING.md` for process gates.
- Add or update unit tests with code changes.
- Keep documentation under `product-docs/` in lockstep with product features when operators need it.
- Open PRs against **`main`** for the active 8.2 line.

## Related

- [Installation Overview](id:install-overview)
- [Virtual Sites](id:developer-virtual-sites)
- [REST API](id:developer-rest)
