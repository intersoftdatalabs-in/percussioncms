# Design: Pure React / TypeScript WebUI — Eliminate JSP Shells

| Field | Value |
|-------|-------|
| **Status** | Implementation-ready (rev **3.2** — **aggressive SPA-first**, **login-first demo path**) |
| **Module** | `WebUI/` |
| **Branch base** | `development` |
| **Stack (verified)** | React 19.1, TypeScript 5.8, Vite 8, Jetty WAR under `/cm/` |
| **Canonical frontend** | `WebUI/src/main/frontend/` (Maven `frontend-maven-plugin` `workingDirectory`). Root `WebUI/package.json` / `WebUI/vite.config.ts` are **not** product build paths. |
| **Supersedes** | Track B shell architecture in `docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md`; design revs 1–2 dual-mode / soft-flag strategies; rev 3.1 “login deferred” sequencing |
| **Cutover stance** | **Aggressive SPA-first.** The SPA is the product UI. No dual-mode production path, no feature-flag kill-switch story. Old JSP shells may linger as **reference only** until cleanup deletes them. |
| **Demo sequencing (locked)** | **Start at the front door:** React Login is the first shippable vertical slice so stakeholders can demo Login → SPA app without walking through legacy `rxlogin.jsp`. Authenticated shell routes follow immediately. |
| **Out of scope (initially)** | Full React rewrite of webmgt editor, template layout/style, site architecture, Dojo AA/CB (Track A), Package Manager GWT, Desktop CE; inventing a parallel auth API (reuse existing `/login` POST) |

---

## 1. Problem statement & current state

### 1.1 Problem

CM1 WebUI modernized features by mounting React shells into per-feature JSP hosts via `window.PercModernUI.mount()`. That bridge got features shipped; it is no longer the product architecture we will extend.

**Product decision (locked):** stop investing in multi-page JSP shells and dual production modes. Build the pure React + TypeScript SPA as the **real** CM1 WebUI for modern features **now**. Navigation, chrome, bootstrap, and deep links live in the SPA. Client routing owns modern feature navigation. Legacy jQuery pages remain temporary **exits** (full page load out of the SPA), not peers of the SPA.

Pain of the current hybrid:

- Full multi-page reloads via `index.jsp` → `*Modern.jsp` between Home, Publish, Workflow Admin, Admin, Widget Builder.
- Each JSP re-implements locale/TMX, CSRF, header, allowlists, inline mount scripts (XSS surface).
- No `react-router-dom` today; aspirational `REACT-ROUTER-GUIDE.md` / `REDUX-ARCHITECTURE.md` are **not** implemented.
- Dual tree drift: `cm/app/` vs `cm/pages/app/` (workflow modern vs legacy; `admin` missing on pages).
- Static `registry.ts` pulls every shell into one ~882KB bundle for every embed host.

### 1.2 Verified hybrid architecture (code baseline)

| Layer | Path / fact |
|-------|-------------|
| TS sources | `WebUI/src/main/ts/` (~95 `.tsx`, ~70 `.ts`) |
| Canonical build | `WebUI/src/main/frontend/` → Vite `base: "/cm/modern/"`, outDir `target/generated-webui/cm/modern`, entry `assets/perc-modern-ui.js` |
| Entry today | `index.ts` → `bridge.ts` only |
| Bridge | `window.PercModernUI.mount(elementId, componentName, props)` |
| Registry | Static imports of all shells (no code-split) |
| Dispatcher | `cm/app/index.jsp` (diverging `cm/pages/app/index.jsp`) |
| REST | `api/client.ts` + feature APIs; CSRF from `window.OWASP_CSRFTOKEN` |
| i18n | `i18n/message.ts` → TMX `window.I18N` |
| Themes | `ui-themes/ThemeProvider.tsx` (often per-shell) |

### 1.3 Inventory: what becomes SPA routes vs temporary legacy exit

#### A. SPA-owned product surfaces (compose existing shells — no rewrite)

| Registry / shell | Source | Today host | SPA role |
|------------------|--------|------------|----------|
| `HomeShell` | `home/HomeShell.tsx` | `homeModern.jsp` | SPA route; server entry `?entry=home` |
| `PublishingShell` | `publishing/…` | `publishModern.jsp` | Route publish |
| `WorkflowAdminShell` | `workflowAdmin/…` | `adminWorkflowModern.jsp` | Route workflow |
| `AdminShell` | `admin/…` | `adminModern.jsp` | Route admin |
| `WidgetBuilderApp` | `widgetbuilder/…` | `widgetBuilderModern.jsp` | Route widget-builder |
| `UnavailableView` | `home/UnavailableView.tsx` | `unavailableModern.jsp` | SPA 404 / unknown |
| `ContentExplorerShell` | `contentExplorer/…` | explorerModern + embeds | Product route **and** optional bridge mount only inside **still-legacy** jQuery pages |
| `ContentBrowser`, `SearchPanel`, `FolderSecurityPanel`, `ActionToolbar`, `ContextMenu` | contentExplorer / contentBrowser | host dialog JSPs | Prefer SPA dialog routes when openers allow; else keep thin host until openers updated |
| `Dashboard` + widgets | `dashboard/*` | Registered; product `dashboard.jsp` still jQuery PercDashboard | **Gadgets stay valuable** — long-term target is **compose on Home** (not a peer SPA `/dashboard`). Until then: legacy exit `?view=dash` only |

#### B. `index.jsp` modern views → SPA immediately (aggressive)

| `?view=` | Old target | Aggressive target |
|----------|------------|-------------------|
| `home` | `homeModern.jsp` | **SPA shell** |
| `publish` | `publishModern.jsp` | **SPA shell** |
| `workflow` | `adminWorkflowModern.jsp` | **SPA shell** |
| `widgetbuilder` | `widgetBuilderModern.jsp` | **SPA shell** |
| `admin` | `adminModern.jsp` | **SPA shell** |
| unknown | `unavailableModern.jsp` | **SPA** unavailable route |

#### C. Temporary hybrid **exits** (full page leave SPA — not dual-mode)

