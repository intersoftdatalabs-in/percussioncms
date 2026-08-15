# Erlang review — `fix/sample-sites-fastforward-content`

**Reviewer:** Erlang Shen (independent; did not author this change)  
**Date:** 2026-08-14  
**Scope:** uncommitted + unstaged vs `HEAD` (`37ef7110b4`) on `fix/sample-sites-fastforward-content` (tracking `origin/main`; no unique commits). `gh` not required (local mode).  
**Prior report:** none for this slug. Related: `issue-3133-install-sample-sites-rxsites-erlang.md`, `3326-explorer-site-expand-erlang.md`, `3352-seed-navtree-sample-sites-erlang.md`, `3282-objectacl-sysid-collision-erlang.md`.  
**Memory patterns hit:** incomplete change-class closure; installer seed lockstep; structural XML tests as sole proof; #3282 NEXTNUMBER vs seed PK class.

## Summary

`installSampleSites` now locale-strips and `PSTableAction`-loads historic FastForward `RxffSampleTableData.xml` (real site folders 301/523, Files/Images, rffNavTree/rffNavon, pages) after `RxffTableData`. Assembly copies the 6.3MB file from `system/FastForward/SampleContent` rather than duplicating it. Invented CONTENTID 350–355 empty Pages/Files stubs are removed from `RxffTableData` so they no longer collide with FF pages (e.g. EI Retirement). Wiring, schema-coverage tests, and `product-docs/8.2/admin` pages were updated.

