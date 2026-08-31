# Erlang review — issue #4039 CD-18 auto-translation PUT lock NPE

**Date:** 2026-08-31  
**Branch:** `fix/issue-4039-auto-translation-put-lock-npe-2`  
**Scope:** uncommitted PUT persist fix (system lock/content + rest path + sitemanage adaptor + Playwright + product-docs)  
**Memory patterns hit:** change-class closure (adaptor tests + resource path + Playwright); lock 409 vs 500; CXF jaxrs:server refs vs `{idOrLang}` steal; hot-deploy exploded `sitemanage-beans.xml`; dummy AUTO_TRANSLATIONS GUID load-all; UUID vs typed long PK

## Summary

Admin PUT `/services/locales/auto-translations` no longer 500s from `List.copyOf` NPE on bulk lock results with null slots. Dummy-GUID load returns all `PSX_AUTOTRANSLATION` rows; composite-key delete + UUID-normalized keys avoid unique-PK insert of locale `ar` / type 1033. Same-user leftover locks steal (`overrideLock=true`); other users 409. CXF: `LocalesResource` regex + explicit GET/PUT `/auto-translations` so the path is not `{idOrLang}`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No remaining hard-gate bugs. Behavioral tests: `PSLockExceptionBulkCtorTest` (null slots), `PSAutoTranslationSetGuidTest`, adaptor copyVersions UUID/typed long, `LocalesResourceTest` path/delegate, Playwright live persist (GET round-trip, PUT persist, empty PUT clear) on H2 QA. Standalone `mvnw clean install`: system Tests run: 2623 Failures: 0; rest 903/0; sitemanage 1914/0; perc-qa-automation BUILD SUCCESS. C5: qa-up TEST_CMS_URL=http://127.0.0.1:9993; hot-deploy perc-system + sitemanage + rest; qa-health RESULT:OK; `npm run test:surface -- --path tests/developer-auto-translations-persist.spec.js` 1 passed; console-clean=yes; server.log ERROR count 0 for feature.

## Issues

None (hard-gate).

### Notes (non-blocking)

- Exploded QA `sitemanage-beans.xml` may omit `restAutoTranslationsResource` until copied; LocalesResource GET/PUT delegates cover that.
- SPA table chrome remains out of scope (#4028 / #4044).
- Cross-platform path review: Playwright unit test uses `path.join`; no new filesystem joins in Java.

## Cross-platform path review

Clean for this slice (no production path I/O).