| `?view=` | JSP | Notes |
|----------|-----|-------|
| `dash` | `dashboard.jsp` | Until gadgets live on Home; then remove peer dash surface |
| `editor` | `webmgt.jsp` | Long-lived until editor migration |
| `design` | `admin.jsp` | Temporary exit |
| `arch` | `siteArchitecture.jsp` | Temporary exit |
| `editAsset` / `editTemplate` | legacy JSPs | Temporary exit |

Returning to modern work = navigate back into SPA entry (not a second product UI for the same feature).

#### D. Dual-tree drift (must fix while shipping SPA)

| Item | `cm/app` | `cm/pages/app` |
|------|----------|----------------|
| `workflow` | `adminWorkflowModern.jsp` | **legacy** `adminWorkflow.jsp` |
| `admin` | `adminModern.jsp` | **missing** |

**Policy:** `cm/app` is canonical. Align or redirect `cm/pages/app` in the same PRs that cut over modern views — not a late cleanup afterthought.

#### E. Bridge retention (narrow — not product centerpiece)

`PercModernUI.mount` remains **only** for:

1. ContentExplorer (or similar) **embeds inside still-legacy jQuery pages** that have not been deleted yet (`dashboard.jsp`, `admin.jsp`, `webmgt.jsp`, etc. if they still mount explorer).
2. True host/popup dialogs until openers move to SPA routes.

The **feature app** (Home, Publish, Workflow, Admin, Widget Builder, primary explorer) is **SPA-owned**. Do not design product pages as “JSP + mount”.

### 1.4 Completed Track B work — reuse, do not redo

**989** Home + Widget Builder · **990** Publishing · **992** content explorer surfaces · **993** WorkflowAdmin + AdminShell.

This design **routes** those shells; it does not re-implement them.

---

## 2. Target architecture

### 2.1 SPA-first high level

```
Authenticated browser
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│  Server SPA document (single entry)                       │
│  /cm/app/  or  /cm/app/spa.jsp  (see §3)                  │
│  • Auth + maintenance + server role gate for intended view│
│  • XSS-safe bootstrap JSON                                │
│  • CSRF (/JavaScriptServlet) → TMX → module               │
│  • <div id="root">  (or perc-app-root)                    │
│  • /cm/modern/assets/perc-modern-ui.js                    │
└────────────────────────┬──────────────────────────────────┘
                         │ createRoot → <App />
                         ▼
┌───────────────────────────────────────────────────────────┐
│  ThemeProvider → BootstrapProvider → AuthProvider         │
│  → Router (BrowserRouter preferred; HashRouter only if    │
│     needed for same-week ship)                            │
│  AppLayout (TopNav, UserMenu) → <Outlet /> feature shells │
│  Legacy exit links → window.location full load to JSP     │
└───────────────────────────────────────────────────────────┘

Legacy jQuery pages (temporary):
  may still load perc-modern-ui.js and call PercModernUI.mount
  for embeds only — not the modern feature product path
```

### 2.2 Application modules

```
WebUI/src/main/ts/
  index.ts                 # SPA-first: boot App; register bridge for residual embeds
  bridge.ts                # PercModernUI.mount/unmount — sync API, async load (§2.9)
  registry.ts              # loadComponent(name): Promise<ComponentType> via import()
  app/
    App.tsx
    main.tsx               # createRoot on #root; apply entry query → router
    routes.tsx
    layout/
      AppLayout.tsx
      TopNav.tsx
      UserMenu.tsx
    bootstrap/
      types.ts
      BootstrapContext.tsx
      loadBootstrap.ts
    auth/
      AuthContext.tsx
      RequireRole.tsx      # UX; server gates + REST authoritative
      sessionHandlers.ts   # 401 → login (return URL = query entry contract, no #)
    deepLinks/
      allowlists.ts
      parseEntryQuery.ts   # server-driven ?entry=&section=&… → client route
      parseRoute.ts
    legacy/
      navigateToLegacy.ts  # full-page exit to unmigrated views
  home/, publishing/, workflowAdmin/, admin/, widgetbuilder/,
  contentExplorer/, contentBrowser/, dashboard/, api/, ui-themes/, i18n/
```

### 2.3 SPA-first entry (`index.ts`) — replaces dual-mode design

**Default production path:**

1. Load module on SPA document that contains `#root` (or `#perc-app-root`).
2. `import("./app/main")` → `createRoot` → render `<App />` with Router + providers.
3. Always also register `window.PercModernUI` from `bridge.ts` so **legacy embed pages** that load the same bundle still work until those pages are removed.

**Not** the design centerpiece: “boot SPA only if a magic node exists and otherwise live as bridge-only product.” The **product** pages are SPA documents. Bridge is a **secondary export** for residual embeds.

Vite entry remains one bundle path (`perc-modern-ui.js`) for simplicity; registry + routes use dynamic `import()` for code-split.

### 2.4 Routing (aggressive, implementable)

#### Server-driven entry contract (locked — query params only)

**Problem:** `Location` redirects and post-login return URLs that use **fragments** (`…#/home`) are unreliable: many proxies/clients strip or ignore the fragment on redirect; login return flows cannot depend on hash.

**Rule:** All **server-driven** navigations to the SPA use a **query-based entry contract**. Never use `#/…` as the primary server redirect or return URL.

Canonical SPA document: **`/cm/app/spa.jsp`** (or same params on the URL that `index.jsp` ultimately serves as the SPA shell).

| Server entry (examples) | Meaning |
|-------------------------|---------|
| `/cm/app/spa.jsp?entry=home` | Home (default section) |
| `/cm/app/spa.jsp?entry=home&section=library` | Home section |
| `/cm/app/spa.jsp?entry=publish&section=logs&siteId=…` | Publish deep link |
| `/cm/app/spa.jsp?entry=workflow&tab=users` | Workflow admin tab |
| `/cm/app/spa.jsp?entry=admin&tab=tools` | Admin tab (includes `tools`) |
| `/cm/app/spa.jsp?entry=widget-builder` | Widget Builder |
| `/cm/app/spa.jsp?entry=explorer&path=/…` | Content explorer |
| `/cm/app/spa.jsp?entry=unavailable` | Unknown / retired |

