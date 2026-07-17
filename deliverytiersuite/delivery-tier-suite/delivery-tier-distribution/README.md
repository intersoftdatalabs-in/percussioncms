# delivery-tier-distribution

This module contains all the configuration files for DTS. For e.g.
* Log4j configurations
* script files for installing DTS as service
* DB configurations
* Spring Security Configurations
* Email Configurations
* DTS tomcat configurations
etc...

## Java 17 Refactoring Status

✅ **Fully refactored to Java 17** (August 4, 2025)

### Refactored Classes:

- `MainDTSPreInstall.java` - Java 17 features including var, lambda expressions, improved string handling
- `AntJobFailedException.java` - Updated formatting and copyright

### Key Improvements:

- Applied modern Java 17 var keyword for better type inference
- Used lambda expressions for cleaner, more readable code
- Improved string comparisons and null checks
- Enhanced code formatting and consistency
- Removed unused dependencies (Apache Axis StringUtils)

## Build

```bash
./mvn-env.sh -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am clean install
# Windows: mvn-env.bat -pl deliverytiersuite/delivery-tier-suite/delivery-tier-distribution -am clean install
```

## Installer jar (`java -jar`)

The package artifact `target/delivery-tier-distribution.jar` is launched with:

```bash
java -jar delivery-tier-distribution.jar <install-or-upgrade-folder>
```

`MainDTSPreInstall` validates Zip entry paths with
`com.percussion.security.validation.PathValidation` (CWE-22 / ZipSlip). That class
lives in `perc-security-utils` and is **not** on a thin jar classpath when using
`java -jar`.

**GH-1180:** package runs a **minimal** `maven-shade-plugin` step that merges only:

| Artifact | What is included |
|----------|------------------|
| `com.percussion:perc-security-utils` | `PathValidation` + nested types only |
| `org.apache.logging.log4j:log4j-api` | Required by `PathValidation`'s logger |

This is intentionally **not** a full `jar-with-dependencies` (unlike
`perc-distribution-tree`), so wars/Tomcat/Spring stay out of the installer jar.

Verify phase fails the build if `PathValidation.class` or `LogManager.class` is
missing from the packaged jar (`verify-pathvalidation-shaded` antrun).

