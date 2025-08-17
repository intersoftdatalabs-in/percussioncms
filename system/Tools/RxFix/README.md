# RxFix - Rhythmyx Database Repair and Verification Tool

## Overview

RxFix is a comprehensive database repair and verification framework for Rhythmyx CMS installations. It provides tools to identify, diagnose, and fix common database integrity issues that can occur in production environments.

## Java 17 Modernization

This module has been modernized to leverage Java 17 features and best practices:

### Key Improvements

- **var keyword**: Local variable type inference for cleaner code
- **Enhanced Collections**: Use of `List.of()`, `Set.of()` for immutable collections
- **Stream API**: Modern collection processing and functional programming
- **Optional**: Null-safe operations and better error handling
- **Try-with-resources**: Enhanced resource management
- **Text blocks**: Improved string literals for better readability

### Refactored Classes

- ✅ **PSRxFix.java** - Main framework class with Java 17 features
- ✅ **PSVerify.java** - Installation verification tool with modern patterns
- 🔄 **PSFixBase.java** - Abstract base class for all fixes (in progress)
- 🔄 **Database fix implementations** - Individual fix modules (in progress)

## Architecture

### Core Components

#### 1. PSRxFix

The main framework class that orchestrates database repair operations:
- Manages a collection of fix modules
- Supports preview and execution modes
- Provides UI integration for administrative interfaces

#### 2. PSVerify

Installation verification tool that:
- Generates bills of materials (BOM) for installations
- Verifies installations against existing BOMs
- Checks database integrity and file consistency

#### 3. Database Fix Modules

Individual repair modules located in `dbfixes/` package:
- **PSFixBrokenRelationships** - Removes orphaned relationship records
- **PSFixContentStatusHistory** - Repairs content status inconsistencies
- **PSFixOrphanedSlots** - Cleans up orphaned slot references
- **PSFixAcls** - Repairs access control list inconsistencies
- **And many more...**

### Design Patterns Used

- **Strategy Pattern**: Individual fix modules implement `IPSFix` interface
- **Template Method**: `PSFixBase` provides common functionality
- **Factory Pattern**: Dynamic instantiation of fix modules
- **Observer Pattern**: Result logging and status reporting

## Usage

### Command Line Usage

#### Generate Installation BOM

```bash
java -cp rxfix.jar com.percussion.rxverify.PSVerify \
  -generate \
  -rxroot /opt/rhythmyx \
  -bomfile installation.bom
```

#### Verify Installation

```bash
java -cp rxfix.jar com.percussion.rxverify.PSVerify \
  -rxroot /opt/rhythmyx \
  -bomfile installation.bom
```

#### Run Database Fixes

```bash
java -cp rxfix.jar com.percussion.rxfix.PSRxFixCmd \
  -preview \
  -rxroot /opt/rhythmyx
```

### Programmatic Usage

#### Basic Fix Execution

```java
// Create and configure the fix framework
var rxFix = new PSRxFix();

// Preview mode - see what would be fixed
rxFix.doFix(true);

// Get preview results
var entries = rxFix.getRunentries();
entries.forEach(entry -> {
    var results = entry.getResults();
    if (results != null && !results.isEmpty()) {
        System.out.println("Fix: " + entry.getFixname());
        results.forEach(result -> 
            System.out.println("  " + result.getStatus() + ": " + result.getMessage())
        );
    }
});

// Execute actual fixes
rxFix.doFix(false);
```

#### Custom Fix Implementation

```java
public class MyCustomFix extends PSFixBase {
    @Override
    public String getOperation() {
        return "Fix Custom Database Issue";
    }
    
    @Override
    public void fix(boolean preview) throws Exception {
        // Implementation using Java 17 features
        var connection = getConnection();
        var issues = findIssues();
        
        issues.stream()
            .filter(issue -> shouldFix(issue))
            .forEach(issue -> {
                if (preview) {
                    logPreview(issue.getId(), "Would fix: " + issue.getDescription());
                } else {
                    fixIssue(issue);
                    logSuccess(issue.getId(), "Fixed: " + issue.getDescription());
                }
            });
    }
}
```

## Configuration

### System Properties

- `log4j.configuration` - Logging configuration file
- `javax.xml.parsers.SAXParserFactory` - XML parser implementation

### Database Connection

The framework uses JNDI to obtain database connections. Ensure your application server is configured with the appropriate data source.

## Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PSRxFixTest
```

### Integration Tests

Integration tests require a running Rhythmyx installation and database.

## Error Handling

The framework provides comprehensive error handling:
- **Preview Mode**: Safe execution that shows what would be changed
- **Graceful Degradation**: Individual fix failures don't stop the entire process
- **Detailed Logging**: Comprehensive logging of all operations and results
- **Result Status**: Each operation returns detailed status information

## Performance Considerations

- **Batch Processing**: Database operations are batched for efficiency
- **Connection Pooling**: Reuses database connections where possible
- **Memory Management**: Large result sets are processed in chunks
- **Caching**: Results are cached to avoid redundant operations

## Security

- **SQL Injection Prevention**: All queries use prepared statements
- **Access Control**: Operations require appropriate administrative privileges
- **Audit Trail**: All operations are logged for compliance

## Dependencies

### Core Dependencies

- Java 17+
- Spring Framework
- Apache Commons Lang3
- Log4j2
- JDBC drivers for supported databases

### Build Dependencies

- Maven 3.6+
- JUnit 5 (for testing)

## Contributing

When adding new fix modules:

1. Extend `PSFixBase` or `PSFixDBBase`
2. Implement the `fix(boolean preview)` method
3. Use Java 17 features for modern, readable code
4. Add comprehensive logging
5. Include unit tests
6. Update the fix registration in `PSRxFix.java`

### Code Style

- Follow Google Java Style Guide
- Use Java 17 features where appropriate
- Maintain backward compatibility for public APIs
- Document all public methods with Javadoc

## License

Copyright 1999-2023 Percussion Software, Inc.
Licensed under the Apache License, Version 2.0

## Support

For issues and questions:
- Check the logs for detailed error information
- Review the RXVERIFYREADME and RXVERIFYTABLEREADME files
- Contact the development team for complex issues

---

> 🤠 "Refactor today, so we don't have to rewrite tomorrow!" - Sunny Sal the Senior Java Developer

