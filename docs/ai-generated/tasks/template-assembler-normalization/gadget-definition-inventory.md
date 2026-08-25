# Gadget definition inventory (Phase 0)

**Date:** 2026-08-09  
**Question:** Are dashboard gadgets still shipped as XML definition files, and what does “out of Gadget XML” mean for packaging?

## Findings

### Registry still XML

Product still maintains a **gadget registry** XML:

- `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml`

It lists ~21 gadgets (Percussion + Deprecated groups), each with:

- `name`
- `baseuri` (e.g. `/cm/gadgets/repository/PercBlogsGadget`)
- `file` (e.g. `PercBlogsGadget.xml`)

### Per-gadget definition XML files are largely gone from the tree

Under both:

- `WebUI/src/main/webapp/cm/gadgets/repository/`
- `WebUI/war/gadgets/repository/`

the tree currently contains primarily **shared common assets** (CSS/images/uploadify).  
Repo-wide search for registered files such as `perc_welcome_gadget.xml`, `PercBlogsGadget.xml`, `perc_activity_gadget.xml` returns **no** non-`target` hits.

**Interpretation:** gadget **definition XML** is already mid-elimination or orphaned-registry state. Modernization work (see `docs/ai-generated/tasks/completed/GADGET-MODERNIZATION-ANALYSIS.md` and Home gadgets SPA) moved many gadgets toward React/SPA hosts while the **registry** still points at classic OpenSocial-style gadget XML paths.

### What “ship without Gadget XML definition files” should mean

| Artifact | Keep / change |
|----------|----------------|
| Classic OpenSocial gadget `*.xml` per gadget | **Eliminate** as product packaging (already largely absent) |
| `GadgetRegistry.xml` (or equivalent) | **Replace** with a first-class catalog (JSON/YAML in package, or DB + REST) authored by product — not a private gadget XML dialect |
| SPA gadget components under `WebUI` | **Keep** as the implementation; register via catalog |
| Shared `repository/common` assets | Keep until SPA gadgets no longer need them |

Gadgets are **not** assembly templates. They do not use Velocity/JEXL assembly. They share the broader “XML side-car definition” packaging smell with Widgets/Pages.

## Registry entries (from `GadgetRegistry.xml`)

### Group: Percussion

| Name | baseuri folder | file (registry) |
|------|----------------|-----------------|
| Assets By Status | PercAssetStatusGadget | PercAssetStatusGadget.xml |
| Blogs | PercBlogsGadget | PercBlogsGadget.xml |
| Bulk Upload | perc_bulk_file_upload_gadget | perc_bulk_file_upload_gadget.xml |
| Comments | perc_comments_gadget | perc_comments_gadget.xml |
| Cookie Consent | perc_cookie_consent_gadget | perc_cookie_consent_gadget.xml |
| Forms Tracker | PercFormTrackerGadget | PercFormTrackerGadget.xml |
| Global Variables | PercGlobalVariablesGadget | PercGlobalVariablesGadget.xml |
| Google Setup | perc_google_setup_gadget | perc_google_setup_gadget.xml |
| Iframe | perc_iframe_gadget | perc_iframe_gadget.xml |
| Pages By Status | perc_workflow_status_gadget | perc_workflow_status_gadget.xml |
| Process Monitor | PercProcessorMonitorGadget | PercProcessorMonitorGadget.xml |
| Reports | perc_reports_gadget | perc_reports_gadget.xml |
| SEO Audit | perc_seo_gadget | perc_seo_status_gadget.xml |
| Sitewide Framework | PercSiteFrameworkGadget | perc_sitewide_framework_gadget.xml |
| Traffic | perc_traffic_gadget | perc_traffic_gadget.xml |
| Welcome | cm1_welcome_gadget | perc_welcome_gadget.xml |
| What's Working | perc_effectiveness_gadget | perc_effectiveness_gadget.xml |

### Group: Deprecated

| Name | baseuri folder | file (registry) |
|------|----------------|-----------------|
| Activity | perc_activity_gadget | perc_activity_gadget.xml |
| Siteimprove | perc_site_improve_gadget | perc_site_improve_gadget.xml |
| Membership | perc_membership_gadget | perc_membership_gadget.xml |
| Widget Configuration | PercWidgetConfigGadget | PercWidgetConfigGadget.xml |

Redirect Management gadget assets already removed (issue #715 note in registry).

## Recommended Phase 0/3 actions for gadgets

1. Decide whether registry `file=` entries are dead and can be deleted/replaced immediately (fix for broken dashboard loads).
2. Replace `GadgetRegistry.xml` with a catalog format consumed by SPA Home/Dashboard (JSON under package or config).
3. Do **not** fold gadgets into assembly assemblers — separate “dashboard component catalog” track, same anti-XML packaging goal.
4. Cross-link Home acceptance / gadget SPA work so this track only owns **packaging/registry** cleanup, not full gadget UX rewrite.

## Gadget registry → catalog / package model (slice #2771)

Compiler for upgrade-input `GadgetRegistry.xml` → modern ship format (landed after cluster #2766):

| Artifact | Location |
|----------|----------|
| Parser / compiler / catalog IO | `modules/perc-packages/.../gadgetxml/PSGadgetRegistry*.java`, `PSGadgetCatalog*.java` |
| Aggregate catalog ship file | `gadget-catalog.json` (`PSGadgetCatalog.DEFAULT_CATALOG_FILE_NAME`) |
| Product modern catalog (authoring) | `modules/perc-packages/src/main/resources/catalogs/gadgets/gadget-catalog.json` |
| Per-gadget packages | `component-package.json` with `catalog.kind = "gadget"` (no CT/templates required) |
| Golden parity | Welcome (`cm1_welcome_gadget`) + full product catalog (21 entries) under `src/test/resources/gadgetxml/golden/` |
| Validator rule | `PSComponentPackageManifestValidator` — `catalog.kind=gadget` requires `catalog.title`; CT/templates optional |

**WebUI dual-load (#2788 / #3025):** runtime prefers `gadget-catalog.json` with legacy `GadgetRegistry.xml` fallback; INFO selection metrics and test-visible last-load source/entry count. **Still residual:** delete product/legacy `GadgetRegistry.xml` fallback only when Phase 5 criteria pass (#2852 / M2–M3) — not unattended mass-delete.

**G4 Packages ship-path inventory (#3581 / #3737):** `PSGadgetDefinitionXmlInventory` + Surefire fails CI if Gadget definition XML reappears under `Packages/**/sys__UserDependency--rxconfig/Gadgets` or `Packages/**/rxconfig/Gadgets` (waiver set empty after perc.Test page dual-ship exit; shared Page/Gadget list). This does **not** cover WebUI `GadgetRegistry.xml` (outside Packages ship paths).

## Related docs

- `docs/ai-generated/tasks/completed/GADGET-MODERNIZATION-ANALYSIS.md`
- `docs/ai-generated/tasks/home-acceptance-status.md`
- `docs/ai-generated/tasks/design-templates-item-types/README.md` (Home/Gadgets dependencies)
- [component-package-manifest.md](./component-package-manifest.md) (`catalog.kind = gadget`)
