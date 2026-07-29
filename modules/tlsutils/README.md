# TLS Utils Module

The TLS Utils module provides utilities for TLS/SSL certificate management and trust manager functionality within the Percussion CMS system.

## Overview

This module contains three main components:

- **TLSUtils**: Utility class for converting X.509 certificates to PEM format
- **WrappedTrustManager**: A custom trust manager that wraps multiple trust managers for enhanced certificate validation
- **TLSTester**: Utility class for testing TLS connections and analyzing SSL/TLS configurations

## Features

- X.509 certificate conversion to PEM format
- Multi-trust manager support for flexible certificate validation
- TLS connection testing and diagnostics
- SSL cipher suite enumeration
- Operating system detection utilities
- Keystore management utilities
- Comprehensive logging with log4j2 integration

## Requirements

### Java Version Compatibility

- **Java 17 or higher** is required (updated from Java 8)
- Fully tested and compatible with Java 17, 17, and 21
- Compatible with OpenJDK and Oracle JDK implementations
- Uses Java 17 module system features with proper module exports

### Dependencies

- Apache Commons Codec (for Base64 encoding)
- Apache Log4j 2 (for structured logging)
- Percussion CMS core libraries
- JUnit 4.13.2 (for testing)
- Mockito 4.11.0 (for mocking in tests)

## Building the Project

### Prerequisites

Ensure you have the following installed:
- Java Development Kit (JDK) 11 or higher
- Apache Maven 3.6 or higher

### Build Commands

```bash
# Navigate to the tlsutils module directory
cd modules/tlsutils

# Clean and compile the project (use wrapper so Maven runs with JDK 21)
./mvnw clean compile

# Package the module (creates JAR file)
./mvnw clean package

# Install to local Maven repository
./mvnw clean install
```

### Build from Parent Project

You can also build this module as part of the entire Percussion CMS build:

```bash
# From the root directory of percussioncms
mvn clean install -pl modules/tlsutils

# Or build all modules
mvn clean install
```

### Java 17 Specific Build Configuration

The module includes Java 17 specific configuration:

```bash
# Compile with Java 17 module system support
mvn clean compile -Dmaven.compiler.release=11

# Run tests with module system arguments
mvn test -DargLine="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.security.ssl=ALL-UNNAMED"
```

## Running Tests

The module includes comprehensive unit tests with 100% code coverage.

### Test Execution

```bash
# Run all tests
mvn test

# Run tests with verbose output
mvn test -X

# Run a specific test class
mvn test -Dtest=TLSUtilsTest

# Run tests and generate coverage report
mvn clean test jacoco:report

# Run tests with Java 17 module system support
mvn test -DargLine="--add-opens java.base/java.lang=ALL-UNNAMED"
```

### Test Coverage

The test suite covers:
- Certificate conversion functionality
- Trust manager operations with various scenarios
- Error handling and edge cases
- Console output verification (now using log4j2)
- Mock object interactions
- Java 17 specific features and compatibility

### Test Classes

- **TLSUtilsTest**: Tests for certificate conversion utilities
- **WrappedTrustManagerTest**: Tests for custom trust manager functionality
- **TLSTesterTest**: Tests for TLS testing and utility methods

## Recent Updates

### Version 8.1.6-SNAPSHOT Changes

- **Java 17 Compatibility**: Complete refactoring for Java 17+ support
- **Logging Enhancement**: Replaced all System.out calls with proper log4j2 logging
- **Dependency Updates**: Updated to latest compatible versions (JUnit 4.13.2, Mockito 4.11.0)
- **Module System Support**: Added proper Java module system configuration
- **Code Quality**: Fixed all compilation warnings and improved code quality
- **Test Improvements**: Enhanced test coverage and reliability

## Usage Examples

### Converting Certificate to PEM Format

```java
import com.percussion.tls.TLSUtils;
import java.security.cert.X509Certificate;

// Convert an X.509 certificate to PEM format
X509Certificate cert = // ... obtain certificate
String pemFormat = TLSUtils.convertToPem(cert);
System.out.println(pemFormat);
```

### Using WrappedTrustManager

```java
import com.percussion.tls.WrappedTrustManager;
import java.security.KeyStore;

// Create a wrapped trust manager
WrappedTrustManager trustManager = new WrappedTrustManager();

// Add additional keystores
KeyStore customKeyStore = // ... load your keystore
trustManager.addKeyStore("Custom Store", customKeyStore);

// Use in SSL context
SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(null, new TrustManager[]{trustManager}, null);
```

### TLS Testing

```java
import com.percussion.tls.TLSTester;

// Test TLS connection to a host
String[] args = {"example.com", "443"};
TLSTester.main(args);

// Check OS compatibility
if (TLSTester.isWindows()) {
    System.out.println("Running on Windows");
}

// Get enabled cipher suites (now logs via log4j2)
TLSTester.getEnabledCiphers();
```

## Configuration

### System Properties

The module respects standard Java system properties for SSL/TLS configuration:

- `javax.net.ssl.trustStore`: Path to trust store
- `javax.net.ssl.trustStorePassword`: Trust store password
- `javax.net.ssl.keyStore`: Path to key store
- `javax.net.ssl.keyStorePassword`: Key store password

### Default Keystore Password

The module uses the default Java keystore password: `changeit`

### Logging Configuration

The module now uses log4j2 for all logging output. Configure logging levels:

```xml
<!-- log4j2.xml configuration example -->
<Logger name="com.percussion.tls" level="INFO" additivity="false">
    <AppenderRef ref="Console"/>
</Logger>
```

## Troubleshooting

### Common Issues

1. **Java 17 Module System Issues**
   - Add JVM arguments: `--add-opens java.base/java.lang=ALL-UNNAMED`
   - Ensure proper module path configuration
   - Check for illegal reflective access warnings
2. **Certificate Validation Failures**
   - Ensure proper trust store configuration
   - Check certificate validity and chain
   - Verify system time is correct
3. **Compilation Issues**
   - Verify Java 17+ is being used
   - Ensure all dependencies are available
   - Check Maven configuration and version
4. **Test Failures**
   - Some tests may require network connectivity
   - Firewall settings may affect SSL connection tests
   - Ensure proper JVM security policies for Java 17+

### Debug Logging

Enable debug logging for SSL/TLS operations:

```bash
# Java 17+ SSL debugging
-Djavax.net.debug=ssl:handshake:verbose

# Module system debugging
-Djava.base/java.lang=ALL-UNNAMED --illegal-access=debug
```

## Migration from Java 8

If upgrading from a Java 8 environment:

1. Update JDK to version 11 or higher
2. Rebuild the module with `mvn clean install`
3. Update JVM arguments for module system compatibility
4. Review log4j2 configuration for new logging output

## Contributing

When contributing to this module:

1. Maintain 100% test coverage
2. Follow existing code style and patterns
3. Add appropriate log4j2 logging instead of System.out
4. Test with Java 17+ and ensure module system compatibility
5. Ensure backward compatibility where possible
6. Document any Java 17+ specific features used

## License

This module is part of the Percussion CMS project and is licensed under the Apache License 2.0.

## Support

For issues and questions related to this module, please refer to the main Percussion CMS documentation and support channels.
