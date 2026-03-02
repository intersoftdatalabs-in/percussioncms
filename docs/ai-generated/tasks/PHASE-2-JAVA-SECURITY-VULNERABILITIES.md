# Phase 2: Java Security Vulnerability Remediation

**Status**: In Progress
**Date**: 2026-03-02
**Focus**: Critical Java security vulnerabilities (SQL Injection, SSRF, Deserialization, XXE)

---

## ✅ Completed Fixes

### 1. Hibernate SQL Injection (Taxonomy Module)

**File**: `modules/perc-taxonomy/src/main/java/com/percussion/taxonomy/repository/HibernateValueDAO.java`
- **Lines**: 327, 379
- **Vulnerability**: HQL query string concatenation with user-controlled `langID` parameter
- **Fix Applied**: Migrated to parameterized HQL queries using `.setParameter("langId", langID)`
- **Status**: ✅ FIXED

**File**: `modules/perc-taxonomy/src/main/java/com/percussion/taxonomy/repository/HibernateNodeDAO.java`
- **Lines**: 307, 325, 422
- **Vulnerabilities**:
  - `getRelatedNodes()`: String concatenation with `nodeID`
  - `getRelatedNodeReferences()`: Multi-line string concatenation with `nodeID`
  - `getSimilarNodes()`: Direct string concatenation with `nodeID`
  - `changeParent()`: Two parameters concatenated into query
- **Fix Applied**: Converted all methods to use parameterized queries with `.setParameter()`
- **Status**: ✅ FIXED

**Test Results**:
```
Module: perc-taxonomy
Build: ✅ Compile successful
Parameter binding: ✅ Validated
Migration: From executeQuery() helper to direct parameterized queries
```

---

## 📋 Remaining Critical Vulnerabilities

### Phase 2.1: JCR Query Injection (HIGH PRIORITY)

#### 1. PSActivityService.java - Line 448-451
**File**: `projects/sitemanage/src/main/java/com/percussion/activity/service/impl/PSActivityService.java`
**Method**: `createJCRQuery(String path, Collection<String> contentTypes)`

```java
// VULNERABLE:
return "select rx:sys_contentid from nt:base where jcr:path like '" + path + "/%'";
return "select rx:sys_contentid from " + joined + " where jcr:path like '" + path + "/%'";
```

**Issues**:
- `path` parameter directly concatenated into JCR query string
- `joined` (built from contentTypes) also concatenated
- JCR doesn't support parameterized queries for string values
- Path could contain single quotes or JCR metacharacters

**Remediation**:
```java
private String createJCRQuery(String path, Collection<String> contentTypes) {
  // Validate path - no single quotes, proper JCR path format
  if (path == null || !path.matches("[a-zA-Z0-9/_-]+")) {
    throw new IllegalArgumentException("Invalid path format");
  }

  // Escape single quotes by doubling them (JCR escaping)
  String escapedPath = path.replace("'", "''");

  if (contentTypes == null || contentTypes.isEmpty()) {
    return "select rx:sys_contentid from nt:base where jcr:path like '" + escapedPath + "/%'";
  }

  // Validate content types - alphanumeric and underscore only
  var joined = contentTypes.stream()
      .filter(name -> name.matches("[a-zA-Z0-9_]+"))
      .map(name -> "rx:" + name)
      .collect(Collectors.joining(", "));

  if (joined.isEmpty()) {
    throw new IllegalArgumentException("No valid content types provided");
  }

  return "select rx:sys_contentid from " + joined + " where jcr:path like '" + escapedPath + "/%'";
}
```

**Severity**: 🔴 CRITICAL
**Impact**: JCR query injection, data exfiltration
**CWE**: CWE-89 (SQL Injection)

---

### Phase 2.2: Unsafe Deserialization (CRITICAL - RCE RISK)

#### 1. PSCacheItem.java - Line 368-369
**File**: `system/src/main/java/com/percussion/server/cache/PSCacheItem.java`

