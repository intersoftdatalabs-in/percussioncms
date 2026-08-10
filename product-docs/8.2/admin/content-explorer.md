---
id: admin-content-explorer
title: Content Explorer
description: Product Content Explorer shell — browse, server actions, and context menus
version: "8.2"
order: 42
tags: [admin, content explorer, ui]
---

# Content Explorer

The **Content Explorer** is the product web shell for browsing Sites, folders, pages, and
assets without launching Desktop Content Explorer (DCE). Open it from the SPA at
`/cm/app/spa.jsp?entry=explorer` (or the Explorer entry in the product navigation).

## What you see

| Chrome | Purpose |
|--------|---------|
| **Menu bar** (Content / View / Help) | Product commands: search, clipboard, site/subfolder copy, view tools |
| **Reduced actions** | Always-available open / preview / create folder / rename / move / copy / delete |
| **Server action toolbar** | Configuration-driven actions from the CMS action catalog (`rest/actions`) for the current selection |
| **Tree + detail list** | Folder navigation and list of children; optional display-format columns |
| **Context menu** | Right-click an item or folder row for the same catalog filtered for the popup surface |

## Server actions and context menu

Menus and toolbar buttons come from the server action catalog used by Content Explorer:

- When you select a content item, the shell loads allowed menus for that content type.
- When only a folder is active, the shell loads the cascading action tree for the Explorer UI.
- **Desktop-only** actions (for example custom application protocols that only DCE can run) are
  **hidden** in the web shell so operators are not offered controls that cannot succeed in the
  browser.
- Actions of type **context menu** appear on right-click, not as permanent toolbar buttons.
- Workflow transition triggers (when available for the selected item) appear as a labeled group
  on the toolbar and in the context menu.

Selecting a server action either navigates to a product-safe same-origin URL or refreshes the
list after a client-handled command (for example a workflow transition).

## Display format

Use the **display format** selector in the menu bar to choose list columns for the current folder
(`validForFolder` formats). Changing the format reloads the detail list with the selected columns.

## Search, security, and advanced tools

From the **View** menu you can toggle:

- **Search** — extended search panel (criteria for the current folder path)
- **Folder security** — ACL / properties for the selected folder
- **Translations**, **Relationships**, and **Dependencies** — advanced item tools when a suitable
  item is selected

## Related

- [Sites & content structure](id:admin-sites)
- [Users, roles & security](id:admin-users-roles)
- [Publishing](id:admin-publishing)
