# Task: Backport aria-label Navigation Widgets (Issue #663)

## Objective

Backport WCAG 2.1 AA accessibility fixes for the Navigation and Breadcrumb widgets from the 8.2 development branch to the 8.1.x branch, and fix the missing Jexl bindings that prevented `aria-label` from rendering.

## Changes Made

1. **Breadcrumb Widget (`system/Packages/perc.widgets.nav/sys__UserDependency--rxconfig/Widgets/percNavBreadcrumb.xml`)**:
   - Added the `navLabel` UserPref with a default value of `"Breadcrumb Navigation"`.
   - Updated nested `<nav>` elements (both normal and edit-mode) to include `aria-label="$!{navLabel}"`.
   - Fixed Jexl script block to fetch `navLabel` via `$navLabel = $perc.widget.item.properties.get('navLabel');`.
2. **Navigation Widget (`system/Packages/perc.widgets.nav/sys__UserDependency--rxconfig/Widgets/percNavBar.xml`)**:
   - Added the `navLabel` UserPref with a default value of `"Main Navigation"`.
   - Updated the primary `<nav>` element to include `aria-label="$!{navLabel}"`.
   - Fixed Jexl script block to fetch `navLabel` via `$navLabel = $perc.widget.item.properties.get('navLabel');`.
3. **Package Versioning (`system/Packages/perc.widgets.nav/psx_archiveInfo.xml`)**:
   - Incremented the package version from `1.3.3` to `1.3.4` to ensure correct upgrade application by the CMS Package Manager.

## Verification

- Ran `./mvn-env.sh spotless:apply` successfully.
- Re-built `perc-packages` module via `./mvn-env.sh clean install -pl modules/perc-packages -DskipTests` to ensure widget package `perc.widgets.nav.ppkg` builds and archives correctly.
- Verified that `perc.widgets.nav` package passes the package reference structure tests.