**`entry` allowlist (server + client):**  
`home` | `publish` | `workflow` | `admin` | `widget-builder` | `explorer` | `unavailable`  
(Map legacy `view=widgetbuilder` → `entry=widget-builder`.)

**Additional query params** (allowlisted per entry; same rules as § deep-link table):  
`section`, `tab`, `siteId`, `serverId`, `path` (explorer), plus mapped legacy `initialScreen` → `section` on server when translating `?view=home&initialScreen=…`.

**Server role gates** use the same `entry` value (not hash): e.g. `entry=workflow` / `entry=admin` require Admin; `entry=publish` / `entry=widget-builder` require Admin or Designer.

**SPA boot sequence:**

1. Parse `window.location.search` with `parseEntryQuery` (allowlists only).
2. Map to client route (`/home/library`, `/publish/logs`, …).
3. `navigate(route, { replace: true })` (HashRouter or BrowserRouter).
4. Optionally strip entry query via `history.replaceState` / router replace so the address bar shows the client route form only (hash path or clean path) — **after** route applied.
5. If no/invalid `entry`, default to home (or bootstrap `defaultView` if dash/editor → those are legacy full loads, not SPA entry).

**In-SPA client navigations** use the router normally (hash or path links). No need for `?entry=` after first paint.

**Post-login return URL** must be a **query entry URL** (e.g. `/cm/app/spa.jsp?entry=home`), never `…#/home`.

#### Client routes (after entry applied)

| Client path (basename `/cm/app` if BrowserRouter; or hash `#/…`) | Module | Server gate on `entry` |
|-----------------------------------------------------------------|--------|-------------------------|
| `/` or `/home` | HomeShell | Auth |
| `/home/:section?` | HomeShell | Auth |
| `/publish` / `/publish/:section` | PublishingShell | Admin or Designer |
| `/workflow` / `/workflow/:tab` | WorkflowAdminShell | Admin |
| `/admin` / `/admin/:tab` | AdminShell | Admin |
| `/widget-builder` | WidgetBuilderApp | Admin or Designer + WB active |
| `/explorer` | ContentExplorerShell | Auth |
| `/unavailable` | UnavailableView | Auth |
| *(do not target)* `/dashboard` | — | Prefer **Home section / widgets**; avoid peer SPA dashboard |

#### Router choice (client-only; still one product UI)

| Item | Choice |
|------|--------|
| Product shell URL | `index.jsp` maps modern views → **`spa.jsp?entry=…`** (query contract) |
| Server redirects / login return | **Query only** — never `#` fragments |
| Client router first ship | **HashRouter** OK so *in-SPA* refresh of client routes does not 404 under Jetty |
| Client router end-state | BrowserRouter + optional rewrite — polish only |
| `*Modern.jsp` | Not product path; optional 302 → `spa.jsp?entry=…` (query) |

Do **not** keep serving `homeModern.jsp` as a working product alternative.

#### Frozen deep-link allowlists (TS source of truth)

| Area | Canonical | Aliases |
|------|-----------|---------|
| Home section | `recent`, `bookmarks`, `library`, `search`, `create` | `list`→`recent`, `newitem`→`create`, `bookmark`→`bookmarks` |
| Publish section | `sites`, `status`, `logs`, `design`, `runtime`, `editions` | `site`→`sites`, `log`→`logs`, `edition`→`editions`; non-designer `design`→`sites` |
| Workflow tab | `workflow`, `roles`, `users`, `categories` | — |
| Admin tab | `tasks`, `logs`, `notifications`, **`tools`** | `tools` is intentional (shell has ToolsSection) |
| IDs | `siteId`, `serverId` | `^[A-Za-z0-9_-]{1,128}$` |
| Explorer path | leading `/`, length &lt; 2048, `[/A-Za-z0-9._-]+` | — |

#### `?view=` → SPA query entry map (aggressive)

| Incoming | Server target (use `proxyURL` + path; **no hash**) |
|----------|-----------------------------------------------------|
| `view=home` (+ `initialScreen`) | `spa.jsp?entry=home&section={mapped}` |
| `view=publish` (+ section/ids) | `spa.jsp?entry=publish&section=…&siteId=…&serverId=…` |
| `view=workflow` (+ section) | `spa.jsp?entry=workflow&tab=…` |
| `view=admin` (+ tab) | `spa.jsp?entry=admin&tab=…` |
| `view=widgetbuilder` | `spa.jsp?entry=widget-builder` |
| unknown modern | `spa.jsp?entry=unavailable` |
| `dash` / `editor` / `design` / `arch` / edit* | **Legacy JSP exit** (full page, existing forwards) |

### 2.5 Layout & navigation

- SPA always uses React `AppLayout` + `TopNav` (no jQuery `header.jsp` / `mainnav.jsp` on SPA document).
- Nav parity with `mainnav.jsp` (adjusted for product direction):
  - Always: **Home** (default landing), Editor (**exit**)
  - **Dashboard:** temporary **legacy exit** only (`?view=dash`) while gadgets still live on jQuery dash; **do not** invest in a peer SPA dashboard route — fold React gadgets into **Home** instead
  - Admin or Designer: Architecture (**exit**), Design (**exit**), Publish (**SPA**)
  - Admin: Administration → **SPA workflow** (product label unchanged)
  - WB active + Admin/Designer: Widget Builder → **SPA**
- Shells accept `embedded` (or layout context): hide BrandBar/BrandFooter/duplicate ThemeProvider under AppLayout (**required when routed**, not optional polish).
- **spa entry never includes `header.jsp`** (avoids `dispatched` redirect traps).

### 2.6 Providers

Theme (app root) → Bootstrap JSON → Auth → Router. i18n stays TMX + `message()`. No Redux.

### 2.7 Bundle strategy

1. **Lazy registry** via shared `loadComponent(name)` (`import()` by name) so residual embeds do not static-import all shells into the main chunk.
2. SPA routes use `React.lazy(() => loadComponent(…))` or equivalent on the **same** loader (shared chunks).
3. Single Vite entry is fine; dual entry only if size budget forces it later.
4. **PR-1 exit:** main chunk must not statically import Home/Publish/Workflow/Admin/WB/Dashboard shells; Vitest or build assertion documents that.

