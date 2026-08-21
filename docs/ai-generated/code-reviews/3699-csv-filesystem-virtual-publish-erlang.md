# Erlang review — #3699 REST/UI csv-filesystem virtual/publish

**Branch:** `feat/issue-3699-csv-filesystem-virtual-publish`  
**Stacked on:** cluster #3705 (`cluster/night-issue-20260821-csv-virtual-site`)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** approve  
**Memory patterns hit:** change-class closure (adaptor tests + Playwright + product-docs); portable Path/Files; fail-closed unsafe roots; WebUI Playwright HARD GATE

## Summary

Parent #2678 slice. `POST /sites/{nameOrId}/virtual/publish` already built-then-copied for any Virtual Site; this slice proves **csv-filesystem**, shows Developer Sites **Publish** chrome (hidden for repository), and documents it.

Relative `IPSSite.root` values (H2 demo `../CI_Home`) are resolved against the CMS install directory with NIO `Path` before the remaining-`..` check — peer of `PSRxPublisherService` archive location resolution. Fail-closed when relative and no install root.

## Change class

REST/UI Virtual Site publish for csv-filesystem (peer git-filesystem #3301 / UI #3366).

## Companions

| Kind | Present |
|------|---------|
| Adaptor tests (CSV fixture + injected `buildRunner`) | yes — `SitesAdaptorTest` |
| REST OpenAPI notes | yes — `SitesResource` publish description |
| WebUI chrome + Vitest | yes — `shouldShowVirtualPublishChrome` |
| Playwright surface spec | yes — intercept + live H2 |
| product-docs 8.2 | yes — admin Sites/Publishing, developer REST/virtual-sites, reference site-config |
| Cross-platform Path I/O | yes — `Path.of` / `resolve` / `normalize`; no hardcoded separators |

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] Relative Site root resolved with `Path.resolve` against install dir
- [x] Tests do not assert Unix-only absolute path strings
- [x] CSV QA fixture still uses POSIX in-container path (URL/container path, not OS join)

## Issues

None.

## Tests / evidence

- `system` clean install — BUILD SUCCESS; `PSVirtualSiteFilesystemPublisherTest` Tests run: 13, Failures: 0
- `rest` clean install — BUILD SUCCESS; Tests run: 544, Failures: 0
- `projects/sitemanage` clean install — BUILD SUCCESS; `SitesAdaptorTest` Tests run: 51, Failures: 0
- `WebUI` clean install — BUILD SUCCESS; Vitest Tests 2997 passed
- Playwright `developer-site-virtual-source.spec.js` 10/10 on H2 QA (`TEST_CMS_URL=http://127.0.0.1:9993`)
- console-clean=yes; server.log-clean=yes (no virtual/publish ERROR/FATAL)

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
