# Membership Services Module

This module contains all the backend support required by DTS (Delivery Tier Suite) for secure published pages which need Authentication and Authorization.

If a page is set to be secure, then this service takes care of Authentication/Authorization before DTS presents the page to the user.

## Features

- **User Authentication**: Login/logout functionality with session management
- **Account Management**: Create, update, and deactivate user accounts
- **Password Reset**: Secure password reset workflow with token-based verification
- **Session Management**: Active session tracking and validation
- **User Search**: Search and retrieve user information
- **Security**: Pre-authentication filtering and secure credential handling

## Technology Stack

- **Java 17** - Modern Java features including `var`, `Optional`, and `LocalDateTime`
- **Spring Boot 2.7+** - Application framework with auto-configuration
- **Spring Security 5.8** - Authentication and authorization
- **Spring Web MVC** - RESTful web services
- **Hibernate 5.6** - Object-relational mapping
- **Jackson 2.15** - JSON processing
- **JUnit 5** - Unit testing framework
- **Log4j 2** - Logging framework

## Architecture

### Core Components

- **PSMembershipApplication**: Main Spring Boot application class
- **PSPreAuthenticatedProcessingFilter**: Security filter for pre-authentication
- **IPSMembershipService**: Core service interface for membership operations
- **Data Classes**: Immutable DTOs with builder patterns and factory methods

### Data Layer

All data classes are **immutable** and follow modern Java 17 patterns:

- `PSMembershipAccount` - Core membership account information
- `PSLoginRequest/PSLoginResult` - Authentication request/response
- `PSUserSession` - Session management and tracking
- `PSAccountCreateResult` - Account creation results
- `PSResetRequest` - Password reset operations
- `PSUserSummary/PSAccountSummary` - Summary views for search and admin

### Service Layer

- **Authentication Services**: Handle login, logout, and session validation
- **Account Management**: Create, update, and manage user accounts
- **Password Management**: Secure password reset and validation
- **Search Services**: User lookup and search functionality

## API Endpoints

The membership service provides RESTful endpoints for:

- `POST /api/membership/authenticate` - User authentication
- `POST /api/membership/accounts` - Create new account
- `GET /api/membership/users/{id}` - Get user by ID
- `PUT /api/membership/accounts/{id}` - Update account
- `POST /api/membership/password-reset` - Initiate password reset
- `GET /api/membership/sessions/{sessionId}` - Validate session
- `DELETE /api/membership/sessions/{sessionId}` - Logout/invalidate session

## Building

### Prerequisites

- Java 17 or later
- Maven 3.6+
- Parent project dependencies

### Compile

```bash
mvn clean compile
```

### Test

```bash
mvn test
```

### Package

```bash
mvn clean package
```

### Full Build

```bash
mvn clean install
```

## Configuration

### Application Properties

Configure the following properties in your application:

```properties
# Database Configuration
spring.datasource.url=jdbc:derby:memory:membership;create=true
spring.datasource.driver-class-name=org.apache.derby.jdbc.EmbeddedDriver

# Session Configuration
server.servlet.session.timeout=30m
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true

# Security Configuration
percussion.membership.password.min-length=8
percussion.membership.session.max-concurrent=5
percussion.membership.reset-token.expiry=24h
```

### Security Configuration

The module includes comprehensive security features:

- **Password Encryption**: Secure password hashing and validation
- **Session Management**: Token-based session tracking
- **OWASP Compliance**: Protection against common vulnerabilities
- **Secure Headers**: CSRF protection and secure cookie handling

## Java 17 Refactoring Notes

This module has been **fully refactored to Java 17** standards:

### Modern Java Features Used

- **`var` declarations** for improved readability
- **`Optional` API** for null safety and functional programming
- **`LocalDateTime`** for modern date/time handling
- **Immutable data classes** with builder patterns
- **Factory methods** for object creation
- **Functional interfaces** and method references
- **Text blocks** for multi-line strings (where applicable)

### Key Improvements

