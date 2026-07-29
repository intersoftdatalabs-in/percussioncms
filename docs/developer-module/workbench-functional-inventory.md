# Percussion Developer Module — Functional Inventory & Requirements

**Source system:** Rhythmyx 7.3.2 Workbench (Eclipse RCP Designer)  
**Codebase roots:** `Designer/ui`, `Designer/core`, `Designer/Src` (E2Designer / data pipelines), `webservices/design`, `ReleasedDocuments/online/com.percussion.doc.workbench`  
**Companion:** [data-pipeline-engine-inventory.md](./data-pipeline-engine-inventory.md)  
**Document type:** Tool-agnostic inventory and functional requirements  
**Intended consumer:** Sibling repository `percussioncms` (React-based Developer module design & implementation)  
**Status:** Reverse-engineered inventory (v1)

---

## 1. Purpose and rules of engagement

### 1.1 Purpose

Capture what the Rhythmyx Workbench enables a CMS implementer to do at **design time**, so a replacement **Developer module** can achieve functional parity without depending on Eclipse/SWT.

### 1.2 What this document is

- Inventory of navigation areas, design objects, editors, wizards, associations, and cross-cutting behaviors
- Functional requirements phrased as user-visible capabilities and data the user must manage
- Capability matrix and prioritization guidance for replacement work

### 1.3 What this document is not

- Not a React, routing, component-library, or visual design specification
- Not a wire-protocol or REST API contract (backend domains are noted only for orientation)
- Not a pixel-perfect layout recreation of Eclipse views

### 1.4 Terminology mapping (legacy → logical)

| Legacy Workbench term | Logical Developer-module term |
|----------------------|-------------------------------|
| Perspective / Views | Navigation modules / panels |
| Editor (multi-page) | Object detail workspace (tabs/sections) |
| Wizard | Create flow |
| Catalog / tree node | Navigator entry |
| Reference node | Association / link (not owned instance) |
| Connect to server | Authenticated design session |
| Object ACL | Design-time + runtime access control |
| CMS Navigator | Design object browser |

---

## 2. Product context and personas

### 2.1 Product role

The Workbench is the **design-time tooling surface** for configuring how content is modeled, assembled, secured for communities, presented in Content Explorer, and extended by server plugins.

It is distinct from:

| Surface | Role |
|---------|------|
| Content Explorer | Authors create/edit **content items** |
| Server Administrator | Server config, security providers, runtime ops |
| Workflow applications | Full workflow graph design (Workbench only catalogs workflows) |
| Package Manager / Deployer | Package install/export across servers |

### 2.2 Primary persona

**CMS implementer / developer** who:

- Defines content types and fields
- Defines templates/slots for assembly and publishing
- Configures Content Explorer UI (menus, searches, views, display formats)
- Assigns community visibility and object ACLs
- Registers extensions and relationship behavior
- Builds integration/query/update pipelines (historically “XML Applications”; see data-pipeline companion doc)

### 2.3 Session model

- One active server connection at a time
- Multiple saved connection profiles
- Optional auto-connect on start
- Disconnect and reconnect to another server without restarting the tool

---

## 3. Information architecture

### 3.1 Logical shell regions

The Workbench layout maps to four logical regions any replacement UI must support:

1. **Session / connection chrome** — connected server, connect/disconnect, user identity context  
2. **Primary design navigators** — hierarchical browsers of design objects by domain  
3. **Object workspace** — multi-section editor for the selected object (dirty state, save, validation)  
4. **Auxiliary panels** — properties, snippet palette, problems/validation list, help, object sorter, community visibility lens, file/DB explorers  

Default primary navigators (left stack):

| Navigator | Default visible | Domain focus |
|-----------|-----------------|--------------|
| Assembly Design | Yes | Slots, templates, item filters |
| Content Design | Yes | Content types, keywords, shared fields, localization, system def |
| System Design | Yes | Extensions, configs, relationship types, sites/workflows (refs), CMS files |
| UI Elements Design | Yes | CE controls, CX menus/entries, display formats, searches, views |
| Security Design | Yes | Communities, roles (catalog) |
| File Explorer | Yes | Local filesystem |
| XML Server | Yes (legacy) | XML applications, variants |
| Database Explorer | Yes (legacy/support) | Backend table catalog |
| Community Visibility | Placeholder / on demand | Objects by community ACL lens |

Auxiliary (bottom / secondary):

- Snippet palette (Velocity macros for template source)
- Properties sheet
- Problems / validation messages
- Help
- Object Sorter

### 3.2 Common navigator behaviors

Every primary design navigator tree must support, where applicable:

| Behavior | Description |
|----------|-------------|
| Catalog load | Load object lists from server for the connected session |
| Open | Double-click or context menu opens object in workspace |
| New | Context-sensitive create (wizard) for allowed types on node |
| Rename | When `renamable` for type |
| Delete | When `deletable` for type |
| Copy / paste | Clone or place copies where handlers allow |
| Refresh | Reload catalog branch |
| Security | Open ACL editor for securable owned objects |
| Release lock | Drop design lock without save |
| Drag-drop associations | Drop related objects onto association nodes |
| User folders | Create workbench folders to organize objects (not all roots) |
| System vs user partitions | Some trees separate non-deletable system objects from user objects |
| Reference decoration | Distinguish owned objects from references to objects living elsewhere |

### 3.3 Context menu operations (generic)

Observed action inventory (applies per-type with handler flags):

