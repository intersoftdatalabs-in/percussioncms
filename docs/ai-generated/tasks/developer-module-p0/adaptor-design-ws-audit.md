# Developer REST adaptors — design webservices audit

|   Field    |                                               Value                                               |
|------------|---------------------------------------------------------------------------------------------------|
| **Date**   | 2026-07-30                                                                                        |
| **Parent** | [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690)                        |
| **Goal**   | Rock-solid path: **REST → thin sitemanage adaptor → same design WS SOAP/Workbench used** → system |

## Target architecture

```text
Workbench (classic):
  SOAP *DesignSOAPImpl  →  IPS*DesignWs (system/webservices)  →  system services / objectstore

Developer SPA (target):
  REST *Resource (rest module)
    → I*Adaptor (rest)
    → *Adaptor (@PSSiteManageBean in sitemanage)   // host only — not a second domain
    → IPS*DesignWs / same methods SOAP used
    → system
```

**Sitemanage** only hosts the Spring adaptor bean. It must not become a parallel design stack.

## Design WS surfaces (SOAP-era)

|                Interface                | Package  |              Typical SOAP entry               |
|-----------------------------------------|----------|-----------------------------------------------|
| `IPSContentDesignWs`                    | content  | `ContentDesignSOAPImpl`                       |
| `IPSUiDesignWs`                         | ui       | `UiDesignSOAPImpl`                            |
| `IPSSystemDesignWs`                     | system   | `SystemDesignSOAPImpl`                        |
| `IPSSecurityDesignWs`                   | security | `SecurityDesignSOAPImpl`                      |
| `IPSAssemblyDesignWs` / `IPSAssemblyWs` | assembly | `AssemblyDesignSOAPImpl` / `AssemblySOAPImpl` |

Key content-design methods that Workbench-style catalogs use:

- Keywords: `findKeywords` / `loadKeywords` / `saveKeywords` / `deleteKeywords`
- Locales: `findLocales` / `loadLocales` / …
- Content types: `findContentTypes` / `loadContentTypes` / …
- System def: `loadContentEditorSystemDef` / `saveContentEditorSystemDef`
- Shared def: `loadContentEditorSharedDef` / `saveContentEditorSharedDef`
- UI: actions, display formats, searches, views (`IPSUiDesignWs`)
- Assembly: slots/templates via assembly design/runtime WS

---

## Audit matrix (Developer-relevant)

Legend:

|   Rating   |                                             Meaning                                             |
|------------|-------------------------------------------------------------------------------------------------|
| **OK**     | Primary list/load goes through design (or publishing/runtime WS that SOAP used for that object) |
| **BYPASS** | Uses managers/services/PSServer directly; **design WS exists** for the same object              |
| **ALT**    | No design-WS method; uses system managers that are the only practical API (document)            |
| **MIX**    | Design WS for some ops, direct services for others                                              |

### Content design (CD / UI / SY catalogs)

