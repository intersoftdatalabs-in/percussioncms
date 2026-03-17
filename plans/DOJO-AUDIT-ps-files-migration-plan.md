# Dojo 0.4.x Dependency Audit — `ps/` JavaScript Files

**Date:** 2025-02-27
**Scope:** `system/cms/content/applications/sys_resources/ApplicationFiles/ps/`
**Context:** Track A0 pruned unused Dojo files. Track A1 rewrote `ps/io/Actions.js` from `dojo.io.bind()` to `$.ajax()`. This audit plans Track A2 and beyond.

---

## Executive Summary

|                        Metric                        |                                         Value                                          |
|------------------------------------------------------|----------------------------------------------------------------------------------------|
| Total `.js` files                                    | **42**                                                                                 |
| Total lines of code                                  | **14,696**                                                                             |
| Total lines referencing `dojo.*`                     | **974**                                                                                |
| Unique `dojo.*` API symbols used                     | **~95**                                                                                |
| Files with `dojo.widget.defineWidget` (Hard)         | **12**                                                                                 |
| Files using `dojo.dnd.*` (Hard)                      | **3**                                                                                  |
| Files using `dojo.event.connect` (Medium)            | **20**                                                                                 |
| Files using `dojo.html.*` / `dojo.dom.*` (Medium)    | **14**                                                                                 |
| Files with only `dojo.provide`/`dojo.require` (Easy) | **5**                                                                                  |
| Already migrated (Track A1)                          | **1** (`ps/io/Actions.js` — partial, still has residual `dojo.provide`/`dojo.require`) |

---

## Global Dojo API Usage (Top 30)

| Count |                 API                 |                   jQuery Replacement                   |
|------:|-------------------------------------|--------------------------------------------------------|
|   159 | `dojo.lang.assert`                  | Plain `console.assert()` or custom `ps.assert()`       |
|   137 | `dojo.require`                      | Remove (use `<script>` load order or ES modules)       |
|    78 | `dojo.widget.createWidget`          | jQuery widget factory / manual DOM construction        |
|    70 | `dojo.lang.assertType`              | Custom type-check helper or remove                     |
|    53 | `dojo.event.connect`                | `$(el).on()` / `.click()` / `.change()`                |
|    46 | `dojo.widget.byId`                  | `$('#id')` or custom widget registry                   |
|    40 | `dojo.provide`                      | Remove (namespace declaration — use `window.ps = ...`) |
|    39 | `dojo.debug`                        | `console.log()` / `console.debug()`                    |
|    36 | `dojo.html.hide`                    | `$(el).hide()`                                         |
|    33 | `dojo.byId`                         | `document.getElementById()` or `$('#id')`              |
|    32 | `dojo.html.show`                    | `$(el).show()`                                         |
|    25 | `dojo.collections.ArrayList`        | Plain `Array`                                          |
|    15 | `dojo.html.setStyle`                | `$(el).css()`                                          |
|    13 | `dojo.lang.forEach`                 | `Array.prototype.forEach()`                            |
|    12 | `dojo.widget.defineWidget`          | jQuery UI widget factory or plain class                |
|    11 | `dojo.lang.has`                     | `'prop' in obj` or `obj.hasOwnProperty()`              |
|     9 | `dojo.json` (require)               | Native `JSON`                                          |
|     8 | `dojo.lang.setTimeout`              | `setTimeout()`                                         |
|     8 | `dojo.html.getAttribute`            | `el.getAttribute()` or `$(el).attr()`                  |
|     8 | `dojo.collections.Dictionary`       | Plain `Object` or `Map`                                |
|     7 | `dojo.widget.manager.getWidgetById` | Custom widget registry                                 |
|     7 | `dojo.html.getElementsByClass`      | `$(selector)` or `document.querySelectorAll()`         |
|     7 | `dojo.html.classMatchType.IsOnly`   | CSS selector specificity                               |
|     7 | `dojo.dnd.HtmlDropTarget`           | jQuery UI Droppable                                    |
|     6 | `dojo.event.connectAround`          | Custom AOP wrapper or jQuery plugin                    |
|     6 | `dojo.lang.declare`                 | Plain prototype / class syntax                         |
|     5 | `dojo.declare`                      | Plain prototype / class syntax                         |
|     4 | `dojo.kwCompoundRequire`            | Remove                                                 |
|     4 | `dojo.dnd.dragManager`              | jQuery UI Draggable internals                          |
|     4 | `dojo.string.trim`                  | `String.prototype.trim()`                              |

---

## File-by-File Breakdown

### EASY — Only `dojo.provide`/`dojo.require`/`dojo.kwCompoundRequire` (5 files)