1. **Type Safety**: Extensive use of `Optional` prevents null pointer exceptions
2. **Immutability**: All data classes are immutable with defensive copying
3. **Builder Pattern**: Fluent APIs for complex object construction
4. **Factory Methods**: Named constructors for clarity and type safety
5. **Modern Collections**: Stream API for data processing
6. **Exception Handling**: Detailed exception classes with context

### Backward Compatibility

All public APIs maintain backward compatibility with existing clients while leveraging modern Java features internally.

## Testing

### Unit Tests

The module includes comprehensive JUnit 5 tests:

- **Service Layer Tests**: Mock-based testing of business logic
- **Data Class Tests**: Validation of immutability and equals/hashCode
- **Security Tests**: Authentication and authorization scenarios
- **Integration Tests**: End-to-end workflow testing

### Test Structure

```
src/test/java/
├── com/percussion/membership/services/
│   ├── PSBaseMembershipServiceTest.java
│   ├── PSBaseMembershipRestServiceTest.java
│   └── rdbms/
├── com/percussion/generickey/utils/services/
│   └── PSGenericKeyServiceTest.java
└── resources/
    ├── test-beans.xml
    └── test configuration files
```

## Dependencies

### Runtime Dependencies

- Spring Boot Starter Web
- Spring Security
- Hibernate Core
- Jackson Databind
- Guava
- Commons Lang3
- Log4j 2

### Test Dependencies

- JUnit 5
- Mockito
- Spring Test
- Derby Database (embedded)

## Deployment

### WAR Packaging

The module builds as a WAR file for deployment to application servers:

```bash
mvn clean package
# Produces: target/perc-membership-services-8.1.6-SNAPSHOT.war
```

### Docker Support

The module can be containerized using the provided configuration:

```dockerfile
FROM openjdk:11-jre-slim
COPY target/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.war"]
```

## Security Considerations

### Password Security

- **BCrypt Encryption**: Industry-standard password hashing
- **Salt Generation**: Unique salts for each password
- **Minimum Complexity**: Configurable password requirements

### Session Security

- **Secure Tokens**: Cryptographically secure session identifiers
- **Timeout Management**: Configurable session expiry
- **Concurrent Sessions**: Limit on simultaneous user sessions

### API Security

- **HTTPS Only**: All endpoints require secure connections
- **CSRF Protection**: Cross-site request forgery prevention
- **Input Validation**: Comprehensive request validation
- **Rate Limiting**: Protection against brute force attacks

## Monitoring and Logging

### Logging

- **Structured Logging**: JSON format for log aggregation
- **Security Events**: Detailed audit trail of authentication events
- **Performance Metrics**: Response time and throughput monitoring
- **Error Tracking**: Comprehensive error logging with context

### Health Checks

- **Actuator Endpoints**: Spring Boot health and metrics
- **Database Connectivity**: Connection pool monitoring
- **Memory Usage**: JVM memory and garbage collection metrics

## Troubleshooting

### Common Issues

1. **Authentication Failures**: Check user credentials and account status
2. **Session Timeouts**: Verify session configuration and timeout settings
3. **Database Connectivity**: Ensure database is accessible and schema is current
4. **Memory Issues**: Monitor JVM heap usage and garbage collection

### Debug Configuration

Enable debug logging for troubleshooting:

```properties
logging.level.com.percussion.membership=DEBUG
logging.level.org.springframework.security=DEBUG
```

## Contributing

### Code Style

- Follow Google Java Style Guide
- Use Java 17 features where appropriate
- Maintain immutability in data classes
- Include comprehensive unit tests
- Document public APIs with Javadoc

### Pull Request Process

1. Create feature branch from main
2. Implement changes with tests
3. Run full test suite: `mvn clean verify`
4. Update documentation as needed
5. Submit pull request with detailed description

---

**Note**: This module has been fully refactored to Java 17 standards as part of the Percussion CMS modernization initiative. All classes include the `// REFACTORED: CP-JAVA11` marker indicating completion of the refactoring process.
