# Erlang review — #3366 Developer Sites Publish Virtual Site chrome

**Branch:** `feat/issue-3366-virtual-site-publish-chrome`  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-14  
**Recommendation:** approve

## Change class

WebUI product chrome on Developer → Sites Virtual panel: Publish control calling existing `POST /services/sites/{nameOrId}/virtual/publish`. Companions: `sitesApi` helper + DTO parse, Vitest (API + helpers + panel), H2 Playwright surface spec, `product-docs/8.2` operator steps.

## Gate checklist

| Gate | Result |
|------|--------|
| Bugs | None found |
| Behavioral tests | Vitest covers hide-for-repository, success dest/files, API error, client validation; Playwright live + mocked success |
| Change-class closure | WebUI + perc-qa-automation spec + product-docs 8.2 |
| Portable paths | Display-only dest strings from REST; no host filesystem join |
| Java API shape / `final` | N/A — TypeScript client only; does not reimplement `SitesAdaptor.publishVirtualSite` |
| Agent rule files | None |

## Notes (non-blocking)

- Publish reuses the Build chrome visibility predicate (`shouldShowVirtualPublishChrome` → `shouldShowVirtualBuildChrome`).
- Live H2 publish success is not required; panel posts to REST and surfaces 4xx when source is unsaved / Site root missing (sibling #3365).
- Pre-existing QA install `PSX_OBJECTACL` PK ERROR during `qa-up` is unrelated to this surface (install-time folder ACL).
