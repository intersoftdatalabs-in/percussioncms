# Spec stub: React Content Editor

**Status**: Stub — **not** implemented in the Explorer action-execution train.  
**Split from**: Explorer server actions (`specs/992-react-content-explorer/contracts/action-execution.md`)

## Why this is separate

Explorer **Edit / Quick Edit / View content / View properties / revision CE** rows historically opened Data Flow Content Editor applications (`sys_action/checkoutedit.xml`, `sys_cxSupport/contenteditorurls.html`, `sys_ceSupport`, CM1 `?view=editor`). Those are schema-driven **forms and controls**, not Explorer chrome.

Opening the legacy CM1 editor from Explorer is **not** an acceptable stand-in.

## In scope (later)

- React editor route/shell (not `/cm/app/?view=editor`)
- REST for item fields, checkout/checkin, views (`sys_All`, metadata, revisions **form**, audit **form**)
- Control inventory (legacy CE controls → React)
- Quick Edit = workflow transition + checkout + this editor
- AA-doc checkout (`checkoutaadoc.xml`) when AA has a React host

## Out of scope here

- Explorer browse, menus, preview-by-template, purge, workflow toolbar, folder ops
- Data Flow Server as a UI renderer

## Explorer behavior until this spec lands

Dispatcher classifies `Edit`, `Edit_Content`, `Edit_Properties`, `Quick_Edit`, `View_Content`, `View_Properties`, `Revision_ViewContent`, `Revision_ViewProperties`, `Revision_Promote` as **`editor`**. The SPA shows a TMX message and does **not** navigate.
