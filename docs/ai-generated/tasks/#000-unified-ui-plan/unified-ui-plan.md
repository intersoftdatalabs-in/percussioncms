# Unified UI Plan — React / TypeScript Only (Aggressive)

| Field | Value |
|-------|--------|
| **Status** | **Active — product direction (rev 4.1, 2026-07-27)** |
| **Supersedes** | Track A (Dojo→jQuery as product strategy), Track B dual-mode / soft cutover, bridge-first hybrid, dual production modes, “carry jQuery into React” |
| **Canonical SPA design** | [`#000-pure-react-spa/design.md`](../#000-pure-react-spa/design.md) (infra PRs 1–9) |
| **Module** | `WebUI/` (React + TypeScript + Vite) |
| **Stack** | React 19, TypeScript 5.8, Vite, Jetty WAR under `/cm/` |

---

## 1. Product locks (non-negotiable)

1. **The product UI is React + TypeScript.** New user-facing work ships in `WebUI/src/main/ts/` inside the SPA (`spa.jsp` / path routes). Not jQuery, not Knockout, not Dojo, not a new bridge island for a product page.
2. **Do not carry jQuery forward into the new UI.**  
   - SPA / modern bundle (`perc-modern-ui.js`) and all of `src/main/ts/**` are **jQuery-free**.  
   - No `import` of jQuery, no `window.$`, no jQuery UI / FancyTree / plugins from React.  
   - Reimplement needed behavior with React + typed REST (`api/*`).  
   - jQuery remaining in the WAR or in `frontend/package.json` is **only** for packing residual legacy pages (`build:legacy`) until those pages are deleted after SPA acceptance.  
   - **Forbidden:** wrapping jQuery widgets in React, calling legacy `$.perc_*` from TS, or loading jQuery on `spa.jsp` / login for “convenience.”
3. **No dual mode.** No feature flag that keeps classic and modern as peer production UIs for the same feature. Rollback = git revert / redeploy.
4. **No new bridges.** `PercModernUI.mount` is **legacy debt**, not a pattern for new work. Do not add mounts for product navigation. Prefer SPA routes (and SPA dialogs when openers are ready).
5. **No “shell counts as done.”** A screen is **not** accepted until features work end-to-end (load data, act, navigate, errors, roles). Empty chrome is a defect, not a milestone.
6. **Screen-by-screen, feature-by-feature.** Prove one surface fully, then delete its legacy peer when safe, then move on. Home first.
7. **Server entry stays query-only for redirects/login.** `spa.jsp?entry=…` (or path after client handoff). Never `Location: …#/…`.

---

## 2. What already shipped (infra — not functional acceptance)

SPA program PR-1…PR-9 built the **front door and shell**:

| Wave | What landed | Honest status |
|------|-------------|----------------|
| Login | React `LoginPage` on `rxlogin.jsp` | **Product path** — verify CSRF/errors/return on real CMS |
| App shell | TopNav, bootstrap, 401→login, BrowserRouter + filter | **Chrome only** |
| Routes | Home, Publish, Workflow, Admin, WB, Explorer mounted | **Embedded shells** — functional depth varies; many not product-ready |
| Cutover | `index.jsp` → SPA for modern views; obsolete `*Modern.jsp` product hosts deleted | **Routing done** |
| Gadgets | React gadgets section on Home | **Compose done** — verify widgets work |

**Vendor Dojo library** was removed from the tree (security). Residual `ps.*` Active Assembly code and jQuery/JSF/GWT layers still exist as **legacy debt to eliminate or replace in React**, not as a long-lived second product UI.

---

## 3. Target architecture (single product path)

```
Browser
  │
  ├─ Public:  /rxlogin.jsp  → React LoginPage  → POST /login
  │
  └─ Auth:    /cm/app/spa.jsp?entry=…  OR  /cm/app/{home|publish|…}
              → React App (BrowserRouter + AppLayout + TopNav)
              → Feature route → *Shell / feature modules (TS)
              → REST (typed api/*) + CSRF + TMX
```

| Concern | Rule |
|---------|------|
| Product navigation | SPA only |
| Feature code | `WebUI/src/main/ts/**` |
| New pages | SPA route (+ server allowlist if deep-linked) |
| Legacy full-page exits | Temporary only until that feature is React; then delete exit |
| Residual `PercModernUI.mount` | Only until that host is rewritten or deleted — **no new hosts** |
| jQuery / Knockout / JSF / GWT / residual Dojo-shaped `ps.*` | **Debt only.** Never import into SPA. Touch only to fix blockers or to **delete** after React acceptance |

---

## 4. Screen register (order of work)

Status meanings:

| Status | Meaning |
|--------|---------|
| **Shell** | Route/chrome exists; features incomplete or broken |
| **Partial** | Some features work; gaps known |
| **Accepted** | Passes acceptance checklist on a real CMS; legacy peer removed or explicitly N/A |
| **Legacy exit** | Still full-page leave SPA (must become SPA or be retired) |

### Wave 0 — Front door (keep green)

| Surface | Entry | Status | Notes |
|---------|-------|--------|-------|
| Login | `/rxlogin.jsp` | Partial → prove | CSRF, locales, errors, return to SPA |
| App chrome | TopNav / user menu / 401 | Partial → prove | Roles hide/show; logout |

### Wave 1 — **Home first (current focus)**

| Surface | Entry | Status | Notes |
|---------|-------|--------|-------|
| **Home** | `/cm/app/home`, `?entry=home` | **Shell — not accepted** | User report: shell visible, **nothing functional** |

**Home sections (each must work):**

| Section | Route | Must work |
|---------|-------|-----------|
| Recent | `/home` / `/home/recent` | List loads; open item → editor (or agreed SPA path); empty/error states |
| Bookmarks | `/home/bookmarks` | List; open; empty/error |
| Library | `/home/library` | Browse/list; open; empty/error |
| Search | `/home/search` | Query; results; open; empty/error |
| Create | `/home/create` | Page/asset/blog wizards complete successfully |
| Gadgets | `/home/gadgets` | Widgets load/configure/persist as product requires |

**Home acceptance checklist (all required):**

- [ ] Fresh login lands on Home (correct role homepage metadata)
- [ ] Section tabs switch and deep links (`/home/gadgets`) work + refresh (path URL)
- [ ] Each section loads real data against live REST (not forever loading / silent fail)
- [ ] Open item navigates to a working editor path
- [ ] Create wizards finish without dead ends
- [ ] Gadgets usable for default set (no blank shell)
- [ ] TMX labels (no raw keys for primary chrome)
- [ ] Non-admin / admin differences correct
- [ ] Vitest for non-trivial logic; manual smoke on docker/dev CMS recorded
- [ ] No dependency on classic CUI Home for the same job

**Home exit criteria:** checklist green → mark Home **Accepted** → remove any remaining classic Home peers still reachable from product nav.

### Wave 2 — Primary SPA features (after Home accepted)

Order (adjust only with product reason):

1. **Publish** (`PublishingShell`) — sites, status, logs, design, runtime  
2. **Explorer** (`ContentExplorerShell`) — tree, list, open, core actions  
3. **Admin tools** (`AdminShell`) — tasks, logs, notifications, tools  
4. **Workflow admin** (`WorkflowAdminShell`) — workflows, roles, users, categories  
5. **Widget Builder** (`WidgetBuilderApp`) — if still product-enabled  

Same rule: **feature checklist per screen**, then delete legacy peer.

### Wave 3 — Remaining product surfaces (SPA or retire)

| Surface | Today | Direction |
|---------|--------|-----------|
| Editor (webmgt / AA) | Legacy exit | React rewrite (large); interim: fix critical bugs only |
| Design / Architecture | Legacy exit | React or retire paths |
| Residual dialog hosts (pickers, search panel, US7 tools) | Bridge mounts | SPA dialogs / explorer actions; then **delete** hosts |
| Package Manager (GWT) | Legacy | React when scheduled |
| JSF admin/publishing residual | Packaging | Prefer SPA Admin/Publish; delete dead JSF |
| Desktop CE / Eclipse | Out of band | Not SPA; do not block Home |

---

## 5. Working rules for agents & developers

### DO

- Implement features in **React + TypeScript** under `WebUI/src/main/ts/`.
- Use SPA routes and existing REST + CSRF + TMX patterns (**no jQuery**).
- For each screen: **acceptance checklist** + Vitest for non-trivial logic + real CMS smoke.
- Prefer fixing broken SPA behavior over wiring another jQuery page.
- After a surface is **Accepted**, delete obsolete hosts/scripts (including jQuery packs for that surface) in the same or immediate follow-up PR.

### DO NOT

- Add dual-mode flags or “keep classic as production peer.”
- Add new `PercModernUI.mount` product pages.
- **Import or depend on jQuery from the SPA / modern bundle / `src/main/ts`.**
- Wrap jQuery widgets in React or call `$.perc_*` from TypeScript.
- Add new Dojo or new Knockout features.
- Expand jQuery except critical security/hotfix on a **legacy** surface not yet replaced.
- Call a route “done” because the shell renders.

### Residual bridge / legacy code

- Documented only as **delete candidates** (`#000-pure-react-spa/residual-bridge-embeds.md`).
- Allowed to touch for: security, compile, or **removing** after React replacement.
- Not a place to invest new product UX.

---

## 6. Dojo / Track A historical note

Earlier plans used **Track A = Dojo→jQuery** and **Track B = React**. That split is **retired as product strategy**.

| Fact | Status |
|------|--------|
| Vendored Dojo 0.4.3 under ApplicationFiles | **Removed** (security; e.g. #1197) |
| Residual `ps.*` AA code / compat shims | **Debt** — replace with React or delete when AA is rewritten; **not** a jQuery product investment track |
| install-dojo.xml / residual `dojo.*` call sites | Cleanup when safe; not a parallel roadmap |

---

## 7. Immediate next work (execution)

1. **Home functional recovery (Wave 1)** — diagnose why shell is empty/non-functional (API paths, CSRF, bootstrap, section routing, open-item, create wizards, gadgets). Fix until Home checklist is green.  
2. Update residual docs/AGENTS as Home acceptance lands.  
3. Then Wave 2 screen by screen with the same bar.

Infra PRs (login shell, cutover, path URLs) stay; **value is measured by accepted screens**, starting with Home.

---

## 8. Verification standard (every screen)

| Layer | Required |
|-------|----------|
| Manual | Checklist on real CMS (docker/dev) |
| Automated | Vitest for logic; Playwright where already used for the surface |
| Build | `cd WebUI && ../mvn-env.sh clean install` before PR |
| Review | Erlang pre-commit on authored code |
| Legacy | Explicit “delete / keep temporary” note in PR |

---

## 9. Related artifacts

| Artifact | Role |
|----------|------|
| [`#000-pure-react-spa/design.md`](../#000-pure-react-spa/design.md) | SPA entry, bootstrap, router, PR 1–9 history |
| [`#000-pure-react-spa/residual-bridge-embeds.md`](../#000-pure-react-spa/residual-bridge-embeds.md) | Residual mounts to eliminate |
| [`WebUI/AGENTS.md`](../../../WebUI/AGENTS.md) | Module agent rules (React-only product) |
| [`ui-layer-inventory.md`](ui-layer-inventory.md) | Historical inventory (may lag; this plan wins on direction) |

---

## 10. Key decisions (rev 4.0)

| ID | Decision |
|----|----------|
| KD-R1 | Product UI = React + TypeScript SPA only |
| KD-R2 | **No jQuery in the new UI** (modern bundle / `src/main/ts`); jQuery is delete-bound legacy only |
| KD-R3 | No dual mode / no soft feature-flag cutover |
| KD-R4 | No new PercModernUI product hosts |
| KD-R5 | Shell ≠ done; feature acceptance required |
| KD-R6 | Home first, then Publish → Explorer → Admin → Workflow → WB |
| KD-R7 | Track A Dojo→jQuery as product strategy is retired; vendor Dojo already gone |
| KD-R8 | Server redirects stay query `entry` contract; client uses path URLs |
