# Erlang review: issue #3582 archive-manifest Widget XML dual-ship

**Scope:** uncommitted changes on `fix/issue-3582-archive-manifest-widget-xml` vs `origin/main`  
**Module:** `modules/perc-packages`  
**Date:** 2026-08-18

## Summary

Product package source `psx_archiveInfo.xml` / `psx_archiveManifest.xml` no longer author `rxconfig/Widgets/*.xml` except waived `perc.Test`. Install emitter re-injects those user-dependencies on the staging copy so built `.ppkg` still ships Widget XML. Surefire inventory fails if the paths return.

## Recommendation

approve

## Gate

pass

## Cross-platform path checklist

- [x] On-disk I/O uses `Path.resolve` / `Files`
- [x] ZIP / archive / install-relative paths use `/` (not OS separators)
- [x] Widget stems reject `/`, `\`, and `..`
- [x] Tests do not assert OS `toString()` path shapes
- [x] Line-ending sensitive inject uses the file's existing `\r\n` or `\n`

## Issues

None (hard-gate).

Nit: stripped descriptors may retain a blank line where the Widget XML block was removed; remaining user-dependencies and XML structure stay valid.

## Memory patterns hit

- Missing behavioral unit tests — covered (scan/strip/inject/encode/materialize + product tree)
- Non-portable path joins — not present
- Deployer/packaging: install wire format preserved via emit-time inject; shim not deleted
- Incomplete change-class — companions present (inventory, tests, product source strip, emitter, README)

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
