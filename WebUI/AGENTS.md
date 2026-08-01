# WebUI Module Agent Guidelines

Read the root [@AGENTS.md](../AGENTS.md) for general guidelines. This file contains WebUI-specific rules that supersede general rules for this module.

## Module Overview

**WebUI** is the CMS browser UI WAR. The **product user interface is React + TypeScript** (Vite), delivered as a pure SPA under `/cm/app/`.

|        Field         |                                                                        Value                                                                        |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| **Product UI**       | React 19 + TypeScript + Vite → `/cm/modern/assets/perc-modern-ui.js`                                                                                |
| **Canonical source** | `WebUI/src/main/ts/` (build cwd: `WebUI/src/main/frontend/`)                                                                                        |
| **SPA document**     | `cm/app/spa.jsp` + path routes (`/cm/app/home`, …) via `BrowserRouter` + `PSWebUiSpaFallbackFilter`                                                 |
| **Login**            | React `LoginPage` on `rxlogin.jsp` (POST `/login`)                                                                                                  |
| **Direction plan**   | [`docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md`](../docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md) **rev 4.1** |
| **SPA infra design** | [`docs/ai-generated/tasks/#000-pure-react-spa/`](../docs/ai-generated/tasks/#000-pure-react-spa/)                                                   |

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
8. **Playwright is mandatory for UI screen changes.** Any change that alters user-visible UI behavior on a product screen must create or update Playwright specs in `modules/perc-qa-automation/` (see **Playwright (HARD GATE)** below). Vitest alone is not enough.

### Current status (honest)

|                                Area                                |                                                     Status                                                      |
|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| SPA shell, login front door, routing cutover, path URLs (PR-1…9)   | **Infra landed**                                                                                                |
| Home, Publish, Explorer, Admin, Workflow, WB **as usable product** | **Not accepted** — shells/routes exist; **Home is first functional gate** (user: shell visible, not functional) |
| Residual jQuery pages / AA / dialog hosts / JSF / GWT              | **Legacy debt** — replace with React or delete; **not** imported into SPA                                       |
| Vendored Dojo library                                              | **Removed** from ApplicationFiles; residual `ps.*` AA code may remain as debt                                   |

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

|               Layer               |                    Tech                    |                         Policy                          |
|-----------------------------------|--------------------------------------------|---------------------------------------------------------|
| Residual bridge dialogs / embeds  | React via `PercModernUI.mount` on old JSPs | Delete when SPA owns the job; **no new mounts**         |
| WebUI legacy pages                | jQuery / Backbone                          | No new features; **never** depend on from `src/main/ts` |
| Contributor UI leftovers          | Knockout                                   | No new features                                         |
| Active Assembly / residual `ps.*` | Historical Dojo-shaped code + shims        | No new Dojo; replace with React when editor wave runs   |
| Package Manager                   | GWT                                        | Legacy until React wave                                 |
| JSF residual packaging            | JSF                                        | Prefer SPA Admin/Publish                                |

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
../mvnw clean install
```

### Frontend-only iteration

```bash
cd WebUI/src/main/frontend
npm run build:modern     # tsc + vite
npm run test             # Vitest (also runs in Maven test phase — see Testing)
npm run dev              # HMR when wired to a running CMS
```

### Hot copy to CMS install (dev mode)

**Dev mode:** CMS is a **local install** on the machine; docker **binds** to it. Copy artifacts into that install and re-run Playwright — **no restart** for typical JS/CSS/JSP.  
**QA mode:** fully containerized stack; pass/fail only — no host-install hot copy.  
See [`docs/developer-module/workbench-rest-and-qa-modes.md`](../docs/developer-module/workbench-rest-and-qa-modes.md).

```bash
# Paths: use your DEV_PERCUSSION_INSTALL (e.g. /opt/Percussion or C:\Installs\…)
cp WebUI/target/generated-webui/cm/modern/assets/perc-modern-ui.js \
   "$DEV_PERCUSSION_INSTALL/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.js"
cp WebUI/target/generated-webui/cm/modern/assets/perc-modern-ui.css \
   "$DEV_PERCUSSION_INSTALL/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.css"
# Path routes: spa.jsp + filter are on the server; rebuild WAR or copy JSP/filter classes as needed
```

---

## Coding Standards

### React + TypeScript (product code)

- Functional components + hooks; strict TypeScript (avoid `any`)
- Feature modules under `src/main/ts/<feature>/`
- REST via `api/client.ts` + feature APIs; CSRF from bootstrap / `OWASP_CSRFTOKEN`
- i18n via TMX `message()` helpers — no raw keys in primary chrome
- **Crowdsource translation (alpha, third-party):** vendored `@mkd/language` under `WebUI/vendor/mkd-language/`. Adapter: `src/main/ts/i18n/mkdLanguage.ts`. Keys come from **tracked** `message()` (`createTrackedMessage` / `getTrackedMessageId`) — do **not** mass-annotate templates with `data-i18n-key`. Optional `i18nKeyAttr` only for collisions / non-`message` chrome. Mark user content with `data-mkd-lang-ignore` / `mkdLangIgnoreProps()` (explorer names, search hit titles, usernames, iframe gadgets) so triggers stay on product chrome. **Opt-in only:** `?mkdLang=1` or `localStorage.perc-mkd-lang=1`. Default submit is no-op; popover z-index is 20000. Do not edit vendored `dist/`; refresh procedure is in the vendor README. Treat mkd-language / mkd-gcm as external — no product design notes for that stack in this monorepo.
- Styles: CSS modules preferred; theme tokens via `ui-themes`
- Tests: **two layers** — Vitest for unit/component logic **and** Playwright for live-CMS screen behavior (see **Playwright (HARD GATE)**)
- Prefer stable `data-testid` on interactive chrome so Playwright selectors stay reliable
- SPA routing: `app/routes.tsx` + deep-link allowlists; server entry allowlists stay in lockstep
- **No jQuery** — see product lock #2. If a legacy page used `$('…')` or FancyTree, reimplement in React or use a non-jQuery primitive.

### Java (WebUI)

- Thin filters/servlets; portable paths (`Path` / `Files` when filesystem)
- New filters (e.g. SPA fallback) must have unit tests for allowlists

### Legacy (jQuery, Knockout, residual AA, bridge)

- **Do not** add product features
- **Do not** add Dojo
- **Do not** add new `PercModernUI.mount` hosts
- **Do not** pull jQuery into the modern SPA bundle or TS sources
- Security/hotfix only, or delete after React acceptance

---

## Testing Guidelines

### React/TypeScript (Vitest)

**Maven (preferred pre-PR / CI):** WebUI’s `frontend-maven-plugin` runs `npm run test` (Vitest) in the **`test`** phase after the modern/legacy builds. A failing Vitest suite fails the module build the same way Surefire does for Java. Skip with `-DskipTests` (or `-Dmaven.test.skip=true`) like other unit tests.

```bash
cd WebUI
../mvnw test
# or full gate:
../mvnw clean install
```

**Frontend-only iteration:**

```bash
cd WebUI/src/main/frontend
npm run test
```

Vitest covers pure logic, component contracts, and mocked API paths under `WebUI/src/test/ts/`. It does **not** replace live-CMS UI verification.

### Playwright (HARD GATE) — UI screens

**When modifying a product UI screen, agents MUST create or update Playwright automation** in [`modules/perc-qa-automation/`](../modules/perc-qa-automation/) so the changed user-visible behavior is covered against a running CMS.

|         Must add/update Playwright when…          |                            Examples                             |
|---------------------------------------------------|-----------------------------------------------------------------|
| User-visible chrome or copy changes               | Labels, buttons, titles, empty states, i18n after locale change |
| Interaction / navigation changes                  | Form submit, dropdown selection, routing, dialogs, menus        |
| New or changed screen flows                       | Login, Home sections, Publish, Explorer, Admin, Workflow, WB    |
| Bug fixes that alter what the user sees or can do | GH-style UI bugs (#1608, #1609, …)                              |

| Playwright not required (still need Vitest/Java tests as applicable) |                 Examples                  |
|----------------------------------------------------------------------|-------------------------------------------|
| Pure refactors with no UI behavior change                            | Rename internal helper, type-only cleanup |
| CSS-only polish with no behavioral selector/flow change              | Spacing tweak with no layout/logic impact |
| Non-UI WebUI Java (filters, servlets) with no screen change          | SPA fallback allowlist unit tests only    |

#### Requirements

1. **Module:** `modules/perc-qa-automation/frontend/tests/` (see that module’s [`AGENTS.md`](../modules/perc-qa-automation/AGENTS.md)).
2. **Create or extend** a spec that asserts the **new/changed behavior** (not only “page loads”).
3. Prefer **`data-testid`** (and existing helpers in `tests/helpers/`) over brittle CSS/XPath.
4. Run against a live CMS when validating (e.g. `/opt/Percussion` or docker dev):

   ```bash
   cd modules/perc-qa-automation/frontend
   npm test -- tests/<relevant-spec>.spec.js
   ```
5. **Same PR** as the WebUI change when practical (or a tightly stacked follow-up PR linked from the feature PR). Do not treat the feature as done with Vitest-only coverage.
6. **PR evidence:** name the Playwright file(s), command(s) run, and pass/fail. If Playwright could not be run (no CMS), say so explicitly and still land the test code so CI/dev can execute it.

Naming conventions (perc-qa-automation):

- Screen / feature: `tests/<area>.spec.js` or `tests/workflows/<feature>.spec.js`
- Bug regression: `tests/bugs/bug-<issue-id>.spec.js` (e.g. `bug-1608-1609-login-locale.spec.js`)

Root monorepo rule still applies: **unit tests for every logic change**. Playwright is **additional** for UI screens, not a substitute for Vitest.

### Acceptance (product)

Follow the **screen checklist** in the unified UI plan. Home must pass on a **real CMS** (not only unit tests) before Home is marked accepted. Playwright specs are the automated record of that acceptance loop.

### Pre-PR

- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS, tests pass, no new warnings
- Vitest for touched React/TS behavior
- **Playwright create/update** for any product screen UI change (HARD GATE above)
- Erlang review on authored diffs
- PR body: commands run + test counts + Playwright specs + which checklist items were verified

---

## Agent DO / DO NOT (summary)

|                         DO                         |                               DO NOT                                |
|----------------------------------------------------|---------------------------------------------------------------------|
| Ship SPA React features                            | Dual-mode / classic peer UIs                                        |
| Fix Home until functional                          | Declare “done” because shell renders                                |
| Delete obsolete hosts after acceptance             | New bridge product pages                                            |
| Align allowlists (TS + JSP/filter)                 | **jQuery / `$` / jQuery plugins in `src/main/ts` or modern bundle** |
| Document acceptance evidence                       | New Dojo / Knockout product code                                    |
| Typed `api/*` + React UI                           | Wrap jQuery widgets in React “for speed”                            |
| **Create/update Playwright for UI screen changes** | Ship UI behavior with Vitest-only coverage                          |

---

## Related docs

- [Unified UI Plan rev 4.0](../docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md) — **direction of record**
- [Pure React SPA design](../docs/ai-generated/tasks/#000-pure-react-spa/design.md) — entry contract, bootstrap, PR 1–9
- [Residual bridge embeds](../docs/ai-generated/tasks/#000-pure-react-spa/residual-bridge-embeds.md) — delete list
- [perc-qa-automation AGENTS](../modules/perc-qa-automation/AGENTS.md) — Playwright module, helpers, run commands
- Root [AGENTS.md](../AGENTS.md) — monorepo, Maven, cross-platform, Erlang

