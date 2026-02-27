# Dynatree → Fancytree Migration Plan

## Executive Summary

**Scope:** Migrate 16 JavaScript/JSP files from Dynatree v1.1.0 to Fancytree v2.38.3

**Status:** Planning phase (migration guide ready)

**Key Points:**
- Fancytree is a modern, maintained successor to Dynatree
- Both have very similar APIs with only minor differences
- Fancytree is already vendored in jslib/ and npm-managed (Phase 1)
- Migration is low-risk due to API similarity

---

## Dynatree → Fancytree API Mapping

### 1. Plugin Initialization

**Dynatree:**
```javascript
$("#perc-finder-tree").dynatree({
    selectMode: 1,
    autoFocus: false,
    children: rootChildren,
    clickFolderMode: 3,
    onLazyRead: function(dtnode) { ... },
    onClick: function(dtnode) { ... }
});
```

**Fancytree:**
```javascript
$("#perc-finder-tree").fancytree({
    selectMode: 1,
    autoFocus: false,
    source: rootChildren,
    clickFolderMode: 3,
    lazyLoad: function(event, data) { ... },
    click: function(event, data) { ... }
});
```

**Key Differences:**
| Property | Dynatree | Fancytree | Notes |
|----------|----------|-----------|-------|
| Plugin name | `.dynatree()` | `.fancytree()` | Function call difference |
| Children source | `children:` | `source:` | Renamed property |
| Lazy load callback | `onLazyRead:` | `lazyLoad:` | Renamed, signature slightly different |
| Click callback | `onClick:` | `click:` | Renamed, receives (event, data) |
| Expand callback | `onExpand:` | `expand:` | Renamed |
| Render callback | `onRender:` | `renderNode:` | Renamed |
| Query select | `onQuerySelect:` | `selectMode + custom logic` | May need adjustment |
| Query activate | `onQueryActivate:` | `activate:` or custom logic | May need adjustment |

### 2. Getting the Tree Instance

**Dynatree:**
```javascript
$("#perc-finder-tree").dynatree("getTree")
$("#perc-finder-tree").dynatree("getRoot")
$("#perc-finder-tree").dynatree("getSelectedNodes")
```

**Fancytree:**
```javascript
$("#perc-finder-tree").fancytree("getTree")
$("#perc-finder-tree").fancytree("getRoot")
$("#perc-finder-tree").fancytree("getSelectedNodes")
// or access directly:
var tree = $.ui.fancytree.getTree("#perc-finder-tree");
```

### 3. Node Methods

**Dynatree Node Methods:**
```javascript
dtnode.expand(true/false)
dtnode.addChild(dtobj)
dtnode.remove()
dtnode.activateSilently()
dtnode.visit(function(node) { ... })
dtnode.render()
dtnode.setLazyNodeStatus(DTNodeStatus_Ok|Error)
```

**Fancytree Node Methods (SAME NAMES - Good news!):**
```javascript
node.setExpanded(true/false)    // (rename: expand → setExpanded)
node.addChild(dtobj)            // (SAME)
node.remove()                   // (SAME)
node.setActive(silent=true)     // (rename: activateSilently → setActive)
node.visit(function(node) { ... })  // (SAME)
node.render()                   // (SAME)
// No lazy load status method needed - Fancytree handles automatically
```

**Summary of Node Method Changes:**
| Dynatree | Fancytree | Type |
|----------|-----------|------|
| `dtnode.expand(flag)` | `node.setExpanded(flag)` | Rename |
| `dtnode.activateSilently()` | `node.setActive(true)` | Rename & API change |
| `dtnode.render()` | `node.render()` | SAME |
| `dtnode.addChild(obj)` | `node.addChild(obj)` | SAME |
| `dtnode.remove()` | `node.remove()` | SAME |
| `dtnode.visit(fn)` | `node.visit(fn)` | SAME |
| `dtnode.setLazyNodeStatus(status)` | (Not needed) | Removed |
| `dtnode.data` | `node.data` | SAME |
| `dtnode.span` | `node.span` | SAME |