|     Developer surface     |  REST path (approx)  |          Adaptor          |                      Primary backing today                       |                   Design WS available?                   |   Rating   |                                                                                Notes / action                                                                                 |
|---------------------------|----------------------|---------------------------|------------------------------------------------------------------|----------------------------------------------------------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Content types list/detail | `/contenttypes`      | `ContentTypeAdaptor`      | **`IPSContentDesignWs`** `findContentTypes` / `loadContentTypes` | Yes                                                      | **OK**     | Write path also design WS. Associations use assembly/workflow services (acceptable MIX for CT↔template/wf). Empty SPA rows were **Jackson Optional** (#1696), not missing WS. |
| Keywords                  | `/keywords`          | `KeywordsAdaptor`         | **`IPSContentService`** `findKeywordsByLabel`                    | Yes — `IPSContentDesignWs.findKeywords` / `loadKeywords` | **BYPASS** | **Retarget** list/get/CUD to content **design** WS (Workbench path).                                                                                                          |
| Locales                   | `/locales`           | `LocalesAdaptor`          | **`IPSCmsObjectMgr`** (legacy Hibernate)                         | Yes — `IPSContentDesignWs.findLocales` / `loadLocales`   | **BYPASS** | **Retarget** to content design WS.                                                                                                                                            |
| System def fields         | `/systemdef`         | `SystemDefAdaptor`        | **`PSServer.getContentEditorSystemDef()`**                       | Yes — `loadContentEditorSystemDef`                       | **BYPASS** | **Retarget** load to `IPSContentDesignWs`.                                                                                                                                    |
| Shared field groups       | `/sharedfields`      | `SharedFieldsAdaptor`     | **`PSServer.getContentEditorSharedDef()`**                       | Yes — `loadContentEditorSharedDef`                       | **BYPASS** | **Retarget** load to `IPSContentDesignWs`.                                                                                                                                    |
| CE controls               | `/cecontrols`        | `ControlAdaptor`          | `PSSystemControlManager` / `PSCustomControlManager`              | No dedicated design WS found                             | **ALT**    | Document as control-manager path; no SOAP design twin.                                                                                                                        |
| Searches                  | `/searches`          | `SearchAdaptor`           | **`IPSUiDesignWs`** find/load searches                           | Yes                                                      | **OK**     |                                                                                                                                                                               |
| Views                     | `/views`             | `ViewAdaptor`             | **`IPSUiDesignWs`** find/load views                              | Yes                                                      | **OK**     |                                                                                                                                                                               |
| Display formats           | `/displayformats`    | `DisplayFormatAdaptor`    | **`IPSUiDesignWs`**                                              | Yes                                                      | **OK**     |                                                                                                                                                                               |
| Action menus              | `/actions` (etc.)    | `ActionMenuAdaptor`       | **`IPSUiDesignWs`** (+ menu helpers)                             | Yes                                                      | **MIX**    | Prefer design WS for load/save; helpers OK for CT/template menu expand.                                                                                                       |
| Relationship types        | `/relationshiptypes` | `RelationshipTypeAdaptor` | **`IPSSystemDesignWs`**                                          | Yes                                                      | **OK**     |                                                                                                                                                                               |
| Extensions                | `/extensions`        | `ExtensionAdaptor`        | **`IPSExtensionService`**                                        | No design WS twin                                        | **ALT**    | Extension registry is system service path.                                                                                                                                    |
| Server configs            | `/serverconfigs`     | `ServerConfigAdaptor`     | **`IPSSystemService`** config types                              | No design WS twin                                        | **ALT**    | Runtime server config files.                                                                                                                                                  |
| Slots                     | `/slots`             | `SlotsAdaptor`            | **`IPSAssemblyService.findSlotsByName`**                         | Yes — assembly WS `loadSlots` / `IPSAssemblyDesignWs`    | **BYPASS** | **Retarget** list/load (and writes) to assembly design/runtime WS used by SOAP.                                                                                               |
| Templates                 | `/templates`         | `TemplateAdaptor`         | **`IPSContentWs`** + **`IPSAssemblyService`**                    | Assembly design WS + content WS                          | **MIX**    | Prefer assembly design WS for template design ops where SOAP did; document content WS usage.                                                                                  |
| Sites                     | `/sites`             | `SitesAdaptor`            | **`IPSPublishingWs`**                                            | Publishing (not “content design”)                        | **OK**     | Correct for site catalog.                                                                                                                                                     |
| Pipelines / apps          | `/pipelines`         | `PipelinesAdaptor`        | **`PSServerXmlObjectStore`**                                     | No thin design WS                                        | **ALT**    | Objectstore server path; document as XML-app runtime.                                                                                                                         |
| Communities               | communities          | `CommunityAdaptor`        | **`IPSSecurityDesignWs`** + system                               | Yes                                                      | **OK**     |                                                                                                                                                                               |
| ACL                       | `/acls`              | `AclAdaptor`              | **`IPSAclService`**                                              | Security design may wrap ACL                             | **MIX**    | Prefer security design WS if SOAP ACL design went there; else document service as canonical.                                                                                  |

### Non-Developer (context only)

Folder/page/asset adaptors use sitemanage product services heavily — **expected** for CMS content UX, not Workbench design tools.

---

## Relation to QA automation failures (2026-07-30)

Failures against `C:\Installs\8.2-july-29` were **mostly not** “wrong service layer” at runtime:

|                Observed failure                 |                          Primary cause                          |              Related to design-WS bypass?               |
|-------------------------------------------------|-----------------------------------------------------------------|---------------------------------------------------------|
| Explorer tree empty / `folder//` 400            | WebUI **not redeployed** (#1680 encodePath)                     | No                                                      |
| Catalog endpoints 500 wrapping **404 NotFound** | Install **rest/sitemanage jars lag** `development`              | No (missing resource registration)                      |
| Content types JSON = `{hideFromMenu}` only      | **Jackson** missing `Jdk8Module` for `Optional` getters (#1696) | No — adaptor already called `IPSContentDesignWs`        |
| Keywords/slots/locales 500 on install           | Same install lag **or** bean missing                            | Bypass is separate **correctness** debt; redeploy first |

**Conclusion:** QA reds were dominated by **deploy lag + JSON façade**, not by Keywords going through `IPSContentService` instead of design WS.  
**However**, for long-term rock-solid design tools, **BYPASS** rows should still be retargeted so REST matches Workbench SOAP call sites (locking, ACL, dependencies, delete rules).

---

## Priority retarget list (minimize design bugs)

1. **KeywordsAdaptor** → `IPSContentDesignWs` find/load/(save/delete)
2. **LocalesAdaptor** → `IPSContentDesignWs` find/load/(save)
3. **SystemDefAdaptor** → `loadContentEditorSystemDef`
4. **SharedFieldsAdaptor** → `loadContentEditorSharedDef`
5. **SlotsAdaptor** → assembly design/runtime WS (`loadSlots` / design create-save), not raw assembly service only
6. **TemplateAdaptor** — document and tighten toward assembly design WS for design mutations

Leave as **ALT** with docs: controls managers, extensions registry, server configs, pipelines objectstore.

---

## Acceptance for “rock solid”

For each **BYPASS** item when fixed:

- [ ] Adaptor injects/locates design WS (same locator SOAP uses)
- [ ] List/detail use find/load* design methods
- [ ] Mutations use save/delete design methods with session/user where required
- [ ] Unit tests with mocked design WS (not only Hibernate/service mocks)
- [ ] Playwright/catalog smoke green after redeploy
- [ ] PR notes “Workbench parity: SOAP X → IPS*DesignWs.Y”

---

## Related PRs / issues

|            Item            |     Link      |
|----------------------------|---------------|
| Post-P0 tracker            | #1690         |
| Redeploy QA install        | #1692         |
| CT empty rows (Jackson)    | #1693 / #1696 |
| Catalog 500/404 on install | #1694         |
| Explorer encodePath verify | #1695         |
| Playwright harness         | #1691         |