### 2.8 Bridge module role (secondary)

```ts
// index.ts (conceptual)
import "./bridge"; // registers PercModernUI for legacy embeds that still load this file

const rootEl = document.getElementById("root") ?? document.getElementById("perc-app-root");
if (rootEl) {
  void import("./app/main").then((m) => m.boot(rootEl));
} else {
  // Bundle loaded on a legacy embed page without SPA root — bridge-only load.
  // Residual support for embeds, not the product feature path.
  console.info("[PercModernUI] bridge ready (no SPA root)");
}
```

Product modern views **always** include the SPA root.

### 2.9 Bridge mount contract (lazy registry — implement in PR-1)

Today `PercModernUI.mount` is **synchronous** `void` and JSPs call it after a short poll for `window.PercModernUI`. Lazy `import()` must not break that call shape.

| Rule | Spec |
|------|------|
| **Public API** | `mount(elementId, componentName, props?): void` and `unmount(elementId): void` remain **sync** (no Promise return to hosts). |
| **Load** | Internally `void loadComponent(componentName).then(…)` — shared with SPA routes. |
| **Generation token** | Per `elementId`, increment a generation (or store `AbortController` / nonce) on each `mount`/`unmount`. When a load resolves, apply only if generation still matches; **ignore stale** resolutions. |
| **unmount** | Sync: unmount active React root if any; **cancel/invalidate** pending load for that id (bump generation) so a late import does not remount. |
| **Unknown name** | `console.error` with name; do not throw into host page. |
| **Load failure** | `console.error`; optional `data-perc-mount-error="1"` (or similar) on the container; **no throw** to JSP. |
| **Missing container** | `console.error`; return (existing behavior). |
| **Shared loader** | `export function loadComponent(name: string): Promise<ComponentType<any>>` in `registry.ts` (or `componentLoader.ts`); bridge + SPA both use it. |
| **Main chunk** | Must not static-import all shell modules; only the loader map of dynamic `import()` factories. |

**Vitest (PR-1 hard exit criteria):**

1. Success path: mount registered name → component appears (await microtasks/import).
2. Unknown name: no throw; error logged.
3. Race: `mount` then immediate `unmount` before import resolves → no mount / no leftover root; late resolve ignored.
4. Race: `mount` A then `mount` B same elementId → only B remains after loads settle.
5. Bundle/static analysis or unit guard: shell modules not in main entry static graph (as practical in Vitest/Vite).

---

## 3. Server integration

### 3.1 Single SPA document

**`WebUI/src/main/webapp/cm/app/spa.jsp`** (or equivalent include used by `index.jsp`):

1. Maintenance redirects (shared with current `index.jsp` logic).
2. Bootstrap user / roles / sites / WB / defaultView / locale (same beans).
3. **Server role gates** for the intended modern view using **`entry`** query (and/or legacy `view=` when still on index before forward) — same `adminViews` / `designerViews` semantics. Unauthorized → redirect to default SPA home entry or user default **using query entry URLs only** — **do not** emit privileged chrome.
4. XSS-safe bootstrap JSON (§3.2).
5. CSRF + TMX (canonical paths §7).
6. `<div id="root"></div>` + module script to `perc-modern-ui.js` (optional deploy cache-buster query).
7. **No** `header.jsp` / `mainnav.jsp` / feature mount scripts.
8. Preserve allowlisted `entry` / `section` / `tab` / ids / `path` on the request so the client can parse them from `location.search`.

### 3.2 Bootstrap JSON — XSS-safe (mandatory)

`type="application/json"` is **not** a security control. Shared helper must:

1. Serialize with Jackson (or equivalent).
2. Escape for script context: `<` → `\u003c`, `>` → `\u003e`, `&` → `\u0026`, U+2028/U+2029 escaped.
3. Emit `<script type="application/json" id="perc-bootstrap">…</script>`.
4. Client: `JSON.parse(el.textContent)`.
5. Tests with names/roles containing `</script><script>…`, quotes, Unicode line separators.

```ts
interface PercBootstrap {
  user: { name: string; admin: boolean; designer: boolean; accessibility?: boolean; roles: string[] };
  locale: string;
  hasSites: boolean;
  widgetBuilderActive: boolean;
  defaultView: "home" | "dash" | "editor";
  // Prefer reading entry from location.search (query contract).
  // Optional echo of already-allowlisted entry for debugging only — not required.
}
```

No `spaEnabled` product kill-switch. SPA **is** the product for modern views.

### 3.3 `index.jsp` aggressive behavior + proxyURL

```
if maintenance → redirect maint pages
else if view is modern (home|publish|workflow|admin|widgetbuilder) or null default is home:
  enforce role gates (map view → entry)
  redirect or forward to spa.jsp?entry=…&… (allowlisted params only)
else if view is legacy exit (dash|editor|design|arch|editAsset|editTemplate):
  forward to existing JSP (unchanged)
else:
  spa.jsp?entry=unavailable
```

**proxyURL parity (mandatory):** Every **redirect** `Location` to the SPA (and any 302 from obsolete `*Modern.jsp`) **must** reuse the same `proxyURL` construction as existing `index.jsp`: when `PSServer.isRequestBehindProxy(request)`, prefix with `PSServer.getProxyURL(request, true)`; otherwise site-relative `/cm/app/spa.jsp?…` is fine. Do not invent a second base-URL scheme. Applies to post-login return targets that point at SPA as well.

Default homepage:

- User homepage Home → `spa.jsp?entry=home`  
- Dashboard → legacy dash exit only (until gadgets on Home)  
- Editor → legacy editor  

**Stop forwarding modern views to `*Modern.jsp`.** Those files become reference-only (or 302 to `spa.jsp?entry=…` with **query** params and **proxyURL**).

### 3.4 Dual-tree (`cm/pages/app`)

Same cutover rules on both trees in the cutover PR, **or** pages tree redirects everything to `/cm/app/…`. Do not leave pages serving legacy workflow while app serves SPA.

### 3.5 History / path URLs (later polish, not dual UI)

