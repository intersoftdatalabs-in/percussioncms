# Erlang review: issue #4087 REST UI-01 user control write

**Branch:** `feat/issue-4087-cecontrols-user-write`  
**Scope:** `rest` CE controls resource/adaptor/DTO/tests; `projects/sitemanage` ControlAdaptor + UserControlIo; `product-docs/8.2/developer/rest.md`  
**Base:** `origin/main`

## Summary

Admin POST/PUT/DELETE `/services/cecontrols` persist **user** CE controls through `PSCustomControlManager` (XSL file under `rx_resources/stylesheets/controls` + `writeImports`). System controls are 409 and packaged files are not mutated. REST only — no SPA chrome.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

Hard gates checked:

- Behavioral tests: `ControlsResourceTest` (22) and `ControlAdaptorWriteTest` (18) cover create/duplicate/blank/whitespace/wildcard/403/system-409/delete-204/path write.
- Change-class closure: resource + `IControlAdaptor` write methods + Mockito tests + Spring `TestControlAdaptor` (exact `IControlAdaptor`) + sitemanage adaptor + product-docs. No WebUI/Playwright (REST-only slice).
- Cross-platform I/O: `Path.resolve` chain and `Files.writeString` / `Files.deleteIfExists`; no hardcoded OS separators for filesystem joins. Tests use `@TempDir Path` and `dir.resolve(name + ".xsl")`.
- Spring stub type matches `IControlAdaptor`.
- Standalone `mvnw clean install`: rest then `projects/sitemanage` both BUILD SUCCESS.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New path logic uses `Path` / `Files`
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp files use JUnit `@TempDir` (portable)
- [x] Line-ending sensitive assertions not used on OS file `toString()`
- [x] XSL import href `/` is URL-style (existing manager; not this slice’s filesystem join)

## Issues

None blocking.

Nit (not a gate): optional `xslSource` parse uses product `PSXmlDocumentBuilder` (same as control-file load). Invalid source is 400.

## Memory patterns hit

- Incomplete change-class closure (rest Spring stub + sitemanage adaptor tests)
- Non-portable filesystem path joins
- Path containment uses trusted user-controls directory, not a parent derived from the name
- rest `MainTest` needs Spring test stubs for adaptor interfaces

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 954, Failures: 0; `ControlsResourceTest` 22
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2102, Failures: 0, Skipped: 125; `ControlAdaptorWriteTest` 18
