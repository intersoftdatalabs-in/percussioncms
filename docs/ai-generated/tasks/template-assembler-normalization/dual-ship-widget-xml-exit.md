# Dual-ship Widget XML exit checklist

| Field | Value |
|-------|--------|
| **Status** | Active — batch A + B + C modern authoring roots landed; **batch A ship-exit** (#2883) stops committing install Widget XML |
| **Parent** | [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) · Grandparent [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Related** | Compiler #2751 / #2772 / #2789 / #2802 · Shim #2752 · Phase 5 #2632 · Page dual-ship #2786 / native #2806 · ship-exit #2883 / #2884 / #2885 |
| **Code** | `PSWidgetXmlDualShip`, `PSWidgetXmlInstallEmitter`, `PSWidgetXmlCompiler`, `PSLegacyDefinitionXmlShim` |

## Purpose

Batches A/B/C **author** modern `widgets/<stem>/component-package.json` + template sources (ADR-004) so dual-run selection prefers the Component Package Manifest when both exist.

**Batch A ship-exit (#2883):** product source trees for base/core packages **no longer commit** `sys__UserDependency--rxconfig/Widgets/*.xml`. Package build materializes install Widget XML from modern roots via `PSWidgetXmlInstallEmitter` / `PSPackageBuilder` so deployer / `PSWidgetDao` still receive the legacy wire format. Runtime shim remains until Phase 5 criteria.

**Batches B/C:** still dual-ship committed install Widget XML until their ship-exit residuals (#2884 / #2885).

## Authoring vs install

| Layer | Batch A (after #2883) | Batch B + C | Other |
|-------|------------------------|-------------|--------|
| **Authoring truth** | `widgets/<stem>/` modern only | modern roots + dual-ship XML | `perc.Test` residual (#2830) |
| **Install wire format** | Materialized at package-build from modern | Committed dual-ship XML | Dual-ship XML |
| **Selection** | Modern roots (shim prefers modern) | Prefers modern when both exist | Legacy until modern roots land |

### Configuration / APIs

| Knob / API | Notes |
|------------|--------|
| `PSWidgetXmlDualShip.materializeModernWidgetSources(packageDir)` | Widget XML → `widgets/` (migration / refresh) |
| `PSWidgetXmlDualShip.materializeModernBatchA/B/C(packagesRoot)` | Named batch materialize modern |
| `PSWidgetXmlInstallEmitter.materializeInstallWidgetXml(packageDir)` | Modern-only → install Widget XML (no-op if committed XML present) |
| `PSWidgetXmlDualShip.materializeInstallWidgetXml` | Delegates to install emitter (#2883) |
| `PSWidgetXmlDualShip.hasModernWidgetSources` / `compileModernWidgets` | Product parity tests |
| CLI | `PSWidgetXmlDualShip materialize-modern\|…-batch-a/b/c\|materialize-install <path>` |

Policy alignment: modern preferred in `PSLegacyDefinitionXmlShim` (root `component-package.json` **or** `widgets/<stem>/component-package.json`).

## Packages on modern dual-ship authoring (batch A)

| Package | Widgets | Notes |
|---------|---------|-------|
| `perc.baseWidgets` | percSimpleText, percRichText, percRawHtml | Core content widgets; goldens exist |
| `perc.defaultLanguage` | percDefaultLang, percLocalLang | Multi-widget language package |
| `perc.eventWidget` | percEvent | Content CT; golden |
| `perc.openGraphWidget` | percOpenGraph | Social meta |
| `perc.twitterSummaryCards` | percTwitterSummaryCards | Social meta |

**Batch A total:** 5 packages · **8** widgets with modern roots.

**Batch A ship-exit (#2883):** committed install Widget XML **removed** from product source (before **48** → after **40** product Widget def XML files). Install XML is regenerated at package-build time only.

## Packages on modern dual-ship authoring (batch B)

| Package | Widgets | Notes |
|---------|---------|-------|
| `perc.widget.title` | percTitle | High-traffic (#2772) |
| `perc.widgets.lists` | simplePageAutoList, simpleTextAutoList | High-traffic |
| `perc.widgets.nav` | percNavBar, percNavBreadcrumb | High-traffic chrome |
| `perc.FileAssetWidget` | percFile | High-traffic |
| `perc.widgets.image` | percImage | High-traffic |
| `perc.widgets.blog` | percBlogPost | Residual long-tail (#2789) |
| `perc.widget.calendar` | percCalendar, percCalendarTwo | Residual |
| `perc.widget.directory` | percDepartment, percDirectory, percOrganization, percPerson | Residual multi-widget |
| `perc.widget.socialButtons` | percSocialButtons | Residual |
| `perc.widget.form` | percForm | Residual; golden |
| `perc.widget.poll` | percPoll | Residual; golden |
| `perc.widget.login` | percLogin | Residual |
| `perc.widget.rss` | percRss | Residual |
| `perc.widget.iframe` | percIframe | Residual; golden |

**Batch B total:** 14 packages · **20** widgets with modern roots.

## Packages on modern dual-ship authoring (batch C)

| Package | Widgets | Notes |
|---------|---------|-------|
| `perc.ImageAutoListWidget` | percImageAutoList | Remaining product residual (#2802 / #2844) |
| `perc.PageAutoListWidget` | percPageAutoList | Auto-list |
| `perc.widget.fileAutoList` | percFileAutoList | Auto-list |
| `perc.widget.blogIndexPage` | percBlogIndexPage | Blog companion |
| `perc.widget.archiveList` | percArchiveList | Blog companion |
| `perc.widget.categoryList` | percCategoryList | Blog companion |
| `perc.widget.taglist` | percTagList | Blog companion |
| `perc.widget.MostReadBlogPosts` | percMostReadBlogPosts | Blog companion |
| `perc.widgets.comments` | percComments | Social / comments |
| `perc.widgets.liked` | percLiked | Social / comments |
| `perc.widget.commentForm` | percCommentsForm | Social / comments |
| `perc.widget.imageSlider` | percImageSlider | Residual product |
| `perc.widget.cookieConsent` | percCookieConsent | Residual product |
| `perc.widget.jquery` | percJQueryWidget | Residual product |
| `perc.widget.jqueryUI` | percJQueryUIWidget | Residual product |
| `perc.widget.registration` | percRegistration | Login variant |
| `perc.widget.secureLogin` | percSecureLogin | Login variant |
| `perc.widget.Result` | percResult | Residual product |
| `perc.widgets.Redirect` | percRedirect | Residual product |

**Batch C total:** 19 packages · **19** widgets with modern roots.

**Cumulative modern dual-ship authoring roots:** **47** widgets / **38** packages (batch A + B + C). Product modern-root gap excl. Test is **0**. Committed product Widget def XML after batch A ship-exit: **40** (was **48**).

## Retirement checklist (per package)

1. **Confirm compiler goldens / package compile** — `PSWidgetXmlPackageCompiler.compilePackage` green for the package.
2. **Materialize modern** — `widgets/<stem>/component-package.json` + templates committed (or refresh via `materializeModernWidgetSources`).
3. **Parity test** — modern manifest/template equals compile-from-XML (`PSWidgetXmlDualShipTest` pattern).
4. **Shim** — `selectForPackageRoot` / `selectDefinition` prefer modern when XML co-located.
5. **Ship-exit install XML** — delete committed Widget XML only when modern roots exist **and** package-build materializes install XML (`PSWidgetXmlInstallEmitter`). Batch A done (#2883); B/C residuals #2884 / #2885.
6. **Shim** remains until Phase 5 criteria (#2852 / #2632) — do not delete runtime dual-run selection.

## Residual after batch A ship-exit + batch C modern roots

| Residual | Scope | Guidance |
|----------|-------|----------|
| Product modern-root gap | **0** (excl. `perc.Test`) | Batch A+B+C complete product dual-ship authoring |
| Batch A install XML (committed) | **0** (8 removed #2883) | Install materialize at package-build |
| Committed product Widget def XML | **40** remaining (was 48) | Batches B + C dual-ship + `perc.Test` |
| Batch B ship-exit | high-traffic + long-tail dual-ship XML | #2884 |
| Batch C ship-exit + M1 inventory | remaining dual-ship XML | #2885 |
| `perc.Test` | Test package modern roots | #2830 |
| Global shim removal | #2632 / #2852 | Metrics + zero required legacy loads |

**Before/after (#2883 batch A):** product Widget def XML **48 → 40** (−8 batch A stems).

## Dual-run / dual-ship relationship

| Concept | Layer | Status |
|---------|-------|--------|
| Dual-run **definition XML shim** | Runtime selection modern vs Widget XML | Time-boxed; Phase 5 #2632 |
| Dual-ship **widget modern roots** | Package **authoring** under `widgets/` | Batch A (#2831) + B (#2832) + C (#2844); product complete excl. Test |
| Dual-ship **page templateDef** | Package-build install bridge | Optional; native preferred for base/responsive (#2806) |
| Widget install materialize | Package-build stages install Widget XML from modern | **Landed for modern-only packages** (#2883 `PSWidgetXmlInstallEmitter`) |

## See also

- [widget-xml-inventory.md](./widget-xml-inventory.md)
- [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md)
- [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md)
- [adr/004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md)
