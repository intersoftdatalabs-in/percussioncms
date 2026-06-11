# Task 777: Asset Status Border Indicator (Red Dotted Border) – Publish/Approval Awareness + Recycle False-Positive Fix

**GitHub Issue**: [#777](https://github.com/intersoftdatalabs-in/percussioncms/issues/777)

## Summary

The red dotted border shown around File widgets (and File List / Auto List widgets) in the page editor (Layout + Content tabs) and Preview is currently driven **only** by a "is this asset in the recycle bin?" check.

Per the clarified requirements:
- The border should primarily highlight **assets that are not in an approved/publishable workflow state** (i.e. will not appear on the published site).
- The recycle check must be made smarter: an asset that has a `RecycledContent` relationship **but also has a valid `FolderContent` relationship** must **not** trigger the border.

This folder contains the full design spec and implementation guidance.

## Key Documents

- **[SPEC.md](./SPEC.md)** – Full design specification, current vs. desired behavior, technical analysis, proposed API changes, implementation plan, risks, and success criteria.

## Quick Context from Investigation

- Visual: `.perc-recycled-asset` (outline: dotted red) in `perc_decoration.css`.
- Logic: `sys_assembly.vm` → `$rx.pageutils.isInRecycler($asset_id)` → `PSPageUtils.isInRecycler` (Hibernate relationship query for config 8).
- Publishable states (standard): `Pending` + `Live` (see `PSWorkflowHelper`).
- Valid folder presence: `FolderContent` relationship (config 3).

## Recommended Branch Name (per AGENTS.md)

`bugfix/777-asset-status-border-publish-state`

## Next Actions

1. Review this spec with the team (use the "Implementation Plan" section inside SPEC.md as the basis for story/task breakdown).
2. Create the feature branch from latest `development-8.1.x` (after pulling).
3. Implement per the plan.
4. Add `IMPLEMENTATION_NOTES.md` and `PR_DESCRIPTION.md` (or GitHub PR body) to this folder before opening the PR.
5. Run `./mvn-env.sh spotless:apply && ./mvn-env.sh verify` (at minimum on the sitemanage + WebUI modules + any affected tests).

## Related Code Locations (from analysis)

- Assembly template: `system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm:402`
- Core logic to extend: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java:924`
- Workflow states: `projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSWorkflowHelper.java` (WF_APPROVE_STATES, isItemInApproveState, etc.)
- CSS: `WebUI/war/css/perc_decoration.css:40`

---

*Generated as part of the response to GitHub issue #777.*
