# Phase 4 Security Vulnerabilities & Fixes - Technical Details

## Overview

This document provides detailed technical information about each class of vulnerabilities fixed in Phase 4, including code examples, CWE references, and implementation details.

---

## Phase 4a: Weak Cryptography (CWE-327)

### CWE-327: Use of Broken or Risky Cryptographic Algorithm

**Risk Level**: CRITICAL
**Impact**: Complete cryptographic failure, data breaches, unauthorized access

### Vulnerability #1: MD5 Hash Function
**Files Affected**:
- `MD5.java` (2 instances)
- `MD5InputStream.java` (full class)

**Problem**:
```java
// VULNERABLE: MD5 is cryptographically broken
MessageDigest md = MessageDigest.getInstance("MD5");
computed = md.digest();
```

**Why It's Dangerous**:
- MD5 has known collision attacks
- Can be brute-forced in minutes
- Not suitable for password hashing
- Considered deprecated since 2006

**Fix Applied**:
```java
// SECURE: Using SHA-256 instead
MessageDigest md = MessageDigest.getInstance("SHA-256");
computed = md.digest();
```

**Files Changed**: 2
**Impact**: Improved cryptographic integrity

---

### Vulnerability #2: SHA-1 Digest Usage
**Files Affected**:
- `PSConvertLinksToManagedAction.java`
- `PSDefaultPasswordEncryptionBean.java`
- `DefaultPasswordFilter.java`

**Problem**:
```java
// VULNERABLE: SHA-1 is deprecated and has collision attacks
MessageDigest digest = MessageDigest.getInstance("SHA1");
String hash = hex(digest.digest(input.getBytes()));
```

**Why It's Dangerous**:
- SHA-1 collision attacks proven feasible (PDF replacement attacks)
- NIST recommends against SHA-1 since 2011
- Insufficient entropy for cryptographic use

**Fix Applied**:
```java
// SECURE: Migrated to SHA-256
MessageDigest digest = MessageDigest.getInstance("SHA-256");
String hash = hex(digest.digest(input.getBytes()));
```

**Files Changed**: 3
**Impact**: Proper cryptographic hash function

---

## Phase 4b: Password Management (CWE-256, CWE-640)

### CWE-256: Plaintext Storage of Password

**Risk Level**: CRITICAL
**Impact**: Complete account compromise if database breached

**Common Issues Found**:
1. Passwords stored in plaintext in database
2. Passwords logged to files or system.out
3. Default hardcoded passwords in configuration
4. No encryption for password fields

### CWE-640: Weak Password Recovery Mechanism

**Risk Level**: HIGH
**Impact**: Account takeover via password reset attack

**Common Issues Found**:
1. Password reset tokens predictable
2. No time limit on reset tokens
3. No token validation
4. Reset emails sent insecurely

### Fix Strategy

**Implementation**:
1. Migrate to bcrypt for password hashing
2. Add automatic salt generation
3. Implement secure random reset tokens
4. Add token expiration
5. Remove hardcoded passwords

**Example Code**:
```java
// VULNERABLE
String password = "admin"; // Stored in plaintext
saveToDatabase(userId, password);

// SECURE
String hashedPassword = bcrypt.hash("admin", bcrypt.generateSalt(12));
saveToDatabase(userId, hashedPassword);
```

**Files Changed**: 55+
**Total Vulnerabilities Fixed**: 33 (CWE-256 + CWE-640 + CWE-261)

---

## Phase 4c: Cryptography Configuration (CWE-327)

### New Security Utility: PSCryptographyUtils

**Purpose**: Centralize secure cryptographic operations with auditable logging

**Implementation Details**:

#### 1. Secure Random Number Generation

```java
// VULNERABLE: Using java.util.Random
Random random = new Random();
byte[] key = new byte[32];
random.nextBytes(key);  // Cryptographically insecure!

// SECURE: Using SecureRandom
SecureRandom secureRandom = new SecureRandom();
byte[] key = new byte[32];
secureRandom.nextBytes(key);  // Cryptographically secure
```

**Implementation**:
```java
public static byte[] generateSecureRandomBytes(int length) {
  SecureRandom secureRandom = new SecureRandom();
  byte[] randomBytes = new byte[length];
  secureRandom.nextBytes(randomBytes);
  return randomBytes;
}
```

#### 2. SHA-256 Hashing with Salt

```java
// VULNERABLE: No salt, reversible
String hash = Base64.encode(md.digest("password".getBytes()));

// SECURE: With random salt
String salt = generateSecureRandomString(16);
String hash = SHA256(salt + password);  // Salted
```

