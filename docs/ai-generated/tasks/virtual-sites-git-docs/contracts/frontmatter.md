# Frontmatter contract

Every Markdown page under a version folder should start with a YAML frontmatter block:

```yaml
---
id: install-overview
title: Installation Overview
description: How to install Percussion CMS 8.2
version: "8.2"
sidebar: true
order: 10
tags: [install, admin]
deprecated: false
---
```

## Fields

| Field | Required | Notes |
|-------|----------|-------|
| `id` | **Yes** | Stable identity for virtual participants / links. Unique within a version. |
| `title` | **Yes** | Page title. |
| `description` | No | Short summary. |
| `version` | No | Inherited from version folder name when omitted. |
| `sidebar` | No | Default `true`. |
| `order` | No | Sort key among siblings (default `0`). |
| `tags` | No | List of strings. |
| `deprecated` | No | Default `false`. |

## Validation

- Missing `id` or `title` fails the page (build error).
- Duplicate `id` within the same version fails the build.
