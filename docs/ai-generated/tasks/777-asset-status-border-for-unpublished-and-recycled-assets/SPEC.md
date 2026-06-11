# Design Spec: Asset Status Border Indicator (Red Dotted Border) for Unpublished / Problematic Assets

**Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/777  
**Related to**: 8.1.7 File (and File List / Auto List) widgets showing erroneous red dotted borders after assets are approved and published.  
**Status**: Draft for review / implementation planning  
**Date**: 2026-05-29  
**Author**: Grok (AI-assisted analysis based on codebase review)

---

## 1. Problem Statement

### Current Behavior (Bug)

- The red dotted border (CSS class `.perc-recycled-asset`) appears around File widgets (and potentially File Auto List widgets) in:
  - Layout tab (page editor)
  - Content tab
  - Full Page Preview
- This happens **even after** the backing `percFileAsset` (or listed files) have been approved and published.
- The border is driven exclusively by `PSPageUtils.isInRecycler(...)`, which only checks for the existence of a `RecycledContent` relationship (config ID 8).
- Tooltip is always "Asset is in Recycle Bin".
- When two File widgets are added, often only "one of the files" shows the border (non-deterministic from user perspective; related to finder results + Velocity variable scoping in assembly).

### User Clarified Requirements (Desired Behavior)

1. **Primary purpose of the indicator**: Highlight **assets that are not approved and will not be published**.
   - Standard publishable/approved states for most assets and pages: `Pending` and `Live` (see `PSWorkflowHelper.WF_APPROVE_STATES` and `WF_PUBLIC_STATES`).
   - Assets in other states (Draft, Quick Edit in some contexts, Archive, custom workflow states that are not publishable, etc.) **should** receive the visual treatment so authors know the widget content will be missing or stale on the live site.
2. **Recycle bin nuance (anti-false-positive rule)**:
   - If an asset **has** a `RecycledContent` relationship **but also has a valid `FolderContent` relationship** (i.e., it is still visible and accessible in a normal folder hierarchy), **do not show the border**.
   - This covers cases of stale/orphaned recycle relationships left behind after restore, partial recycle operations, or data inconsistencies after publish/approve workflows.
   - The border for "recycled" should **only** appear for assets that are *exclusively* in the recycle bin (no valid folder parent relationship).
3. The indicator must work for:
   - Simple File / Image / Flash asset widgets (`percFile`, etc.).
   - Auto-list widgets (`percFileAutoList`, etc.) — at minimum on the widget container; ideally per-listed-item in future.
   - Both shared and local assets.
   - Preview (non-edit) rendering as well as editor surfaces.

### Impact

- Authors lose trust in the editor when "green" (published) assets incorrectly show as problematic.
- False recycle detection pollutes the UI after normal publish/approve flows.
- The current class name and tooltip ("recycled-asset", "Asset is in Recycle Bin") no longer match the evolved purpose of the feature.

---

## 2. Current Implementation Analysis

### Visual Trigger

- **CSS**: [WebUI/war/css/perc_decoration.css:40-43](/home/nate/projects/java8/percussioncms/WebUI/war/css/perc_decoration.css)

  ```css
  .perc-recycled-asset {
      outline-style: dotted;
      outline-color: red;
  }
  ```
- Applied to the outer `<div class="perc-widget ...">` wrapper generated during assembly.

### Assembly Logic (Velocity)

- [system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm:377-409](/home/nate/projects/java8/percussioncms/system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm) (inside `region` macro).
- For every `WIDGET` result:
  - Pulls `ownerAssetIds[0]` (from `PSWidgetAssemblyContext` populated by widget finders).
  - Calls `$rx.pageutils.isInRecycler($asset_id)`.
  - If true → adds `perc-recycled-asset` class + overrides tooltip.
- The `<div>` and attributes (`assetId`, `ownerId`) are emitted for **both** edit and preview modes.

### Backend Check

