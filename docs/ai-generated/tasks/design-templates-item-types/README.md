# Design, templates & item types — placeholder plan

| Field | Value |
|-------|--------|
| **Status** | **Placeholder** — planning only; not started as product surface |
| **Created** | 2026-07-28 |
| **Depends on** | Home Wave 1 (in progress); blog create partially blocked by template prerequisites |
| **Related** | [unified-ui-plan](../#000-unified-ui-plan/unified-ui-plan.md) Wave 3 “Design / Architecture”; [home-acceptance-status](../home-acceptance-status.md) (when present); PR #1577 Gadgets/Blogs |
| **Out of wave** | Full Active Assembly / page editor rewrite (larger than this track) |

## 1. Why this track exists

Home functional recovery exposed a hard dependency: **blogs (and several content flows) need Design-side artifacts that the old WebUI never fully generalized.**

Classic CM1-oriented WebUI effectively centered on:

| Classic focus | Examples |
|---------------|----------|
| **Pages** | Site folder pages, landing pages |
| **Page templates** | Layout + region widgets for pages |
| **Assets** | Shared assets driven by creatable widgets + content types |

The **new SPA** must also support, honestly and first-class:

| New / expanded | Notes |
|----------------|--------|
| **Non-page items** | Rhythmyx-style content items that are not CM1 “pages” |
| **Non-page templates** | Templates that are not only page-layout templates |
| **Non-asset items** | Items that are not shared assets under `/Assets` |
| **Rhythmyx-style items & templates** | Broader content type / template model beyond CM1 page+asset vocabulary |

This is **not** a Home subtask. It is a **separate product surface** (Design + template library + item-type awareness) that Home will *call into*. Capture gaps here so Home can stay focused while Design gains a head start.

## 2. Product intent (draft — refine later)

1. **Design SPA (or SPA-owned Design vertical)** — list/create/open templates; eventually edit layout without jQuery Design.
2. **Template type model** — distinguish page templates vs other template kinds; eligibility rules (e.g. blog list vs blog post) stay explicit.
3. **Item type model** — pages, assets, non-page / non-asset content; create paths and editors respect type.
4. **Public REST** — prefer `rest` module OpenAPI for anything new; internal sitemanage remains implementation detail.
5. **No dual-mode** — legacy Design is exit until SPA owns the job; then delete peer (unified-ui-plan rules).

## 3. Scope slices (suggested phasing)

Refine into specs/issues when the track is activated.

### Phase D0 — Inventory & contracts (docs + API survey)

- [ ] Inventory classic Design / template / architecture entry points (`view=design`, `editTemplate`, site architecture)
- [ ] Inventory template REST (`pagemanagement/template`, site templates summary)
- [ ] Inventory content type / item create paths (page, asset, generic item)
- [ ] Document widget-on-template model (definitionId, regions, `hasWidget`)
- [ ] Map “page / asset / other” to wire DTOs and SPA routes

### Phase D1 — Template library (SPA)

- [ ] List site templates (SPA)
- [ ] Create template from source/base (SPA or controlled REST + open)
- [ ] Open template (interim: legacy `editTemplate` exit is acceptable if explicit)
- [ ] Filter helpers already used by Home: templates containing given widget definition ids

### Phase D2 — Template type eligibility & blog base templates

- [ ] Blog **list** templates: must include **Blog List** widget (`percBlogIndexPage`)
- [ ] Blog **post** templates: must include **Blog Post** widget (`percBlogPost`)
- [ ] UX when site has no eligible templates (message + link to Design)
- [ ] Optional: seed/clone product base templates with those widgets (vs full visual editor)

### Phase D3 — Non-page / non-asset item & template types

- [ ] Glossary: Non-Page Template, Non-Asset Item, Rhythmyx item vs CM1 page/asset
- [ ] Create/open flows for non-page items
- [ ] Template types beyond page layout
- [ ] Permissions / roles (Designer vs Admin)

### Phase D4 — Design layout editor (large)

- [ ] Region tree + widget placement in React
- [ ] Save template / theme hooks
- [ ] Retire classic Design for accepted flows

**D4 is intentionally last** — do not block D1–D3 on full layout parity.

## 4. Gaps observed from Home (seed backlog)

See **[gaps-from-home.md](./gaps-from-home.md)** for the living list. Summary:

| Gap | Home impact | Design track |
|-----|-------------|--------------|
| No SPA Design surface | Nav still legacy exit | D1 |
| Blog templates need Blog List / Blog Post widgets | Cannot create blog section on bare Demo | D2 |
| Create Blog Post needs existing blog section | Home Create empty until gadget/section create | D1/D2 + Gadgets |
| Gadgets host was broken (dashboard URL); many gadgets still fake APIs | Home Gadgets incomplete | Gadgets wave (separate) + Design only where gadgets need templates |
| Template open = full editor complexity | Risk of under-scoping | D4 deferred |
| Classic WebUI mental model = Pages + Assets + Page templates only | SPA must not hardcode that trio | D3 |

## 5. Related Home work (do not fold into this track)

| Work | Status / note |
|------|----------------|
| Home Recent / Bookmarks / Library / Search / Create page | Largely landed (#1568–#1574) |
| Gadgets host + Blogs gadget + blog template **filter** | #1577 |
| FTS body HTML extract (assembly connection null) | #1561 residual — Engineering, not Design SPA |
| Publish SPA | Wave 2 after Home acceptance policy |

## 6. Open questions (capture answers here later)

1. Minimum **Accepted** Design for Home: template library + seed blog templates only, or full layout editor?
2. Where do **non-page templates** live in IA (Design vs Admin vs Content)?
3. Are **Rhythmyx items** created from Explorer, Design, or a new “Content types” surface?
4. Should blog base templates be **shipped** in product packages (install-time) vs created on first use?
5. Public OpenAPI: new `rest` resources for template eligibility vs keep internal pagemanagement only?

## 7. How to use this folder

- **While on Home:** append to `gaps-from-home.md` whenever Home hits a Design/template/item-type wall.
- **When starting Design:** promote D0→D1 into GitHub issues / Speckit feature; update Status to Active.
- **Do not** implement full Design under `fix/home-*` branches — keep PRs scoped.

## 8. Entry points (legacy — for D0 inventory)

| Entry | Path / note |
|-------|-------------|
| Design nav | `/cm/app/?view=design` (TopNav designer) |
| Architecture | `/cm/app/?view=arch` |
| Template edit | classic `editTemplate` / webmgt design flows |
| Template REST | `/services/pagemanagement/template/{id}`, site template summaries |
| Section/blog create | `/services/sitemanage/section` (`sectionType=blog`, list + post template ids) |
| Widget defs | e.g. packages `percBlogIndexPage`, `percBlogPost` |

---

*Placeholder only. No implementation commitment beyond tracking.*
