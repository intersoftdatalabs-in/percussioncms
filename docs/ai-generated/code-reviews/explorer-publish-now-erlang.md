# Erlang review: Explorer Publish Now (feat/explorer-publish-now)

**Branch:** `feat/explorer-publish-now`  
**Base:** `origin/main`  
**Date:** 2026-08-15  
**Persona:** Erlang (independent of implementer)

## Summary

Leftover P0 **Publish Now** is dispatched as REST: confirm, then existing sitemanage `GET /services/sitemanage/publish/page|{resource}/{id}`. Catalog `demandpublishing` URL is not navigated. Active Assembly remains unavailable; `specs/996-react-active-assembly/spec.md` records the host decision.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover classify-by-name, confirm/cancel, page vs asset GET paths, and no `demandpublishing` fetch. Playwright extends the existing dispatch blocklist.

## Change-class closure

Change class: **WebUI Explorer dispatcher + existing sitemanage publish GET + product-docs + Playwright**. No new rest adaptor.

| Companion | Status |
|-----------|--------|
| Dispatcher + `publishSelectedItem` | present |
| Vitest (dispatch + itemPublish) | present |
| Playwright Data Flow/servlet blocklist | present (`demandpublishing`) |
| product-docs admin + rest | present |
| 996 AA stub | present |
| Seed URL rewrite | **intentionally not done** — name-first dispatch; avoids `PublishNowActionSeedUrlTest` / distribution-tree build |

## Cross-platform path checklist

CMS/URL paths only (`/`). **Outcome: clean.**

## Issues

None blocking. Suggestion: later clear ACTION 217 URL and retarget `PublishNowActionSeedUrlTest` so the catalog does not advertise the servlet.

## Handoff

Approve leftover P0 Publish Now. P2 AA still blocked on host decision in 996.

## Re-review (2026-08-15, PR #3444 peer thread)

**Scope:** `publishSelectedItem` now runs `mapPublishResponse` on HTTP 200 bodies. Preflight statuses (`FORBIDDEN`, `BADCONFIG`, …) throw instead of returning `true`. Vitest covers unwrapped `FORBIDDEN` and wrapped `SitePublishResponse`/`BADCONFIG`. Playwright route-mock case added. Product-docs updated.

**Recommendation:** approve  
**May commit/push:** yes  
**Memory patterns hit:** HTTP 200 application-level publish failures (`#936` / `mapPublishResponse`).

**Cross-platform path checklist:** URL/CMS paths only. **Outcome: clean.**

**Issues:** none blocking. Playwright case soft-skips when Publish Now is not in the toolbar for the current selection; Vitest is the behavioral contract.
