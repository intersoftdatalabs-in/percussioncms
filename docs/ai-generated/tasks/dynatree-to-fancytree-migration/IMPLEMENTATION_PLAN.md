# Dynatree to Fancytree Migration Implementation Plan

## API Specs

* https://wwwendt.de/tech/fancytree/doc/jsdoc/global.html

* https://github.com/mar10/fancytree/wiki/WhatsNew

## Overview

This document outlines the implementation plan to complete the migration from the deprecated dynatree jQuery plugin to its modern successor, fancytree. The migration is currently **partially complete** with approximately 300+ files still referencing dynatree.

## Migration Status Summary

|            Component            |   Status   |                                            Notes                                             |
|---------------------------------|------------|----------------------------------------------------------------------------------------------|
| PercFinderTree.js               | ✅ Migrated | Uses fancytree API                                                                           |
| Bulk File Upload Gadget         | ✅ Migrated | Uses fancytree, version updated to 1.1.7                                                     |
| CUI Widgets (Page/Asset Wizard) | ✅ Migrated | Page/Asset wizards now use fancytree                                                         |
| Tag List Widget                 | ✅ Migrated | Uses fancytree, references updated                                                           |
| Category List Widget            | ✅ Migrated | Uses fancytree, JS uses fancytree()                                                          |
| System Packages (Widgets)       | ✅ Migrated | Phase 2 completed: File/Image/Page Auto List, Calendar done                                  |
| Gadgets                         | ✅ Migrated | Phase 3 completed: All 10 gadgets migrated                                                   |
| Core System Files               | ✅ Migrated | Phase 4 completed: checkboxTree.js and theme CSS migrated                                    |
| Plugin Files                    | ✅ Migrated | Phase 5 completed: perc_assign_workflow_sites_folder_dialog.js and percWorkflow.css migrated |

### Phase 1 Completion Status

- [x] **1.1 Update RequireJS Configuration** - `cui/pages/_bootstrap.js` updated
- [x] **1.2 Migrate Page Wizard** - `pagewizard.js`, `pagewizard.viewmodel.js` migrated
- [x] **1.3 Migrate Asset Wizard** - `assetwizard.viewmodel.js` migrated

### Phase 2 Completion Status

- [x] **2.1 Tag List Widget** - `tagListWidgetControl.xsl`, `jquery.tagList.css` migrated
- [x] **2.2 Category List Widget** - `category.js`, `categoryListWidgetControl.xsl` migrated
- [x] **2.3 File Auto List Widget** - `resourceAutoListWidgetControl.xsl`, `jquery.resourceAutoList.css` migrated
- [x] **2.4 Image Auto List Widget** - `imageAutoListWidgetControl.xsl`, `jquery.imageAutoList.css` migrated
- [x] **2.5 Page Auto List Widget** - `jquery.pageAutoList.css` migrated
- [x] **2.6 Calendar Widget** - `percCalendarWidgetControl.xsl` migrated

### Phase 3 Completion Status

- [x] **3.1 Page By Status Gadget** - `perc_workflow_status_gadget.xml` dynatree script removed
- [x] **3.2 Membership Gadget** - `perc_membership_gadget.xml` dynatree script removed
- [x] **3.3 Global Variables Gadget** - `PercGlobalVariablesGadget.xml` dynatree script removed
- [x] **3.4 Google Setup Gadget** - `perc_google_setup_gadget.xml` dynatree script removed
- [x] **3.5 Form Tracker Gadget** - `PercFormTrackerGadget.xml` dynatree script removed
- [x] **3.6 License Monitor Gadget** - `perc_license_monitor_gadget.xml` dynatree script removed
- [x] **3.7 Effectiveness Gadget** - `perc_effectiveness_gadget.xml` dynatree script removed
- [x] **3.8 Blogs Gadget** - `PercBlogsGadget.xml` dynatree script removed
- [x] **3.9 Comments Gadget** - `perc_comments_gadget.xml` dynatree script removed
- [x] **3.10 Cookie Consent Gadget** - `perc_cookie_consent_gadget.xml` dynatree script removed

### Phase 4 Completion Status

- [x] **4.1 Checkbox Tree Component** - `checkboxTree.js` verified as migrated (fancytree API used)
- [x] **4.2 Theme CSS Files** - `theme.css` CSS selectors migrated from `.dynatree-*` to `.fancytree-*`

### Phase 3 Completion Status