- [projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java:924-939](/home/nate/projects/java8/percussioncms/projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java) (`isInRecycler` JEXL method exposed via `pageutils.extension`).
  - Uses autowired `IPSRelationshipService.findByDependentIdConfigId(..., ID_RECYCLED_CONTENT = 8)`.
  - Hibernate query on `PSRelationshipData`.
- Separate implementation lives in `IPSRecycleService` / `PSRecycleService` (uses legacy `PSRelationshipProcessor` + category filter). These two paths can return inconsistent results (stale Hibernate session vs. direct processor).
- `PSWidgetAssemblyContext.setWidgetContents(...)` builds the `ownerAssetIds` list from `PSRenderAsset` objects produced by `PSRelationshipWidgetContentFinder` (default for File widgets).

### Workflow / Publishability Knowledge (Already Exists)

- `PSWorkflowHelper`:
  - `WF_APPROVE_STATES = {Live, Pending}`
  - `WF_PUBLIC_STATES = {Live, Pending, Quick Edit}`
  - `isItemInApproveState(int contentId)`
  - `isItemInStagingState(...)`
- `PSAbstractWorkflowExtension.WorkflowItem` + `isPublishable(...)` / `getPublishRevision(...)` (used by item management and publishing).
- `IPSItemWorkflowService` exposes many transition and state methods.
- No current JEXL exposure on `PSPageUtils` (the main object available in widget/page assembly Velocity) for "is this asset publishable / in good state?"

### Relationship Constants

- `PSRelationshipConfig`:
  - `ID_RECYCLED_CONTENT = 8`, `TYPE_RECYCLED_CONTENT = "RecycledContent"`
  - `ID_FOLDER_CONTENT = 3`, `TYPE_FOLDER_CONTENT = "FolderContent"`
  - `ID_ACTIVE_ASSEMBLY = 1`, `TYPE_ACTIVE_ASSEMBLY`, `TYPE_LOCAL_CONTENT`, etc.

### Other Callers of Recycle Checks

- Publish filters (`PSPublicAssetItemFilterRule`)
- Search, path services, nav helpers, item workflow service
- These generally want the *strict* "has recycle rel" semantics and should **not** be changed by this work.

---

## 3. Proposed Design

### 3.1 New / Enhanced JEXL API on PSPageUtils

Add the following method (and supporting private helpers):

```java
@IPSJexlMethod(
    description = "Returns true if the asset (by id/guid) should be visually highlighted in the editor/preview as problematic (will not publish or is truly recycled).",
    params = {@IPSJexlParam(name = "itemId", description = "asset GUID or content id string")},
    returns = "boolean")
public Boolean shouldShowAssetProblemBorder(String itemId);

/**
 * Optional richer variant for future use (to drive dynamic tooltips).
 */
public AssetProblemInfo getAssetProblemInfo(String itemId);
```

`AssetProblemInfo` (new small DTO, or just use a Map for Velocity friendliness):
- `boolean showBorder`
- `String reason` ("RECYCLED", "NOT_APPROVED", "NONE")
- `String currentState` (if workflowable)
- `String tooltipText`

**Logic inside `shouldShowAssetProblemBorder` (and `getAssetProblemInfo`)**:

1. If `itemId` blank → false.
2. **Recycle check (with valid-folder override)**:
   - `hasRecycledRel = relationshipService.findByDependentIdConfigId(contentId, ID_RECYCLED_CONTENT).size() > 0`
   - `hasValidFolderRel = relationshipService.findByDependentIdConfigId(contentId, ID_FOLDER_CONTENT).size() > 0` (or equivalent via folder helper / legacy processor for robustness)
   - `trulyRecycled = hasRecycledRel && !hasValidFolderRel`
3. If `trulyRecycled` → return true (reason = RECYCLED).
4. Else, attempt to load `PSComponentSummary` + workflow state via `cmsObjectMgr` / `PSWebserviceUtils` / `IPSItemWorkflowService`.
5. If the item has no workflow (or workflowId <= 0) → treat as not problematic for this rule (or log).
6. `isPublishable = workflowHelper.isItemInApproveState(...)` (or the full `isPublishable` logic from `PSAbstractWorkflowExtension`, taking staging into account if we can detect context).
7. If `!isPublishable` → return true (reason = NOT_APPROVED).
8. Otherwise → false.

