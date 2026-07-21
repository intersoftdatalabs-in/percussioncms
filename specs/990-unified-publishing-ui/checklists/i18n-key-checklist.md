# i18n Key Checklist: Unified Publishing UI

**Feature**: `990-unified-publishing-ui`  
**Purpose**: SC-007 / FR-012 — user-visible chrome uses TMX catalogs (reuse `perc.ui.publish.*` where possible).  
**Status**: Seed — fill during implementation.

## Process

1. Prefer existing keys from Minuet publish templates (`perc.ui.publish.title@…`, `perc.ui.publish.view@…`, etc.).  
2. For Design/Runtime chrome, add keys under `perc.ui.publish.design@…` / `perc.ui.publish.runtime@…` (or consistent neighboring prefix).  
3. New keys: structural locale parity with neighboring `perc.ui` units (en-us + peer langs; non-English may temporarily equal en-us).  
4. Runtime: shell includes `tmx.jsp`; React resolves via `I18N.message` / TS wrapper.  
5. PR review: mark rows **Done** when UI uses keys (not hardcoded English).

## Reuse candidates (from Minuet)

| Key pattern | Used for |
|-------------|----------|
| `perc.ui.publish.title@*` | Column headers, actions Stop/Card/List |
| `perc.ui.publish.view@*` | Servers, Production, Staging, Add server |
| `perc.ui.publish.reports@*` | Back, reports chrome |
| `perc.ui.publish.incrementalPreview@*` | Incremental empty/queue |
| `perc.ui.navMenu.publish@Publish` | Nav label |
| `perc.ui.page.dialog@*` | Delete logs confirmation |

## Net-new keys (track here)

| Key | en-us | Peer langs | UI location | Status |
|-----|-------|------------|-------------|--------|
| `perc.ui.publish.modern@Sites & Servers` | Sites & Servers | es, hi | Shell nav | Done |
| `perc.ui.publish.modern@Logs` | Logs | es, hi | Shell nav | Done |
| `perc.ui.publish.modern@Design` | Design | es, hi | Shell nav | Done |
| `perc.ui.publish.modern@Runtime` | Runtime | es, hi | Shell nav | Done |
| `perc.ui.publish.modern@Incremental` | Incremental | es, hi | Site workspace | Done |
| `perc.ui.publish.modern@Back` | Back | es, hi | Site workspace | Done |
| `perc.ui.publish.modern@No Sites` | empty sites | es, hi | Sites section | Done |
| `perc.ui.publish.modern@No Servers` | empty servers | es, hi | Site workspace | Done |
| `perc.ui.publish.modern@No Active Jobs` | empty jobs | es, hi | Status | Done |
| `perc.ui.publish.modern@No Logs` | empty logs | es, hi | Logs | Done |
| `perc.ui.publish.modern@Publish Forbidden` | AuthZ | es, hi | Publish actions | Done |
| `perc.ui.publish.modern@Bad Server Configuration` | BADCONFIG | es, hi | Publish actions | Done |
| `perc.ui.publish.modern@Section Coming Soon` | placeholders | es, hi | Design/Runtime | Done |
| `perc.ui.publish.modern@Add Server` | Add server | es, hi | Server list | Done |
| `perc.ui.publish.modern@Edit Server` | Edit server | es, hi | Site workspace | Done |
| `perc.ui.publish.modern@Server Name` | Server name | es, hi | Server editor | Done |
| `perc.ui.publish.modern@Driver` / Delivery Type / Folder / Save | form labels | es, hi | Server editor | Done |
| `perc.ui.publish.modern@Set as Publish Now Server` | default flag | es, hi | Server editor | Done |
| `perc.ui.publish.modern@Discard Changes` | dirty guard | es, hi | Shell nav | Done |

## Sign-off

| Milestone | Reviewer | Date |
|-----------|----------|------|
| Ops shell (US1–3) | | |
| Design (US4) | | |
| Runtime (US5) | | |