These files only declare module membership. Migration = delete the dojo lines or replace with namespace stubs.

|            File             | Lines |                    Dojo APIs                     |                          Purpose                          |
|-----------------------------|------:|--------------------------------------------------|-----------------------------------------------------------|
| `ps/aa/__package__.js`      |    12 | `dojo.provide` (1), `dojo.kwCompoundRequire` (1) | Package declaration for `ps.aa` module                    |
| `ps/content/__package__.js` |     8 | `dojo.provide` (1), `dojo.kwCompoundRequire` (1) | Package declaration for `ps.content` module               |
| `ps/io/__package__.js`      |     4 | `dojo.provide` (1), `dojo.kwCompoundRequire` (1) | Package declaration for `ps.io` module                    |
| `ps/io/Response.js`         |    58 | `dojo.provide` (1)                               | Response object for AJAX results — already self-contained |
| `ps/widget/__package__.js`  |    19 | `dojo.provide` (1), `dojo.kwCompoundRequire` (1) | Package declaration for `ps.widget` module                |

**Effort estimate:** ~1 hour total

---

### EASY-MEDIUM — Only `dojo.lang.*` utilities (assertions, type checks) (2 files)

These use only `dojo.lang.assert`/`assertType` — trivially replaced with a small shim.

|              File              | Lines |                                                                               Dojo APIs                                                                                |                     Purpose                     |
|--------------------------------|------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| `ps/aa/SnippetMove.js`         |   172 | `dojo.lang.assert` (4), `dojo.lang.assertType` (4), `dojo.lang.isBoolean` (2), `dojo.lang.type` (1), `dojo.lang.isNumeric` (1), `dojo.provide` (1), `dojo.require` (2) | Data object for snippet move/reorder operations |
| `ps/content/History.js`        |    68 | `dojo.collections.Stack` (2), `dojo.lang.assertType` (2), `dojo.lang.assert` (1), `dojo.provide` (1), `dojo.require` (1)                                               | Browser-history stack for content browsing      |
| `ps/widget/ScrollableNodes.js` |    66 | `dojo.lang.assert` (4), `dojo.lang.forEach` (1), `dojo.provide` (1)                                                                                                    | Utility for scrollable node lists               |

**Effort estimate:** ~2 hours total

---

### MEDIUM — `dojo.event.connect`, `dojo.html.*`, `dojo.dom.*`, `dojo.declare` (14 files)

Standard jQuery replacements: `$(el).on()`, `$(el).show()`/`.hide()`, `$(el).attr()`, etc.

