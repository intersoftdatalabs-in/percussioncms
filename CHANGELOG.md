# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

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

