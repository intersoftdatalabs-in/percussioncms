# Erlang review — #3457 sample site Pages/Files listing

- Branch: `fix/issue-3457-sample-site-pages-listing`
- Date: 2026-08-15
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: empty REST list treated as success; sitename vs FOLDER_ROOT mismatch (#3326 / #3410)

## Summary

FastForward sample sites have no `Pages` folder. `GET …/paginatedFolder/Sites/Corporate_Investments/Pages` mapped to `//Sites/CorporateInvestments/Pages`, `folderHelper.findItems` returned empty (pathToNothing), and recover only ran on throw / site-only paths. Explorer showed “No items in this folder”.

Fix recovers nested `/Pages` and `/Files` (including empty physical stub folders), lists site-root / About-section page children, and injects virtual **Pages** chrome on the site node.

## Issues

None blocking.

## Cross-platform path checklist

N/A for OS file I/O. All new joins are CMS `/` repository paths (`//Sites/…`), not `File.separator`. Tests do not assert Windows/Unix filesystem shapes.

## Tests

- `PSSitePathItemServiceVirtualPagesTest` — Pages/Files listing, empty stub Pages fallback, section-folder flatten, site chrome inject
- WebUI `DetailList.test.tsx` — non-empty `childrenInPage` on `/Sites/Corporate_Investments/Pages` renders rows
- perc-qa-automation helper + Playwright spec (demo-sites skip/enforce)
