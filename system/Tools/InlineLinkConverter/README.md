# Inline Link Converter Tool

## Overview

The Inline Link Converter Tool is a Java 11 modernized utility for processing and converting inline links in Rhythmyx CMS content items. This tool provides batch processing capabilities with XSL-based transformations and comprehensive workflow state management.

## Key Features

- **Batch Processing**: Convert inline links across multiple content items efficiently
- **XSL Transformations**: Apply custom XSLT stylesheets for content transformation
- **Workflow Management**: Automatically handle content item checkout/checkin and state transitions
- **Site Filtering**: Optional filtering by site root folder hierarchy
- **Comprehensive Logging**: Detailed logging of conversion progress, successes, and failures
- **Java 11 Modernized**: Utilizes modern Java features for improved performance and maintainability

## Project Structure

```
InlineLinkConverter/
├── README.md                               # This file
├── makefile                                # Build configuration
├── *.properties                           # Configuration files
├── *.xsl                                  # XSL transformation files
├── *.bat                                  # Windows batch scripts
└── src/
    └── com/percussion/inlinelinkconverter/
        ├── PSInlineLinkConverter.java      # Main converter class
        └── PSInlineLinkClearAttribs.java   # Extended converter with site filtering
```

## Prerequisites

- **Java 11** or higher
- **Rhythmyx CMS** server access
- Appropriate permissions for content item modification
- Maven 3.6+ (for building)

## Configuration

### Properties Files

#### inlinelinkconverter.properties
Main configuration file containing server connection details and conversion settings:

```properties
# Server connection
hostName=localhost
port=9992
serverRoot=Rhythmyx
loginId=admin
password=demo

# Community and content type settings
communityId=1001
contentType=MyContentType

# Optional: Specific content IDs to convert (comma-separated)
contentId=301,302,303

# Workflow transitions (semicolon-separated: publicToEdit;editToPublic)
MyWorkflow=DirectToPublic;QuickEdit
```

#### InlineLinkClearAttribs.properties
Extended configuration for site-filtered conversion:

```properties
# All settings from inlinelinkconverter.properties plus:
siteRoot=/Sites/MySite
```

### XSL Transformation Files

- **inlinelinkconverter.xsl**: Main transformation stylesheet
- **InlineLinkClearAttribs.xsl**: Stylesheet for attribute clearing
- **inlinelinkconverter_remove_sys_folderid.xsl**: Remove system folder ID attributes

## Usage

### Basic Conversion

```bash
# Compile the project
javac -cp "lib/*" src/com/percussion/inlinelinkconverter/*.java

# Run the main converter
java -cp ".:lib/*" com.percussion.inlinelinkconverter.PSInlineLinkConverter
```

### Site-Filtered Conversion

```bash
# Run the site-filtered converter
java -cp ".:lib/*" com.percussion.inlinelinkconverter.PSInlineLinkClearAttribs
```

### Maven Build

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Package
mvn package
```

## Java 11 Modernization Features

This codebase has been modernized to leverage Java 11 features:

### Language Features
- **`var` keyword**: Local variable type inference for cleaner code
- **Enhanced for-loops**: Replace iterator-based loops where possible
- **Try-with-resources**: Automatic resource management for file operations
- **Optional**: Null-safe operations and cleaner conditional logic

### Stream API
- **Method references**: `this::convertTypeSafely`
- **Stream operations**: Functional-style collection processing
- **Filtering and mapping**: Elegant data transformation pipelines

### Modern Practices
- **Objects.requireNonNull()**: Consistent null validation
- **String.format()**: Type-safe string formatting
- **LocalDateTime**: Modern date/time handling
- **Generics**: Type-safe collections throughout

## Logging

The tool generates three log files:

- **convert.log**: Complete conversion log with all activities
- **convert_success.log**: Successfully converted items (ID, revision)
- **convert_fail.log**: Failed conversion attempts with reasons

## Error Handling

The modernized version includes:

- **Comprehensive exception handling** with proper logging
- **Graceful degradation** for network or server issues
- **Detailed error reporting** with context information
- **Automatic resource cleanup** via try-with-resources

## Configuration Examples

### Workflow Transitions

Format: `WorkflowName=publicToEditTransition;editToPublicTransition`

```properties
# Simple workflow
StandardWorkflow=QuickEdit;DirectToPublic

# Multi-step workflow  
ComplexWorkflow=Edit,Review;Approve,Publish
```

### Content Type Filtering

```properties
# Convert specific content type
contentType=Article

# Convert all content types (leave empty)
contentType=
```

### Site Root Filtering

```properties
# Only convert items under this site path
siteRoot=/Sites/Corporate
```

## API Compatibility

The refactored code maintains **full backward compatibility** with existing:

- **Public methods and interfaces**
- **Configuration file formats**
- **Command-line usage patterns**
- **Integration points**

## Best Practices

### Before Running
1. **Backup your content** - Always backup before bulk operations
2. **Test on staging** - Verify transformations in non-production environment
3. **Review XSL files** - Ensure transformations meet requirements
4. **Check permissions** - Verify user has necessary content modification rights

### During Operation
1. **Monitor logs** - Watch conversion progress in real-time
2. **Check disk space** - Ensure adequate space for log files
3. **Network stability** - Maintain stable connection to CMS server

### After Completion
1. **Review logs** - Check success/failure ratios
2. **Verify results** - Spot-check converted content
3. **Archive logs** - Save conversion logs for audit trail

## Troubleshooting

### Common Issues

**Connection Refused**
```
Solution: Verify hostName, port, and serverRoot in properties file
```

**Authentication Failed**
```
Solution: Check loginId and password credentials
```

**Content Type Not Found**
```
Solution: Verify contentType name matches exactly (case-sensitive)
```

**Workflow Transition Failed**
```
Solution: Check workflow transition names in properties file
```

### Debug Mode

Enable detailed logging by setting log level to DEBUG in your logging configuration.

## Contributing

When contributing to this modernized codebase:

1. **Follow Java 11 practices** - Use modern language features appropriately
2. **Maintain compatibility** - Don't break existing public APIs
3. **Add comprehensive tests** - Include unit tests for new functionality
4. **Update documentation** - Keep README and Javadoc current
5. **Follow Google Java Style** - Consistent code formatting

## License

Copyright 1999-2023 Percussion Software, Inc.

Licensed under the Apache License, Version 2.0. See LICENSE file for details.

## Support

For technical support and questions:

- Review the conversion logs for specific error details
- Check Rhythmyx CMS server status and connectivity
- Verify configuration file syntax and values
- Consult Percussion CMS documentation for workflow and content type details