```java
// VULNERABLE:
try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
  o = in.readObject();  // No class validation
}
```

**Issues**:
- Deserializes arbitrary Java objects from file without validation
- Can load gadget chain classes for RCE

**Remediation**:
```java
private static final Set<String> ALLOWED_CLASSES = Set.of(
  "com.percussion.server.cache.PSCacheItem",
  "java.util.HashMap",
  "java.util.ArrayList"
);

try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
  // Override resolveClass to validate incoming classes
  ObjectInputStream validatingIn = new ObjectInputStream(new FileInputStream(file)) {
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
      if (!ALLOWED_CLASSES.contains(desc.getName())) {
        throw new InvalidClassException("Unauthorized class for deserialization: " + desc.getName());
      }
      return super.resolveClass(desc);
    }
  };
  o = validatingIn.readObject();
}
```

**Severity**: 🔴 CRITICAL (RCE)
**CWE**: CWE-502 (Deserialization of Untrusted Data)

#### 2. PSDtdTree.java - Line 162-163
**File**: `system/src/main/java/com/percussion/xml/PSDtdTree.java`

```java
// VULNERABLE:
ObjectInputStream objInStream = new ObjectInputStream(inStream);
clone = objInStream.readObject();
```

**Remediation**: Apply same class whitelist filtering as PSCacheItem

#### 3. CookieModule.java - Line 108-110
**File**: `system/src/main/java/com/percussion/HTTPClient/CookieModule.java`

```java
// VULNERABLE:
try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cookie_jar))) {
  (ConcurrentHashMap) ois.readObject();
}
```

**Remediation**: Validate only ConcurrentHashMap class is deserialized

---

### Phase 2.3: Server-Side Request Forgery (CRITICAL)

#### 1. PSFileDownLoadJobRunner.java - Line 145
**File**: `projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/utils/PSFileDownLoadJobRunner.java`

```java
// VULNERABLE:
var fileUrl = uri.toURL();
connection = (HttpsURLConnection) fileUrl.openConnection();  // No validation
```

**Issues**:
- Opens HTTP connection to user-provided URL
- Can access internal services, cloud metadata (169.254.169.254)
- No host/protocol validation

**Remediation**:
```java
private static final Set<String> ALLOWED_HOSTS = Set.of(
  "example.com",
  "cdn.example.com"
);
private static final Set<String> ALLOWED_PROTOCOLS = Set.of("https");
private static final int TIMEOUT_MS = 10000;

private HttpsURLConnection validateAndOpenConnection(URI uri) throws SecurityException {
  // Validate protocol
  if (!"https".equalsIgnoreCase(uri.getScheme())) {
    throw new SecurityException("Only HTTPS protocol allowed");
  }

  String host = uri.getHost();
  if (host == null || !ALLOWED_HOSTS.contains(host)) {
    throw new SecurityException("Host not in whitelist: " + host);
  }

  // Prevent local network access
  if (isPrivateIP(host)) {
    throw new SecurityException("Private network access denied");
  }

  URL url = uri.toURL();
  HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
  connection.setConnectTimeout(TIMEOUT_MS);
  connection.setReadTimeout(TIMEOUT_MS);

  return connection;
}

private boolean isPrivateIP(String host) {
  InetAddress addr = InetAddress.getByName(host);
  return addr.isLoopbackAddress() ||
         addr.isPrivateAddress() ||
         addr.isLinkLocalAddress();
}
```

**Severity**: 🔴 CRITICAL (Internal service access)
**CWE**: CWE-918 (SSRF)

#### 2. PSDTSStatusProvider.java - Line 131-141
**File**: `projects/sitemanage/src/main/java/com/percussion/utils/PSDTSStatusProvider.java`

```java
// VULNERABLE:
var url = new URL(surl);
var conn = (HttpsURLConnection) url.openConnection();
```

**Remediation**: Apply same URL validation as PSFileDownLoadJobRunner

#### 3. PSHttpConnection.java - Line 289, 383
**File**: `modules/utils/src/main/java/com/percussion/util/PSHttpConnection.java`