**Important notes for implementation**:
- Prefer the legacy `PSRelationshipProcessor` path (or a shared utility) for both recycle and folder checks inside this method to reduce staleness vs. Hibernate.
- For assembly-time calls we are usually in a preview/edit context; staging vs. prod publishability may differ. Start with non-staging "approve state" logic (Pending + Live) and note staging as a follow-up.
- Local vs. Shared assets: Local assets often use the "LocalContent" workflow. The same `isItemInApproveState` helper should work.
- Cache the result per assembly render if performance becomes an issue (assembly is already expensive).

### 3.2 Update the Assembly Template

In `sys_assembly.vm` (widget wrapper section):

- Replace the direct `isInRecycler` call with the new `shouldShowAssetProblemBorder($asset_id)`.
- Set dynamic class:
  - If recycled → keep `perc-recycled-asset` (for backward CSS) + add `perc-problem-asset`.
  - If not-approved → `perc-problem-asset` (new primary class) — or just keep using the existing class name for minimal diff.
- Set dynamic tooltip based on reason (e.g. "Asset is not approved/published (current state: Draft)" or "Asset is in the Recycle Bin and not accessible via any folder").
- Still emit the `assetId` / `ownerId` attributes for debugging and future client-side enhancements.

Recommendation on class names (to minimize disruption):
- Keep `.perc-recycled-asset` working exactly as before (authors may have custom CSS).
- Introduce `.perc-problem-asset` as the new semantic class (same red dotted style initially).
- In the VM, always add `perc-problem-asset` when the border should show; also add the old class when the *reason* is pure recycle.

Update the extension XML description for `isInRecycler` (it remains for other uses) and document the new method.

### 3.3 CSS

- [WebUI/war/css/perc_decoration.css](/home/nate/projects/java8/percussioncms/WebUI/war/css/perc_decoration.css) — add:

  ```css
  .perc-problem-asset,
  .perc-recycled-asset {
      outline: 1px dotted red;   /* or keep the split properties */
  }
  ```
- Consider making the style slightly different (e.g., dashed vs dotted, or warning yellow) for "not approved" vs "recycled" in a future iteration. For v1 keep identical.

### 3.4 Tooltip & Editor Experience

- Make tooltip dynamic (requires small change in how the region macro builds the `title` attribute or uses `data-*` + JS enhancement).
- In the Layout/Content editor decoration layer, the existing `title` attribute on the widget div is already used for hover.

### 3.5 File Auto List / Multi-Item Widgets

- The widget *container* will correctly reflect the status of its configuration asset.
- Individual listed files inside an auto-list currently have no wrapper that receives the class.
- **Out of scope for initial fix** but recommended for follow-up: pass problem status down or perform per-result checks inside the list widget templates (similar to how `widgetContents` works). Document this in the spec as known limitation / future work.

### 3.6 Rename / Deprecation Considerations

- Do **not** delete `isInRecycler` — many other call sites depend on the strict "has recycle rel" meaning.
- The JEXL method name `isInRecycler` on pageutils can stay; we are adding a higher-level `shouldShowAssetProblemBorder`.
- Update the extension description to clarify the distinction.

---

## 4. Implementation Plan (Suggested PR / Task Breakdown)

1. **Core logic (no UI change yet)**
   - Add `shouldShowAssetProblemBorder(String)` + supporting `getAssetProblemInfo(...)` (or internal `AssetProblem` record) to `PSPageUtils`.
   - Implement the dual recycle+folder check + workflow state check using existing helpers (`PSWorkflowHelper`, relationship queries, `PSComponentSummary`).
   - Add unit tests (existing patterns in `PSSearchServiceTest`, path service tests, etc.).
   - Add the new method to the `pageutils.extension` XML with proper description.
2. **Assembly template + tooltip**
   - Update `sys_assembly.vm` to call the new method and produce dynamic class + title.
   - Keep backward-compatible class names.
