# Erlang review: #4055 CD-01 SPA content type create/delete

- Branch: `feat/issue-4055-spa-content-type-create-delete`
- Scope: uncommitted WebUI SPA catalog create/delete, perc-qa-automation Playwright, product-docs
- Base: `origin/main`
- Memory patterns hit: behavioral tests for new logic; WebUI Playwright companion; product-docs for operator chrome; no extra Spring/Java adaptor (REST already shipped)

## Summary

Developer Content Types catalog now POSTs `createContentType` (catalog **New**) and DELETEs after a held design lock. Client does not implement rename or REST ALTER. Peer chrome is Locales create + lock-gated Content Type detail.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None in SPA code (bugs).

### Notes (non-blocking)

- Client-side name charset is stricter than the first REST create check (letters/digits/`_`/`.`) so invalid characters never POST; 400 from REST is still surfaced.
- Unlocked DELETE is disabled in chrome; Playwright also probes live DELETE 409 without acquiring a lock.
- Live H2 `POST /services/contenttypes` persist currently fails (`PSErrorsException` from `saveContentTypes`, log `Content Type (id=0)`). SPA surfaces the error. Duplicate 409 works. Residual REST persist is out of this SPA slice (do not re-implement REST here).
- Cross-platform path checklist: N/A (no filesystem path I/O; REST URLs use `/`).

## Tests

- Vitest: invalid name 400 (client disable + REST), 409 duplicate, 403 non-Admin, 409 unlocked delete (API no lock POST; UI delete disabled / 409 does not re-lock).
- Playwright: `tests/developer-content-type-create-delete.spec.js` (H2 surface).
- Product-docs: `product-docs/8.2/admin/developer-content-types.md` and REST notes.
