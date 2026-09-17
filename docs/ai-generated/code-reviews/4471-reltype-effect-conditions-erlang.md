# Erlang review: #4471 relationship-type effect conditions and execution context

- **Date:** 2026-09-16
- **Branch:** `fix/issue-4471-reltype-effect-conditions`
- **Scope:** uncommitted work vs `origin/main` (REST DTO + sitemanage adaptor + WebUI SPA + Playwright + product-docs)
- **Recommendation:** approve
- **Gate:** May commit/push: yes
- **Memory patterns hit:** change-class closure (rest DTO + sitemanage adaptor + SPA + Playwright + product-docs); behavioral tests for write/clear/400/403; no new adaptor interface (existing `IRelationshipTypeAdaptor` / MainTest stub unchanged)

## Summary

Slice 14 of parent #1690 adds read/write of relationship-type **effect conditions** and **execution contexts** as one vertical increment. GET projects conditions/contexts on each effect. PUT matches existing effects by `extensionRef` or `name` and replaces or clears those lists (`[]` or `clearConditions` / `clearExecutionContexts`). SPA Developer → Relationship types editor is no longer display-only for those fields. The previous `DESIGN_GAPS` string is dropped.

## Cross-platform path checklist

Not applicable: no filesystem path joins, installer, or OS-specific I/O. REST/SPA/Playwright use URL paths only.

## Issues

None (hard-gate).

## Notes (non-blocking)

- Adaptor does not add/remove effect extensions (documented). Unknown match is 400.
- Playwright copies from `ActiveAssembly-Mandatory` (H2 `ActiveAssembly` has no packaged effects; `NewCopy` copy-from hits a pre-existing `PSCollection.clone` InternalError on clone overrides). The spec resets existing condition/context rows before add/clear.
- Product-docs updated: `product-docs/8.2/admin/developer-relationship-types.md` and `product-docs/8.2/developer/rest.md`.