Optional WebUI filter `com.percussion.webui.filter.PSWebUiSpaFallbackFilter`: GET `/cm/app/{home|publish|workflow|admin|widget-builder|explorer|unavailable}/**` → forward `spa.jsp`; exclude `*.jsp`, static assets, `/cm/modern/**`, non-GET. Used only when switching HashRouter → BrowserRouter. **Still one product UI.**

### 3.6 No soft-flag cutover story

Do **not** implement `perc.webui.spa.enabled` as the delivery model. Ops rollback = **git revert / redeploy** of the cutover commit(s), or temporarily restore JSP forwards in a hotfix — not a long-lived dual production mode. Reference files in tree are not a runtime fallback product.

### 3.7 Query entry vs client hash (summary)

| Actor | Mechanism |
|-------|-----------|
| `index.jsp` / login return / 302 from old Modern JSP | `proxyURL` + `/cm/app/spa.jsp?entry=…` (**query only**) |
| SPA first paint | Parse query → router `navigate(..., { replace: true })` |
| In-SPA TopNav / links | Client router (hash or path) |
| Forbidden | Server `Location: …#/…` as primary deep link |

---

## 4. Migration phases (aggressive, **login-first**)

### Stakeholder demo path (product lock)

Demo script from day one of implementation work:

1. Open product URL → **React Login** (not `rxlogin.jsp` chrome).
2. Sign in → land in **React SPA** shell (TopNav + Home or placeholder).
3. As routes land, continue demo: Home → Publish → Admin… without multipage `*Modern.jsp` hosts.

Login is intentionally first so the story is “pure React UI from the front door,” not “React after a legacy form.”

### Phase 0 — React Login + minimal SPA boot (start immediately)

- Add `react-router-dom` if needed for post-login app (login page may be a dedicated public document).
- **`LoginPage` / `LoginApp`** React component: username, password, locale, optional Select UI, error display, Intersoft/theme chrome.
- Thin public server document (replace or forward `rxlogin.jsp` → SPA login host) with CSRF + TMX + `#root` + modern bundle.
- **Auth stays server-side:** form POST to existing `/login` (`j_username`, `j_password`, `j_locale`, `j_selectUI`, CSRF) — same as today’s `rxlogin.jsp`. Do **not** invent a parallel login REST API unless product later requires it.
- Locales: bootstrap JSON from the login JSP (server iterates `PSLocaleManager` as today) — XSS-safe encoding.
- Success redirect / default return: `proxyURL + /cm/app/spa.jsp?entry=home` (query contract).
- Error: show `j_error` (and any server error query params) in React UI.
- Minimal authenticated SPA shell (`#root` + App layout placeholder) so post-login is not a JSP shell.
- Vitest: Login form fields, allowlisted redirect construction, error rendering; CSRF field present in form.

**Exit:** Stakeholder can demo **React Login → authenticated SPA document**. Legacy `rxlogin.jsp` markup unused (file may remain as reference until delete).

### Phase 1 — SPA App scaffold + entry query + session handlers

- Full `app/` App, routes skeleton, AppLayout + TopNav.
- Shared `loadComponent` + race-safe bridge mount (§2.9).
- `spa.jsp` + authenticated XSS-safe bootstrap (`PercBootstrap` user/roles/sites/WB).
- **`parseEntryQuery`**: boot maps `?entry=` → router `replace`.
- 401 from SPA → **React Login** with query return URL (`?return=` allowlisted to `spa.jsp?entry=…`).
- Server role gates on `entry`.

**Exit:** Authenticated GET `spa.jsp?entry=home` shows App; mid-session 401 returns to React Login.

### Phase 2 — Wire all ready shells as SPA routes

Mount Home, Publish, Workflow, Admin, Widget Builder, Explorer, Unavailable with `embedded` + allowlists. Split across PRs for review size; each PR is **product SPA functionality**.

**Exit:** All modern features usable entirely inside SPA without opening `*Modern.jsp`.

### Phase 3 — Aggressive `index.jsp` cutover

- Modern `?view=` → `proxyURL + spa.jsp?entry=…` only (both trees); **no `#` in Location**.
- `*Modern.jsp`: stop using (302 to query entry or leave unreferenced).
- TopNav complete; legacy exits for dash/editor/design/arch.

**Exit:** Primary product navigation for modern features never mounts via feature JSP shells.

### Phase 4 — Cleanup reference files + remaining hybrid exits

- Delete obsolete `*Modern.jsp` (and dead mounts) once SPA routes + tests own them.
- Compose gadgets into Home (retire peer dash surface when ready).
- Dialog hosts → SPA dialogs or delete when openers updated.
- Optional BrowserRouter + rewrite.
- Align docs; mark stale REACT-ROUTER/REDUX guides non-authoritative.

### Parallel track

Unmigrated jQuery (editor, templates, arch, Track A): remain **full-page exits** until each is migrated into SPA as its own program of work.

---

## 5. Route map

See §2.4 tables (canonical paths/hashes, aliases, `?view=` map). That is the implementer source of truth.

---

## 6. Hybrid strategy (exits only)

| Kind | Behavior |
|------|----------|
| **SPA product** | Home (+ gadgets when folded in), Publish, Workflow, Admin, Widget Builder, (Explorer) |
| **Legacy exit** | Full `window.location` to remaining JSPs |
| **Legacy embed** | Bridge mount of explorer (etc.) **inside** those JSPs until page deleted |
| **Not allowed** | Shipping the same feature as both live JSP shell and SPA “optional mode” |

No iframe default for webmgt.

---

## 7. Security model

| Concern | Target |
|---------|--------|
| First paint auth | `PSSecurityFilter` on `/*` — unchanged |
| Mid-session | Global SPA 401 → login with **query entry return URL** (e.g. `/cm/app/spa.jsp?entry=home` or current entry reconstructed from allowlisted state — **never** `#/…`); optional sessionCheck on focus |
| CSRF | Blocking `/JavaScriptServlet` before module; SPA waits for token or shows error |
| Role gates | **Server** on index/spa for modern views; client `RequireRole` UX only; REST 403 |
| Bootstrap XSS | Mandatory script-context encoding + tests |
| Deep links | Server + TS allowlists on `entry`/`section`/`tab`/ids/`path`; no raw query echo into JS; server redirects use query entry contract only |
| TMX | Prefer `/tmx/tmx.jsp?...`; `/Rhythmyx/tmx/...` only when context path requires (same idea as `api/paths.ts`) |
| CSP | External scripts + JSON text; no feature inline mount scripts on SPA |

