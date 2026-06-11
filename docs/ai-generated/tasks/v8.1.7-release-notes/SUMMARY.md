# Summary: v8.1.7 Release Notes Preparation

## Purpose

This set of documents captures the release notes content for PercussionCMS v8.1.7 based on analysis of:
- Git history between tag `v8.1.6` and current `HEAD` on `development-8.1.x`
- Actual dependency version changes in `pom.xml` (and module poms)
- Precedent and templates from the v8.1.6 release notes work (`#524-v8.1.6-release-notes/`)

## Scope of Changes Since v8.1.6

**93 commits** (including merges and dependabot activity) between v8.1.6 and the current 8.1.7-SNAPSHOT state.

### High-Impact Areas

1. **Category Administration UI** — Largest body of work. Full Dynatree → Fancytree migration plus extensive bug fixing.
2. **Google Analytics 4** — New integration path.
3. **DTS Stability** — Multiple targeted fixes for startup noise, missing dependencies, platform issues.
4. **PDFBox 3.0.6** — First successful major version upgrade of PDFBox in the 8.1.x line (previous attempt in 8.1.6 cycle was rolled back).
5. **Deprecation Housekeeping** — Preparing customers for 8.2 removals.

### Dependency Highlights (Actual Changes)

- PDFBox 2.0.30 → 3.0.6 (with code adaptation)
- Jackson 2.20.1 → 2.21.1
- Shiro → 2.1.0
- Multiple supporting library and plugin bumps
- New: opencsv 5.12.0

All changes preserve Java 8 compatibility.

## Recommended Release Artifacts

|       Artifact       |                 File                  |                Audience                 |
|----------------------|---------------------------------------|-----------------------------------------|
| GitHub Release Body  | `RELEASE_NOTES_v8.1.7_GITHUB_BODY.md` | Public / customers                      |
| Full Internal Record | `RELEASE_NOTES_v8.1.7_DETAILED.md`    | Support, engineering, help site authors |
| Orientation          | `README.md`                           | Future AI agents / release engineers    |

## Next Steps (when preparing actual release)

1. Create GitHub issue for "v8.1.7 release notes" (similar to #524) and move these docs under a `#NNN-v8.1.7-release-notes/` folder if desired.
2. Update commit SHA and exact tag name in the documents once `v8.1.7` tag is created.
3. Cross-reference any customer-facing help site articles that need updating (GA4, Category UI changes, PDFBox implications if any).
4. Ensure the version bump commit and final release commit messages are clean.

## Status

**Ready for review.** Drafts are complete and follow the established format and quality level of the v8.1.6 corrected release notes.
