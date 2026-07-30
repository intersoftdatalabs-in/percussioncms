# Erlang review — GH#706 Remove EMS Event List + DTS perc-integrations

**Branch:** `chore/706-remove-ems-event-list`  
**Scope:** Uncommitted / branch vs `origin/development`  
**Date:** 2026-07-24  
**Reviewer persona:** Erlang (pre-commit gate)

## Summary

Full retirement of the EMS Event List community widget and the EMS-only DTS
`integrations` / `perc-integrations` service:

|       Area       |                                                                           Change                                                                           |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Package          | Deleted `modules/perc-packages/.../perc.widget.emseventlist`                                                                                               |
| Install lists    | Removed from all 5 InstallPackages (`system` + `perc-distribution-tree`)                                                                                   |
| Widget tray      | Removed from `WidgetRegistry.xml`; empty Community group removed                                                                                           |
| CMS proxy        | Deleted `projects/sitemanage/.../integrations/ems/**` (+ error-exposure tests for dead code)                                                               |
| DTS              | Deleted entire `deliverytiersuite/.../integrations` module (already commented out of reactor); cleaned distribution POM, log4j2, `server.xml` serviceNames |
| Delivery config  | Dropped `perc-integrations` from `delivery-servers.xml*` and test fixtures                                                                                 |
| Constants / CSRF | Removed `SERVICE_INTEGRATIONS`; removed unused `CSRF_INTEGRATION_PATH`                                                                                     |
| REST example     | Dropped `percEmsEventList` from `ContentTypesResource` OpenAPI sample                                                                                      |
| Tests            | `EmsEventListRemovalTest` (package/proxy/module + InstallPackages + delivery-servers); updated `PSWidgetServiceValidationTest`                             |

## Recommendation

**approve**

## Gate

|                        Gate item                        |                                                                              Result                                                                              |
|---------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Bugs                                                    | None found                                                                                                                                                       |
| Behavioral unit tests for new/changed non-trivial logic | Present — package absence + registry + installer list + delivery-servers assertions                                                                              |
| Cross-platform path / file I/O                          | Clean — tests use `java.nio.file.Path` / `Files` and monorepo-root walk (same pattern as other package tests); no hardcoded `/` filesystem joins for local paths |
| May commit/push                                         | **yes**                                                                                                                                                          |

## Issues

None (blocking).

### Notes (non-blocking)

1. **Existing widget instances:** Customers with EMS widgets already on pages will keep orphan content-type / asset rows until manually cleaned; package uninstall is not automated (same approach as Evergage GH#709). Acceptable for 8.2 retirement.
2. **Share This** remains in Deprecated on this base (`development`); GH#690 is a separate PR and was intentionally not rebased into this work.
3. **System module** change is a single constant removal (`SERVICE_INTEGRATIONS`); no remaining call sites after EMS proxy deletion.
4. **Checked-in WebUI slim bundles** had the dead CSRF constant stripped for consistency; next common-ui-bundle rebuild will keep them aligned.

## Cross-platform path checklist

- [x] No new `".../" +` / `"...\\" +` filesystem path construction in production code
- [x] New test path logic uses `Path` / `Files`
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Monorepo root resolution walks parents (works from module basedir or repo root)

## Memory patterns hit

- Widget / package retirement inventory (Evergage #709, Share This #690): registry + InstallPackages + package dir + tests
- Prefer deleting dead integration surfaces over leaving commented reactor modules

## Build evidence (pre-PR)

|              Module              |                   Command                   |                                                     Result                                                      |
|----------------------------------|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `modules/perc-packages`          | `../../mvnw clean install`                  | BUILD SUCCESS                                                                                                   |
| `projects/sitemanage`            | `../../mvnw clean install`                  | BUILD SUCCESS — Tests run: 552, Failures: 0; `EmsEventListRemovalTest` 3/0; `PSWidgetServiceValidationTest` 1/0 |
| `rest`                           | `../mvnw clean install`                     | BUILD SUCCESS                                                                                                   |
| `deployer`                       | `../mvnw clean install`                     | BUILD SUCCESS                                                                                                   |
| `modules/perc-common-ui-bundle`  | `../../mvnw clean install`                  | BUILD SUCCESS                                                                                                   |
| `delivery-tier-distribution`     | `../../../mvnw clean install`               | BUILD SUCCESS                                                                                                   |
| `modules/perc-distribution-tree` | `../../mvnw clean install`                  | BUILD SUCCESS                                                                                                   |
| `system`                         | `../mvnw clean install -Djavadoc.skip=true` | BUILD SUCCESS                                                                                                   |