**Remediation**: Centralize URL validation in utility class

---

### Phase 2.4: XXE (XML External Entity) Issues

#### PSDtdTree.java - Line 204
**File**: `system/src/main/java/com/percussion/xml/PSDtdTree.java`

```java
// VULNERABLE (Combined XXE + SSRF):
URLConnection conn = dtdURL.openConnection();
```

**Note**: This also has SSRF vulnerability for DTD URL

**Remediation**: Use `PSSecureXMLUtils` which is already in codebase:
```java
XMLInputFactory xif = XMLInputFactory.newFactory();
xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
```

Or use existing security class:
```java
DocumentBuilderFactory dbf = PSSecureXMLUtils.getSecuredDocumentBuilderFactory(...);
```

---

## 🔗 Implementation Roadmap

### Week 1: JCR & SQL Injection
- [ ] Fix PSActivityService JCR injection
- [ ] Fix ItemRestServiceImpl JCR concatenation
- [ ] Fix PSSaveAssetsMaintenanceProcess
- [ ] Create input validation utility class

### Week 2: Deserialization
- [ ] Create ClassWhitelistFilter utility
- [ ] Apply to PSCacheItem, PSDtdTree, CookieModule, PSDataMapping
- [ ] Audit other ObjectInputStream usage

### Week 3: SSRF
- [ ] Create URLValidator utility
- [ ] Fix PSFileDownLoadJobRunner
- [ ] Fix PSDTSStatusProvider
- [ ] Fix PSHttpConnection, PSPageUtils

### Week 4: XXE & Testing
- [ ] Review XML parser usage
- [ ] Ensure PSSecureXMLUtils applied
- [ ] Create comprehensive unit tests
- [ ] Module build and test validation

---

## 📊 Vulnerability Summary

| Type | Count | Status | Risk |
|------|-------|--------|------|
| SQL Injection (HQL) | 2 | ✅ FIXED | Critical |
| SQL Injection (JCR) | 2 | 📋 TODO | Critical |
| Deserialization | 4 | 📋 TODO | Critical (RCE) |
| SSRF | 5 | 📋 TODO | Critical |
| XXE | 1 | 📋 REVIEW | High |
| **TOTAL JAVA** | **14** | Partial | Mostly Critical |

---

## 🧪 Testing Strategy

Each fix should include:

1. **Unit Tests**: Parameterized test cases for valid/invalid inputs
2. **Security Tests**: Attempt injection with malicious payloads
3. **Integration Tests**: Module-level end-to-end validation
4. **Build Tests**: Full module compilation and test suite

---

## 📝 Key Files Modified

### Completed
- ✅ `HibernateValueDAO.java` (2 injections fixed)
- ✅ `HibernateNodeDAO.java` (4 injections fixed)
- ✅ `PSWebResourcesRestService.java` (path validation added in Phase 1)

### Pending
- 📋 `PSActivityService.java` (JCR query validation)
- 📋 `ItemRestServiceImpl.java` (JCR string concatenation)
- 📋 `PSCacheItem.java` (Class whitelist validation)
- 📋 `PSDtdTree.java` (Multiple fixes: deserialization, SSRF, XXE)
- 📋 `PSFileDownLoadJobRunner.java` (URL whitelist validation)
- 📋 `PSDTSStatusProvider.java` (URL whitelist validation)
- 📋 And 6+ more related files

---

## 📚 References

- [OWASP SQL Injection](https://owasp.org/www-community/attacks/SQL_Injection)
- [OWASP Deserialization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html)
- [OWASP SSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)
- [OWASP XXE Prevention](https://owasp.org/www-community/attacks/xslt/xsl_injection)
- [CWE-89: SQL Injection](https://cwe.mitre.org/data/definitions/89.html)
- [CWE-502: Deserialization RCE](https://cwe.mitre.org/data/definitions/502.html)
- [CWE-918: SSRF](https://cwe.mitre.org/data/definitions/918.html)