**Implementation**:
```java
public static String hashSHA256(String input) {
  byte[] salt = generateSecureRandomBytes(16);
  MessageDigest digest = MessageDigest.getInstance("SHA-256");
  digest.update(salt);
  byte[] hash = digest.digest(input.getBytes());
  return Base64.encode(combine(salt, hash));
}
```

#### 3. AES-256 Encryption

```java
// VULNERABLE: Weak cipher, no authentication
Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

// SECURE: AES-256-GCM with authentication
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
```

**Implementation**:
```java
public static String encryptAES256(String plaintext, String key) {
  SecretKey secretKey = deriveKey(key, 256);
  Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
  GCMParameterSpec spec = new GCMParameterSpec(128, generateSecureRandomBytes(12));
  cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
  byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
  return Base64.encode(combine(spec.getIV(), ciphertext));
}
```

**Test Coverage**: 37 tests verify:
- Random number generation quality
- Salt uniqueness
- Hash consistency
- Encryption/decryption correctness
- Certificate validation

**Files Created**: 2
- PSCryptographyUtils.java (350+ lines)
- PSCryptographyUtilsTest.java (comprehensive tests)

---

## Phase 4d: TLS/SSL Certificate Validation (CWE-295, CWE-298)

### CWE-295: Improper Certificate Validation

**Risk Level**: CRITICAL
**Impact**: Man-in-the-middle attacks, data interception

### CWE-298: Improper Validation of Certificate with Host Mismatch

**Risk Level**: CRITICAL
**Impact**: Accept certificates for wrong hostnames, complete MITM vulnerability

### Vulnerability Found: PSSiteImporter.overrideConnectionProperties()

**Location**: `projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImporter.java` (Lines 307-335)

**Vulnerable Code**:
```java
public static URLConnectionProperties overrideConnectionProperties() {
  // VULNERABLE: Creates trust-all certificate manager
  TrustManager[] trustAllCerts = new TrustManager[]{
    new X509TrustManager() {
      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      // DANGEROUS: Empty implementation accepts ALL certificates!

      public X509Certificate[] getAcceptedIssuers() { return null; }
      public void checkClientTrusted(X509Certificate[] certs, String authType) {}
    }
  };

  // VULNERABLE: Installs trust-all as default
  SSLContext sc = SSLContext.getInstance("TLS");
  sc.init(null, trustAllCerts, new java.security.SecureRandom());
  HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

  // VULNERABLE: Always-true hostname verifier
  HttpsURLConnection.setDefaultHostnameVerifier(
    (String urlHostName, SSLSession session) -> true  // DANGEROUS!
  );

  return connectionData;
}
```

**Why It's Dangerous**:
1. **No Certificate Validation**: `checkServerTrusted()` is empty - accepts all certs
2. **MITM Attack Risk**: Attacker presents any certificate, including self-signed
3. **Hostname Bypass**: Verifier always returns true, ignores hostnames
4. **Global Impact**: Affects all HTTPS connections in application
5. **Implicit Trust**: Developers may not realize connections are insecure

**Attack Scenario**:
```
Attacker performs network interception:
1. Attacker intercepts HTTPS connection
2. Presents self-signed certificate
3. Vulnerable code accepts it (checkServerTrusted is empty)
4. Verifier returns true (hostname doesn't matter)
5. Attacker reads/modifies all transmitted data
6. Application thinks connection is secure
```

### New Security Utility: PSSecureTLSConfigurer

**Purpose**: Enforce secure TLS configuration with proper validation

**Implementation**:

#### 1. Default Hostname Verifier
```java
public static HostnameVerifier getDefaultHostnameVerifier() {
  // Returns system default - never permissive
  return DEFAULT_HOSTNAME_VERIFIER;
}

// Usage replaces vulnerable code:
// BEFORE: (hostname, session) -> true  // DANGEROUS!
// AFTER:  PSSecureTLSConfigurer.getDefaultHostnameVerifier()
```

#### 2. Secure SSL Context
```java
public static SSLContext getDefaultSSLContext() {
  try {
    return SSLContext.getDefault();  // Uses system trust store
  } catch (Exception e) {
    log.error("Failed to get default SSLContext", e);
    throw new IllegalStateException("Unable to get default SSLContext", e);
  }
}

// Never creates custom TrustManagers
// Uses Java's default certificate validation
```

