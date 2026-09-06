---
id: admin-developer-object-sorter
title: Developer Object Sorter
description: Admin session-only sort and custom order for the current Developer object list (Content Types)
version: "8.2"
order: 54
tags: [admin, developer, object-sorter]
---

# Developer Object Sorter

**Object Sorter** (classic Workbench supporting panel) is an **Admin** aid for
**organizing the current Developer object list**. Operators open it from
**Developer → Object Sorter**. It does not create, rename, or delete design
objects.

This increment lists the **Content Types** catalog (the default Developer object
list). Other catalogs stay on their own tabs.

This is **not** File Explorer, Database Explorer, or the Problems panel.

## Developer SPA

1. Sign in as **Admin**.
2. Open **Developer** and select the **Object Sorter** tab
   (`/cm/app/developer/object-sorter`).
3. The table lists the current Content Types catalog (name and label).
4. Choose **Sort**:
   - **Label A to Z** (default, same as the Content Types tab)
   - **Label Z to A**
   - **Name A to Z**
   - **Name Z to A**
   - **Custom order** — **Move up** / **Move down** on a row
5. The **Content Types** tab uses the same sort preference for its catalog
   list.

## Session-only preference

Sort mode and custom row order are stored in the **browser session**
(`sessionStorage`). They survive reload of the same tab.

They are **not** saved as a CMS user preference. There is no Preference REST
peer for this list. Closing the tab, or signing in from another browser,
resets the order to **Label A to Z**.

## REST

This increment does **not** add a REST resource. The panel reads the existing
Admin Content Types catalog (`GET /services/contenttypes`).

Integrator notes: [REST API](id:developer-rest).
