# Developer Module documentation

Tool-agnostic reverse engineering of the Rhythmyx 7.3.2 **Workbench** (Eclipse Designer product) for designing and implementing a replacement **Developer module** in this repository (`percussioncms`).

**Provenance:** Inventories were reverse-engineered from the sibling codebase `rx732patchdev` (Rhythmyx 7.3.2 sources) and migrated here for implementation design.

## Documents

| File | Description |
|------|-------------|
| [workbench-functional-inventory.md](./workbench-functional-inventory.md) | Primary inventory + functional requirements: IA, object catalog, module FR, cross-cutting platform needs, prioritization, appendices |
| [data-pipeline-engine-inventory.md](./data-pipeline-engine-inventory.md) | **XML Application / E2Designer data pipeline engine** — full stage inventory (tanks, mapper, selector, updater, txn, hooks, value system) + modernization brief (JSON, datasources, hooks, IR) |
| [data-pipeline-server-runtime-map.md](./data-pipeline-server-runtime-map.md) | **Server runtime map** in this repo — `PSApplicationHandler` → `PSQueryHandler` / `PSUpdateHandler` (+ reuse vs reimplement notes for Slice A) |

## Source system (sibling: `rx732patchdev`)

| Layer | Path in `rx732patchdev` |
|-------|-------------------------|
| Workbench UI | `Designer/ui` |
| Client core | `Designer/core` |
| Data pipeline visual designer | `Designer/Src` (`E2Designer`) |
| Design service contracts | `webservices/design` |
| User help | `ReleasedDocuments/online/com.percussion.doc.workbench` |

## Sequencing (recommended)

1. **P0 CMS design tools** — content types, fields, shared/system defs, keywords, templates, slots, communities/ACL, connection session  
2. **Server runtime map** — [data-pipeline-server-runtime-map.md](./data-pipeline-server-runtime-map.md) (done as docs; use before estimating engine reuse)  
3. **Pipeline Slice A (parallel or next)** — pipeline IR + SQL runtime + JSON I/O + hooks + classic app import  


Implementation task tracking and **PR review deferrals / tech debt** live under  
[`docs/ai-generated/tasks/developer-module-p0/`](../ai-generated/tasks/developer-module-p0/)  
(especially [review-followups-tech-debt.md](../ai-generated/tasks/developer-module-p0/review-followups-tech-debt.md)).

## How to use these docs

1. Treat `workbench-functional-inventory.md` as the **parity checklist**, not a React design.  
2. Start implementation design from **§15 prioritization** (P0 first).  
3. Map FR IDs (`CD-*`, `AS-*`, `UI-*`, `SE-*`, `SY-*`, and §5 cross-cutting) to epics/stories.  
4. Use Journeys A–D as end-to-end acceptance scenarios.  
5. Treat **Data Pipelines** as a parallel modernization track: preserve pipe semantics; replace XML-only I/O and Swing canvas. Do not require pixel parity with E2Designer.  
6. Related existing planning may live under `docs/ai-generated/tasks/` (e.g. pure-react SPA / unified UI plans); keep those separate from this FR source of truth.

## Out of scope here

React component architecture, API schemas, visual mockups, and Server Admin / Content Explorer authoring UIs (except where Developer module must integrate).
