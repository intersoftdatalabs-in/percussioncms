# Erlang review — #4543 extension method map edit

Independent review of uncommitted work on `fix/issue-4543-extension-method-map`.

## Change class

Workbench-replacement write for extension **method map**: REST DTO + sitemanage adaptor persist, SPA editor, Playwright, product-docs. Not a layer split.

## Findings

### Bugs

None. PUT `methods == null` keeps the current map; empty map clears. Blank/duplicate method names are `IllegalArgumentException` → HTTP 400. System/handler-owned rows remain 409 before persist. GET copies `returnType` and parameters.

### Tests

Behavioral coverage exists:

- `ExtensionAdaptorWriteTest` — register round-trip, replace, omit-keep, clear, blank name 400, duplicate 400, system 409, default return type
- `ExtensionsResourceTest` — invalid method map 400
- Vitest — wrap wire, gap filter, normalize/rows, SPA save includes methods
- Playwright — `developer-extension-method-map.spec.js`

### Paths / portability

No filesystem path construction. Extension keys already reject `..` / `\` / NUL.

### Companions

rest resource + DTO + adaptor tests; sitemanage persist; WebUI Vitest; perc-qa-automation Playwright; `product-docs/8.2/admin/developer-extensions.md`. Avoided `product-docs/8.2/developer/rest.md` (open PR #4566 already touches that thrash path). Integrator page still links to this admin page.

Live QA: GET JSON unwraps a single method as a bare object (not an array). SPA
`normalizeMethods` treats that shape as one method. Empty `[]` is dropped by JAXB;
clear-save sends `{name:""}` which the adaptor skips. `PSExtensionHandler.storeConfig`
now persists methods in Extensions.xml (`excludeMethods=false`).

## Verdict

Pass. No remaining hard-gate bugs, missing behavioral tests, or non-portable I/O.
Playwright `developer-extension-method-map.spec.js` 1/1 on QA H2.