- Open  
- New… (type-specific submenu)  
- Copy, Paste, Delete, Rename  
- Refresh  
- Security (ACL)  
- Release Lock  
- Enable / Disable (content types)  
- Add templates / slots / workflows (association helpers)  
- Assign new searches by community  
- Show Object Sorter  
- XML application: start/stop, export, debug/tracing (legacy)

---

## 4. Domain object catalog

### 4.1 Capability legend

| Code | Meaning |
|------|---------|
| **F** | Full design: create, open, edit, save, typically rename/delete |
| **A** | Association only (link/unlink; object owned elsewhere) |
| **C** | Catalog / pick-list only (no full editor in Workbench) |
| **R** | Read-only or system-locked (copy maybe, no delete/rename) |
| **P** | Partial editor (subset of fields) |
| **L** | Legacy path |

### 4.2 Object matrix

| Object | Subtypes | Nav home | Capability | ACL | Import/Export | Notes |
|--------|----------|----------|------------|-----|---------------|-------|
| Content Type | Nav / uncategorized | Content Design | **F** + enable/disable | Yes | Yes | Core model; field subsystem |
| Shared Fields file | — | Content Design | **F** | No (handler) | — | Shared field sets reused by CTs |
| Content Type System Config | — | Content Design | **F** | — | — | Global system fields/meta |
| Keyword | — | Content Design | **F** | Yes | — | Choices list |
| Locale | — | Content Design | **F** (rename restricted) | Yes | — | |
| Auto Translation Set | singleton set | Content Design | **F** | — | — | Translation settings collection |
| Template | SHARED, LOCAL, GLOBAL, VARIANT | Assembly (+ XML Server for VARIANT) | **F** (VARIANT legacy-ish) | Yes | Yes | Multi-tab editor |
| Slot | Regular / Inline (property) | Assembly | **F** | Yes | — | Content finder + allowed CTs/templates |
| Item Filter | — | Assembly | **F** | Yes | — | |
| Community | — | Security | **F** | — | — | Role membership |
| Role | Community / Workflow / Unassigned | Security | **C** | — | — | Full admin elsewhere |
| UI Action Menu | Cascading/Dynamic × System/User | UI Elements | **F** user; **R** system | Yes | — | Entries nested under menus |
| UI Menu Entry | System / User | UI Elements | **F** user; **R** system | Yes | — | |
| UI Search | STANDARD / CUSTOM | UI Elements | **F** | Yes | — | DB or FTS standard |
| UI View | STANDARD / CUSTOM | UI Elements | **F** | Yes | — | CX folder views |
| UI Display Format | — | UI Elements | **F** | Yes | — | Columns + icons |
| CE Controls | SYSTEM / USER | UI Elements | **P** (config files) | No | — | Control library XML/files |
| Relationship Type | System / User | System Design | **F** user; **R** system | — | — | Cloning + effects |
| Extension | Many interface categories | System Design | **F** | No | — | Java / JavaScript registration |
| Configuration File | Logger, Nav, Page tags, Tidy, Thumbnail, Workflow props, Sys/User Velocity macros | System Design | **P** (text/config edit) | No | — | |
| CMS / Resource File | Folder / File | System Design | **F** files | No | — | App resource tree |
| Site | — | System + Community Visibility | **C** + **A** | Runtime ACL relevance | — | Catalog for associations |
| Workflow | — | System + Content Type props | **C** + **A** | Runtime ACL relevance | — | Not full WF designer |
| XML Application / Pipeline App | SYSTEM / USER | XML Server → modern “Pipelines” | **F** (+ modernize) | Partial | App XML export | Full engine: see data-pipeline-engine-inventory.md |
| XML Application File | Folder / File | under app | **F** | — | — | |
| USER_FILE folder | Workbench folder | Various | **F** | No | — | Organization only |
| LOCAL_FILE | Folder / File | File Explorer | **F** local | — | — | Client FS |
| DB_TYPE | Table / View / etc. | Database Explorer | **C** | — | — | Catalog only |
| SHARED_PROPERTY | — | (internal) | **P** | Yes | — | Client shared prefs |

---

## 5. Cross-cutting platform requirements

These apply across modules and **must** be specified for any parity replacement.

### 5.1 Connection management

**Requirements**

1. User can maintain multiple named server connection profiles.  
2. Profile fields: Name, Server (host/IP), Port, UID, Password, Save password (bool), Make default (bool), SSL (bool), Timeout (seconds, default 60).  
3. User can create, edit, delete, apply, and connect from the profile list.  
4. Only one active connection; connecting switches session catalogs.  
5. User can disconnect.  
6. Preference: automatically connect on open (when default + saved password).  
7. Locale selection related to connection (connection locales dialog exists in UI).  
8. Clear failure messaging on auth/network/timeout errors.

**Source anchors:** `PSConnectionsDialog`, help `12977.htm`, `14548.htm`.

### 5.2 Design object locking

**Requirements**

1. Opening an object for edit acquires a server-side design lock when the model supports it.  
2. Concurrent edit attempts surface lock errors with owner context where available.  
3. User can release lock without saving.  
4. Closing/saving lifecycle interacts with lock release consistently.  
5. Multi-operation strategies handle batch failures (`PSMultiOperationException` patterns).

**Source anchors:** `PSLockHelper`, `PSEditorBase`, models/proxies.

### 5.3 Save / dirty state / validation

**Requirements**

