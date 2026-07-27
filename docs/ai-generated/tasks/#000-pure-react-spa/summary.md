# Design summary: Pure React / TypeScript WebUI

## Direction of record (2026-07-27)

**Product UI = React + TypeScript SPA only.** No dual mode, no new bridges, **no jQuery in the SPA**, no “shell = done.”

- **Plan of record:** [`#000-unified-ui-plan/unified-ui-plan.md`](../#000-unified-ui-plan/unified-ui-plan.md) **rev 4.1**
- **Module rules:** [`WebUI/AGENTS.md`](../../../WebUI/AGENTS.md)
- **Infra design (entry, bootstrap, PR 1–9):** [`design.md`](design.md)

### Immediate focus

**Home must become fully functional** (Recent, Bookmarks, Library, Search, Create, Gadgets). SPA chrome without working features is not acceptance.

Then: Publish → Explorer → Admin → Workflow → Widget Builder, same bar.

## Infra already produced (PR-1…PR-9)

| PR | Outcome |
|----|---------|
| 1–2 | React Login + SPA app shell |
| 3–4 | Feature routes (shells embedded) |
| 5 | `index.jsp` cutover to SPA |
| 6 | Explorer route + residual bridge doc |
| 7 | Gadgets on Home |
| 8 | Deleted obsolete product `*Modern.jsp` hosts |
| 9 | BrowserRouter + `PSWebUiSpaFallbackFilter` path URLs |

These are **routing/chrome**. Functional acceptance is **screen-by-screen** per unified-ui-plan rev 4.1.

## Product locks

1. SPA is the product UI for modern features  
2. React Login front door  
3. Home is default landing (gadgets on Home, not peer `/dashboard`)  
4. Server entry = query `spa.jsp?entry=…` only (never `#` on redirects)  
5. No dual-mode / no new PercModernUI product hosts  
6. **No jQuery (or Knockout/Dojo) in `src/main/ts` / modern bundle**  
7. Shell ≠ done  

## Explicitly not product strategy

- Track A Dojo→jQuery as a long-lived product track (vendor Dojo already removed; residual AA is debt)  
- Soft feature-flag cutover  
- Parallel auth REST API  
- Bridge-first new features  
