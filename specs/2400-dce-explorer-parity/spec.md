# Spec: DCE ↔ Explorer functional parity program

**Parent tracking issue:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Priority:** p1 (research + product UI backlog)  
**Related prior art:** `specs/992-react-content-explorer/` (capability matrix, cutover inventory)

## Problem

The modern SPA **Explorer** (`WebUI` `ContentExplorerShell`, route `/cm/app/explorer`) is the product replacement for the Java **Desktop Content Explorer (DCE)** (`modules/DesktopContentExplorer`). Feature 992 delivered many **components** and REST surfaces. As of 2026-08-09:

1. **Primary product shell composition has landed** (#2407 / PR #2412, #2408 / PR #2522 and follow-on chrome slices): search, menus, display formats, folder security, multi-select + clipboard, advanced wizards/viewers, and P-Trans locales+create are on the live route. **Living gap matrix:** [contracts/gap-matrix.md](./contracts/gap-matrix.md). Remaining open work is **human QA**, cross-epic Partial (object ACL), intentional **OUT** rows (e.g. P-Trans in-flight queue / content-locale session — [p-trans-out-disposition.md](./research/p-trans-out-disposition.md)), and newly dispositioned **Missing** rows for **Views (DCE navigation category)** + **Inbox** (#3102 / #3108 — [views-inbox-missing-disposition.md](./research/views-inbox-missing-disposition.md); product IN/OUT required before implement).
2. First-wave public REST gaps for Explorer (saved-search execute #2505, P-Trans create-variant #2429) have landed; do not invent new wire fields without `rest` + sitemanage companions.
3. The 992 capability matrix may still overstate some rows **Done** at the component level; operator-visible **product parity** uses this package’s gap matrix as the source of truth.

## Goals

1. **1:1 functional parity** with DCE for operator-visible capabilities wherever possible.
2. Explicit **OUT / redesign / blocked** dispositions when parity is not possible (desktop-only, obsolete).
3. Implementation via **PR-sized slices** with parent tracing on #2400.
4. New UI work uses **React/TS only** (`WebUI/src/main/ts`); new public HTTP contracts live in **`rest`** (resource + DTO + `IXxxAdaptor`) with **sitemanage apibridge** implementations.

## Non-goals

- Reimplementing DCE as a desktop app.
- Full DCE packaging uninstall (informed by this program, separate cutover).
- jQuery / FancyTree bridges for Explorer product surfaces.

## Systems

| Side | Path |
|------|------|
| DCE (reference) | `modules/DesktopContentExplorer` — menus, dialogs, wizards, clipboard, search, ACL, dependency viewer |
| Explorer (target UI) | `WebUI/src/main/ts/contentExplorer/*` + `app/routes/ExplorerRoute.tsx` |
| Public REST | `rest/src/main/java/com/percussion/rest/**` |
| Domain / apibridge | `projects/sitemanage` pathmanagement + `com.percussion.apibridge.*` |

## Acceptance (program)

- [x] Gap matrix maintained in `contracts/gap-matrix.md` with Present / Partial / Missing / OUT.
- [x] First-wave Missing / material Partial rows have children (`Parent: #2400`); phase-4 advanced chrome deferred until prioritized (see plan).
- [x] Parent #2400 progress table updated as slices open/merge (living section on issue body).
- [x] Spec + plan + matrix checked in (this package) and linked from #2400.
- [x] Product Explorer route exercises primary composed surfaces (search, menus, DF, clipboard) — PR #2412 / #2522; further surfaces follow open children.
- [ ] Vitest for logic/components; Playwright surface tests for user-visible Explorer changes (per slice).
- [ ] **i18n + 508 / a11y HARD GATE** on every UI slice — see [checklists/i18n-a11y-hard-gate.md](./checklists/i18n-a11y-hard-gate.md) and `WebUI/AGENTS.md` (Content Explorer section). No bare English chrome; `renderA11yGate` + Playwright axe gates green.

## Slice principles

1. **Compose before invent** — prefer wiring existing panels/APIs into the shell.
2. **REST first for missing data** — no invented JSON fields; extend `rest` + sitemanage when the wire shape is insufficient.
3. **Companion closure** — new REST: resource, adaptor interface, apibridge impl, Mockito resource test, Spring stub if scanned, sitemanage adaptor test as peers require.
4. **Playwright HARD GATE** for product-visible Explorer changes (`modules/perc-qa-automation`).
