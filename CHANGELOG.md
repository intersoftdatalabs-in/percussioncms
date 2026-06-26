# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

## [8.1.7 Build 928] - 2026-06-25

### Fixed

- Fixed PSPackageBuilderTest#testAllPackagesMatchReferenceStructure test failure by syncing reference .ppkg files with source content type labels (#1153). Updated perc.widget.registration and perc.widget.secureLogin reference packages to mark content types as "(Deprecated)".

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-06-26

### Fixed

- Fixed misleading "Path not found" error dialog that appeared immediately after a folder create or rename (#867). After rename, `update_btn` in `perc_delete_page_button.js` would call `open_path` on the new path while the JCR was still indexing it; the existing 3x200ms client retry was insufficient and the error handler showed a false-positive alert. Increased the client retry to 6x300ms in `perc_path_manager.js` and made `update_btn` silently disable the delete button on lookup failure (the path was just navigated to, so it must exist) instead of showing the error dialog.

## [8.1.7 Build 922] - 2026-06-25

### Fixed

- Fixed duplicate validation error displays and incorrect error status responses when converting a folder to a section (#866). Updated `PSValidationExceptionMapper`, `PSBeanValidationExceptionMapper`, and `PSSpringValidationExceptionMapper` to map validation errors to standard `400 BAD_REQUEST` instead of `500 INTERNAL_SERVER_ERROR`. Fixed `PercServiceUtils.js` to use an `else if` for `globalError` parsing to prevent duplicate extraction of the same error message when both `globalErrors` and `globalError` are populated in the response.

## [8.1.7 Build 921] - 2026-06-25

### Fixed

- Fixed a folder creation race condition and exception mapping issue where folder creation would display a "Path not found" error popup even though folder creation succeeded in the database (#867). Registered `PSPathServiceExceptionMapper` as a provider under CXF, mapped path service exceptions implementing `IPSNotFoundException` to a `404 NOT_FOUND` status code, and added a short retry loop with a 100ms sleep when lookup of the newly created folder path fails.

## [8.1.7 Build 920] - 2026-06-24

### Fixed

- Fixed footer widget appearing in wrong location in the CMS page editor (#757). The `vspan_X` region height rules in `perc_decoration.css` now use `!important` to assert fixed heights in the editor iframe, preventing `theme.css` `min-height` values from allowing sidebar regions to overflow and displace the footer region.

## [8.1.7 Build 919] - 2026-06-24

### Fixed

- Fixed server startup crash (StringIndexOutOfBoundsException) by restoring date format to `buildNumber` and mapping the sequential run number to `buildId`. Retained defensive robustness checks in `PSFormatVersion` and `PSLogHandler` to prevent future build version format crashes.

## [8.1.7 Build 917] - 2026-06-24

### Added

- Added GitHub Actions workflow to automatically update and commit the build number in `Version.properties` files and `CHANGELOG.md` upon pushes to `development-8.1.x`.

### Changed

- Updated `Version.properties` buildNumber to use `917` placeholder and updated `AGENTS.md` guidelines to use the automated workflow instead of manual increments.

### Fixed

- Fixed a JavaScript TypeError ("Cannot read properties of null (reading 'scrollHeight')") on the Admin Console page by ensuring DOM elements exist before referencing them (#906).

