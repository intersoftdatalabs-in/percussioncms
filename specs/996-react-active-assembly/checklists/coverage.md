# WP0 inventory — React Active Assembly (996)

**Issue**: #6154 · **Spec**: `specs/996-react-active-assembly/` · **Date**: 2026-11-12
**Tasks**: T003 (FR-001–006 map), T004 (FR-007/008 hidden-implementation check)

## FR-001 — Assembly window opens from Explorer Active Assembly actions

| Layer | File |
|---|---|
| Dispatch | `WebUI/src/main/ts/contentExplorer/actionDispatch.ts` (`AA_PARENT_ACTIONS`: `item_activeassembly`, `enterpriseitem_activeassembly`, `corporateitem_activeassembly`, `item_assembly` → preview-first host) |
| URL/window | `WebUI/src/main/ts/assembly/assemblyHostUrl.ts` (`buildAssemblyHostUrl`, `percAssembly_{contentId}`), `WebUI/src/main/ts/app/routes/AssemblyRoute.tsx` |
| Host | `WebUI/src/main/ts/assembly/AssemblyHost.tsx` (chrome-less, no BrandBar/TopNav) |

Tests: `modules/perc-qa-automation/frontend/tests/explorer-active-assembly.spec.js`
("chrome-less and does not request Data Flow AA HTML",
"Explorer Active Assembly does not navigate leftover AA HTML");
`WebUI/src/test/ts/assembly/AssemblyHost.test.tsx`, `assemblyHostUrl.test.ts`.
**Status: shipped — verify (WP1).**

## FR-002 — Template resolve via `GET /actions/find/templates/{id}?isAA=true`

| Layer | File |
|---|---|
| Client | `AssemblyHost.loadAssemblyTemplates` → `findAllowedTemplateMenus(contentId, true)` (`WebUI/src/main/ts/api/contentExplorer/actionMenuApi.ts`), AA-first with non-AA fallback |
| REST | `rest/src/main/java/com/percussion/rest/actions/ActionMenuResource.java` (`@Path("/find/templates/{id}")`, `isAA` query param → `menuAdaptor.findAllowedTemplates`) |

Tests: Playwright "unmatched templateId shows an error instead of silently using
another template" + this-issue T009 test "denied template resolve (HTTP 403)
surfaces an error and never renders a preview" (`@aa-denied`).
**Status: shipped — verify (WP1).**

## FR-003 — Preview via `preview-location` + `/assembler/render` iframe + light overlay

| Layer | File |
|---|---|
| Client | `fetchPreviewLocation` (`WebUI/src/main/ts/api/contentExplorer/assemblyApi.ts` → `PATHS.ASSEMBLY_PREVIEW_LOCATION`), `resolveAssemblerHref` (rejects non-`/assembler/render` hrefs → `PREVIEW_FAILED`) |
| REST | `rest/src/main/java/com/percussion/rest/assembly/AssemblyResource.java` + `IAssemblyAdaptor.java` + `PreviewLocation.java` |
| Bridge | `projects/sitemanage/src/main/java/com/percussion/apibridge/AssemblyAdaptor.java` |

Tests: Playwright preview-host + overlay-chrome tests in `explorer-active-assembly.spec.js`.
**Status: shipped — verify (WP1).**

## FR-004 — Slot add/create/arrange via canvas relationship REST

| Layer | File |
|---|---|
| Client | `WebUI/src/main/ts/assembly/slotContext.ts`, `SlotCreateDialog.tsx`, `SlotChangeDialog.tsx`, `SlotDependentPick`/`slotCreatePick`/`slotTemplateSlotPick`, `WebUI/src/main/ts/api/contentExplorer/slotRelationshipApi.ts` |
| REST | `rest/src/main/java/com/percussion/rest/slotrelationships/SlotRelationshipResource.java` (+ `ISlotRelationshipAdaptor`, `SlotCanvas`) |
| Bridge | `projects/sitemanage/src/main/java/com/percussion/apibridge/SlotRelationshipAdaptor.java` |

Tests: Vitest `AssemblyHost.slotAdd.test.tsx`, `AssemblyHost.slotCreate.test.tsx`,
`slotContext.test.ts`; Playwright `explorer-create-item-in-slot.spec.js` (#5267),
`explorer-move-relationship-slot.spec.js`, `explorer-link-existing-slot.spec.js`,
`explorer-add-relationship.spec.js`, plus slot arrange/create tests in
`explorer-active-assembly.spec.js`.
**Status: shipped — verify (WP1).**

## FR-005 — Scalar overlay persist via itemmanagement (closed #3475)

| Layer | File |
|---|---|
| Client | `WebUI/src/main/ts/assembly/overlayFields.ts` (`scalarOverlayFields`: text/long-text/HTML/link/number/decimal/date/datetime/keyword/yes-no; NUL + required guards), `saveItemEditorFields` path in `AssemblyHost.tsx` (HTTP 400/403/409 do not claim success) |

Tests: Vitest `WebUI/src/test/ts/assembly/overlayFields.test.ts` (~2300 lines),
`AssemblyHost.test.tsx` `it.each([400, 403, 409])` no-false-success cases;
Playwright `assembly-text-field.spec.js` ("overlay edits known assembled text
fields through itemmanagement") + `assembly-{date,datetime,decimal,html,keyword,link,long-text,number,required-*,yesno}-field.spec.js` family.
**Status: shipped — verify (WP1).**

## FR-006 — Denied/failed ops message; no silent loss

Implementation: `AssemblyHost` `errorKey` → `[data-testid="assembly-error"]`
(`role="alert"`); slot-dialog failures → `assembly-slot-notice` (`role="status"`)
via `runSlotDialogWork`; template mismatch / resolve-throw / preview-throw all set
an error key and clear `previewHref`.

Tests: Vitest 400/403/409 suites; Playwright unmatched-template + denied-403
(`@aa-denied`, added by #6154) tests.
**Status: shipped — verify (WP1).**

## T004 — FR-007/FR-008 hidden-implementation check (2026-11-12)

- **FR-007** (`updaterelateditems.html` parity): zero hits for `updaterelateditems`
  in `WebUI/src/main/ts`, `rest/src`, `projects/sitemanage/src`. No hidden
  implementation. Scope stays with **#6104** (OPEN, p3); T011 is not ticked here.
- **FR-008** (Compare / `sys_Compare`): only WebUI hit is the legacy blocklist
  entry `sys_compare` in `DATA_FLOW_PATH_MARKERS`
  (`WebUI/src/main/ts/contentExplorer/actionDispatch.ts`) — i.e. explicitly
  *not* routed to React. Backend `compareRevisions` (`PSItemService` /
  `IPSItemService`, `PSItemServiceCompareRevisionsTest`) serves the **revision
  compare** surface (`editor-host-revision-compare.spec.js`,
  `explorer-revision-compare.spec.js`), not assembly Compare. No hidden assembly
  Compare implementation. Decision: **defer** — recorded in `checklists/closeout.md` (T012).
