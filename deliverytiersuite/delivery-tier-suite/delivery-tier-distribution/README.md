# delivery-tier-distribution

This module contains all the configuration files for DTS. For e.g.
* Log4j configurations
* Script files for installing DTS as service
* DB configurations
* Spring Security Configurations
* Email Configurations
* DTS tomcat configurations
etc...

## Java 11 Refactoring Status

✅ **Fully refactored to Java 11** (August 4, 2025)

### Refactored Classes:

- `MainDTSPreInstall.java` - Java 11 features including var, lambda expressions, improved string handling
- `AntJobFailedException.java` - Updated formatting and copyright

### Key Improvements:

- Applied modern Java 11 var keyword for better type inference
- Used lambda expressions for cleaner, more readable code
- Improved string comparisons and null checks
- Enhanced code formatting and consistency
- Removed unused dependencies (Apache Axis StringUtils)

## Build

```bash
mvn clean install
```

## Migration Notes

The refactored code maintains full backward compatibility. All public APIs remain unchanged.
No client code changes are required when upgrading to this version.

