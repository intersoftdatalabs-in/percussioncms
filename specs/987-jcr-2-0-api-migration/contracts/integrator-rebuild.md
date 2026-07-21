# Integrator Rebuild Contract: JCR 2.0 API Upgrade

**Specification**: Spec 987 (JCR 1.0 to 2.0 API Migration)  
**Target Release**: Percussion CMS 8.2.0  
**Requirement**: FR-011 / FR-009

---

## 1. Overview

Percussion CMS 8.2.0 upgrades the Java Content Repository API dependency from `javax.jcr:jcr:1.0` to `javax.jcr:jcr:2.0` (`JSR-283`).

All built-in Percussion CMS modules and core extensions have been compiled against and verified with `javax.jcr:jcr:2.0`. 

**Action Required for Custom Extensions & Third-Party Integrations**:  
Custom Java extensions, custom plugins, or third-party integrations compiled against Percussion CMS 8.1.x (or earlier JCR 1.0 builds) MUST be recompiled against the Percussion CMS 8.2.0 SDK / API dependencies.

---

## 2. API & Signature Changes

### 2.1 Deprecated Method Calls
- `javax.jcr.Node.getUUID()` is deprecated in JCR 2.0. Replace with `Node.getIdentifier()`.
- `javax.jcr.Property.getStream()` and `setValue(InputStream)` are deprecated in JCR 2.0. Replace with `Property.getBinary()` and `Property.setValue(Binary)`.

### 2.2 Extended JCR 2.0 Interfaces
- Implementors of `javax.jcr.Node`, `javax.jcr.Property`, `javax.jcr.Value`, `javax.jcr.query.Query`, or `javax.jcr.nodetype.NodeType` must support the new JSR-283 interface methods.
- Built-in Percussion types (e.g. `PSContentNode`, `PSProperty`, `PSQuery`) throw `UnsupportedRepositoryOperationException` for optional features not supported by the underlying CMS engine (such as JCR Query Object Model or lifecycle transitions).

---

## 3. Query Language Compatibility

- Percussion CMS maintains full backward compatibility for `Query.SQL` and `Query.XPATH` strings.
- Custom extensions executing repository queries via `PSOQueryTools` or `PSContentMgr` do NOT need to rewrite existing JCR SQL queries to JCR-SQL2.

---

## 4. Rebuild Verification Checklist

1. Update project POM/build dependencies to `javax.jcr:jcr:2.0` (or set Percussion CMS parent POM to `8.2.0-SNAPSHOT` / `8.2.0`).
2. Run a clean build (`mvn clean compile` / `gradle clean build`).
3. Replace any usage of `Node.getUUID()` with `Node.getIdentifier()`.
4. Deploy updated JARs into `/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/lib/` or custom extension directory.
