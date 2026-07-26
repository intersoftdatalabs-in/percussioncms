# Spec 005 — v8.1.7 Non-Dependabot PR Inventory

Scope: PRs merged into `development-8.1.x` between v8.1.6 (2026-01-02) and v8.1.7 (2026-06-26), excluding Dependabot dependency-update PRs.

## 1. Tag Range and Lineage Analysis

- `v8.1.6` commit date: **2026-01-02** (commit on `development-8.1.x`)
- `v8.1.7` commit date: **2026-06-26**
- Tags exist on `development-8.1.x` as annotated releases produced by `maven-release-plugin`.
- `git log v8.1.6..v8.1.7` contains **9410 commits** (highly inflated by release-prep, build-number bumps, and cherry-picks from `development`).
- **Window confirmed correct:** the v8.1.6..v8.1.7 range covers all v8.1.7-lineage changes since the previous tag.
- Caveat: many merge commits in this range are *cherry-picks from `development`* (the main JDK-21 branch) forward-ported to `development-8.1.x`. The 8.1.x-specific PR base is `development-8.1.x` per `gh pr list --base development-8.1.x`.

## 2. Methodology

Commands executed:

```bash
# Tag dates
git log -1 --format='%cI' v8.1.6   # 2026-01-02
git log -1 --format='%cI' v8.1.7   # 2026-06-26

# Full PR list to development-8.1.x (370 total)
gh pr list --state merged --base development-8.1.x --limit 500 \
   --json number,title,mergedAt,baseRefName,author,labels

# Identify dependabot PRs (229 of 370 — 62%)
jq -r '.[] | select(.author.login | test("dependabot"; "i")) | .number'

# Cross-check v8.1.6..v8.1.7 merge log (837 unique PR refs, most cherry-picks from development)
git log v8.1.6..v8.1.7 --merges --oneline | grep -oE '#[0-9]+' | sort -u

# Files-changed fetch (for module inference)
gh api repos/intersoftdatalabs-in/percussioncms/pulls/<N>/files --paginate --jq '.[].filename'
```

Filters applied:

1. Base branch must be `development-8.1.x`.
2. `mergedAt >= 2026-01-02` (v8.1.6 commit timestamp) OR the PR is referenced in the v8.1.6..v8.1.7 merge log.
3. Author login must NOT contain `dependabot` (other bots like `app/copilot-swe-agent[bot]` were inspected — none excluded).
4. Files-changed were fetched via `gh api .../files --paginate` and top-level module paths derived (e.g. `modules/perc-...`, `system/...`, `deliverytiersuite/...`). Non-project top-level entries (`pom.xml`, `mvnw`, `CHANGES.md`, `.github/...`, `docs/...`, etc.) are excluded from the Module column.

Result: **141 non-Dependabot PRs** in window, **229 Dependabot PRs** excluded.

## 3. Inventory Table (141 PRs)

Columns: PR# · Title (truncated 90 chars) · Merge date · Module(s) · JDK8-only? (Y) · Security? (Y)

