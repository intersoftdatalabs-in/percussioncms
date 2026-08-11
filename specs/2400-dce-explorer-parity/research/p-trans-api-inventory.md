# P-Trans API inventory (content item translation)

**Parent epic:** [#2411](https://github.com/intersoftdatalabs-in/percussioncms/issues/2411) (slice of [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400))  
**Child slices:** [#2428](https://github.com/intersoftdatalabs-in/percussioncms/issues/2428) (this inventory/disposition), [#2429](https://github.com/intersoftdatalabs-in/percussioncms/issues/2429) (REST façade if needed), [#2430](https://github.com/intersoftdatalabs-in/percussioncms/issues/2430) (Explorer UI)  
**Matrix source:** `specs/992-react-content-explorer/contracts/capability-matrix.md` → **Translation (P-Trans)**  
**Gap matrix row:** `specs/2400-dce-explorer-parity/contracts/gap-matrix.md` → Translation workflow  
**Inventory date:** 2026-08-08  
**OUT disposition residual:** [#2829](https://github.com/intersoftdatalabs-in/percussioncms/issues/2829) · [p-trans-out-disposition.md](./p-trans-out-disposition.md)  
**Status:** Inventory complete. Locales + create-variant **Present**. In-flight queue + content-locale session **OUT** (signed 2026-08-10 — see OUT note).

## Scope of this note

Map **existing public REST / related surfaces** and **DCE / legacy create paths** against each P-Trans capability.  
This is **not** TMX / UI chrome i18n (Explorer string catalog). Content-item **locale variants** and **translation relationships** are in scope.

## Legend

| Disposition | Meaning |
|-------------|---------|
| **Present** | Public REST (or product Explorer path) already supports the operator outcome |
| **Partial** | Related surface exists but does not complete the P-Trans acceptance |
| **Missing** | No public contract / no Explorer product path |
| **Legacy-only** | Exists outside modern `rest` (SOAP, CX XML app, extension, DCE) — not SPA-callable without façade |
| **OUT** | Explicit product non-goal (requires human product sign-off; in-flight + session **signed OUT** 2026-08-10) |

---

## P-Trans row matrix

| P-Trans capability | Acceptance (992 matrix) | Public REST / Explorer today | DCE / legacy evidence | Disposition | Follow-up |
|--------------------|-------------------------|------------------------------|------------------------|-------------|-----------|
| Show item locales (current + available) | Item shows current locale + available locale list | **Present:** `GET /rest/content-explorer/translations/{itemId}` (#2429 / PR #2601) + Explorer `TranslationsPanel` (#2430). Catalog still via `GET /rest/locales` / services locales for create targets. | DCE locale cataloger `PSLocaleCataloger`; item locale on content status | **Present** (REST + Explorer UI) | Human QA on #2430 surface |
| Translate (create new locale variant) | Authorized user creates a new locale for an item | **Present:** `POST /rest/content-explorer/translations` (NewTranslations domain path, PR #2601) + Explorer create form in `TranslationsPanel` (#2430). | SOAP `content.NewTranslations`; CX `sys_CreateTranslations`; DCE `ACTION_PASTE_NEW_TRNSL` | **Present** (public REST + Explorer UI) | Human QA create-variant on live content |
| In-flight translation status | Filter/list `translationState=inFlight` (or equivalent) | **Not exposed** by REST façade; Explorer shows OUT note only | CE “translation queue” / relationship + workflow state in legacy CE | **OUT** (signed #2829 / [p-trans-out-disposition.md](./p-trans-out-disposition.md)) | Reopen only with product sign-off + PR-sized child |
| Switch source/target locale session context | Selecting locale re-issues path APIs under that content locale | Community switch exists (`/communities/switch/{name}`). UI TMX locale loading is **chrome** strings, not content locale session. Per-item locale + create-variant cover Explorer. | DCE login locale + change-locale header flows | **OUT** (signed #2829 / [p-trans-out-disposition.md](./p-trans-out-disposition.md)) | Reopen only as redesign with multi-module contract |

---

## Public REST surfaces inspected (evidence)

### Present (related, incomplete for P-Trans)

| Path / type | Module | Role vs P-Trans |
|-------------|--------|-----------------|
| `GET /rest/locales` | `rest` `LocalesResource` | CMS **locale catalog** (language string, label, status, base flag). Design doc states create/edit/delete and auto-translation settings are **later slices** / unsupported. |
| `GET /rest/locales/{idOrLang}` | same | Locale detail + optional RXLOCALEFORMAT row. Write unsupported. |
| `ILocalesAdaptor` / `LocalesAdaptor` | `rest` + `projects/sitemanage` apibridge | Read-only via content design WS `findLocales` / `loadLocales`. Explicit design gaps: no locale CRUD, no auto-translation config. |
| `Folder.locale` | `rest` folders | Folder **default** locale property — not item translation variants. |
| `GET /rest/content-explorer/relationships/{itemId}/outgoing` (and incoming/summary/…) | `rest` `RelationshipSummaryResource` | **Counts** including translation relationship category; not create, not per-locale variant DTO. |
| `GET /rest/actions/...` | `rest` `ActionMenuResource` | Server-driven menus; may expose translate **if** configured — not a typed create-variant contract. |
| `POST /rest/i18n/corrections` | `rest` `I18nCorrectionsResource` | Crowd-sourced **UI string** corrections — **not** content-item translation. |

### Shipped under public `rest` (after #2429)

| Needed for P-Trans | Result |
|--------------------|--------|
| Create locale variant / NewTranslations | **Present:** `POST /rest/content-explorer/translations` |
| List translation variants for content id | **Present:** `GET /rest/content-explorer/translations/{itemId}` |

### Intentional OUT (no public rest by design)

| Needed for P-Trans | Result |
|--------------------|--------|
| `translationState=inFlight` (or equivalent filter) | **OUT** — no public query parameter / resource; see [p-trans-out-disposition.md](./p-trans-out-disposition.md) |
| Content-locale session switch for path APIs | **OUT** — no peer to community switch for content locale |

### ObjectTypeEnum markers (design vocabulary only)

`rest` `ObjectTypeEnum` includes `AUTO_TRANSLATIONS`, `LOCALE`, `FOLDER_TRANSLATIONS` — vocabulary for object types, **not** an implemented P-Trans operator API.

---

## DCE / legacy create & catalog paths (evidence)

| Surface | Location | Notes |
|---------|----------|-------|
| Paste as new translation | `modules/DesktopContentExplorer/.../PSActionManager.java` `ACTION_PASTE_NEW_TRNSL` | Enabled only when selection is **items** and target is **folder** |
| Locale cataloger | `PSLocaleCataloger` | Loads locales via `../sys_i18nSupport/languagelookup.xml` |
| CX create translations extension | `modules/extensions-main` `sys_CreateTranslations` → `com.percussion.extensions.cx.PSCreateTranslations` | Result-document processor used by CE actions |
| Workflow create translations | `sys_createTranslations` → `com.percussion.workflow.PSCreateTranslations` | Config under `rxconfig/I18n/sys_createTranslations.properties` |
| SOAP NewTranslations | `modules/webservices` content.wsdl; tests in `system/webservices/test/.../ContentTestCase` | Proven create-variant contract for Workbench/SOAP clients |
| Translate action XML app | `system/cms/content/applications/sys_actionTranslate/` | Legacy CE translate application (CONTENTSTATUS + relationship tables) |

---

## Recommendation (implementation order) — historical + current

1. **#2428 (this note)** — **Done** (inventory). Living disposition updated by #2829 OUT sign-off.
2. **#2429 REST façade** — **Done** ([PR #2601](https://github.com/intersoftdatalabs-in/percussioncms/pull/2601)): create-variant + item-locale list. In-flight not included (OUT).
3. **#2430 Explorer UI** — **Done** ([PR #2648](https://github.com/intersoftdatalabs-in/percussioncms/pull/2648)): locales list + create; Vitest + Playwright. Human QA **#2649**.
4. **#2829 OUT residual** — **Done** (docs): formal OUT for in-flight + session; no further agent implement without product re-open.

### Do not

- Call SOAP from the SPA as a long-term product path.
- Treat `/rest/i18n/corrections` or TMX loaders as content translation.
- Claim P-Trans **Present** from relationship **count** endpoints alone.
- Implement in-flight queue or content-locale session without product re-open ([p-trans-out-disposition.md](./p-trans-out-disposition.md)).

---

## Residual / product questions (answered 2026-08-10)

| # | Question | Answer |
|---|----------|--------|
| 1 | Is **in-flight translation queue** still a 8.2 operator requirement? | **OUT** for 8.2 Explorer parity. Reopen only with product sign-off + typed contract. |
| 2 | Is **session content-locale context** required? | **OUT**. Per-item locale + explicit create-variant is enough for Explorer. |
| 3 | Dedicated REST resource vs action execute for create-variant? | **Dedicated REST** shipped: `GET\|POST /rest/content-explorer/translations` (#2429). |

---

## Change log

| Date | Note |
|------|------|
| 2026-08-08 | Initial overnight inventory for #2411 split; children #2428–#2430 filed. |
| 2026-08-09 | #2429 REST merged (PR #2601). #2430 Explorer UI: item locales + create-variant **Present**; in-flight + session content-locale remain **OUT**. |
| 2026-08-10 | #2829: formal OUT sign-off for in-flight + session; residual questions answered; link [p-trans-out-disposition.md](./p-trans-out-disposition.md). |
