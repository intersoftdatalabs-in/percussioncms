# Erlang review: issue #3674 leftover binary TemplateDefs

| Field | Value |
|-------|--------|
| **Branch** | `fix/issue-3674-widget-binary-templatedefs` |
| **Scope** | `modules/perc-packages` leftover `perc.fileBinary` / `perc.imageMainBinary` / `perc.imageThumbBinary` |
| **Recommendation** | approve |
| **Gate** | pass |
| **May commit/push** | yes |
| **Date** | 2026-08-20 |

## Summary

Converted the last three product-authored root `*.templateDef` files (binary asset assemblers in widget packages) to modern `pages/<id>/component-package.json` and native archive install. Widget XML emitter is not used: these are assembly TemplateDefs (`output-format=Binary`, `binaryAssembler`), not Widget definition XML. Dual-ship emitter now omits HTML charset/mime defaults for `type=binary` and preserves `Local` vs `Shared` via `legacyTemplateType`. Inventory scanner fails if unexplained root `*.templateDef` reappear.

Memory patterns hit: portable `Path`/`Files` for inventory; do not silently delete install wire format; keep mapping + ACL side-cars.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Inventory/tests use `Path` / `Files` / `DirectoryStream`
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp files use `@TempDir` / `Path`
- [x] Line-ending sensitive comparisons normalize `\r\n`

## Issues

None (no bugs, missing behavioral tests, or non-portable I/O).

## Tests

`PSAuthoredRootTemplateDefInventoryTest`, leftover XML compile tests, native product tests for FileAssetWidget / widgets.image, emit binary empty mime/charset. Module `mvnw clean install`: Tests run: 188, Failures: 0. Package build: `native-install page TemplateDefs for perc.FileAssetWidget: 1` and `perc.widgets.image: 2`.
