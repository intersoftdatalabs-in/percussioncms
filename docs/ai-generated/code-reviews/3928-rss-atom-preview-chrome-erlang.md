# Erlang review — #3928 Developer Sites rss-atom Preview chrome

**Branch:** `feat/issue-3928-rss-atom-preview-chrome`  
**Base:** `feat/issue-3927-rss-atom-build-chrome` (Build chrome not yet on `main`)  
**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-08-27

## Summary

Parent #2678 slice 2/3. Developer Sites **Preview assembled site** is shown for
`sourceKind=rss-atom` after save. Operators save a local RSS/Atom fixture root,
**Build Virtual Site**, then Preview last-build home HTML (REST `GET …/virtual/preview`
from cluster #3923 / #3916). **Build** chrome stays visible (stacked #3927 / PR #3933).
**Publish** chrome stays hidden (#3917 REST; UI later). Repository / unknown kinds still
hide Virtual chrome. git/csv/sql/http-json/object-storage Preview unchanged. Local QA
fixture only (`feed.xml` guid `index` so last-build home is `{version}/index.html`; no
live feeds or credentials).

## Recommendation

approve

## Gate

May commit/push: yes

## Change class

WebUI product screen chrome (Developer Sites Virtual Site source panel) + Playwright
companion + product-docs 8.2 admin/developer/reference Sites.

## Companions (closure)

| Artifact | Present |
|----------|---------|
| `shouldShowVirtualPreviewChrome` includes `rss-atom` | yes |
| Build helper unchanged (rss-atom already true from #3927) | yes |
| Publish helper unchanged (rss-atom false) | yes |
| i18n `SITE_VIRT_RSS_ATOM_HINT` + `SITE_VIRT_PREVIEW_HINT` | yes |
| Vitest `virtualSiteBuild` + `VirtualSiteSourcePanel` | yes |
| Playwright `developer-site-virtual-source.spec.js` + live Preview | yes |
| Local RSS fixture (`guid>index` / `_config.yaml` `rss.file`) | yes |
| product-docs 8.2 admin Sites (plus developer/reference lockstep) | yes |

## Issues

None that block.

## Cross-platform path checklist

- Host fixture paths use `path.join` (Node).
- In-container root is a POSIX literal (`/opt/Percussion/tmp/rss-atom-virtual-qa`) — URL/container
  path, not OS join. Unit test asserts no backslash.
- Operator-style examples (`C:/rss-atom-docs`) remain field values, not filesystem joins.
- Preview home path still sanitized via existing `sanitizeVirtualPreviewHomePath` (rejects
  `..`, drive letters, URLs).
- Playwright preview URL segments encode path parts; filters `.` / `..`.
- Line-ending assertions not added.

## Memory patterns hit

- Missing behavioral tests for changed chrome helpers — covered.
- Playwright required for WebUI screen change — spec extended, fail-closed live Preview.
- Non-portable path joins — not introduced.
- Secrets in fixtures — local RSS only; unit test rejects credentials / live URLs.
- Consume REST preview sibling without re-implementing it.

## Tests

- `virtualSiteBuild` — rss-atom shows Build + Preview; Publish still false; repository /
  `sql-api` stay hidden; git/csv/sql/http-json/object-storage unchanged
- `VirtualSiteSourcePanel` — load/save rss-atom shows Build + Preview, hides Publish;
  Preview click opens last-build home; repository switch hides chrome
- Playwright — rss-atom option; Build + Preview visible; Publish hidden; live Preview
  after Build on H2 QA fixture; restore repository hides Preview
- REST internals — N/A (consume cluster #3923 / #3916)