1. Editors track dirty state; save persists design object via model.  
2. Control-level validators: required, length, numeric, illegal chars, whitespace, duplicate name, file exists, content-type name rules.  
3. Problem list surface aggregates validation issues (Problems panel).  
4. Save blocked or warned when validators fail (per control registration).  
5. Multi-page editors validate across pages as needed.

### 5.4 Object ACL (Security dialog)

**Requirements**

1. For securable owned objects, user can open Object ACL editor.  
2. ACL entries: Role, Community, or User; always include non-deletable **Default** and **AnyCommunity** where applicable.  
3. Design access permissions: Read, Update, Delete, Modify ACL.  
4. Runtime access (for community-associated object types): Read (Content Explorer visibility).  
5. Runtime-relevant object types include: Content Types, Display Formats, Menus, Menu Entries, Searches, Sites, Templates, Variants, Views, Workflows.  
6. Add/Delete ACL entries (cannot delete Default/AnyCommunity).  
7. Preferences support **default object ACL** for newly created objects.

**Source anchors:** `PSAclDialog`, help `13625.htm`, preferences security page.

### 5.5 Associations (drag-drop and dialogs)

**Requirements**

1. System supports linking related design objects without full re-edit of both sides where possible.  
2. Association patterns include:  
   - Content Type ↔ Allowed Workflows  
   - Content Type ↔ Allowed Templates / Variants  
   - Template ↔ Contained slots  
   - Template ↔ Allowed content types (local templates: singular ownership semantics)  
   - Slot ↔ Allowed content types and nested allowed templates  
   - Menu ↔ Menu entries (ordered)  
   - Community visibility membership via ACL/runtime  
3. References in trees open the **real** object editor when opened (or navigate to owner).  
4. Deleting a referenced object updates dependent editors (e.g., template reacts to slot delete).

### 5.6 Naming

**Requirements**

1. Objects have system **name** (often immutable after create) and display **label**.  
2. Names must be unique within type scope and pass ID/text validators.  
3. Help documents naming constraints for objects (see Naming Objects topic).  
4. Rename supported only when handler allows.

### 5.7 Import / export

**Requirements**

1. Export Content Types to local filesystem.  
2. Import Content Types from local filesystem.  
3. Export Templates to local filesystem.  
4. Import Templates from local filesystem.  
5. Flows are multi-step wizards (select objects, select path, confirm).  
6. XML Application export of application XML (legacy path).

### 5.8 Community visibility lens

**Requirements**

1. Dedicated navigator groups design objects by community visibility.  
2. Supports inspecting which content types, templates, menus, entries, display formats, searches, sites, views, workflows are visible to a community.  
3. Complements per-object ACL editing; does not replace it.

### 5.9 Preferences

**Requirements**

1. General Workbench preferences (including auto-connect behavior).  
2. Security preferences: default ACL template for new objects.  
3. Preferences persist per user/client.

### 5.10 Help / guidance

**Requirements**

1. Contextual help topic mapping for editors/controls.  
2. Optional help panel.  
3. Ability to locate object IDs/GUIDs for supportability.

### 5.11 Identity & catalog refresh

**Requirements**

1. Every design object has stable identity (GUID / id) displayable for support.  
2. Catalogs refresh after create/delete/rename/association changes.  
3. Model change events keep open editors coherent.

---

## 6. Module: Content Design

### 6.1 Navigator structure

```
Content Types
  Navigation/          (system nav content types; limited mutate)
  [user folders]
  [content type instances...]
    Allowed Workflows/           (references)
    Allowed Templates and XSL Variants/  (references)
Keywords
  [keyword instances...]
Shared Fields
  [shared def files / field sets...]
Localization
  Locales/
  Translation Settings           (AUTO_TRANSLATION_SET)
Content Types Global Configuration  (system def)
```

### 6.2 Content Type — create flow

**Wizard:** New Content Type  

Typical steps (pages present in code/help):

1. **Properties** — name, label, description (and related identity fields)  
2. **Communities** — initial community visibility / ACL-related community assignment  
3. **Workflows** — allowed workflows + default workflow selection  

On Finish: open Content Type editor.

### 6.3 Content Type — editor

**Tabs / sections**

1. **Content Type tab (parent fields)**  
2. **Field Set tabs** — one tab per child field set (dynamic)  
3. **Properties tab**

#### 6.3.1 Content Type tab (fields)

**Layout regions**

- **Shared and system fields palette** — tree of available system + shared fields to pull in  
- **Fields and Field Sets table** — ordered list of fields/sets included in this type  
- **Actions:** Move up/down, Insert local field, Delete, Add child field set, Add shared/system field into type, Remove field back, Groups editor  

**Quick field creation**

- User can type a new local field name into the table  
- Assign control/datatype defaults  

**Field row essentials (inline)**

- Name  
- Label  
- Control  
- Other high-frequency properties exposed in main grid (see Fields and Field Sets editor help)

**Field properties (full dialog)**

User must manage:

| Field | Notes |
|-------|-------|
| Field name | Read-only after create |
| Data type | Often tied to control; may be read-only in dialog |
| Storage size | Backend storage |
| Default value | |
| Mime type mode | Default / From Selection / From Extension Field / From Mime Type Field |
| Mime type value | Depends on mode |
| Label | Display label |
| Mnemonic | ALT shortcut letter |
| Error label | Validation message text |
| Control | Editor control type |
| Show in summary | Child fields only |
| Show in preview | |
| Show clear field for binary | Binary fields |
| Allow search | Index field |
| Include in full-text multi-field query | |
| Read Only rules | Nested editor |
| Visibility rules | Nested editor |
| Validation rules | Nested editor |
| Transforms (input/output) | Nested editor |