|   #   |                                           Title                                            |   Merged   |                                         Module(s)                                          | JDK8 | Sec |
|-------|--------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------|------|-----|
| #107  | Feature/dependency updates=2025                                                            | 2025-12-18 | WebUI, deliverytiersuite/delivery-tier-suite, deployer, modules/CMLight-Main-cactus-tes... |      | Y   |
| #108  | Add dependency submission workflow for development-8.1.x branch                            | 2025-12-18 | *unclear*                                                                                  |      |     |
| #110  | Add exclusion / legacy axis reference to fix action failure                                | 2025-12-18 | modules/webservices                                                                        |      |     |
| #111  | Feature/fix webservice action failure                                                      | 2025-12-18 | modules/webservices, system                                                                |      |     |
| #112  | Feature/fix webservice action failure                                                      | 2025-12-18 | modules/webservices, system                                                                |      |     |
| #113  | Feature/fix webservice action failure                                                      | 2025-12-18 | modules/webservices, system                                                                |      |     |
| #114  | Feature/fix webservice action failure                                                      | 2025-12-18 | PCM-PkgMgtUI, modules/webservices, system                                                  |      |     |
| #115  | Feature/fix webservice action failure                                                      | 2025-12-18 | PCM-PkgMgtUI, WebUI, modules/webservices, system                                           |      |     |
| #116  | Feature/fix webservice action failure                                                      | 2025-12-18 | *unclear*                                                                                  |      |     |
| #117  | Feature/fix webservice action failure                                                      | 2025-12-18 | *unclear*                                                                                  |      |     |
| #118  | Feature/fix webservice action failure                                                      | 2025-12-18 | *unclear*                                                                                  |      |     |
| #119  | Feature/fix webservice action failure                                                      | 2025-12-18 | deliverytiersuite/delivery-tier-suite                                                      |      |     |
| #132  | Revert "Bump cxf.version from 3.5.9 to 3.6.9"                                              | 2025-12-18 | *unclear*                                                                                  |      |     |
| #138  | Fix failing test and update dependencies                                                   | 2025-12-18 | deliverytiersuite/delivery-tier-suite, system                                              |      |     |
| #149  | Feature/spotless google format baseline                                                    | 2025-12-19 | PCM-PkgMgtUI, SUPPORT.md, WebUI, cui, delivery, deliverytiersuite/delivery-tier-suite, ... | Y    | Y   |
| #245  | Feature/post security update fixes 12 20                                                   | 2025-12-20 | system                                                                                     |      | Y   |
| #302  | Maintenance: Fix slow running system tests & security fixes for depends updates            | 2025-12-21 | system                                                                                     |      | Y   |
| #303  | Revert "Bump com.diffplug.spotless:spotless-maven-plugin from 2.30.0 to 3.1.0"             | 2025-12-21 | *unclear*                                                                                  |      |     |
| #304  | Fix PDFBox loading method in PSTextConverterPdf to use new API                             | 2025-12-21 | system                                                                                     |      |     |
| #416  | Revert "Bump commons-digester:commons-digester from 1.8.1 to 2.1"                          | 2025-12-21 | *unclear*                                                                                  |      |     |
| #419  | Java 1.8 Toolchain and 2025 security update refactoring                                    | 2025-12-22 | PCM-PkgMgtUI, WebUI, delivery, deliverytiersuite/delivery-tier-suite, deployer, modules... |      | Y   |
| #420  | Bugfix/post security update pass 3                                                         | 2025-12-22 | *unclear*                                                                                  |      | Y   |
| #491  | Downgrade myfaces to 1.8 compatible version                                                | 2025-12-23 | deployer, modules/utils, system                                                            |      |     |
| #499  | Revert "Bump javax.jcr:jcr from 1.0 to 2.0"                                                | 2025-12-23 | *unclear*                                                                                  | Y    |     |
| #500  | Revert "Bump com.phloc:phloc-commons from 4.3.9 to 5.0.0"                                  | 2025-12-23 | projects                                                                                   |      |     |
| #504  | Bugfix/jcr icu update testing                                                              | 2025-12-23 | deliverytiersuite/delivery-tier-suite, modules/DesktopContentExplorer, modules/jcadf-ma... |      | Y   |
| #509  | Bugfix/jetty logging fixes                                                                 | 2025-12-23 | deliverytiersuite/delivery-tier-suite, modules/CMLight-Main-cactus-tests, modules/Deskt... |      | Y   |
| #515  | Bugfix/510 bad executable bit post install                                                 | 2025-12-29 | WebUI, deliverytiersuite/delivery-tier-suite, modules/perc-distribution-tree, modules/p... |      | Y   |
| #524  | Correct v8.1.6 release notes dependency versions for Java 8 compatibility                  | 2025-12-31 | *unclear*                                                                                  | Y    |     |
| #525  | Feature/8.1.6 release prep                                                                 | 2026-01-02 | PCM-PkgMgtUI, WebUI, delivery, deliverytiersuite/delivery-tier-suite, deployer, modules... |      | Y   |
| #526  | Bump version to 8.1.7                                                                      | 2026-01-02 | PCM-PkgMgtUI, WebUI, delivery, deliverytiersuite/delivery-tier-suite, deployer, modules... |      | Y   |
| #572  | Fix #571 for new PDFbox version                                                            | 2026-01-21 | system                                                                                     |      |     |
| #573  | Rollback jackson version update                                                            | 2026-01-21 | rest                                                                                       |      |     |
| #574  | Bugfix/jackson compatibility                                                               | 2026-01-22 | deliverytiersuite/delivery-tier-suite, rest                                                |      |     |
| #601  | fix(#600): Add missing role and other HTML attributes to html-cleaner.properties           | 2026-02-20 | modules/utils                                                                              |      |     |
| #653  | Add missing navigation role to breadcrumb widget.  Update widget vers…                     | 2026-02-27 | system                                                                                     |      |     |
| #658  | Update form widget version to 1.4.8 and set accessibility attributes …                     | 2026-02-27 | system                                                                                     |      |     |
| #665  | Log a warning for orphaned managed links in findPagesLinkedToItem met…                     | 2026-03-18 | deliverytiersuite/delivery-tier-suite, modules/TableFactory, modules/patch-tools, modul... |      |     |
| #676  | Bugfix/issue 675 jackson null handling                                                     | 2026-03-18 | projects                                                                                   |      | Y   |
| #716  | Bugfix/8.1.7 release testing                                                               | 2026-03-24 | modules/perc-qa-automation, modules/utils, package-lock.json, package.json, projects, r... |      |     |
| #722  | For #720.  Add deprecated lists to ui, add widgets and gadgets to be …                     | 2026-03-26 | WebUI, system                                                                              |      |     |
| #727  | Bulk upload gadget 710                                                                     | 2026-04-07 | WebUI, cui, delivery, deliverytiersuite/delivery-tier-suite, modules/DesktopContentExpl... |      |     |
| #735  | Improve error reportinng when email not configured.  Add report help …                     | 2026-04-09 | projects, rest, system                                                                     |      |     |
| #736  | Bugfix/dts windows 672                                                                     | 2026-04-09 | deliverytiersuite/delivery-tier-suite                                                      |      |     |
| #755  | fix: #750 missing bullets + #753 Category List fancytree errors in de…                     | 2026-05-22 | modules/perc-distribution-tree, system                                                     |      |     |
| #760  | Feature/756 ga4 analytics migration                                                        | 2026-05-26 | projects, system                                                                           |      | Y   |
| #761  | bugfix/758: Fix category creation node activation and Fancytree seria…                     | 2026-05-26 | WebUI                                                                                      |      |     |
| #762  | bugfix/749: Fix auto list widgets not working on published sites                           | 2026-05-26 | system                                                                                     |      |     |
| #763  | bugfix/757: Fix footer misalignment by using min-height for vspan reg…                     | 2026-05-26 | system                                                                                     |      |     |
| #764  | bugfix/748: Replace sqlCheck with indexExists to eliminate SQL precon…                     | 2026-05-26 | deliverytiersuite/delivery-tier-suite                                                      |      |     |
| #767  | Bugfix/757 footer alignment fix                                                            | 2026-05-27 | WebUI                                                                                      |      |     |
| #771  | Bugfix/749 fix auto list widgets                                                           | 2026-05-27 | WebUI                                                                                      |      |     |
| #772  | Bugfix/766 category browser fancytree                                                      | 2026-05-27 | system                                                                                     |      |     |
| #773  | bugfix/768: Fix null pointer exception in Google Setup gadget                              | 2026-05-27 | system                                                                                     |      |     |
| #774  | bugfix/756: Fix JSON payload leaking in extractDefaultErrorMessage wh…                     | 2026-05-27 | WebUI                                                                                      |      |     |
| #775  | fix(bulk upload gadget): show error message when zero-size file uploa…                     | 2026-05-28 | projects, system                                                                           |      |     |
| #776  | Bugfix/728 bulk upload gadget zero size error                                              | 2026-05-28 | projects                                                                                   |      |     |
| #778  | Refactor Admin Category editor UI                                                          | 2026-05-28 | WebUI                                                                                      |      |     |
| #781  | Feature/admin category UI refactor                                                         | 2026-05-29 | WebUI, projects                                                                            |      |     |
| #782  | fix: DTS startup warnings, Java 8 TLS/cipher config, and JUL bridge n…                     | 2026-05-29 | deliverytiersuite/delivery-tier-suite, modules/perc-ant                                    | Y    | Y   |
| #783  | fix: Admin Category UI - overlapping icons, tree rendering, move up/d…                     | 2026-05-29 | WebUI, projects, system                                                                    |      |     |
| #786  | Fix View Metadata dialog rendering for Tags and Categories (#785)                          | 2026-06-01 | system                                                                                     |      |     |
| #788  | Bugfix/785 fix view metadata tags categories                                               | 2026-06-01 | WebUI, system                                                                              |      |     |
| #789  | Bugfix/757 footer alignment fix                                                            | 2026-06-01 | *unclear*                                                                                  |      |     |
| #794  | Bugfix/793 redirect management to deprecated                                               | 2026-06-02 | WebUI                                                                                      |      |     |
| #796  | Bugfix/795 calendar widget hidecalendarsource                                              | 2026-06-03 | system                                                                                     |      |     |
| #807  | bugfix/803: Fix missing twitter:site metadata and update branding/log…                     | 2026-06-08 | system                                                                                     |      |     |
| #812  | bugfix/809: Rebrand Twitter to X in Social Buttons widget                                  | 2026-06-08 | system                                                                                     |      |     |
| #813  | bugfix/801: Fix Image Slider Edit Template warning and handle relativ…                     | 2026-06-08 | projects, system                                                                           |      |     |
| #814  | bugfix/811: Prevent duplicate Facebook Open Graph tags from being ren…                     | 2026-06-08 | system                                                                                     |      |     |
| #816  | bugfix/815: Fix JavaScript errors in Image Slider widget content edit…                     | 2026-06-09 | system                                                                                     |      |     |
| #821  | bugfix/809: Fix alignment and sizing of X (Twitter) social icon                            | 2026-06-10 | system                                                                                     |      |     |
| #822  | Bugfix/803 twitter site metadata                                                           | 2026-06-10 | system                                                                                     |      |     |
| #823  | bugfix/820: Prevent duplicate entries in Insert Global Variable dialog                     | 2026-06-10 | modules/perc-tinymce                                                                       |      |     |
| #824  | bugfix/819: Correct load order and defer dependent scripts when using…                     | 2026-06-10 | system                                                                                     |      |     |
| #825  | bugfix/818: Allow data: and blob: image URIs in Content Security Poli…                     | 2026-06-10 | deliverytiersuite/delivery-tier-suite                                                      |      | Y   |
| #826  | bugfix/728: Prevent bulk upload queue count from going negative when …                     | 2026-06-10 | system                                                                                     |      | Y   |
| #827  | bugfix/663: Backport WCAG 2.1 AA navigation landmarks and aria-label …                     | 2026-06-10 | system                                                                                     |      |     |
| #830  | Bugfix/828 lock timeout rich text save                                                     | 2026-06-10 | projects                                                                                   |      |     |
| #834  | Bugfix/833 sitewide framework gadget                                                       | 2026-06-11 | projects, system                                                                           |      |     |
| #835  | Bugfix/832 cookie consent syntax error                                                     | 2026-06-11 | system                                                                                     |      |     |
| #836  | Bugfix/831 report email attachment error                                                   | 2026-06-11 | system                                                                                     |      |     |
| #837  | Bugfix/829 Auto Touch Pages with Directory index when Directory related Assets are approve | 2026-06-11 | modules/extensions-main, modules/perc-distribution-tree, modules/perc-packages, system     |      |     |
| #838  | bugfix(803): remove duplicate twitter:site tag insertion block                             | 2026-06-11 | system                                                                                     |      |     |
| #846  | bugfix/818: Update CSP in restored perc-security.properties on DTS up…                     | 2026-06-12 | deliverytiersuite/delivery-tier-suite                                                      |      | Y   |
| #848  | Deprecate Secure Login widget and associated DTS security configurations                   | 2026-06-12 | deliverytiersuite/delivery-tier-suite, projects, rest, system                              |      | Y   |
| #850  | Deprecate Registration widget                                                              | 2026-06-12 | projects, rest, system                                                                     |      |     |
| #851  | Fix NullPointerException during publish for newly created sites                            | 2026-06-13 | projects, system                                                                           |      |     |
| #853  | bugfix/849: Handle concurrency exceptions gracefully during publish jobs                   | 2026-06-13 | projects, system                                                                           |      |     |
| #855  | bugfix/663: Add missing Jexl navLabel property retrieval to Nav/Bread…                     | 2026-06-13 | system                                                                                     |      |     |
| #856  | bugfix/847: Default DTS Server dropdown to NONE when not configured                        | 2026-06-13 | WebUI                                                                                      |      |     |
| #859  | Bugfix/847 dts server dropdown none                                                        | 2026-06-15 | system                                                                                     |      |     |
| #861  | bugfix/860: Fix invalid Google Analytics date format in Traffic Gadget                     | 2026-06-16 | projects                                                                                   |      | Y   |
| #863  | bugfix/829: Automatic configuration of Directory Index Touch workflow…                     | 2026-06-16 | modules/perc-distribution-tree, system                                                     |      |     |
| #864  | bugfix/757: Revert editor styling overrides to keep original min-heig…                     | 2026-06-16 | WebUI                                                                                      |      |     |
| #865  | bugfix/862: Fix ClassCastException String to RowKeySet by upgrading l…                     | 2026-06-16 | system                                                                                     |      |     |
| #869  | bugfix/866: Stop validator early on folder-not-found to prevent dupli…                     | 2026-06-17 | projects                                                                                   |      |     |
| #870  | bugfix/868: Fix theme list serialization and load error in style gall…                     | 2026-06-17 | WebUI, projects                                                                            |      |     |
| #872  | bugfix/867: Fix folder creation path-not-found error caused by incorr…                     | 2026-06-17 | WebUI                                                                                      |      |     |
| #874  | bugfix/871: Escape hyphen in workflow name and step name regex patter…                     | 2026-06-17 | WebUI                                                                                      |      |     |
| #883  | Fix #880 #881: Remove global jQuery UI tooltip that caused tooltips t…                     | 2026-06-19 | cui                                                                                        |      |     |
| #885  | Fix #882: Undeprecate Google Setup, Traffic, and What's Working gadgets                    | 2026-06-22 | WebUI                                                                                      |      |     |
| #886  | Fix #879: Friendly error message for orphaned pages                                        | 2026-06-22 | projects                                                                                   |      |     |
| #889  | Fix Lucene ParseException for path-based search terms                                      | 2026-06-22 | projects                                                                                   |      |     |
| #890  | Fix: Recalculate directory widget alphabet filters dynamically on dropdown selection chang | 2026-06-22 | system                                                                                     |      |     |
| #892  | Fix: Fully reset Organization, Department, and search values when clearing filters         | 2026-06-22 | system                                                                                     |      |     |
| #893  | GH-877: Filter out recycled pages from My Bookmarks                                        | 2026-06-22 | projects                                                                                   |      |     |
| #894  | GH-891: Support leading Sites/ in Page by Path REST resource                               | 2026-06-22 | rest                                                                                       |      |     |
| #896  | GH-876: Enforce size constraints on theme preview thumbnails in Style tab                  | 2026-06-22 | WebUI                                                                                      |      |     |
| #897  | GH-867: Normalize validateEnteredPath input and handle virtualized site prefix             | 2026-06-22 | projects                                                                                   |      |     |
| #898  | bugfix/866: Guard page and parent path validations when source folder is not found         | 2026-06-22 | projects                                                                                   |      |     |
| #899  | bugfix/895: Safe guard getPageIdCallback in new site dialog to avoid JS error              | 2026-06-22 | WebUI                                                                                      |      |     |
| #900  | bugfix/862: Add implicit.tld files to force JSP 2.1 compilation for implicit tag libraries | 2026-06-22 | system                                                                                     |      |     |
| #901  | bugfix/829: Automatic Directory Index touch configuration and post-indexing safety net     | 2026-06-22 | *unclear*                                                                                  |      |     |
| #911  | bugfix/910: Fix Traffic Gadget I18N is not defined reference error                         | 2026-06-24 | WebUI, system                                                                              |      |     |
| #912  | bugfix/909: Fix Directory Widget Last Name filter to preserve Organization and Department  | 2026-06-24 | system                                                                                     |      |     |
| #913  | bugfix/879: Replace straight apostrophes with curly apostrophes in user-facing site path s | 2026-06-24 | projects, system                                                                           |      |     |
| #914  | Fix search query ParseException on path-based terms (fixes #878)                           | 2026-06-24 | projects                                                                                   |      |     |
| #915  | Downgrade PDFBox to 2.0.31 to fix NoSuchMethodError in Tika                                | 2026-06-24 | system                                                                                     |      |     |
| #916  | Hardcode buildNumber in Version.properties and establish update guidelines                 | 2026-06-24 | CHANGELOG.md, modules/DesktopContentExplorer, system                                       |      |     |
| #918  | bugfix: Skip running Dependency Submission workflow on dependency submission commits (#348 | 2026-06-25 | *unclear*                                                                                  |      |     |
| #919  | bugfix: Guard elements in setCaretToEnd on Admin Console (#906)                            | 2026-06-24 | CHANGELOG.md, system                                                                       |      |     |
| #921  | Fix build number format startup crash                                                      | 2026-06-25 | CHANGELOG.md, deployer, modules/DesktopContentExplorer, system                             |      |     |
| #923  | bugfix/757: Fix footer widget misalignment in CMS page editor                              | 2026-06-25 | CHANGELOG.md, WebUI                                                                        |      |     |
| #924  | Fix folder creation 'Path not found' error popup (#867)                                    | 2026-06-25 | CHANGELOG.md, projects                                                                     |      |     |
| #929  | Fix duplicate validation errors and map validation exceptions to BAD_REQUEST (#866)        | 2026-06-25 | CHANGELOG.md, WebUI, projects                                                              |      |     |
| #931  | Fix #879: Use curly apostrophe to prevent HTML encoding of error messages                  | 2026-06-25 | projects, system                                                                           |      |     |
| #932  | Fix #930: Repopulate department dropdown when clearing last name filter                    | 2026-06-25 | system                                                                                     |      |     |
| #933  | Fix #878: Search fails for path-based terms + compilation fix                              | 2026-06-25 | projects                                                                                   |      |     |
| #1152 | bugfix/867: Fix folder creation 'Path not found' error popup                               | 2026-06-25 | WebUI, projects                                                                            |      |     |
| #1155 | bugfix/1153: Sync reference packages with source content type labels                       | 2026-06-25 | CHANGELOG.md, modules/perc-packages                                                        |      |     |
| #1156 | bugfix/867: Suppress false 'Path not found' dialog after folder rename                     | 2026-06-26 | CHANGELOG.md, WebUI                                                                        |      |     |
| #1158 | slim down compiler settings                                                                | 2026-06-27 | PCM-PkgMgtUI, WebUI, delivery, deliverytiersuite/delivery-tier-suite, deployer, modules... |      | Y   |
| #1159 | Feature/add temp to agents                                                                 | 2026-06-30 | *unclear*                                                                                  |      |     |
| #1161 | bugfix: dashboard proxy scheme fallback and logging for spec retrieval failures            | 2026-06-28 | WebUI, system                                                                              |      |     |
| #1165 | don't write to source folders when running tests                                           | 2026-06-30 | modules/jcadf-master                                                                       |      |     |
| #1166 | fix install fail on buildid                                                                | 2026-06-30 | modules/jcadf-master                                                                       |      |     |
| #1168 | feature/updateaipluginversion                                                              | 2026-07-01 | *unclear*                                                                                  |      |     |
| #1169 | bugfix/issue-784-Categories tab fancytree browser errors                                   | 2026-07-03 | WebUI, scripts                                                                             |      |     |
| #1171 | Fix category name input issues and improve validation alerts in Admin…                     | 2026-07-06 | CHANGELOG.md, WebUI, system                                                                |      |     |
| #1173 | 1182-Fix stale user lock issue                                                             | 2026-07-06 | projects                                                                                   |      |     |

