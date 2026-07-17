# Home capability matrix (preserve classic CUI/Home)

**Feature**: 989-react-cui-widget-builder  
**Intent**: Classic Home was **functionally limited but product-complete for contributors**. The redesign modernizes **stack and presentation**, not a permission to drop contributor capabilities.  
**Source of truth for “as today”**: CUI Home + library mode as of `development` before cutover (`PercContributorUiAdaptor`, CUI widgets under `cm/cui/widgets/*`, classic `home.jsp` library mode).

**Status legend**

| Status | Meaning |
|--------|---------|
| **MUST** | Required by locked spec (FR-001 / US1 / SC-001 / assumptions) |
| **SHOULD** | Present in classic Home/CUI; keep unless product explicitly de-scopes |
| **OUT** | Explicitly out of scope (advanced finder admin, full editor rewrite) |
| **Impl** | Current PR branch status (honest) |

---

## 1. Shell & navigation

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| Open Home from main nav | Yes | MUST | Done | `view=home` → modern shell |
| Single modern shell (not iframe dual-mode) | Dual library vs CUI iframe | MUST (FR-002) | Done | New IA by design |
| Sections: Recent, **My Bookmarks**, Library, Search, Create | Recent + Bookmarks + Search tab; Add/Search dialogs; Library mode separate | MUST | Done (structure) | My Bookmarks = content list (`getMyContent`), not URL deep-links |
| Deep link `initialScreen=library\|list\|search\|newitem` | Yes | MUST | Done (map) | See `home-deep-links.md` |
| Visual language consistent with modern CM | CUI was already “minuet-ish” | MUST (US1) | Partial | Shell is functional; polish vs Dashboard ongoing |

---

## 2. Recent (classic “My Recent”)

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| List recent items user worked on | Yes (`recent` type item) | MUST (FR-001, SC-001) | Thin | Need proven REST shape + usable list |
| Show useful row metadata (name, path, type/status as classic list did) | Yes (content list) | SHOULD | Partial | Spec does not enumerate columns; product expects usable list |
| Open item into editor | Yes (`openPathItem`) | MUST (open from Home) | Partial | Must use product open-path pattern, not ad-hoc query only |
| Preview / copy / delete / bookmark from list | Yes (content list rollovers) | SHOULD | Missing | Not named in FR-001 acceptance; classic had them—**preserve unless de-scoped** |
| Empty-state messaging | Yes | SHOULD | Thin | TMX chrome present |

---

## 3. Bookmarks (classic “My Bookmarks”)

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| List bookmarked content (`getMyContent` / `item/mycontent`) | Yes (CUI tab) | **MUST** | Done (section) | Product decision 2026-07-17: **Keep** as Home section **My Bookmarks**. |
| Open bookmarked item | Yes | MUST | Partial | Path-first open (FR-001b) |
| Bookmark from item context (star on lists) | Yes | SHOULD | Missing | Follow-on |

**Clarify-note**: Spec deep-link “bookmarks” = **URL** bookmarks. **My Bookmarks content list** is separately locked as MUST (FR-002 / FR-002b).

---

## 4. Library (contributor browse/open)

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| Browse sites | Yes | MUST (FR-002a) | Done (API) | |
| Navigate folders | Yes | MUST | Done (API) | |
| Open content item | Yes | MUST | Partial | Open-path parity |
| Advanced finder admin actions | On other screens | OUT | N/A | FR-002a / Out of Scope |
| Empty: no sites (admin vs non-admin) | Yes | MUST (FR-003) | Thin | Messaging keys exist |

---

## 5. Search

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| Run content search and see results | Dialog + results tab | MUST | Thin | Criteria object must match product |
| Open result item | Yes | MUST | Partial | Same open-path as Recent |
| Workflow/state (and related) filters | Available via adaptor | SHOULD | Missing | Classic search dialog depth; minimum = working search + open |
| Empty / error messaging | Yes | SHOULD | Thin | |

---

## 6. Create capability matrix (primary)

Classic entry: **Add New** → type chooser (Page / Asset / Blog if blogs exist) → type-specific wizard.

