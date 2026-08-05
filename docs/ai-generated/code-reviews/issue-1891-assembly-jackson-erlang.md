# Erlang review — #1891 assembly Jackson migration

**Verdict:** PASS (self-review before commit)

## Change class

Design-object XML domain batch (assembly templates/slots) under Jackson-backed
`PSXmlSerializationHelper` + companion shared GUID converters + golden/package tests.

## Checklist

|          Gate           |                                             Result                                              |
|-------------------------|-------------------------------------------------------------------------------------------------|
| Bugs / broken contracts | None found — package association normalize preserved; non-zero CT/template ids                  |
| Behavioral unit tests   | `PSAssemblyXmlSerializationTest` (10), `PSTemplateSlotXmlRestoreTest` (6), GUID helper test     |
| Cross-platform paths    | Portable `Path` / classpath resources only; no OS path separators                               |
| Companions              | Restored `templateSlotIds`; GUID converters; `jackson-dataformat-xml` on system; deviations doc |
| `.betwixt` drop         | Proven via golden + package smoke; both assembly production betwixt files removed               |
| Scope creep             | No publisher/sitemgr/workflow remainder                                                         |

## Residual

None for this slice. Dual-engine full Betwixt rollback for these types is intentionally dropped
pending monorepo Betwixt removal (#1824).