**Nested rule editors (field-level)**

| Editor | Purpose |
|--------|---------|
| Field Validation | Ordered validation rules + parameters; defaults catalog documented in help |
| Field Visibility | Rules controlling show/hide |
| Field Editability (Read Only) | Rules controlling read-only state |
| Field Transforms | Input and output transforms with parameters |
| Control Properties | Control-specific parameters; Choices tab for choice sources |
| URL Request Properties | Internal vs external choice lookup requests |
| Create Choice Lookup Request | Build lookup request for choices |

**Child field sets**

- Add child table/field set → new editor tab  
- Child tab mirrors field grid for that set  
- Field set properties (name, label, etc.)  
- Show-in-summary behavior for child columns  

#### 6.3.2 Properties tab

| Field / control | Behavior |
|-----------------|----------|
| Content Type name | Read-only |
| Label | Editable |
| Description | Editable |
| Allowed workflows | Multi-select checklist of all workflows; All/None |
| Default workflow | Single select; must be in allowed set; sync rules when unchecked |
| Enable searching for this Content Type | Bool (default on) |
| Content Type Icon | None / Specified file / From File Field |
| Item transforms & validations | Five lists: Input Transforms, Output Transforms, Validation, Pre-Processing, Post-Processing |
| Extension list ops | Add, delete, reorder; conditions dialog where applicable |
| Max errors before stopping | Validation tab only |

#### 6.3.3 Content Type lifecycle extras

- **Enable / Disable** content type (not same as delete)  
- **Delete** content type  
- **Import/Export** content type definitions  
- Tree associations for workflows and templates via DnD or dialogs (Allowed Templates / Allowed Workflows)  
- Merge/demerge of system + shared field definitions into local mapper on load/save path  

### 6.4 Shared Field Definitions

**Create:** New Shared Field Definition File wizard (name/properties page).  

**Editor:** Shared Def editor  

- Maintain shared field sets and fields analogous to CT field UI  
- Used by content types via shared field inclusion  
- Delete shared field file or shared field set supported  

### 6.5 Content Types Global Configuration (System Def)

- Singleton-like system configuration object  
- Editable as structured field definition and/or underlying XML-oriented representation  
- Defines system fields available to all content types  

### 6.6 Keywords

**Create wizard steps**

1. Keyword properties (name, label, description)  
2. Choices list (value/label pairs; order)

**Editor**

- Maintain keyword metadata and ordered choices  
- CRUD keyword; used by controls as choice source  

### 6.7 Locales

**Create wizard:** locale properties (language/country style identity, label, description).  

**Editor:** maintain locale object fields.  

**Constraints:** rename restricted in navigator handler.

### 6.8 Translation Settings (Auto Translation Set)

- Single collection editor for auto-translation configuration across locales/content types  
- Create/modify/remove translation setting rows  

### 6.9 Content Design FR checklist

| ID | Requirement |
|----|-------------|
| CD-01 | Browse/create/open/save/delete content types |
| CD-02 | Organize content types in user folders |
| CD-03 | Define local fields and child field sets with order |
| CD-04 | Include system and shared fields into a content type |
| CD-05 | Configure full field properties including mime, search, control |
| CD-06 | Configure field validation, visibility, editability, transforms |
| CD-07 | Configure control parameters and choice sources |
| CD-08 | Configure allowed workflows and default workflow |
| CD-09 | Configure item-level transforms/validations/pre/post extensions |
| CD-10 | Enable/disable search indexing at type level |
| CD-11 | Configure content type icon strategy |
| CD-12 | Associate allowed templates/variants |
| CD-13 | Enable/disable content type for runtime use |
| CD-14 | Import/export content types |
| CD-15 | Maintain shared field definition files |
| CD-16 | Maintain system def / global field configuration |
| CD-17 | Maintain keywords and choices |
| CD-18 | Maintain locales and auto-translation settings |
| CD-19 | ACL + community visibility for content types |

---

## 7. Module: Assembly Design

### 7.1 Navigator structure

```
Slots/
  [user folders]
  [slot instances]
    Allowed Content Types/
      [CT refs]
        Allowed Templates/   (nested association)
Templates/
  Shared/
    [folders + shared templates]
      Contained Slots/
      Allowed Content Types/
  Type Specific/   (LOCAL)
    [local templates]
      Contained Slots/
      Allowed Content Type/   (singular semantics)
Global Templates/
  [global templates]
    Contained Slots/
Item Filters/
  [filters]
```

### 7.2 Slot

**Create wizard:** name/label/description essentials (`PSSlotPropertiesPage`).  

**Editor fields**

| Field | Description |
|-------|-------------|
| Slot name | Read-only after create |
| Label | CX display label |
| Description | Free text |
| Type | Regular \| Inline |
| Allowed relationship types | Active Assembly category relationships |
| Content finder | Extension selecting population strategy + parameters |
| Content finder parameters | Key/value args (e.g., query, template, max_results, resource) |
| Allowed content (table) | Content types (and nested template allowances via tree associations) |

**Content finder options (documented behaviors)**

- None → defaults to Regular  
- Regular — manual AA assignment; optional template param  
- Automatic — JSR-170 query; params: query (req), type, template (req), max_results  
- Legacy Automatic — XML app resource path + template  

**Inline vs Regular**

