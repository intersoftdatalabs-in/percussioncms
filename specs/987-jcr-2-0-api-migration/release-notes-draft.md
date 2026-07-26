# Release Notes Draft: JCR 2.0 API Migration (Spec 987)

**Release**: Percussion CMS 8.2.0  
**Issues Addressed**: #506, #531

---

## Highlights

### JCR 2.0 API Upgrade (JSR-283)

Percussion CMS has upgraded its internal Java Content Repository interface library from `javax.jcr:jcr:1.0` to `javax.jcr:jcr:2.0` (`JSR-283`). This modernization ensures compatibility with Java 21, modern library standards, and security compliance guidelines.

### Compatibility & Content Data Safety

- **No Database / Content Data Migration Required**: Content items, properties, and repository data schemas remain 100% compatible.
- **Query Compatibility**: Legacy `Query.SQL` and `Query.XPATH` query languages remain fully supported by `PSContentMgr` and query finders.

### Important Information for Extension Developers

Custom Java extensions, PSO plugins, and third-party integrations compiled against earlier versions of Percussion CMS **must be recompiled** against the 8.2.0 API release.

For complete details and signature migration guidance, please refer to the [Integrator Rebuild Contract](./contracts/integrator-rebuild.md).
