# Erlang self-review — #2242 runtime inline link title resolve

**Date**: 2026-08-07  
**Branch**: `fix/issue-2242-runtime-inline-link-title-field`  
**Verdict**: **approve** (pre-commit)

## Change class

Insert-time title resolve for inline links/images: pure resolver helper + wire into `PSRenderLinkService` preview paths + optional `titleField` query/body + client pass-through from TinyMCE control option.

## Companions checked

| Companion | Status |
|-----------|--------|
| Pure resolver with documented fallback | `PSInlineLinkTitleResolver` |
| Behavioral unit tests for resolve + fallback | `PSInlineLinkTitleResolverTest` (11 cases) |
| Asset path uses resolver | `renderPreviewResourceLink` |
| Page path uses resolver + type default BC | `renderPreviewPageLink(..., titleField)` |
| API accepts per-control field | `@QueryParam("titleField")` + `PSInlineLinkRequest.titleField` |
| Client pass-through | `PercPathService.getInlineRenderLink` (3 trees) + percadvlink/percadvimage |
| Playwright residual | deferred #2243 (out of slice) |
| Image runtime assembly (R1/R3) | deferred — insert-time is P0; runtime image refresh remains displaytitle (inventory P1) |

## Fallback chain (documented)

1. Configured field (non-blank value on target)  
2. `displaytitle` (if custom missing/empty and not already displaytitle)  
3. Type default: page `resource_link_title` / `page.getLinkTitle()`; asset `displaytitle`  
4. Empty string  

Unset/blank config → type default only (pre-feature BC).

## Bug / path / test gates

- No portable path I/O introduced.  
- No NPE on null field maps.  
- JAX-RS method names distinct from interface overloads (signature clash avoided).  
- `sitemanage` `mvnw clean install -DskipITs` BUILD SUCCESS.  
- `perc-tinymce` `mvnw clean install` BUILD SUCCESS.  
- Focused tests: 11/11 green.

## Residual

- Vitest/Playwright E2E matrix → #2243  
- Assembly-time image title refresh (optional P1) — not this PR  

> Co-Authored by Grok Build using grok-4.5 with agent main.