|                  File                   | Lines |                                                                                                                    Key Dojo APIs (count)                                                                                                                     |                                                      Purpose                                                      |
|-----------------------------------------|------:|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `ps/io/Actions.js` *(A1 done)*          | 1,127 | `dojo.provide` (2), `dojo.require` (2), `dojo.io.bind` (1 — comment only), `dojo.string.extras` (1 — comment), `dojo.json` (1 — comment)                                                                                                                     | AJAX action dispatcher — **already migrated** to `$.ajax()`. Residual `dojo.provide`/`dojo.require` stubs remain. |
| `ps/aa.js`                              |   475 | `dojo.json.evalJson` (1), `dojo.json.serialize` (1), `dojo.string.startsWith` (2), `dojo.event.connect` (1), `dojo.lang.assert` (3)                                                                                                                          | Core namespace + `ObjectId` class — needs `JSON.parse()`/`JSON.stringify()`, `String.startsWith()`                |
| `ps/UserInfo.js`                        |    60 | `dojo.require` (6), `dojo.provide` (1), `dojo.lang.assert` (1), `dojo.html` (1), `dojo.widget.Menu2` (1)                                                                                                                                                     | User info display — light widget reference                                                                        |
| `ps/aa/Page.js`                         |   141 | `dojo.html.scrollIntoView` (1), `dojo.html.getClass` (1), `dojo.html.isTag` (1), `dojo.html.getAttribute` (1), `dojo.render.html.ie` (1), `dojo.lang.assert` (3), `dojo.lang.isUndefined` (3)                                                                | Page model — AA page structure parser                                                                             |
| `ps/content/CreateItem.js`              |   256 | `dojo.widget.byId` (4), `dojo.event.connect` (1), `dojo.html.setStyle` (1), `dojo.byId` (2)                                                                                                                                                                  | Create-content item dialog                                                                                        |
| `ps/content/SelectTemplates.js`         |   272 | `dojo.widget.byId` (3), `dojo.event.connect` (1), `dojo.lang.assertType` (2)                                                                                                                                                                                 | Template selection dialog                                                                                         |
| `ps/content/FolderSitesBaseTabPanel.js` |   111 | `dojo.event.connect` (7), `dojo.event.connectBefore` (1), `dojo.lang.declare` (1), `dojo.declare` (1)                                                                                                                                                        | Base class for folder/sites tab panels                                                                            |
| `ps/content/FoldersTabPanel.js`         |    43 | `dojo.declare` (1), `dojo.lang.declare` (1), `dojo.lang.assert` (3), `dojo.widget.byId` (1)                                                                                                                                                                  | Folders tab panel (extends base)                                                                                  |
| `ps/content/SitesTabPanel.js`           |    62 | `dojo.declare` (1), `dojo.lang.declare` (1), `dojo.lang.filter` (1), `dojo.string.splitEscaped` (1), `dojo.widget.byId` (1)                                                                                                                                  | Sites tab panel (extends base)                                                                                    |
| `ps/content/SearchTabPanel.js`          |   518 | `dojo.event.connect` (7), `dojo.html.hide` (4), `dojo.html.show` (3), `dojo.dom.removeNode` (1), `dojo.dom.removeChildren` (1), `dojo.declare` (1), `dojo.byId` (4)                                                                                          | Search tab panel with result list                                                                                 |
| `ps/content/SnippetPicker.js`           |   514 | `dojo.html.setStyle` (14), `dojo.widget.byId` (6), `dojo.html.getElementsByClass` (3), `dojo.event.connect` (3), `dojo.html.hide` (2), `dojo.html.hasClass` (2)                                                                                              | Snippet picker dialog with selection UI                                                                           |
| `ps/content/Browse.js`                  |   240 | `dojo.lang.setTimeout` (3), `dojo.widget.byId` (2), `dojo.html.show` (2), `dojo.html.hide` (2), `dojo.render.html.*` (4), `dojo.lang.declare` (1), `dojo.collections.Dictionary` (1)                                                                         | Content browser initialization                                                                                    |
| `ps/DivActionHelper.js`                 |   366 | `dojo.event.topic.subscribe` (2), `dojo.render.html.ie` (4), `dojo.widget.byId` (2), `dojo.string.trim` (1), `dojo.html.toCoordinateObject` (1), `dojo.collections.ArrayList` (1)                                                                            | Div-based action menus on hover — mouse event handling                                                            |
| `ps/workflow/WorkflowActions.js`        |   698 | `dojo.event.connect` (11), `dojo.widget.byId` (9), `dojo.byId` (9), `dojo.html.show` (2), `dojo.html.hide` (1), `dojo.html.hasClass` (1), `dojo.widget.createWidget` (1), `dojo.json.evalJson` (1), `dojo.collections.ArrayList` (2), `dojo.string.trim` (2) | Workflow action dialogs (checkin, checkout, transition)                                                           |

**Effort estimate:** ~2–4 days total (varies by file complexity)

---

### MEDIUM-HARD — Heavy `dojo.event.connect`, `dojo.widget.*`, `dojo.collections.*`, `dojo.html.*` (5 files)

These have deep Dojo integration but don't define custom widgets. They use widgets heavily and need careful testing.

|              File              | Lines |                                                                                                                                                                        Key Dojo APIs (count)                                                                                                                                                                        |                                 Purpose                                 |
|--------------------------------|------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `ps/content/BrowseTabPanel.js` | 1,402 | `dojo.event.connect` (6), `dojo.event.topic.subscribe` (1), `dojo.lang.assertType` (19), `dojo.lang.assert` (17), `dojo.widget.createWidget` (2), `dojo.widget.byId` (1), `dojo.string.trim` (2), `dojo.collections.Dictionary` (2), `dojo.declare` (1), `dojo.io.cookie` (2)                                                                                       | Content browse tab — tree navigation, filtering, content type selection |
| `ps/aa/Field.js`               |   502 | `dojo.event.connect` (7), `dojo.widget.byId` (5), `dojo.html.isTag` (2), `dojo.html.getBorderBox` (2), `dojo.html.show` (1), `dojo.html.hide` (1), `dojo.html.getAttribute` (1), `dojo.html.getAbsolutePosition` (1), `dojo.widget.createWidget` (1), `dojo.event.browser.stopEvent` (1)                                                                            | AA field editing — inline edit with modal dialogs                       |
| `ps/aa/Menu.js`                | 1,425 | `dojo.widget.createWidget` (72!), `dojo.html.hide` (26), `dojo.html.show` (24), `dojo.collections.ArrayList` (3), `dojo.lang.assert` (3)                                                                                                                                                                                                                            | AA context menus — **heaviest `dojo.widget.createWidget` user**         |
| `ps/aa/Tree.js`                |   902 | `dojo.collections.ArrayList` (16), `dojo.lang.assert` (13), `dojo.lang.assertType` (11), `dojo.html.getAttribute` (1), `dojo.dom` (1)                                                                                                                                                                                                                               | AA tree model — hierarchical page/snippet structure                     |
| `ps/aa/controller.js`          | 2,474 | `dojo.lang.assert` (42), `dojo.lang.assertType` (13), `dojo.widget.byId` (9), `dojo.html.getElementsByClass` (4), `dojo.html.createNodesFromText` (3), `dojo.dom.destroyNode` (3), `dojo.event.connect` (3), `dojo.collections.ArrayList` (4), `dojo.widget.ModalFloatingPane` (4), `dojo.json.serialize` (1), `dojo.io.cookie` (2), `dojo.string.splitEscaped` (1) | **Main AA controller** — orchestrates Active Assembly, heaviest file    |
| `ps/util.js`                   |   732 | `dojo.widget.FloatingPane` (6), `dojo.widget.createWidget` (3), `dojo.widget.byId` (2), `dojo.event.connect` (2), `dojo.html.ELEMENT_NODE` (2), `dojo.html.*` (various), `dojo.lang.mixin` (2), `dojo.render.html.ie` (2), `dojo.debug` (2), `dojo.byId` (2)                                                                                                        | Shared utility functions — dialog creation, DOM helpers                 |