- [x] **3.1 Page By Status Gadget** - `perc_workflow_status_gadget.xml` dynatree import removed
- [x] **3.2 Membership Gadget** - `perc_membership_gadget.xml` dynatree import removed
- [x] **3.3 Global Variables Gadget** - `PercGlobalVariablesGadget.xml` dynatree import removed
- [x] **3.4 Google Setup Gadget** - `perc_google_setup_gadget.xml` dynatree import removed
- [x] **3.5 Form Tracker Gadget** - `PercFormTrackerGadget.xml` dynatree import removed
- [x] **3.6 License Monitor Gadget** - `perc_license_monitor_gadget.xml` dynatree import removed
- [x] **3.7 Effectiveness Gadget** - `perc_effectiveness_gadget.xml` dynatree import removed
- [x] **3.8 Blogs Gadget** - `PercBlogsGadget.xml` dynatree import removed
- [x] **3.9 Comments Gadget** - `perc_comments_gadget.xml` dynatree import removed
- [x] **3.10 Cookie Consent Gadget** - `perc_cookie_consent_gadget.xml` dynatree import removed

## Key API Differences

### Initialization

```javascript
// Dynatree (OLD)
$("#tree").dynatree({
    onActivate: function(node) { },
    onSelect: function(select, node) { }
});

// Fancytree (NEW)
$("#tree").fancytree({
    activate: function(event, data) {
        var node = data.node;
    },
    select: function(event, data) {
        var node = data.node;
    }
});
```

### Node Access

```javascript
// Dynatree (OLD)
var tree = $("#tree").dynatree("getTree");
var node = $("#tree").dynatree("getActiveNode");

// Fancytree (NEW)
var tree = $("#tree").fancytree("getTree");
var node = tree.getActiveNode();
```

### CSS Classes

```css
/* Dynatree (OLD) */
.dynatree-container { }
.dynatree-node { }
.dynatree-expander { }

/* Fancytree (NEW) */
.fancytree-container { }
.fancytree-node { }
.fancytree-expander { }
```

## Implementation Phases

### Phase 1: CUI Core Components ✅ COMPLETED

**Priority: High**
**Estimated Files: 6**

These are foundational components used by other parts of the system.

#### ✅ 1.1 Update RequireJS Configuration

- **File**: `cui/pages/_bootstrap.js`
- **Changes**:
  - Remove dynatree from paths ✅
  - Update shim configuration ✅
  - Verify fancytree paths are correct ✅

#### ✅ 1.2 Migrate Page Wizard

- **Files**:
  - `cui/widgets/pagewizard/pagewizard.js`
  - `cui/widgets/pagewizard/pagewizard.viewmodel.js`
  - `cui/widgets/pagewizard/pagewizard.css`
- **Changes**:
  - Replace dynatree CSS import with fancytree CSS ✅
  - Update JavaScript API calls ✅
  - Update CSS class names ✅

#### ✅ 1.3 Migrate Asset Wizard

- **Files**:
  - `cui/widgets/assetwizard/assetwizard.viewmodel.js`
- **Changes**:
  - Update tree initialization code ✅
  - Update event handlers ✅

### Phase 2: System Package Widgets

**Priority: High**
**Estimated Files: 30+**

These widgets are used in content creation and management.

#### ✅ 2.1 Tag List Widget

- **Files**:
  - `system/Packages/perc.widget.taglist/sys__UserDependency--rx_resources/stylesheets/controls/tagListWidgetControl.xsl`
  - `system/Packages/perc.widget.taglist/sys__UserDependency--rx_resources/widgets/tagList/css/jquery.tagList.css`
  - `system/Packages/perc.widget.taglist/sys__UserDependency--rx_resources/widgets/tagList/js/jquery.tagList.js`
- **Changes**:
  - Uses fancytree CSS (`ui.fancytree.css`)
  - Uses fancytree JS (`jquery.fancytree-all.min.js`)
  - Uses PercFinderTree which is fancytree-based
  - CSS uses fancytree class selectors (`.fancytree-container`, etc.)

#### ✅ 2.2 Category List Widget

- **Files**:
  - `system/Packages/perc.widget.categoryList/sys__UserDependency--web_resources/widgets/category/js/category.js`
  - `system/Packages/perc.widget.categoryList/sys__UserDependency--rx_resources/stylesheets/controls/categoryListWidgetControl.xsl`
- **Changes**:
  - JavaScript uses `$(this).fancytree({...})` directly
  - XSL references fancytree CSS and JS libraries