- Inline: for rich-text inline links/content; not assigned to normal templates/variants the same way  
- Regular: assignable to templates/variants  

### 7.3 Template

**Subtypes**

| Subtype | Meaning |
|---------|---------|
| SHARED | Usable by 0..N content types |
| LOCAL | Restricted to exactly one content type |
| GLOBAL | Site/global wrapper template (`#inner()` pattern) |
| VARIANT | Legacy XSL variant-style template |

**Create wizard pages (velocity/modern path)**

1. Template type (shared/local/global; variant has separate wizard)  
2. Output dialog (snippet/page/binary implications)  
3. General properties  
4. Target tables (DB publishing path when applicable)  
5. Slots selection  
6. Content types association  

**Editor tabs**

| Tab | Purpose |
|-----|---------|
| Source / Velocity | Template body source editing; snippet insertion |
| General | Label, description, assembler, AA format, output form, mime/charset, location prefix/suffix, publish mode; global variant of general is reduced |
| Bindings | Named binding variables; expression/value; system variables & `$rx.*` tools |
| Slots | Contained slots membership |
| Sites | Site associations for publishing usage |
| Target Tables | DB publishing targets when applicable |
| Variant-specific pages | Legacy variant general/slots/sites |

**General tab field inventory (local/shared)**

- Template name (RO), label, description  
- Assembler: Velocity / Binary / Dispatch (and legacy assemblers as cataloged)  
- Active Assembly format: Normal / Auto Index / Non-HTML  
- Output: Snippet / Text Page / Binary  
- For Text Page: global template option Default / Specified / None  
- Mime type (binary), Character set (text)  
- Location prefix/suffix  
- Publish: Always / Default / Never  

**Bindings**

- Add/edit/remove binding rows  
- Binding variable properties dialog  
- Access to system variables, system functions, and `$rx` tool namespaces (asmhelper, codec, cond, db, doc, ext, guid, i18n, keyword, link, location, nav, pagination, session, string, etc.)  
- Clipboard support for bindings (code present)

**Source editing requirements**

- Edit template source text  
- Insert Velocity snippets from palette (see Appendix C)  
- Optional Velocity-aware editing aids (syntax/support plugin `vedit` historically)

**Import/export:** templates supported.

### 7.4 Item Filter

**Create wizard + editor** to define filter name/label/description and filter rules (extensions of type item filter rule).  

Used when resolving/filtering items in assembly contexts.

### 7.5 Assembly FR checklist

| ID | Requirement |
|----|-------------|
| AS-01 | CRUD slots with type, relationship, content finder + params |
| AS-02 | Associate allowed content types/templates to slots |
| AS-03 | CRUD shared, local, global templates |
| AS-04 | Maintain template source, general meta, bindings, slots, sites |
| AS-05 | Support DB publishing target tables where used |
| AS-06 | Associate templates to content types (shared multi, local single) |
| AS-07 | CRUD item filters |
| AS-08 | Import/export templates |
| AS-09 | Provide snippet library for common Velocity macros |
| AS-10 | Support legacy variants at least as readable/editable objects if still in data |

---

## 8. Module: UI Elements Design

### 8.1 Navigator structure

```
Content Editor/
  System Controls   (RO config)
  User Controls     (user control definitions)
Content Explorer/
  Menus/
    System/ Cascading|Dynamic   (limited mutate)
    User/ Cascading|Dynamic
  Menu Entries/
    System|User
  Display Formats/
  Searches/
    Standard|Custom
  Views/
    Standard|Custom
```

### 8.2 Content Editor controls

- System controls: packaged defaults; not freely deleted  
- User controls: implementer-defined control definitions (file/config oriented)  
- Used when selecting field **Control** in content type field editors  

Replacement must allow listing available controls and managing user control definitions if still part of product strategy.

### 8.3 Menus and Menu Entries

**Types**

- Cascading menu (container)  
- Dynamic menu  
- Menu entry (command leaf)  
- System vs User copies  

**Create wizards:** New Menu, New Menu Entry  

**Editor tabs**

| Tab | Content |
|-----|---------|
| General (Menu) | Name/label/desc and menu-specific options |
| General (Entry) | Entry identity + presentation |
| Usage | Where menu appears in CX (contexts/modes) |
| Command | URL/command invocation details; window style dialog |
| Visibility | Visibility rules (roles/communities/conditions) |

**Associations**

- Drag menu entries onto cascading menus  
- Ordered children under cascading menus  
- System objects: copy to user rather than edit in place (typical pattern)

**Action panels**

- Special menu usage for action panels documented in help  

### 8.4 Display Formats

**Create wizard + editor**

- Define columns shown in CX lists  
- Column field selection via Field Selection editor shared with searches/views  
- Optional graphics / thumbnail preview columns  

### 8.5 Searches

**Subtypes**

- Standard — configured DB search or full-text search definition  
- Custom — URL-based custom search  

**Editor surfaces**

- Standard Database Search editor  
- Standard Full Text Search editor  
- Custom Search editor  

**Related**

- Configure which searches are available as “new search” defaults per community  
- Field selection for query/result fields  

### 8.6 Content Explorer Views

**Subtypes**

- Standard (DB or FTS backed)  
- Custom (URL)  

**Editor**

- Parent category, query definition, display format linkage, etc.  
- Field selection shared component  

### 8.7 UI Elements FR checklist

