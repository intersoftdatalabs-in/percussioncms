# DCE icon vocabulary retire-or-map audit (issue #6604)

Parent: #4530. Follow-on to #6515 (scope 1, PR #6605) and #6603/#6708 (scope 2).
Deliverable is this documented audit — no new glyphs.

## Source

84 GIFs under
`modules/DesktopContentExplorer/src/main/resources/com/percussion/cx/images/`
(55 base names + 29 `_EXPANDED` variants), loaded through
`PSImageIconLoader.loadIcon(iconKey, expanded, applet)`:

- **Tree / child-table node icons** — keyed by server-supplied `iconkey`
  (`PSNode.ICONKEY_ATTR`, defaults to the `TYPE_xxx` node type) plus the
  `ms_displayNameToIconName` checkout-status map (`CheckedIn` /
  `CheckedOut` / `CheckedOutByMe`) and `FolderActionManager.getFolderIconKey`.
- **Hardcoded chrome** — `up` / `down` (child-table sort buttons),
  `REFRESH_ICON`, `CONTENT_PATH_SEPARATOR_ICON` (`PSActionBar`),
  `Tree.collapsedIcon` / `Tree.expandedIcon`, `update` (login panel +
  header info), `user_member` / `group_member` / `virtual_role` (ACL
  dialogs), splash/about branding.
- **Fallback** — `Missing` is the loader's last-resort bitmap for any
  unresolvable key.

The modern pipeline under test is `WebUI/src/main/ts/contentExplorer/actionIcons.tsx`
(`resolveActionIcon` / `resolveMenuCommandIcon`, 45 glyphs + prefix
fallbacks), rendered by `ActionToolbar`, `ExplorerMenuBar`, and `ContextMenu`.
`resolveActionIcon` results below were verified against the shipped module
(the contract is pinned by `WebUI/src/test/ts/contentExplorer/dceIconAudit.test.ts`).

## Verdicts

### Mapped — modern glyph in the toolbar/menu pipeline