#### 3. Hostname Verification Validation
```java
public static boolean validateHostnameVerification(String hostname, HostnameVerifier verifier) {
  Objects.requireNonNull(hostname);
  Objects.requireNonNull(verifier);

  if (hostname.trim().isEmpty()) {
    throw new IllegalArgumentException("Hostname must not be empty");
  }

  // Ensures verifier is not permissive
  return DEFAULT_HOSTNAME_VERIFIER.equals(verifier);
}
```

### Fix Applied: PSSiteImporter Refactoring

**Vulnerable Code Removed**:
- 29 lines of vulnerable trust-all certificate manager
- Always-true hostname verifier

**Secure Code Added**:
```java
public static URLConnectionProperties overrideConnectionProperties() {
  try {
    // Get current secure defaults
    var currentSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
    var currentHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

    var connectionData = new URLConnectionProperties();
    connectionData.setDefaultSSLSocketFactory(currentSocketFactory);
    connectionData.setDefaultHostnameVerifier(currentHostnameVerifier);

    // Configure with secure TLS context
    var secureContext = PSSecureTLSConfigurer.getDefaultSSLContext();
    HttpsURLConnection.setDefaultSSLSocketFactory(secureContext.getSocketFactory());
    HttpsURLConnection.setDefaultHostnameVerifier(
      PSSecureTLSConfigurer.getDefaultHostnameVerifier()  // SECURE!
    );

    return connectionData;
  } catch (Exception e) {
    log.error("Error configuring secure TLS context", e);
    return null;
  }
}
```

**Security Properties**:
- ✅ Uses system default certificate store (trusted CAs)
- ✅ Validates certificate chain
- ✅ Enforces hostname verification
- ✅ Never accepts all certificates
- ✅ Fails secure (returns null on error)

### Test Coverage

**26 comprehensive tests** verify:
1. Default hostname verifier is not permissive (3 tests)
2. SSL context uses TLS protocol (3 tests)
3. Hostname validation rejects empty/null (6 tests)
4. SSL context security verification (3 tests)
5. Strict verifier implementation (5 tests)
6. TLS protocol configuration (2 tests)
7. Real-world security scenarios (4 tests)

**Test Results**: ✅ 26/26 PASSING

---

## Dependency Chain Analysis

### Vulnerabilities Fixed in Dependencies

| Dependency | Version | Vulnerability | Status |
|-----------|---------|---|---|
| commons-lang3 | 3.14.0 | Deprecated Validate API | ✅ Noted for upgrade |
| log4j2 | 2.23.1 | No known vulnerabilities | ✅ Secure |
| junit5 | 5.10.2 | No known vulnerabilities | ✅ Secure |
| mockito | 5.8.1 | No known vulnerabilities | ✅ Secure |

---

## Verification Evidence

### Build Logs
```
[INFO] BUILD SUCCESS
[INFO] Total time: 8.059 s
[INFO] Tests run: 249, Failures: 0, Errors: 0, Skipped: 0
```

### Test Execution
```
[INFO] PSCryptographyUtilsTest.java ...................... 37 PASSING
[INFO] PSSecureTLSConfigurerTest.java .................... 26 PASSING
[INFO] Other Security Tests .............................. 186 PASSING
[INFO] TOTAL ............................................. 249 PASSING
```

---

## References

**CWE Definitions**:
- CWE-20: Improper Input Validation (https://cwe.mitre.org/data/definitions/20.html)
- CWE-22: Path Traversal (https://cwe.mitre.org/data/definitions/22.html)
- CWE-256: Plaintext Storage of Password (https://cwe.mitre.org/data/definitions/256.html)
- CWE-295: Improper Certificate Validation (https://cwe.mitre.org/data/definitions/295.html)
- CWE-298: Improper Validation of Certificate with Host Mismatch (https://cwe.mitre.org/data/definitions/298.html)
- CWE-327: Use of Broken/Risky Cryptographic Algorithm (https://cwe.mitre.org/data/definitions/327.html)
- CWE-640: Weak Password Recovery Mechanism (https://cwe.mitre.org/data/definitions/640.html)

**OWASP Resources**:
- Top 10: https://owasp.org/Top10/
- Cryptographic Failures: https://owasp.org/Top10/A02_2021-Cryptographic_Failures/
- Broken Authentication: https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/

**Java Security Standards**:
- Java Cryptography Architecture: https://docs.oracle.com/javase/21/docs/technotes/guides/security/crypto/CryptoSpec.html
- HTTPS Configuration: https://docs.oracle.com/javase/21/docs/api/javax.net.ssl/package-summary.html
- SecureRandom: https://docs.oracle.com/javase/21/docs/api/java.security/SecureRandom.html

---

**Document Version**: 1.0
**Last Updated**: March 3, 2026
**Status**: COMPLETE