---

## 8. Build / deploy

| Item | Spec |
|------|------|
| package.json / vite | **Only** `WebUI/src/main/frontend/` |
| Deps | `react-router-dom` (+ types if needed) |
| Entry | SPA-first `index.ts`; lazy registry/routes |
| Assets | `/cm/modern/assets/perc-modern-ui.js` |
| Cache | Deploy cache-buster query or short-cache headers on stable entry name |
| Pre-PR | `cd WebUI && ../mvn-env.sh clean install` |

---

## 9. Testing strategy

| Layer | Cases |
|-------|-------|
| Unit | Allowlists + **parseEntryQuery**; bootstrap parse + **`</script>` XSS payloads**; role UX guards; **bridge mount async races** (§2.9); shared `loadComponent`; routes render shells; 401 handler with query return URL; CSRF missing token |
| Integration / QA | Login → each modern SPA route → deep link refresh → designer vs admin → WB off → legacy exit editor → return SPA |
| Cutover | `?view=…` modern maps to `spa.jsp?entry=…` (query, proxyURL); never serves feature mount JSP as product UI; no server redirects with `#` |
| Dual-tree | Both trees SPA for modern views or pages→app redirect |
| Embed residual | Legacy pages that still embed explorer still mount via bridge until removed |
| Bundle | Main chunk must not static-import all shells; loadComponent dynamic graph |

Manual QA (short):

1. Admin: SPA home → publish → workflow → admin → widget-builder without full multipage JSP shells.  
2. Deep publish via `spa.jsp?entry=publish&section=logs` then refresh (client route stable).  
3. Designer: publish OK; workflow denied server-side.  
4. Editor exit works; return Home SPA.  
5. Home as landing; gadgets on Home when ready (legacy dash exit until then).  
6. Bootstrap XSS test fixtures green.

---

## 10. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Jetty path 404 on in-SPA refresh | HashRouter for client routes short-term; server deep links always query `entry` |
| Aggressive cutover bugs | Fast PR feedback; fix-forward; reference JSPs still in git for comparison (not runtime dual product) |
| Double chrome | `embedded` required on routed shells |
| Bundle size on legacy embeds | Lazy `loadComponent` + §2.9 race-safe mount |
| Dual-tree drift | Cutover both trees together |
| Bootstrap XSS | Encoder + tests |
| Missing server role gates | index/spa enforce before HTML |
| Mid-session expiry | Global 401 handler |
| Deleting JSPs too early | Phase 3 stop **using**; Phase 4 delete after tests |

---

## 11. Alternatives considered

| Alternative | Verdict |
|-------------|---------|
| Dual-mode bridge product + SPA optional | **Rejected by product owner** — do not design this |
| Soft cutover flag default false | **Rejected** as delivery story — SPA is product now |
| Keep `*Modern.jsp` as production peers | **Rejected** |
| Big-bang rewrite shells | Reject — compose existing shells |
| Permanent multi-JSP + shared includes | Reject for modern features |
| Redux default | Reject |
| Next.js / SSR | Reject (Jetty WAR) |
| HashRouter short-term (client only) | **Accept** for in-SPA refresh convenience |
| BrowserRouter + rewrite | Accept as URL polish, single UI |
| Server deep links via `#` fragment | **Reject** — use query `entry` contract |
| Query-only client router forever | Acceptable if hash/path both blocked |
| iframe legacy editors | Reject default |

---

## 12. Open questions

| ID | Question | Status |
|----|----------|--------|
| OQ-1 | Client hash vs path URLs | **Pragmatic lock:** client Hash first OK; **server always query `entry`**. Path client later. |
| OQ-2 | Dashboard SPA vs Home | **Resolved:** gadgets → **Home**; no long-term peer SPA dashboard |
| OQ-3 | Dialog hosts → SPA routes timing | Keep thin hosts until openers updated; not dual product for main nav |
| OQ-4 | Feature flag soft cutover | **Closed — not used** |
| OQ-5 | Administration → workflow | **Locked** (preserve mainnav) |

---

## 13. Key Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| **KD-1** | **SPA-first entry**: product modern UI boots App/Router on `#root`; bridge is secondary for residual embeds only | Product owner: aggressive new UI now; no dual-mode centerpiece |
| **KD-2** | Compose existing shells as route modules | 989/990/992/993 done |
| **KD-3** | Single SPA server document (`spa.jsp` / index forward) with XSS-safe bootstrap | One host, security + simplicity |
| **KD-4** | `react-router-dom`; no Redux default | Router load-bearing |
| **KD-5** | Server deep links = **query `entry` contract**; client HashRouter OK short-term; BrowserRouter later polish | Fragments unreliable on redirects/login return; still one product UI |
| **KD-6** | **Aggressive cutover**: modern `?view=` → SPA only; no flag dual path | Owner override of soft cutover |
| **KD-7** | Unmigrated jQuery = full-page **exits** only | Temporary hybrid boundary |
| **KD-8** | Deep-link allowlists in TS | XSS hygiene |
| **KD-9** | **React Login is the first product slice** (front door for demos); posts to existing `/login` | Stakeholder demo from the door; no parallel auth API |
| **KD-10** | One Vite bundle name; lazy chunks underneath | Hosts + embeds |
| **KD-11** | Server role gates + REST authoritative; client guards UX | Defense in depth |
| **KD-12** | PR-1 = Login SPA + post-login SPA landing; PR-2 = full app shell | Demo path first, then depth |
| **KD-13** | `cm/app` canonical; dual-tree fixed with cutover | Stop drift |
| **KD-14** | Lazy `loadComponent` + sync mount API with generation tokens (§2.9) | Embed payload + JSP-compatible bridge |
| **KD-15** | Optional rewrite filter in WebUI module when path URLs wanted | Ownership |
| **KD-16** | No `spaEnabled` kill-switch product story | Aggressive SPA |
| **KD-17** | Maven `src/main/frontend` is package.json source of truth | Avoid wrong tree |
| **KD-18** | `embedded` required for routed shells | No double chrome |
| **KD-19** | Retain old JSP files as **reference** until cleanup PR deletes them | Not runtime dual UI |
| **KD-20** | `*Modern.jsp` not production path after Phase 3 | SPA owns modern features |
| **KD-21** | All server SPA redirects use **proxyURL** parity with existing `index.jsp` | Behind-proxy deployments |
| **KD-22** | Login posts to existing `/login` form action; success → `spa.jsp?entry=…` (query) | Preserve auth security; SPA owns UI only |
| **KD-23** | 401 mid-session returns to **React Login**, not `rxlogin.jsp` | Consistent front door |
| **KD-24** | Logout UI may follow Login (same PR or PR-1b); server logout endpoint unchanged | Demo completeness |

