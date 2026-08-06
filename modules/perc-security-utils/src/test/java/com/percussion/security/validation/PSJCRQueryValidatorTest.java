/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.security.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PSJCRQueryValidator. Tests validate JCR query injection prevention with positive
 * and negative test cases.
 *
 * <p>Test Coverage: - Positive tests: Valid paths, content types, and queries accepted safely -
 * Negative tests: Injection attempts, malformed inputs, and null bytes rejected - SQL injection
 * attack simulations: Common injection patterns prevented
 */
@DisplayName("PSJCRQueryValidator - JCR Query Injection Prevention")
class PSJCRQueryValidatorTest {

  // ============================================================================
  // POSITIVE TESTS: Valid paths should be accepted
  // ============================================================================

  @Test
  @DisplayName("Should accept valid simple path")
  void testValidSimplePath() {
    String path = "/Sites/MySite";
    String result = PSJCRQueryValidator.validateAndEscapePath(path);
    assertEquals("/Sites/MySite", result);
  }

  @Test
  @DisplayName("Should accept path with hyphens")
  void testValidPathWithHyphens() {
    String path = "/Sites/My-Site-Name";
    String result = PSJCRQueryValidator.validateAndEscapePath(path);
    assertEquals("/Sites/My-Site-Name", result);
  }

  @Test
  @DisplayName("Should accept path with underscores")
  void testValidPathWithUnderscores() {
    String path = "/Sites/My_Site_Name";
    String result = PSJCRQueryValidator.validateAndEscapePath(path);
    assertEquals("/Sites/My_Site_Name", result);
  }

  @Test
  @DisplayName("Should accept complex path with mixed valid characters")
  void testValidComplexPath() {
    String path = "/Sites/2024-My-Site_v1";
    String result = PSJCRQueryValidator.validateAndEscapePath(path);
    assertEquals("/Sites/2024-My-Site_v1", result);
  }

  @Test
  @DisplayName("Should escape single quotes in valid path")
  void testEscapeSingleQuotes() {
    String path = "/Sites/Site's-Page";
    String result = PSJCRQueryValidator.validateAndEscapePath(path);
    assertEquals("/Sites/Site''s-Page", result);
  }

  @Test
  @DisplayName("Should accept single quote in path and escape it")
  void testSingleQuoteEscaping() {
    String path = "/Path/With'Quote";
    String result = PSJCRQueryValidator.validateAndEscapePath(path);
    assertEquals("/Path/With''Quote", result);
  }

  // ============================================================================
  // NEGATIVE TESTS: Invalid paths should be rejected
  // ============================================================================

