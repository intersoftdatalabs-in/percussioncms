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

Operators using **Developer → Locales** create/save/delete chrome and the
auto-translation set editor: [Developer Locales](id:admin-developer-locales).

Operators using **Developer → Shared Fields** create/save/delete chrome: [Developer Shared Fields](id:admin-developer-shared-fields).

Operators using **Developer → System definition** field save/add/delete chrome: [Developer System Def](id:admin-developer-system-def).

Operators using **Developer → Slots** create/delete chrome: [Developer Slots](id:admin-developer-slots).

Operators using **Developer → Item Filters** create/save/delete chrome: [Developer Item Filters](id:admin-developer-item-filters).

Operators using **Developer → Searches** create/delete chrome: [Developer Searches](id:admin-developer-searches).

Operators using **Developer → Communities** create/delete chrome: [Developer Communities](id:admin-developer-communities).

Operators using **Developer → Views** create/delete chrome: [Developer Views](id:admin-developer-views).

Operators using **Developer → Display Formats** create/delete and column add/remove/reorder chrome: [Developer Display Formats](id:admin-developer-display-formats).

Operators using **Developer → Action Menus** create/delete chrome: [Developer Action Menus](id:admin-developer-action-menus).

Operators using **Developer → CE Controls** create/save/delete chrome: [Developer CE Controls](id:admin-developer-ce-controls).

## Accessibility — delete confirm

Destructive **Delete** on Developer catalog editors (Searches, Views, Display
Formats, Item Filters, Locales, Slots, Communities, Shared Fields, Keywords,
System definition fields, Content Types, Action Menus, and CE Controls) uses an
**in-app confirm dialog**, not the browser `window.confirm` prompt. The dialog is a
modal with a title, message, **Cancel**, and **Delete**. Screen readers can
announce it; Escape cancels when the delete is not in progress.

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
