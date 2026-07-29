# SQL Injection Prevention - Testing Guide

**Context**: CodeQL Alert Remediation for 8 SQL Injection vulnerabilities
**Framework**: Hibernate 6.x with JUnit5 + Mockito
**Location**: PSPageDaoHelper.java (projects/sitemanage/)
**Testing Principle**: Parameterized queries eliminate injection risk entirely

## Testing Strategy

The fundamental principle of SQL injection prevention via parameterized queries is: **once you've converted string concatenation to parameter binding, the vulnerability is eliminated by design**. This means testing focuses on two areas:

1. **Structural Verification**: Confirm the SQL uses parameterized placeholders (`:paramName`)
2. **Integration Testing**: Confirm the methods still work correctly with legitimate data

## Integration Test Pattern for SQL Fixes

### Test Setup

```java
// Location: projects/sitemanage/src/test/java/.../PSPageDaoHelperTest.java

@SpringBootTest
@DataJpaTest // Or appropriate test annotation for your DAO test setup
@Transactional
class PSPageDaoHelperSecurityIntegrationTest {

  @Autowired
  private PSPageDaoHelper daoHelper;

  @Autowired
  private TestEntityManager entityManager;

  // Setup test data...
}
```

### Positive Test Example

```java
@Test
@DisplayName("SQL Injection Fix: findPageIdsByTemplate handles legitimate template ID")
void testFindPageIdsByTemplateWithValidData() {
  // Setup: Create test page with template
  PSPage testPage = createTestPage("templateId123");
  entityManager.persist(testPage);
  entityManager.flush();

  // Execute
  Collection<Integer> results = daoHelper.findPageIdsByTemplate("templateId123");

  // Verify: Legitimate template lookup works
  assertNotNull(results);
  assertTrue(results.contains(testPage.getContentId()));
}
```

### Negative Test Example

```java
@Test
@DisplayName("SQL Injection Fix: SQL injection payload treated as literal parameter")
void testFindPageIdsByTemplateBlocksSQLInjection() {
  // Setup: Create test data without matching template
  PSPage testPage = createTestPage("legitimate");
  entityManager.persist(testPage);
  entityManager.flush();

  // Execute: Query with SQL injection payload
  // With parameterized query, this is treated as literal string "' OR '1'='1"
  // No matching template, so no results
  Collection<Integer> results = daoHelper.findPageIdsByTemplate("' OR '1'='1");

  // Verify: Injection payload didn't execute; no results found
  assertNotNull(results);
  assertTrue(results.isEmpty()); // No match for literal string "' OR '1'='1"
}
```

## Integration Test: Inline Verification

For faster development, you can verify the fix with a unit test that mocks the session:

```java
@Test
@DisplayName("Verify parameterized query structure")
void testFindPageIdsByTemplateUsesParameterization() {
  // Arrange: Mock the session to capture the query
  var mockSession = mock(Session.class);
  daoHelper.setSession(mockSession); // If setter exists, or use reflection

  ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

  var mockQuery = mock(NativeQuery.class);
  when(mockSession.createNativeQuery(sqlCaptor.capture()))
      .thenReturn(mockQuery);
  when(mockQuery.list()).thenReturn(List.of());

  // Act
  daoHelper.findPageIdsByTemplate("testTemplate");

  // Assert: SQL contains parameter placeholder, not string concatenation
  String capturedSql = sqlCaptor.getValue();
  assertTrue(capturedSql.contains(":template"),
      "SQL should use parameterized placeholder :template");
  assertFalse(capturedSql.contains("' + "),
      "SQL should not use string concatenation");
  assertFalse(capturedSql.contains("TEMPLATEID = '"),
      "SQL should not use direct string quotes");
}
```

## Key Testing Principles

### ✅ What to VERIFY in Tests

1. **Legitimate data flows correctly**
   - Template lookup returns expected pages
   - Multiple page IDs in IN list process correctly
   - Search criteria filters work as intended
2. **Injection patterns are safe**
   - Query returns empty/no results (attempted injection matches no data)
   - No SQL errors thrown during query parsing
   - No unintended data accessed
3. **Edge cases handled**
   - Null template ID
   - Empty page list
   - Special characters in legitimate values
   - Very long input values

### ❌ What NOT to do

- **Don't test that the database rejects the injection** - The protection is earlier (parameter binding), not in the database
- **Don't try to force SQL syntax errors** - That would indicate the payload was parsed as SQL (bad); with parameterization it's treated as a literal value
- **Don't create overly complex Hibernate mocks** - The framework handles parameter binding; trust the design

## Testing the Four Fixed Methods

### 1. findPageIdsByTemplate(String templateId)

```java
@Nested
@DisplayName("Security: SQL Injection Fix for findPageIdsByTemplate")
class FindPageIdsByTemplateSecurityTests {

  @Test
  @DisplayName("POSITIVE: Valid template ID returns pages using that template")
  void testValidTemplateLookup() { /* ... */ }

  @Test
  @DisplayName("NEGATIVE: OR injection payload finds no results")
  void testBlocksOrInjection() {
    // Query with "' OR '1'='1" - no template with that literal name
  }

  @Test
  @DisplayName("NEGATIVE: DROP TABLE payload doesn't execute")
  void testBlocksDropTableInjection() {
    // Query with "'; DROP TABLE PAGES; --" - literal string doesn't match
  }

  @Test
  @DisplayName("NEGATIVE: UNION injection payload doesn't execute")
  void testBlocksUnionSelectInjection() {
    // Query with "1 UNION SELECT ..." - no template with that name
  }
}
```

### 2. findPageIdsByTemplateAndImportedPageIds(String templateId, List<Integer> pages)