## 4. Categorized Summary (mapped to Release Notes sections)

### Accessibility / WCAG 2.1 AA (release notes: `Accessibility Improvements`)

- 

# 658 Update form widget version to 1.4.8 and set accessibility attributes …

- 

# 823 bugfix/820: Prevent duplicate entries in Insert Global Variable dialog

- 

# 827 bugfix/663: Backport WCAG 2.1 AA navigation landmarks and aria-label …

- 

# 883 Fix #880 #881: Remove global jQuery UI tooltip that caused tooltips t…

### GA4 / Google Analytics Migration

- 

# 760 Feature/756 ga4 analytics migration

- 

# 773 bugfix/768: Fix null pointer exception in Google Setup gadget

- 

# 861 bugfix/860: Fix invalid Google Analytics date format in Traffic Gadget

- 

# 885 Fix #882: Undeprecate Google Setup, Traffic, and What's Working gadgets

- 

# 911 bugfix/910: Fix Traffic Gadget I18N is not defined reference error

### Workflow Automation (Directory Index Touch)

- 

# 837 Bugfix/829 Auto Touch Pages with Directory index when Directory related Assets are approved

- 

# 863 bugfix/829: Automatic configuration of Directory Index Touch workflow…

- 

# 901 bugfix/829: Automatic Directory Index touch configuration and post-indexing safety net

### REST API Improvements (Page-by-Path, validation, DELETE)

- 

# 846 bugfix/818: Update CSP in restored perc-security.properties on DTS up…

- 

# 894 GH-891: Support leading Sites/ in Page by Path REST resource

- 

# 898 bugfix/866: Guard page and parent path validations when source folder is not found

- 

# 900 bugfix/862: Add implicit.tld files to force JSP 2.1 compilation for implicit tag libraries

- 

# 929 Fix duplicate validation errors and map validation exceptions to BAD_REQUEST (#866)

- 

# 1171 Fix category name input issues and improve validation alerts in Admin…

### Folder & Path Management (bugfix/867 family)

- 

# 665 Log a warning for orphaned managed links in findPagesLinkedToItem met…

- 

# 869 bugfix/866: Stop validator early on folder-not-found to prevent dupli…

- 

# 872 bugfix/867: Fix folder creation path-not-found error caused by incorr…

- 

# 886 Fix #879: Friendly error message for orphaned pages

- 

# 897 GH-867: Normalize validateEnteredPath input and handle virtualized site prefix

- 

# 898 bugfix/866: Guard page and parent path validations when source folder is not found

- 

# 924 Fix folder creation 'Path not found' error popup (#867)

- 

# 1152 bugfix/867: Fix folder creation 'Path not found' error popup

- 

# 1156 bugfix/867: Suppress false 'Path not found' dialog after folder rename

- 

# 1165 don't write to source folders when running tests

### Search & Directory Widgets

- 

# 837 Bugfix/829 Auto Touch Pages with Directory index when Directory related Assets are approved

- 

# 863 bugfix/829: Automatic configuration of Directory Index Touch workflow…

- 

# 870 bugfix/868: Fix theme list serialization and load error in style gall…

- 

# 889 Fix Lucene ParseException for path-based search terms

- 

# 890 Fix: Recalculate directory widget alphabet filters dynamically on dropdown selection changes

- 

# 892 Fix: Fully reset Organization, Department, and search values when clearing filters

- 

# 896 GH-876: Enforce size constraints on theme preview thumbnails in Style tab

- 

# 901 bugfix/829: Automatic Directory Index touch configuration and post-indexing safety net

- 

# 912 bugfix/909: Fix Directory Widget Last Name filter to preserve Organization and Department filters

- 

# 914 Fix search query ParseException on path-based terms (fixes #878)

- 

# 932 Fix #930: Repopulate department dropdown when clearing last name filter

- 

# 933 Fix #878: Search fails for path-based terms + compilation fix

### Bookmarks & Dashboard Tooltips

- 

# 883 Fix #880 #881: Remove global jQuery UI tooltip that caused tooltips t…

- 

# 885 Fix #882: Undeprecate Google Setup, Traffic, and What's Working gadgets

- 

# 893 GH-877: Filter out recycled pages from My Bookmarks

### Publishing

- 

# 736 Bugfix/dts windows 672

- 

# 762 bugfix/749: Fix auto list widgets not working on published sites

- 

# 782 fix: DTS startup warnings, Java 8 TLS/cipher config, and JUL bridge n…

- 

# 846 bugfix/818: Update CSP in restored perc-security.properties on DTS up…

- 

# 848 Deprecate Secure Login widget and associated DTS security configurations

- 

# 851 Fix NullPointerException during publish for newly created sites

- 

# 853 bugfix/849: Handle concurrency exceptions gracefully during publish jobs

- 

# 856 bugfix/847: Default DTS Server dropdown to NONE when not configured

- 

# 859 Bugfix/847 dts server dropdown none

### Workflow (validation regex, saves)

- 

# 108 Add dependency submission workflow for development-8.1.x branch

- 

# 863 bugfix/829: Automatic configuration of Directory Index Touch workflow…

- 

# 874 bugfix/871: Escape hyphen in workflow name and step name regex patter…

- 

# 918 bugfix: Skip running Dependency Submission workflow on dependency submission commits (#348)

### Content Security (duplicate meta tags, OG, Twitter)

- 

# 788 Bugfix/785 fix view metadata tags categories

- 

# 807 bugfix/803: Fix missing twitter:site metadata and update branding/log…

- 

# 812 bugfix/809: Rebrand Twitter to X in Social Buttons widget

- 

# 814 bugfix/811: Prevent duplicate Facebook Open Graph tags from being ren…

- 

# 821 bugfix/809: Fix alignment and sizing of X (Twitter) social icon