| ID | Requirement |
|----|-------------|
| UI-01 | Browse system/user controls; manage user controls |
| UI-02 | CRUD user menus and menu entries; respect system immutability |
| UI-03 | Configure menu usage, command, visibility |
| UI-04 | Compose cascading menus from entries via association |
| UI-05 | CRUD display formats and columns |
| UI-06 | CRUD standard and custom searches |
| UI-07 | CRUD standard and custom CX views |
| UI-08 | Shared field-selection UX for DF/search/view |
| UI-09 | Community defaults for new searches |
| UI-10 | ACL/runtime visibility for UI objects |

---

## 9. Module: Security Design

### 9.1 Navigator structure

```
Roles/
  Community/     (roles assigned to communities)
  Workflow/      (roles assigned to workflows)
  Unassigned/
Communities/
  [community instances]
    (expansion may show related refs)
```

### 9.2 Community editor

- Name (RO after create), description  
- Associated roles (multi-select / dual list pattern)  

**Wizard:** New Community with properties page.

### 9.3 Roles

- Catalog only in Workbench  
- Grouped by usage  
- Full role membership/admin is outside this module historically  

### 9.4 Security FR checklist

| ID | Requirement |
|----|-------------|
| SE-01 | CRUD communities |
| SE-02 | Assign roles to communities |
| SE-03 | Browse roles by community/workflow/unassigned |
| SE-04 | Edit object ACLs (cross-cutting) |
| SE-05 | Community visibility navigator |

---

## 10. Module: System Design

### 10.1 Navigator structure (summary)

```
CMS Files/                 (resource files/folders)
Extensions/
  JavaScript/
  Assemblers/
  Assembly Location Generators/
  Content List Generators/
  JEXL Expressions/
  Relationship Effects/
  Password Filters/
  Search Analyzers/
  Search Result Processors/
  Slot Content Finders/
  Template Expanders/
  Text Converters/
  Workflow Actions/
  Content Item Input/Output Transformers/
  Content Item Validators/
  Field Input/Output Transformers/
  Field Validators/
  Field Visibility Rules/
  Field Editability Rules/
  Item Filter Rules/
  Scheduled Tasks/
  Edition Tasks/
  XML Server/
    Request Pre-processors/
    Result Document Processors/
    UDF Processors/
Configurations/
  Logging, Navigation, Server Page Tags, Tidy,
  Thumbnail URL, Workflow, System Velocity Macros, User Velocity Macros
Relationship Types/
  System/   (copy-oriented)
  User/
Sites/      (catalog refs)
Workflows/  (catalog refs; expand allowed content types)
```

### 10.2 Extensions

**Create:** Extension Registration wizard  

**Editor**

- Java registration form (class, interfaces, properties, data dialog)  
- JavaScript registration form  
- Parameter definitions  

**Categories** matter for discoverability in pickers throughout CT/template/slot UIs.

### 10.3 Configuration files

Open as editable configuration/text resources:

- Logger properties  
- Navigation properties  
- Server page tags  
- Tidy properties  
- Thumbnail URL properties  
- Workflow properties  
- System / user Velocity macros  

### 10.4 Relationship Types

**Editor tabs**

1. General  
2. Properties (including field overrides)  
3. Cloning  
4. Effects (with execution context dialog)

System relationship types: non-renamable/non-deletable; user types full CRUD. Function properties dialog for effect/function configuration.

### 10.5 Sites & Workflows

- Listed for association/DnD  
- Not fully designed in Workbench  
- Workflow node can show Allowed Content Types (association view)

### 10.6 System FR checklist

| ID | Requirement |
|----|-------------|
| SY-01 | Browse extension categories; register/edit/delete extensions |
| SY-02 | Edit listed server configuration files |
| SY-03 | CRUD user relationship types; copy system types |
| SY-04 | Browse sites and workflows for association |
| SY-05 | Manage CMS/resource files for applications |
| SY-06 | Associate content types to workflows from either side |

---

## 11. Module: XML Server / Data Pipeline Engine

### 11.1 Scope note

This is a **complete request/data pipelining engine** historically authored as “XML Applications” via the E2Designer visual canvas (`Designer/Src`, hosted by `PSXmlApplicationEditor`). The UI and XML/DTD/XSL presentation layer are dated; the **pipeline semantics are not**.

**Full inventory + modernization brief:**  
[data-pipeline-engine-inventory.md](./data-pipeline-engine-inventory.md)

Treat as:

- **Strategic subsystem** for integrations, headless APIs, and data services (modernize IR/I/O)  
- **Legacy compatibility path** for existing XML applications  
- **Parallel track** to Content/Assembly P0 — do not block core CMS design tools, but do not dismiss as dead weight

### 11.2 Capabilities (summary)

| Capability | Description |
|------------|-------------|
| Application lifecycle | Create, save, start, stop, export, delete; system vs user apps |
| Resources | Query, update, and binary/non-text endpoints inside an app |
| Resource bootstrap | From HTML, DB table, DTD, white page, scratch, non-text |
| Page Data Tank | Document/payload schema (classic: DTD/XML) |
| Backend Data Tank | Tables + joins + datasource catalog |
| Mapper | Backend ↔ document mappings, conditions, UDFs, “Guess” |
| Selector | Structured WHERE builder + manual SQL + param tokens |
| Results Pager | Page size, max pages, multi-column sort |
| Updater properties | Allow create/update/delete + key columns + index check |
| Transaction Manager | Per-row vs all-rows atomicity |
| Exits / hooks | Pre/post processors, UDF functions, parameterized calls |
| Value system | Params, CGI/headers, cookies, user context, CMS fields, literals, functions |
| Result pages / XSL | Optional presentation layer |
| Cache + tracing | Query cache settings; runtime trace flags |