### 4. Node Data Structure

**Both use same data format:**
```javascript
{
    title: "Node Title",
    isFolder: true,
    isLazy: true,
    icon: "path/to/icon.png",
    key: "unique-id",
    data: { /* custom data */ }
}
```

**Notes:**
- Dynatree: Property is `isFolder`
- Fancytree: Also supports `isFolder` (backward compatible!)
- Both support custom data in `.data` property

### 5. Event Handlers

**Pattern Change:**

Dynatree callbacks receive just the node:
```javascript
onLazyRead: function(dtnode) { ... }
onClick: function(dtnode) { ... }
```

Fancytree callbacks receive event object first:
```javascript
lazyLoad: function(event, data) {
    var node = data.node;
    // ... use node
}
click: function(event, data) {
    var node = data.node;
    // ... use node
}
```

**Key Events & Mappings:**

| Dynatree | Fancytree | Signature Change |
|----------|-----------|------------------|
| `onLazyRead` | `lazyLoad` | `onLazyRead(dtnode)` → `lazyLoad(event, data)` where `data.node` is the node |
| `onClick` | `click` | `onClick(dtnode)` → `click(event, data)` where `data.node` is the node |
| `onExpand` | `expand` | `onExpand(flag, dtnode)` → `expand(event, data)` where `data.flag` is expand/collapse |
| `onRender` | `renderNode` | `onRender(dtnode)` → `renderNode(event, data)` where `data.node` is the node |

### 6. CSS Classes & Styling

**Key Differences:**
- Dynatree uses classes like: `dynatree-container`, `dynatree-nodes`, `dynatree-node`
- Fancytree uses classes like: `fancytree-container`, `fancytree-nodes`, `fancytree-node`

**Class Mapping:**
```
dynatree- → fancytree-
```

All existing CSS changes from `dynatree-*` to `fancytree-*` in span styling.

### 7. Icon Handling

**Dynatree:** `icon: "/path/to/icon.gif"`

**Fancytree:** Same format supported, but also:
```javascript
icon: "material-folder"  // Uses icon fonts/classes
// or
registerExtension({ ... })  // Custom icon handler
```

Current usage (explicit icon paths) works identically.

### 8. Tree Mode Settings

**Dynatree:**
```javascript
selectMode: 1,      // Single select
selectMode: 2,      // Multi-select (checkbox)
selectMode: 3,      // Multi-select (highlight)
clickFolderMode: 3  // Click behavior for folders
```

**Fancytree:** (SAME - fully compatible)
```javascript
selectMode: 1,      // Single select
selectMode: 2,      // Multi-select (checkbox)
selectMode: 3,      // Multi-select (highlight)
clickFolderMode: 3  // Click behavior for folders
```

---

## File Refactoring Checklist

### Tier 1: Core Widget (highest priority)
- [ ] **PercFinderTree.js** (443 lines)
  - Primary tree widget used by all pages
  - Updates: `.dynatree()` → `.fancytree()`, callback renames, event handler params
  - Impact: All 12 JSP pages depend on this

### Tier 2: Data Trees & Dialogs
- [ ] **PercDataTree.js** (407 lines)
  - Workflow tree with checkboxes
  - Updates: Similar callback changes, node operations

- [ ] **PercSectionTreeDialog.js**
  - Updates: Event handler changes

- [ ] **PercCopySiteDialog.js**
  - Updates: Event handler changes

- [ ] **perc_assign_workflow_sites_folder_dialog.js**
  - Updates: Event handler changes

### Tier 3: Views & Components
- [ ] **PercFinderTree.js** (Core widget - do first!)
- [ ] **PercCategoryView.js**
  - DynatingTree initialization in view

- [ ] **PercWorkflowView.js**
  - DynatingTree initialization in view

