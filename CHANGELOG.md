# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org/).

## [8.1.7 Build GH_POST_PR_COMMIT_RUN_ID] - 2026-06-24

### Added

- Added GitHub Actions workflow to automatically update and commit the build number in `Version.properties` files and `CHANGELOG.md` upon pushes to `development-8.1.x`.

### Changed

- Updated `Version.properties` buildNumber to use `GH_POST_PR_COMMIT_RUN_ID` placeholder and updated `AGENTS.md` guidelines to use the automated workflow instead of manual increments.
- Configured the Dependency Submission workflow to be skipped when triggered by a commit containing "Dependency Submission" in its message (#348).

