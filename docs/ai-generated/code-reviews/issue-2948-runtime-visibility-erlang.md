# Erlang review — #2948 Runtime visibility not persisted

**Branch:** `fix/issue-2948-runtime-visibility-persist`  
**Scope:** uncommitted changes for issue #2948  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**  
**Memory patterns hit:** preference/DTO convert must copy all wire fields; load vs save converter drift

## Summary

Developer Preferences Default ACL Template Runtime visibility (`RUNTIME_VISIBLE`) was lost after reload because `ApiUtils.convertUserProperty` (used by `PreferencesAdaptor.loadPreference` for `GET /preferences/{name}`) never copied `PROPERTYVALUE` onto the REST `UserPreference`. Save path used `convertPSPersistentProperty` (which does set value), so PUT appeared to succeed and local UI state looked correct until remount/reload fell back to system default without Visible on Default.

## Findings

| Severity | Issue | Status |
|----------|--------|--------|
| (none blocking) | — | — |

### Notes (non-blocking)

- Fix delegates `convertUserProperty` → `convertPSPersistentProperty` to prevent future field drift.
- Client serialize/parse already preserved `RUNTIME_VISIBLE`; extra Vitest round-trip tests lock that contract.
- No public method signature change; no path/file I/O in the diff.
- Product-docs N/A (bugfix restoring documented intent; no operator procedure change).
- Playwright not required (no UI surface change; server conversion bug).

## Cross-platform path checklist

N/A — no filesystem path construction or assertions in this change.

## Tests

- `ApiUtilsUserPreferenceConvertTest` — value preserved; parity with save converter; empty value → `""`
- `defaultAclTemplate.test.ts` — serialize/parse keeps `RUNTIME_VISIBLE` on Default
- `DeveloperPreferencesPanel.test.tsx` — save→remount load shows Visible checked

## Build evidence (pre-PR)

- `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS (incl. new UnitTest)
- `cd WebUI && ../mvnw clean install` → BUILD SUCCESS
- Focused Vitest: 18 tests passed (defaultAclTemplate + DeveloperPreferencesPanel)