#### ✅ 2.3 File Auto List Widget

- **Files**:
  - `system/Packages/perc.widget.fileAutoList/sys__UserDependency--rx_resources/stylesheets/controls/resourceAutoListWidgetControl.xsl`
  - `system/Packages/perc.widget.fileAutoList/sys__UserDependency--rx_resources/controls/percResourceAutoListControl/css/jquery.resourceAutoList.css`
- **Changes**:
  - Updated XSL to reference fancytree CSS (`/cm/css/ui.fancytree.css`) and JS (`jquery.fancytree-all.min.js`) ✅
  - Updated CSS selectors from `.dynatree-*` to `.fancytree-*` ✅

#### ✅ 2.4 Image Auto List Widget

- **Files**:
  - `system/Packages/perc.widget.ImageAutoListWidget/sys__UserDependency--rx_resources/stylesheets/controls/imageAutoListWidgetControl.xsl`
  - `system/Packages/perc.widget.ImageAutoListWidget/sys__UserDependency--rx_resources/widgets/imageAutoList/css/jquery.imageAutoList.css`
- **Changes**:
  - Updated XSL to reference fancytree CSS and JS ✅
  - Updated CSS selectors from `.dynatree-*` to `.fancytree-*` ✅

#### ✅ 2.5 Page Auto List Widget

- **Files**:
  - `system/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/css/jquery.pageAutoList.css`
  - `system/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/css/jquery.pageAutoList.css`
- **Changes**:
  - Updated CSS selectors from `.dynatree-*` to `.fancytree-*` ✅

#### ✅ 2.6 Calendar Widget

- **Files**:
  - `system/Packages/perc.widget.calendar/sys__UserDependency--rx_resources/stylesheets/controls/percCalendarWidgetControl.xsl`
  - `system/Packages/perc.widget.calendar/SupportFile-rx_resources/stylesheets/controls/percCalendarWidgetControl.xsl`
- **Changes**:
  - Updated XSL to reference fancytree CSS (`/cm/css/ui.fancytree.css`) instead of dynatree ✅

### Phase 3: Gadgets ✅ COMPLETED

**Priority: Medium**
**Estimated Files: 20+**

Gadgets provide dashboard functionality and can be migrated incrementally.

#### ✅ 3.1 Page By Status Gadget

- **Files**:
  - `system/Packages/perc.gadget.pageByStatus/sys__UserDependency--cm/gadgets/repository/perc_workflow_status_gadget/perc_workflow_status_gadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.2 Membership Gadget

- **Files**:
  - `system/Packages/perc.gadget.membership/sys__UserDependency--cm/gadgets/repository/perc_membership_gadget/perc_membership_gadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.3 Global Variables Gadget

- **Files**:
  - `system/Packages/perc.gadget.globalVariables/sys__UserDependency--cm/gadgets/repository/PercGlobalVariablesGadget/PercGlobalVariablesGadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.4 Google Setup Gadget

- **Files**:
  - `system/Packages/perc.gadget.googleSetup/sys__UserDependency--cm/gadgets/repository/perc_google_setup_gadget/perc_google_setup_gadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.5 Form Tracker Gadget

- **Files**:
  - `system/Packages/perc.gadget.formTracker/sys__UserDependency--cm/gadgets/repository/PercFormTrackerGadget/PercFormTrackerGadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.6 License Monitor Gadget

- **Files**:
  - `system/Packages/perc.gadget.licenseMonitor/sys__UserDependency--cm/gadgets/repository/perc_license_monitor_gadget/perc_license_monitor_gadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.7 Effectiveness Gadget

- **Files**:
  - `system/Packages/perc.gadget.effectiveness/sys__UserDependency--cm/gadgets/repository/perc_effectiveness_gadget/perc_effectiveness_gadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.8 Blogs Gadget

- **Files**:
  - `system/Packages/perc.gadget.blogs/sys__UserDependency--cm/gadgets/repository/PercBlogsGadget/PercBlogsGadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.9 Comments Gadget