**Effort estimate:** ~3–5 days total

---

### HARD — `dojo.widget.defineWidget` (custom widget definitions) (12 files)

These files define Dojo 0.4.x widgets using `dojo.widget.defineWidget()`. They inherit from Dojo base widgets and use the Dojo widget lifecycle. Each needs a complete rewrite as either jQuery UI widgets, plain JS classes, or jQuery plugins.

|                File                | Lines |               Base Widget Extended                |                                   Other Dojo APIs                                    |                          Purpose                           |
|------------------------------------|------:|---------------------------------------------------|--------------------------------------------------------------------------------------|------------------------------------------------------------|
| `ps/widget/MenuBar2.js`            |    49 | `dojo.widget.MenuBar2`                            | `PopupMenu2.closeSubmenu` (2), `PopupContainerBase.close` (2)                        | Custom menu bar — overrides close behavior                 |
| `ps/widget/MenuBarItem2.js`        |    25 | `dojo.widget.MenuBarItem2`                        | (none beyond define)                                                                 | Menu bar item stub — minimal override                      |
| `ps/widget/MenuBarIcon.js`         |    45 | `dojo.widget.MenuBarItem2`                        | `dojo.dom.getFirstChildElement` (2)                                                  | Menu bar item with icon support                            |
| `ps/widget/MenuBarItemDropDown.js` |    54 | `dojo.widget.MenuBarItem2`                        | `dojo.event.connectBefore` (2), `dojo.uri.dojoUri` (1)                               | Dropdown menu bar item                                     |
| `ps/widget/PopupMenu.js`           |    61 | `dojo.widget.PopupMenu2`                          | (none beyond define)                                                                 | Custom popup menu                                          |
| `ps/widget/PSButton.js`            |    20 | `dojo.widget.Button`                              | `dojo.uri.moduleUri` (1)                                                             | Themed button widget                                       |
| `ps/widget/PSImageGallery.js`      |    54 | `dojo.widget.HtmlWidget`                          | `dojo.style.hide` (1), `dojo.lang.assertType` (3)                                    | Image gallery widget                                       |
| `ps/widget/PSSplitContainer.js`    |    66 | `dojo.widget.SplitContainer`                      | `SplitContainer` prototype overrides (3)                                             | Resizable split pane                                       |
| `ps/widget/Tree.js`                |   537 | `dojo.widget.TreeV3` + `TreeNodeV3`               | `dojo.widget.manager.getWidgetById` (6), `dojo.debug` (27), `dojo.uri.moduleUri` (1) | Custom tree widget — **complex lifecycle, 27 debug calls** |
| `ps/widget/TreeDndController.js`   |    79 | `dojo.widget.TreeDndControllerV3`                 | `dojo.event.connectAround` (1), `dojo.dnd.TreeDragAndDropV3` (1)                     | Tree drag-and-drop controller                              |
| `ps/widget/TreeIcon.js`            |    70 | `dojo.widget.TreeNodeV3` + `TreeDocIconExtension` | `dojo.uri.moduleUri` (1), `dojo.debug` (2)                                           | Tree node icon customization                               |
| `ps/widget/TreeSelector.js`        |    33 | `dojo.widget.TreeSelectorV3`                      | `dojo.event.connect` (1)                                                             | Tree node selection handler                                |
| `ps/widget/ContentPaneProgress.js` |    56 | `dojo.widget.ContentPane` (extends via connect)   | `dojo.event.connect` (2)                                                             | Loading progress for content panes                         |
| `ps/widget/Autoscroller.js`        |   254 | N/A (uses `dojo.dnd.dragManager`)                 | `dojo.dnd.dragManager` (4), `dojo.event.connect` (1), `dojo.event.connectAround` (1) | Auto-scroll during drag operations                         |