- 

# 822 Bugfix/803 twitter site metadata

- 

# 838 bugfix(803): remove duplicate twitter:site tag insertion block

### Security / CVE / Cipher / CSP

- 

# 107 Feature/dependency updates=2025

- 

# 149 Feature/spotless google format baseline

- 

# 245 Feature/post security update fixes 12 20

- 

# 302 Maintenance: Fix slow running system tests & security fixes for depends updates

- 

# 419 Java 1.8 Toolchain and 2025 security update refactoring

- 

# 420 Bugfix/post security update pass 3

- 

# 504 Bugfix/jcr icu update testing

- 

# 509 Bugfix/jetty logging fixes

- 

# 515 Bugfix/510 bad executable bit post install

- 

# 525 Feature/8.1.6 release prep

- 

# 526 Bump version to 8.1.7

- 

# 676 Bugfix/issue 675 jackson null handling

- 

# 760 Feature/756 ga4 analytics migration

- 

# 782 fix: DTS startup warnings, Java 8 TLS/cipher config, and JUL bridge n…

- 

# 825 bugfix/818: Allow data: and blob: image URIs in Content Security Poli…

- 

# 826 bugfix/728: Prevent bulk upload queue count from going negative when …

- 

# 846 bugfix/818: Update CSP in restored perc-security.properties on DTS up…

- 

# 848 Deprecate Secure Login widget and associated DTS security configurations

- 

# 861 bugfix/860: Fix invalid Google Analytics date format in Traffic Gadget

- 

# 1158 slim down compiler settings

### Build / Release / Toolchain

- 

# 132 Revert "Bump cxf.version from 3.5.9 to 3.6.9"

- 

# 149 Feature/spotless google format baseline

- 

# 303 Revert "Bump com.diffplug.spotless:spotless-maven-plugin from 2.30.0 to 3.1.0"

- 

# 304 Fix PDFBox loading method in PSTextConverterPdf to use new API

- 

# 416 Revert "Bump commons-digester:commons-digester from 1.8.1 to 2.1"

- 

# 419 Java 1.8 Toolchain and 2025 security update refactoring

- 

# 499 Revert "Bump javax.jcr:jcr from 1.0 to 2.0"

- 

# 500 Revert "Bump com.phloc:phloc-commons from 4.3.9 to 5.0.0"

- 

# 509 Bugfix/jetty logging fixes

- 

# 524 Correct v8.1.6 release notes dependency versions for Java 8 compatibility

- 

# 525 Feature/8.1.6 release prep

- 

# 526 Bump version to 8.1.7

- 

# 572 Fix #571 for new PDFbox version

- 

# 716 Bugfix/8.1.7 release testing

- 

# 782 fix: DTS startup warnings, Java 8 TLS/cipher config, and JUL bridge n…

- 

# 915 Downgrade PDFBox to 2.0.31 to fix NoSuchMethodError in Tika

- 

# 916 Hardcode buildNumber in Version.properties and establish update guidelines

- 

# 921 Fix build number format startup crash

- 

# 1158 slim down compiler settings

### Platform Modernization / Deprecations

- 

# 722 For #720.  Add deprecated lists to ui, add widgets and gadgets to be …

- 

# 794 Bugfix/793 redirect management to deprecated

- 

# 848 Deprecate Secure Login widget and associated DTS security configurations

- 

# 850 Deprecate Registration widget

- 

# 885 Fix #882: Undeprecate Google Setup, Traffic, and What's Working gadgets

### Admin Console / JSP / TinyMCE

- 

# 830 Bugfix/828 lock timeout rich text save

- 

# 919 bugfix: Guard elements in setCaretToEnd on Admin Console (#906)

### JCR / ICU / PhantomJS removals

- 

# 499 Revert "Bump javax.jcr:jcr from 1.0 to 2.0"

- 

# 504 Bugfix/jcr icu update testing

### JDK8-specific (must be re-evaluated for migration)

PRs flagged with `javax.*`/`juel`/`jaxb` markers, explicit Java 8 mentions, or revert-to-javax commits. These likely need to be **dropped or re-implemented** on `development` (JDK 21 / Jakarta).

- 

# 149 Feature/spotless google format baseline

- 

# 499 Revert "Bump javax.jcr:jcr from 1.0 to 2.0"

- 

# 524 Correct v8.1.6 release notes dependency versions for Java 8 compatibility

- 

# 782 fix: DTS startup warnings, Java 8 TLS/cipher config, and JUL bridge n…

### Uncategorized (data gap — needs manual mapping)

PRs whose title does not obviously map to a release-notes section:

- 

# 110 Add exclusion / legacy axis reference to fix action failure

- 

# 111 Feature/fix webservice action failure

- 

# 112 Feature/fix webservice action failure

- 

# 113 Feature/fix webservice action failure

- 

# 114 Feature/fix webservice action failure

- 

# 115 Feature/fix webservice action failure

- 

# 116 Feature/fix webservice action failure

- 

# 117 Feature/fix webservice action failure

- 

# 118 Feature/fix webservice action failure

- 

# 119 Feature/fix webservice action failure

- 

# 138 Fix failing test and update dependencies

- 

# 491 Downgrade myfaces to 1.8 compatible version

- 

# 573 Rollback jackson version update

- 

# 574 Bugfix/jackson compatibility

- 

# 601 fix(#600): Add missing role and other HTML attributes to html-cleaner.properties

- 

# 653 Add missing navigation role to breadcrumb widget.  Update widget vers…

- 

# 727 Bulk upload gadget 710

- 

# 735 Improve error reportinng when email not configured.  Add report help …

- 

# 755 fix: #750 missing bullets + #753 Category List fancytree errors in de…

- 

# 761 bugfix/758: Fix category creation node activation and Fancytree seria…

- 

# 763 bugfix/757: Fix footer misalignment by using min-height for vspan reg…

- 

# 764 bugfix/748: Replace sqlCheck with indexExists to eliminate SQL precon…

- 

# 767 Bugfix/757 footer alignment fix

- 

# 771 Bugfix/749 fix auto list widgets

- 

# 772 Bugfix/766 category browser fancytree

- 

# 774 bugfix/756: Fix JSON payload leaking in extractDefaultErrorMessage wh…

- 

# 775 fix(bulk upload gadget): show error message when zero-size file uploa…

- 

# 776 Bugfix/728 bulk upload gadget zero size error

- 

# 778 Refactor Admin Category editor UI

- 

# 781 Feature/admin category UI refactor

- 

# 783 fix: Admin Category UI - overlapping icons, tree rendering, move up/d…

- 

# 786 Fix View Metadata dialog rendering for Tags and Categories (#785)

- 

# 789 Bugfix/757 footer alignment fix

- 

# 796 Bugfix/795 calendar widget hidecalendarsource

- 

# 813 bugfix/801: Fix Image Slider Edit Template warning and handle relativ…

- 

# 816 bugfix/815: Fix JavaScript errors in Image Slider widget content edit…

- 

# 824 bugfix/819: Correct load order and defer dependent scripts when using…

- 

# 834 Bugfix/833 sitewide framework gadget

- 

# 835 Bugfix/832 cookie consent syntax error

- 

# 836 Bugfix/831 report email attachment error

- 

# 855 bugfix/663: Add missing Jexl navLabel property retrieval to Nav/Bread…

- 

# 864 bugfix/757: Revert editor styling overrides to keep original min-heig…

- 

# 865 bugfix/862: Fix ClassCastException String to RowKeySet by upgrading l…

- 

# 899 bugfix/895: Safe guard getPageIdCallback in new site dialog to avoid JS error

- 

# 913 bugfix/879: Replace straight apostrophes with curly apostrophes in user-facing site path service error messages

- 

# 923 bugfix/757: Fix footer widget misalignment in CMS page editor

- 

# 931 Fix #879: Use curly apostrophe to prevent HTML encoding of error messages

- 

# 1155 bugfix/1153: Sync reference packages with source content type labels

- 

# 1159 Feature/add temp to agents

- 

# 1161 bugfix: dashboard proxy scheme fallback and logging for spec retrieval failures

- 

# 1166 fix install fail on buildid

- 

# 1168 feature/updateaipluginversion

- 

# 1169 bugfix/issue-784-Categories tab fancytree browser errors

- 

# 1173 1182-Fix stale user lock issue

## 5. Dependabot PRs Excluded (229 total)

Full list retained in `tmp/dependabot-excluded.txt`. First 40 entries (sample):