3. **CSS**
   - Add `.perc-problem-asset` rule (alias or combined selector with existing rule).
4. **Documentation & messaging**
   - Update any internal comments.
   - Add a short note in the 8.1.7 (or next) release notes under "Editor / Widget Improvements".
   - Update the JEXL method docs if they are published.
5. **Verification**
   - Manual reproduction using the steps in #777 (two File widgets, publish assets, verify border disappears).
   - Test cases:
     - Asset in Draft / other non-approve state → border.
     - Asset in Pending → no border.
     - Asset in Live → no border.
     - Asset truly recycled (only recycle rel) → border + recycle tooltip.
     - Asset with both recycle + valid folder rel (post-restore scenario) → no border.
     - Local asset vs shared asset.
     - File Auto List widget (container level).
     - Preview vs editor rendering.
   - Performance: the check is per-widget during assembly; acceptable given existing cost of widget content finding.
6. **Optional polish (can be follow-up PR)**
   - Expose richer info for client-side (data attributes).
   - Per-item highlighting inside auto-list widgets.
   - Different visual treatment or icon for "recycled" vs "draft" reasons.
   - Invalidate Hibernate relationship cache or force fresh lookup inside the new method.

---

## 5. Risks & Mitigations

- **Staleness between relationship service implementations** → Mitigate by using the legacy processor path (consistent with `PSRecycleService`) inside the new check, or by adding a small `@Transactional` + `flush` if needed.
- **Workflow differences per content type / custom workflows** → The existing `isItemInApproveState` + `WF_APPROVE_STATES` are already used broadly; reuse them. Document that highly custom workflows may need the "approve" states list to be configurable in the future.
- **Performance in assembly** → The relationship query is cheap (indexed on dependent_id + config_id). Add the method only for widgets that have `ownerAssetIds`.
- **Backward CSS** → Keep the old class name forever.
- **Tooltip change** → Authors who relied on the exact string will see new text; this is desired (the old text was often wrong).

---

## 6. Out of Scope (for this spec / initial work)

- Changing publish filter rules or search behavior.
- Client-side decoration logic in `PercLayoutView.js` / `PercDecorationController.js` (the server-rendered class is authoritative).
- Full per-file-item borders inside every auto-list widget (future enhancement).
- Staging vs. production publishability nuance in the border (note it; implement the common "approve state" rule first).

---

## 7. Files Likely to Change

- `projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java`
- `system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm`
- `WebUI/war/css/perc_decoration.css`
- `system/Packages/perc.Baseline/Extension-Java/global/percussion/system/pageutils.extension`
- New or updated test classes under `projects/sitemanage/src/test/...`
- Possibly `PSWorkflowHelper` or a small shared utility if we extract the "has valid folder rel" check.
- `docs/ai-generated/tasks/777-.../` (this spec + any implementation notes)

---

## 8. Success Criteria

- After following the exact reproduction steps in #777, both File widgets are clean (no red dotted border) once their assets reach Pending or Live and have a valid folder relationship.
- An asset that is still in Draft shows the border with an appropriate "not approved" tooltip.
- An asset that is in the recycle bin *and* has no FolderContent relationship shows the border (with recycle tooltip).
- An asset that has a recycle relationship *plus* a FolderContent relationship does **not** show the border.
- No regression in other recycle checks (search, publish, navigation, etc.).
- The change is fully documented in this task folder and (if merged) in release notes.

---

**Next Steps After Approval of this Spec**
1. Create feature branch `bugfix/777-asset-status-border-publish-state` (per AGENTS.md: include issue number).
2. Implement per the plan above.
3. Run full `./mvn-env.sh verify` (or at minimum spotless + the affected module tests).
4. Self-review + peer review.
5. Update this folder with an `IMPLEMENTATION_NOTES.md` and `PR_DESCRIPTION.md` before opening the PR.

---

*This spec was generated from deep codebase exploration of the assembly pipeline, widget finders, workflow helper, relationship service, and the exact reproduction scenario described in the GitHub issue.*