**Effort estimate:** ~5–8 days total

---

### HARD — `dojo.dnd.*` (Drag and Drop) (3 files, 2 overlap with above)

|               File               | Lines |                                                                                DnD APIs Used                                                                                 |                                  Purpose                                   |
|----------------------------------|------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `ps/aa/dnd.js`                   |   566 | `dojo.dnd.HtmlDragSource` (3), `dojo.dnd.HtmlDropTarget` (7), `dojo.dnd.dragManager` (1), `dojo.dnd.DragEvent` (1), `dojo.event.connectAround` (4), `dojo.event.connect` (3) | **Core AA drag-and-drop** — snippet reordering with custom drop indicators |
| `ps/widget/Autoscroller.js`      |   254 | `dojo.dnd.dragManager` (4+)                                                                                                                                                  | Auto-scroll during drag                                                    |
| `ps/widget/TreeDndController.js` |    79 | `dojo.dnd.TreeDragAndDropV3` (1), `dojo.widget.TreeDndControllerV3` (2)                                                                                                      | Tree DnD controller                                                        |

**Effort estimate:** ~3–5 days (requires jQuery UI Draggable/Droppable or custom implementation)

---

## Dojo API → jQuery/Vanilla Migration Reference

|                  Dojo 0.4.x API                  |                         Replacement                         |                        Notes                         |
|--------------------------------------------------|-------------------------------------------------------------|------------------------------------------------------|
| `dojo.provide("ps.foo")`                         | `window.ps = window.ps \|\| {}; ps.foo = ...`               | Or remove if using `<script>` tags                   |
| `dojo.require("ps.foo")`                         | Remove (ensure script load order)                           | Or ES module `import`                                |
| `dojo.kwCompoundRequire(...)`                    | Remove                                                      |                                                      |
| `dojo.byId(id)`                                  | `document.getElementById(id)`                               |                                                      |
| `dojo.widget.byId(id)`                           | `$('#' + id)` + custom registry                             | Need widget-specific migration                       |
| `dojo.widget.createWidget(type, props, node)`    | Manual DOM + jQuery or jQuery UI                            | **72 calls in Menu.js alone**                        |
| `dojo.widget.defineWidget(name, base, proto)`    | jQuery UI `$.widget()` or ES6 class                         | Major rewrite per widget                             |
| `dojo.event.connect(src, evt, tgt, method)`      | `$(src).on(evt, ...)`                                       | Watch: Dojo connects to method names, not DOM events |
| `dojo.event.connectBefore(...)`                  | Custom pre-hook wrapper                                     |                                                      |
| `dojo.event.connectAround(...)`                  | Custom AOP wrapper                                          |                                                      |
| `dojo.event.topic.subscribe(topic, ...)`         | `$(document).on(topic, ...)` or custom pubsub               |                                                      |
| `dojo.event.browser.stopEvent(e)`                | `e.preventDefault(); e.stopPropagation()`                   |                                                      |
| `dojo.html.show(node)`                           | `$(node).show()`                                            |                                                      |
| `dojo.html.hide(node)`                           | `$(node).hide()`                                            |                                                      |
| `dojo.html.setStyle(node, prop, val)`            | `$(node).css(prop, val)`                                    |                                                      |
| `dojo.html.getAttribute(node, attr)`             | `$(node).attr(attr)`                                        |                                                      |
| `dojo.html.getClass(node)`                       | `node.className` or `$(node).attr('class')`                 |                                                      |
| `dojo.html.hasClass(node, cls)`                  | `$(node).hasClass(cls)`                                     |                                                      |
| `dojo.html.isTag(node, tag)`                     | `node.tagName.toLowerCase() === tag`                        |                                                      |
| `dojo.html.getElementsByClass(cls, ...)`         | `$(parent).find('.cls')`                                    |                                                      |
| `dojo.html.classMatchType.IsOnly`                | CSS selector                                                |                                                      |
| `dojo.html.createNodesFromText(html)`            | `$(html)`                                                   |                                                      |
| `dojo.html.scrollIntoView(node)`                 | `node.scrollIntoView()`                                     |                                                      |
| `dojo.html.getBorderBox(node)`                   | `node.getBoundingClientRect()`                              |                                                      |
| `dojo.html.getAbsolutePosition(node)`            | `$(node).offset()`                                          |                                                      |
| `dojo.html.getViewport()`                        | `{ w: window.innerWidth, h: window.innerHeight }`           |                                                      |
| `dojo.html.getPixelValue(node, prop)`            | `parseInt($(node).css(prop))`                               |                                                      |
| `dojo.html.getMarginBox(node)`                   | `$(node).outerWidth(true)` / `outerHeight(true)`            |                                                      |
| `dojo.html.renderedTextContent(node)`            | `$(node).text()`                                            |                                                      |
| `dojo.html.toCoordinateObject(...)`              | `$(node).offset()` + dimensions                             |                                                      |
| `dojo.html.boxSizing.BORDER_BOX`                 | CSS `box-sizing: border-box`                                |                                                      |
| `dojo.html.ELEMENT_NODE`                         | `Node.ELEMENT_NODE` (=1)                                    |                                                      |
| `dojo.html.removeNode(node)`                     | `$(node).remove()`                                          |                                                      |
| `dojo.html.destroyNode(node)`                    | `$(node).remove()`                                          |                                                      |
| `dojo.dom.destroyNode(node)`                     | `$(node).remove()`                                          |                                                      |
| `dojo.dom.removeNode(node)`                      | `$(node).detach()` or `.remove()`                           |                                                      |
| `dojo.dom.removeChildren(node)`                  | `$(node).empty()`                                           |                                                      |
| `dojo.dom.replaceNode(old, new)`                 | `$(old).replaceWith(new)`                                   |                                                      |
| `dojo.dom.getFirstChildElement(node)`            | `node.firstElementChild`                                    |                                                      |
| `dojo.dom.ELEMENT_NODE`                          | `Node.ELEMENT_NODE`                                         |                                                      |
| `dojo.lang.assert(cond, msg)`                    | `console.assert(cond, msg)` or custom `ps.assert()`         |                                                      |
| `dojo.lang.assertType(val, type)`                | Custom type-check or remove                                 |                                                      |
| `dojo.lang.has(obj, prop)`                       | `prop in obj`                                               |                                                      |
| `dojo.lang.isUndefined(v)`                       | `v === undefined`                                           |                                                      |
| `dojo.lang.isString(v)`                          | `typeof v === 'string'`                                     |                                                      |
| `dojo.lang.isBoolean(v)`                         | `typeof v === 'boolean'`                                    |                                                      |
| `dojo.lang.isNumeric(v)`                         | `!isNaN(v)`                                                 |                                                      |
| `dojo.lang.isArray(v)`                           | `Array.isArray(v)`                                          |                                                      |
| `dojo.lang.isArrayLike(v)`                       | `Array.isArray(v) \|\| (v && typeof v.length === 'number')` |                                                      |
| `dojo.lang.isOfType(v, type)`                    | Custom check                                                |                                                      |
| `dojo.lang.type(v)`                              | `typeof v`                                                  |                                                      |
| `dojo.lang.forEach(arr, fn)`                     | `arr.forEach(fn)`                                           |                                                      |
| `dojo.lang.filter(arr, fn)`                      | `arr.filter(fn)`                                            |                                                      |
| `dojo.lang.map(arr, fn)`                         | `arr.map(fn)`                                               |                                                      |
| `dojo.lang.inArray(arr, v)`                      | `arr.includes(v)`                                           |                                                      |
| `dojo.lang.mixin(dest, src)`                     | `Object.assign(dest, src)` or `$.extend()`                  |                                                      |
| `dojo.lang.shallowCopy(obj)`                     | `{ ...obj }` or `Object.assign({}, obj)`                    |                                                      |
| `dojo.lang.hitch(ctx, fn)`                       | `fn.bind(ctx)`                                              |                                                      |
| `dojo.lang.setTimeout(fn, ms)`                   | `setTimeout(fn, ms)`                                        |                                                      |
| `dojo.lang.delayThese(arr)`                      | Custom sequential setTimeout                                |                                                      |
| `dojo.lang.declare(name, base, props)`           | ES6 class or prototype chain                                |                                                      |
| `dojo.declare(name, base, props)`                | ES6 class or prototype chain                                |                                                      |
| `dojo.json.serialize(obj)`                       | `JSON.stringify(obj)`                                       |                                                      |
| `dojo.json.evalJson(str)`                        | `JSON.parse(str)`                                           |                                                      |
| `dojo.string.trim(str)`                          | `str.trim()`                                                |                                                      |
| `dojo.string.startsWith(str, pfx)`               | `str.startsWith(pfx)`                                       |                                                      |
| `dojo.string.endsWith(str, sfx)`                 | `str.endsWith(sfx)`                                         |                                                      |
| `dojo.string.isBlank(str)`                       | `!str \|\| str.trim() === ''`                               |                                                      |
| `dojo.string.splitEscaped(str, delim)`           | Custom split function                                       |                                                      |
| `dojo.io.cookie.getCookie(name)`                 | `document.cookie` parser or js-cookie lib                   |                                                      |
| `dojo.io.cookie.setCookie(name, val)`            | `document.cookie = ...` or js-cookie lib                    |                                                      |
| `dojo.io.bind(request)`                          | `$.ajax(request)`                                           | **Done in Track A1**                                 |
| `dojo.render.html.ie` / `ie55` / `ie60` / `ie70` | Feature detection or remove (IE is dead)                    |                                                      |
| `dojo.render.html.safari`                        | Feature detection or remove                                 |                                                      |
| `dojo.uri.moduleUri(mod, path)`                  | Hardcoded relative path                                     |                                                      |
| `dojo.uri.dojoUri(path)`                         | Hardcoded relative path                                     |                                                      |
| `dojo.debug(msg)`                                | `console.debug(msg)`                                        |                                                      |
| `dojo.experimental(feature)`                     | Remove or `console.warn()`                                  |                                                      |
| `dojo.dnd.HtmlDragSource`                        | jQuery UI Draggable                                         |                                                      |
| `dojo.dnd.HtmlDropTarget`                        | jQuery UI Droppable                                         |                                                      |
| `dojo.dnd.dragManager`                           | jQuery UI internal or custom                                |                                                      |
| `dojo.dnd.TreeDragAndDropV3`                     | jQuery UI or jstree DnD                                     |                                                      |
| `dojo.collections.ArrayList`                     | `Array`                                                     |                                                      |
| `dojo.collections.Dictionary`                    | `Object` or `Map`                                           |                                                      |
| `dojo.collections.Stack`                         | `Array` (push/pop)                                          |                                                      |

