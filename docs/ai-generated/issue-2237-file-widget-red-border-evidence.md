# Issue #2237 — File widget red dotted border after recycle/recreate (Slice 1 evidence)

| Field | Value |
|-------|-------|
| **Issue** | [#2237](https://github.com/intersoftdatalabs-in/percussioncms/issues/2237) (Slice 1 of [#777](https://github.com/intersoftdatalabs-in/percussioncms/issues/777)) |
| **Follow-ups** | Product fix [#2238](https://github.com/intersoftdatalabs-in/percussioncms/issues/2238); Playwright residual [#2239](https://github.com/intersoftdatalabs-in/percussioncms/issues/2239) |
| **Date (UTC)** | 2026-08-07 |
| **Operator** | Grok night-issue-prs (model grok-4.5) |
| **Purpose** | Repro path + root-cause **classification** with code/DOM evidence. **No product fix** in this slice. |

## Classification (summary)

| Question | Answer |
|----------|--------|
| Orphan / stale relationship? | **Yes (primary).** Widget still binds the page to the **recycled content id** via AA/asset-widget relationship. That dependent still has a `RecycledContent` relationship (`configId = 8`). |
| Pure UI chrome bug? | **No.** Red dotted outline is **intentional chrome** for recycled assets (class `perc-recycled-asset`). |
| Both? | **Effectively both surfaces:** data integrity (stale page→asset id) + intentional UI that paints chrome when `isInRecycler(assetId)` is true. Fix belongs in relationship/rebind/recycle lifecycle (#2238), not “delete the CSS class” alone. |
| Why downloads still work | Recycled items keep binary content until purged; assembly can still resolve a link for the bound (recycled) id. Red outline is independent of download success. |
| Confirmed workaround (#777) | Purge/delete the old file **from Recycling** → `isInRecycler` becomes false → red outline goes away. |

## DOM / CSS that paints the red border

### CSS class

```css
/* WebUI/war/css/perc_decoration.css (also WebUI/src/.../legacy/perc_decoration.css) */
.perc-recycled-asset {
  outline-style: dotted;
  outline-color: red;
}
```

Matches the reported “red dotted border” on Layout / Content / Preview (preview uses the same assembled widget chrome when edit-mode / decoration styles apply).

### Where the class is applied (assembly)

```velocity
## system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm
#set($isRecycled = $rx.pageutils.isInRecycler($asset_id))##
#if("$isRecycled" == "true")##
    #set($class = "perc-widget perc-recycled-asset")##
    #set($tooltip = "Asset is in Recycle Bin")##
#else
    #set($class = "perc-widget")##
#end##
<div class="$class" ... assetId="$asset_id" ownerId="$owner_id" title="$tooltip" ...>
```

- **DOM:** outer widget wrapper `<div class="perc-widget perc-recycled-asset" assetId="…" ownerId="…" title="Asset is in Recycle Bin">`
- **Selector for #2239:** `.perc-widget.perc-recycled-asset` (or `[assetId]…` with that class)

### How `$asset_id` is chosen

From the same macro: first pair in `widget.ownerAssetIds` (`ownerId`, `assetId`), populated when widget contents are set:

```java
// projects/sitemanage/.../PSWidgetAssemblyContext.java
this.widgetContents.stream()
    .filter(ai -> ai.getOwnerId() != null && ai.getId() != null)
    .map(ai -> new PSPair<>(ai.getOwnerId().toString(), ai.getId()))
    .forEach(ownerAssetIds::add);
widget.setOwnerAssetIds(ownerAssetIds);
```

Those ids come from the **active assembly / related-content relationship** for the widget instance on the page (set via WebUI `PercAssetService.set_relationship` → `ASSET_WIDGET_REL` POST).

### `isInRecycler` truth source

```java
// projects/sitemanage/.../PSPageUtils.java
public Boolean isInRecycler(String itemId) {
  ...
  List<PSRelationshipData> psRelationshipDataList =
      relationshipService.findByDependentIdConfigId(
          idMapper.getContentId(itemId), PSRelationshipConfig.ID_RECYCLED_CONTENT);
  return !psRelationshipDataList.isEmpty();
}
```

Constants:

| Constant | Value |
|----------|-------|
| `PSRelationshipConfig.TYPE_RECYCLED_CONTENT` | `"RecycledContent"` |
| `PSRelationshipConfig.ID_RECYCLED_CONTENT` | `8` |

Parallel check in `PSRecycleService.isInRecycler`: relationship filter category recycled / dependent = content id.

**Implication:** chrome is driven by **content id still being a dependent of a RecycledContent relationship**, not by path name alone and not by “is there a live twin at the same Assets path.”

## Relationship model (what to capture on a live repro)

When reproducing on a stocked CMS, capture these for one red-bordered File widget:

| Evidence item | How / where |
|---------------|-------------|
| Page content id | Editor / path API / `ownerId` on widget div |
| Widget instance id | `widgetId` attribute on widget div |
| Bound asset id | `assetId` on widget div (recycled id if bug present) |
| Recreated asset id | Path under `/Assets/...` after recreate (new content id) |
| Page↔asset AA row | Dependent = bound asset id; owner = page; widget sys_id/slot props as configured |
| Recycle row | `PSRelationshipData` where `dependentId = <bound asset content id>` and `configId = 8` (`RecycledContent`) |
| DOM | `div.perc-widget.perc-recycled-asset`, tooltip `Asset is in Recycle Bin` |

**Expected under failure:**

1. Widget `assetId` = **old** (recycled) content id.
2. RecycledContent relationship exists for that id.
3. A **new** `percFileAsset` may exist at the same folder path with a **different** content id.
4. Class `perc-recycled-asset` present → red dotted outline.
5. Link/download may still succeed for the recycled binary until purge.

**Expected after workaround (empty Recycling of that item):**

1. RecycledContent row for old id gone (purged).
2. `isInRecycler` → false → class drops to `perc-widget` only.
3. (Product fix should also rebind or clear stale AA when recycle/recreate happens — #2238.)

## Step-by-step repro (refined)

Uses Editor + Assets + Recycling (original #777 + recycle/recreate path from title/comments).

### Prerequisites

- CMS with **File** widget package (`percFile` / `percFileAsset`) installed.
- A site/page path equivalent to `widget-test-page/file/index.html` **or** any page that can host two File widgets.
- Ability to recycle assets and empty/purge Recycling.

### Steps

1. **EDITOR** → open page (e.g. `widget-test-page/file/index.html`).
2. **Layout** → add **two File widgets**.
3. Browse/select a distinct **file asset** for each widget (under Assets).
4. Approve/publish the file assets and the page as needed.
5. Confirm **no** red outline yet (both widgets class `perc-widget` only).
6. In Assets, **recycle** one of the bound file assets (the one you will recreate).
7. **Recreate** a file asset with the **same name/path** under Assets (new content id).
8. Optionally re-open the page without rebinding the widget (bug path: relationship still points at recycled id).
9. Approve/publish page and/or new asset as in QA steps.
10. Observe Layout / Content / Preview: recycled-bound widget shows **red dotted outline**.
11. DevTools: confirm wrapper has `perc-recycled-asset` and `title="Asset is in Recycle Bin"`; note `assetId` vs recreated id.
12. **Workaround check:** purge the recycled copy from Recycling → reload page → outline should clear (per #777 collaborator confirmation).

### Non-goals of this slice

- Do not change product code (#2238).
- Do not add Playwright residual (#2239) beyond documenting the selector.

## Live environment notes (2026-08-07, this agent run)

| Item | Observation |
|------|-------------|
| Stack | Existing `perc-matrix-cms-h2` (`perc-devctl` qa-up style cell) |
| URL | `http://127.0.0.1:9993` (host `9993` → container `9992`) |
| Health | `perc-devctl.py qa-health --url http://127.0.0.1:9993/Rhythmyx/login` → **RESULT:OK HTTP:200** |
| Auth | Admin + generated password file in container works with Basic + `RX_USEBASICAUTH` |
| Sites | `GET /Rhythmyx/services/sitemanage/site/` → **`SiteSummary: []` (empty)** |
| Content types | **No `percFileAsset`** in `GET /Rhythmyx/services/contenttypes` on this cell (has other assets + `percFileAutoList` only) |
| Live UI repro | **Blocked** on this cell without reinstalling File widget package + seeding site/page/assets |
| Screenshots | Use existing attachments on parent **#777** (Layout / Content / Preview red outline) |

**Conclusion for live H2 this run:** code-path + #777 QA/workaround evidence are sufficient to classify root cause. Full click-through repro requires a cell with FileAssetWidget + seeded `widget-test-page/file` (or equivalent).

## Root-cause narrative (for #2238)

1. User binds File widget → AA/asset-widget relationship stores **content id A**.
2. User recycles asset A → system adds **RecycledContent** relationship for A; A no longer lives under Assets folder path.
3. User recreates file at same path → **new content id B**.
4. Widget relationship often still points at **A** (unless user explicitly rebinds to B).
5. Assembly: `isInRecycler(A)` true → `perc-recycled-asset` → red dotted outline + tooltip.
6. Download may still work for A until purge; publishing new asset B does not fix chrome if relationship still targets A.
7. Emptying Recycling of A removes RecycledContent → chrome clears even if AA still references A (border symptom gone; integrity may still be wrong until rebind/purge of AA).

### Fix direction hints (out of scope here; for #2238)

Prefer data integrity over CSS removal:

- On recycle: clear or mark page-widget relationships to recycled assets; surface orphan tray.
- On recreate/rebind by path: ensure relationship retargets to **new** content id.
- Optional: if bound asset is recycled, refuse “clean” publish chrome until rebind (keep warning but drive rebind UX).
- Do **not** only remove `.perc-recycled-asset` CSS — that hides legitimate recycled warnings.

## File map (evidence pointers)

| Area | Path |
|------|------|
| Red outline CSS | `WebUI/war/css/perc_decoration.css` (`.perc-recycled-asset`) |
| Class application | `system/cms/.../vm/sys_assembly.vm` |
| `isInRecycler` (assembly JEXL) | `projects/sitemanage/.../PSPageUtils.java` |
| Recycler service check | `projects/sitemanage/.../PSRecycleService.java` |
| Owner/asset id population | `projects/sitemanage/.../PSWidgetAssemblyContext.java` |
| File widget package | `modules/perc-packages/.../perc.FileAssetWidget/` (`percFile.xml`) |
| Client set relationship | `WebUI/war/services/PercAssetService.js` (`set_asset_relationship`) |
| Recycle config constants | `system/.../PSRelationshipConfig.java` (`TYPE_RECYCLED_CONTENT`, `ID_RECYCLED_CONTENT=8`) |

## Acceptance checklist (#2237)

- [x] Step-by-step repro path documented (and H2 non-repro env notes when File package / sites absent)
- [x] Evidence linking border to **stale relationship + intentional recycled UI class** (code + #777 workaround)
- [x] DOM/CSS class named: `perc-recycled-asset` / `outline-style: dotted; outline-color: red`
- [x] Classification comment for parent #777 / this issue
- [x] No product code fix in this PR
- [x] Leave issues unassigned

> Co-Authored by Grok Build using grok-4.5 with agent main.
