# P-Trans intentional OUT disposition (in-flight queue + content-locale session)

**Parent epic:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Translation slice:** [#2411](https://github.com/intersoftdatalabs-in/percussioncms/issues/2411)  
**This residual:** [#2829](https://github.com/intersoftdatalabs-in/percussioncms/issues/2829) — finalize intentional **OUT** + epic remaining-open criteria  
**Inventory source:** [p-trans-api-inventory.md](./p-trans-api-inventory.md)  
**Shipped Present surfaces:** #2428 inventory · #2429 REST ([PR #2601](https://github.com/intersoftdatalabs-in/percussioncms/pull/2601)) · #2430 Explorer UI ([PR #2648](https://github.com/intersoftdatalabs-in/percussioncms/pull/2648)) · human QA [#2649](https://github.com/intersoftdatalabs-in/percussioncms/issues/2649)  
**Disposition date:** 2026-08-10  
**Status:** **OUT signed for 8.2 Explorer parity** — do **not** implement in-flight queue or content-locale session without explicit product re-open.

---

## Executive decision

| Capability (992 P-Trans) | 8.2 Explorer disposition | Agent action |
|--------------------------|--------------------------|--------------|
| Show item locales (current + available) | **Present** | Done (#2429/#2430); human QA #2649 |
| Translate (create new locale variant) | **Present** | Done (#2429/#2430); human QA #2649 |
| In-flight translation status / queue | **OUT** | No implement; no residual implement issue |
| Switch source/target locale **session** context | **OUT** | No implement; no residual implement issue |

**Product rationale (summary):** Operator-required translation parity for Explorer is **per-item locale list + create-variant**. Legacy CE “translation queue” and global content-locale session context are **not** 8.2 SPA parity goals. Reopening either row requires human product sign-off on #2411 (or a new child), not overnight agent invent.

---

## Row detail

### A — In-flight translation status / queue → **OUT**

| | |
|--|--|
| **992 acceptance** | Filter/list items with `translationState=inFlight` (or equivalent “translation queue”) |
| **REST / Explorer today** | Not exposed by `GET\|POST /rest/content-explorer/translations`. Explorer `TranslationsPanel` documents OUT; no queue UI |
| **DCE / legacy** | CE translation queue / relationship + workflow state in legacy CE apps |
| **Why OUT for 8.2** | (1) Public REST façade deliberately scoped to item locales + create-variant (#2411 B). (2) Operators can inspect per-item variants and use relationship/workflow surfaces already Present on product shell for state — a dedicated queue is a **power-user redesign**, not a drop-in SPA port. (3) Implementing a queue without a typed public contract would invent fields or scrape legacy CX apps — both banned by package principles. |
| **Re-open criteria** | Product requests a typed contract (e.g. `translationState` filter on search or a dedicated list resource) **and** files a PR-sized child under #2411/#2400 with acceptance + Playwright |
| **Do not** | File agent residuals that only restate “build translation queue”; implement without sign-off; claim Partial forever as silent debt |

### B — Content-locale session context → **OUT**

| | |
|--|--|
| **992 acceptance** | Selecting a content locale re-issues path/list APIs under that **content** locale session |
| **REST / Explorer today** | Community switch exists (`/communities/switch/{name}`). UI/TMX locale loading is **chrome** strings, not content-item locale. Per-item locale is shown and create-variant targets an explicit locale |
| **DCE / legacy** | DCE login locale + change-locale header flows |
| **Why OUT for 8.2** | (1) Per-item locale + explicit create-variant covers day-to-day translate-without-DCE for Explorer. (2) Global “session content locale” would require redesign of path, search, and list contracts (headers, filters, cache invalidation) — not a thin façade. (3) Confusing chrome locale with content locale caused prior false Present claims; keep them separate. |
| **Re-open criteria** | Product designs a content-locale session model (request headers or explicit context API) with multi-module companion closure; new child issue, not agent freestyle |
| **Do not** | Treat community switch or TMX loaders as content-locale session; re-issue all path APIs client-side with invented locale params |

---

## Matrix mapping

| Doc | Update |
|-----|--------|
| [p-trans-api-inventory.md](./p-trans-api-inventory.md) | In-flight + session rows **OUT** (signed); residual product questions A/B answered |
| [gap-matrix.md](../contracts/gap-matrix.md) | Translation workflow → **Present** for locales+create; in-flight + session listed under **Explicit OUT** |
| plan / #2411 / #2400 | Agent implement for #2411 complete except human QA; no new translation implement spam |

---

## What remains open (epic #2400 — not this disposition)

### Human QA only (no new agent implement for translation)

| Item | Issue | Role |
|------|-------|------|
| Explorer translations locales + create-variant | [#2649](https://github.com/intersoftdatalabs-in/percussioncms/issues/2649) | QA for #2430 / PR #2648 |
| Other Explorer shell / chrome / search / ACL QA | #2588, #2600, #2607, #2645, #2741, #2743, #2745, #2774, #2776, #2778, #2781, #2783, #2797, #2799, … | Handoffs from merged implement slices |

### Cross-epic Partial (not #2411 implement)

| Item | Notes |
|------|-------|
| Object ACL editor (full) | Gap-matrix **Partial** → ACL epics (#2274 family); residual pointer work separate from P-Trans |

### Product OUT (this note) — **not** open agent backlog

| Item | Status |
|------|--------|
| In-flight translation queue | **OUT** — reopen only with product sign-off |
| Content-locale session context | **OUT** — reopen only with product sign-off |

---

## Epic #2400 remaining-open criteria (research / parity program)

**Close #2400 only when all of the following are true:**

1. **Gap matrix** has **no silent omissions**: every known DCE capability is Present / Partial / **Missing** / Explicit OUT (or cross-epic pointer). **Missing** rows (as of 2026-08-11: Views DCE category + Inbox — #3108 / [views-inbox-missing-disposition.md](./views-inbox-missing-disposition.md)) must reach product IN/OUT/REDESIGN (then implement or Explicit OUT) before epic close — not agent invent OUT.
2. **No open agent implement children** under #2400 that are still required for Present rows (chrome, search execute, translation Present surface, etc.).
3. **Open work is human QA, product decisions, and any product-IN Missing implement children** — QA (`qa task`) issues assigned for UAT, or OUT/redesign tracked as product (not agent invent).
4. **Human product owner** accepts that residual OUT rows (including P-Trans in-flight/session) do not block research-epic close, **or** reopens them as new prioritized children.
5. Optionally: primary human QA handoffs for first-wave Present surfaces have pass/fail disposition (close or fail-fix residuals) — not a hard code gate for this docs residual, but preferred before labeling the program “done.”

**Do not keep #2400 open solely to “track” OUT rows** once documented. Prefer close when implement children are done and only QA/product OUT remain, **or** leave open explicitly for the open QA set + dispositioned Missing rows (current state as of 2026-08-11).

**#2411 close criteria:**

| Gate | State after this disposition |
|------|------------------------------|
| Inventory + REST + Explorer locales/create | **Done** (#2428/#2429/#2430) |
| In-flight + session | **OUT** (this note) — not agent implement |
| Human QA create-variant / locales list | **Open** #2649 |
| Agent implement residual | **None** — close #2411 when #2649 is done/dispositioned, **or** leave open only while #2649 is open |

---

## Hard bans (agents)

- Do **not** implement translation queue or content-locale session under overnight queues without product re-open.
- Do **not** file padded residuals that restate OUT as implement work.
- Do **not** flip gap-matrix translation row back to **Missing**/**Partial** for OUT rows alone after this sign-off.
- Do **not** write multi-phase progress only into `docs/ai-generated/tasks/**` — use #2400 / #2411 Agent progress.

---

## Change log

| Date | Note |
|------|------|
| 2026-08-10 | #2829 residual: formal OUT for in-flight + session; epic remaining-open criteria for #2400; link inventory + gap-matrix. |
| 2026-08-11 | Epic remaining-open criteria clarified for **Missing** Views + Inbox (#3108); silent omission still banned; product IN/OUT required. |
