# Erlang review — #4465 template CT assoc remove+save

**Scope:** uncommitted vs `HEAD` on absorb PR #4464 (`feat/issue-4461-tpl-content-type-assoc`).
**Modules:** `projects/sitemanage`, Playwright spec, `product-docs/8.2`.
**Recommendation:** approve
**Gate:** May commit/push: yes
**Cross-platform paths:** N/A (no new filesystem path I/O)

## Summary

Cycle Verify still listed `percImageAsset` on PUT/GET after UI remove+save. Add used
`findContentTypes` catalog GUIDs; remove used `PSContentTemplateDesc.getContentTypeId()`,
which rebuilds `new PSGuid(NODEDEF, storedLong)` (often host-0). Lock/load/save on host-0
does not mutate the real node. `persistableContentTypeGuid` remaps UUID → catalog GUID for
both add and remove. Unit test covers catalog host 1001 vs descriptor host-0. Playwright
opens `perc.page` via `data-tpl-name`.

HARD BAN honored: no #4462 / #4463 work.

## Issues

None (bugs). Behavioral test added for the host-0 vs catalog GUID remove path.

## Memory patterns hit

GUID identity / host-0 vs catalog packed long; omit vs empty list on collection replace.

## Re-review

Live H2 showed non-empty replace worked and `associatedContentTypes: []` was treated as omit
under CXF UNWRAP_ROOT_VALUE (same class as display-format `allowedCommunities:[]` / #4098).
Added `TemplateDetailJsonReader` + jaxrs:providers registration ahead of jacksonProvider.
Playwright 1 passed; golden 2; login 2. Gate still approve.
