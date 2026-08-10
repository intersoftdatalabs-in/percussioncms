# #804 / #2335 — Faculty Directory: ops runbook (Slice 3)

**Issues:** Parent [#804](https://github.com/intersoftdatalabs-in/percussioncms/issues/804) · Slice 3 [#2335](https://github.com/intersoftdatalabs-in/percussioncms/issues/2335)  
**Depends on:** [00-inventory.md](./00-inventory.md) (Slice 1) · [01-classification.md](./01-classification.md) (Slice 2, when merged — PR #2356)  
**Scope:** **Ops / support documentation only** — no Directory query or publish product change.  
**Why docs-only:** Slice 2 classified the customer report as primary **H1** (misunderstood membership model). Stock full-site re-assembly of the directory page re-runs the person query; a product “full publish ignores repo state” defect was **not** reproduced without a customer snapshot. Do **not** blind-rewrite Directory membership to AA children.

---

## Product model (one paragraph for operators)

The stock **Directory** widget (`perc.widget.directory` / `percDirectory`) does **not** store a list of faculty members on the directory asset.

|    What editors often say     |                                   What the product actually uses                                   |
|-------------------------------|----------------------------------------------------------------------------------------------------|
| Faculty Directory **asset**   | `percDirectory`: title, placeholder, **organizationSearch**, **departmentID**                      |
| Faculty **member**            | Separate **`percPerson`** asset                                                                    |
| “On the directory”            | Person’s **`personOrganization`** / **`personDepartment`** match the directory filters             |
| “Remove from directory asset” | **Not** an Active Assembly unlink of people — stock UI has no member collection on `percDirectory` |

At **publish/preview assembly**, the widget runs a JCR query over `percPerson` (org/dept + `sys_contentstateid != 7`), then applies the edition **public** item filter. Client `perc-directory.js` only filters/sorts already-rendered HTML.

---

## Correct ways to remove someone from the published Faculty Directory

Pick **one** of the following on the **person** asset (or change the directory’s org filter so the person no longer matches). Then publish as in the next section.

### Option A — Change membership fields (person stays Public)

1. Open the **`percPerson`** item (not only the Directory widget asset).
2. Clear or change **`personOrganization`** and/or **`personDepartment`** so they **no longer match** the Faculty Directory widget’s org/dept filters.
3. **Approve** so the **public** revision has the new field values (tip-only Draft/Quick Edit is not enough — see “Public revision lag” below).
4. Publish the Faculty Directory **page** or run a **full site** publish (below).
5. Purge CDN / edge cache if used (below).

### Option B — Archive the person

1. Transition the person to **Archive** (default CM1 workflow: Archive is state id **7** on stock WF app 5).
2. Confirm the person is no longer Public / publishable (`CONTENTVALID` not `y`/`i` on the public path).
3. Publish directory page or full site; purge CDN.

### Option C — Recycle the person

1. Recycle the person asset (stock path moves Live items toward Archive when Quick Edit is available and clears AA widget bindings — AA clear does **not** drive Directory membership).
2. Confirm the person is under Recycling / not matching a public query result.
3. Publish directory page or full site; purge CDN.

### What does **not** remove someone from the published list

|                                         Action                                         |                                             Result                                             |
|----------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| Edit only the Directory asset title/placeholder without changing person org/dept       | Person still matches → **still listed** after full publish (**H1** — correct product behavior) |
| Clear person fields in tip but leave public revision on old org without Approve        | Publish uses public rev → **still listed** (**H2**)                                            |
| Incremental publish only after person change (no DirectoryIndexTouch / no page in job) | Directory page may not re-assemble → **stale HTML** (**H8**)                                   |
| Full CMS publish success but CDN still serving old object                              | Browser/CDN shows old list (**H4**)                                                            |

---

## Agreed publish path (after person is correctly non-matching)

Use **either**:

1. **Full site publish** for the affected site (customer report path), **or**
2. **Publish the Faculty Directory page** (and any other pages that host a Directory widget listing that org/dept).

### Why full site usually works when the person truly no longer matches

Full site content lists re-assemble **public pages under the site**. When the directory page is in that job and assembly succeeds, the person query runs again against current repository state. A person who no longer matches org/dept **or** fails the public filter **must not** appear in newly written HTML.

### Incremental publish caveat (stock)

Incremental change tracking queues pages that own the changed asset via **relationships**. Directory lists people by **query**, not AA ownership of each person. So person field/Archive changes often **do not** queue the Faculty Directory page for incremental jobs.

- Stock Default Workflow (post ~8.1.7 / GH-829) may wire **`PSDirectoryIndexTouchWorkflowAction`** on Approve to touch directory assets/pages — verify on the customer build/workflow.
- Customer report version **8.1.5** may lack that wire. When in doubt: **full site** or **explicit page** publish of every Directory page after person membership changes.

### Job verification checklist

- [ ] Correct **site** and **pub server** selected
- [ ] Job type is **full site** (or the directory **page** is explicitly in the edition)
- [ ] Job completed successfully
- [ ] Pub log shows **assemble + deliver** for the Faculty Directory page after the person change
- [ ] Directory page itself is **Public** and under the site root used by the content list

---

## CDN / edge / browser cache

Published directory HTML is static delivery (file/FTP/S3/etc.). There is no separate DTS rebuild of faculty lists.

After a successful CMS publish:

1. Confirm **on-disk / pub-root HTML** for the directory path **does not** contain the removed name.
2. Fetch the live URL with cache bypass if available; compare body to disk.
3. If disk is clean but HTTP still shows the name → **purge CDN / reverse-proxy cache** for that URL (and related assets if fingerprinted poorly).
4. Hard-refresh browser / private window after purge.

---

## Support triage (ordered)

Use when a customer still sees a name (example: “Amy Kern”) after “removal + full publish.”

1. **Directory page** — path + content id.
2. **`percDirectory` asset** — `organizationSearch`, `departmentID`.
3. **Person asset** — content id; tip vs **public** revision; state; `CONTENTVALID`; `personOrganization` / `personDepartment`; folder (Assets vs Recycling).
4. **Would stock query still return them?**
   - **Yes** → fix data or approve public rev (**H1** / **H2**); re-publish.
   - **No** → continue.
5. **Pub log** — directory page assemble/deliver success and timestamp after the person change.
6. **On-disk HTML** — name present?
7. **HTTP** + cache headers — name present while disk clean → purge (**H4**).
8. **Edition type** — full vs incremental; DirectoryIndexTouch on workflow transitions (**H8**).
9. **Second source** — other pages, custom widgets, static mirrors (**H3**).

Detailed hypothesis table: [00-inventory.md](./00-inventory.md). Snapshot classification matrix: [01-classification.md](./01-classification.md) (when present).

---

## When product engineering is still needed

Do **not** change Directory JCR queries or publish filters without snapshot evidence. Escalate for product only if triage shows:

|                                            Evidence                                            |      Likely class       |                       Product direction (gated)                        |
|------------------------------------------------------------------------------------------------|-------------------------|------------------------------------------------------------------------|
| Person does not match query + public filter; full job assembled page; disk HTML still has name | Rare assembly/query bug | Targeted fix + tests                                                   |
| Custom workflow Archive state ≠ 7 and CONTENTVALID still publishable                           | **H6**                  | Query/filter hardening (CONTENTVALID / recycled exclusion)             |
| Incremental-only ops; no DirectoryIndexTouch on Approve                                        | **H8**                  | Ensure touch action on customer WF **or** keep ops “full/page publish” |
| Confirmed H1/H2/H4 only                                                                        | Ops                     | **No** product query rewrite                                           |

**Avoid:** rewriting stock Directory to an AA children membership list without an explicit product decision (breaks org/dept auto-list UX).

---

## Acceptance (parent #804)

Close **#804** only when:

- Customer session or snapshot shows the person **no longer matches** query + public filter **and** re-assembled published HTML **omits** the name after the agreed publish path, **or**
- Customer confirms the ops path (correct person change + Approve + full/page publish + CDN purge) resolved the report.

This runbook alone does **not** close #804.

---

## Related paths (repo)

```
modules/perc-packages/.../Widgets/percDirectory.xml          # person JCR queries
projects/sitemanage/.../PSLivePublishChangeHandler.java      # incremental owner walk
projects/sitemanage/.../PSRecycleService.java                # recycle → Archive path
modules/extensions-main/.../PSDirectoryIndexTouchWorkflowAction.java
docs/ai-generated/tasks/804-faculty-directory-stale-publish/00-inventory.md
docs/ai-generated/tasks/804-faculty-directory-stale-publish/01-classification.md
```

