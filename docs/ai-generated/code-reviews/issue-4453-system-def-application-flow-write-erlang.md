# Erlang review — issue #4453 SYS_APP_FLOW write

Independent pre-commit review of system-def application-flow GET/PUT
(REST + sitemanage adaptor + Developer SPA + Playwright + product-docs).

## Change class

New public REST adaptor surface + WebUI product screen + Playwright +
product-docs companions (same class as stylesheet write #4452 / #4458).

## Findings

### Bugs

None remaining after implementation. PUT validates handlers before acquiring
the system-def design lock (400 does not hold the lock). Omitted handlers are
removed; empty href keeps an empty Workbench MakeAbsLink first param so GET
round-trip of clone/modify defaults does not drop handlers. Existing
`sys_MakeAbsLink` converters are cloned and only the first text path is
updated (query params preserved). New handlers persist as `PSUrlRequest` href
defaults.

### Tests

- rest `SystemDefResourceTest` — GET/PUT 200, missing body 400, invalid 400,
  lock 409, Admin 403
- rest `TestSystemDefAdaptor` Spring stub implements new methods
- sitemanage `SystemDefAdaptorTest` — map href and MakeAbsLink, persist,
  add/remove, empty set 400, invalid href 400 without load, 403, 409
- `DesignGapsStructuredTest` — `SYS_APP_FLOW` / `SYS_STYLESHEET` dropped
- WebUI Vitest wrap/unwrap, panel save/add
- Playwright `developer-system-def-application-flow.spec.js`

### Portability

Href validation is string-shape (relative CMS app URL), not OS filesystem
paths. No hardcoded separators for local files.

### Companions

REST DTO + resource + adaptor interface + sitemanage impl + Spring test stub
+ SPA API/panel + product-docs + Playwright smoke-set entry. Did not reintroduce
`SYS_STYLESHEET`. Did not implement `TPL_LOCK` / `TPL_CONTENT_TYPE_ASSOC`.

## Verdict

Pass for commit after module clean installs and C5 surface Playwright.
