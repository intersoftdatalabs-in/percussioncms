# Erlang review — #3522 SiteCreateWizard type picker + Traditional path

**Branch:** `feat/issue-3522-site-type-picker`  
**Scope:** uncommitted vs `origin/main`  
**Date:** 2026-08-17  
**Reviewer persona:** Erlang (independent of implementer)

## Summary

Slice 1 of parent #3512 adds a first **Site type** step (Traditional / Page / Virtual) on the shared `SiteCreateWizard` used by Explorer **Content → Create Site** and Navigation **New Site**. Traditional create skips the page/base template prompt; Page and Virtual stay on the type step with a blocking message. Traditional still `POST`s `/services/sitemanage/site/` via `createTraditionalSite` with generated template name + `perc.base.plain`.

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (Vitest wizard + validation; Playwright surface + ArchitectureShell)
- Cross-platform path I/O: N/A (no filesystem path construction)
- **May commit/push:** yes

## Issues

None (gate).

### Notes (non-blocking)

- `loadBaseTemplates` was dropped from wizard props because Traditional no longer catalogs templates. Callers did not pass it in production; ArchitectureShell tests still spy `listBaseTemplates` harmlessly.
- Page/Virtual radios are selectable (not `disabled`) so operators see the kinds and the unavailable message; Next is disabled so those kinds cannot silently create Traditional sites.
- Confirm no longer lists template/base for Traditional. API body still includes generated values so the existing Site contract stays valid.

## Change-class companions

| Kind | Status |
|------|--------|
| WebUI product screen | `SiteCreateWizard.tsx` + hosts unchanged |
| Vitest | picker + Traditional skip-template + ArchitectureShell first-step |
| Playwright | helper TEST_IDS, explorer chrome/happy path, new type-picker spec, architecture smoke |
| product-docs | `sites.md`, `architecture-navigation.md`, `content-explorer.md` |
| i18n | few `EXPLORER_MSG` keys only |

## Cross-platform path checklist

N/A — no new file I/O or path assertions.

## Memory patterns hit

- WebUI screen change requires Playwright companion
- Product-facing change requires `product-docs/8.2`
- Shared wizard used by two hosts — update both test surfaces

> Co-Authored by Grok Build 1.0.4 using grok-4.6 with agent night-issue-prs.