```java
@Nested
@DisplayName("Security: SQL Injection Fix for findPageIdsByTemplateAndImportedPageIds")
class FindPageIdsByTemplateAndImportedSecurityTests {

  @Test
  @DisplayName("POSITIVE: Valid template with page list returns matching pages")
  void testValidTemplateAndPageLookup() { /* ... */ }

  @Test
  @DisplayName("NEGATIVE: Injection in template ID doesn't affect results")
  void testBlocksTemplateIdInjection() {
    // Query with "valid_template' AND 1=2 --" - no template with that literal name
  }

  @Test
  @DisplayName("NEGATIVE: Injection in page ID list is treated as literal string")
  void testBlocksPageIdListInjection() {
    // Page list with injection: should be parameterized, not concatenated
  }
}
```

### 3. getContentIdsForFetchingByStatus(PSSearchCriteria criteria, List<Integer> contentIDs)

```java
@Nested
@DisplayName("Security: SQL Injection Fix for getContentIdsForFetchingByStatus")
class GetContentIdsSecurityTests {

  @Test
  @DisplayName("POSITIVE: Valid search criteria returns matching content IDs")
  void testValidSearchCriteria() { /* ... */ }

  @Test
  @DisplayName("NEGATIVE: Injection in templateId field doesn't execute")
  void testBlocksTemplatIdCriteriaInjection() { /* ... */ }

  @Test
  @DisplayName("NEGATIVE: Injection in status modifier doesn't execute")
  void testBlocksStatusModifierInjection() { /* ... */ }

  @Test
  @DisplayName("NEGATIVE: Complex injection patterns blocked")
  void testBlocksComplexInjectionPatterns() { /* ... */ }
}
```

### 4. formGetByStatusSQLQuery(PSSearchCriteria criteria, String sql)

```java
@Nested
@DisplayName("Security: SQL Injection Fix for formGetByStatusSQLQuery")
class FormGetByStatusSqlQuerySecurityTests {

  @Test
  @DisplayName("POSITIVE: Helper method preserves parameterized structure")
  void testHelperMaintainsParameterization() {
    // Verify output SQL uses :paramName placeholders
  }

  @Test
  @DisplayName("NEGATIVE: No string concatenation introduced")
  void testHelperDoesntIntroduceConcatenation() {
    // Verify output SQL doesn't use ' + ' string concatenation
  }
}
```

## Test Organization in Existing Test Suite

If the project already has test infrastructure for PSPageDaoHelper:

1. **Add to existing test class**:
   - Locate: `projects/sitemanage/src/test/java/.../PSPageDaoHelperTest.java`
   - Add: `@Nested` test classes for each fixed method
   - Result: Organized tests grouped by method
2. **Create security-focused test class**:
   - Create: `PSPageDaoHelperSecurityTest.java`
   - Focus: Purely on SQL injection prevention validation
   - Result: Clear separation of concerns, easy to find security tests
3. **Use existing test fixtures**:
   - Leverage: Existing test data builders, database state setup
   - Pattern: Follow existing test naming and structure
   - Result: Consistent with codebase conventions

## Verification Checklist

After implementing tests, verify:

- [ ] All 4 fixed methods have positive tests (legitimate data works)
- [ ] All 4 fixed methods have negative tests (injection patterns blocked)
- [ ] Tests follow JUnit5 best practices
- [ ] Test names clearly describe what's being tested
- [ ] Setup/Teardown properly manages test data
- [ ] SQL parameterization verified structurally
- [ ] Tests compile without warnings
- [ ] Tests pass locally with `./mvnw test`
- [ ] No performance regression in data access operations

## Example Full Test Class

```java
@SpringBootTest
@DataJpaTest
@Transactional
@DisplayName("PSPageDaoHelper - SQL Injection Prevention")
class PSPageDaoHelperSecurityTests {

  @Autowired PSPageDaoHelper daoHelper;
  @Autowired TestEntityManager entityManager;

  private PSPage testPage;

  @BeforeEach
  void setupTestData() {
    testPage = new PSPage();
    testPage.setContentId(100);
    testPage.setTemplateId("template_secure");
    entityManager.persistAndFlush(testPage);
  }

  // ========== findPageIdsByTemplate ==========

  @Test
  @DisplayName("✅ POSITIVE: Valid template returns pages")
  void testValidTemplateLookup() {
    var result = daoHelper.findPageIdsByTemplate("template_secure");

    assertNotNull(result);
    assertTrue(result.contains(100));
  }

  @Test
  @DisplayName("✅ NEGATIVE: OR injection treated as literal string")
  void testOrInjectionBlocked() {
    var result = daoHelper.findPageIdsByTemplate("' OR '1'='1");

    // No template with that literal name - parameterization protected us
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("✅ NEGATIVE: DROP TABLE injection cannot execute")
  void testDropTableInjectionBlocked() {
    var result = daoHelper.findPageIdsByTemplate("'; DROP TABLE PAGES; --");

    assertTrue(result.isEmpty());
  }
}
```

---

## Security References for Test Design

- [OWASP Testing for SQL Injection](https://owasp.org/www-project-web-security-testing-guide/stable/4-Web_Application_Security_Testing/07-Input_Validation_Testing/05-Testing_for_SQL_Injection.html)
- [Hibernate Security Best Practices](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#security)
- [JUnit5 Best Practices](https://junit.org/junit5/docs/current/user-guide/#writing-tests)
- [Mockito Best Practices](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

**Note**: The core principle remains: **Parameterized queries eliminate SQL injection by design**. Testing validates this principle is correctly implemented and legitimate functionality is preserved. The injection "fails safe"—attempting an injection simply finds no matching data rather than executing arbitrary SQL.
