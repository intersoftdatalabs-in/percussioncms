# Erlang review — issue #2460 HTTPClient rawtypes

**Scope:** `system/.../com/percussion/HTTPClient/**` rawtypes/unchecked cleanup  
**Reviewer persona:** Erlang (pre-commit gate)  
**Date:** 2026-08-09

## Verdict

**PASS** — safe to commit / open PR.

## Findings

### Bugs
None. Generics match existing map/list element types; cast removals preserve runtime behavior. Cookie jar load retains a single justified `@SuppressWarnings("unchecked")` on `ObjectInputStream.readObject()`.

### Tests
Added behavioral unit tests:
- `UtilGenericsTest` — parseHeader / getElement / assembleHeader / getList
- `CIHashtableGenericsTest` — case-insensitive keys + key enumeration
- `URIDefaultPortsGenericsTest` — typed scheme maps

`cd system && ../mvnw clean install`: **BUILD SUCCESS**, Tests run: 1439, Failures: 0 (new HTTPClient suites green).

### Cross-platform paths
No path I/O changes.

### Change-class companions
Package-scoped rawtypes cleanup; consumers of `Util.parseHeader` / `getList` updated in-package. Public API `Class[]` module methods widened to `Class<?>` (binary-compatible for callers). No Spring/DI surface.

### Residual
Package rawtypes/unchecked for in-scope collections zeroed in this PR; no residual child filed.

## Notes
- `HTTPConnection` module lists typed as `Vector<Class<?>>`; instantiation uses `getDeclaredConstructor().newInstance()`.
- Avoided overlap with open serial/this-escape work (#2650/#2653).
