# Erlang review — 004/us3-java-xss-sink-annotations

**Date:** 2026-07-18  
**Branch:** `004/us3-java-xss-sink-annotations`  
**Scope:** same-line CodeQL annotations for 23 open `java/xss` alerts (Jackson/JAXB/CXF REST + proxy sinks)  
**Reviewer:** Erlang (independent pre-commit)

## Summary

Open `java/xss` alerts are residual false positives: JAX-RS methods return typed DTOs serialized as JSON/XML by Jackson/JAXB/CXF, or reverse-proxy/byte-pump sinks that do not construct HTML. Prior multi-line `// codeql[java/xss] justification: …` blocks sat several lines above the `return`/`write` and were ignored by CodeQL. This change moves annotations onto the **exact sink line** (preferred over bulk dismiss / path-exclude alone). Runtime defenses (`requireSafeId`, `XSSValidation`, typed DTOs) unchanged.

## Recommendation

**approve**

## Gate

|            Check             |                                   Result                                    |
|------------------------------|-----------------------------------------------------------------------------|
| Bugs                         | none                                                                        |
| Behavioral unit tests        | present (`PSSiteDataRestServiceXssTest` + new same-line source pin)         |
| Cross-platform path/file I/O | test uses `Path.of` + `Files.readString` with dual relative roots; portable |
| May commit/push              | **yes**                                                                     |

## Issues

None blocking.

### Nits

1. Local CodeQL model packs still not loaded by GHA; same-line annotations are the effective gate until GHCR pack publish.
2. Path query-filters for these files remain as belt-and-suspenders (already present).

## Tests run

```text
./mvn-env.sh -pl projects/sitemanage -Dtest=PSSiteDataRestServiceXssTest -Dai.integrity.skip=true test
# Tests run: 17, Failures: 0
```