- **Files**:
  - `system/Packages/perc.gadget.comments/sys__UserDependency--cm/gadgets/repository/perc_comments_gadget/perc_comments_gadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

#### ✅ 3.10 Cookie Consent Gadget

- **Files**:
  - `system/Packages/perc.gadget.cookieconsent/sys__UserDependency--cm/gadgets/repository/perc_cookie_consent_gadget/perc_cookie_consent_gadget.xml`
- **Changes**:
  - Removed dynatree script import ✅

### Phase 4: Core System Files ✅ COMPLETED

**Priority: High**
**Estimated Files: 10+**

These are shared components used across the system.

#### ✅ 4.1 Checkbox Tree Component

- **File**: `system/cms/content/applications/sys_resources/ApplicationFiles/js/checkboxTree.js`
- **Changes**:
  - Complete migration from dynatree to fancytree API
  - Updated event handlers (select, click, focus, keydown)
  - Updated CSS class selectors (`.dynatree-*` → `.fancytree-*`)
  - Updated node property access (`node.data.key` → `node.key`, etc.)
  - Updated cookie option format
  - Updated aria-related functions for fancytree

#### ✅ 4.2 Theme CSS Files

- **Files**:
  - `system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css`
- **Changes**:
  - Replaced dynatree CSS class selectors with fancytree equivalents ✅
  - Updated `.dynatree-*` selectors to `.fancytree-*` in Category List Widget sections

### Phase 5: Plugin Files ✅ COMPLETED

**Priority: Medium**
**Estimated Files: 5**

These are the source plugin files in the WebUI war directory.

#### ✅ 5.1 PercDataTree Plugin

- **File**: `WebUI/war/plugins/PercDataTree.js`
- **Changes**:
  - Already migrated to fancytree API; only comment references to dynatree remain ✅

#### ✅ 5.2 Copy Site Dialog

- **File**: `WebUI/war/plugins/PercCopySiteDialog.js`
- **Changes**:
  - No dynatree references found; no changes needed ✅

#### ✅ 5.3 Workflow Sites Folder Dialog

- **Files**:
  - `WebUI/war/plugins/perc_assign_workflow_sites_folder_dialog.js`
  - `WebUI/war/css/percWorkflow.css`
- **Changes**:
  - Updated CSS class selectors: `dynatree-custom-checkbox-*` → `fancytree-custom-checkbox-*` ✅
  - Updated `dynatree-partsel` → `fancytree-partsel` ✅
  - Updated `.dynatree-checkbox` → `.fancytree-checkbox` ✅
  - Updated `.dynatree-selected` → `.fancytree-selected` ✅
  - Updated `dynatree-container`, `dynatree-expander`, `dynatree-title`, `dynatree-has-children`, `dynatree-expanded` in `percWorkflow.css` ✅

### Phase 6: Cleanup ✅ COMPLETED

**Priority: Low (after all migrations complete)**

#### ✅ 6.1 Remove Dynatree Library

- **Removed**:
  - `cui/components/dynatree/` directory and all files (6 files deleted) ✅

#### ✅ 6.2 Update Bundle Configurations

- **File**: `WebUI/src/main/resources/minify/common-bundles.json`
- **Changes**:
  - Removed `jslib/profiles/3x/jquery/plugins/jquery-dynatree/jquery.dynatree.js` from shared-finder.js bundle ✅

#### ✅ 6.3 Verify and Remove All Remaining References

- Performed codebase scan and migrated all remaining functional dynatree references:
  - `WebUI/war/app/includes/finder_js.jsp` — removed dynatree script import ✅
  - `WebUI/war/app/siteArchitecture.jsp` — replaced dynatree CSS with fancytree CSS ✅
  - `WebUI/war/css/styles.css` — updated `.dynatree-*` CSS selectors to `.fancytree-*` ✅
  - `WebUI/war/views/PercWorkflowView.js` — updated dynatree-expander class and loading.gif path ✅
  - `WebUI/war/views/PercCategoryView.js` — **full API migration**: `dynatree()` → `fancytree()`, callbacks updated (`onPostInit`→`init`, `onActivate`→`activate`, `onQueryActivate`→`beforeActivate`), DND events updated to fancytree dnd5 extension ✅
  - `WebUI/war/widgets/PercFinderTree/PercFinderTree.css` — all `.dynatree-*` selectors and loading.gif URL updated ✅
  - `WebUI/war/widgets/PercFinderTree.js` — all `.dynatree-*` class references updated ✅
  - `system/cms/content/applications/sys_resources/ApplicationFiles/css/checkboxTree/tree.css` — all `.dynatree-*` selectors updated ✅
  - `system/cms/content/applications/sys_resources/ApplicationFiles/css/cmlite.css` — `.dynatree-*` selectors updated ✅
  - `system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl` — dynatree FileDescriptor entries replaced with fancytree equivalents ✅
  - `system/Packages/perc.Baseline/SupportFile-rx_resources/stylesheets/controls/percQueryControl.xsl` — dynatree CSS and JS FileLocation entries replaced with fancytree ✅
  - `system/Packages/perc.Baseline/SupportFile-rx_resources/controls/percQueryControl/css/percQueryControl.css` — `.dynatree-*` selectors updated ✅
  - `system/Packages/perc.gadget.bulkFileUpload/sys__UserDependency--cm/gadgets/repository/perc_bulk_file_upload_gadget/css/perc_bulk_file_upload.css` — `.dynatree-*` selectors updated ✅
  - `system/Packages/perc.widget.categoryList/SupportFile-rx_resources/widgets/categoryList/css/jquery.categoryList.css` — `.dynatree-*` selectors updated ✅
  - `system/Packages/perc.widget.categoryList/sys__UserDependency--rx_resources/widgets/categoryList/css/jquery.categoryList.css` — `.dynatree-*` selectors updated ✅
  - `system/Packages/perc.widget.categoryList/sys__UserDependency--rxconfig/Resources/percCategoryList.xml` — dynatree CSS/JS file references replaced with fancytree ✅
  - `system/Packages/perc.widget.categoryList/sys__UserDependency--web_resources/widgets/category/js/category.min.js` — `$(this).dynatree()` replaced with `$(this).fancytree()` ✅
  - `delivery/common/js/views/PercCategoryListView.js` — `.dynatree-*` references updated ✅
  - `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` — dynatree copy entries removed ✅
- Only comment/documentation references to dynatree remain (acceptable) in:
  - `WebUI/war/plugins/PercDataTree.js`, `PercSectionTreeDialog.js`, `perc_assign_workflow_sites_folder_dialog.js`, `PercFinderTree.js`, `checkboxTree.js`, `installDistributionFiles.xml`
- The legacy `WebUI/war/css/dynatree/` directory remains as a static asset (the actual CSS library file - not loaded by any code after migration)

## Testing Strategy

### Unit Testing

- Test individual widget tree functionality
- Verify checkbox selection behavior
- Test expand/collapse functionality
- Verify lazy loading works correctly

### Integration Testing

- Test CUI wizard workflows
- Verify gadget dashboard functionality
- Test content creation with tree-based widgets
- Verify theme styling is correct

### Regression Testing

- Test all tree-related user workflows
- Verify keyboard navigation
- Test accessibility features
- Cross-browser testing (Chrome, Firefox, Edge)

## Common Migration Patterns

### Pattern 1: Simple Tree Initialization

```javascript
// Before
$("#tree").dynatree({
    checkbox: true,
    selectMode: 3,
    onSelect: function(select, node) {
        // handle selection
    }
});

