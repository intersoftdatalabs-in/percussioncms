# Spec: React Content Editor

**Status**: First slice implemented — chrome-less field form + checkout.  
**Split from**: Explorer server actions (`specs/992-react-content-explorer/contracts/action-execution.md`)

## Why this is separate

Explorer **Edit / Quick Edit / View content / View properties / revision CE** rows historically opened Data Flow Content Editor applications (`sys_action/checkoutedit.xml`, `sys_cxSupport/contenteditorurls.html`, `sys_ceSupport`, CM1 `?view=editor`). Those are schema-driven **forms and controls**, not Explorer chrome.

Opening the legacy CM1 editor from Explorer is **not** an acceptable stand-in.

## Host

New window (peer of Active Assembly):

- `spa.jsp?entry=editor&contentId=&mode=edit|view` → `/cm/app/editor?…`
- Named window `percEditor_{contentId}`
- No BrandBar / TopNav

## This slice

1. Explorer **Edit** / **Quick Edit** / **Edit content** / **Edit properties** open the host in **edit** mode
2. **View content** / **View properties** / revision view open **view** mode (no checkout)
3. Edit mode calls existing `GET /services/itemmanagement/workflow/checkOut/{id}`
4. Fields: `GET` / `PUT /services/itemmanagement/item/fields/{id}` (scalar map from `PSContentItem`; `sys_*` except `sys_title` omitted; binary omitted)
5. Labels from `GET /services/contenttypes/{type}`
6. Save + Check In (existing checkIn REST)

## Later

- TinyMCE / file / image / keyword / community controls
- New Item (create in folder + content type)
- AA contenteditable overlay on the assembled preview
- Revision promote form
- Home / TopNav still use leftover `?view=editor` until those shells switch

## Out of scope here

- Explorer browse, preview-by-template, purge, folder ops
- Data Flow Server as a UI renderer
- Slot add/arrange
