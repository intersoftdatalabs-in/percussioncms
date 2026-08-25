# Erlang review — issue 3744 Developer Content Type lock/save chrome

**Scope:** uncommitted chrome vs `feat/issue-3744-content-type-lock-save-chrome` stacked on #3749/#3748 (REST lock + PUT-requires-lock already on this branch). Base for product: `origin/main`.
**Reviewer:** Erlang (independent of implementer).
**Date:** 2026-08-23
**Memory patterns hit:** change-class closure (WebUI screen → Vitest + Playwright + product-docs); behavioral tests for lock/save/unlock; empty-catch only with justified ignore; i18n English-after-`@` fallback (TMX matrix optional, prior UI slices).

## Summary

Developer Content Type detail now has explicit **Lock / Save / Unlock** chrome. The SPA client no longer wraps PUT with lock/unlock; PUT requires a held lock. Fields stay read-only until lock. Playwright covers Admin lock → description save → unlock. Product-docs page `admin-developer-content-types` plus REST catalog note.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A for production I/O (no new filesystem joins). Playwright spec uses `URLSearchParams` and existing `catalogRowSelector` helpers (`path.join` in auth helper). Line endings not asserted as raw `\n` file dumps.

## Issues

### suggestion (REST live, residual)

H2 QA PUT save returns `OBJECT_NOT_LOCKED` for id `8,589,935,593` after a successful POST `/lock`. `saveContentTypes` looks up `new PSGuid(NODEDEF, typeId)` (packed long) while `loadContentTypes(..., lock=true)` stores `nodeDef.getGUID().longValue()` (uuid-only when host is 0). Chrome wrap + lock UX is correct; live description save is a REST lock-id residual.

### nit

- New `DEV_MSG` keys use English-after-`@` fallback without `DeveloperUi.tmx` TUs (same as prior Developer chrome slices). Optional locale-pack follow-up.
- `handleBack` / unmount unlock errors are swallowed after a justifying comment so Back cannot trap the operator.

## Tests

- `contentTypesApi.test.ts`: lock POST unwrap, unlock POST, PUT without wrap.
- `ContentTypeDetailPanel.test.tsx`: disabled until lock; lock/save/unlock; 409 lock; 409 save clears lock; Back unlocks.
- `DeveloperShell.test.tsx`: lock before field/association save.
- Playwright: `developer-content-type-lock-save.spec.js`.
