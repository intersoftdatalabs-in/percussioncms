---
id: admin-faculty-directory
title: Faculty Directory membership and publish
description: How Directory widget lists people, how to remove a member from a published Faculty Directory, and how to verify after full site publish
version: "8.2"
order: 44
tags: [admin, publishing, directory]
---

# Faculty Directory membership and publish

The stock **Directory** widget (`perc.widget.directory` / `percDirectory`) does **not** store a list of faculty members on the directory asset. Editors who “remove someone from the Faculty Directory asset” and then run a full site publish often still see the name on the live page because membership is **query-based**, not an Active Assembly child list.

This page is the operator procedure for removing a person from a published Faculty Directory and verifying the result. Related publishing concepts: [Publishing](id:admin-publishing).

## Product model

| What editors often say | What the product uses |
|------------------------|------------------------|
| Faculty Directory **asset** | `percDirectory`: title, placeholder image, **organizationSearch**, **departmentID** |
| Faculty **member** | A separate **`percPerson`** asset |
| “On the directory” | The person’s **`personOrganization`** / **`personDepartment`** match the directory widget filters |
| “Remove from directory asset” | **Not** an Active Assembly unlink of people — stock UI has no member collection on `percDirectory` |

At **publish and preview assembly**, the widget runs a JCR query over `percPerson` (organization/department plus `sys_contentstateid != 7`), then applies the edition **public** item filter. Client script (`perc-directory.js`) only filters and sorts HTML that assembly already rendered.

## Remove someone from the published list

Pick **one** of the following on the **person** asset (or change the directory’s organization/department filter so the person no longer matches). Then publish as in the next section.

### Option A — Change membership fields (person stays Public)

1. Open the **`percPerson`** item (not only the Directory widget asset).
2. Clear or change **`personOrganization`** and/or **`personDepartment`** so they **no longer match** the Faculty Directory widget’s organization/department filters.
3. **Approve** so the **public** revision has the new field values. A tip-only Draft or Quick Edit is not enough; publish uses the public revision.
4. Publish the Faculty Directory **page** or run a **full site** publish (below).
5. Purge CDN or edge cache if the site uses one (below).

### Option B — Archive the person

1. Transition the person to **Archive** (stock CM1 workflow: Archive is state id **7** on the default workflow).
2. Confirm the person is no longer Public / publishable.
3. Publish the directory page or full site; purge CDN.

### Option C — Recycle the person

1. Recycle the person asset.
2. Confirm the person is under Recycling / does not match a public query result.
3. Publish the directory page or full site; purge CDN.

### Actions that do **not** remove someone

| Action | Result after full publish |
|--------|---------------------------|
| Edit only the Directory asset title or placeholder without changing person org/dept | Person still matches the query → **still listed** (correct product behavior) |
| Change person fields in the tip revision but leave the public revision on the old organization without Approve | Publish uses the public revision → **still listed** |
| Incremental publish only after a person change (directory page not in the job) | Directory page may not re-assemble → **stale HTML** |
| CMS publish success but CDN still serving the old object | Browser or CDN shows the old list |

## Publish after the person no longer matches

Use **either**:

1. **Full site publish** for the affected site, or
2. **Publish the Faculty Directory page** (and any other pages that host a Directory widget listing that organization/department).

Full site content lists re-assemble **public pages under the site**. When the directory page is in that job and assembly succeeds, the person query runs again against current repository state. A person who no longer matches organization/department **or** fails the public filter **must not** appear in newly written HTML.

### Incremental publish caveat

Incremental change tracking queues pages that own the changed asset via **relationships**. Directory lists people by **query**, not Active Assembly ownership of each person. Person field or Archive changes often **do not** queue the Faculty Directory page for incremental jobs.

Some Default Workflow configurations (later 8.1.x / 8.2 lines) may include a directory-index touch action on Approve. If you are unsure whether that action is wired, use **full site** publish or an **explicit page** publish of every Directory page after membership changes.

### Job verification checklist

- Correct **site** and **publish server** selected
- Job type is **full site**, or the directory **page** is explicitly in the edition
- Job completed successfully
- Publish log shows **assemble and deliver** for the Faculty Directory page **after** the person change
- The directory page itself is **Public** and under the site root used by the content list

## CDN, disk, and browser cache

Published directory HTML is static delivery (file, FTP, S3, or similar). There is no separate Delivery Tier rebuild of faculty lists.

After a successful CMS publish:

1. Confirm **on-disk / publish-root HTML** for the directory path **does not** contain the removed name.
2. Fetch the live URL with cache bypass if available; compare the body to disk.
3. If disk is clean but HTTP still shows the name, **purge CDN / reverse-proxy cache** for that URL.
4. Hard-refresh the browser or use a private window after purge.

## Support triage

Use this order when a name still appears after “removal + full publish”:

1. Identify the **directory page** (path and content id).
2. Open the **`percDirectory`** asset and note `organizationSearch` and `departmentID`.
3. Open the **person** asset: content id; tip versus **public** revision; workflow state; `personOrganization` / `personDepartment`; folder (Assets versus Recycling).
4. Would the stock query still return them?
   - **Yes** — fix data or approve the public revision; re-publish.
   - **No** — continue.
5. Check the publish log for directory page assemble/deliver success **after** the person change.
6. Check **on-disk HTML** for the name.
7. Check **HTTP** and cache headers. Name present while disk is clean → purge CDN.
8. Confirm edition type (full versus incremental) and whether a directory-index touch runs on workflow transitions.
9. Look for a **second source** (other pages, custom widgets, static mirrors).

Engineering classification of the original Faculty Directory stale-publish report lives under repository `docs/ai-generated/tasks/804-faculty-directory-stale-publish/` (inventory, H1–H9 matrix, slice notes). Operators should follow this product page, not those internal task files.
