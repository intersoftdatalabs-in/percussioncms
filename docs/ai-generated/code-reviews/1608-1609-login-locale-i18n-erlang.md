# Erlang Code Review — GH-1608 / GH-1609 login locale i18n

## Summary

Fixes two related login-page locale bugs on the React front door:

1. **GH-1609** — Login chrome stayed English after locale change because
   `LOGIN_KEYS` (`perc.ui.login.modern@*`) had no TMX entries. Added six
   translation units to `CmsUi.tmx` (en-us / es / hi) and a behavioral test
   that re-reads `window.I18N` after the TMX script load resolves.
2. **GH-1608** — Locale dropdown re-labeled every option in the selected UI
   language via `Intl.DisplayNames(viewer)`. Switched to **endonyms** (each
   option named in its own language) so the list stays stable when the
   selected locale changes.

Scope is five files: `localeLabels.ts`, `LoginPage.tsx`, two test files, and
`CmsUi.tmx`.

## Scope

- Base: `origin/development`
- Branch: `fix/1608-1609-login-locale-i18n`
- Files: 5 changed
- Issues: #1608, #1609
- Prior related work: `native-locale-dropdown-login-erlang.md` (#1551)

## Recommendation

**approve**

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Findings to address before merge: 0
- Follow-up (non-blocking): locales other than es/hi still fall back to
  English chrome until TMX gains those language variants (existing product
  catalog limitation; not introduced here).

## Cross-platform path / file I/O checklist

- No filesystem I/O introduced. `tmxLoader` / script `src` URLs continue to
  use forward slashes (correct for URL paths).
- Tests do not assert OS path shapes.
- **Outcome**: clean.

## Issues

None blocking.

### Note 1 — Severity: suggestion (unused `_viewer` parameter)

- File: `WebUI/src/main/ts/login/localeLabels.ts`
- Description: Second argument retained for API compatibility but ignored.
  Acceptable for a minimal fix; a future cleanup can drop the parameter and
  update call sites in one go.
- Status: accepted as-is

### Note 2 — Severity: suggestion (product brand untranslated)

- Issue #1609 lists "Percussion CMS" among untranslated strings. That string
  is `theme.brand.productName`, intentionally brand identity, not TMX chrome.
  Title segment "Sign in" is now localized; brand remains English.
- Status: intentional

## Behavioral tests

|                    Behaviour                    |                   Coverage                   |
|-------------------------------------------------|----------------------------------------------|
| Endonym labels (français, español, Deutsch)     | `localeLabels.test.ts`, `LoginPage.test.tsx` |
| Labels stable across UI locale change (GH-1608) | both test files                              |
| Fallback when `Intl.DisplayNames` absent        | both test files                              |
| Chrome re-reads I18N after TMX load (GH-1609)   | `LoginPage.test.tsx` new async test          |
| Document title tracks locale                    | same GH-1609 test                            |
| Form/CSRF preserved                             | existing test unchanged                      |

## Pre-commit evidence

```bash
cd WebUI && npm test -- --run src/test/ts/login
# → 5 files, 41 tests passed

cd modules/perc-i18n && ../../mvnw clean install
# → BUILD SUCCESS; Tests run: 6, Failures: 0

cd WebUI && ../mvnw clean install
# → BUILD SUCCESS; surefire Tests run: 12, Failures: 0
```

## Memory patterns hit

- None new; aligns with prior login i18n work (dynamic TMX re-apply, endonym
  vs viewer localization distinction).

