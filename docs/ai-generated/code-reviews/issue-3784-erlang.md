# Erlang review — issue #3784 Content Type field rule expressions PUT REST (CD-05-07)

**Branch:** `fix/issue-3784-field-rule-expressions-put`  
**Date:** 2026-08-25  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + wire DTOs + two Spring stubs + Mockito resource tests + sitemanage apibridge + adaptor tests + product-docs); behavioral tests for lock 409, unknown field, persist, post-save cache miss; no path I/O.

## Summary

Thin REST GET/PUT for Content Type **field-level** validation, visibility, and input/output translation expressions. PUT requires a held design-session lock (`409` without). Unknown field names are rejected (`404` GET, `400` PUT). Companions match rest/sitemanage AGENTS.md. Standalone `clean install` green on `rest` and `projects/sitemanage`.

## Scope

- Base: `origin/main`
- Head: `fix/issue-3784-field-rule-expressions-put`
- Files: rest content-type resource/adaptor/DTOs/stubs/tests; sitemanage `ContentTypeAdaptor` + `ContentTypeAdaptorFieldRuleExpressionsTest`; `product-docs/8.2/developer/rest.md` and `product-docs/8.2/admin/developer-content-types.md`
- Peer: CD-07 `controlProperties` (#3786) and CD-09 `itemExits`
- Prior report: none for this ticket
- Memory patterns hit: rest↔sitemanage companions; Spring `TestContentTypeAdaptor` + `ContentTypesTestAdaptor`; post-save `reloadItemDef` fallback (CD-07 pattern)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- Missing behavioral tests: no
- Non-portable path/file I/O: no
- May commit/push: yes

## Issues

None blocking.

PUT persist uses `IPSContentDesignWs.saveContentTypes` with `lock=true` load and does not release the lock. `requireHeldLock` maps unlocked / other-user to `ContentTypeDesignLockException` (HTTP 409). Unknown field on PUT is `IllegalArgumentException` ("Unknown field") → 400; GET is 404. Post-save item-def cache miss falls back to the locked in-memory `PSField` (`reloadItemDef` + `findField` null → `target`), matching the CD-07 Kilo cache-miss fix.

Change-class closure: `IContentTypesAdaptor` `get/replaceFieldRuleExpressions`; `ContentTypesResource` GET/PUT `.../fields/{fieldName}/ruleExpressions`; Spring stubs implement both new methods; sitemanage apibridge + 18 adaptor tests; product-docs REST + Developer Content Types notes. Detail PUT still ignores expression strings (`update_ignoresReadOnlyFieldExpressions`).

`contentTypeDesignGaps` `CT_FIELD_RULE_EXPR` now points at the dedicated write path. Envelope `designGaps` code `CT_FIELD_RULE_APPLY_WHEN` documents apply-when / literal limits.

## Cross-platform path checklist

Applied. No filesystem path construction. REST `/services/contenttypes/...` strings are URI paths (correct `/`).

## Tests

- rest: Mockito GET/PUT success, 404 missing type/field, 400 missing lists and unknown field, 409 lock not held / other user, JSON envelope
- rest Spring stubs: `TestContentTypeAdaptor`, `ContentTypesTestAdaptor`
- sitemanage: mapping round-trip (`!=` → `<>`), extension FQN, empty clears, visibility rejects `reference`, GET maps conditionals + summary, PUT persist + cache-miss fallback, 409 lock, 400 unknown field, 403 not admin
- Module suites: `rest` Tests run: 605, Failures: 0; `sitemanage` Tests run: 1568, Failures: 0, Skipped: 125

## Non-blocking notes

- WebUI expression editor remains out of scope (issue #3784).
- Conditional variable/value are text literals (documented gap); not a bug for this slice.
- C5 Playwright N/A (no WebUI chrome).
