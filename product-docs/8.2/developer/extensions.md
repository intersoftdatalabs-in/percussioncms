---
id: developer-extensions
title: Extensions & packages
description: Java extensions, packages, and deployment units for Percussion CMS 8.2
version: "8.2"
order: 53
tags: [developer, extensions]
---

# Extensions & packages

Percussion CMS is extended through **Java extensions**, **packages** (`.ppkg`), templates/variants,
and related design objects.

## Extensions

Built-in and custom extensions live in modules such as:

- `modules/extensions-main`
- `modules/extensions-workflow`
- `modules/extensions-nav`
- `modules/extensions-sfp`
- Additional specialized extension modules

Extensions implement product extension points (assembly exits, workflow actions, content converters,
and more). New extension code should:

1. Target **JDK 21** APIs used by the 8.2 line.
2. Include unit tests for non-trivial logic.
3. Avoid non-portable path construction (use `java.nio.file.Path` / `Files`).
4. Ship with package/resources as required so installs receive them.

## Packages

A **package** is a deployable unit of CMS components (content types, templates, apps, configs)
distributed as a `.ppkg` (zip). Installers and startup packaging deploy packages into the CMS.

`perc.Baseline` system templates (`perc.page`, `perc.pageDatabase`, `perc.pageDispatcher`,
`perc.pageXml`, `perc.sys.resource`, `perc.widget`, `perc.widgetDispatcher`) install with stable
GUIDs `0-4-602`..`0-4-614` on a **fresh** 8.2 instance. Existing databases keep the UUID assigned on
first install; package apply does not rewrite customer template GUIDs. See
[Design templates](id:admin-design-templates).

When changing packaging:

- Keep platform entry points (`.bat` / `.cmd` / `.sh`) in lockstep where operators need them.
- Document install/upgrade impact for operators.

## REST vs extensions

Prefer **public REST + adaptors** for new integration surfaces that clients will call over HTTP.
Prefer **extensions** when you must participate in assembly, workflow, or other in-process CMS
extension points.

## Related

- [REST API](id:developer-rest)
- [Publishing](id:admin-publishing)
