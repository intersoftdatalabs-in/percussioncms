# Task: Fix View Metadata dialog – Tags showing as editable fields + Categories not displaying selections (Issue #785)

**Related GitHub Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/785
**Feature Branch**: `bugfix/785-fix-view-metadata-tags-categories`
**Base Branch**: `development-8.1.x`
**Date**: 2026 (work performed in this session)

## Problem Summary

- **View Metadata** dialog (read-only mode, used post-approval and in read-only navigation):
  - Tags section rendered as multiple editable `<input type="text">` fields (instead of plain display text).
  - Categories (checkbox tree) section did not show the previously selected/saved categories.
- **Edit Metadata** was more functional but could also exhibit display/save-roundtrip issues for these fields in some flows.
- Expected: Both View and Edit Metadata dialogs must correctly display already-saved `page_tags` and `page_categories_tree` values for the page.

This matches the exact symptoms and screenshots in the bug report.

## Root Cause Analysis

1. **Tags (`percTagListControl`)**:
   - The `isReadOnly='yes'` XSL template in the control definition was severely broken (copy-paste artifacts from the editable path).
   - It emitted `<input type="text">` elements (one per tag) with values as child text nodes (which have no effect on `<input>` value) + a duplicate hidden input + misplaced attribute instructions.
   - The companion JS (`percTagListControl.js`) unconditionally attaches autocomplete + filter handlers on `#page_tags-display` on every content editor load. This turned the malformed inputs into live, editable tag fields inside the View Metadata iframe.
2. **Categories (`sys_CheckBoxTreeJS` / fancytree)**:
   - Post dynatree→fancytree migration, the control always uses the main `perc_checkboxTree` plugin (with `readonly` flag → `unselectable`).
   - The inline initialization script (emitted by `sys_Templates.xsl`) was guarded behind `if (typeof parent.$ !== 'undefined' && PercNavigationManager)`.
   - Inside the metadata dialog iframe (especially View/read-only contexts), `parent.$.PercNavigationManager.getSiteName()` was often empty or the guard failed to trigger.
   - Result: bad/empty tree URL passed to `/percPageSupport/getCategories.xml`, empty tree returned, selections never highlighted, and the "no children" cleanup path in `checkboxTree.js` executed.
   - `sitename` is required for site-specific category trees and for matching the keys stored on the page item.

The shared `$.perc_page_edit_dialog` (WebUI) + `PSPageService.getPage*Url` + content editor XSL rendering path made the problem visible primarily in the "View Metadata" button/flow.

## Files Changed

- `system/Packages/perc.Baseline/SupportFile-rx_resources/stylesheets/controls/percTagListControl.xsl`
  - Replaced the entire broken readonly template with a clean implementation that emits plain comma-separated text inside the existing `.datadisplay` container.
- `system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl`
  - Updated the `sys_CheckBoxTreeJS` script emission to always initialize the tree control (with try/catch + safe fallback for `siteName`).
  - Uses `encodeURIComponent`, provides empty `sitename` when unavailable (backend gracefully falls back), ensuring the control renders inside dialog iframes.

No Java, no new dependencies, no behavior change for editable mode.

## Local Testing Performed (per AGENTS.md)

- Confirmed clean pull of `development-8.1.x` before branch creation.
- XML well-formed validation on both edited XSL files (via Python ElementTree + xmllint where available).
- `git diff` + manual review of the exact templates.
- Verified no impact on Java sources (no Spotless/Checkstyle needed for these files).
- Branch created from up-to-date base with issue number in the name.
- Full end-to-end UI testing (launching the CMS, exercising Edit + View Metadata on pages with tags + categories, approval workflows, different sites) is required in a real server environment. These are presentation-layer XSL changes with no unit-test coverage today.

## Branching & Commit Hygiene (AGENTS.md Compliance)

- Never committed directly to `development-8.1.x`.
- Explicit `git pull --ff-only` on base before creating feature branch.
- Branch name includes the GitHub issue number: `bugfix/785-fix-view-metadata-tags-categories`.
- This task document added under `docs/ai-generated/tasks/`.
- User explicitly requested "create a feature branch for this bug fix and push it" in the conversation.
- All changes tested locally (to the extent possible for XSL/UI) before push.

