# PercussionCMS v8.1.6 Release Notes

## Overview
This release provides security updates and bug fixes for PercussionCMS while maintaining compatibility with Java 8 (JDK 1.8.0).

## Important Note
Several dependency updates were rolled back to maintain Java 8 compatibility. The versions listed below reflect what is actually deployed in this release.

## Dependency Updates

### Security & Core Libraries

- **OWASP CSRF Guard:** Updated to 4.5.0
- **Jackson:** Updated to 2.20.1
- **Apache Tika:** Updated to 2.9.4
- **JSON:** Updated to 20251224
- **Eclipse Jetty:** Updated to 9.4.58.v20250814
- **Apache CXF:** Updated to 3.5.11
- **Apache PDFBox:** Updated to 2.0.30
- **Apache FOP:** Updated to 2.11
- **Rome:** Updated to 2.1.0
- **ICU4J:** Updated to 77.1

### Java 8 Compatibility Maintenance

The following dependencies remain at Java 8-compatible versions:
- **Apache MyFaces:** 2.3.11 (3.x requires Java 11+)
- **Apache Shindig:** 1.1-BETA5-incubating (3.x requires Java 11+)

### Rolled Back Updates

The following dependency updates were attempted but rolled back for compatibility:
- Apache Commons Digester - Reverted to maintain stability
- Spotless Maven Plugin - Reverted to maintain compatibility
- Phloc Commons - Reverted to maintain compatibility
- javax.jcr - Reverted to maintain compatibility

## Requirements

- **Java Version:** JDK 1.8.0 (Java 8)
- All dependencies in this release are compatible with Java 8

## Build Information

- **Branch:** development-8.1.x
- **Commit:** 0a58214c1b6378f07dec0cad2c868c09c7da2cc9

---

For detailed information about specific PRs and version changes, see the [detailed correction document](../ai-generated/RELEASE_NOTES_v8.1.6_CORRECTED.md).
