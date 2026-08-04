# UI Layer Inventory & Technical Research

**Created**: 2026-02-27
**Purpose**: Detailed technical inventory supporting the [Unified UI Plan](unified-ui-plan.md)

---

## Table of Contents

- [Dojo 0.4.3 Footprint](#dojo-043-footprint)
- [Layer 1: Desktop Content Explorer](#layer-1-desktop-content-explorer-swingjavafx)
- [Layer 2: Rhythmyx Admin — MyFaces/JSF](#layer-2-rhythmyx-admin--myfacesjsf)
- [Layer 3: Rhythmyx Publishing — MyFaces/JSF](#layer-3-rhythmyx-publishing--myfacesjsf)
- [Layer 4: Package Manager — GWT/SmartGWT](#layer-4-package-manager--gwtsmartgwt)
- [Layer 5: WebUI Legacy — jQuery/Backbone](#layer-5-webui-legacy--jquerybackbone)
- [Layer 6: Contributor UI — RequireJS/Knockout](#layer-6-contributor-ui--requirejsknockout)
- [Layer 7: React Modern](#layer-7-react-modern)
- [Layer 8: Eclipse Workbench](#layer-8-eclipse-workbench-external)
- [Feature-to-Layer Matrix](#feature-to-layer-matrix)
- [Dojo API Surface Catalog](#dojo-api-surface-catalog-track-a-reference)

---

## Dojo 0.4.3 Footprint

**Version**: Dojo 0.4.3 — a pre-1.0 release from 2006. Declared in `dojo.js` (lines 36-42):
`major: 0, minor: 4, patch: 3, flag: "release"`, revision 8617.

### Vendor Distribution (~1,477 files)

Root: `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/`

|              Path              |                                     Description                                     |
|--------------------------------|-------------------------------------------------------------------------------------|
| `dojo/dojo.js`                 | Compiled/minified bootstrap (27,207 lines)                                          |
| `dojo/dojo.js.uncompressed.js` | Uncompressed source (42,375 lines)                                                  |
| `dojo/build.txt`               | Build manifest listing baked-in modules (138 lines)                                 |
| `dojo/src/`                    | Full source tree: widget/, dnd/, io/, lfx/, html/, lang/, event/, gfx/, data/, etc. |
| `dojo/src/widget/`             | ~100+ widget source files (pre-1.0 structure)                                       |
| `dojo/src/crypto/`             | Ancient Blowfish/Rijndael/MD5/SHA (scanner flagged)                                 |
| `dojo/tests/`                  | Test suite HTML/JS/XSL files                                                        |
| `dojo/demos/`                  | Demo HTML pages                                                                     |
| `dojo/release/dojo/`           | Nested release build output (includes SWF files)                                    |

### Custom `ps.*` Modules (~43 JS files)

Root: `system/cms/content/applications/sys_resources/ApplicationFiles/ps/`

|    Package    |                                                                                                Key Files                                                                                                 |          Purpose          |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| `ps.aa`       | aa.js, controller.js, dnd.js, Field.js, Menu.js, Page.js, Tree.js, SnippetMove.js                                                                                                                        | Active Assembly UI        |
| `ps.content`  | Browse.js, BrowseTabPanel.js, FoldersTabPanel.js, SitesTabPanel.js, SearchTabPanel.js, History.js, SelectTemplates.js, SnippetPicker.js, CreateItem.js                                                   | Content Browser           |
| `ps.io`       | Actions.js, Response.js                                                                                                                                                                                  | AJAX/server communication |
| `ps.widget`   | Tree.js, TreeSelector.js, TreeIcon.js, TreeDndController.js, PSButton.js, PSSplitContainer.js, PopupMenu.js, MenuBar2.js, ContentPaneProgress.js, ScrollableNodes.js, Autoscroller.js, PSImageGallery.js | Custom Dojo widgets       |
| `ps.workflow` | WorkflowActions.js                                                                                                                                                                                       | Workflow action triggers  |
| `ps.util`     | util.js                                                                                                                                                                                                  | Utilities/constants       |
| `ps.UserInfo` | UserInfo.js                                                                                                                                                                                              | User session info         |
| `sos`         | manifest.js, charting/tooltip.js, charting/events.js                                                                                                                                                     | SOS charting module       |

### Files Referencing Dojo (Outside Vendor Directory)

#### XSL Files

|                                                       File                                                       |                             Nature                             |
|------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `system/cms/content/applications/sys_rcSupport/ApplicationFiles/rceditor.xsl` (line 32)                          | Script loading: `<script src="../sys_resources/dojo/dojo.js">` |
| `system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl` (line 3883)       | Control file descriptor: `psxctl:FileDescriptor` for `dojo.js` |
| `system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/singleFieldEdit.xsl` (lines 159-169) | Widget markup: `dojoType="ps:PSButton"` on buttons             |
| `system/cms/content/applications/sys_searchSupport/ApplicationFiles/getQuery.xsl` (lines 133-134)                | Widget markup: `dojoType="Button"` on search buttons           |
| `system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/activeEdit.xsl` (lines 145-155)      | Dojo API calls via `ps.aa.controller`                          |

#### JSP Files

|                               File                               |                                 Nature                                 |
|------------------------------------------------------------------|------------------------------------------------------------------------|
| `system/ear/jsps/ui/content/ContentBrowserDialog.jsp` (line 21)  | Script loading + `dojo.hostenv.writeIncludes()`                        |
| `system/ear/jsps/ui/content/snippetpicker.jsp`                   | `dojoType="ContentPane"`, `dojoType="ps:PSButton"`                     |
| `system/ear/jsps/ui/content/selecttemplate.jsp`                  | `dojoType="LayoutContainer"`, `"ps:PSSplitContainer"`, `"ContentPane"` |
| `system/ear/jsps/ui/activeassembly/workflow/workflowactions.jsp` | `dojoType="Button"`, `dojoType="ps:PSButton"`                          |
| `system/ear/jsps/ui/activeassembly/workflow/adhocsearch.jsp`     | `dojoType="ps:PSButton"`                                               |
| `system/ear/jsps/ui/content/searchpanel.jsp`                     | `dojoType="ps:PSSplitContainer"`, `"ContentPane"`                      |
| `system/ear/jsps/ui/content/filterpanel.jsp`                     | CSS class `PsDojoLabelText`                                            |
| `system/ear/jsps/ui/content/commandpanel.jsp`                    | `dojoType="ps:PSButton"`                                               |
| `system/ear/jsps/ui/content/CreateItem.jsp`                      | `dojoType="Select"`, `"PSImageGallery"`, `"ps:PSButton"`               |
| `system/ear/jsps/ui/content/addressbarpanel.jsp`                 | `dojoType="ContentPane"`, `"Button"`                                   |
| `system/ear/jsps/ui/content/filteringtable.jsp`                  | `dojoType="filteringTable"`                                            |
| `system/ear/jsps/ui/content/sitesfolderpanel.jsp`                | `dojoType="ps:PSSplitContainer"`, `"ContentPane"`                      |

#### HTML Files

|                                            File                                             |                              Nature                               |
|---------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| `system/cms/content/applications/sys_resources/ApplicationFiles/html/sys_aaPageHeader.html` | Core AA page header: loads dojo.js, loads ~25 custom ps.* modules |

#### Java Files

|                                                 File                                                 |                   Nature                   |
|------------------------------------------------------------------------------------------------------|--------------------------------------------|
| `system/services/src/com/percussion/services/aaclient/PSPageTree.java` (line 389)                    | Emits `dojoType="TreeNodeV3"`              |
| `system/services/src/com/percussion/services/aaclient/PSActionBar.java` (line 304)                   | Emits `dojoType="MenuItem2"`               |
| `modules/ContentUI/src/main/java/com/percussion/content/ui/search/PSSearchResult.java` (lines 64-69) | References "dojo table widget" in comments |

#### CSS Files

|           File           |                              Nature                              |
|--------------------------|------------------------------------------------------------------|
| `ps/widget/PSButton.css` | `.dojoButton`, `.dojoButtonContents`, `.dojoButtonHover` styles  |
| `ps/widget/Tree.css`     | References `../../dojo/src/widget/templates/images/TreeV3/`      |
| `css/aa/styles.css`      | Overrides `.dojoButton`, `.dojoMenuBar2`, `.dojoSplitPane`, etc. |

#### Build Files

|                  File                  |                   Nature                    |
|----------------------------------------|---------------------------------------------|
| `system/ear/install-dojo.xml`          | Ant script to deploy Dojo from build output |
| `system/ear/install.xml` (line ~598)   | Invokes `install-dojo.xml`                  |
| `pom.xml` (lines 2337-2343, 2419-2471) | Spotless/Prettier exclusions for Dojo files |

---

## Layer 1: Desktop Content Explorer (Swing/JavaFX)

**Module**: `modules/DesktopContentExplorer/`
**Technology**: Java AWT + Swing + JavaFX (WebView)
**Entry Point**: `PSContentExplorerApplication.java` (extends `javafx.application.Application`)

**Server Communication**: JAX-WS (SOAP) via CXF/Jakarta XML-WS

- Service stubs: `Content`, `Security` (Login/Logout), `System` (TransitionItems)
- Operations: CreateItems, LoadItems, SaveItems, CheckinItems, PrepareForEdit,
  ReleaseFromEdit, AddFolderTree, FindFolderChildren, TransitionItems
- Also uses `HttpURLConnection` for some direct HTTP calls

**Key classes** (~67 Java files in `com.percussion.cx`): PSActionBar, PSClipBoard,
PSSearchViewActionManager, PSSaveSearchDialog, PSContentExplorerMenuBar,
PSContentExplorerHeader, PSContentExplorerLoginPanel, etc.

**Exclusive Features**: Clipboard/copy-paste, desktop application experience

---

## Layer 2: Rhythmyx Admin — MyFaces/JSF

**Technology**: JSF (MyFaces) + Apache Trinidad components
**Config**: `system/ear/WEB-INF/admin-faces-config.xml`

**Admin JSP pages (12)**:

|              Page               |              Feature              |
|---------------------------------|-----------------------------------|
| `console.jsp`                   | Rhythmyx Command Console          |
| `ScheduledTaskList.jsp`         | List scheduled tasks/timed events |
| `ScheduledTask.jsp`             | Edit scheduled task               |
| `TaskNotificationList.jsp`      | List task notification templates  |
| `TaskNotificationEditor.jsp`    | Edit notification template        |
| `TaskLogs.jsp`                  | View task logs                    |
| `TaskDetailLog.jsp`             | Detailed task log                 |
| `RemoveConfirmation.jsp`        | Delete confirmation dialog        |
| `DeleteAllTaskLogWarning.jsp`   | Warning before deleting all logs  |
| `NoTaskLogSelectionWarning.jsp` | Warning when no log selected      |
| `ConsistencyChecker.jsp`        | Content type consistency checker  |
| `RxFix.jsp`                     | Database fix utility              |

---

## Layer 3: Rhythmyx Publishing — MyFaces/JSF

**Technology**: JSF (MyFaces) + Apache Trinidad components
**Config**: `system/ear/WEB-INF/publishing-faces-config.xml`

**Publishing JSP pages (28)**:

|                       Page                        |                                                                           Feature                                                                            |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SiteList.jsp` / `SiteEditor.jsp`                 | Publishing site management                                                                                                                                   |
| `EditionList.jsp` / `EditionEditor.jsp`           | Edition management                                                                                                                                           |
| `ContentlistView.jsp` / `ContentlistEditor.jsp`   | Content list management                                                                                                                                      |
| `ContextList.jsp` / `ContextEditor.jsp`           | Publishing context management                                                                                                                                |
| `DeliveryTypeList.jsp` / `DeliveryTypeEditor.jsp` | Delivery type management                                                                                                                                     |
| `LocationSchemeEditor.jsp`                        | Location scheme configuration                                                                                                                                |
| `ActiveJobStatus.jsp`                             | Active publishing job status                                                                                                                                 |
| `RuntimeEdition.jsp` / `RuntimeEditionList.jsp`   | Runtime edition execution                                                                                                                                    |
| `AllPubLogs.jsp` / `SitePubLogs.jsp`              | Publishing logs                                                                                                                                              |
| `JobPubLog.jsp` / `ItemPubLog.jsp`                | Job/item level logs                                                                                                                                          |
| `DemandPublish.jsp`                               | On-demand publishing — server consumer rewired (#1842): `PSDemandPublishServlet` → `/cm/app/?view=publish&section=status`; JSP delete still deferred (#1818) |

---

## Layer 4: Package Manager — GWT/SmartGWT

**Module**: `PCM-PkgMgtUI/`
**Technology**: Google Web Toolkit (GWT) + SmartGWT
**Entry Point**: `PkgMgtUI.java` (implements `com.google.gwt.core.client.EntryPoint`)

**Key classes**:

|              Class              |                Purpose                 |
|---------------------------------|----------------------------------------|
| `PkgMgtUI.java`                 | Main entry point, data sources, layout |
| `PSPackagesTab.java`            | Packages listing tab                   |
| `PSVisibilityTab.java`          | Package visibility configuration       |
| `PSCommPkgSelectionDialog.java` | Community package selection dialog     |
| `PSConvertToSourceDialog.java`  | Convert-to-source dialog               |
| `PSUninstallPackageDialog.java` | Package uninstall dialog               |

**Exclusive Features**: Package install/uninstall, package visibility per community

---

## Layer 5: WebUI Legacy — jQuery/Backbone

**Module**: `WebUI/`
**Technology**: JSP + jQuery 3.6 + jQuery UI + Backbone + custom plugins

**Main entry**: `WebUI/war/app/index.jsp` → dispatches via `?view=` parameter

|      View       |          JSP           |                     Feature                     |
|-----------------|------------------------|-------------------------------------------------|
| `dash`          | `dashboard.jsp`        | Dashboard (now React)                           |
| `home`          | `home.jsp`             | Content browser/finder (miller columns)         |
| `editor`        | `webmgt.jsp`           | Page/content editor                             |
| `design`        | `admin.jsp`            | Design/admin view                               |
| `arch`          | `siteArchitecture.jsp` | Site architecture/navigation                    |
| `publish`       | `publish.jsp`          | Publishing status/logs (Bootstrap + Handlebars) |
| `workflow`      | `adminWorkflow.jsp`    | Workflow administration                         |
| `editTemplate`  | `editTemplate.jsp`     | Template editor                                 |
| `editAsset`     | `editAsset.jsp`        | Asset editor                                    |
| `widgetbuilder` | `widgetBuilder.jsp`    | Widget builder (Backbone + Backgrid)            |

**Service layer**: 31 JS service files in `WebUI/war/services/` — REST/JSON calls
**Controllers**: 11 JS files in `WebUI/war/controllers/`
**Views**: ~37 JS view files in `WebUI/war/views/`

---

## Layer 6: Contributor UI — RequireJS/Knockout

**Entry**: `WebUI/war/cui/index.html` → loads RequireJS bootstrap
**Technology**: RequireJS + Knockout.js + jQuery UI + widGEL + Bootstrap 3

**Widgets**:

|      Widget       |                         Feature                         |
|-------------------|---------------------------------------------------------|
| `app/`            | Main shell with tab navigation                          |
| `contentList/`    | Content listing (thumbnails, bookmarks, recent, search) |
| `search/`         | Search functionality                                    |
| `pagewizard/`     | Page creation wizard                                    |
| `assetwizard/`    | Asset creation wizard                                   |
| `blogpostwizard/` | Blog post creation wizard                               |
| `addwizard/`      | Generic add-item wizard                                 |
| `basedialog/`     | Base dialog component                                   |

---

## Layer 7: React Modern

**Entry**: `WebUI/src/main/ts/index.ts`
**Build**: Vite 6 + TypeScript 5.8
**Bridge**: `window.PercModernUI.mount(elementId, componentName, props)`

**Registered components**: Dashboard (24 widgets), HelloWorld — see `registry.ts`

**API client**: Typed Fetch wrapper with CSRF injection in `api/client.ts`

---

## Layer 8: Eclipse Workbench (External)

**Technology**: Eclipse RCP plugin (not in this repository)
**Documentation**: https://percussioncmshelp.intsof.com/percussion-cm1/developers/advanced/workbench/

**Views**:

|      View      |                      Purpose                      |
|----------------|---------------------------------------------------|
| Content Design | Content type editor, content editor configuration |
| System Design  | Widget definitions, web resources, themes         |
| XML Server     | XML Application editor (middleware data mapping)  |

---

## Feature-to-Layer Matrix

|      CMS Feature       |  DCE (1)  | JSF Admin (2) | JSF Pub (3) |  GWT (4)  | jQuery (5) |  CUI (6)  | React (7) | Workbench (8) |
|------------------------|-----------|---------------|-------------|-----------|------------|-----------|-----------|---------------|
| Dashboard/Home         |           |               |             |           | legacy     |           | **done**  |               |
| Content Browsing       | yes       |               |             |           | yes        | yes       |           |               |
| Content Editing        | yes       |               |             |           | yes        |           |           |               |
| Content Search         | yes       |               |             |           | yes        | yes       |           |               |
| Workflow Transitions   | yes       |               |             |           | yes        | yes       |           |               |
| Workflow Admin         |           |               |             |           | exclusive  |           |           |               |
| Page/Asset Creation    |           |               |             |           | yes        | yes       |           |               |
| Blog Post Creation     |           |               |             |           |            | exclusive |           |               |
| Template Design/Layout |           |               |             |           | exclusive  |           |           |               |
| Template Style/CSS     |           |               |             |           | exclusive  |           |           |               |
| Site Architecture/Nav  |           |               |             |           | exclusive  |           |           |               |
| User Management        |           |               |             |           | exclusive  |           |           |               |
| Role Management        |           |               |             |           | exclusive  |           |           |               |
| Category/Taxonomy      |           |               |             |           | exclusive  |           |           |               |
| Publishing Status/Logs |           |               |             |           | exclusive  |           |           |               |
| Pub Server Config      |           |               |             |           | exclusive  |           |           |               |
| Publishing Reports     |           |               |             |           | exclusive  |           |           |               |
| Widget Builder         |           |               |             |           | exclusive  |           |           |               |
| Revision Comparison    |           |               |             |           | exclusive  |           |           |               |
| Clipboard/Copy-Paste   | exclusive |               |             |           |            |           |           |               |
| Server Console         |           | exclusive     |             |           |            |           |           |               |
| Scheduled Tasks        |           | exclusive     |             |           |            |           |           |               |
| Task Notifications     |           | exclusive     |             |           |            |           |           |               |
| Consistency Checker    |           | exclusive     |             |           |            |           |           |               |
| DB Fix (RxFix)         |           | exclusive     |             |           |            |           |           |               |
| Variant Migration      |           | exclusive     |             |           |            |           |           |               |
| Pub Site CRUD          |           |               | exclusive   |           |            |           |           |               |
| Edition CRUD           |           |               | exclusive   |           |            |           |           |               |
| Content List Design    |           |               | exclusive   |           |            |           |           |               |
| Pub Context Mgmt       |           |               | exclusive   |           |            |           |           |               |
| Delivery Type Mgmt     |           |               | exclusive   |           |            |           |           |               |
| Location Scheme Config |           |               | exclusive   |           |            |           |           |               |
| Pub Runtime (jobs)     |           |               | exclusive   |           |            |           |           |               |
| Package Management     |           |               |             | exclusive |            |           |           |               |
| Package Visibility     |           |               |             | exclusive |            |           |           |               |
| Content Type Editor    |           |               |             |           |            |           |           | exclusive     |
| System Design          |           |               |             |           |            |           |           | exclusive     |
| XML Application Editor |           |               |             |           |            |           |           | exclusive     |
| Active Assembly        | Dojo 7    |               |             |           |            |           |           |               |
| Content Browser (AA)   | Dojo 7    |               |             |           |            |           |           |               |
| Relationship Editor    | Dojo 7    |               |             |           |            |           |           |               |

---

## Dojo API Surface Catalog (Track A Reference)

### `ps.io.Actions` — Server Communication Layer

All requests go to: `/contentui/aa?action=<ActionName>` (the `PSAAClientServlet`)

|            Action Name            |                        Method                         |              Purpose               |
|-----------------------------------|-------------------------------------------------------|------------------------------------|
| `Move`                            | `move()`                                              | Move slot item (up/down/reorder)   |
| `MoveToSlot`                      | `moveToSlot()`                                        | Move snippet between slots         |
| `GetUrl`                          | `getUrl()`                                            | Get URL for content editor actions |
| `GetActionVisibility`             | `getActionVisibility()`                               | Check action permissions           |
| `GetActionLabels`                 | `getActionLabels()`                                   | Get display labels                 |
| `GetAllowedContentTypeForSlot`    | `getAllowedContentTypeForSlot()`                      | Allowed content types for slot     |
| `GetContentTypeByContentId`       | `getContentTypeByContentId()`                         | Lookup content type by ID          |
| `GetTemplateImagesForContentType` | `getTemplateImagesForContentType()`                   | Template thumbnails                |
| `CreateItem`                      | `createItem()`                                        | Create new content item            |
| `GetItemPath`                     | `getItemPath()`                                       | Folder path for item               |
| `GetIdByPath`                     | `getIdByPath()`                                       | Resolve ID from path               |
| `GetAllowedSnippetTemplates`      | `getAllowedSnippetTemplates()`                        | Renderable template variations     |
| `GetItemTemplatesForSlot`         | `getItemTemplatesForSlot()`                           | Templates for slot                 |
| `GetFieldContent`                 | `getFieldContent()`                                   | Assembled field HTML               |
| `GetSlotContent`                  | `getSlotContent()`                                    | Assembled slot HTML                |
| `GetSnippetContent`               | `getSnippetContent()`                                 | Assembled snippet HTML             |
| `GetSnippetMimeType`              | `getSnippetMimeType()`                                | Snippet MIME type                  |
| `GetSnippetPickerSlotContent`     | `getRenderedSlotContent()`                            | Slot content for picker dialog     |
| `RemoveSnippet`                   | `removeSnippet()`                                     | Remove snippet relationship(s)     |
| `AddSnippet`                      | `addSnippet()`                                        | Add snippet to slot                |
| `Workflow`                        | `checkInItem()`, `checkOutItem()`, `transitionItem()` | Workflow operations                |
| `UpdateItem`                      | `getUpdateItemUrl()`                                  | URL for item update (form-based)   |
| `GetMaxTimeout`                   | `getMaxTimeout()`                                     | Session timeout value              |
| `GetLocaleCount`                  | `getLocaleCount()`                                    | Number of enabled locales          |
| `GetSearchResults`                | `getRcSearchUrl()`                                    | Related content search URL         |
| `Test`                            | `test()`                                              | Server connectivity test           |

### Dojo → jQuery API Translation Reference

|              Dojo 0.4.3 API               |                 jQuery Equivalent                 |
|-------------------------------------------|---------------------------------------------------|
| `dojo.byId(id)`                           | `$('#' + id)[0]` or `document.getElementById(id)` |
| `dojo.query(selector)`                    | `$(selector)`                                     |
| `dojo.event.connect(obj, event, handler)` | `$(obj).on(event, handler)`                       |
| `dojo.io.bind({ url, sync, mimetype })`   | `$.ajax({ url, async, dataType })`                |
| `dojo.lang.declare("Name", Base, {})`     | ES6 class or `$.extend()` pattern                 |
| `dojo.lang.extend(Class, mixin)`          | `$.extend(Class.prototype, mixin)`                |
| `dojo.provide()` / `dojo.require()`       | Script tags in load order                         |
| `dojo.hostenv.writeIncludes()`            | Remove — not needed                               |

### Dojo Widget → jQuery UI Translation Reference

|               Dojo Widget               |             jQuery Replacement             |
|-----------------------------------------|--------------------------------------------|
| `dojo.widget.Button` / `ps:PSButton`    | `<button>` + CSS, or jQuery UI Button      |
| `dojo.widget.Dialog`                    | jQuery UI Dialog                           |
| `dojo.widget.TabContainer`              | jQuery UI Tabs                             |
| `dojo.widget.ContentPane`               | `<div>` + `$.load()` for AJAX content      |
| `dojo.widget.SplitContainer`            | CSS flexbox + jQuery UI Resizable          |
| `dojo.widget.LayoutContainer`           | CSS flexbox/grid                           |
| `dojo.widget.FloatingPane`              | jQuery UI Dialog (draggable, resizable)    |
| `dojo.widget.Menu2`                     | jQuery UI Menu                             |
| `dojo.widget.FilteringTable`            | DataTables.js or plain table w/ filtering  |
| `dojo.widget.TreeV3` / `ps.widget.Tree` | FancyTree                                  |
| `dojo.dnd.*` / `ps.aa.dnd`              | jQuery UI Draggable + Droppable + Sortable |

---

## Security Scanning Context

|              Scanner               |              Scope               |                        JS Coverage                         |
|------------------------------------|----------------------------------|------------------------------------------------------------|
| CodeQL (GitHub Actions)            | Static analysis on push + weekly | Scans ALL JS/TS — flags vulnerable patterns in legacy code |
| OWASP Dependency-Check (Maven)     | Maven dependency CVE + RetireJS  | Can detect CVEs in vendored JS (Dojo, old jQuery)          |
| Dependabot (dependency-submission) | Maven dependency graph           | Maven deps only — no JS                                    |
| npm audit                          | Not configured in CI             | Would need to be added for WebUI `package.json`            |

Dojo 0.4.3 known CVE exposure: CVE-2018-6561, CVE-2020-4051, CVE-2021-23450.
The distribution also includes `src/crypto/` with ancient Blowfish/Rijndael/MD5/SHA implementations.
