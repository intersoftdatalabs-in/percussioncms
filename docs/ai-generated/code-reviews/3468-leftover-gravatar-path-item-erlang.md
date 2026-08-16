# Erlang review — #3468 leftover gravatar named-GET 404 and empty path/item 400

**Scope:** `fix/issue-3468-leftover-gravatar-path-item` vs `origin/main` (`d0838a584f`, includes merged #3466). Cherry-pick of `f7ee305977c8` plus product-docs prefs-note tweak.

**Change class:** WebUI leftover callers after #3466 — Explorer still GETs named `perc_profile_gravatar_email` (404 on `/services` and `/Rhythmyx/services`) and `pathmanagement/path/item/` (400). Companions: Vitest on path/prefs/avatar, Playwright `explorer-console-clean.spec.js`, `product-docs/8.2/developer/rest.md`.

**Memory patterns hit:** change-class completeness (WebUI + Playwright + product-docs); behavioral tests for new skip/list-load logic; URL `/` is not OS file I/O.

## Summary

#3466 skipped UserMenu named-pref GET and `defaultResolveFolderId` for CMS root. Live H2 still hit leftover callers: `findItemByPath("/")` (encodes to `…/path/item/`) and Profile/chrome named GET for an unset Gravatar pref.

This slice:

1. Adds `isPathItemLookupPath` and returns an empty `PSPathItem` from `findItemByPath` when the encoded suffix is empty (every caller, not only `defaultResolveFolderId`).
2. Loads Gravatar override via `GET /preferences/` list; UserMenu does not probe prefs.
3. Treats PreferenceResource `200` + empty `value` as unset (`loadUserPreference` → `null`).
4. Does **not** change rest SNAPSHOT / PreferenceResource Java (already 200-empty on main).

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral unit tests: present (`pathApi.test.ts`, `preferencesApi.test.ts`, `avatarPrefs.test.ts`, `ContentExplorerShell.test.tsx`)
- Cross-platform path I/O: N/A (CMS URL `/` segments via `encodePath`; not filesystem)
- Change-class companions: Playwright spec + product-docs prefs note present
- May commit/push: **yes** (after standalone `mvnw clean install` on `WebUI` and `modules/perc-qa-automation`)

## Issues

None.

## Cross-platform path checklist

- [x] No new `".../" +` filesystem joins
- [x] CMS path encode uses `/` as repository URL segments (correct)
- [x] Tests assert URL shapes (`/path/item/Sites/Foo`), not OS `Path.toString()`
- [x] No Unix-only temp/absolute roots

## Tests / docs

- `findItemByPath("/")` and `""` do not call `fetch`
- `isPathItemLookupPath` false for null/blank/`/`/`///`
- `loadGravatarEmailOverride` uses `getAllUserPreferences`, never `loadUserPreference`
- `loadUserPreference` 200-empty → `null`
- Playwright: login → Explorer → sample site → Refresh, expect `hits []`
- `product-docs/8.2/developer/rest.md`: list-first Gravatar; named GET 200-empty = unset