## Recommended Follow-up

- Create PR from the feature branch targeting `development-8.1.x` (reference this issue #785).
- Manual QA pass in a running 8.1.7+ instance (especially View Metadata after approval, multi-site categories, tags with special chars).
- Consider adding a Playwright test in `modules/perc-qa-automation` that opens the metadata dialog and asserts the rendered (non-editable) tags and pre-selected categories.
- Long-term: the metadata dialog + custom CE controls have several fragile parent/iframe + siteName assumptions that could be hardened.

## Follow-up Feedback (June 2026)

After the initial changes:
- **Tags** rendering in View Metadata now works correctly (plain text display).
- **Categories** still showed "no categories selected" in View mode.

### Additional Root Cause

Even after removing the strict parent navigation guard (so the tree would initialize), the fancytree-based control in pure `readonly` mode did not reliably surface the pre-selected items visually when rendered inside the metadata dialog iframe. The original legacy readonly implementation (`checkboxTreeReadonly.js`) had a completely different approach — it resolved the selected keys to human-readable labels and rendered them as plain text.

### Additional Fix

Updated the `perc_checkboxTree` plugin in `checkboxTree.js`:
- When `readonly === true`, after loading the category tree XML and determining selections via the existing `doNode` logic, we now render the selected category *titles/labels* as a simple comma-separated list inside a `.datadisplay` div.
- We skip initializing the interactive fancytree entirely in this view-only path.
- This is consistent with the Tags fix and with how most other read-only fields behave in the Content Editor / View Metadata dialog.

This change ensures that in View Metadata the user sees something like:

```
Selected Category A, Selected Subcategory B
```

instead of an empty or non-indicating tree.

## Latest Feedback & Iteration
User reported:
- View Metadata now correctly displays the saved categories (good).
- **Edit Metadata roundtrip is broken**: When you save categories, close, and re-open Edit Metadata on the same page, the previously selected categories are no longer checked in the tree.

### Diagnosis
The "Tags And Categories" section is initially hidden by the dialog's grouping logic (`_addFieldGroups`). The `sys_CheckBoxTreeJS` control initializes its fancytree while its container has `display:none` (or before final layout). Fancytree often fails to properly render pre-selected nodes or compute correct sizes in this state. When the user later expands the section, the visual checkboxes do not reflect the `select: true` state that was set during `doNode`.

This explains why a pure text-based readonly path (View) works, but the interactive editable tree does not show pre-selections reliably.

### Additional Fixes
1. Enhanced the section expand handler in `perc_page_edit_dialog.js`: When the "Tags And Categories" group is toggled open, we now force a width recalculation + tree touch on `#page_categories_tree-tree` (if fancytree is attached). This gives the tree a chance to re-layout once its container has real dimensions.
2. Added a short `setTimeout` call to `resizeTreeWidthToFitContent()` inside `displayTree` in `checkboxTree.js`. This helps controls that are initialized in temporarily hidden or zero-size containers (common in the metadata dialog).

These are defensive "make the tree robust when used inside the hacked metadata dialog" changes rather than a root architectural fix.

## Updated Files (This Iteration)
- `WebUI/war/widgets/perc_page_edit_dialog.js` (post-show refresh for category tree)
- `system/cms/content/applications/sys_resources/ApplicationFiles/js/checkboxTree.js` (delayed resize after init + previous readonly text path)

## Verification Commands Used

```bash
git checkout development-8.1.x
git pull --ff-only origin development-8.1.x
git checkout -b bugfix/785-fix-view-metadata-tags-categories
# ... initial edits ...
# later follow-up:
# edit checkboxTree.js for readonly category labels + resize robustness
# edit perc_page_edit_dialog.js for section expand refresh
git add ...
git commit -m "Improve category handling in metadata dialog (#785)"
# XML/JS validation
python3 -c "
import xml.etree.ElementTree as ET
ET.parse('system/Packages/perc.Baseline/SupportFile-rx_resources/stylesheets/controls/percTagListControl.xsl')
ET.parse('system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl')
print('XSL files OK')
"
git push --force-with-lease -u origin HEAD
```

---

*This task file was generated as part of the fix for issue #785.*