| DCE GIF(s) | Modern glyph | Notes |
|---|---|---|
| Folder, Folders, FolderRef | `folder` | Plus open/closed `FolderOpenGlyph` / `FolderClosedGlyph` in the detail list (`data-kind="folder"`, `data-folder-state`) and `▾/▸` tree disclosure |
| Item | `item` | Detail-list `ItemGlyph` (`data-kind="item"`) |
| Inbox | `inbox` | Workflow-state view; node label stays text |
| MyContent, CommunityContent | `user` | |
| AllContent | `grid` | |
| Refresh | `refresh` | The one chrome bitmap that survives as an action |
| Site, Sites | `site` | Site roots render as labeled tree rows |
| View, Views | `preview` (eye) | Views catalog lists them as text rows (group-empty / loading states, #6418) |
| SavedSearch, NewSearch, EmptySearch, CustomSearch, StandardSearch, SearchResults | `search` | Search panel + saved-search rows are text with NUL/blank guards |
| Relationship | `relationships` | |
| Slot, SlotItem | `slot` | Active Assembly slot rows |

### Fallback — generic glyph, distinction carried elsewhere

| DCE GIF(s) | Modern rendering | Why no distinct glyph |
|---|---|---|
| FolderMarked (+`_Expanded`), SiteSubfolderMarked (+`_EXPANDED`) | `folder` glyph; marked state = row selection highlight | `resolveActionIcon` prefix match on `folder`; selection highlight already conveys marked state (scope-1 audit) |
| SiteSubfolder (+`_EXPANDED`) | `folder` glyph; site name stays in the tree label text | Prefix order matches `folder` before `site`; folderish tree/detail rendering is identical |
| DTItem (+`_EXPANDED`) | `item` glyph | No design-time mode in the modern Explorer; design-time rows are generic items |

### Retired — replaced by text / CSS / existing chrome (no glyph)

| DCE GIF(s) | Replacement | Rationale |
|---|---|---|
| CheckedIn, CheckedOut, CheckedOutByMe | `CheckoutOwnerPanel` owner-name text (`explorer-checkout-owner-user`, `…-none`, `…-error`) | Per-state bitmaps collapsed to one text readout with loading/error states (#4910) |
| Reader | Enablement + text (no per-row read-only glyph) | Read-only is conveyed by disabled actions / notices, not a bitmap |
| MySelf, Nobody, SomeOneElse, Assignee | Owner/assignee names as text where shown | No assignee picker or avatar UI in modern Explorer; workflow transition is comment-only (#5199) |
| group_member, user_member, virtual_role | `FolderSecurityHost` / `FolderSecurityPanel` member/role text rows | ACL dialogs render identities as text; no avatar column |
| Parent (+`_EXPANDED`) | `ExplorerBreadcrumb` clickable ancestors + tree | Up-level node replaced by breadcrumb navigation (#6512) |
| Outbox (+`_EXPANDED`) | Nothing dedicated; server-exposed views appear as text rows in the Views catalog | Modern Explorer has no Outbox view; Inbox keeps its glyph, Outbox has no modern surface |
| ROOT (+`_EXPANDED`) | Tree root label text | No root bitmap in the CSS tree |
| Category (+`_EXPANDED`), SystemCategory | Text rows | No `Category` toolbar action exists, so the pipeline stays label-only (`resolveActionIcon("Category")` is `null`). Scope-1 docblock read "AllContent / Category → grid" as the AllContent mapping; corrected in the `actionIcons.tsx` docblock |
| SystemView | Text rows in the Views catalog | No bitmap; views render as labeled rows |
| ContentPathSeparator | `ExplorerBreadcrumb` bar (overflow ellipsis, copy-folder deep link) | Separator bitmap replaced by real breadcrumb chrome (#6512) |
| collapsedIcon, expandedIcon | `▸` / `▾` text disclosure with `aria-expanded` (`ExplorerTree`) | CSS/text toggle replaces `UIManager` tree bitmaps |
| up, down | Detail-list header sort buttons with `aria-sort` | Column sorting replaced bitmap sort buttons (#5759) |
| Admin | `(Admin)` text marker in the status-bar user readout | No admin badge bitmap; admin state is a text suffix (`ExplorerStatusBar`) |
| update | SPA login page / standard deploy flow | Applet login-panel button has no SPA equivalent |
| about, splash, main | Help menu entries (`help` glyph) + SPA loading states | Applet About bitmap / splash screen / main-window branding have no SPA bitmap equivalent |
| Missing (+`_EXPANDED`) | Text empty/error states (e.g. `SEARCH_EMPTY`, `LIST_EMPTY`, `VIEWS_RUN_EMPTY`) | Retired by design: the loader's last-resort bitmap is replaced by locale-resolved text states, never a missing-image bitmap |
| None, Other, OtherContent | Text rows / label-only buttons | Generic/unknown kinds carry no distinct modern meaning (scope-1 audit) |

### `_EXPANDED` variants (29 files) — retired as distinct bitmaps

Every `*_EXPANDED` GIF (including the mixed-case `FolderMarked_Expanded`)
loses its dedicated bitmap. Expand state is now carried by the `▾/▸`
disclosure control (tree, with `aria-expanded`) and the open/closed folder
glyph (detail list, `data-folder-state`). No expanded variant needs its own
row above — each follows its base name's verdict.

## Operator-visible summary

End users upgrading from DCE/Finder will notice: toolbar, menu-bar, and
context-menu actions render DCE-familiar icon+label pairs for the mapped set
above; everything else is text-first (labels, tree rows, breadcrumbs, status
bar, checkout owner, ACL members). Nothing user-visible was dropped without a
text or chrome replacement — the only workflows with no modern surface at all
are applet chrome (splash/About/update button), the Outbox view, assignee
avatars, and design-time items. See `product-docs/8.2/admin/content-explorer.md`
("Classic DCE icon vocabulary") for the operator-facing pointer.

## Gaps / residuals

None filed. The audit found no product defect: every user-visible GIF has a
glyph, text, or chrome replacement, and the one docblock/code nuance
(`Category` vs `grid`) is corrected in the `actionIcons.tsx` docblock in this
change set. If a future slice adds an assignee picker, an Outbox view, or a
design-time mode, that slice owns the new glyph + test + docs.