---

## Recommended Track Plan

### Track A2 — "Low-Hanging Fruit" (Est. 1–2 days)

**Goal:** Eliminate all Easy files + create a `ps.util.compat` shim for `dojo.lang.*` functions used everywhere.

1. **Delete or stub all `__package__.js` files** (4 files) — they do nothing but `dojo.provide`/`dojo.kwCompoundRequire`
2. **Strip `dojo.provide`/`dojo.require` from all files** — replace with namespace declarations (`window.ps = window.ps || {};`)
3. **Create `ps/compat.js` shim** providing:
   - `ps.assert(cond, msg)` → wraps `console.assert()`
   - `ps.assertType(val, type)` → simple type check
   - `ps.ArrayList` → thin Array wrapper (or just use Array)
   - `ps.Dictionary` → thin Object/Map wrapper
   - `ps.Stack` → thin Array wrapper
4. **Migrate `ps/io/Response.js`** — remove sole `dojo.provide` line
5. **Migrate `ps/aa/SnippetMove.js`** — replace `dojo.lang.*` with compat shim
6. **Migrate `ps/content/History.js`** — replace `dojo.collections.Stack` with Array
7. **Migrate `ps/widget/ScrollableNodes.js`** — replace `dojo.lang.*` with compat shim
8. **Clean up `ps/io/Actions.js`** — remove residual `dojo.provide`/`dojo.require`

