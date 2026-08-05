# Erlang review — issue #1822 Jackson XmlMapper parallel helper

**Branch:** `fix/issue-1822-jackson-xml-helper`  
**Scope:** `modules/utils` only (parallel Jackson XML path; Betwixt remains production default)  
**Date:** 2026-08-04  
**Recommendation:** approve  
**May commit/push:** yes

## Summary

Adds `PSJacksonXmlSerializationHelper` + shared `PSXmlElementNameMapper` beside
`PSXmlSerializationHelper`, pilot golden XML parity tests for keyword-shaped
`SampleKeyword`/`SampleChoice`, and parent-managed `jackson-dataformat-xml` dependency.
Production Betwixt path is unchanged except `PSNameMapper` delegates to the shared mapper
(same naming rules; existing Betwixt unit tests still green).

## Gate checklist

- [x] No production cutover of `PSXmlSerializationHelper` default
- [x] Behavioral unit tests for name mapper, golden parity, Betwixt→Jackson round-trip,
  legacy `<null>` root rewrite on Jackson path, `@IPSXmlSerialization(suppress=true)`
- [x] Cross-platform path / file I/O: not touched (classpath golden resource only)
- [x] Module standalone `mvnw clean install` — BUILD SUCCESS (Tests run: 255, Failures: 0)
- [x] Spotless apply then check on `modules/utils` — clean; out-of-scope monorepo Spotless debt not included

## Issues

None blocking.

### Suggestions (non-blocking)

1. **TYPE_MAP not yet used for Jackson polymorphic deserialize** — `addType` stores entries
   for API parity with Betwixt; pilot collection item names use `@JacksonXmlProperty`. Wire
   polymorphic handling in #1823 when migrating domain beans.
2. **Betwixt graph-identity `id="…"` attributes** — golden compare strips them; Jackson does
   not emit them. Documented in golden fixture + test normalizer.
3. **Betwixt without `.betwixt` files** does not restore nested collection items for the pilot
   (even write→read). Jackson→Betwixt test asserts scalars only; full nest restore is
   Betwixt→Jackson and Jackson self-path.

## Memory patterns hit

- Parallel helper / no silent production default flip
- Golden fixture from current producer (Betwixt write)
- Companion tests for new helper logic