```
#63 | Bump owasp.csrfguard.version from 4.0.0 to 4.5.0-jakarta | merged 2025-08-19
#64 | Bump commons-codec:commons-codec from 1.14 to 1.19.0 | merged 2025-08-19
#66 | Bump jetty.version from 9.4.54.v20240208 to 9.4.58.v20250814 | merged 2025-08-19
#67 | Bump rome.version from 1.15.0 to 2.1.0 | merged 2025-08-19
#69 | Bump org.apache.xmlgraphics:fop from 2.2 to 2.11 | merged 2025-08-19
#72 | Bump commons-beanutils:commons-beanutils from 1.9.4 to 1.11.0 | merged 2025-08-20
#73 | Bump javax.mail:mail from 1.4.1 to 1.4.7 | merged 2025-08-20
#74 | Bump cactus:cactus from 13-1.7.1 to 13-1.7.2 | merged 2025-08-20
#76 | Bump org.jboss.arquillian.test:arquillian-test-impl-base from 1.1.11.Final to 1.10.0.Final | merged 2025-08-20
#83 | Bump org.dom4j:dom4j from 2.1.3 to 2.2.0 | merged 2025-08-31
#84 | Bump com.h3xstream.findsecbugs:findsecbugs-plugin from LATEST to 1.14.0 | merged 2025-08-31
#86 | Bump org.jboss.shrinkwrap:shrinkwrap-impl-base from 1.2.3 to 1.2.6 | merged 2025-08-31
#87 | Bump org.apache.xmlgraphics:xmlgraphics-commons from 2.6 to 2.11 | merged 2025-08-31
#91 | Bump cxf.version from 3.5.8 to 3.6.8 | merged 2025-08-25
#92 | Bump spring.version from 5.3.27 to 5.3.39 | merged 2025-08-31
#96 | Bump wsdl4j:wsdl4j from 1.6.2 to 1.6.3 | merged 2025-09-03
#97 | Bump trinidad.version from 1.0.2 to 2.2.1 | merged 2025-12-16
#99 | Bump tika.version from 1.28.5 to 2.4.1 | merged 2025-12-16
#100 | Bump org.liquibase.ext:liquibase-hibernate5 from 4.0.0 to 4.27.0 | merged 2025-12-16
#101 | Bump com.google.inject.extensions:guice-jmx from 2.0 to 7.0.0 | merged 2025-12-16
#103 | Bump jackson.version from 2.14.2 to 2.20 | merged 2025-12-16
#124 | Bump twelvemonkeys.version from 3.9.4 to 3.12.0 | merged 2025-12-18
#127 | Bump org.apache.maven.plugins:maven-gpg-plugin from 1.6 to 3.2.8 | merged 2025-12-18
#128 | Bump com.fasterxml:classmate from 1.5.1 to 1.7.1 | merged 2025-12-18
#129 | Bump log4j2.version from 2.18.0 to 2.25.3 | merged 2025-12-18
#130 | Bump cxf.version from 3.5.9 to 3.6.9 | merged 2025-12-18
#134 | Bump velocity.version from 2.3 to 2.4.1 | merged 2025-12-18
#136 | Bump joda-time:joda-time from 2.8.1 to 2.14.0 | merged 2025-12-18
#137 | Bump org.apache.commons:commons-csv from 1.0 to 1.14.1 | merged 2025-12-18
#140 | Bump com.thoughtworks.xstream:xstream from 1.4.20 to 1.4.21 | merged 2025-12-19
#142 | Bump de.odysseus.juel:juel-impl from 2.2.2 to 2.2.7 | merged 2025-12-19
#146 | Bump junit:junit from 4.13.1 to 4.13.2 | merged 2025-12-19
#147 | Bump org.apache.maven.scm:maven-scm-provider-perforce from 1.9.2 to 1.13.0 | merged 2025-12-19
#151 | Bump cxf.version from 3.5.9 to 3.5.11 | merged 2025-12-19
#159 | Bump com.google.gwt:gwt-user from 2.8.2 to 2.10.0 | merged 2025-12-19
#160 | Bump org.apache.maven.plugins:maven-jarsigner-plugin from 3.0.0 to 3.1.0 | merged 2025-12-19
#162 | Bump com.google.api-client:google-api-client from 1.25.0 to 2.8.1 | merged 2025-12-19
#167 | Bump org.apache.commons:commons-compress from 1.26.0 to 1.28.0 | merged 2025-12-19
#170 | Bump com.google.gwt:gwt-servlet from 2.8.2 to 2.10.0 | merged 2025-12-19
#171 | Bump org.yaml:snakeyaml from 2.0 to 2.5 | merged 2025-12-19
...
```

Range: PR #63 (Bump owasp.csrfguard.version, merged 2025-08-19) through approximately PR #1270+.

### Most-bumped components in excluded dependabot set

|           Component            | Count |
|--------------------------------|-------|
| `org.apache.maven.plugins`     | 27    |
| `org.codehaus.mojo`            | 11    |
| `org.apache.commons`           | 10    |
| `org.apache.xmlgraphics`       | 4     |
| `com.google.http-client`       | 4     |
| `commons-codec`                | 3     |
| `cxf.version`                  | 3     |
| `jackson.version`              | 3     |
| `twelvemonkeys.version`        | 3     |
| `com.fasterxml`                | 3     |
| `com.google.api-client`        | 3     |
| `org.apache.httpcomponents`    | 3     |
| `swagger.version`              | 3     |
| `junit.jupiter.version`        | 3     |
| `org.dom4j`                    | 2     |
| `tika.version`                 | 2     |
| `com.google.inject.extensions` | 2     |
| `de.odysseus.juel`             | 2     |
| `com.google.gwt`               | 2     |
| `io.netty`                     | 2     |
| `com.google.apis`              | 2     |
| `org.apache.xbean`             | 2     |
| `awssdk.version`               | 2     |
| `org.codehaus.plexus`          | 2     |
| `net.sf.ehcache`               | 2     |
| `org.webjars`                  | 2     |
| `org.apache.jackrabbit`        | 2     |
| `pdfbox.version`               | 2     |
| `org.jsoup`                    | 2     |
| `org.apache.shiro`             | 2     |

