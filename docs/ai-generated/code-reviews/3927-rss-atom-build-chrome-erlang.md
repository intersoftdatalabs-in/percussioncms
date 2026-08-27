# Erlang review — #3927 Developer Sites rss-atom Build chrome

**Branch:** `feat/issue-3927-rss-atom-build-chrome`  
**Base:** `origin/main`  
**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-08-27

## Summary

Parent #2678 slice 1/3. Developer Sites **Build Virtual Site** chrome is enabled for
`sourceKind=rss-atom` after save (peer of object-storage #3869). Preview and Publish
chrome stay hidden (slices #3928 / #3917). i18n hint updated; repository / unknown
kinds still hide Build.

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
| `shouldShowVirtualBuildChrome` includes `rss-atom` only | yes |
| Preview/Publish helpers unchanged (rss-atom false) | yes |
| i18n `SITE_VIRT_RSS_ATOM_HINT` (no hard-coded English chrome) | yes |
| Vitest `virtualSiteBuild` + `VirtualSiteSourcePanel` | yes |
| Playwright `developer-site-virtual-source.spec.js` + live Build | yes |
| Local RSS fixture (`feed.xml` / `_config.yaml` `rss.file`) | yes |
| product-docs 8.2 admin Sites (plus developer/reference lockstep) | yes |

## Issues

None that block.

## Cross-platform path checklist

- Host fixture paths use `path.join` (Node).
- In-container root is a POSIX literal (`/opt/Percussion/tmp/rss-atom-virtual-qa`) — URL/container
  path, not OS join. Unit test asserts no backslash.
- Operator-style examples (`C:/rss-atom-docs`) remain field values, not filesystem joins.
- Docker `mkdir` / `cp` destinations use `/` (Linux QA cell).

## Memory patterns hit

- Missing behavioral tests for changed chrome helpers — covered.
- Playwright required for WebUI screen change — spec extended, fail-closed live Build.
- Non-portable path joins — not introduced.
- Secrets in fixtures — local RSS only; unit test rejects credentials / live URLs.

## Build evidence (pre-PR)

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Vitest Test Files 396 passed;
  Tests 3203 passed; Surefire Tests run: 63, Failures: 0.
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS.
- `npm run test:unit` (frontend) — 473 passed, including `rss-atom-virtual-qa-fixture`.
