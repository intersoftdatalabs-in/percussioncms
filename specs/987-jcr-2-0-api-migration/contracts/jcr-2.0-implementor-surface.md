# Contract: JCR 2.0 Implementor Surface (Internal)

**Feature**: `987-jcr-2-0-api-migration`  
**Audience**: Core CMS engineers  
**Stability**: Internal — not a public HTTP API

## Purpose

Product classes that implement `javax.jcr.*` interfaces MUST satisfy **JSR-283 (JCR 2.0)** method sets when compiling against `javax.jcr:jcr:2.0`.

## Dependency contract

| Coordinate | Version | Managed in |
|------------|---------|------------|
| `javax.jcr:jcr` | `2.0` | Parent `pom.xml` dependencyManagement |
| `org.apache.jackrabbit:jackrabbit-jcr-commons` | existing 2.22.x line | Parent `pom.xml` |

Modules MUST NOT reintroduce `javax.jcr:jcr:1.0`.

## Implementor obligations

### Required complete implementations (behavior-preserving)

| Interface | Primary product type(s) | Notes |
|-----------|-------------------------|-------|
| `Node` | `PSContentNode` (`IPSNode`) | `getIdentifier()` maps to existing identity; new optional features may UROE |
| `Property` | `PSProperty`, `PSMultiProperty` | Binary/decimal accessors |
| `Value` | `PSBaseValue` hierarchy | `getBinary`, `getDecimal` |
| `ValueFactory` | `PSValueFactory` | `createBinary`, decimal/binary factory methods |
| `Query` | `PSQuery` | bind/limit/offset; execute signature |
| `QueryResult` | `PSQueryResult`, publisher `RowQueryResult` | `getSelectorNames` |
| `QueryManager` | `PSContentMgr` / `IPSContentMgr` | `getQOMFactory` |
| `PropertyDefinition` | `PSPropertyDefinition` | query metadata methods |
| `NodeType` | `PSTypeConfiguration` | 2.0 hierarchy / new methods |

### Unsupported capability pattern

If the CMS does not implement a JCR optional feature:

```text
throw new UnsupportedRepositoryOperationException("<capability> not supported");
```

or return empty iterators / `false`, matching existing `PSContentNode` versioning/lock stubs.

Empty or UROE is preferred over silent incorrect success.

## Query language contract (unchanged product behavior)

Supported languages remain product SQL and XPath as returned by `getSupportedQueryLanguages()` (today `Query.SQL`, `Query.XPATH`).  
**No requirement** to add `Query.JCR_SQL2` or `Query.JCR_JQOM` in this feature.

## Public HTTP / package contracts

- REST, SOAP, sitemanage JSON, and `.ppkg` formats: **no intentional change**.
- If a public Java type exposes `javax.jcr.Node` / `Query` in a signature, binary compatibility for third-party JARs is **not** guaranteed (integrators rebuild).