  @Test
  @DisplayName("Should reject null path")
  void testNullPath() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(null),
        "Path cannot be null");
  }

  @Test
  @DisplayName("Should reject empty path")
  void testEmptyPath() {
    assertThrows(
        IllegalArgumentException.class, () -> PSJCRQueryValidator.validateAndEscapePath(""));
  }

  @Test
  @DisplayName("Should reject blank path (whitespace only)")
  void testBlankPath() {
    assertThrows(
        IllegalArgumentException.class, () -> PSJCRQueryValidator.validateAndEscapePath("   "));
  }

  @Test
  @DisplayName("Should reject path with null byte injection")
  void testNullByteInjection() {
    String path = "/Sites/MySite\0/admin";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(path),
        "Should detect null byte injection");
  }

  @Test
  @DisplayName("Should reject path with URL-encoded null byte (%00)")
  void testUrlEncodedNullByteInjection() {
    String path = "/Sites/MySite%00/admin";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(path),
        "Should detect URL-encoded null byte injection");
  }

  @Test
  @DisplayName("Should reject path with special characters")
  void testPathWithSpecialCharacters() {
    String path = "/Sites/MySite;DROP TABLE;";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(path),
        "Should reject special characters");
  }

  @Test
  @DisplayName("Should reject path with double quotes")
  void testPathWithDoubleQuotes() {
    String path = "/Sites/MySite\" OR 1=1 --";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(path),
        "Should reject double quotes");
  }

  @Test
  @DisplayName("Should reject path with parentheses")
  void testPathWithParentheses() {
    String path = "/Sites/MySite(1)";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(path),
        "Should reject parentheses");
  }

  @Test
  @DisplayName("Should reject path with wildcards")
  void testPathWithWildcards() {
    String path = "/Sites/MySite*";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(path),
        "Should reject wildcard characters");
  }

  @Test
  @DisplayName("Should reject path with comment syntax")
  void testPathWithCommentSyntax() {
    String path = "/Sites/MySite -- comment";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(path),
        "Should reject comment syntax");
  }

  // ============================================================================
  // CONTENT TYPE VALIDATION TESTS
  // ============================================================================

  @Test
  @DisplayName("Should accept valid content type")
  void testValidContentType() {
    assertTrue(PSJCRQueryValidator.isValidContentType("percPage"));
  }

  @Test
  @DisplayName("Should accept content type with underscores")
  void testValidContentTypeWithUnderscores() {
    assertTrue(PSJCRQueryValidator.isValidContentType("perc_Page_Type"));
  }

  @Test
  @DisplayName("Should accept content type with numbers")
  void testValidContentTypeWithNumbers() {
    assertTrue(PSJCRQueryValidator.isValidContentType("percPage123"));
  }

  @Test
  @DisplayName("Should reject null content type")
  void testNullContentType() {
    assertFalse(PSJCRQueryValidator.isValidContentType(null));
  }

  @Test
  @DisplayName("Should reject empty content type")
  void testEmptyContentType() {
    assertFalse(PSJCRQueryValidator.isValidContentType(""));
  }

  @Test
  @DisplayName("Should reject content type with spaces")
  void testContentTypeWithSpaces() {
    assertFalse(PSJCRQueryValidator.isValidContentType("perc Page"));
  }

  @Test
  @DisplayName("Should reject content type with special characters")
  void testContentTypeWithSpecialCharacters() {
    assertFalse(PSJCRQueryValidator.isValidContentType("perc-Page"));
  }

  @Test
  @DisplayName("Should reject content type with slashes")
  void testContentTypeWithSlashes() {
    assertFalse(PSJCRQueryValidator.isValidContentType("perc/Page"));
  }

  @Test
  @DisplayName("Should reject content type with null bytes")
  void testContentTypeWithNullBytes() {
    assertFalse(PSJCRQueryValidator.isValidContentType("percPage\0"));
  }

  // ============================================================================
  // COLLECTION FILTERING TESTS
  // ============================================================================

  @Test
  @DisplayName("Should filter out invalid content types from collection")
  void testFilterValidContentTypes() {
    List<String> input = Arrays.asList("validType", "invalid-type", "another_valid", "", null);
    Collection<String> result = PSJCRQueryValidator.filterValidContentTypes(input);

    assertEquals(2, result.size());
    assertTrue(result.contains("validType"));
    assertTrue(result.contains("another_valid"));
    assertFalse(result.contains("invalid-type"));
  }

  @Test
  @DisplayName("Should handle null collection gracefully")
  void testFilterNullCollection() {
    Collection<String> result = PSJCRQueryValidator.filterValidContentTypes(null);
    assertNull(result);
  }

  @Test
  @DisplayName("Should handle empty collection")
  void testFilterEmptyCollection() {
    Collection<String> input = Collections.emptyList();
    Collection<String> result = PSJCRQueryValidator.filterValidContentTypes(input);
    assertTrue(result.isEmpty());
  }

  // ============================================================================
  // COMPLETE QUERY BUILDING TESTS
  // ============================================================================

  @Test
  @DisplayName("Should build safe query with valid path and no content types")
  void testBuildSafeQueryNoContentTypes() {
    String query = PSJCRQueryValidator.buildSafeJCRQuery("/Sites/MySite", null);
    assertEquals(
        "select rx:sys_contentid from nt:base where jcr:path like '/Sites/MySite/%'", query);
  }

  @Test
  @DisplayName("Should build safe query with valid path and empty content types")
  void testBuildSafeQueryEmptyContentTypes() {
    String query = PSJCRQueryValidator.buildSafeJCRQuery("/Sites/MySite", Collections.emptyList());
    assertEquals(
        "select rx:sys_contentid from nt:base where jcr:path like '/Sites/MySite/%'", query);
  }

  @Test
  @DisplayName("Should build safe query with valid path and single content type")
  void testBuildSafeQuerySingleContentType() {
    String query =
        PSJCRQueryValidator.buildSafeJCRQuery("/Sites/MySite", Arrays.asList("percPage"));
    assertEquals(
        "select rx:sys_contentid from rx:percPage where jcr:path like '/Sites/MySite/%'", query);
  }

  @Test
  @DisplayName("Should build safe query with valid path and multiple content types")
  void testBuildSafeQueryMultipleContentTypes() {
    String query =
        PSJCRQueryValidator.buildSafeJCRQuery(
            "/Sites/MySite", Arrays.asList("percPage", "percAsset"));
    assertTrue(query.contains("rx:percPage"));
    assertTrue(query.contains("rx:percAsset"));
    assertTrue(query.contains("where jcr:path like '/Sites/MySite/%'"));
  }

  @Test
  @DisplayName("Should reject query with invalid path")
  void testBuildSafeQueryInvalidPath() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSJCRQueryValidator.buildSafeJCRQuery(
                "/Sites/MySite'; DROP TABLE; --", Arrays.asList("percPage")),
        "Should reject query with invalid path");
  }

  @Test
  @DisplayName("Should reject query when all content types are invalid")
  void testBuildSafeQueryAllInvalidContentTypes() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSJCRQueryValidator.buildSafeJCRQuery(
                "/Sites/MySite", Arrays.asList("perc-invalid", "bad type")),
        "Should reject when all content types are invalid");
  }

  @Test
  @DisplayName("Should filter invalid types and build safe query with valid types")
  void testBuildSafeQueryMixedValidInvalidContentTypes() {
    String query =
        PSJCRQueryValidator.buildSafeJCRQuery(
            "/Sites/MySite", Arrays.asList("percPage", "invalid-type", "percAsset"));
    assertTrue(query.contains("rx:percPage"));
    assertTrue(query.contains("rx:percAsset"));
    assertFalse(query.contains("invalid-type")); // Invalid type should be filtered out
  }

  // ============================================================================
  // SQL INJECTION ATTACK SIMULATION TESTS
  // ============================================================================

  @Test
  @DisplayName("Should prevent classic SQL injection attack in path")
  void testPreventSQLInjectionInPath() {
    String maliciousPath = "/Sites/MySite' OR '1'='1";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(maliciousPath),
        "Should prevent SQL injection");
  }

  @Test
  @DisplayName("Should prevent UNION-based SQL injection")
  void testPreventUnionBasedSQLInjection() {
    String maliciousPath = "/Sites/MySite' UNION SELECT * FROM users--";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(maliciousPath),
        "Should prevent UNION-based injection");
  }

  @Test
  @DisplayName("Should prevent time-based blind SQL injection")
  void testPreventTimeBasedBlindInjection() {
    String maliciousPath = "/Sites/MySite' AND SLEEP(5)--";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(maliciousPath),
        "Should prevent time-based blind injection");
  }

  @Test
  @DisplayName("Should prevent stacked queries SQL injection")
  void testPreventStackedQueriesInjection() {
    String maliciousPath = "/Sites/MySite'; DELETE FROM users;--";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(maliciousPath),
        "Should prevent stacked queries injection");
  }

  @Test
  @DisplayName("Should prevent command injection attempts")
  void testPreventCommandInjection() {
    String maliciousPath = "/Sites/MySite`; rm -rf /`;";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(maliciousPath),
        "Should prevent command injection");
  }

  @Test
  @DisplayName("Should prevent XML/XPATH injection patterns")
  void testPreventXPathInjection() {
    String maliciousPath = "/Sites/' or 1=1 or 'x'='x";
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePath(maliciousPath),
        "Should prevent XPATH injection");
  }

  // ============================================================================
  // PROPERTY NAME VALIDATION TESTS
  // ============================================================================

  @Test
  @DisplayName("Should accept valid property name with namespace")
  void testValidPropertyNameWithNamespace() {
    String result = PSJCRQueryValidator.validateAndEscapePropertyName("rx:propertyName");
    assertEquals("rx:propertyName", result);
  }

  @Test
  @DisplayName("Should accept property name with underscores")
  void testValidPropertyNameWithUnderscores() {
    String result = PSJCRQueryValidator.validateAndEscapePropertyName("rx:proxy_name");
    assertEquals("rx:proxy_name", result);
  }

  @Test
  @DisplayName("Should accept property name with numbers")
  void testValidPropertyNameWithNumbers() {
    String result = PSJCRQueryValidator.validateAndEscapePropertyName("rx:field123");
    assertEquals("rx:field123", result);
  }

  @Test
  @DisplayName("Should reject null property name")
  void testNullPropertyName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePropertyName(null));
  }

  @Test
  @DisplayName("Should reject empty property name")
  void testEmptyPropertyName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePropertyName(""));
  }

  @Test
  @DisplayName("Should reject property name with spaces")
  void testPropertyNameWithSpaces() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePropertyName("rx: propertyName"));
  }

  @Test
  @DisplayName("Should reject property name with hyphens")
  void testPropertyNameWithHyphens() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePropertyName("rx-property"));
  }

  @Test
  @DisplayName("Should reject property name with slashes")
  void testPropertyNameWithSlashes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePropertyName("rx/property"));
  }

  @Test
  @DisplayName("Should reject property name with null bytes")
  void testPropertyNameWithNullBytes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.validateAndEscapePropertyName("rx:property\0name"));
  }

  // ============================================================================
  // VALUE ESCAPING TESTS
  // ============================================================================

  @Test
  @DisplayName("Should escape single quotes in value")
  void testEscapeQuotesInValue() {
    String result = PSJCRQueryValidator.escapeQueryValue("O'Reilly");
    assertEquals("O''Reilly", result);
  }

  @Test
  @DisplayName("Should escape multiple single quotes")
  void testEscapeMultipleQuotes() {
    String result = PSJCRQueryValidator.escapeQueryValue("It's a 'test' value");
    assertEquals("It''s a ''test'' value", result);
  }

  @Test
  @DisplayName("Should accept value with special characters (non-injection)")
  void testValueWithSpecialCharacters() {
    String result = PSJCRQueryValidator.escapeQueryValue("value-with-hyphens_and_underscores");
    assertEquals("value-with-hyphens_and_underscores", result);
  }

  @Test
  @DisplayName("Should reject null value")
  void testNullValue() {
    assertThrows(IllegalArgumentException.class, () -> PSJCRQueryValidator.escapeQueryValue(null));
  }

  @Test
  @DisplayName("Should reject empty value")
  void testEmptyValue() {
    assertThrows(IllegalArgumentException.class, () -> PSJCRQueryValidator.escapeQueryValue(""));
  }

  @Test
  @DisplayName("Should reject value with null bytes")
  void testValueWithNullBytes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.escapeQueryValue("value\0injection"));
  }

  @Test
  @DisplayName("Should accept value with numeric content")
  void testValueWithNumbers() {
    String result = PSJCRQueryValidator.escapeQueryValue("12345");
    assertEquals("12345", result);
  }

  // ============================================================================
  // WHERE CLAUSE BUILDING TESTS
  // ============================================================================

  @Test
  @DisplayName("Should build safe WHERE clause")
  void testBuildSafeWhereClause() {
    String result = PSJCRQueryValidator.buildSafeWhereClause("rx:content_id", "12345");
    assertEquals("rx:content_id='12345'", result);
  }

  @Test
  @DisplayName("Should escape quotes in WHERE clause value")
  void testBuildSafeWhereClauseWithQuotes() {
    String result = PSJCRQueryValidator.buildSafeWhereClause("rx:title", "O'Reilly");
    assertEquals("rx:title='O''Reilly'", result);
  }

  @Test
  @DisplayName("Should reject WHERE clause with invalid property")
  void testBuildSafeWhereClauseInvalidProperty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSJCRQueryValidator.buildSafeWhereClause("rx-invalid", "value"),
        "Should reject invalid property name");
  }

  @Test
  @DisplayName("Should prevent SQL injection in WHERE clause")
  void testBuildSafeWhereClausePreventSQLInjection() {
    // The value will be escaped, but the property name validation will protect against
    // injection in the property name itself
    String result = PSJCRQueryValidator.buildSafeWhereClause("rx:content_id", "123' OR '1'='1");
    assertEquals("rx:content_id='123'' OR ''1''=''1'", result);
  }
}
