# Spec: React Active Assembly

**Status**: Preview-first host implemented (Explorer open + assembled iframe). Slot add / create / arrange use relationship REST from the AA canvas. Contenteditable field editing is a later slice.  
**Split from**: Explorer server actions (`specs/992-react-content-explorer/contracts/action-execution.md`)

## Why this is separate

Explorer **Active Assembly**, slot add/create, arrange (move / change template / remove), and related-content menus historically opened Data Flow HTML (`sys_cxSupport/variantlistwithslots.html`, `sys_cxItemAssembly/itemassembly.html`, `sys_rcSupport/updaterelateditems.html`). Those need an assembly host (preview + slot overlays + relationship ids). Explorer folder browse has no slot context.

Opening leftover Dojo AA JSPs or Data Flow HTML from Explorer is **not** an acceptable stand-in.

## Host decision

**New window** with assembler preview + light overlay (classic AA shape). Not an in-place Explorer chrome route.

- Deep link: `spa.jsp?entry=assembly&contentId=&templateId=` → `/cm/app/assembly?…`
- Named window `percAssembly_{contentId}`
- **No** BrandBar / TopNav — chrome-less so the assembled page is the canvas

## Preview first (this slice)

When Explorer **Active Assembly** runs we already know the content id and the page or snippet **template**. This slice:

1. Opens the assembly window
2. Resolves templates via `GET /actions/find/templates/{id}?isAA=true` (falls back to preview templates)
3. Loads `GET /services/assembly/preview-location`
4. Renders `/assembler/render` in an iframe
5. Shows a light overlay (title, content id, template select, note)

Field editing is **not** invented here. When the Content Editor (`specs/995-react-content-editor`) is wired, the overlay can use **contenteditable** against known content ids / types on the assembled output. Slot add / create / arrange use `GET /services/assembly/slot-relationships/canvas` plus add / move / template-slot / delete REST when a slot (and for arrange, a relationship) is selected on the AA canvas.

## In scope (later)

- Contenteditable / content-type overlay (depends on 995)
- Slot Add (Content Browser + relationship REST)
- Slot Create (types + templates; creating an item still needs the Content Editor spec)
- Arrange move / change template-slot / remove (relationship REST; relationship id from AA)
- Compare (`sys_Compare`) may follow as a small add-on

## Out of scope here

- Explorer browse, Publish Now, revisions, translations, purge
- Implementing Arrange_* from folder browse with no slot selected
- Content Editor forms (`aa_table_editor` stays `specs/995-react-content-editor`)
- Dual-shipping leftover Dojo AA JSPs or Data Flow HTML

## Explorer behavior

| Action | Behavior |
|--------|----------|
| Item_ActiveAssembly / Enterprise / Corporate / Item_Assembly (and template children under those parents) | Open the assembly window |
| Slot add / create / arrange / change template / paste-as-link / move-to-slot | `rest` when AA has a selected slot (relationship id required for arrange). Folder browse without a slot does not invent Arrange_*. |
| Item_Preview template children | Unchanged: raw assembler preview window |