---

## 14. Monday-morning first PR (concrete) — **Login front door**

**Title:** `feat(webui): React Login SPA as product front door`

**Does:**

1. Add `LoginPage` (or `login/LoginApp`) under `WebUI/src/main/ts/` — modern React UI matching product theme direction (`ui-themes` where practical).
2. Thin public host: rewrite/replace runtime path for `rxlogin.jsp` (or forward it) so the **visible** page is React + CSRF + TMX + locales bootstrap. Keep old JSP as reference until cleanup if needed.
3. Form: multipart POST to **`login`** with `j_username`, `j_password`, `j_locale`, `j_selectUI` + CSRF token field (same contract as current `rxlogin.jsp`).
4. Locales list from server bootstrap JSON (XSS-safe), not hardcoded.
5. Display login errors (`j_error` / server error params) in React.
6. On successful auth, server continues existing login flow; ensure post-login landing targets **`proxyURL + /cm/app/spa.jsp?entry=home`** (or allowlisted `return` query). Coordinate with security filter / default view if required.
7. Minimal authenticated `spa.jsp` + `#root` landing shell (placeholder Home or “signed in” chrome) so the demo does not drop into a `*Modern.jsp` host after login.
8. Vitest for Login form + redirect URL builders + error UI; security review checklist (no password logging, no XSS via error echo).
9. **No** dual-mode flags; **no** parallel auth API.

**Demo after PR-1:** open CMS → React Login → sign in → React SPA landing.

**Next:** PR-2 full App/TopNav/loadComponent; PR-3+ real shells; index cutover.

---

## 15. Handoff

| Audience | Notes |
|----------|--------|
| **Hephaestus** | **Login first**; then SPA shell, query entry, shells, cutover |
| **DevOps** | Anonymous path still `rxlogin` / login; assets under `/cm/modern/`; proxyURL unchanged |
| **Sherlock** | Login XSS/error handling; CSRF on form; bootstrap XSS; no password in logs; 401 → React Login |
| **Patton** | Demo script: Login → SPA → feature routes as they land |
| **Orchestrator** | login → app shell → routes → index cutover → cleanup |

### Explicitly deferred

Editor/template/arch React; Track A; GWT/Desktop; Redux; dual-mode infrastructure; parallel login REST (unless later required).

---

## 16. Login technical contract (implementer detail)

### 16.1 Current baseline (verified)

| Item | Fact |
|------|------|
| Page | `WebUI/src/main/webapp/rxlogin.jsp` (~165 lines) |
| Form | `<csrf:form … action="login" enctype="multipart/form-data">` |
| Fields | `j_username`, `j_password`, `j_locale`, `j_selectUI` |
| Error | `j_error` request param rendered into page |
| Locales | Server `PSLocaleManager.getLocales()` in JSP |
| Security | `system-security-conf.xml`: `/rxlogin.jsp`, `/login` anonymous |
| CSRF | `/JavaScriptServlet` + csrf form tag |

### 16.2 Target

| Item | Spec |
|------|------|
| UI | React `LoginPage` in modern bundle |
| Host | Public HTML document with `#root` (thin JSP or equivalent) — **product path is React** |
| POST | Unchanged `/login` multipart form + CSRF |
| Bootstrap (login) | `{ locales: {name, displayName}[], autocomplete, error?, returnUrl? }` XSS-safe |
| Success | Existing server login success → SPA entry query URL |
| Failure | Stay on React Login with error message (allowlist/encode error text) |
| Mid-session | SPA 401 → React Login + allowlisted return |

### 16.3 What Login is *not*

- Not a new OAuth/OIDC project unless product expands scope later.
- Not dual UI with classic `rxlogin.jsp` still linked from product chrome.
- Not blocked on full Home/Publish migration — landing shell can be minimal for first demo.

---

## PR Plan

Each PR advances **SPA product UI**, starting at the **front door**. No soft-flag dual-path PRs.

### PR-1 — React Login front door + post-login SPA landing

| | |
|--|--|
| **Title** | `feat(webui): React Login SPA as product front door` |
| **Files** | `ts/login/**`; thin login host JSP; security path if needed; minimal `spa.jsp` landing; Vitest; theme styles |
| **Deps** | None |
| **Description** | Stakeholders open product → React Login → POST `/login` → SPA landing. Replace runtime use of classic `rxlogin.jsp` UI. **Exit:** demoable front door; tests; no password leakage; CSRF present. |

### PR-2 — SPA App shell + TopNav + lazy loadComponent + entry query + session

| | |
|--|--|
| **Title** | `feat(webui): SPA app shell, TopNav, entry query, race-safe bridge, 401→Login` |
| **Files** | `app/**`; `loadComponent` / `bridge.ts`; `parseEntryQuery`; auth handlers; `spa.jsp` bootstrap; Vitest |
| **Deps** | PR-1 |
| **Description** | Full authenticated App chrome. 401 returns to React Login with query return. Lazy registry contract (§2.9). |

### PR-3 — Home + Publish routes (embedded shells)

| | |
|--|--|
| **Title** | `feat(webui): SPA routes for HomeShell and PublishingShell` |
| **Files** | routes; shell `embedded`; allowlists; tests |
| **Deps** | PR-2 |
| **Description** | Product usable for home + publish entirely in SPA. Demo extends past landing. |

