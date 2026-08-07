# #2240 — Inventory: `displaytitle` call sites for inline link titles

**Date**: 2026-08-07  
**Parent**: [#946](https://github.com/intersoftdatalabs-in/percussioncms/issues/946) — Configure custom title field for inline links  
**Slice**: 1 of 4 — Inventory only (no product UI/runtime changes)  
**Follow-ups**: [#2241](https://github.com/intersoftdatalabs-in/percussioncms/issues/2241) control setting; [#2242](https://github.com/intersoftdatalabs-in/percussioncms/issues/2242) runtime resolve; [#2243](https://github.com/intersoftdatalabs-in/percussioncms/issues/2243) tests residual  

## Goal (this slice)

Map every hardcode/assumption of `displaytitle` (and peer title fields) when building or resolving **inline link titles** across TinyMCE, WebUI services, server-side link resolve, and content-type / control definitions.

## Executive summary

| Area | Hardcodes `displaytitle` for link **title**? | Timing | Priority for #946 |
|------|-----------------------------------------------|--------|-------------------|
| CMS insert path — assets (`PSRenderLinkService.renderPreviewResourceLink`) | **Yes** — `asset.getFields().get("displaytitle")` | Insert-time (server) | **P0 — primary** |
| CMS insert path — pages (`renderPreviewPageLink`) | **No** — uses `page.getLinkTitle()` → field `resource_link_title` | Insert-time (server) | **P0** (must support custom field here too) |
| TinyMCE `percadvlink` / `percadvimage` | **No** field name — consumes `renderLink.title` from server | Insert-time (client) | Wire config only if client needs field name |
| TinyMCE `rxinline` + legacy `editorinline.js` (Content Editor) | **No** `title` attribute from item fields (uses selection text only) | Insert-time (CE) | Lower for title-attr feature |
| Assembly `PSInlineLinkContentHandler` (images) | **Yes** — `rx:displaytitle` for `title` / alt refresh | Runtime assembly | **P1** for images; hyperlinks keep baked-in `title` |
| Managed links `PSManagedLinkService` | **Yes** on **images** only (`rx:displaytitle` → `title`) | Runtime | **P1** images; hyperlink path does **not** rewrite `title` |
| `PSLocationUtils` target title | **Yes** fallback property `displaytitle` | Runtime link gen | **P2** (location/metadata, not RTE dialog) |
| `sys_tinymce` control params | **No** title-field param today | Static control | **P0 for slice 2** |
| Shared field `displaytitle` (`rxs_ct_shared`) | Field definition (not a call site) | Schema | Context only |
| Nav / list widgets / templates | Many `displaytitle` uses | Publish templates | **Out of scope** for #946 unless later expanded |

**Important correction vs parent issue wording:** for **pages**, the CMS insert path already uses **`resource_link_title`** (`page.getLinkTitle()`), not the shared `displaytitle` field. Assets/files/images use **`displaytitle`**. Slice 3 should parameterize **both** paths with the same control setting + fallback chain.

## Architecture (current)

```
Author picks page/asset in TinyMCE link dialog
  → percadvlink.updateLinkData()
  → PercPathService.getInlineRenderLink(itemId)
  → GET …/render/preview/{id}/default
  → PSRenderLinkService.renderPreviewLink
       ├─ page  → renLink.setTitle(page.getLinkTitle())          // resource_link_title
       └─ asset → renLink.setTitle(fields.get("displaytitle"))  // HARDCODE
  → client sets <a title="…"> / <img title="…">

Later publish/preview assembly
  → rxhyperlink: rewrite href only (title stays as stored in HTML)
  → rximage / managed image: re-read rx:displaytitle into title (HARDCODE)
```

## Call-site table

| ID | Module | File | Symbol / location | Timing | Class | What it does | Change in slices 2–3? |
|----|--------|------|-------------------|--------|-------|--------------|------------------------|
| T1 | `modules/perc-tinymce` | `…/plugins/percadvlink/plugin.js` | `updateLinkData` ~L189–218; `addLink` ~L223–240 | **Insert-time (client)** | Consumer | Sets `data.title = renderLink.title`; writes `title` on `<a>` | Pass-through only; optional: stop overwriting manual title if product wants |
| T2 | `modules/perc-tinymce` | `…/plugins/percadvimage/plugin.js` | `getInlineRenderLink` callback ~L329–404 | **Insert-time (client)** | Consumer | Same for images: `cm1LinkData.title = renderLink.title` | Same |
| T3 | `modules/perc-tinymce` | `…/js/tinymce_init.js` | `getBaseConfig` / `file_picker_callback` | **Static + insert** | Config | Registers plugins, finder for links; **no** title-field option | Slice 2: accept new editor option from CE init |
| T4 | `modules/perc-tinymce` | `…/plugins/rxinline/plugin.js` | `rxinlinelink` buttons / slots | **Insert-time (CE)** | UI | Opens CE browser via `createInlineSearchBox`; slots 103/104/105 | No title field hardcode |
| T5 | system cms | `…/js/editorinline.js` | `inlineCallback` for `rxhyperlink` | **Insert-time (CE)** | Consumer | Builds `<a href=…>` with relationship attrs; **no** `title` from item | Optional later if CE should get title attr |
| S1 | `projects/sitemanage` | `…/PSRenderLinkService.java` | `renderPreviewResourceLink` **L489** | **Insert-time (server)** | **Hardcode** | `renLink.setTitle((String) asset.getFields().get("displaytitle"))` | **Slice 3 P0** |
| S2 | `projects/sitemanage` | `…/PSRenderLinkService.java` | `renderPreviewPageLink` **L331** | **Insert-time (server)** | Peer hardcode | `renLink.setTitle(page.getLinkTitle())` → `resource_link_title` | **Slice 3 P0** (custom field instead of link title when configured) |
| S3 | WebUI | `war/services/PercPathService.js` (and `cm/…` dual tree) | `getInlineRenderLink` | **Insert-time (client)** | API client | GET `RENDER_LINK_PREVIEW/{id}/default` | May need query/header if title field is per-editor (see design note) |
| S4 | `projects/sitemanage` | `…/PSManagedLinksConverter.java` | uses `renderPreviewPageLink` / `renderPreviewResourceLink` | Convert-time | Consumer | Title from same service paths | Follows S1/S2 |
| S5 | `projects/sitemanage` | `…/PSPreviewItemContent.java` | same | Preview servlet | Consumer | Same | Follows S1/S2 |
| R1 | `system/services` | `…/PSInlineLinkContentHandler.java` | `getAltTextAndTitleFromAsset` **L898–899** | **Runtime assembly** | **Hardcode** | `rx:displaytitle` → TITLE for **images** | **Slice 3 P1** (images) |
| R2 | `system/services` | `…/PSInlineLinkContentHandler.java` | `doRxHyperLink` **L542+** | **Runtime assembly** | N/A title | Updates **href only**; does **not** re-resolve title from fields | Hyperlink title stays insert-time HTML unless product adds refresh |
| R3 | `projects/sitemanage` | `…/PSManagedLinkService.java` | `renderImageLink` **L782–788** | **Runtime** | **Hardcode** | Image `title` from `rx:displaytitle` | **Slice 3 P1** |
| R4 | `projects/sitemanage` | `…/PSManagedLinkService.java` | anchor `render` path ~L650+ | **Runtime** | N/A title | Sets href / broken classes; **does not** set `title` from displaytitle | Hyperlinks: insert-time only today |
| R5 | `system/services` | `…/jexl/PSLocationUtils.java` | target title **L201–210** | **Runtime link gen** | **Hardcode fallback** | After `$sys.metadata.title`, falls back to property `displaytitle` | **P2** — confirm if used for RTE titles |
| C1 | system cms | `…/stylesheets/sys_Templates.xsl` | `psxctl:ControlMeta name="sys_tinymce"` ~L3828–3874 | **Static control** | Def | Params: width, height, config_src_url, css_file, **InlineLinkSlot**, InlineImageSlot, InlineVariantSlot, dlg_*, aarenderer, helptext — **no title field param** | **Slice 2 P0** add param |
| C2 | system cms | `sys_Templates.xsl` | `perc_tinymce_init({…})` ~L4102–4118 | **Static → editor** | Wire | Passes slots into TinyMCE init | Slice 2: pass new param name into init/options |
| C3 | system design | `PSControlMeta` / `PSControlParameter` | control library model | **Static** | Persist peer | Control params already serializable on field UISet | Slice 2: use existing param machinery |
| C4 | test peer | `projects/sitemanage/…/controlMeta.xml` | `sys_tinymce` ParamList | Test fixture | Peer | Smaller param set than production XSL | Update if tests assert param list |
| F1 | FastForward / shared | `…/shared/rxs_ct_shared.xml` | field `displaytitle` ~L1199+ | **Schema** | Field def | Shared field → `RXS_CT_SHARED.DISPLAYTITLE`, required | Default fallback target name |
| F2 | packages | `perc.Baseline/percPage.itemDef.contentType` | `page_title`, `resource_link_title` | **Schema** | Page fields | Page titles are **local** fields, not shared `displaytitle` | Custom config often points here (e.g. `page_title`) |
| F3 | packages | image/file asset defs | shared/local `displaytitle` | **Schema** | Asset fields | Standard asset title field | Default for assets |

### Explicit non-goals (related greps, not inline-link-title hardcodes)

| Area | Why excluded |
|------|----------------|
| `PSSiteSectionService` / `PSNavConfig` `navon.field.displaytitle` | Nav labels; already has **server property** override pattern, not RTE control setting |
| `PSAssetService` list rows `fields.get("displaytitle")` | Finder/list labels, not `<a title>` |
| `PSPageChangeHandler` blog `displaytitle` sync | Widget ↔ page link text sync, different feature |
| Velocity/templates `#field("displaytitle")` / `#fieldLink("displaytitle"…)` | Published content templates, not insert/resolve of managed inline links |
| WebDAV `FieldName>displaytitle` | WebDAV mapping |
| `PSEdition.displaytitle` | Publisher edition name field |

## Control-settings surface (slice 2 handoff)

**Existing pattern to copy** for a new control parameter on `sys_tinymce`:

1. **Declare** in `sys_Templates.xsl` under `psxctl:ControlMeta[@name='sys_tinymce']/psxctl:ParamList`  
   Suggested name: `InlineLinkTitleField` (or `inlineLinkTitleField` — match existing PascalCase peers: `InlineLinkSlot`).  
   Default: empty or `displaytitle` (empty = use product default per target type; see fallback).

2. **Render into editor init** next to `inlineLinkSlot` in the `perc_tinymce_init({…})` block (~L4109).

3. **Register TinyMCE option** in `rxinline` / `tinymce_init` (same style as `inlineLinkSlot` registration in `rxinline/plugin.js`).

4. **Persist** via existing content-type field control `ParamList` / Workbench control property UI (no new serialization format). Peers: `PSControlParameter`, `PSDisplayFieldElementBuilder.addParamListElement`.

5. **Where admins set it:** content type field that uses `sys_tinymce` (body/HTML fields), control properties — same place as Inline Link Slot id.

6. **No WebUI SPA control-settings screen** was found for these params; configuration is CE/Workbench control-parameter metadata driven by `sys_Templates.xsl` ControlMeta.

### Design note: source control vs target field

The control setting lives on the **source** rich-text field (the editor the author is typing in). The field name it stores is a property on the **target** content item being linked.

Implications for slice 3:

- Insert path must know the configured field name **from the active TinyMCE instance** when calling `getInlineRenderLink` / render preview API (e.g. query param `titleField=page_title`), **or** bake default into server only (global) — product intent is **per control**, so prefer passing from client.
- Runtime assembly for **hyperlinks** does not re-read title from JCR today; changing runtime only helps if product also refreshes title on publish, or for **images** (R1/R3).
- Recommend fallback chain (document in #2242):  
  `configured field (if non-blank value on target)` →  
  `type default` (`resource_link_title` for pages, `displaytitle` for assets)` →  
  empty / leave existing static title attribute.

## Slice 3 handoff — files to change first

### Must change (insert-time title for CMS UI)

1. **`PSRenderLinkService.java`**
   - `renderPreviewResourceLink`: replace hard-coded `"displaytitle"` with resolver(configured, default `"displaytitle"`).
   - `renderPreviewPageLink`: replace `page.getLinkTitle()` when custom field configured (still default to link title for BC).
2. **API surface** — accept optional title field name on preview render (query param or request body) so per-control setting works; keep default when absent.
3. **`PercPathService.getInlineRenderLink`** + **`percadvlink` / `percadvimage`** — pass editor option through to API.
4. **`sys_tinymce` + `perc_tinymce_init`** — already covered by slice 2 output.

### Should change (runtime title refresh for images)

5. **`PSInlineLinkContentHandler.getAltTextAndTitleFromAsset`** — configurable property name with `rx:displaytitle` fallback.  
   *Note:* assembly may not have easy access to source field’s control param; options: (a) store field name on relationship/HTML data attribute at insert, (b) global server property, (c) only apply custom field at insert and leave image runtime on displaytitle. Prefer (a) if true per-control behavior is required at publish.
6. **`PSManagedLinkService.renderImageLink`** — same decision as (5).

### Optional / later

7. CE path (`rxinline` / `editorinline.js`) if title tooltips required for legacy Content Editor inserts.  
8. `PSLocationUtils` if product wants location utils aligned.  
9. Tests: unit for resolver + fallback; Playwright for TinyMCE link dialog title (#2243).

## Existing tests / coverage gaps

| Coverage | Status |
|----------|--------|
| Unit tests asserting `displaytitle` → `PSInlineRenderLink.title` | Not found as focused tests for title field choice |
| Managed link href / rel tests | Present (`PSManagedLinkServiceAnchorTest`, etc.) — **do not** assert title field source |
| TinyMCE Vitest/Playwright for link title field | Residual #2243 |
| Control param round-trip for new param | Slice 2 + #2243 |

## Recommended implementation order

1. **#2241** — Add `InlineLinkTitleField` (name TBD) to `sys_tinymce` ControlMeta + XSL init wire + TinyMCE option registration. Default empty/absent.  
2. **#2242** — Resolver helper (shared): `resolveInlineLinkTitle(target, configuredField)` with fallback; use in `PSRenderLinkService` first; API query param; client pass-through. Then decide image runtime (R1/R3) vs insert-only.  
3. **#2243** — Component + E2E residual after product slices.

## Grep anchors used

```
displaytitle / rx:displaytitle / DISPLAYTITLE
getInlineRenderLink / PSInlineRenderLink / setTitle
percadvlink / rxinline / tinymce_init
sys_tinymce ControlMeta / InlineLinkSlot
PSRenderLinkService / PSInlineLinkContentHandler / PSManagedLinkService
```

## Acceptance checklist (#2240)

- [x] Documented inventory on parent-linked path  
- [x] Explicit list of files/modules that hardcode `displaytitle` (or peer) for inline link titles  
- [x] Clear handoff notes for slice 2 (control setting) and slice 3 (runtime/insert resolve)  
- [x] No product UI/runtime code changes in this PR  

---

> Co-Authored by Grok Build using grok-4.5 with agent main.
