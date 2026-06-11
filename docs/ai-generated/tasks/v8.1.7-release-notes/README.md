# v8.1.7 Release Notes Documentation

This directory contains AI-generated documentation and drafts for the PercussionCMS v8.1.7 release notes.

## Context

- The public `percussion/percussioncms` GitHub repository was archived on 2026-03-02 with the last public release being v8.1.5.
- This workspace tracks an internal/development fork (development-8.1.x) that continued beyond the archive.
- Local repository contains tag `v8.1.6` and is currently at `8.1.7-SNAPSHOT`.
- Release notes work follows the pattern established for v8.1.6 (see `../#524-v8.1.6-release-notes/`).

## Documents in This Directory

|                 File                  |                                        Purpose                                        |
|---------------------------------------|---------------------------------------------------------------------------------------|
| `RELEASE_NOTES_v8.1.7_GITHUB_BODY.md` | Concise release notes suitable for GitHub Releases page body                          |
| `RELEASE_NOTES_v8.1.7_DETAILED.md`    | Comprehensive release notes with full details, PR references, and dependency analysis |
| `SUMMARY.md`                          | High-level summary of changes and release scope                                       |
| `NOTABLE_CHANGES.md`                  | Focused list of user-facing features and fixes                                        |

## How to Use

1. Review `RELEASE_NOTES_v8.1.7_GITHUB_BODY.md` for the recommended GitHub release text.
2. Use `RELEASE_NOTES_v8.1.7_DETAILED.md` for internal records, support docs, or the help site.
3. Update PR/issue numbers and commit SHAs as the actual release tag is created.

## Key Themes for v8.1.7

- Major overhaul of the Admin Category management UI (Fancytree migration + many UX fixes)
- Google Analytics 4 (GA4) integration/migration
- Delivery Tier (DTS) stability and startup noise reduction
- Successful upgrade of PDFBox to 3.x (with compatibility fixes)
- Deprecation signaling for widgets/gadgets targeted for removal in 8.2
- Numerous small bug fixes and accessibility improvements

## Related

- Version bump PR: #526 (Bump version to 8.1.7)
- Previous release: v8.1.6 (see `../#524-v8.1.6-release-notes/`)
- Branch: `development-8.1.x`

