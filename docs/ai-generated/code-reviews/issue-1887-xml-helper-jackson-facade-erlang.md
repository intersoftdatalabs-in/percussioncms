# Erlang review — issue #1887 PSXmlSerializationHelper Jackson facade

**Branch:** `fix/issue-1887-xml-helper-jackson-facade`  
**Scope:** `modules/utils` only (production facade cutover to Jackson; Betwixt retained for rollback)  
**Date:** 2026-08-04  
**Recommendation:** approve  
**May commit/push:** yes  

## Summary

Switches `PSXmlSerializationHelper` public `writeToXml` / `readFromXML` defaults to Jackson via
`PSJacksonXmlSerializationHelper` while preserving signatures (`addType`, `readFromXML`,
`writeToXml`, `getIdFromXml`, `rewriteLegacyNullRoot`). `addType` dual-registers Betwixt + Jackson
type maps. Betwixt remains on the classpath; emergency rollback via system property
`com.percussion.xml.serialization.engine=betwixt` (default `jackson`). No domain bean migration
(out of scope — later #1823 slices / #1888+). No `commons-betwixt` POM removal.

## Gate checklist

- [x] Public API signature parity preserved
- [x] Behavioral unit tests: golden fixture via facade, Jackson round-trip with nested choices,
      legacy `<null>` root, suppress annotation, engine property rollback, dual `addType` registry
- [x] Cross-platform path / file I/O: not touched (classpath golden resource only)
- [x] Module standalone `mvnw clean install` — BUILD SUCCESS
- [x] Spotless apply then check on `modules/utils` (in-scope only)
- [x] `synchronized` retained on public write/read until Betwixt fully removed
- [x] No Betwixt POM / `.betwixt` file removal

## Issues

None blocking.

### Suggestions (non-blocking)

1. **Jackson TYPE_MAP still not wired for polymorphic item deserialize** — collection item names
   for domain beans still need Jackson annotations or mix-ins in domain slices (#1888+).
2. **Rollback flag removal** — drop `ENGINE_PROPERTY` and Betwixt private paths after domain
   migration and #1824 (`commons-betwixt` removal). Documented on the helper and in the PR body.
3. **Approved XML deviation** — Jackson does not emit Betwixt graph-identity `id="…"` attributes;
   golden compare continues to strip them.

## Memory patterns hit

- Facade cutover after parallel pilot proved golden parity
- Dual-engine rollback property with explicit removal plan
- Companion golden + round-trip + legacy null-root tests for production path change
- Keep Betwixt dependency until dedicated removal issue