(Note: dependency bumps that were *manually* written — e.g. PR #107 `Feature/dependency updates=2025`, #245 `post security update fixes`, #419 `Java 1.8 Toolchain and 2025 security update refactoring` — are NOT in this list; they are in the 141 non-dependabot set, since they reflect curated/combined changes.)

## 6. Open Questions / Data Gaps

1. **Cherry-pick vs. true merge.** Many PRs in the v8.1.7 lineage were originally merged to `development` (JDK 21) and then cherry-picked to `development-8.1.x`. The 141-PR inventory merges both. For migration to `development`, prefer the **original `development`-base PR** (different PR number in some cases — e.g. the bugfix PRs in the 870–930 range were originally opened on `development` and forward-ported). Cross-reference with `gh pr list --state merged --base development` is recommended for each item.

2. **Module inference from files-changed.** Module paths were derived from `gh api .../files --paginate`. For PRs whose title doesn't clearly indicate a module (e.g. #789 `Bugfix/757 footer alignment fix` — returned 0 files due to API pagination timing; #901 `bugfix/829 Automatic Directory Index Touch...` — same), the Module column is `*unclear*`. These should be re-verified manually.

3. **JDK8-only classification is heuristic.** Detection of `javax.*`, `juel`, `jaxb`, etc. is keyword-based. Manual review of each JDK8-flagged PR is required; some apply to both surfaces (e.g. #782 `DTS TLS/cipher config` — cipher suite list differs between JDK 8 and 21, but the PR is conceptually portable).

4. **Security flag is heuristic.** Driven by keywords (`cve`, `security`, `cipher`, `shiro`, `tls`, `csp`, `x-frame`, `csrf`, etc.). The release-notes Security Updates section (#107, #245, #419, #420, #504, #509, #515, #782, #825, #846) maps cleanly, but the Shiro 2.1.0 / Tomcat 9.0.115 upgrades listed in release notes are likely bundled into PR #419 (`Java 1.8 Toolchain and 2025 security update refactoring`) or dependabot PRs.

5. **Dependabot PRs (#63–#1270+):** 229 PRs were excluded. The corresponding dependency-version *outcomes* must still be re-applied on `development` if not already there. Use `.github/dependabot.yml` exclusions on `development` branch as the ground truth.

6. **Build/release-prep commits.** The v8.1.6..v8.1.7 merge log contains many `chore: update build number` and `maven-release-plugin` commits — these are not PRs and are not in the inventory; they should be skipped during migration.

7. **PRs not yet in v8.1.7 (carry `8.1.8` label).** PRs #1166, #1168, #1173 were merged to `development-8.1.x` after the v8.1.7 cut and appear in the inventory because they target 8.1.x post-v8.1.6. **Treat them as v8.1.8 candidates, not v8.1.7.** Their content:

   - 

   # 1166 — `fix install fail on buildid`

   - 

   # 1168 — `feature/updateaipluginversion` (AI plugin bump)

   - 

   # 1173 — `1182-Fix stale user lock issue`

8. **AGENTS.md / copilot-instructions changes (#1159, #1168).** Tooling-only changes that don't affect runtime code.

9. **Empty `files` arrays.** Several PRs returned 0 files from `gh api .../files --paginate`. Likely API pagination timing. Re-fetch without `--jq` or use a larger page size to confirm.

10. **No PR-side diff cross-check.** Each PR's full diff was NOT fetched (only file paths). If a migration decision hinges on actual code changes (e.g. to determine if a fix is purely configuration vs. behavioral), the diff must be inspected.

---

## Methodology Notes (Per-PR Verdict Procedure)

This section documents a repeatable procedure for assigning each v8.1.7 PR one of the five verdict classifications used by spec FR-003:

- `already-present` — the v8.1.7 fix (or a functionally equivalent fix) is already on `development`.
- `needs-migration` — the fix is NOT on `development`; a cherry-pick or manual port is required.
- `not-applicable` — the fix is JDK-8-only or targets a dependency that `development` already replaced.
- `superseded` — `development` solved the same problem by a different code path or different import / API.
- `conflicts-with-newer-design` — the v8.1.7 fix touches a file/module that `development` deleted or refactored away.

### Inputs

- `BASE` = `development` local branch HEAD. Confirmed at this run: `6fd1e683166a67036ffc52712b78bf2ffa67e6c8`.
- `TAG` = `v8.1.7`. Confirmed merge SHA: `ebad961e8dc395a2979c2b3b61436b87a5ebf372` (parent is `1152c10f4344de580e74861723677f4944ed1602` from maven-release-plugin).
- Sampled PR list (20 PRs spread across categories): see Section "Per-PR Sample Verdicts" below.
- Each PR's merge commit SHA was fetched via `gh pr view <PR> --json mergeCommit`.
- Each PR's changed file list was fetched via `gh pr view <PR> --json files`.
- Each PR's full diff was fetched via `gh pr diff <PR>` (cached at `tmp/release-audit/v8.1.7/sample-diffs-full.txt` for re-use).

### Per-PR Decision Steps

For each PR:

1. **Read the v8.1.7 diff** to identify (a) the function/field/CSS rule/error message that is being added or changed, (b) the list of file paths changed.
2. **Verify each file exists at the path implied by the diff on `development`.** Older 8.1.x paths such as `system/Packages/perc.widgets.nav/sys__UserDependency--rxconfig/Widgets/percNavBar.xml` were moved on `development` to `modules/perc-packages/src/main/resources/Packages/...`. Always run `git ls-tree -r --name-only development -- <dir-glob>` first to discover the new path; do not assume 8.1.x paths still exist. If the file is absent entirely (e.g. `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml`, which was deleted in commit `a16d21e972`), the verdict is `superseded` or `conflicts-with-newer-design`.
3. **Find the "key string" added/changed in the diff** — typically the new error message, new field default, new regex, new CSP directive, new error class name, etc. Examples used in this run:
   - PR #763: `min-height : 120px` (vs original `height : 120px`)
   - PR #767: `vspan_2 { height : 120px !important` (with the `!important` override)
   - PR #827 / #855: `navLabel` and `aria-label="$!{navLabel}"` in nav widgets
   - PR #886: `"The requested page is no longer available."` (vs `"This page should have been deleted when its site was deleted."`)
   - PR #894: `normalizePath` helper method in `PagesResource`
   - PR #889: `QueryParser.escape(` (vs `QueryParserUtil.escape(`)
   - PR #851: `pubServer.getPublishServer().equalsIgnoreCase("none")` direct call (vs `pubServer.getProperty("publishServer").getValue()`)
   - PR #853: `isConcurrencyException(` static method on `PSAbstractWorkflowExtension`
   - PR #915: `pdfbox.version>2.0.31` and absence of `pdfbox-io` artifact
   - PR #794 / #885: `name="Redirect Management"` in `GadgetRegistry.xml`
   - PR #850: `Registration Asset (Deprecated)` label and `<widget name="Registration (Deprecated)" />`
4. **Run `git show development:<path> | grep '<key-string>'`**. Three outcomes:
   - Match found (verbatim) → `already-present` (cite the file path on `development` and, when possible, the matching commit SHA).
   - No match and the file exists → `needs-migration`.
   - The file is gone (or the dev-trees match the function signature replaced by a *different* implementation with the same net effect, e.g. `Optional`-based `getPublishServer`) → `superseded`.
5. **Sanity-check with `git log development -1 --oneline -- <path>`** — confirm the last commit on `development` affecting that path is older than the v8.1.7 merge, otherwise the dev branch may have independently evolved away from the file's prior state.
6. **For deletions**, additionally run `git grep -n '<key-string>' development` to confirm the key string is genuinely absent across the work-tree, not just in the original path. PR #794 and #885 are good examples where the key string `Redirect Management` is no longer in any tracked file in `development`.
7. **For "not-applicable"**, confirm dependency or JDK-version reasons: the dev branch is on a different PDFBox (3.0.6)/Tika (3.2.3) combination, on Java 21 / Jakarta EE 10, or uses Lucene 8 with the legacy `queryparser.flexible` package — none of which exhibit the v8.1.7 trigger condition.

### Automation Hooks

The entire procedure can be packaged as a Bash script and run via:

```bash
./scripts/release-audit/classify-pr.sh \
    --tag v8.1.7 \
    --target-branch development \
    --pr-numbers 763,767,827,855,883,...
```

Inputs it should take:

- `--tag` — the new release tag (defaults to `v8.1.7`).
- `--target-branch` — the back-port target (defaults to `development`).
- `--from-tag` — the previous release tag (optional, otherwise inferred from `mvn versions`).
- `--prs-file` — a file with one PR number per line (or stdin) for batching.
- `--skip-deps` — auto-exclude dependabot[bot] PRs.

For each PR it should:

1. Use `gh pr view <PR> --json files,mergeCommit` to get path list and merge SHA.
2. Use `git log <mergeSHA>^..<mergeSHA> --name-only --pretty=format:` to re-derive paths independently from `gh` (cross-check #1 above).
3. For each path, run `git show development:<path>` and compare against the unified diff hunk.
4. Grep the dev tree for any new symbol introduced by the PR (e.g. `git grep -n '<new-error-message>' development`).
5. Write a row to a CSV: `pr,verdict,evidence_path,evidence_commit_sha_or_hash`.

### Caveats Discovered During This Run

1. **Path rewrites.** The package layout was moved from `system/Packages/` to `modules/perc-packages/src/main/resources/Packages/` in commit `f5a33ea8bd` ("Refactor package building to perc-packages…") on the `development` branch. The audit must resolve the new path before any string search. The legacy path may still appear in the `8.1.x` worktrees under Docker/dev mounts (e.g. `docker/dev-data/cms-dts/jetty/base/webapps/Rhythmyx/WEB-INF/classes/com/percussion/webui/gadget/servlets/GadgetRegistry.xml`) but those are runtime artefacts, not source.
2. **File deletions.** Several 8.1.x source files were deleted on `development` (e.g. `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` deleted by `a16d21e972`) — the audit must detect these as `superseded`/`conflicts-with-newer-design`, not `needs-migration`.
3. **Divergent functional equivalents.** Some v8.1.7 fixes were independently rewritten on `development` against a different library API (Lucene 8 `queryparser.classic` vs. dev's pre-existing `queryparser.flexible` + custom `escapeLuceneQuery`; PDFBox 3.x `Loader.loadPDF` instead of v8.1.7's downgrade to 2.0.31). These are verdicts of `superseded` or `not-applicable` rather than `already-present`.
4. **Cherry-pick history noise.** Many files carry lineage that includes both a JDK-21 evolution AND a subsequent 8.1.x-only tweak. The diff approach above (read the v8.1.7 diff, search dev for its key strings) is robust to this because it asks "is the dev tree's state equivalent", not "did the v8.1.7 commit show up".
5. **JDK-21 stabilization baseline.** Commit `a16d21e97295ee513c67ffad84630ed4aa6c21b4` (Feature/jdk 21 stabilization #605) is the de-facto baseline that v8.1.7 needs to be migrated on top of. Anything earlier than that on `development` was a JDK-11 draft; anything later is post-stabilization and should be checked individually.

---

## Per-PR Sample Verdicts

20-PR sample chosen to cover UI/accessibility, REST API, publishing/workflow, security/dependency, deprecation/refactor, and other (gadget/CMS-editor) categories. `BASE` = `6fd1e683166a67036ffc52712b78bf2ffa67e6c8` (development HEAD at this run).

### UI / Accessibility (5)

#### PR #763 — "bugfix/757: Fix footer misalignment by using min-height for vspan reg…"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `e71a24c58f8b14e697395ad4835bf794c38223e9`
- **v8.1.7 change:** Swaps `height : Xpx` → `min-height : Xpx` for `.vspan_{2,4,6,8}` in `system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css`.
- **Evidence on development:** `git show development:system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css` still emits `.vspan_2 { height: 120px; }` (lines 60–72 in the current worktree) — the `min-height` swap is **not** present. Last touch on `development` for the file: `a16d21e972` (Feature/jdk 21 stabilization #605).

#### PR #767 — "Bugfix/757 footer alignment fix"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `285ad7845f90165128fd3ad3616ad9ae8512702f`
- **v8.1.7 change:** Adds `!important` overrides in `WebUI/war/css/perc_decoration.css`: `.vspan_2 { height : 120px !important; min-height: 0 !important; }` (and 4/6/8 variants).
- **Evidence on development:** No occurrence of `height : 120px !important` or `vspan_2.*!important` in any `*.css` on `development`. `git show development:WebUI/war/css/perc_decoration.css` shows plain `.vspan_2 { height: 120px; }` — no override.

#### PR #827 — "bugfix/663: Backport WCAG 2.1 AA navigation landmarks and aria-label…"

- **Verdict:** `already-present`
- **v8.1.7 merge commit:** `87a7de7a4e6099dab39a92cc4d79e04c858298f6`
- **v8.1.7 change:** Adds `navLabel` `UserPref` and `aria-label="$!{navLabel}"` on `<nav>` elements in `system/Packages/perc.widgets.nav/sys__UserDependency--rxconfig/Widgets/{percNavBar,percNavBreadcrumb}.xml`; bumps package to 1.3.3.
- **Evidence on development:** Path was rewritten on `development` to `modules/perc-packages/src/main/resources/Packages/perc.widgets.nav/sys__UserDependency--rxconfig/Widgets/percNavBar.xml` (rename refactor in `f5a33ea8bd`). At the new path, `git show development:.../percNavBar.xml` includes `<UserPref name="navLabel" …/>` (line 51) and `<nav aria-label="$!{navLabel}">` (line 129). `percNavBreadcrumb.xml` also has the `navLabel` `UserPref` and two `<nav aria-label="$!{navLabel}">` elements (lines 58, 88). `psx_archiveInfo.xml` is at version 1.3.3 (line 26).
- **Migration strategy:** skip — both files and the version bump are already in `development`. (Note: PR #855, which is the follow-up that fixed the missing Jexl `navLabel` binding, is also already present in `development` — see PR #855 below.)

#### PR #855 — "bugfix/663: Add missing Jexl navLabel property retrieval…"

- **Verdict:** `already-present`
- **v8.1.7 merge commit:** `0fc2f421ff66061a3c5eb3be3dafd86f49412f76`
- **v8.1.7 change:** Adds `$navLabel = $perc.widget.item.properties.get('navLabel');` to the Jexl block at the top of both widget `.xml`s; bumps package 1.3.3 → 1.3.4.
- **Evidence on development:** The Jexl binding line `navLabel = $perc.widget.item.properties.get('navLabel');` is present in both `percNavBar.xml` (line ~80) and `percNavBreadcrumb.xml` (line ~42). Package version is `1.3.3` rather than `1.3.4`, but this is functionally equivalent — the upstream work landed on the 1.3.3 package (likely the same commit that this PR back-ports the *follow-up* to).

#### PR #883 — "Fix #880 #881: Remove global jQuery UI tooltip that caused tooltips…"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `21b94950ad9fb77d3d3e056b2609ae780871bb9a`
- **v8.1.7 change:** Removes the `jquery-ui` `define` dependency from `cui/widgets/contentList/contentList.ViewModel.js` and deletes the global `$(document).tooltip({...})` initialization block.
- **Evidence on development:** The file now exists at three worktree locations (`WebUI/src/main/webapp/cm/cui/widgets/contentList/contentList.ViewModel.js`, `WebUI/src/main/webapp/cm/pages/cui/widgets/contentList/contentList.ViewModel.js`, `WebUI/war/cui/widgets/contentList/contentList.ViewModel.js`). On `development`, all three locations still contain `define(["knockout", "pubsub", "utils", "jquery-ui"], function (...)` (line 18) and a `$(document).tooltip({...})` block (around line 470). The dev has a separate `f3e3784c3f "Remove deprecated jQuery UI and related packages"` commit, but that did not propagate to this file. PR #883's removal is **not** present.

### REST API (3)

#### PR #886 — "Fix #879: Friendly error message for orphaned pages"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `e9c48d09b39ab775a8a606f5022ca91fffd4bad0`
- **v8.1.7 change:** In `PSSitePathItemService.findItem`, replaces `"This page should have been deleted when its site was deleted. Please contact Customer Success for assistance."` with `"The requested page is no longer available."`.
- **Evidence on development:** `git grep -n "requested page is no longer available" development -- projects/sitemanage/` returns no matches. `git show development:projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSSitePathItemService.java` line 122 still shows the old `should have been deleted when its site was deleted` wording. Last file change on `development`: `0218be645d` (Chore/commons lang3 migration #45).

#### PR #894 — "GH-891: Support leading Sites/ in Page by Path REST resource"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `0a4e8cd8a2b9133089530323966cca52eda4b940`
- **v8.1.7 change:** Adds a `normalizePath(String)` helper to `rest/src/main/java/com/percussion/rest/pages/PagesResource.java` that strips a leading `/` and `Sites/` prefix, then calls it from `getPage`, `updatePage`, `renamePage`, `deletePage`. Test cases in `PagesTest.java` (e.g. `testPageWithLeadingSitesPath`) and an `IllegalArgumentException` guard in `PageTestAdaptor.java`.
- **Evidence on development:** No `normalizePath` method in `git show development:rest/src/main/java/com/percussion/rest/pages/PagesResource.java`. All four `Matcher m = p.matcher(path)` calls (lines 112, 167, 241, 286) remain unwrapped. Test guard `siteName cannot be Sites` not present in `rest/src/test/java/com/percussion/rest/pages/PageTestAdaptor.java`.

#### PR #889 — "Fix Lucene ParseException for path-based search terms"

- **Verdict:** `superseded`
- **v8.1.7 merge commit:** `6fb03af8652878d8af2576eb092cf5d06dd6bd57`
- **v8.1.7 change:** In `projects/sitemanage/src/main/java/com/percussion/searchmanagement/service/impl/PSSearchRestService.java`, swaps `org.apache.lucene.queryparser.flexible.standard.QueryParserUtil.escape(q)` for `org.apache.lucene.queryparser.classic.QueryParser.escape(q)`, and adds a unit test `PSSearchRestServiceTest` that asserts `people\\/donna\\-williams` output.
- **Evidence on development:** Dev still uses `import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;` and `QueryParserUtil.escape(q)` (line 43 and line 75 respectively of `development:projects/sitemanage/src/main/java/com/percussion/searchmanagement/service/impl/PSSearchRestService.java`). However, `development:projects/sitemanage/src/main/java/com/percussion/searchmanagement/service/impl/PSSearchService.java` lines 382–392 contain a divergent functional equivalent: a private `escapeLuceneQuery(String)` method that handles a `luceneSpecialCharacters` list (lines 461–463) and is invoked from `excludeLocalWorkflow` (line 248). The dev has its own Lucene-8 escape logic (initial commit `de7a69f852`) that handles the same ParseException scenario through `PSSearchService` rather than `PSSearchRestService`. **Cherry-picking the v8.1.7 change would regress the dev design.** The unit test class `PSSearchRestServiceTest` is also absent on `development`.

### Publishing / Workflow (3)

#### PR #762 — "bugfix/749: Fix auto list widgets not working on published sites"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `19c844e3fe89eeac1f576193833c9a2ab1acb0bc`
- **v8.1.7 change:** In `system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm`, drops `($includeOnPublishedPage != "no")` guards around the `jquery-ui.js` script tag, the Mobile Preview script tag, the inline `getDeliveryServiceBase/getCMSVersion` script, and the conditional comment in the print_jqueryUI macro.
- **Evidence on development:** `git show development:system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm` retains the `($includeOnPublishedPage != "no")` guards in 5 distinct places (lines 675, 677, 686, 748, 764, 795, 1044). Last touch on `development`: `e288bedf74 #1258 - Rich text Widget problem post-upgrade`. The v8.1.7 simplification is **not** present.

#### PR #851 — "Fix NullPointerException during publish for newly created sites"

- **Verdict:** `superseded`
- **v8.1.7 merge commit:** `61db53810b53718e5c1b9b413e8745012a1f5459`
- **v8.1.7 change:** Replaces `pubServer.getProperty("publishServer").getValue()` with `pubServer.getPublishServer()` (a new direct getter); adds logging in the underlying `PSPubServer.getPublishServer()`.
- **Evidence on development:** The dev evolved the API to use `Optional`: `git show development:system/services/src/com/percussion/services/pubserver/data/PSPubServer.java` line 473 now declares `public java.util.Optional<String> getPublishServer()`. The callers in dev (`projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java` line 2873 and `system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java` lines 115–116) were updated to use `pubServer.getProperty("publishServer").map(PSPubServerProperty::getValue).orElse(...)`. The functional intent (NPE safety for missing publishServer) is preserved, but the dev API shape and caller patterns diverged; the v8.1.7 commit is not a clean cherry-pick target.

#### PR #853 — "bugfix/849: Handle concurrency exceptions gracefully during publish jobs"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `56ea8dc11e73994b987f5e60e52de1d309011fda`
- **v8.1.7 change:** Adds `isConcurrencyException(Throwable)` static helper to `PSAbstractWorkflowExtension`; wraps `handleError` and `PSWorkflowEditionTask.perform` to demote `LockAcquisition`/`OptimisticLock`/`PessimisticLock`/`ConcurrencyFailure` exceptions to `log.info` instead of `log.error`; demotes a noisy `ms_log.warn("Query problem: type not found…")` to `ms_log.debug` in `PSContentRepository.prepareQuery`.
- **Evidence on development:** None of `isConcurrencyException`, `LockAcquisitionException`, `OptimisticLock`, or `PessimisticLock` are present in `git grep` against `development` under `projects/sitemanage/` or `system/services/src/com/percussion/services/contentmgr/impl/legacy/`. `git show development:system/services/src/com/percussion/services/contentmgr/impl/legacy/PSContentRepository.java` line 2022 still emits `ms_log.warn` (not `ms_log.debug`). The v8.1.7 publishing-job hardening is **not** present on `development`.

### Security / Dependency (3)

#### PR #825 — "bugfix/818: Update CSP in restored perc-security.properties on DTS up…"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `cfd4858d184e406f1b3b50c0d001867a06d910a7`
- **v8.1.7 change:** Adds `data: blob:` (and `img-src 'self' data: blob:`) to CSP directives across 13 DTS files (`comments`, `feeds`, `forms`, `integrations`, `membership`, `metadata`, `polls`) including `delivery-tier-distribution/.../installDts.xml` and the `perc-security.properties` files of every service.
- **Evidence on development:** `git show development:deliverytiersuite/delivery-tier-suite/feeds/src/main/java/webapp/WEB-INF/perc-security.properties` retains `contentSecurityPolicy=default-src 'self' *;` (no `data: blob:`). `installDts.xml` retains `default=default-src * ; img-src * 'self' 'unsafe-inline' 'unsafe-eval'…` without `data: blob:`. No CSP update with `data: blob:` exists anywhere in `git grep` against `development` for the DTS module.

#### PR #848 — "bugfix/793: Add @Deprecated annotations to secure-membership classes…"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `7620825ea894e56ca3cc4d9ef7a285559be4d428`
- **v8.1.7 change:** Adds `/** @deprecated … */ @Deprecated` annotations to `CustomAuthenticationProvider` (x2), `PSMembershipConfiguration`, `AuthFormProcessingFilter`, `PSCacheControlFilter`, `PSLdapMembershipAuthProvider`, `PSLdapUserDetailsMapper`, `PSMembershipAuthProvider`, `PSMembershipAuthUtils`, `PSMembershipLoginHandler`, `PSMembershipLogoutHandler`. Also adds deprecation comments at the top of `security.xml` for 7 DTS services.
- **Evidence on development:** `git show development:deliverytiersuite/delivery-tier-suite/secure-membership/src/main/java/com/percussion/secure/data/PSMembershipConfiguration.java` has no `@Deprecated` annotation; same for all the other secure-membership classes and the `security.xml` headers. The v8.1.7 deprecation work is **not** present.

#### PR #915 — "Downgrade PDFBox to 2.0.31 to fix NoSuchMethodError in Tika"

- **Verdict:** `not-applicable`
- **v8.1.7 merge commit:** `d2f0d366622ce2a896cbaf879e0b85e751a1f404`
- **v8.1.7 change:** Downgrades `pdfbox.version` from 3.0.6 to 2.0.31 in `pom.xml`; removes `pdfbox-io` dependency from `pom.xml` and `system/pom.xml`; reverts `Loader.loadPDF(new RandomAccessReadBuffer(is))` to `PDDocument.load(is)` in `PSTextConverterPdf.java`.
- **Evidence on development:** `pom.xml` is on `<pdfbox.version>3.0.6</pdfbox.version>` (pom.xml line 177) and uses `<tika.version>3.2.3</tika.version>` (line 260-ish). `git show development:system/src/main/java/com/percussion/search/lucene/textconverter/PSTextConverterPdf.java` uses `org.apache.pdfbox.Loader.loadPDF(data)` (with a `byte[] data` pre-read). Last touch: `a16d21e972 Feature/jdk 21 stabilization (#605)`. The Tika 3.2.3 / PDFBox 3.0.6 pairing on `development` does not exhibit the `NoSuchMethodError` from `org.apache.pdfbox.io.RandomAccessReadBuffer` that v8.1.7 was downgrading to avoid; the v8.1.7 PR is **not applicable** to the `development` branch's PDFBox/Tika version pair.

### Deprecation / Refactor (3)

#### PR #794 — "Bugfix/793 redirect management to deprecated"

- **Verdict:** `conflicts-with-newer-design`
- **v8.1.7 merge commit:** `ceefe12b887b1be6dd055802e268faf182987926`
- **v8.1.7 change:** Adds `<gadget name="Redirect Management" baseuri="/cm/gadgets/repository/perc_website_config_gadget" file="perc_website_config_gadget.xml"/>` to the `Deprecated` group in `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml`; adds an assertion in `PSGadgetRepositoryListingTests`.
- **Evidence on development:** The file `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` **does not exist on `development`** (deleted in commit `a16d21e972` "Feature/jdk 21 stabilization (#605)"). `git grep -n "Redirect Management" development -- '*.xml'` finds no gadget registry reference; only `projects/sitemanage/src/main/java/com/percussion/redirect/service/IPSRedirectService.java:35` mentions "Redirect Management", and it's about a service check, not a gadget registry entry. The dev branch refactored / replaced the legacy gadget registry. The dev copy at `docker/dev-data/cms-dts/jetty/base/webapps/Rhythmyx/WEB-INF/classes/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` is a runtime artefact installed by the old webapp, not a source-controlled file. **Conflict — not applicable as-is**.

#### PR #885 — "Fix #882: Undeprecate Google Setup, Traffic, and What's Working gadgets"

- **Verdict:** `conflicts-with-newer-design`
- **v8.1.7 merge commit:** `d795261ff662f87d830a07c25cc96f506a84b6a5`
- **v8.1.7 change:** Moves Google Setup, Traffic, and What's Working entries from the `Deprecated` group back to the default group in the same `GadgetRegistry.xml`.
- **Evidence on development:** Same as PR #794 — the `GadgetRegistry.xml` is gone from the source tree on `development`. The follow-up `PSGadgetRepositoryListingTests` test is also absent (`git ls-tree -r --name-only development | grep PSGadgetRepositoryListingTests` returns no hits). **Conflict — not applicable as-is.** A maintainer must decide whether the dev branch's refactored gadget catalog ever re-classified these three gadgets and, if so, ensure the classification aligns with the v8.1.7 intent.

#### PR #850 — "Deprecate Registration widget"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `a0e3a6c333929fdacb68d93b6c6bcd2621c90b91`
- **v8.1.7 change:** Removes `<widget name="Registration"/>` from the `Percussion` group in `WidgetRegistry.xml`; adds `<widget name="Registration (Deprecated)"/>` to the `Deprecated` group; updates `ContentTypesResource.java` REST label from `"Registration Asset"` to `"Registration Asset (Deprecated)"`; updates `percRegistrationAsset.itemDef.contentType`, `percRegistrationAsset.nodeDef.contentType`, and `percRegistration.xml` to use the `(Deprecated)` suffix.
- **Evidence on development:** `git show development:projects/sitemanage/src/main/resources/com/percussion/pagemanagement/service/impl/WidgetRegistry.xml` line 36 still has `<widget name="Registration" />` in the `Percussion` group; the `Deprecated` group (line 56) only has `Share This` and `Calendar`. The legacy path `system/Packages/perc.widget.registration/` was migrated on `development` to `modules/perc-packages/src/main/resources/Packages/perc.widget.registration/`; at the new path, `percRegistrationAsset.itemDef.contentType` line 4 still has `<label>Registration Asset</label>` (no `(Deprecated)` suffix), and `percRegistration.xml` still has `title="Registration"` and `description="Widget to build and render a registration form"`. `ContentTypesResource.java` line 656 still has `"label": "Registration Asset",`. The v8.1.7 deprecation **is not** present on `development`.

### Other (CMS-Editor / Gadget widget fixes) (3)

#### PR #786 — "Fix View Metadata dialog rendering for Tags and Categories (#785)"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `b71ce10f0382208c4c05783a9f0a185c4f84df74`
- **v8.1.7 change:** Replaces broken `<input type="text">` per-tag output with plain comma-separated `Value` text in `percTagListControl.xsl`'s `isReadOnly='yes'` template; adds try/catch and `encodeURIComponent` to the `sys_CheckBoxTreeJS` script emission in `sys_Templates.xsl`.
- **Evidence on development:** The file was migrated from `system/Packages/perc.Baseline/...` to `modules/perc-packages/src/main/resources/Packages/perc.Baseline/SupportFile-rx_resources/stylesheets/controls/percTagListControl.xsl`. At the new path, the `isReadOnly='yes'` template (lines 105–148 in the dev worktree) still emits the broken `<input type="text" name="{concat(@paramName,'-display')}" id="{concat(@paramName,'-display')}" class="percTagListControl">` (line 110). The companion `sys_Templates.xsl` migration is **not** present on `development`. The v8.1.7 fix is **not** applied.

#### PR #814 — "bugfix/811: Prevent duplicate Facebook Open Graph tags from being ren…"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `f39a825b04230f5af58d07f4e4a40aaf75850770`
- **v8.1.7 change:** In `perc.openGraphWidget` widget Jexl, wraps every `setAdditionalHeadContent(...)` call with a guard `if (! $addlHead.contains("property=\"og:<key>\""))`. Touches 9 distinct `og:` tags (sitename, title, description, url, type, image, image:width, image:height, locale, fb_app_id).
- **Evidence on development:** The file was migrated to `modules/perc-packages/src/main/resources/Packages/perc.openGraphWidget/sys__UserDependency--rxconfig/Widgets/percOpenGraph.xml` on `development`. At the new path (lines 438–476), every `setAdditionalHeadContent` call is unconditional — no `contains` guards. The v8.1.7 de-duplication **is not** present.

#### PR #869 — "bugfix/866: Stop validator early on folder-not-found to prevent dupli…"

- **Verdict:** `needs-migration`
- **v8.1.7 merge commit:** `c55f17632aa65f9509e98fedcba06a706f10a7e8`
- **v8.1.7 change:** Adds two `return;` statements to `PSSiteSectionService.doValidation(PSCreateSectionFromFolderRequest, PSBeanValidationException)` — one after the `e.reject("…path does not correspond to a folder.")` and one after the `e.reject("…folder with that path cannot be found.")` catch — so that the validator stops and does not also run the parent-folder and landing-page checks.
- **Evidence on development:** `git show development:projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java` lines 2235–2245 still have the unconditional fall-through after the two rejections — no `return;` added. Last file touch on `development`: `30dba9c79a More javadoc cleanup`. The v8.1.7 fix is **not** present.

### Summary Table (Sample of 20 PRs)

|  PR  |  Category   |            Verdict            |                                         Evidence (commit on development / not-found)                                         |
|------|-------------|-------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| #763 | UI          | `needs-migration`             | `theme.css` still has `height: Xpx` for vspan_ on development                                                                |
| #767 | UI          | `needs-migration`             | `perc_decoration.css` vspan_ rules have no `!important` override                                                             |
| #827 | UI          | `already-present`             | `navLabel` `UserPref` + `aria-label="$!{navLabel}"` in `modules/perc-packages/.../percNavBar.xml` line 51/129                |
| #855 | UI          | `already-present`             | `$navLabel = $perc.widget.item.properties.get('navLabel');` binding present in both widgets                                  |
| #883 | UI          | `needs-migration`             | `$(document).tooltip({...})` still in `WebUI/src/main/webapp/cm/cui/widgets/contentList/contentList.ViewModel.js` (3 copies) |
| #886 | REST        | `needs-migration`             | old error message still in `PSSitePathItemService.java` line 122                                                             |
| #894 | REST        | `needs-migration`             | no `normalizePath` in `PagesResource.java`; all `p.matcher(path)` remain                                                     |
| #889 | REST        | `superseded`                  | dev has its own `escapeLuceneQuery` + `luceneSpecialCharacters` in `PSSearchService.java`                                    |
| #762 | Publishing  | `needs-migration`             | all 5 `($includeOnPublishedPage != "no")` guards retained in `sys_assembly.vm`                                               |
| #851 | Publishing  | `superseded`                  | dev migrated `getPublishServer()` to return `Optional`, callers use `.map(...).orElse(...)`                                  |
| #853 | Workflow    | `needs-migration`             | no `isConcurrencyException`, no graceful demotion of lock exceptions                                                         |
| #825 | Security    | `needs-migration`             | `data: blob:` CSP directive not in any DTS `perc-security.properties` on development                                         |
| #848 | Security    | `needs-migration`             | no `@Deprecated` annotations on secure-membership classes; no `security.xml` deprecation comments                            |
| #915 | Dependency  | `not-applicable`              | dev is on PDFBox 3.0.6 + Tika 3.2.3, no `NoSuchMethodError` trigger                                                          |
| #794 | Refactor    | `conflicts-with-newer-design` | `WebUI/.../GadgetRegistry.xml` deleted by `a16d21e972`                                                                       |
| #885 | Refactor    | `conflicts-with-newer-design` | same — `GadgetRegistry.xml` and `PSGadgetRepositoryListingTests` both absent                                                 |
| #850 | Deprecation | `needs-migration`             | `WidgetRegistry.xml` still has `Registration` in `Percussion` group; no `(Deprecated)` labels                                |
| #786 | Other       | `needs-migration`             | dev `percTagListControl.xsl` still emits broken `<input type="text">` for readOnly                                           |
| #814 | Other       | `needs-migration`             | dev `percOpenGraph.xml` has unconditional `setAdditionalHeadContent` calls                                                   |
| #869 | Other       | `needs-migration`             | dev `PSSiteSectionService.doValidation` still falls through after rejection                                                  |

Distribution of verdicts in this 20-PR sample: `needs-migration`=14, `already-present`=2, `superseded`=2, `not-applicable`=1, `conflicts-with-newer-design`=2. A single PR can carry sub-fixes; counts above enumerate distinct PRs. The 70% `needs-migration` rate suggests the bulk of the v8.1.7 porting work is real and not already covered on `development`.
