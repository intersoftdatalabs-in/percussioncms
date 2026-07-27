# Design summary: Pure React / TypeScript WebUI — eliminate JSP shells

## Produced

| Artifact | Path |
|----------|------|
| Full design (**rev 3.2** — aggressive SPA-first, **login-first**) | `docs/ai-generated/tasks/#000-pure-react-spa/design.md` |
| This summary | `docs/ai-generated/tasks/#000-pure-react-spa/summary.md` |

## Product locks

1. **SPA is the product UI** for modern features. No dual-mode, no soft feature-flag cutover. Old JSPs = reference until delete.
2. **Start at the front door:** React Login is the first shippable slice for **stakeholder demos** (Login → SPA), then authenticated routes.
3. **Home is the default product landing** after sign-in (not a separate “dashboard” peer).
4. **Dashboard gadgets have real product value**, but that capability should **live on Home** (section / widgets / compose), **not** as a long-term separate SPA route. Until then: legacy `?view=dash` full-page exit only.

## Stakeholder demo path

1. Open product → **React Login** (not classic `rxlogin.jsp` UI)
2. Sign in (POST existing `/login`) → **React SPA** landing
3. As later PRs land: Home → Publish → Admin… without `*Modern.jsp` hosts

## Implementability locks (still apply)

1. **Server entry = query only** — `spa.jsp?entry=…` (never `Location: …#/…`)
2. **Bridge** — sync `mount` + lazy `loadComponent` + generation tokens
3. **proxyURL** parity on all SPA redirects
4. **Login** — UI is React; auth remains existing form POST to `/login`

## Monday PR-1

`feat(webui): React Login SPA as product front door`

- React LoginPage + thin public host
- POST `/login` (CSRF, same fields as `rxlogin.jsp`)
- Success → `spa.jsp?entry=home` (proxyURL-aware)
- Minimal SPA landing so demo does not drop into a JSP shell

## PR plan (login-first)

1. **Login front door** + SPA landing — **done** (#1523)  
2. App shell + TopNav + entry query + 401→Login — **done** (#1526)  
3. Home + Publish routes (embedded shells) — **done** (#1527); **Home is default landing**  
4. Workflow + Admin + Widget Builder — **done** (#1528)  
5. Aggressive `index.jsp` cutover — **in review** (#1531); includes login CSS load fix  
6. Explorer SPA route + residual bridge doc — **in progress**  
7. **Home + gadgets:** fold React Dashboard **widgets into Home** (not a peer `/dashboard` SPA). Legacy jQuery dash remains temporary exit until then.  
8. Delete obsolete JSPs  
9. Optional path URLs  

## Explicitly not first

- Dual-mode / feature-flag fallback  
- Parallel auth REST API  
- Full editor/template/arch rewrite in PR-1  