### Track A3 — "Medium: Event Binding & DOM" (Est. 3–5 days)

**Goal:** Migrate files that use `dojo.event.connect` + `dojo.html.*`/`dojo.dom.*` to jQuery.

Priority order (smallest to largest, easiest to hardest):

1. `ps/aa.js` — Core namespace, `JSON.parse`/`JSON.stringify` swaps, `String.startsWith()`
2. `ps/UserInfo.js` — Small file, light widget ref
3. `ps/aa/Page.js` — DOM queries only, IE-specific code can be removed
4. `ps/content/CreateItem.js` — Simple dialog
5. `ps/content/SelectTemplates.js` — Simple dialog
6. `ps/content/FolderSitesBaseTabPanel.js` — Base class (migrate before subclasses!)
7. `ps/content/FoldersTabPanel.js` — Subclass of above
8. `ps/content/SitesTabPanel.js` — Subclass of above
9. `ps/content/SearchTabPanel.js` — Moderate complexity, 7 event connects
10. `ps/content/SnippetPicker.js` — 14 `setStyle` calls, 3 `getElementsByClass`
11. `ps/content/Browse.js` — Browser detection code (much can be removed)
12. `ps/DivActionHelper.js` — Topic subscribe, IE detection (IE code removable)
13. `ps/workflow/WorkflowActions.js` — 11 event connects, multiple widget refs

