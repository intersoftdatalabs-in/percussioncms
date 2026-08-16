# Erlang review — #3468 leftover gravatar 404 + empty path/item 400

**Branch:** `fix/issue-3468-explorer-leftover-callers`  
**Base:** `origin/main` (`d0838a584f`, includes merged #3466)  
**Commit:** `f7ee305977` (cherry-pick of `6a477d7061` from #3481)  
**Date:** 2026-08-16  
**Reviewer:** Erlang (Grok Build 1.0.4 / grok-4.6 / night-issue-prs)

## Summary

#3466 stopped UserMenu named-pref GET and `defaultResolveFolderId` for CMS root, but live Explorer still issued:

- `GET /services/preferences/perc_profile_gravatar_email` (404)
- `GET /Rhythmyx/services/preferences/perc_profile_gravatar_email` (404)
- `GET /Rhythmyx/services/pathmanagement/path/item/` (400)

This slice closes leftover callers: `findItemByPath` skips empty suffix, `loadGravatarEmailOverride` uses the preference list, `loadUserPreference` treats 200-empty as unset, and Explorer skips `resolveFolderId` at root.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Cross-platform path checklist

N/A for filesystem I/O. URL/REST suffixes correctly use `/`. Tests do not assert OS path separators.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Notes

- Behavioral tests cover list-vs-named GET (`avatarPrefs.test.ts`), 200-empty (`preferencesApi.test.ts`), `isPathItemLookupPath` + no-fetch root (`pathApi.test.ts`), and shell skip of `resolveFolderId` (`ContentExplorerShell.test.tsx`).
- Playwright `explorer-console-clean.spec.js` asserts hits `[]` and no unexpected `pageerror`.
- `PreferenceResource` 200-empty is already on `main` (#3466); this PR does not change `rest`.
- Product docs: `product-docs/8.2/developer/rest.md` User preferences section.

Memory patterns hit: leftover caller after resource-contract fix; do not treat SNAPSHOT jar copy as UI proof.

> Co-Authored by Grok Build 1.0.4 using grok-4.6 with agent night-issue-prs.