### 11.3 Modernization direction (see companion doc)

| Preserve | Replace |
|----------|---------|
| App → resource → pipe stage model | Swing freeform canvas as only UX |
| Mapper + selector + updater + txn + hooks | XML/DTD as only document model |
| Value system + catalogs | SQL as only backend language |
| Start/stop deploy semantics | XSL as required presentation |

**Target revival:** JSON Schema (or multi-schema) page tanks, pluggable datasources (SQL, REST, CMS), OpenAPI-ish endpoints, webhook/script hooks, versioned pipeline IR with classic XML import.

---

## 12. Supporting navigators

### 12.1 File Explorer

- Browse local filesystem  
- Open files in text editors  
- Drag local files into CMS resource locations where handlers allow  

### 12.2 Database Explorer

- Catalog datasources/tables/views  
- Support design-time selection for backend datatanks / SQL contexts  
- Not a full DBA tool  

### 12.3 Object Sorter

- Auxiliary organization/sorting aid for objects  

### 12.4 Problems

- Show validation/design problems for open editors / session  

---

## 13. Wizards inventory

| Wizard | Creates |
|--------|---------|
| Content Type | CONTENT_TYPE |
| Shared Def File | SHARED_FIELDS |
| Keyword | KEYWORD |
| Locale | LOCALE |
| Slot | SLOT |
| Template | TEMPLATE (shared/local/global) |
| Variant | TEMPLATE:VARIANT |
| Item Filter | ITEM_FILTER |
| Community | COMMUNITY |
| Display Format | UI_DISPLAY_FORMAT |
| Search | UI_SEARCH |
| View | UI_VIEW |
| Action Menu | UI_ACTION_MENU (menu) |
| Action Menu Entry | UI_ACTION_MENU (entry) |
| Relationship Type | RELATIONSHIP_TYPE |
| Extension Registration | EXTENSION |
| XML Application | XML_APPLICATION |
| Content Type Import/Export | — |
| Template Import/Export | — |

**Common post-condition:** open the corresponding editor on Finish.

---

## 14. Primary user journeys (parity scenarios)

### Journey A — New content type to usable model

1. Connect to server  
2. Create content type (wizard: communities, workflows)  
3. Add local fields; pull shared/system fields  
4. Configure field controls, validation, visibility  
5. Set properties (default workflow, search, icon)  
6. Associate templates  
7. Set ACL / community runtime access  
8. Enable content type  
9. Save / release lock  

### Journey B — Template for assembly

1. Create slot(s) with content finder and allowed types  
2. Create shared template  
3. Edit Velocity source; insert field/slot snippets  
4. Define bindings  
5. Attach slots; associate content types  
6. Set publish/AA options  
7. ACL for template runtime visibility  

### Journey C — CX search experience

1. Create display format columns  
2. Create standard search using field selection  
3. Create view using display format  
4. Configure menu entry to invoke search/view  
5. Assign community visibility  

### Journey D — Extension-backed field rule

1. Register Java extension in correct category  
2. On content type field, add validation/transform referencing extension  
3. Configure parameters/conditions  
4. Save and verify problems panel clean  

---

## 15. Parity prioritization (recommendation)

### P0 — Core Developer module

- Connection session  
- Content types + full field subsystem  
- Shared fields + system def (at least consume/edit essential)  
- Keywords  
- Templates (shared/local/global) + source/bindings/slots  
- Slots  
- Communities + object ACL  
- Locking, save, validation problems  
- Basic navigator IA for Content + Assembly + Security  

### P1

- UI Elements: menus/entries, searches, views, display formats  
- Item filters  
- Locales + translation settings  
- Extension registration  
- Template/content type import-export  
- Community visibility navigator  

### P2

- Relationship types  
- Configuration file editors  
- CMS resource files  
- Enable/disable CT and advanced icon strategies  
- Object sorter, preferences polish  

### P3 / Data Pipeline modernization track (parallel)

See [data-pipeline-engine-inventory.md](./data-pipeline-engine-inventory.md) for full FR and slices.

- **Slice A:** Pipeline IR + SQL runtime + JSON I/O + Java hooks + classic import  
- **Slice B:** Developer UI for tanks/mapper/selector (not Swing parity)  
- **Slice C:** REST datasources, OpenAPI publish, webhooks, nested filters  
- **Slice D:** Binary resources, XSL compatibility, deep tracing, optional classic visual metaphor  
- Database explorer remains supporting catalog UX for SQL tanks  
- Variant-only legacy flows if still needed beyond template subtype 

---

## 16. Out of scope / owned elsewhere

| Concern | Workbench posture | Elsewhere |
|---------|-------------------|-----------|
| Full workflow graph design | Catalog + CT association | Workflow web apps |
| Full site publish design | Catalog for association | Publishing design UIs |
| Full role/user directory admin | Role catalog only | Server Admin |
| Content item authoring | Not in scope | Content Explorer |
| Package deployment | Not Workbench core | Deployer / Package Manager |
| Server runtime config beyond listed files | Limited config nodes | Server Admin |

---

## 17. Backend domain orientation (non-normative)

Design capabilities historically cluster as:

