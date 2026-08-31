---
id: developer
title: Developer
description: Developer guides for Percussion CMS 8.2
version: "8.2"
order: 50
tags: [developer]
---

# Developer

Extension points, REST, assemblers, Virtual Sites, and building Percussion CMS 8.2 from source.

## Topics

- [REST API](id:developer-rest)
- [Virtual Sites](id:developer-virtual-sites)
- [Extensions & packages](id:developer-extensions)
- [Product page packages](id:developer-page-packages)
- [Build from source](id:developer-build-source)

Operators using **Developer → Content types** lock/save chrome: [Developer Content Types](id:admin-developer-content-types).

Operators using **Developer → Locales** create/save/delete chrome: [Developer Locales](id:admin-developer-locales).

Operators using **Developer → Shared Fields** create/save/delete chrome: [Developer Shared Fields](id:admin-developer-shared-fields).

Operators using **Developer → System definition** field save/add/delete chrome: [Developer System Def](id:admin-developer-system-def).

## Architecture snapshot

```text
WebUI (SPA / editors)
        │
        ▼
Public REST (rest module: resources, DTOs, IXxxAdaptor)
        │
        ▼
sitemanage apibridge (implements adaptors)
        │
        ▼
system / design webservices / objectstore
```

Key rules for modern APIs:

- New public HTTP surfaces belong in the **`rest`** module.
- **`rest` must not depend on `sitemanage`** (reactor cycle).
- Adaptor implementations live in **sitemanage** and stay thin.

## Assemblers

8.2 continues assembler modernization (HTML-first and Markdown helpers). Product documentation
dogfoods Markdown assembly through Virtual Sites.

## Related engineering notes

Internal task docs for Virtual Sites live under repository
`docs/ai-generated/tasks/virtual-sites-git-docs/` (not part of this product site tree).
