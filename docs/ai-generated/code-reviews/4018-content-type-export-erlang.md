# Erlang review — #4018 CD-14 content-type design XML export

| Field | Value |
|-------|--------|
| **Date** | 2026-08-29 |
| **Branch** | `feat/issue-4018-content-type-export` |
| **Issue** | #4018 (parent #1690, FR CD-14 export) |
| **Recommendation** | **approve** |
| **May commit/push** | yes |
| **Gate** | no blocking bugs |

## Summary

Admin REST `GET /services/contenttypes/{idOrName}/export` returns Workbench-equivalent
`PSItemDefinition` design XML loaded through existing `IPSContentDesignWs`
(`lock=false`, `overrideLock=false`). Peer is AS-08 template export (#4004 / PR #4009).
Change-class closure is complete: rest DTO + adaptor method + resource + Mockito tests +
Spring `TestContentTypeAdaptor` stub + `ContentTypesTestAdaptor` + sitemanage impl +
adaptor tests + product-docs 8.2 + gap-map note. Import remains a later CD-14 slice.

Memory patterns hit: change-class completeness (rest Spring stub + sitemanage impl);
filename sanitizer is HTTP basename (not filesystem path join).

## Cross-platform path checklist

- [x] No filesystem `"/" +` / `"\\" +` construction
- [x] Filename helper sanitizes HTTP `Content-Disposition` basename only
- [x] Tests do not write temp files
- [x] N/A Path/Files — no I/O

## Issues

None.

## Tests / Maven

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 823, Failures: 0
  (`ContentTypesResourceDetailTest` 129/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS;
  Tests run: 1875, Failures: 0, Skipped: 125 (`ContentTypeAdaptorExportTest` 9/0)