| Domain | Objects |
|--------|---------|
| content | Content types, fields, shared/system defs, keywords, locales |
| assembly | Templates, slots, item filters |
| ui | Menus, entries, views, searches, display formats |
| security | Communities, ACLs, roles surfaces |
| system | Extensions, configs, relationships, applications |

`webservices/design/**` WSDLs mirror these domains. A modern REST surface can re-shape endpoints freely as long as FR capabilities remain.

---

## 18. Modernization notes (non-requirements)

Optional improvements for `percussioncms` design phase (not required for FR parity):

- Flatten deep nested dialogs into progressive disclosure panels  
- First-class search/filter in navigators  
- Multi-server read-only compare (beyond single connection)  
- Better bulk edit for field rules  
- Revive data pipelines with JSON IR + modern adapters rather than recreating Swing canvas (see companion doc)  
- Unify “reference vs owned” UX with clearer linking UI than pure DnD  

---

## Appendix A — Source anchors

| Area | Paths |
|------|-------|
| Product / views / editors / wizards | `Designer/ui/plugin.xml` |
| Perspective layout | `Designer/ui/.../PSRxPerspective.java` |
| Hierarchy IA | `Designer/ui/.../views/*_viewHierarchyDef.xml` |
| Object types | `Designer/core/.../PSObjectTypes.java` |
| Models / proxies | `Designer/core/.../models`, `.../proxies/impl` |
| Editors | `Designer/ui/.../editors/form/**` |
| Field subsystem | `Designer/ui/.../editors/form/ce/**` |
| Wizards | `Designer/ui/.../editors/wizards/**` |
| ACL UI | `Designer/ui/.../security/**` |
| Connection | `Designer/ui/.../connection/**`, `.../connections/**` |
| Help TOC | `ReleasedDocuments/online/com.percussion.doc.workbench/toc.xml` |
| Design services | `webservices/design/**` |
| Legacy XML designer | `Designer/Src/com/percussion/E2Designer/**` |
| Velocity editor plugin | `vedit/**` |

---

## Appendix B — Extension category inventory

From System Design hierarchy (picker categories):

- JavaScript  
- Assemblers  
- Assembly Location Generators  
- Content List Generators  
- JEXL Expressions  
- Relationship Effects  
- Password Filters  
- Search Analyzers  
- Search Result Processors  
- Slot Content Finders  
- Template Expanders  
- Text Converters  
- Workflow Actions  
- Content Item Input Transformers  
- Content Item Output Transformers  
- Content Item Validators  
- Content Item Field Input Transformers  
- Content Item Field Output Transformers  
- Content Item Field Validators  
- Content Item Field Visibility Rules  
- Content Item Field Editability Rules  
- Item Filter Rules  
- Scheduled Tasks  
- Edition Tasks  
- XML Server: Request Pre-processors, Result Document Processors, UDF Processors  

---

## Appendix C — Velocity snippet inventory

### Field macros

- `#displayfield(fieldname)`  
- `#field(fieldname)`  
- `#datefield(fieldname, format)`  
- `#field_if_set(before, field, after)`  
- `#datefield_if_set(before, field, format, after)`  
- `#fieldLink(fieldname, pagelink)`  

### Slot macros

- `#slot_simple(slotname)`  
- `#slot_wrapped(slotname, start, end)`  
- `#slot(slotname, header, before, after, footer, params)`  
- `#slot_page(... itemsPerPage, pageNumber)`  
- Raw slot loop (`#initslot` / `#slotItem` / `#endslot`)  
- `#node_slot(node, slotname, ...)`  

### Misc / examples

- `#inner()`  
- `#children(childname, templatename, header, before, after, footer)`  
- `#pager(pagecount, pagenumber, previous, pagetext, next)`  
- Sample HTML page skeleton  
- `#linkback_head()`  
- L-clamp global template sample  
- Breadcrumbs / top nav / left nav samples  

---

## Appendix D — Glossary

| Term | Definition |
|------|------------|
| Content Type | Schema + editor definition for a class of content items |
| Field / Field Set | Single property or repeating child table of fields |
| Shared field | Field definition reusable across content types |
| System field | Platform-provided field available to content types |
| Template | Rendering definition (Velocity/binary/dispatch/etc.) |
| Variant | Legacy template style (XSL/legacy assembler) |
| Slot | Named region in a template for related items/snippets |
| Item Filter | Rule set filtering items in assembly contexts |
| Community | Set of roles; drives runtime visibility via ACL |
| Display Format | Column layout for CX lists |
| Search / View | CX discovery UIs over repository content |
| Extension | Server plugin registered by interface category |
| Design lock | Exclusive edit lock on a design object |
| Reference | Tree node pointing at an object owned in another location |

---

## Appendix E — Handoff to `percussioncms`

Suggested use of this document:

1. **Product** marks P0/P1 rows as MVP vs later.  
2. **Design** produces React IA wireframes mapped to modules in §§6–10 without inventing missing objects.  
3. **Engineering** builds a capability checklist from FR IDs (CD-*, AS-*, UI-*, SE-*, SY-* + §5).  
4. **QA** turns Journeys A–D into acceptance tests.  
5. **API design** maps FR capabilities to services; do not assume SOAP shapes from `webservices/design` are final.

When deeper field-level ambiguity appears, consult:

1. This inventory  
2. Workbench help topic named in related sections  
3. Only then the Java editor class  

---

## Document history

| Version | Date | Notes |
|---------|------|-------|
| v1 | 2026-07-28 | Initial reverse engineering from Rhythmyx 7.3.2 Designer/Workbench sources and Workbench Help |
