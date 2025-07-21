### Support
For issues or questions:
1. Check the documentation above
2. Review test cases for examples
3. Contact the development team
- Jackson 2.15.2

### Setup
1. Ensure Java 11 is installed
2. Update pom.xml dependencies if needed:
```xml
<properties>
    <jakarta.version>9.1.0</jakarta.version>
    <jersey.version>3.1.1</jersey.version>
    <jackson.version>2.15.2</jackson.version>
</properties>
```

### Building
```bash
mvn clean install
```

### Testing
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify -DskipITs=false
```

### Usage Examples

#### Creating a Feed
```java
// Create a feed descriptor
var descriptor = PSFeedDTO.builder()
    .name("blogFeed")
    .site("corporate")
    .type(FeedType.ATOM)
    .description("Corporate blog feed")
    .feedUrl("https://example.com/feeds/corporate/blogFeed")
    .build();

// Get feed items with null safety
List<PSFeedItem> items = feedDao.getFeedItems(descriptor)
    .orElse(Collections.emptyList());

// Generate feed content
String content = feedGenerator.makeFeedContent(descriptor, host, items);
```

#### REST API Usage
```java
// GET /feeds/{site}/{name}
@GET
@Path("/{site}/{name}")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML})
public Response getFeed(@PathParam("site") String site,
                       @PathParam("name") String name) {
    return feedService.getFeed(site, name)
        .map(Response::ok)
        .orElse(Response.status(Status.NOT_FOUND))
        .build();
}
```

### Security Notes
- All feed types are now validated through FeedType enum
- Input validation enforced for site and feed names
- Host validation prevents injection attacks
- XXE prevention in XML processing
- Secure error handling avoids information disclosure

### Performance Improvements
- Efficient media type mapping through enum
- Optimized stream operations
- Reduced object allocation
- Better memory usage with immutable objects
- Improved null handling reduces NPEs

### Known Issues
None currently reported.

### Future Enhancements
- Add caching support for feed responses
- Implement rate limiting
- Add metrics collection
- Support custom feed templates

```

### Parameterized Testing
Tests use JUnit 5's parameterized testing features:

```java
@ParameterizedTest(name = "Feed batch size {0} should be valid")
@ValueSource(ints = {1, 10, 100, 1000})
void shouldValidateFeedBatchSizes(int batchSize) {
    assertTrue(isValidBatchSize(batchSize));
}

@ParameterizedTest
@MethodSource("provideResponseThresholds")
void shouldValidateResponseThresholds(long threshold, boolean valid) {
    // Test implementation
}
```

### Test Coverage
Tests now verify:
- Performance thresholds and limits
- Load test configurations
- XML processing settings
- Security controls
- Property dependencies
- Edge cases and invalid inputs

### Configuration Validation
Automatic validation ensures:
- Positive batch sizes and thresholds
- Minimum load test duration (30s)
- Maximum operation limits
- Safe XML processing defaults
- Memory and CPU thresholds

### Advanced Test Configuration
The module now supports comprehensive test configuration:

```properties
# Performance thresholds
performance.threshold.response-time-ms=500
performance.threshold.memory-mb=256
performance.threshold.cpu-percent=80

# Load test settings
loadtest.min-duration-seconds=30
loadtest.target-throughput=100
loadtest.error-rate-threshold=0.01
```

### Dynamic Performance Testing
The module supports environment-aware test scenarios:

```java
@TestTemplate
@ExtendWith(PerformanceTestTemplateInvocationContextProvider.class)
void testFeedGeneration(PerformanceScenario scenario) {
    // Scenario-specific test implementation
}
```

### Test Scenarios
Pre-configured performance scenarios:
- Light Load (1 user, 100 ops, 500ms max)
- Medium Load (5 users, 200 ops, 1000ms max)
- Heavy Load (10 users, 500 ops, 2000ms max)

### Environment Adaptation
Tests automatically adjust to:
```properties
# CI environment
benchmark.concurrent.users=5
benchmark.timeout.seconds=60

# Development/Production
benchmark.concurrent.users=10
benchmark.timeout.seconds=120
```

### Performance Validation
Tests verify:
- Response times under load
- Memory usage patterns
- Concurrent user handling
- Environment-specific limits
- Resource utilization

### Test Infrastructure Complete!
The feeds module test infrastructure now provides:
1. XML validation with schema support
2. Performance monitoring via JMX
3. XML comparison with XMLUnit
4. Load testing with configurable parameters
5. Security controls and XXE prevention
6. Test data generation
7. Comprehensive configuration