### PR-4 — Workflow Admin + Admin + Widget Builder routes

| | |
|--|--|
| **Title** | `feat(webui): SPA routes for Workflow, Admin, Widget Builder` |
| **Files** | routes; shells embedded; WB gate; admin `tools` tab |
| **Deps** | PR-2 |
| **Description** | Remaining modern feature modules as SPA routes. |

### PR-5 — Aggressive index.jsp cutover (both trees)

| | |
|--|--|
| **Title** | `feat(webui): serve SPA for modern views; stop using *Modern.jsp product hosts` |
| **Files** | `cm/app/index.jsp`, `cm/pages/app/index.jsp` (align dual-tree); redirects to `spa.jsp?entry=…`; optional 302 from `*Modern.jsp`; TopNav |
| **Deps** | PR-3, PR-4 |
| **Description** | **Aggressive:** modern `?view=` → `proxyURL + /cm/app/spa.jsp?entry=…` (query only, never `#`). Role gates on `entry`. Legacy views unchanged. `*Modern.jsp` not product path (reference or 302 with same query contract). |

### PR-6 — Explorer SPA route + residual embed hygiene

| | |
|--|--|
| **Title** | `feat(webui): ContentExplorer SPA route; document residual bridge embeds` |
| **Files** | routes; explorer; docs for which legacy pages still mount bridge |
| **Deps** | PR-5 |
| **Description** | Primary explorer in SPA; embeds only on remaining legacy pages. |

### PR-7 — Gadgets on Home (not peer Dashboard SPA)

| | |
|--|--|
| **Title** | `feat(webui): compose Dashboard gadgets into Home` (retire peer dash surface) |
| **Files** | `home/*` (section or widgets area); reuse `dashboard/*` React widgets; TopNav (drop/relabel legacy dash exit); optional retire `dashboard.jsp` |
| **Deps** | PR-3 (Home SPA route) |
| **Description** | **Product lock:** gadget utility stays; **placement is Home**, not a separate SPA `/dashboard`. Reuse existing React widget components. Legacy jQuery dash exit only until this lands. |

### PR-8 — Delete obsolete login/Modern JSP product hosts + docs

| | |
|--|--|
| **Title** | `chore(webui): remove unused login/*Modern.jsp product shells; update AGENTS/unified-ui-plan` |
| **Files** | Delete unreferenced shells including classic login markup if unused; dual-tree cleanup; docs |
| **Deps** | PR-1+ and PR-5+ green without those hosts |
| **Description** | Reference retention ends; tree cleaned. |

### PR-9 — (Optional) BrowserRouter + WebUI SPA fallback filter

| | |
|--|--|
| **Title** | `feat(webui): path-based SPA URLs with PSWebUiSpaFallbackFilter` |
| **Files** | Filter, web.xml, router switch, tests |
| **Deps** | PR-5 stable |
| **Description** | URL polish; still single product UI. |

---

## Appendix A — Evidence anchors

- `WebUI/src/main/ts/bridge.ts`, `registry.ts`, `index.ts`
- `WebUI/src/main/webapp/cm/app/index.jsp` vs `cm/pages/app/index.jsp`
- `WebUI/src/main/frontend/vite.config.ts`, `package.json`
- `api/csrf.ts`, `api/paths.ts`, `home/deepLinkMap.ts`
- `PSUserService` `/current`, widgetbuilder `/active`

## Appendix B — Shell embedded contract

| Shell | JSP header today | Internal brand | Props | Routed `embedded` |
|-------|------------------|----------------|-------|-------------------|
| HomeShell | yes | BrandBar/Theme | section, isAdmin | hide brand; app Theme only |
| PublishingShell | yes | shell chrome | section, ids, showDesign | no duplicate top chrome |
| WorkflowAdminShell | minimal | shell header | tab | same |
| AdminShell | no shared header | title/tabs | tab (+ tools) | same |
| WidgetBuilderApp | yes | app chrome | — | same |
| ContentExplorerShell | host-dependent | panel | path | route or legacy embed |
| Dashboard gadgets (React) | Home | widgets/section | reuse `dashboard/*` | Compose into Home; no peer SPA |

## Appendix C — What “aggressive” is not

- Not rewriting shell business logic  
- Not deleting git history  
- Not forcing editor into SPA this program  
- Not dual production modes “just in case”  
- Not waiting for perfect path rewrite before shipping SPA product  
- Not using `Location: …#/…` for server or login deep links  

## Appendix D — Server entry query cheat sheet

```
# Home
{proxyURL}/cm/app/spa.jsp?entry=home
{proxyURL}/cm/app/spa.jsp?entry=home&section=library

# Publish
{proxyURL}/cm/app/spa.jsp?entry=publish&section=logs&siteId=ID&serverId=ID

# Workflow / Admin
{proxyURL}/cm/app/spa.jsp?entry=workflow&tab=users
{proxyURL}/cm/app/spa.jsp?entry=admin&tab=tools

# Widget Builder / Explorer / Unavailable
{proxyURL}/cm/app/spa.jsp?entry=widget-builder
{proxyURL}/cm/app/spa.jsp?entry=explorer&path=/Sites/...
{proxyURL}/cm/app/spa.jsp?entry=unavailable
```

`proxyURL` empty string or proxy prefix per existing `index.jsp` pattern.

---

## Revision history

| Rev | Notes |
|-----|--------|
| 1 | Initial hybrid→SPA design |
| 2 | Review fixes: XSS bootstrap, Hash interim, dual-tree, lazy registry, soft cutover flags |
| 3 | **Product owner override:** aggressive SPA-first; remove dual-mode / soft-flag delivery story |
| **3.1** | Query-based **server entry contract** (`?entry=`); no fragment redirects; bridge **async load + sync mount** race contract; **proxyURL** parity on all SPA redirects |
| **3.2** | **Login-first sequencing** (product owner): React Login is PR-1 / Phase 0 for stakeholder demos; POST remains existing `/login`; 401 → React Login; PR plan reordered |

*End of design document.*
