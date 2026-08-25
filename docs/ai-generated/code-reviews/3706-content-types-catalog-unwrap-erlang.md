# Erlang review — #3706 Content Types catalog Jackson list unwrap

**Branch:** `fix/issue-3706-content-types-catalog`  
**Date:** 2026-08-23  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Developer → Content Types catalog threw into `DeveloperSectionErrorBoundary`
(`Unable to load Content Types…`) because `GET /services/contenttypes` is
serialized with Jackson `WRAP_ROOT_VALUE` as `{"ContentTypeList":[…]}` (class
name). SPA `unwrapContentTypeList` only read top-level `ContentType` /
`contentType`, so a truthy non-array could reach `[...items].sort` / `.map`.

This change recursively unwraps `ContentTypeList` / nested `ContentType` /
`ArrayList` / empty-collection beans, re-applies unwrap in the panel, stringifies
catalog cells so object JAXB wraps cannot throw as React children, and isolates
detail with a nested error boundary (Templates peer). Rest unit test documents
the live JSON root. Playwright covers catalog load + first-row Object ACL.

Memory patterns hit: Jackson non-array `.map` (Slot #3554 / searches #3576);
Playwright companion for WebUI screen; product-docs for REST wire.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

Not applicable — no filesystem path/file I/O in the diff (JSON unwrap + React
catalog + Playwright selectors + REST JSON assertion).

## Issues

None blocking.

### nit

- `asContentTypeCatalog` is a one-line alias of `unwrapContentTypeList`; fine
  as a named panel contract.

## Tests

- Vitest: `unwrapContentTypeList` envelopes (root array, nested list, ArrayList,
  empty bean, per-item wrap); `ContentTypesPanel` does not throw on envelope /
  object label / empty bean.
- Rest: `JacksonContextResolverOptionalTest.contentTypeList_serializesNamesNotHideFromMenuOnly`
  asserts `ContentTypeList` root array (plain JsonMapper, not UNWRAP_ROOT_VALUE).
- Playwright: `bug-3706-developer-content-types-catalog.spec.js`.
- Module clean install: `rest` BUILD SUCCESS (Tests run: 544, Failures: 0);
  `WebUI` BUILD SUCCESS (Tests 3014 passed).
- C5 H2 QA: `perc-devctl qa-up --skip-image-build` TEST_CMS_URL=http://127.0.0.1:9993
  HEALTH:healthy; hot-copied `cm/modern/assets`; Playwright
  `tests/bugs/bug-3706-developer-content-types-catalog.spec.js` 1 passed;
  console-clean=yes; server.log ERROR/FATAL for test window=none.

## Change-class closure

WebUI catalog + API unwrap + Vitest + Playwright + product-docs `rest.md` list
envelope note + rest Jackson shape test. No sitemanage adaptor change (list
already returns `List<ContentType>`).