// After
$("#tree").fancytree({
    checkbox: true,
    selectMode: 3,
    select: function(event, data) {
        var node = data.node;
        // handle selection
    }
});
```

### Pattern 2: Programmatic Node Selection

```javascript
// Before
var node = $("#tree").dynatree("getTree").getNodeByKey("key1");
node.select(true);

// After
var tree = $("#tree").fancytree("getTree");
var node = tree.getNodeByKey("key1");
node.setSelected(true);
```

### Pattern 3: CSS Class Updates

```css
/* Before */
.dynatree-container { border: 1px solid #ccc; }
.dynatree-node { padding: 2px; }

/* After */
.fancytree-container { border: 1px solid #ccc; }
.fancytree-node { padding: 2px; }
```

## Risk Assessment

|              Risk               | Impact |               Mitigation                |
|---------------------------------|--------|-----------------------------------------|
| Breaking existing functionality | High   | Thorough testing, migrate incrementally |
| CSS styling issues              | Medium | Maintain parallel CSS during transition |
| Third-party widget dependencies | Medium | Identify and update all dependencies    |
| Performance regression          | Low    | Benchmark before/after                  |

## Success Criteria

1. Zero references to "dynatree" in codebase (excluding documentation)
2. All tree-based widgets function correctly
3. No console errors related to tree functionality
4. Visual appearance matches or exceeds previous implementation
5. All existing tests pass

## Resources

- Fancytree Documentation: https://github.com/mar10/fancytree
- Migration Guide: https://github.com/mar10/fancytree/wiki/MigrateFromDynatree
- Existing Reference Implementation: `WebUI/war/widgets/PercFinderTree.js`