### Track A4 — "Medium-Hard: Core AA Files" (Est. 4–6 days)

**Goal:** Migrate the big AA files that are heavy consumers of widgets but don't define them.

1. `ps/aa/Tree.js` — Heavy `dojo.collections.ArrayList` usage (16x), tree model
2. `ps/aa/Field.js` — Inline editing with widget refs + positioning
3. `ps/util.js` — Shared utility functions, `FloatingPane` dependency
4. `ps/content/BrowseTabPanel.js` — Largest content file (1,402 lines), 19 assertType calls
5. `ps/aa/Menu.js` — **72 `createWidget` calls!** — hardest in this tier
6. `ps/aa/controller.js` — **Largest file (2,474 lines)**, orchestrates everything

### Track A5 — "Hard: Custom Widgets" (Est. 5–8 days)

**Goal:** Rewrite all `dojo.widget.defineWidget` files.

Phase 1 — Simple widget wrappers (can be jQuery UI widgets or plain classes):
1. `ps/widget/PSButton.js` (20 lines)
2. `ps/widget/MenuBarItem2.js` (25 lines)
3. `ps/widget/TreeSelector.js` (33 lines)
4. `ps/widget/MenuBar2.js` (49 lines)
5. `ps/widget/MenuBarIcon.js` (45 lines)
6. `ps/widget/MenuBarItemDropDown.js` (54 lines)
7. `ps/widget/PSImageGallery.js` (54 lines)
8. `ps/widget/PopupMenu.js` (61 lines)
9. `ps/widget/PSSplitContainer.js` (66 lines)
10. `ps/widget/ContentPaneProgress.js` (56 lines)

Phase 2 — Complex widgets (heavy lifecycle, DnD):
11. `ps/widget/TreeIcon.js` (70 lines)
12. `ps/widget/TreeDndController.js` (79 lines) — **requires DnD migration first**
13. `ps/widget/Autoscroller.js` (254 lines) — **requires DnD migration first**
14. `ps/widget/Tree.js` (537 lines) — **Most complex widget, 27 debug calls**

### Track A6 — "Hard: Drag and Drop" (Est. 3–5 days)

**Goal:** Rewrite `dojo.dnd.*` usage, likely in parallel with Track A5 Phase 2.

1. `ps/aa/dnd.js` (566 lines) — Core DnD engine for Active Assembly snippet reordering
2. `ps/widget/Autoscroller.js` — Auto-scroll during drag (overlaps A5)
3. `ps/widget/TreeDndController.js` — Tree DnD (overlaps A5)

---

## Risk Notes

1. **`ps/aa/Menu.js`** has **72 calls to `dojo.widget.createWidget`** — this is by far the most widget-heavy file and will need the widget replacements from Track A5 to be done first.
2. **`ps/aa/controller.js`** at **2,474 lines** is the largest and most complex file. It touches every other `ps/` module. Migrate it last.
3. **IE-specific code** (`dojo.render.html.ie*`) appears in 6 files. Since IE is EOL, this code can likely be **deleted entirely** rather than migrated.
4. **`dojo.event.connectAround`** (AOP-style advice) appears in 3 files — this has no direct jQuery equivalent and needs a small custom wrapper.
5. **`dojo.collections.*`** classes (`ArrayList`, `Dictionary`, `Stack`) are used in 12 files — creating the compat shim early (Track A2) will dramatically simplify later tracks.
6. The **`dojo.declare` / `dojo.lang.declare`** pattern appears in 5 content files — these define pseudo-classes that should become ES6 classes or plain constructor functions.
7. **`dojo.event.topic.subscribe`** (pubsub) is used in 2 files — needs a custom pubsub or jQuery custom events.

---

## Summary Table

|   Track   |  Files |  Est. Days |              Description               |
|-----------|-------:|-----------:|----------------------------------------|
| A0 (done) |      — |          — | Pruned unused Dojo files               |
| A1 (done) |      1 |          — | `ps/io/Actions.js` → `$.ajax()`        |
| **A2**    |  **8** |    **1–2** | Package stubs, compat shim, easy files |
| **A3**    | **13** |    **3–5** | Event binding, DOM, declare → jQuery   |
| **A4**    |  **6** |    **4–6** | Core AA files (heavy widget consumers) |
| **A5**    | **14** |    **5–8** | Custom widget rewrites                 |
| **A6**    |  **3** |    **3–5** | Drag and drop                          |
| **Total** | **42** | **~16–26** | Complete Dojo removal from `ps/`       |

