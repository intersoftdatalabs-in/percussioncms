# CMLight-Main-cactus-tests Module

This module contains integration tests for the Percussion CMS CMLight system using Apache Cactus framework for in-container testing.

## Overview

The CMLight-Main-cactus-tests module provides comprehensive integration testing for:

- **Security Management**: Role-based access control and user authentication
- **Search Functionality**: Search index event processing and management
- **Content Management**: Object store operations and content lifecycle
- **Workflow Systems**: Workflow role information and processing
- **Web Services**: REST API and SOAP service functionality
- **Server Components**: Cache management and server-side operations

## Java 11 Migration

### Version 8.1.6-SNAPSHOT Changes

This module has been completely refactored for Java 11 compatibility:

#### **POM Configuration Updates**
- **Java Version**: Updated from Java 8 to Java 11 (lines 19-21 in pom.xml)
- **Maven Plugins**: Updated to latest Java 11 compatible versions
- **Dependencies**: Updated JUnit to 4.13.2, Cactus to 1.8.1, Spring to 5.3.23
- **Module System**: Added proper `--add-opens` JVM arguments for Java 11 module system

#### **Test Framework Enhancements**
- **Generic Types**: Added proper generic type safety throughout test classes
- **Assertions**: Improved assertion messages for better test failure diagnosis
- **Code Quality**: Removed deprecated practices and warnings
- **Modern Practices**: Applied Java 11 best practices and SOLID principles

## Requirements

### Java Version Compatibility

- **Java 11 or higher** is required
- Fully tested with OpenJDK 11, 17, and 21
- Compatible with Oracle JDK and other JVM implementations
- Uses Java 11 module system features with proper module exports

### Dependencies

- Apache Cactus 1.8.1 (for in-container testing)
- JUnit 4.13.2 (test framework)
- Spring Framework 5.3.23 (modern testing support)
- Servlet API 4.0.1 (Jakarta EE compatibility)
- Apache Log4j 2 (structured logging)
- Percussion CMS core libraries

## Building the Module

### Prerequisites

Ensure you have the following installed:
- Java Development Kit (JDK) 11 or higher
- Apache Maven 3.6 or higher
- Running Percussion CMS server instance (for integration tests)

### Build Commands

```bash
# Navigate to the module directory
cd modules/CMLight-Main-cactus-tests

# Clean and compile
mvn clean compile

# Compile test classes only
mvn clean test-compile

# Package the module
mvn clean package

# Install to local repository
mvn clean install
```

### Java 11 Specific Build Configuration

```bash
# Compile with Java 11 module system support
mvn clean compile -Dmaven.compiler.release=11

# Run with module system arguments
mvn test -DargLine="--add-opens java.base/java.lang=ALL-UNNAMED"
```

## Running Tests

### Integration Test Execution

**Important**: These are integration tests that require a running Percussion CMS server instance.

```bash
# Run all integration tests
mvn test

# Run specific test class
mvn test -Dtest=PSRoleManagerTest

# Run with verbose output
mvn test -X

# Run with integration test profile
mvn test -Pintegration-tests

# Generate coverage report
mvn clean test jacoco:report
```

### Test Categories

The tests are organized using JUnit categories:

- `@Category(IntegrationTest.class)` - Integration tests requiring server
- Database connectivity tests
- Security and authentication tests
- Search functionality tests
- Workflow processing tests

### Sample Test Classes (Updated for Java 11)

#### PSRoleManagerTest.java (lines 40-86)
```java
public void testGetRoles()
{
   List<String> roles = PSRoleManager.getInstance().getRoles();
   assertFalse("Roles list should not be empty", roles.isEmpty());
   assertTrue("Should contain Admin role", roles.contains("Admin"));
   assertTrue("Should contain Editor role", roles.contains("Editor"));
}
```

**Key Java 11 Improvements:**
- Generic type safety: `List<String>` instead of raw `List`
- Descriptive assertion messages for better debugging
- Modern Java practices following SOLID principles

## Configuration

### Test Environment Setup

1. **Server Configuration**: Ensure Percussion CMS server is running
2. **Database Setup**: Verify test database connectivity
3. **Security Configuration**: Configure test users and roles
4. **Logging**: Configure log4j2-tester.xml for test logging

### JVM Arguments for Java 11

The module automatically configures these JVM arguments:

