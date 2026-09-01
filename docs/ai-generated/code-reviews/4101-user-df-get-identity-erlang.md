# Erlang review — #4101 user display format GET identity after REST create

**Verdict:** pass (bugs and missing behavioral tests gated)

## Change class

User display format persist + GET identity after REST create (system save path + sitemanage adaptor identity; Playwright user-format columns).

## Findings

None remaining as hard gates.

- JDBC persist of `PSX_DISPLAYFORMATS` / columns is used because locator `saveComponents` posts `updateDisplayFormats` with no XML document (`Xml Document Expected`).
- GET-by-guid load that would replay By_Author is reconciled from JDBC.
- `sys_title` is not deleted on column replace.
- Empty lock-id lists are not passed to `findLocksByObjectIds`.
- Paths: SQL only; no filesystem path construction.
- Tests: `PSUiDesignWsDisplayFormatPersistTest`, `DisplayFormatAdaptorWriteTest` identity cases, H2 Playwright `developer-display-format-columns.spec.js` (2 passed).
