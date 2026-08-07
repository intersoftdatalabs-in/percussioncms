# #804 / #2334 — Faculty Directory stale publish classification (Slice 2)

**Issues:** Parent [#804](https://github.com/intersoftdatalabs-in/percussioncms/issues/804) · Slice 2 [#2334](https://github.com/intersoftdatalabs-in/percussioncms/issues/2334) · Slice 3 fix [#2335](https://github.com/intersoftdatalabs-in/percussioncms/issues/2335)  
**Date:** 2026-08-07  
**Depends on:** [00-inventory.md](./00-inventory.md) (Slice 1, PR #2336)  
**Scope:** Classification + evidence only — **no product behavior change**.

---

## Executive verdict

| Question | Answer |
|----------|--------|
| Customer DB / site snapshot available? | **No** (none in worktree, `docker/dev-data`, Downloads, or issue attachments beyond UI screenshot) |
| Live H2 full-site publish repro run? | **Not executed** — no running CMS/QA container; last `qa-down` left stack stopped; overnight slice prioritizes classification over multi-hour faculty seed + full publish |
| Stock “correct removal + full site publish still shows person” product defect? | **Not reproduced.** Code path predicts: successful full-site re-assembly of the directory page re-runs the JCR person query under the public item filter; a person who no longer matches org/dept **or** is non-publishable (`CONTENTVALID` not `y`/`i`) **must not** appear in new HTML |
| Best fit for the **customer report as written** | **H1 (misunderstood model)** — primary |
| Secondary fits if H1 ruled out on snapshot | **H2** (public revision lag / unapproved tip edit), then **H4** (CDN/cache), **H5** (page omitted from job), **H8** (incremental mistaken for full / touch not wired) |
| Slice 3 fix class recommendation | **Ops/support first** (correct removal + full page/site publish + CDN purge checklist). Product only if snapshot proves a remaining code path (query Archive hard-code, recycle edge, or missing DirectoryIndexTouch on custom workflow) |

---

## Environment / non-repro bounds

### What was available

| Source | Result |
|--------|--------|
| Issue #804 body + comments | Symptom + “Amy Kern” UI screenshot; **QA cannot reproduce**; customer still affected; snapshot suggested |
| Slice 1 inventory | End-to-end assembly/publish map + H1–H9 |
| Product source (this repo `main`) | Directory widget JEXL queries, auto finder, live change handler, recycle archive, seed STATES, DirectoryIndexTouch wiring |
| Customer snapshot DB / pub logs / delivered HTML | **Absent** |
| Running CMS (H2 qa-up / host install) | **Absent** at classification time (docker hosts Oracle + unrelated services only) |

### What “documented non-repro” means here

We did **not** claim the customer is wrong. We claim:

1. The stock product **cannot** implement “remove member from Faculty Directory **asset**” as an AA unlink of people — there is no such list on `percDirectory`.
2. Given a person that **no longer matches** the directory query **and** is filtered out by the public item filter, a **successful full site publish** that **assembles** the directory page **must** drop that person from newly written HTML.
3. Therefore a customer who still sees the person after an alleged full publish falls into one of: **data still matches (H1/H2)**, **wrong output target (H3/H4/H5)**, **wrong job type (H8)**, or a **rare query/filter edge (H6/H7)** — not a generic “full publish ignores current repository state” bug.

QA non-repro on a clean stock path is consistent with (2).

---

## Scenario matrix (customer-like operations → expected live HTML)

Stock membership rule (assembly):

```text
percPerson WHERE personOrganization matches directory.organizationSearch
           [AND personDepartment matches directory.departmentID when set]
           AND sys_contentstateid != 7
THEN item filter "public" (publishable flags y,i)
```

Evidence: `percDirectory.xml` JEXL queries; seed `STATES` Archive = 7 only on workflow app **5**; public filter `sys_flagValues=y,i` (inventory + seed).

| # | Operator action on “Amy Kern” | Tip / public state after action | Match org/dept? | Expected after **full site** re-assembly of directory page |
|---|------------------------------|----------------------------------|-----------------|--------------------------------------------------------------|
| S1 | Edit only `percDirectory` title/org filter UI; **no** change to person asset | Person still Public | Yes (same org) | **Still listed** (correct) — **H1** |
| S2 | Clear person `personOrganization` / change org; **Approve** to Public | Public fields updated | No | **Gone** |
| S3 | Clear person fields; leave tip in Draft/Quick Edit; public rev still old org | Tip ≠ public | Public rev still Yes | **Still listed** — **H2** |
| S4 | Archive person (state 7 on WF app 5) | CONTENTVALID `u` | N/A | **Gone** (query `!= 7` **and** public filter) |
| S5 | Archive person on **legacy WF app 4** (Archive state id **5**, not 7) | CONTENTVALID `u` | Maybe | **Gone on publish** if public filter applied; may still appear in **unfiltered preview** — **H6** partial |
| S6 | Recycle person (moves to Recycling + Archive when Quick Edit available) | Archived when was Live | Query may still return id if state ≠7 path fails | **Normally gone** via Archive; AA clear is **irrelevant** to Directory list |
| S7 | Soft-delete / hide only in a **custom** list (not stock Directory) | Varies | — | **H3** / customization |
| S8 | Correct person change + **incremental** only; DirectoryIndexTouch **not** on workflow | Directory page not touched | No match | **Stale HTML until full/page publish** — **H8** |
| S9 | Correct person change + full site job; CDN serves old object | CMS disk correct | No match | **Stale HTTP** — **H4** |
| S10 | Full job fails / wrong site / directory page not public | Old HTML remains | — | **H5** |

**Customer wording** (“removed from the Faculty Directory asset”, “record no longer exists in the associated asset”) maps most cleanly to **S1 / H1**, not S2/S4/S6.

---

## H1–H9 classification (ranked)

### H1 — Misunderstood model (person still matches query) — **PRIMARY**

| | |
|--|--|
| **Status** | **Most likely for reported steps** |
| **Fits full publish?** | Yes — publish correctly re-lists matching people |
| **Evidence** | (1) `percDirectory` fields are title / placeholder / `organizationSearch` / `departmentID` only — no member collection. (2) List built solely by JCR over `rx:percPerson` matching org/dept. (3) Package `percPerson.itemDef` exposes `personOrganization` as person fields. (4) QA cannot repro a “removal from directory asset” path that stock UI does not have. |
| **How to confirm on snapshot** | SQL/JCR: Amy Kern `CT_PERCPERSON` / content id; `personOrganization`/`personDepartment` vs directory asset; folder not under Recycling; state Public. |
| **Product fix?** | **No bugfix.** Support education + optional UX copy (“membership is by person org/dept fields”). |

### H2 — Public revision lag — **SECONDARY**

| | |
|--|--|
| **Status** | Plausible if editors change tip without Approve |
| **Fits full publish?** | Yes — assembly uses public filter / public revision |
| **Evidence** | `PSPublicFilter` rewrites to public-or-current; publish editions use public item filter. Tip-only field clears do not change published person fields. |
| **Confirm** | Tip vs public revision for Amy Kern; CONTENTVALID; CMS preview (current) vs publish (public). |
| **Product fix?** | Usually ops (Approve / Archive). Optional UX warning when directory still shows person after tip edit. |

### H3 — Wrong page / second source — **POSSIBLE**

| | |
|--|--|
| **Status** | Open until HTML inventory |
| **Evidence** | Multiple pages can host Directory widgets; templates; static mirrors. |
| **Confirm** | Site-wide search for “Amy Kern”; list all `percDirectory` bindings. |

### H4 — CDN / proxy / browser cache — **POSSIBLE (ops)**

| | |
|--|--|
| **Status** | Classic after successful publish |
| **Evidence** | Delivery is static HTML (`PSFileDeliveryHandler` / FTP / S3); no DTS rebuild of faculty lists. Client `perc-directory.js` only filters already-rendered DOM. |
| **Confirm** | On-disk pub target HTML **without** name vs HTTP body **with** name; `Cache-Control` / CDN age. |
| **Product fix?** | Ops purge docs, not CMS query change. |

### H5 — Full publish omitted directory page — **POSSIBLE**

| | |
|--|--|
| **Status** | Job/content-list failure |
| **Evidence** | Full site content list is site-root JCR search + public filter (`PSSitePublishDao`); page must be public and under site. |
| **Confirm** | Pub log assemble/deliver rows for directory page; job status; site/server selection. |

### H6 — Hard-coded `sys_contentstateid != 7` — **PARTIAL / secondary**

| | |
|--|--|
| **Status** | Real code smell; **weak as sole explanation of full-publish customer report** |
| **Evidence** | All four person queries in `percDirectory.xml` hard-code `!= 7`. Seed: WF app **5** Archive = **7** (`CONTENTVALID=u`); WF app **4** Archive = **5** (`CONTENTVALID=u`). Publish public filter still drops `u`. |
| **When it bites** | Preview / missing filter; custom workflows where Archive is not 7 **and** CONTENTVALID still `y`/`i` (misconfigured WF). |
| **Slice 3** | Optional hardening: filter by publishable flag / CONTENTVALID / not recycled folder — **not** the first fix for H1. |

### H7 — JCR ghost ids — **UNLIKELY**

| | |
|--|--|
| **Status** | No code smell unique to Directory |
| **Confirm** | Same query via diagnostics vs `CONTENTSTATUS` |

### H8 — Incremental-only / touch not effective — **REAL product gap historically; mitigated for Default WF**

| | |
|--|--|
| **Status** | Explains **incremental** stale lists; customer claims **full** site publish |
| **Evidence** | `PSLivePublishChangeHandler.handleAssetChangeForPages` only walks **AA relationship owners** — person assets are **not** AA children of Directory list. `PSDirectoryIndexTouchWorkflowAction` (GH-829 / v8.1.7) touches all `percDirectory` + parent pages on Approve; wired in Default Workflow package + installer SQL for WF app 6 Approve. Customer version **8.1.5** predates or may lack that wire. |
| **Confirm** | Edition type of the job they ran; whether DirectoryIndexTouch is on their transitions; LASTMODIFIED of directory page after person Approve. |
| **Slice 3** | If snapshot is pre-touch / custom WF: ensure touch action registration **or** document mandatory full/page publish of directory after person changes. |

### H9 — AA stale relationship — **UNLIKELY for stock Directory**

| | |
|--|--|
| **Status** | Ruled out for stock list path |
| **Evidence** | List is auto-query, not AA dependents of `percDirectory`. Recycle AA clear does not drive directory membership. |
| **Still check** | Customer customizations / non-stock widgets. |

---

## Code evidence index (repo paths)

| Claim | Path / anchor |
|-------|----------------|
| Directory person queries + `!= 7` | `modules/perc-packages/.../Widgets/percDirectory.xml` (JEXL `$params.put('query', …)`) |
| Auto finder bridge | `projects/sitemanage/.../PSAutoWidgetContentFinder.java`; `system/services/.../PSAutoFinderUtils.java` |
| Incremental owner-only queue | `projects/sitemanage/.../PSLivePublishChangeHandler.java` → `handleAssetChangeForPages` → `getRelationshipOwners` |
| Recycle archives Live items | `projects/sitemanage/.../PSRecycleService.java` `transitionWorkflowItem` → `TRANSITION_TRIGGER_ARCHIVE` |
| Archive state ids | `modules/perc-distribution-tree/.../RxffTableData.xml` STATES: app4 Archive=5; app5 Archive=7 |
| DirectoryIndexTouch | `modules/extensions-main/.../PSDirectoryIndexTouchWorkflowAction.java`; Extensions.xml; `perc.workflow` DefaultWorkflow wires; installer `installRepository.xml` |
| Package regression pins | `modules/perc-packages/.../PSDirectoryWidgetAutoQueryModelTest.java` (this slice); `PSDirectoryIndexTouchWorkflowActionPortTest.java` |

---

## Support triage playbook (when snapshot arrives)

Copy into support ticket; complete in order:

1. **Directory page** content id + published path.  
2. **`percDirectory` asset:** `organizationSearch`, `departmentID`.  
3. **Amy Kern `percPerson`:** content id, tip/public revision, state id, CONTENTVALID, `personOrganization`, `personDepartment`, folder path.  
4. **Would stock query return her?** Yes → **H1/H2** (fix data or approve). No → continue.  
5. **Pub log** for the full job: directory page assemble/deliver success + timestamp.  
6. **On-disk HTML** under pub root: name present?  
7. **HTTP** body + cache headers: name present while disk clean → **H4**.  
8. **Edition type** full vs incremental; DirectoryIndexTouch on WF.  
9. **Custom** Directory / second page / mirror.

---

## Slice 3 recommendation (#2335)

### Do first (likely closes customer pain without risky product change)

1. **Ops runbook** (docs or support KB): correct removal steps — edit/approve person org fields, or Archive/Recycle person; then **full site publish** or **publish the Faculty Directory page**; purge CDN.  
2. On customer call: run support triage above; expect **H1** or **H2** confirmation.

### Product changes — only with matching evidence

| Priority | Change | Gate |
|----------|--------|------|
| P0 | **None** if H1/H2/H4 confirmed | Snapshot evidence |
| P1 | Ensure `sys_DirectoryIndexTouchWorkflowAction` on customer Approve transitions (already stock Default WF post-8.1.7) | H8 + version/custom WF |
| P2 | Directory query: drop hard-coded `!= 7`; exclude non-publishable / recycled | H6 or recycle edge on snapshot |
| P3 | Incremental reverse index / broader touch for auto-list widgets | Product enhancement; not required to explain full-publish report |
| Avoid | Blind rewrite of Directory to AA children list | Model change; breaks org/dept filtering UX |

### Acceptance for closing #804

- Snapshot or customer session shows Amy Kern **no longer matches** query + public filter **and** full re-assembly HTML **without** name, **or** customer confirms ops path (H1/H2/H4) resolved.  
- Do **not** close #804 solely because this classification exists.

---

## Residual / follow-ups

| Item | Owner issue | Notes |
|------|-------------|--------|
| Minimal fix / ops docs per recommendation | **#2335** | Blocked on this classification (unblocked by this doc) |
| Live publish smoke on H2 with seeded percPerson/percDirectory | Optional residual if humans want automated regression | Not required for H1 primary verdict |
| Customer snapshot pull | Support / engineering | Still the gold standard for H2–H5 |

---

## Out of scope (this slice)

- Product / Velocity / publish code changes  
- Closing #804 or #2335  
- Multi-hour QA faculty seed + full publish without snapshot demand
