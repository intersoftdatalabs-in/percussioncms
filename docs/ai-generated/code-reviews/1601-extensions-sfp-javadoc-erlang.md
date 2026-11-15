# Erlang Review — Issue #1601 extensions-sfp Javadoc

## Summary

Documentation-only fix for the `extensions-sfp` module: 3 Javadoc HTML errors
and 100 unique Javadoc warnings (200 plugin-emitted lines) eliminated. No
production-code behavior changes; no new compiler warnings; clean install +
Spotless check pass on the module.

## Scope

- Base: `origin/development` (commit `ea85629c2f`)
- Head: feature branch `fix/1601-javadoc-issues-extensions-sfp`
- Files: 25 changed in `modules/extensions-sfp/src/main/java/**`
- Prior report: none
- Memory patterns hit: none (pure Javadoc cleanup; no I/O, no security, no
  behavioral change)

## Recommendation

**approve**

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Issues

None.

## Specific changes

- `PSSiteFolderContentListBaseExit.java` — fixed the 3 HTML errors. The
  `<ol start="0">` was wrongly closed by `</ul>`; replaced with `<ul>` and
  removed a stray second `</ul>`. Also added a class-level description
  (resolves "no main description" warning).
- `IPSDTDPublisherEdition.java` — added 42 Javadoc comments to the public
  static final `ELEM_*` / `ATTR_*` constants that previously had none.
- `PSCalendarMonthModel.java`, `PSRecurringEvent.java`, `PSExpandRecurringEvents.java`,
  `PSSiteFolderContentListLinkGenerator.java`, `PSSiteFolderCListBase.java`,
  `PSRelationshipHelper.java`, etc. — added missing `@param` / `@throws`
  descriptions, supplied a leading main description on methods whose only
  tag was `@return`, and removed the empty `<p>` tags in
  `PSSiteFolderAssembly.java` / `PSSimpleSqlQuery.java`.
- Added explicit `public <ClassName>()` constructors with a one-line Javadoc
  to 11 classes that previously relied on an implicit default constructor
  (`PSAutoGenerateFileName`, `PSAutoSiteItemFilter`, `PSCopyValueToRequest`,
  `PSAppendPurgedOrMovedItems`, `PSBuildRelationshipsFromIdsExit`,
  `PSExtractIdsFromSlotExit`, `PSMakeCalendar`, `PSSite`,
  `PSSiteFolderAssembly`, `PSSiteFolderContentListBaseExit`,
  `PSSiteFolderContentListBulkExit`, `PSSiteFolderContentListExit`,
  `PSSiteFolderContentListLinkGenerator`, `PSCalendarMonthModel`,
  `PSExpandRecurringEvents`).

## Cross-platform / path review

Not applicable — diff touches Javadoc text only. No new file I/O or path
construction.

## Build evidence

- `cmd /c ..\..\mvnw.cmd clean install` on `modules/extensions-sfp` (JDK 21):
  BUILD SUCCESS, jar + javadoc.jar produced, `Tests run: 4, Failures: 0,
  Errors: 0, Skipped: 4` (skipped count is pre-existing for the
  `PSCalendarMonthModelTest`).
- `cmd /c ..\..\mvnw.cmd spotless:check` on `modules/extensions-sfp`: clean.
- `mvnw.cmd javadoc:javadoc`: `3 errors` and `100 warnings` reduced to
  `0 errors` and `0 warnings` (verified via `Select-String -Pattern
  "warning:|error:"` returning 0 unique lines on `tmp/sfp-final-build.log`).
- Pre-existing compiler WARNINGs (raw `HashMap`, `Iterator` types) in
  `PSAutoGenerateFileName.java`, `PSExpandRecurringEvents.java`,
  `PSCalendarMonthModel.java`, `PSChildRelationshipBuilder.java` are
  unchanged from the baseline and are out of scope for this Javadoc issue.

## Notes

- Per AGENTS.md "Pre-PR Spotless formatting (HARD GATE)", an unrelated
  Spotless reformat of `docker-compose.yml` was observed during a root-level
  spotless run and was reverted; this PR contains only the
  `extensions-sfp`-scoped changes.
- Per AGENTS.md "Pre-PR Maven verification (HARD GATE)", the build was run
  module-standalone (`cd modules/extensions-sfp && ../mvnw.cmd clean
  install`), not as a full reactor build.
