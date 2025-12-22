# Delivery Tier Comments Module

## Overview

The Delivery Tier Comments Module provides REST API endpoints for comment management in Percussion CMS delivery environments. This module has been refactored for Java 17 and Jakarta EE compatibility with enhanced security features.

## Recent Changes (Java 17 Refactoring)

### API Updates

- **PSCommentsRestService**: Modernized with Java 17 features including `var`, `Optional`, and Streams
- **Jakarta Migration**: Updated from `javax.*` to `jakarta.*` namespace for servlet and JAX-RS APIs
- **Enhanced Error Handling**: Improved exception handling with OWASP-compliant logging
- **Security Improvements**: Enhanced CSRF protection, input validation, and anti-bot measures

### Breaking Changes

- Migrated from `javax.servlet.http.*` to `jakarta.servlet.http.*`
- Updated JAX-RS imports to Jakarta namespace
- Enhanced parameter validation and error responses
- Improved logging with Log4j 2.x API
- Stricter input validation with length limits and XSS prevention

### New Security Features ⚡

- **Input Sanitization**: Automatic HTML encoding to prevent XSS attacks
- **Comprehensive Validation**: Email format, content length, and suspicious pattern detection
- **Open Redirect Prevention**: Referer URL validation to prevent redirect attacks
- **Enhanced Honeypot Protection**: Improved bot detection mechanisms
- **OWASP Compliance**: All security practices follow OWASP guidelines

### New Features

- **Modern Date Handling**: Uses `ZonedDateTime` and `DateTimeFormatter` for better timezone support
- **Stream-based Processing**: Leverages Java Streams for collection operations
- **Optional Safety**: Uses `Optional` for null-safe operations
- **Enhanced Validation**: Comprehensive input validation with descriptive error messages
- **Security Constants**: Centralized form parameter constants for maintainability

## Architecture

```
delivery-tier-suite/comments/
├── src/main/java/
│   └── com/percussion/delivery/comments/
│       ├── services/          # REST endpoints and service layer
│       ├── data/             # Comment data models and DTOs
│       └── exceptions/       # Custom exception handling
├── src/test/java/            # JUnit 5 tests with security scenarios
└── src/main/resources/       # Configuration files
```

## Security Features 🔒

### Input Validation & Sanitization

- **XSS Prevention**: Automatic HTML encoding of user input
- **Content Length Limits**:
  - Comments: 5,000 characters max
  - Usernames: 100 characters max
  - Emails: 254 characters max (RFC 5321 compliant)
- **Suspicious Pattern Detection**: Blocks common XSS patterns
- **Email Format Validation**: RFC-compliant email validation

### CSRF Protection

- Automatic CSRF token handling via cookies
- Custom headers for token validation
- Integration with Spring Security

### Anti-Bot Measures

- **Honeypot Field Detection**: Hidden fields to catch automated submissions
- **Form Parameter Validation**: Comprehensive validation of all form inputs
- **Referer Checking**: Validates redirect URLs to prevent open redirects

### OWASP Compliance

- No sensitive data in logs
- Parameterized queries prevent SQL injection
- Input validation prevents XSS attacks
- Secure redirect handling

## Key Components

### PSCommentsRestService

REST controller providing:
- Comment CRUD operations with enhanced validation
- JSONP support for cross-origin requests
- Moderation endpoints for administrative functions
- Site management and bulk operations
- Comprehensive security features

### Validation Constants

```java
// Security constraints
private static final int MAX_COMMENT_LENGTH = 5000;
private static final int MAX_USERNAME_LENGTH = 100;
private static final int MAX_EMAIL_LENGTH = 254;

// Form parameter constants
private static final String FORM_PARAM_HONEYPOT = "url";
```

## Usage Examples

### Secure Comment Submission

```html
<form action="/Rhythmyx/services/delivery/comment/addcomment" method="POST">
    <input type="hidden" name="site" value="example-site" />
    <input type="hidden" name="pagePath" value="/articles/sample" />
    <input type="text" name="username" placeholder="Your Name" maxlength="100" required />
    <input type="email" name="email" placeholder="Email (optional)" maxlength="254" />
    <textarea name="text" placeholder="Your comment" maxlength="5000" required></textarea>
    <!-- Honeypot field for bot protection (must remain hidden and empty) -->
    <input type="text" name="url" style="display:none;" />
    <button type="submit">Submit Comment</button>
</form>
```

### Enhanced Error Handling

```java
try {
    var comments = commentsRestService.getComments(criteria);
} catch (WebApplicationException e) {
    if (e.getResponse().getStatus() == 400) {
        // Handle validation errors
        log.warn("Invalid input: {}", e.getMessage());
    } else {
        // Handle other errors
        log.error("Service error: {}", e.getMessage());
    }
}
```

### Security Testing

```java
@ParameterizedTest
@ValueSource(strings = {"<script>", "javascript:", "onclick="})
void testXssProtection(String maliciousInput) {
    // Test automatically validates XSS protection
    assertThrows(WebApplicationException.class, 
        () -> submitCommentWithText(maliciousInput));
}
```

## Configuration

### Required Dependencies

```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>5.0.0</version>
</dependency>
<dependency>
    <groupId>jakarta.ws.rs</groupId>
    <artifactId>jakarta.ws.rs-api</artifactId>
    <version>3.1.0</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.12.0</version>
</dependency>
```

### Security Configuration

Configure domain validation for redirect protection:

```properties
# In application.properties
comments.allowed.domains=localhost,yourdomain.com
comments.max.comment.length=5000
comments.max.username.length=100
```

## Error Handling

### Validation Errors

- **Invalid Input**: Returns 400 Bad Request with detailed error messages
- **Security Violations**: Logs security alerts and returns appropriate error codes
- **XSS Attempts**: Blocked with descriptive error messages

### Response Codes

- `200 OK` - Successful operation
- `204 No Content` - Successful operation with no response body
- `400 Bad Request` - Invalid input parameters or security violations
- `403 Forbidden` - Security restrictions (e.g., detected bot)
- `500 Internal Server Error` - Server-side errors

## Testing 🧪

Run comprehensive security tests:

```bash
mvn test
```

Test coverage includes:
- **Security Tests**: XSS prevention, input validation, bot detection
- **Functional Tests**: REST endpoint functionality, JSONP validation
- **Edge Cases**: Boundary conditions, error scenarios
- **Performance Tests**: Large input handling, concurrent requests

### Key Test Scenarios

- XSS attack prevention
- Honeypot bot detection
- Input length validation
- Email format validation
- CSRF token handling
- Open redirect prevention

## Migration Notes

### Security Enhancements

1. **Input Validation**: All form inputs now undergo strict validation
2. **Length Limits**: Enforce new character limits in client applications
3. **XSS Protection**: Update any client-side validation to match server-side rules
4. **Bot Protection**: Ensure honeypot fields remain hidden and empty

### From Previous Versions

1. Update servlet imports from `javax.*` to `jakarta.*`
2. Handle new validation error responses in client code
3. Update logging framework to Log4j 2.x
4. Test all form submissions with new validation rules

## Performance Considerations

- **Efficient Validation**: Fast pattern matching for security checks
- **Stream Processing**: Optimized collection handling with Java Streams
- **Memory Management**: Proper resource cleanup and bounded input sizes
- **Caching**: Reusable formatters and validation patterns

## Dependencies

- Jakarta Servlet API 5.0+
- Jakarta JAX-RS API 3.1+
- Spring Framework 5.x+
- Apache Commons Lang3 3.12+
- Log4j 2.x
- Jersey/JAX-RS implementation
- JUnit 5 (testing)
- Mockito (testing)

