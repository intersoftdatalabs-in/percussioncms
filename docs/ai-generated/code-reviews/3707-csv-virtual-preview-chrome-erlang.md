# Erlang review: #3707 Developer Sites CSV virtual preview chrome

**Branch:** `feat/issue-3707-csv-virtual-preview-chrome`  
**Parent:** #2678 (slice 2 of 3; consumes REST #3709 / #3711)  
**Change class:** WebUI product screen (Developer Sites Preview chrome for `csv-filesystem`) + Playwright live H2 QA + product-docs admin Sites

## Scope

- `shouldShowVirtualPreviewChrome` matches Build allow-list (`git-filesystem` | `csv-filesystem`); repository / unknown hide Preview.
- CSV hint and Preview hint are i18n keys (not bare English).
- Playwright live path: save CSV source, Build, GET `/virtual/preview` available, home HTML contains fixture title/body.
- Consumes last-output Preview REST (no second assembler; no publish/rebuild).

## Bugs

None found. Preview URL uses `sanitizeVirtualPreviewHomePath` then encoded path segments. Playwright fallback `page.request.get` uses the same encoded relative home path (no `..`). CSV QA fixture is copied with `docker cp` + POSIX in-container root (`/opt/Percussion/tmp/csv-virtual-qa-3697`).

## Tests

- Vitest: `shouldShowVirtualPreviewChrome`; panel opens CSV last-build home; repository still hides Preview.
- Playwright: repository hides Preview; CSV hint names Preview; live Build then Preview home HTML.

## Cross-platform

- No new filesystem `"/"` concatenations. Fixture helper already uses `path.join` on the host and POSIX strings only inside the Linux QA cell.
- Preview REST paths are URL/relative (`8.2/index.html`), not OS file joins.

## Product-docs

`product-docs/8.2/admin/sites.md` Preview section: CSV after Build; repository hides Preview/Build.

## Hard gates

- Behavioral tests present for new helper + panel + live Playwright.
- Portable paths: pass.
- Companions: Vitest + Playwright + product-docs (change-class closure).
