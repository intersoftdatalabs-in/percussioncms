# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

## [8.1.7 Build 917] - 2026-06-24

### Added

- Added GitHub Actions workflow to automatically update and commit the build number in `Version.properties` files and `CHANGELOG.md` upon pushes to `development-8.1.x`.

### Changed

- Updated `Version.properties` buildNumber to use `917` placeholder and updated `AGENTS.md` guidelines to use the automated workflow instead of manual increments.

### Fixed

- Fixed a JavaScript TypeError ("Cannot read properties of null (reading 'scrollHeight')") on the Admin Console page by ensuring DOM elements exist before referencing them (#906).