### 6.1 Shared Create entry

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| Entry from Create / Add New | Yes | MUST | Partial | Section exists; no type chooser |
| Choose **Page** vs **Asset** vs **Blog** | Yes (`addwizard`) | MUST (FR-001) | Missing | Blog option depends on blogs existing (classic) |
| Block create when no site (with admin messaging) | Yes | MUST (FR-003) | Thin | |
| Multi-step wizard UX (not single free-text form) | Yes | MUST equal capability | Missing | Spec assumes wizards (entities, a11y, assumption) |

### 6.2 Page create

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| Select site (if multi-site) | Yes | MUST | Missing | |
| Select template (all for site) | Yes | MUST | Missing | Free-text template id ≠ parity |
| Prefer/recent templates filter | Yes | SHOULD | Missing | Classic UX |
| Select destination folder (tree / list) | Yes | MUST | Missing | Free-text path ≠ parity |
| Prefer/recent folders filter | Yes | SHOULD | Missing | |
| Page title | Yes | MUST | Partial | Present as field |
| Page file name | Yes | MUST | Partial as “name” | Classic separate title vs file + autofill rules |
| Title → file-name autofill / sanitization | Yes | SHOULD | Missing | Classic behavior |
| Authorization / not-allowed folder messaging | Yes | MUST equal | Missing | |
| Persist via same create semantics as today | Yes | MUST | Unproven | Form-field path via pagemanager; REST shape must match |
| After create: open or locate item | Yes | MUST (US1 #3) | Missing | |

### 6.3 Asset create

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| Select asset type (widget type) | Yes | MUST | Missing | |
| Recent vs all asset types | Yes | SHOULD | Missing | |
| Select asset folder | Yes | MUST | Missing | |
| Recent vs all asset folders | Yes | SHOULD | Missing | |
| Create asset (`createAsset`) | Yes | MUST | Missing | |
| After create: open/locate | Yes | MUST | Missing | |

### 6.4 Blog post create

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| Select site | Yes | MUST | Missing | |
| Select blog (from blogs for site) | Yes | MUST | Missing | |
| Title + file name (+ autofill) | Yes | MUST | Missing | |
| Auth messaging | Yes | MUST equal | Missing | |
| Create post | Yes | MUST | Missing | |
| After create: open/locate | Yes | MUST | Missing | |
| Hide blog option if no blogs | Yes | SHOULD | Missing | Classic add wizard |

### 6.5 Create — explicit non-goals

| Item | Status |
|------|--------|
| Rewrite content editor / Active Assembly | OUT |
| New create types beyond classic Home | OUT (unless product adds later) |
| Pixel-perfect clone of Knockout markup | OUT — **behavior** parity, modern UI |

---

## 7. Widget Builder (for completeness)

| Capability | Classic | Spec | Impl | Notes |
|------------|---------|------|------|-------|
| List / create / edit / validate / save / deploy / delete | Yes | MUST | Done (UI+API) | Live UAT still required |
| Enablement gate | Yes | MUST | Done | |
| Last-write-wins | Server order | MUST | Done (UX) | |

---

## 8. Implementation priority (close the gap without inventing scope)

1. **P0 — Create matrix §6 MUST rows** (chooser + page/asset/blog wizards + post-create open/locate).  
2. **P0 — Open item** via product path navigation (Recent/Library/Search).  
3. **P1 — Recent/Search usable lists** (metadata + proven APIs).  
4. **P1 — My Bookmarks** section shipped; deepen list metadata + star-from-list when needed.  
5. **P2 — List rollover actions** (preview/copy/delete/bookmark) if product wants full classic list parity.  
6. **P2 — Visual polish** to match modern CM (without changing capabilities).

---

## 9. Spec cross-links

- FR-001, FR-002, FR-002a, FR-003, US1, SC-001  
- Assumptions: CUI blog wizard / create equal capability  
- Plan Phase B: “Create: page/asset/blog … equal capability to CUI wizards”  
- `home-rest-inventory.md`, `data-model.md` Create request  

**Rule for implementers**: If a row is **MUST** and **Impl ≠ Done**, the feature is **not** acceptance-complete regardless of cutover/US3 status.