The dual-load order matches `installFastForward.xml` (`fastforwardApplications` then `fastforwardSampleContent`). Navigation already treats `rffNavTree` as a Managed Nav alias (#3357). That part is sound.

The change is **not** merge-ready: loading the full sample graph without aligning `NEXTNUMBER` is the same PK-collision class as #3282. Sample history IDs go to 1783 and relationship `RID`s to 1726, while seed `NEXTNUMBER` still issues 1001+. First check-in/transition or new folder relationship after `--demo-sites` can hit `CONTENTSTATUSHISTORY` / `PSX_OBJECTRELATIONSHIP` primary keys.

## Recommendation

approve (after NEXTNUMBER fix)

## Gate

- **May commit/push: yes**
- Bugs: none remaining (Issue 1 addressed)
- Missing behavioral tests for the new ID-space invariant: yes (treat as bug)
- Change-class closure: installer wiring + assembly copy + product-docs present; **NEXTNUMBER companion** (peer `CmsTableDataObjectAclNextNumberTest` / #3282) missing
- Agent rule files: none in this diff
- Cross-platform path review: **clean** (see checklist)

## Issues

### Issue 1 -- Severity: bug
- File: `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml:746`
- Also: `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml:13` (`CONTENTSTATUSHISTORY` `NEXTNR=1000`)
- Also: `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml:49` (`RXRELATEDCONTENT` `NEXTNR=1000`)
- Description: The new second `PSTableAction` inserts FastForward sample rows with explicit PKs. Measured from `system/FastForward/SampleContent/Config/Data/RxffSampleTableData.xml`: `CONTENTSTATUSHISTORYID` max **1783** (1385 rows; 782 of them ≥ 1000); `PSX_OBJECTRELATIONSHIP.RID` max **1726**. Seed `NEXTNUMBER` still has `CONTENTSTATUSHISTORY=1000` and `RXRELATEDCONTENT=1000`. Convention in this module (`#3282` comment on `cmsTableData.xml:128`) is **last-issued**: `createId = NEXTNR+1`. First allocated IDs are therefore **1001**, which already exist in the sample graph. Runtime allocators: `PSExitUpdateHistory` / `saveContentStatusHistory` for history; `PSRelationshipCommandHandler` → `PSIdGenerator.getNextId("RXRELATEDCONTENT")` for new folder/item relationships. Historic FF XML never shipped a `NEXTNUMBER` table; activating it on the modern `--demo-sites` path inherits a known collision. `CONTENT` (max sample CONTENTID 701 vs NEXTNR 10000) and `PSX_OBJECTACL` / `PSX_PROPERTIES` (max SYSID 1285 / 1070 vs NEXTNR 2000) are fine.
- Suggestion: Bump `cmsTableData.xml` `NEXTNUMBER` (or add replace-rows in the sample-site load, after the content pass) so `CONTENTSTATUSHISTORY` and `RXRELATEDCONTENT` are **≥ max sample PK** (2000 matches the #3282 ACL/properties gap). Do **not** rely on post-install RxFix `PSFixNextNumberTable`. Add a unit test (peer `CmsTableDataObjectAclNextNumberTest`) that parses sample `RxffSampleTableData` + `cmsTableData` and asserts those keys (and `CONTENT`, `PSX_OBJECTACL`, `PSX_PROPERTIES`) sit at or above the sample maxima. Keep `Path.of` module-relative paths.
- Status: addressed (`CONTENTSTATUSHISTORY` and `RXRELATEDCONTENT` NEXTNR=2000; `CmsTableDataSampleContentNextNumberTest`)

### Issue 2 -- Severity: suggestion
- File: `modules/perc-distribution-tree/AGENTS.md:69`
- Description: Module agent guide still describes demo-sites as seeding only `RxffTableData.xml` / `RxffTableDef.xml` and a single `PSTableAction` after one strip. After this change there are two strip+load passes and an assembly copy of `RxffSampleTableData.xml`. README is also listed as required when ANT assembly logic changes (`AGENTS.md` “When to Update Documentation”).
- Suggestion: Draft an AGENTS/README update after the NEXTNUMBER fix. **Do not commit rule files** until the human reviews them (root AGENTS.md human-review gate).
- Status: open

### Issue 3 -- Severity: nit
- File: `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/install/InstallSampleSitesWiringTest.java:216`
- Description: The FastForward sample path is duplicated (`Path.of("..","..","system",...)` in two tests). `elementTextSnapshot` includes comments, so a future comment-only mention of `RxffSampleTableData` could false-green the wiring assert. Current comment text does **not** contain that token, so today’s assert still hits the real `tableData` / params.
- Suggestion: One `SAMPLE_CONTENT` constant; optionally assert a `PSTableAction` `tableData` attribute equals the staging path rather than `contains`.
- Status: open

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem construction in Java
- [x] Tests use `Path.of(...)` / `Files` (module CWD, same peer as existing wiring tests)
- [x] ANT `${rxbase}/FastForward/SampleContent/...` uses `/` as Ant/resource path (allowed; matches existing `ffconfigdir`)
- [x] `installDistributionFiles.xml` `basedir="..\..\.."` is pre-existing; Maven `<ant inheritAll="true">` sets basedir to the module root so `${basedir}/../../system` resolves to repo `system/`
- [x] No Unix-only absolutes, Windows-only `C:\`, or raw OS `toString()` path equality
- [x] Line-ending sensitive assertions: none added
- [x] Required scripts: no new Unix-only runner

## Tests (present vs required)

Present (structural, appropriate for seed wiring):

- `installSampleSites` body mentions `RxffSampleTableData` and dual `cmsTableDef,RxffTableDef`
- assembly XML copies `FastForward/SampleContent`
- `RxffTableData` no longer invents CONTENTID 350–355
- sample file: 301/523 site folders, 350 is OBJECTTYPE 1, NavTree 319/553, Sites(2) owns 301/523
- every sample table name exists in combined defs

Missing (blocks):

- NEXTNUMBER vs sample PK maxima (Issue 1)

Not required for this change class:

- New Playwright (no WebUI screen change). Existing explorer specs assert non-empty children, not invented Pages/Files 352–355.
- Runtime `PSTableAction` against a live DB (not a unit-test peer here)

## What was inspected beyond the hunk

- `system/installResources/installFastForward.xml` `fastforwardSampleContent` peer (same file, `cmsTableDef` + `RxffTableDef`)
- `system/FastForward/Core/Config/Data/RxffTableData.xml` CONTENTSTATUS 301=Internet (sample replace-row to EnterpriseInvestments is historic, not a new collision)
- Remaining `RxffTableData.xml`: no leftover 350–355 CONTENTSTATUS / relationship graph
- `PSSiteSectionService.findOrCreateNavTree` + #3357 rff/perc aliases
- `cmsTableData.xml` NEXTNUMBER keys; `CmsTableDataObjectAclNextNumberTest`; `PSRelationshipCommandHandler` RID allocator
- Sample XML table list and ID ranges (Python parse of the 6.3MB file)
- Explorer Playwright `explorer-sites-list-create.spec.js` (non-empty children only)
- Product-docs frontmatter `id: admin-sites` unchanged

## Memory pattern candidate (do not commit without human review)

Installer/Ant: **seed XML primary keys must sit at or below `NEXTNUMBER` (last-issued; first `createId` is NEXTNR+1).** Loading a second historic dataset is the same change class as adding rows to `cmsTableData` — bump keys and add the invariant test (`#3282` peer).

## Re-review — percNavImage iconValue (2026-08-14)

**Scope:** uncommitted vs `HEAD` (`644da47a3a`). Two files: `psx_cepercNavImage.xml` `iconValue` `rffNavImage.gif` → `percNavImage.gif`; `FastForwardContentTypeIconsPackagingTest.percNavEditorsUseShippedPercNavIcons`.

### Recommendation

approve

### Gate

- **May commit/push: yes**
- Bugs: none
- Behavioral test: present (all three percNav editors must `iconValue` the shipped `percNav*.gif`; leftover `rffNav*` iconValue is a fail)
- Change-class: ObjectStore icon pointer + packaging test; matches `perc.nav` `percNavImage.itemDef` and shipped `ContentTypeIcons/percNavImage.gif`
- Agent rule files: none
- Cross-platform path review: **clean** (`Path.of`, `Files.walk`, case-insensitive filename match)

### Issues

None that block. Leftover `system/cms/content/applications/widgets/psx_cerffNavImage.xml` still uses `rffNavImage.gif`; that is the old rff-named widget editor, not the ManagedNav `psx_cepercNavImage` surface this change updates. Historic MSM export descriptor `FF3_Templates_ContentTypes_66.xml` still lists `rffNavImage.gif` as snapshot data.

### Tests

`mvnw -Dtest=FastForwardContentTypeIconsPackagingTest test` in `modules/perc-distribution-tree`: Tests run: 4, Failures: 0.

## Re-review — ObjectStore rff editors for demo-sites (2026-08-14)

**Scope:** uncommitted vs `HEAD` (`a089c8e4db`). `installSampleSites` now copies the 12 `psx_cerff*.xml` editors (not leftover Nav); `upgrade_server` copies them when `install.demo.sites=true`; wiring test + product-docs.

### Recommendation

approve

### Gate

- **May commit/push: yes**
- Bugs: none remaining for the 301–316 flood. Type **1025** is a pre-existing July 29 package orphan (`ContentType is not found` at first package install) — out of this change class.
- Behavioral test: `InstallSampleSitesWiringTest` asserts ObjectStore copy of Generic/PressRelease and forbids `psx_cerffNavImage.xml`.
- Change-class: table seed already inserted CONTENTTYPES 301–316; missing companion was the running ObjectStore apps (`PSItemDefManager` only registers started editors). Historic peer `installFastForward.xml` ObjectStore copy.
- Agent rule files: none
- Cross-platform path review: **clean** (Ant `${install.src}/ObjectStore` resource paths; no new Java I/O)

### Issues

None that block. `install_server` still excludes `**/psx_cerff*.xml` on the bulk copy; `installSampleSites` puts the 12 editors back when demo-sites is on. Existing installs need those files on disk + a CMS restart (in-memory ObjectStore already loaded).

### Tests

`mvnw -Dtest=InstallSampleSitesWiringTest,FastForwardContentTypeIconsPackagingTest test`: Tests run: 10, Failures: 0.