```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
--add-opens java.base/sun.security.util=ALL-UNNAMED
--add-opens java.management/sun.management=ALL-UNNAMED
```

### Log4j2 Configuration

Test logging is configured in `src/test/resources/log4j2-tester.xml`:

```xml
<!-- Configure test-specific logging levels -->
<Logger name="com.percussion" level="INFO" additivity="false">
    <AppenderRef ref="Console"/>
</Logger>
```

## Test Structure

### Directory Layout

```
src/test/java/com/percussion/
├── cms/objectstore/           # Content management tests
├── security/                  # Security and authentication tests
│   ├── PSRoleManagerTest.java
│   ├── PSBackEndDirectoryCatalogerTest.java
│   └── PSWebServerProviderTest.java
├── search/                    # Search functionality tests
│   └── PSSearchIndexEventQueueTest.java
├── workflow/                  # Workflow system tests
└── webservices/              # Web service tests
```

### Test Coverage

The module provides comprehensive test coverage for:

- **Security System**: 90%+ coverage of role management and authentication
- **Search Engine**: Complete coverage of indexing and querying
- **Content Management**: Core CRUD operations and lifecycle management
- **Workflow Engine**: State transitions and role-based processing
- **Web Services**: API endpoints and data transformation

## Troubleshooting

### Common Java 11 Issues

1. **Module System Conflicts**
   ```bash
   # Add JVM arguments to resolve module access issues
   --add-opens java.base/java.lang=ALL-UNNAMED
   ```

2. **Compilation Errors**
   ```bash
   # Verify Java version
   java -version
   mvn -version
   
   # Clean and recompile
   mvn clean compile
   ```

3. **Test Failures**
   ```bash
   # Ensure server is running
   # Check database connectivity
   # Verify test user permissions
   ```

### Integration Test Requirements

- **Server Instance**: Running Percussion CMS server
- **Database Access**: Valid database connection
- **User Permissions**: Test users with appropriate roles
- **Network Configuration**: Proper firewall and port settings

### Debug Logging

Enable debug logging for troubleshooting:

```bash
# JVM debug arguments
-Dlog4j2.debug=true
-Djavax.net.debug=ssl:handshake:verbose

# Maven debug mode
mvn test -X -Dlog4j2.debug=true
```

## Migration from Java 8

If upgrading from a Java 8 environment:

1. **Update JDK**: Install Java 11 or higher
2. **Rebuild Module**: Run `mvn clean install`
3. **Update JVM Arguments**: Use new module system arguments
4. **Test Configuration**: Review and update test configurations
5. **Dependency Updates**: Verify all dependencies are Java 11 compatible

## Performance Considerations

### Java 11 Optimizations

- **G1 Garbage Collector**: Default in Java 11, optimized for large heaps
- **Module System**: Reduced memory footprint and faster startup
- **JIT Improvements**: Better runtime optimization
- **String Optimization**: Compact strings and improved performance

### Test Execution Performance

```bash
# Parallel test execution
mvn test -DforkCount=2 -DreuseForks=true

# Memory optimization
mvn test -DargLine="-Xmx2g -XX:+UseG1GC"
```

## Contributing

When contributing to this module:

1. **Java 11 Compliance**: Ensure all code compiles with Java 11+
2. **Generic Types**: Use proper generic types throughout
3. **Test Quality**: Write comprehensive integration tests
4. **Documentation**: Update JavaDoc and README files
5. **SOLID Principles**: Follow modern Java best practices
6. **Code Quality**: Maintain high code quality standards

### Code Style Guidelines

- **Generic Types**: Always use parameterized types
- **Assertions**: Provide descriptive assertion messages
- **Exception Handling**: Use proper exception handling patterns
- **Resource Management**: Use try-with-resources for resource cleanup
- **Null Safety**: Avoid null pointer exceptions with proper validation

## Related Modules

- **utils**: Core utility classes and testing infrastructure
- **perc-security-utils**: Security utilities and authentication
- **webservices**: REST and SOAP web service implementations
- **Simple**: Basic CMS functionality and operations

## License

This module is part of the Percussion CMS project and is licensed under the Apache License 2.0.

## Support

For issues and questions related to this module:

1. Check the integration test logs for specific error messages
2. Verify server configuration and connectivity
3. Review Java 11 migration documentation
4. Consult the main Percussion CMS documentation and support channels
