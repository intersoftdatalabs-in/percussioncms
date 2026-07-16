# Contract: Integrator Extension Rebuild (Custom Java)

**Feature**: `987-jcr-2-0-api-migration`  
**Audience**: Partners and custom extension authors  
**Stability**: Documented upgrade expectation

## Summary

Percussion CMS development line uses the **JCR 2.0** (`javax.jcr` 2.0) API library. Custom Java extensions that compile against product APIs **must be rebuilt** with the upgraded product (and JCR 2.0) on the classpath.

## Guarantees

| Area | Guarantee |
|------|-----------|
| Editor / publisher UX | No intentional functional change |
| HTTP APIs (REST/SOAP/UI services) | Backward compatible |
| Content data / DB schema | No migration required for standard installs |
| Pre-built extension JARs compiled only for JCR 1.0 types | **Not** guaranteed to load or run |

## Integrator actions

1. Obtain product SDK / compile dependencies from the upgraded release.
2. Ensure `javax.jcr:jcr:2.0` is on the extension compile classpath (normally via product BOM).
3. Recompile extension sources.
4. If the extension **implements** `javax.jcr.Node`, `Property`, `Value`, `Query`, etc., add any new 2.0 methods (see implementor surface contract).
5. Prefer `Node.getIdentifier()` over deprecated `getUUID()` for JCR nodes.
6. Redeploy rebuilt extension JAR.

## Failure mode (accepted)

Deploying an old extension JAR without rebuild may fail class load or linkage. This is **not** a product defect for this upgrade.

## Product documentation obligation

Release notes MUST state:
- Content repository API standard upgraded 1.0 → 2.0
- Custom Java extensions require rebuild
- No standard content data migration
