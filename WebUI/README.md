# WebUI

This module contains the support for the User Interface for CMS.

## Building

mvn clean install

## Layout

Most ui elements are located under the war folder. When deployed they are placed into the main application war file under the cm folder.

war/


## Modern Publishing UI (feature 990)

Primary nav `view=publish` loads `cm/app/publishModern.jsp` (mirrored under `cm/pages/app`), mounting React `PublishingShell` via `PercModernUI`.

### Query parameters (allowlisted)

| Param | Purpose |
|-------|---------|
| `section` | `sites` (default), `status`, `logs`, `design`, `runtime` |
| `siteId` | Preselect site (safe charset) |
| `serverId` | Preselect server |

Classic Minuet `publish.jsp` and JSF `/ui/publishing`, `/ui/pubruntime` entries redirect to this shell.

Spec/plan/tasks: `specs/990-unified-publishing-ui/`.

## Modern Content Explorer (feature 992)

Modern React replacement for the legacy Miller-column Finder and the Desktop Content Explorer (DCE). New entry points mount `ContentExplorerShell` (US1 core navigate), plus per-feature panels for menus (US3), folder security (US4), search (US5), and advanced CE tools (US7). New reusable `ContentBrowser` dialog (US2) replaces the per-host asset / page / folder pickers.

### Entry points (modern JSP pilots)

| Pilot | URL | Component(s) mounted |
|-------|-----|----------------------|
| Modern explorer shell | `cm/app/explorerModern.jsp` | `ContentExplorerShell` |
| Modern ContentBrowser dialog | `cm/app/assetPickerModern.jsp` | `ContentBrowser` (asset-only, single-select) |
| Modern ContentBrowser dialog | `cm/app/pagePickerModern.jsp` | `ContentBrowser` (page-only, single-select) |
| Modern ContentBrowser dialog | `cm/app/folderPickerModern.jsp` | `ContentBrowser` (folder-only, single-select) |
| Action menu pilot | `cm/app/actionMenuModern.jsp` | `ContextMenu` + `ActionToolbar` |
| Folder security pilot | `cm/app/folderSecurityModern.jsp` | `FolderSecurityPanel` |
| Search panel pilot | `cm/app/searchModern.jsp` | `SearchPanel` |
| Advanced CE pilot | `cm/app/us7AdvancedModern.jsp` | `ClipboardPanel` + `SiteCopyWizard` + `SubfolderCopyWizard` + `DependencyViewer` + `RelationshipsView` |

All modern pilots are mirrored under `cm/pages/app/`. Legacy Finder chrome (`.perc-mcol`, `$.perc_finder()` widgets) is **not** loaded on any modern pilot page.

### Finder / DCE retirement

The legacy Miller-column Finder is hard-cut in 8.2:

- JSP entry points in `cm/app/` (webmgt, dashboard, admin, editAsset, editTemplate, adminWorkflow, users, siteArchitecture) no longer include `finder.jsp` / `finder_js.jsp` and no longer mount `$.Percussion.PercFinderView()`.
- The Desktop Content Explorer (DCE) is **not required for core admin** in 8.2 (CE-not-required sign-off, SC-007).
- Per-host migration: `host-asset-picker`, `host-page-picker`, `host-folder-picker` are migrated to the modern `ContentBrowser` (PRs #1391 + #1394). `host-aa-contentbrowser-dialog` is deferred to 8.3 (Dojo Track A blocker per the WebUI AGENTS.md Track A "no new Dojo code" rule). `host-home-library` is optional (gated on the 989 widget builder).
- Pre-existing Java finder consumers still remain in the legacy view/plugin layer (e.g. `PercFolderPropertiesDialog`, `PercNavigationManager`, `perc_newsitedialog`, `PercContributorUiAdaptor`); these are the per-host follow-up migration tracked in `specs/992-react-content-explorer/checklists/cutover-inventory.md` §C. They are not on the 8.2 critical path; they remain functional through 8.2.x.

### Modern REACT CHROME — interactive map

| Component | Mount path | Role |
|-----------|-----------|------|
| `ContentExplorerShell` | `explorerModern.jsp` | Tree + detail-list + action toolbar + context-menu compose |
| `ExplorerTree` | inside Shell | Sites / folders tree; lazy expand |
| `DetailList` | inside Shell | Item list with pagination; SC-005 perf regression |
| `ReducedActions` | inside Shell | Per-item menu (open/preview/createFolder/rename/move/copy/delete) |
| `ContextMenu` | inside Shell | Right-click menu driven by server `actions/...` REST |
| `ActionToolbar` | inside Shell | Top toolbar driven by the same server action set |
| `FolderSecurityPanel` | `folderSecurityModern.jsp` | ACL viewer + editor; `aclLockout.ts` client gate |
| `SearchPanel` | `searchModern.jsp` | Extended search with retry / open / reveal |
| `ClipboardPanel` | `us7AdvancedModern.jsp` | Copy / cut / paste to a target folder |
| `SiteCopyWizard` | same | 5-step site copy wizard |
| `SubfolderCopyWizard` | same | 3-step subfolder copy wizard |
| `DependencyViewer` | same | 6-dimension dependency summary (AA populated, 5 dimensions client-side preview) |
| `RelationshipsView` | same | 4 primary IA rows + AA / reverse details (same partial) |

### Spec / artifacts

- Spec/plan/tasks: `specs/992-react-content-explorer/`.
- Capability matrix roll-up: `specs/992-react-content-explorer/contracts/capability-matrix.md` §"T086 status roll-up (2026-07-20)".
- a11y checklist (T082): `specs/992-react-content-explorer/checklists/a11y-spotcheck.md`.
- i18n key inventory (T083): `specs/992-react-content-explorer/checklists/i18n-key-presence.md`.
- Cutover inventory + per-phase sign-off (T085): `specs/992-react-content-explorer/checklists/cutover-inventory.md`.
- 8.2 parity evidence (T089a): `docs/ai-generated/release/992-8.2-parity-evidence.md`.
- Security review (T089): `docs/ai-generated/release/security-review-992.md`.

### Verification

| Suite | Where | Pattern |
|-------|-------|---------|
| Vitest (component + a11y gate) | `WebUI/src/test/ts/{contentExplorer,contentBrowser}/` | `cd WebUI/src/main/frontend && npx vitest run` |
| Playwright (E2E + a11y gate) | `modules/perc-qa-automation/frontend/tests/` | `cd modules/perc-qa-automation/frontend && npm test` |

