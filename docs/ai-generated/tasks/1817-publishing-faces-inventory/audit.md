# #1817 — Publishing / pubruntime faces-config + consumer audit

**Date**: 2026-08-04  
**Parent**: [#1372](https://github.com/intersoftdatalabs-in/percussioncms/issues/1372) (RET-06)  
**Slice**: Child A / issue [#1817](https://github.com/intersoftdatalabs-in/percussioncms/issues/1817)  
**Canonical checklist**: [`specs/990-unified-publishing-ui/checklists/removal-inventory.md`](../../../../specs/990-unified-publishing-ui/checklists/removal-inventory.md)  
**Gate**: Inventory only — **no** JSP / faces / packaging deletes in this slice.

## Summary

|                                      Question                                      |                                                Result                                                |
|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| Product-nav callers of Design/Runtime **deep** faces pages?                        | **Zero**                                                                                             |
| Entry redirects already modern?                                                    | **Yes** (`ui/publishing/index.jsp`, `ui/pubruntime/index.jsp`, DCE header, SPA `publish`)            |
| `publishing-faces-config.xml` still in source?                                     | **No** — removed in PR #1337 (`aa46aa5f86`)                                                          |
| JSF managed-bean classes (`PSDesignNavigation` / `PSRuntimeNavigation`) in source? | **No**                                                                                               |
| Installer still cleans faces-config on upgrade?                                    | **Yes** (`install.xml` + `ObsoleteWebInfArtifactsCleanupTest`)                                       |
| Non-nav residual consumers that block blind deep-page delete?                      | **Yes** — DemandPublish servlet forward, Publish_Now seed URL, `PSRunEdition` JobPubLog `.faces` URL |

## Method

1. Listed tracked JSPs under `WebUI/src/main/webapp/ui/publishing/**` and `ui/pubruntime/**`.
2. Grepped product code for `ui/publishing`, `ui/pubruntime`, `.faces`, DemandPublish, JobPubLog, faces-config paths.
3. Recovered last checked-in `publishing-faces-config.xml` via `git show aa46aa5f86^:WebUI/src/main/webapp/WEB-INF/publishing-faces-config.xml`.
4. Mapped deepLinkMap, web.xml (active + commented), distribution installer peers.
5. Recorded deletion checklists for Child B (#1819) and Child C (#1818) in the removal inventory.

## Product navigation evidence

|       Surface       |                       File / symbol                       |                                                 Evidence                                                  |
|---------------------|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| DCE header          | `WebUI/src/main/webapp/dce_header.jsp`                    | Links to `/cm/app/?view=publish&section=design` and `section=runtime` only                                |
| SPA entry           | `WebUI/src/main/webapp/cm/app/index.jsp`                  | `spaViews` includes `"publish"`; no legacy deep faces map for Design/Runtime                              |
| Classic publish JSP | `cm/app/publish.jsp` (+ pages dual tree)                  | 301 redirect to modern shell; preserves query                                                             |
| Design entry        | `ui/publishing/index.jsp`                                 | 301 → modern Design                                                                                       |
| Runtime entry       | `ui/pubruntime/index.jsp`                                 | 301 → modern Runtime                                                                                      |
| Deep-link allowlist | `WebUI/src/main/ts/publishing/deepLinkMap.ts`             | `mapClassicPublishingPath` maps classic path fragments to modern sections; tests in `deepLinkMap.test.ts` |
| Contract            | `specs/990-unified-publishing-ui/contracts/deep-links.md` | Classic `/ui/publishing/*` and `/ui/pubruntime/*` → modern sections                                       |

## Faces-config path catalogue

|                            Path                             |                                                     Status on `main`                                                      |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/webapp/WEB-INF/publishing-faces-config.xml` | Deleted (PR #1337)                                                                                                        |
| `WebUI/src/main/webapp/cm/WEB-INF/web.xml`                  | `jakarta.faces.CONFIG_FILES` (listing publishing + admin + user faces) is inside a **commented** MyFaces workaround block |
| `WebUI/src/main/webapp/WEB-INF/web.xml`                     | No active faces CONFIG_FILES; DemandPublishingServlet mapped                                                              |
| `modules/perc-distribution-tree/.../install.xml`            | Deletes installed `…/WEB-INF/publishing-faces-config.xml` on upgrade                                                      |
| Historical docs (`system/ear/WEB-INF/…`)                    | Stale narrative only                                                                                                      |

### Historical managed beans (deleted with faces stack)

|             Bean name              |                              Class                               |  Scope  |
|------------------------------------|------------------------------------------------------------------|---------|
| `sys_design_navigation`            | `com.percussion.rx.publisher.jsf.beans.PSDesignNavigation`       | session |
| `sys_runtime_navigation`           | `com.percussion.rx.publisher.jsf.beans.PSRuntimeNavigation`      | session |
| `sys_sitelist`                     | `com.percussion.services.publisher.ui.PSSiteListSelect`          | session |
| `sys_design_name_value_provider`   | `com.percussion.rx.jsf.PSCategoryNodesNameProvider`              | session |
| `sys_design_unique_name_validator` | `com.percussion.services.utils.jsf.validators.PSUniqueValidator` | session |
| `sys_path_validator`               | `com.percussion.services.utils.jsf.validators.PSPathExists`      | session |
| converter `sys_normalize_path`     | `com.percussion.services.utils.jsf.PSNormalizePath`              | —       |

Outcome → JSP table: see removal inventory section **Historical faces outcomes → JSP map**.

## Grep consumer audit (non-nav residuals)

|                        Location                        |                           Match                            |                    Classification                     |
|--------------------------------------------------------|------------------------------------------------------------|-------------------------------------------------------|
| `PSDemandPublishServlet.java`                          | `getRequestDispatcher("/ui/pubruntime/DemandPublish.jsp")` | **Required rewire** before deleting DemandPublish.jsp |
| web.xml (×3 trees)                                     | `/publisher/demandpublishing` → DemandPublishingServlet    | Keep mapping                                          |
| `PSRunEdition.java`                                    | builds `/ui/pubruntime/JobPubLog.faces?…`                  | **Required rewire** (already dead without JSF)        |
| `cmsTableData.xml` / `RxffTableData.xml` / FastForward | `../ui/publishing/publish.jsp` Publish_Now                 | **Required rewire** or KEEP publish.jsp               |
| `dce_header.jsp`                                       | modern section links only                                  | Nav OK                                                |
| Deep JSPs themselves                                   | mutual includes of auth / Trinidad tags                    | Dead without JSF runtime; packaged residual only      |
| Modern TS under `src/main/ts/publishing`               | design/runtime React sections                              | Replacement product UI — retain                       |

## File counts (tracked)

- Design: **28** JSPs under `WebUI/src/main/webapp/ui/publishing/`
- Runtime: **13** JSPs under `WebUI/src/main/webapp/ui/pubruntime/`

Matches parent #1372 inventory (2026-07-19).

## Deletion readiness for Child B / C

|           Page set            | Product-nav clear? |                                           Other blockers                                            |
|-------------------------------|--------------------|-----------------------------------------------------------------------------------------------------|
| Design exclusive faces pages  | Yes                | KEEP `index.jsp`; KEEP or rewire `publish.jsp` (seed Publish_Now)                                   |
| Runtime exclusive faces pages | Yes                | KEEP `index.jsp`; KEEP or rewire `DemandPublish.jsp`; rewire `PSRunEdition` before JobPubLog delete |
| faces-config entries          | N/A (already gone) | Installer cleanup must remain for upgrades                                                          |

Full checklists live in `removal-inventory.md` (Child B / Child C sections).

## Out of scope (confirmed not done here)

- Deleting any JSP under `ui/publishing` or `ui/pubruntime`
- Editing faces-config (absent) or packaging install.xml deletes
- UAT SC-001/003/008 (#1371)
- Modern Publish shell / REST publish API changes

