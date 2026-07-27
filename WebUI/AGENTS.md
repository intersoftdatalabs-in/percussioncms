# WebUI Module Agent Guidelines

Read the root [@AGENTS.md](../AGENTS.md) for general guidelines. This file contains WebUI-specific rules that supersede general rules for this module.

## Module Overview

**WebUI** is the CMS browser UI WAR. The **product user interface is React + TypeScript** (Vite), delivered as a pure SPA under `/cm/app/`.

| Field | Value |
|-------|--------|
| **Product UI** | React 19 + TypeScript + Vite → `/cm/modern/assets/perc-modern-ui.js` |
| **Canonical source** | `WebUI/src/main/ts/` (build cwd: `WebUI/src/main/frontend/`) |
| **SPA document** | `cm/app/spa.jsp` + path routes (`/cm/app/home`, …) via `BrowserRouter` + `PSWebUiSpaFallbackFilter` |
| **Login** | React `LoginPage` on `rxlogin.jsp` (POST `/login`) |
| **Direction plan** | [`docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md`](../docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md) **rev 4.1** |
| **SPA infra design** | [`docs/ai-generated/tasks/#000-pure-react-spa/`](../docs/ai-generated/tasks/#000-pure-react-spa/) |

### Product locks (agents must follow)

1. **React + TypeScript only** for product UI work. New features ship as SPA routes/modules under `src/main/ts/`.
2. **Do not carry jQuery (or Knockout/Dojo) into the new UI.**  
   - **Forbidden** in `WebUI/src/main/ts/**`, SPA hosts (`spa.jsp`, `rxlogin.jsp` product path), and the **modern** Vite bundle (`perc-modern-ui.js`).  
   - No `import "jquery"`, no `window.$` / `jQuery` usage, no jQuery plugins, no FancyTree-via-jQuery in React.  
   - Use React state, browser APIs, and typed `api/*` REST instead.  
   - jQuery packages under `frontend/package.json` exist **only** to pack **residual legacy** pages (`build:legacy`) until those pages are deleted — **not** for SPA features.
3. **No dual mode.** No feature flags that keep classic and modern as peer production UIs for the same job.
4. **No new bridges.** Do not add `PercModernUI.mount` product hosts. Bridge mounts are residual debt only (see residual-bridge-embeds doc).
5. **Shell ≠ done.** A screen is incomplete until features work (data, actions, navigation, errors, roles) on a real CMS.
6. **Screen-by-screen.** Prove one surface fully before treating it as accepted. **Current focus: Home.**
7. **Server deep links / login return:** query `spa.jsp?entry=…` only (never `#` in `Location`). Client may use path URLs after handoff.

### Current status (honest)

| Area | Status |
|------|--------|
| SPA shell, login front door, routing cutover, path URLs (PR-1…9) | **Infra landed** |
| Home, Publish, Explorer, Admin, Workflow, WB **as usable product** | **Not accepted** — shells/routes exist; **Home is first functional gate** (user: shell visible, not functional) |
| Residual jQuery pages / AA / dialog hosts / JSF / GWT | **Legacy debt** — replace with React or delete; **not** imported into SPA |
| Vendored Dojo library | **Removed** from ApplicationFiles; residual `ps.*` AA code may remain as debt |

**Do not** describe Track A (Dojo→jQuery) or Track B dual-mode as the active product plan. That framing is retired (see unified-ui-plan rev 4.1).  
**Do not** “modernize” by wrapping jQuery widgets in React or calling `$` from TS.

---

## Active work order

1. **Home** — make Recent / Bookmarks / Library / Search / Create / Gadgets fully functional; pass Home acceptance checklist in the unified UI plan.
2. **Publish** → **Explorer** → **Admin** → **Workflow** → **Widget Builder** (same bar).
3. **Legacy exits** (editor, design, arch, residual dialogs) — SPA rewrite or retire; delete bridges when openers move.

Checklist and register: unified-ui-plan rev 4.1 §4–§5.

---

## What may still exist in the WAR (not product direction)

These are **not** peer product UIs. Agents may touch them only for security/hotfix or to remove after React replacement:

| Layer | Tech | Policy |
|-------|------|--------|
| Residual bridge dialogs / embeds | React via `PercModernUI.mount` on old JSPs | Delete when SPA owns the job; **no new mounts** |
| WebUI legacy pages | jQuery / Backbone | No new features |
| Contributor UI leftovers | Knockout | No new features |
| Active Assembly / residual `ps.*` | Historical Dojo-shaped code + shims | No new Dojo; replace with React when editor wave runs |
| Package Manager | GWT | Legacy until React wave |
| JSF residual packaging | JSF | Prefer SPA Admin/Publish |

Historical inventory (may lag): [`docs/ai-generated/tasks/#000-unified-ui-plan/ui-layer-inventory.md`](../docs/ai-generated/tasks/#000-unified-ui-plan/ui-layer-inventory.md).

---

## Directory Structure

```
WebUI/
├── src/
│   ├── main/
│   │   ├── java/                        # Servlets, filters (e.g. SPA fallback)
│   │   ├── resources/
│   │   ├── webapp/                      # WAR root (JSP, legacy assets)
│   │   │   ├── cm/app/                  # spa.jsp, index.jsp, residual legacy JSPs
│   │   │   ├── cm/modern/               # Generated React bundle (build output)
│   │   │   └── WEB-INF/
│   │   ├── ts/                          # ← Product UI TypeScript/React source
│   │   │   ├── app/                     # SPA App, routes, layout, deep links
│   │   │   ├── home/, publishing/, …    # Feature modules
│   │   │   ├── api/, i18n/, ui-themes/
│   │   │   ├── bridge.ts, registry.ts   # Residual embed support only
│   │   │   └── index.ts
│   │   └── frontend/                    # Vite/npm workingDirectory
│   └── test/
│       ├── java/
│       └── ts/                          # Vitest
├── pom.xml
└── target/generated-webui/              # Build outputs (not git)
```

---

## Build Pipeline

### Standalone module (preferred pre-PR)

```bash
cd WebUI
../mvn-env.sh clean install
```

### Frontend-only iteration

```bash
cd WebUI/src/main/frontend
npm run build:modern     # tsc + vite
npm run test             # Vitest
npm run dev              # HMR when wired to a running CMS
```

### Hot copy to docker CMS (example)

```bash
cp WebUI/target/generated-webui/cm/modern/assets/perc-modern-ui.js \
   /opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.js
cp WebUI/target/generated-webui/cm/modern/assets/perc-modern-ui.css \
   /opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.css
# Path routes: spa.jsp + filter are on the server; rebuild WAR or copy JSP/filter classes as needed
```

---

## Coding Standards

### React + TypeScript (product code)

- Functional components + hooks; strict TypeScript (avoid `any`)
- Feature modules under `src/main/ts/<feature>/`
- REST via `api/client.ts` + feature APIs; CSRF from bootstrap / `OWASP_CSRFTOKEN`
- i18n via TMX `message()` helpers — no raw keys in primary chrome
- Styles: CSS modules preferred; theme tokens via `ui-themes`
- Tests: Vitest for non-trivial logic; update tests with every behavior change
- SPA routing: `app/routes.tsx` + deep-link allowlists; server entry allowlists stay in lockstep

### Java (WebUI)

- Thin filters/servlets; portable paths (`Path` / `Files` when filesystem)
- New filters (e.g. SPA fallback) must have unit tests for allowlists

### Legacy (jQuery, Knockout, residual AA, bridge)

- **Do not** add product features
- **Do not** add Dojo
- **Do not** add new `PercModernUI.mount` hosts
- Security/hotfix only, or delete after React acceptance

---

## Testing Guidelines

### React/TypeScript (Vitest)

```bash
cd WebUI/src/main/frontend
npm run test
```

### Acceptance (product)

Follow the **screen checklist** in the unified UI plan. Home must pass on a **real CMS** (not only unit tests) before Home is marked accepted.

### Pre-PR

- `cd WebUI && ../mvn-env.sh clean install` — BUILD SUCCESS, tests pass, no new warnings
- Erlang review on authored diffs
- PR body: commands run + test counts + which checklist items were verified

---

## Agent DO / DO NOT (summary)

| DO | DO NOT |
|----|--------|
| Ship SPA React features | Dual-mode / classic peer UIs |
| Fix Home until functional | Declare “done” because shell renders |
| Delete obsolete hosts after acceptance | New bridge product pages |
| Align allowlists (TS + JSP/filter) | New Dojo / Knockout / jQuery product code |
| Document acceptance evidence | Invent REST APIs without checking existing `api/` + `rest` module |

---

## Related docs

- [Unified UI Plan rev 4.0](../docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md) — **direction of record**
- [Pure React SPA design](../docs/ai-generated/tasks/#000-pure-react-spa/design.md) — entry contract, bootstrap, PR 1–9
- [Residual bridge embeds](../docs/ai-generated/tasks/#000-pure-react-spa/residual-bridge-embeds.md) — delete list
- Root [AGENTS.md](../AGENTS.md) — monorepo, Maven, cross-platform, Erlang
