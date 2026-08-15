# Spec stub: React Active Assembly

**Status**: Stub — **not** implemented in the Explorer action-execution train.  
**Split from**: Explorer server actions (`specs/992-react-content-explorer/contracts/action-execution.md`)

## Why this is separate

Explorer **Active Assembly**, slot add/create, arrange (move / change template / remove), and related-content menus historically opened Data Flow HTML (`sys_cxSupport/variantlistwithslots.html`, `sys_cxItemAssembly/itemassembly.html`, `sys_rcSupport/updaterelateditems.html`). Those need an **in-page assembly host** (preview + slot overlays + relationship ids). Explorer folder browse has no slot context.

Opening leftover Dojo AA JSPs or Data Flow HTML from Explorer is **not** an acceptable stand-in.

## Host decision (required before implementation)

1. **SPA route** `/cm/app/assembly?contentId=&templateId=` (recommended; matches Explorer / Publishing shells).
2. New window with assembler preview + overlay (closer to classic AA).

Do not implement a canvas until this is chosen.

## In scope (later)

- React AA host (not Data Flow / Dojo)
- Open from Explorer **Active Assembly** with selected item + **template** (`GET /actions/find/templates/{id}` + `GET /services/assembly/preview-location`)
- Slot Add (Content Browser + relationship REST)
- Slot Create (types + templates; creating an item still needs the Content Editor spec)
- Arrange move / change template-slot / remove (relationship REST; relationship id from AA)
- Compare (`sys_Compare`) may follow as a small add-on

## Out of scope here

- Explorer browse, Publish Now, revisions, translations, purge
- Implementing Arrange_* from folder browse with no slot selected
- Content Editor forms (`aa_table_editor` stays `specs/995-react-content-editor`)

## Explorer behavior until this spec lands

Dispatcher classifies Active Assembly / slot / arrange names as **`unavailable`**. The SPA shows a TMX message and does **not** navigate Data Flow HTML.