### Tier 4: Page Wizards
- [ ] **assetwizard.viewmodel.js**
  - Updates: Tree initialization in module

- [ ] **pagewizard.js**
  - Updates: Tree initialization in module

- [ ] **pagewizard.viewmodel.js**
  - Updates: Tree initialization in module

### Tier 5: Bootstrap & JSP
- [ ] **cui/pages/_bootstrap.js**
  - Register any dynatree-specific code

- [ ] **siteArchitecture.jsp**
  - May have dynatree-specific includes

### Tier 6: Delivery Tier (External Module)
- [ ] **modules/perc-common-ui-bundle/src/main/js/views/PercCategoryListView.js**
  - Separate module build

- [ ] **system/cms/content/applications/** (2 files)
  - Widget files, may be dynamically loaded

- [ ] **system/Packages/perc.widget.categoryList/** (2 files)
  - Plugin package files

---

## Migration Steps (Ordered)

### Step 1: Update PercFinderTree.js (Core)

**Changes needed:**
```javascript
// OLD: $("#perc-finder-tree").dynatree({
// NEW:
$("#perc-finder-tree").fancytree({
    source: rootChildren,              // (was: children)
    lazyLoad: function(event, data) {  // (was: onLazyRead:)
        self._loadChildren(data.node);
    },
    click: function(event, data) {     // (was: onClick:)
        self._onClick(data.node);
        self.getFolderID(data.node);
    },
    expand: function(event, data) {    // (was: onExpand:)
        self._onExpand(data.flag, data.node);
    },
    renderNode: function(event, data) { // (was: onRender:)
        // Use data.node here
    }
});

// OLD: dtnode.expand(true)
// NEW:
node.setExpanded(true)

// OLD: dtnode.activateSilently()
// NEW:
node.setActive(true)

// Node creation: Should work as-is!
```

### Step 2: Update Node Operations Across All Files

Find/Replace patterns:

1. **Plugin method:**
   - Find: `\.dynatree\(`
   - Replace: `.fancytree(`

2. **Children → source:**
   - Find: `children:\s*rootChildren`
   - Replace: `source: rootChildren`

3. **Callback renames:**
   - Find: `onLazyRead:`
   - Replace: `lazyLoad:`

   - Find: `onClick:`
   - Replace: `click:`

   - Find: `onExpand:`
   - Replace: `expand:`

   - Find: `onRender:`
   - Replace: `renderNode:`

4. **Node methods:**
   - Find: `\.expand\(`
   - Replace: `.setExpanded(`

   - Find: `\.activateSilently\(\)`
   - Replace: `.setActive(true)`

5. **Event handler params:**
   - Find: `function(dtnode)` in callbacks
   - Replace: `function(event, data)` and use `data.node`

### Step 3: Update bundle configurations

**common-bundles.json** and **common-minuet-bundles.json**:
- Remove: `jslib/profiles/3x/jquery/plugins/jquery-dynatree/jquery.dynatree.js`
- Keep: `jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.js` (already there or add)

### Step 4: CSS Updates

**Find all `dynatree` CSS class references:**
- In inline styles: `span.css("padding-left", ...)`
- In stylesheets: `.dynatree-*` selectors
- Replace with: `fancytree-*` equivalents

---

## Known Issues & Gotchas

### 1. DTNodeStatus Constants

**Dynatree used constants:**
```javascript
DTNodeStatus_Ok
DTNodeStatus_Error
```

**Not needed in Fancytree** — lazy load is handled automatically.

**Fix:** Remove all `setLazyNodeStatus()` calls.

### 2. imagePath Property

**Dynatree:**
```javascript
imagePath: " "  // Empty string or path to icon folder
```

**Fancytree:** Not needed if using custom icons. Can be removed.

### 3. addClass/removeClass properties

**Dynatree supported:**
```javascript
{
    title: "Node",
    addClass: "perc-hide-node-expander"
}
```

**Fancytree:** Also supported! No changes needed.

### 4. Event Handler Parameters

**Most critical change:**
- Dynatree: `function(dtnode) { dtnode.data }`
- Fancytree: `function(event, data) { data.node.data }`

All callbacks must be updated to accept `(event, data)` and extract `data.node`.

### 5. Lazy Load Node Status

**Dynatree:**
```javascript
dtnode.setLazyNodeStatus(DTNodeStatus_Ok);
```

**Fancytree:** Not needed! Handled internally. Remove these lines.

---

## Quick Reference: Before & After

### Before (Dynatree)
```javascript
$("#tree").dynatree({
    selectMode: 1,
    children: [
        {title: "Folder", isFolder: true, isLazy: true, icon: "icon.gif"}
    ],
    onLazyRead: function(dtnode) {
        $.ajax({...}).done(function(data) {
            dtnode.addChild({title: data.name, ...});
            dtnode.setLazyNodeStatus(DTNodeStatus_Ok);
        });
    },
    onClick: function(dtnode) {
        alert(dtnode.data.pathItem.name);
    },
    onRender: function(dtnode) {
        $(dtnode.span).css("padding-left", "10px");
    }
});

var selected = $("#tree").dynatree("getSelectedNodes");
selected[0].expand(true);
```

### After (Fancytree)
```javascript
$("#tree").fancytree({
    selectMode: 1,
    source: [
        {title: "Folder", isFolder: true, isLazy: true, icon: "icon.gif"}
    ],
    lazyLoad: function(event, data) {
        $.ajax({...}).done(function(result) {
            data.node.addChild({title: result.name, ...});
            // No status needed - handled automatically!
        });
    },
    click: function(event, data) {
        alert(data.node.data.pathItem.name);
    },
    renderNode: function(event, data) {
        $(data.node.span).css("padding-left", "10px");
    }
});

var selected = $("#tree").fancytree("getSelectedNodes");
selected[0].setExpanded(true);
```

---

## Testing Checklist Per File

- [ ] Tree renders correctly (all nodes visible)
- [ ] Lazy loading works (nodes expand on click)
- [ ] Pagination works ("Show More" functionality)
- [ ] Click handlers fire correctly
- [ ] Selection/checkboxes work
- [ ] Keyboard navigation works (arrow keys)
- [ ] Icons display correctly
- [ ] CSS styling preserved (padding, colors)
- [ ] Filtering/searching works (if applicable)
- [ ] Drag-and-drop works (if using dnd extension)

---

## Tools & Scripts

### Regex Patterns for Find/Replace

1. **Replace plugin calls:**
   ```
   Find: \.dynatree\(
   Replace: .fancytree(
   ```

2. **Replace children → source:**
   ```
   Find: children:\s*
   Replace: source:
   ```

3. **Replace callback names (automated):**
   Scripts can be run in VS Code find/replace to batch update files.

---

## Risk Assessment

**Low Risk:**
- API structure is very similar
- Callback changes are consistent across all files
- Node methods are mostly the same
- Property names are same (especially for data objects)

**Medium Risk:**
- Event handler signature change (must handle `event, data` properly)
- Need to remove `setLazyNodeStatus` calls that may have error handling

**Mitigation:**
- Test each file after changes
- Use browser DevTools to verify tree rendering
- Create unit tests for tree functionality

---

## Success Criteria

✅ All 16 source files updated and compiling
✅ No JavaScript errors in browser console
✅ All 12 JSP pages load without 404 errors
✅ Tree widgets render and respond to user interaction
✅ All tree features functional (lazy load, selection, pagination)
✅ Bundles rebuild with Fancytree instead of Dynatree
✅ No regression in other features

---

## Next Steps

1. ✅ Create this migration guide (DONE)
2. → Start Step 1: Refactor PercFinderTree.js
3. → Update event handler signatures across all files
4. → Test PercFinderTree in a page load
5. → Cascade changes to dependent files
6. → Update bundle configs
7. → Full build and test
8. → Deploy and verify all pages

