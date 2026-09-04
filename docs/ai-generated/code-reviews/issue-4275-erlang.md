# Erlang review — issue #4275 (REST SY-02 server config write)

**Date:** 2026-09-03  
**Branch:** `feat/issue-4275-rest-server-config-write`  
**Recommendation:** approve  
**Gate:** May commit/push: yes

## Summary

Adds Admin `PUT /services/serverconfigs/{name}` for allow-listed `PSConfigurationTypes` file bodies. Path safety rejects non-enum / traversal keys before `IPSSystemService.saveConfiguration`. Companions match rest change-class: interface method, resource PUT, Mockito tests, Spring `TestServerConfigAdaptor` stub, sitemanage write tests, product-docs.

## Cross-platform path checklist

- No client-supplied filesystem paths; save resolves via enum → `getContentDescriptor`.
- Content bytes use `StandardCharsets.UTF_8` / `IOTools.getContent` (UTF-8).
- Tests assert logical key strings only (no OS path separators in assertions).

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; ServerConfigsResourceTest 16/0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; ServerConfigAdaptorWriteTest 9/0
