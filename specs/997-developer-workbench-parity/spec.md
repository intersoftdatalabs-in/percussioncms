# Feature Specification: Developer Workbench Parity with rx732

**Feature Branch**: `spec-997-developer-workbench-parity` (planning; execution stays on #1690 slice PRs)
**Created**: 2026-11-11
**Status**: Draft
**Parent**: [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690) ([8.2] Developer module — post-P0 follow-ons)
**Parity target**: Rhythmyx 7.3.2 Workbench (Eclipse RCP Designer)
**Functional baseline**: `docs/developer-module/workbench-functional-inventory.md` (reverse-engineered inventory v1: §§4–14 catalog, modules, navigators, wizards, journeys; §15 prioritization; §16 out of scope)

## Why this spec exists

Developer-module work executes slice-by-slice under #1690 (105 slice rows as of 2026-11-11: ~41 done, ~61 PR-open, 1 blocked), but no spec states what "done" means for the program: which rx732 capabilities must have React equivalents, and what evidence closes each one. This spec is that acceptance layer. It does **not** re-plan the slices — it references #1690's table for execution and adds the parity gate (gap matrix + success criteria + closeout), mirroring what `specs/2400-dce-explorer-parity/` did for the Explorer.

## Personas and roles

- **CMS implementer / developer**: defines content types/fields, templates/slots, Explorer UI config, community visibility, object ACLs, extensions, pipelines (per inventory §2.2).
- **QA/UAT owner**: runs parity trials against rx732 behavior.
- **Release manager**: owns the GA parity decision.

## Functional requirements (from inventory §§4–14)

| ID | Area (inventory) | Requirement |
|---|---|---|
| FR-001 | Content Design (§6) | Content types, fields, keywords, locales manageable at parity (create/read/update/delete + labels/descriptions/validation) |
| FR-002 | Assembly Design (§7) | Templates, slots, display formats manageable at parity (labels/descriptions, associations) |
| FR-003 | UI Elements Design (§8) | Explorer menus, searches, views manageable at parity |
| FR-004 | Security Design (§9) | Roles, users-on-roles, communities, ACLs manageable at parity |
| FR-005 | Workflow catalog (§4, design-time slice) | Workflow transitions/steps/roles/aging/inbox/assignment readable and editable at parity (full graph design stays Workbench-side per inventory §2.1) |
| FR-006 | System Design (§10) | Remaining system design objects manageable at parity |
| FR-007 | XML Server / Data Pipeline (§11 + companion) | Pipelines manageable per the data-pipeline inventory (tracked alongside, not redefined here) |
| FR-008 | Navigators, wizards, journeys (§§12–14) | Supporting navigators, create flows, and primary parity scenarios work end-to-end |

## Acceptance scenarios

1. **Given** an rx732 Workbench capability in §§4–14, **When** the gap matrix is consulted, **Then** exactly one row covers it with a status in {Missing, Partial, Present} — silent omission is not allowed.
2. **Given** a matrix row, **When** evidence is reviewed, **Then** **Present** requires merged implementation **plus** closed human QA (agent merge alone is insufficient — the #3102/#3109 rule from the Explorer program).
3. **Given** the program is declared complete, **When** the closeout is checked, **Then** every matrix row is Present, every #1690 slice row is done/verified, and no open parity defect remains.

## Out of scope

Per inventory §16 and program precedent: full workflow graph design, Package Manager/deployer parity, server admin/runtime ops, pixel-perfect Eclipse recreation. Eclipse Workbench, JSF admin/publishing, and GWT Package Manager remain separate efforts (992 spec precedent).

## Success criteria

- SC-001: Gap matrix covers 100% of inventory §§4–14 capabilities (measured by explicit row mapping, zero "unmapped" on either side).
- SC-002: Every matrix row reaches Present with closed human QA evidence.
- SC-003: #1690 slice table reads 100% done/verified with zero open PRs and zero unresolved threads.
- SC-004: Closeout note links matrix + slice evidence + QA results back to #1690.
