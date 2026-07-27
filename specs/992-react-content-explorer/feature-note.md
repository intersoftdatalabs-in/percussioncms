# Feature note — Modern Content Explorer (992) operator summary

**Audience**: CMS operators / release-engineering / UAT.
**Spec**: [`spec.md`](../spec.md)
**GA target**: 8.2 (FR-029 / SC-012).

## What changed

The legacy Miller-column Finder (jQuery / Dojo plugin `$.perc_finder()`) and
the Desktop Content Explorer (DCE) Java application are replaced by a single
React/TypeScript modern Content Explorer. All the JSP shells in `cm/app/`
still exist; they load a different pilot page (`*Modern.jsp`) which mounts
the new components via the `PercModernUI` bridge.

## What is gone in 8.2

- The Miller-column Finder chrome (`.perc-mcol`, `perc_finder.js`,
  `PercFinderView.js`) is removed from `webmgt.jsp`, `dashboard.jsp`,
  `admin.jsp`, `editAsset.jsp`, `editTemplate.jsp`, `adminWorkflow.jsp`,
  `users.jsp`, `siteArchitecture.jsp`. Loading any of these pages now mounts
  the modern `ContentExplorerShell`.
- The Desktop Content Explorer (DCE / Web Start) is **not required** for core
  admin in 8.2. The asset / page / folder picker dialogs use the modern
  `ContentBrowser` React component.
- Legacy `perc_finder.js` and `PercFinderView.js` files remain in the WAR
  briefly (8.2.x) for per-host consumers that have not been migrated yet;
  the modern entry points do not load them.

## What is new

|            Component            |                                           Pilot URL                                            |                              Capability                              |
|---------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `ContentExplorerShell`          | `cm/app/explorerModern.jsp`                                                                    | Tree + list + reduced-action toolbar                                 |
| `ContextMenu` + `ActionToolbar` | `cm/app/actionMenuModern.jsp`                                                                  | Server-driven action menu (≥10 actions)                              |
| `FolderSecurityPanel`           | `cm/app/folderSecurityModern.jsp`                                                              | View + edit folder permissions; self-lockout warning                 |
| `SearchPanel`                   | `cm/app/searchModern.jsp`                                                                      | Extended search; open / reveal from results                          |
| `ClipboardPanel`                | (inline in `us7AdvancedModern.jsp`)                                                            | Multi-item copy / cut / paste                                        |
| `SiteCopyWizard`                | same                                                                                           | 5-step site copy wizard                                              |
| `SubfolderCopyWizard`           | same                                                                                           | 3-step subfolder copy wizard                                         |
| `DependencyViewer`              | same                                                                                           | 6-dimension dependency summary (AA populated; 5 client-side preview) |
| `RelationshipsView`             | same                                                                                           | 4 IA-primary rows + AA / reverse details (same partial)              |
| `ContentBrowser` dialog         | `cm/app/assetPickerModern.jsp`, `cm/app/pagePickerModern.jsp`, `cm/app/folderPickerModern.jsp` | Replaces legacy asset / page / folder pickers                        |

## What is unchanged

- Backend endpoints: every modern action calls an existing REST endpoint
  (`pathmanagement/path/*`, `actions/...`, `searchmanagement/search/get/extendedresults`,
  `pagemanagement/page/copy/*`, `sitemanage/site/copy`). **No new server
  façade** ships in 8.2 for this feature.
- Authentication + CSRF: all client→server calls go through the existing fetch
  wrapper with the CSRF token attribute from the JSP bootstrap.
- Authorization: server-side AuthZ is authoritative. The new client surfaces
  (`aclLockout.ts`, `canPasteInto`, `SEARCH_PERMISSION_DENIED`) provide UX
  gates only.

## Operator quick references

|                           I want to…                           |                                                           Where                                                            |
|----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| See the modern explorer on a shell                             | `cm/app/explorerModern.jsp` (or the legacy shell JSPs that now redirect internally)                                        |
| Find a path picker dialog                                      | `assetPickerModern.jsp` / `pagePickerModern.jsp` / `folderPickerModern.jsp`                                                |
| Restore the legacy Finder chrome                               | **Not possible in 8.2**. The Finder is hard-cut; see [cutover-inventory.md](./cutover-inventory.md) for the per-host list. |
| Map a legacy `$.perc_finder()` call site to its modern adapter | see [cutover-inventory.md](./cutover-inventory.md) §C "View / plugin / widget consumers"                                   |
| Run the Playwright spec                                        | `cd modules/perc-qa-automation/frontend && npm test`                                                                       |
| Run the Vitest suite                                           | `cd WebUI/src/main/frontend && npx vitest run`                                                                             |

## Acceptance evidence

See [`docs/ai-generated/release/992-8.2-parity-evidence.md`](../../../docs/ai-generated/release/992-8.2-parity-evidence.md) (T089a) for the aggregated parity artifact consumed by the SC-012 release decision (T090). The DependencyViewer / RelationshipsView **partial state (1 of 6 dimensions populated, 5 client-side preview)** is the matrix item that requires a release-manager decision; see the SC-012 packet there.

## Restricted / out-of-scope items

- `host-aa-contentbrowser-dialog` (legacy Dojo AA picker) → **8.3+**.
  Pre-req is Dojo Track A removal per the WebUI AGENTS.md Track A "no new
  Dojo code" rule.
- `host-home-library` (Home Library browse) → optional, gated on the 989 widget
  builder readiness.
- Pre-existing Java plugin / view consumers of `$.perc_finder()`
  (`PercFolderPropertiesDialog`, `PercNavigationManager`, `perc_newsitedialog`,
  `PercContributorUiAdaptor`) → per-host follow-up migration. Functional in
  8.2; covered in [cutover-inventory.md](./cutover-inventory.md) §C.

