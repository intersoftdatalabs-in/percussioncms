# Erlang review — #4294 SY-06 workflow allowed content types

**Branch:** `feat/issue-4294-workflow-allowed-content-types`  
**Scope:** uncommitted REST + sitemanage + product-docs  
**Date:** 2026-09-04

## Summary

Admin GET/PUT `/services/workflows/{idOrName}/allowedContentTypes` peers CD-08
`ContentTypeWorkflows` from the workflow side. Change-class companions present
(resource, DTO, adaptor interface, Spring stub, Mockito resource tests, sitemanage
`WorkflowsAdaptor` + unit tests, product-docs note vs CD-08).

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (resource + adaptor)
- Cross-platform paths: N/A (no file I/O)
- May commit/push: **yes**

## Issues

None.

## Notes

- PUT acquires/releases design locks per affected CT (documented vs CD-08 held-lock model).
- Standalone `rest` then `projects/sitemanage` clean install green before PR.
