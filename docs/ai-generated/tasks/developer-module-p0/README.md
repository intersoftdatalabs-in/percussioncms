# Developer module — P0 (CMS design tools)

| Field | Value |
|-------|--------|
| **Status** | **In progress** — track slices on the GitHub issue (not this file) |
| **Progress tracker** | **[#1622](https://github.com/intersoftdatalabs-in/percussioncms/issues/1622)** — living todo list (shipped / open / next) |
| **FR source** | [`docs/developer-module/workbench-functional-inventory.md`](../../developer-module/workbench-functional-inventory.md) §15 P0 |
| **Pipeline track** | [`docs/developer-module/data-pipeline-engine-inventory.md`](../../developer-module/data-pipeline-engine-inventory.md) (parallel; not blocking P0) |
| **Server runtime map** | [`docs/developer-module/data-pipeline-server-runtime-map.md`](../../developer-module/data-pipeline-server-runtime-map.md) |
| **Related** | [design-templates-item-types](../design-templates-item-types/README.md) (CM1 template library — complementary, not a substitute) |
| **Review follow-ups** | [review-followups-tech-debt.md](./review-followups-tech-debt.md) — deferred PR-review items (not FR roadmap) |
| **API gaps** | [content-type-api-gaps.md](./content-type-api-gaps.md) |

## Why progress lives on the issue

Updating "done / next" checklists in this markdown on **every** feature PR caused constant merge conflicts across parallel slices.

**Convention:**

1. Track slice progress **only** on [#1622](https://github.com/intersoftdatalabs-in/percussioncms/issues/1622) (GitHub task list).
2. Feature PRs should **not** edit the shipped/next lists in this file.
3. Link PRs to the issue (`Refs #1622` / "Part of #1622").
4. Cross-cutting review debt still goes in [review-followups-tech-debt.md](./review-followups-tech-debt.md).

## P0 scope (from FR)

| Capability | FR IDs (sample) | SPA slice |
|------------|-----------------|-----------|
| Connection / design session | §5.1 | Use SPA login session (no multi-server profiles in v1) |
| Content types list/open | CD-01 | **P0.1** list via REST |
| Content type fields editor | CD-03–CD-11 | P0.2+ (API survey first) |
| Shared fields / system def | CD-15–CD-16 | P0.3 |
| Keywords | CD-17 | P0.3 |
| Templates + source/bindings/slots | AS-03–AS-06 | P0.4 |
| Slots | AS-01–AS-02 | P0.4 |
| Communities + object ACL | SE-01, SE-04, §5.4 | P0.5 |
| Locking / save / problems | §5.2–5.3 | Cross-cutting as editors land |

## Product locks

- React + TypeScript only under `WebUI/src/main/ts`  
- No jQuery in SPA  
- Prefer `rest` module OpenAPI; document gaps when Workbench design objects lack public REST  
- Shell ≠ done: list is a gate, not acceptance of full CT editor  
