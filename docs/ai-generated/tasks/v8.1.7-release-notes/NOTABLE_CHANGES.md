# Notable Changes for v8.1.7

## User-Facing Highlights

### Category Management (Admin UI)

- Major refresh of the Category tree editor and browser.
- Underlying component migrated from Dynatree (unmaintained) to Fancytree.
- Dozens of specific bugs fixed around selection, ordering, creation, and visual rendering.
- Category List widget rendering issues on published sites also resolved.

### Analytics

- Official support path for Google Analytics 4 (GA4) properties.
- Improved error handling in Google-related gadgets.

### Content Operations

- Bulk upload gadget now gracefully handles and reports zero-byte file failures.
- Clearer messaging when the CMS cannot send email because no mail server is configured.

### Widgets & Published Sites

- Auto List widgets now function correctly when published.
- Form widget email field has proper autocomplete accessibility attributes.
- Breadcrumb widget has correct ARIA navigation role.
- Footer layout issues fixed on certain themes/regions.

### Administration & Operations

- Significantly quieter DTS startup logs.
- Better diagnostics for several edge cases (process monitor, orphaned managed links warning, etc.).
- Windows DTS environments more stable.

## Technical / Platform

- **PDFBox upgraded to 3.0.6** — This is the biggest library jump in the release. Internal PDF processing code was updated for compatibility.
- **Jackson upgraded to 2.21.1** — Modern patch line with security and bug fixes.
- **Shiro upgraded to 2.1.0**.
- New OpenCSV dependency added to support reliable DTS data loading operations.
- Continued aggressive maintenance of the dependency tree while enforcing Java 8 compatibility (multiple attempted major-version bumps were capped or reverted).

## Deprecations & Cleanup

- Several legacy widgets and gadgets are now visibly marked as deprecated in the UI.
- "Community" section removed from relevant administration areas.
- These items are targeted for complete removal in the 8.2 release.

## What Did NOT Change

- Java requirement remains **JDK 1.8.0**.
- Core MyFaces (2.3.11), Shindig (1.1-BETA5), and other Java-8-pinned frameworks stay at their last compatible versions.
- No changes to supported databases or major deployment architecture.
- Upgrade from 8.1.6 should be low-risk for most installations.

## Recommended Testing Focus Areas

1. Category creation, editing, ordering, and assignment to content (including edge cases with deep hierarchies).
2. Sites using Category List or related navigation widgets after publish.
3. Any custom or OOTB usage of PDF generation / PDFBox-dependent features.
4. GA4 configuration and reporting gadgets (if used).
5. DTS startup on both Linux and Windows with various database backends.
6. Bulk asset upload workflows.
7. Form submissions on sites using the Form widget.

