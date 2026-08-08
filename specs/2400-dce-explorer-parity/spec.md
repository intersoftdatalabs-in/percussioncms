# Spec: DCE ↔ Explorer functional parity program

**Parent tracking issue:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Priority:** p1 (research + product UI backlog)  
**Related prior art:** `specs/992-react-content-explorer/` (capability matrix, cutover inventory)

## Problem

The modern SPA **Explorer** (`WebUI` `ContentExplorerShell`, route `/cm/app/explorer`) is the product replacement for the Java **Desktop Content Explorer (DCE)** (`modules/DesktopContentExplorer`). Feature 992 delivered many **components** and REST surfaces, but:

1. The **product shell** still composes only tree + detail list + reduced actions — Search, server action menus, display formats, folder security, clipboard, and advanced tools are largely **not wired into the live route**.
2. Several DCE operator workflows still lack **public REST** in `rest` (or path enrichment) that the SPA can call without inventing fields.
3. The 992 capability matrix claims many rows **Done** at the component level; operator-visible **product parity** is incomplete.

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

- [ ] Gap matrix maintained in `contracts/gap-matrix.md` with Present / Partial / Missing / OUT.
- [ ] Every Missing / material Partial row has a child GitHub issue (`Parent: #2400`).
- [ ] Parent #2400 progress table updated as slices open/merge.
- [ ] Spec + plan + matrix checked in (this package) and linked from #2400.
- [ ] Product Explorer route exercises composed surfaces (not only isolated registry components).
- [ ] Vitest for logic/components; Playwright surface tests for user-visible Explorer changes.
- [ ] **i18n + 508 / a11y HARD GATE** on every UI slice — see [checklists/i18n-a11y-hard-gate.md](./checklists/i18n-a11y-hard-gate.md) and `WebUI/AGENTS.md` (Content Explorer section). No bare English chrome; `renderA11yGate` + Playwright axe gates green.

## Slice principles

1. **Compose before invent** — prefer wiring existing panels/APIs into the shell.
2. **REST first for missing data** — no invented JSON fields; extend `rest` + sitemanage when the wire shape is insufficient.
3. **Companion closure** — new REST: resource, adaptor interface, apibridge impl, Mockito resource test, Spring stub if scanned, sitemanage adaptor test as peers require.
4. **Playwright HARD GATE** for product-visible Explorer changes (`modules/perc-qa-automation`).
