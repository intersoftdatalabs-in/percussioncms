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
package com.percussion.search.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Security-focused unit tests for {@link PSSearchPatternService}.
 *
 * <p><b>Security Focus:</b> Tests verify that regex injection attacks are prevented by validating
 * that user-provided search patterns are treated as literal strings, not as regex expressions. This
 * ensures metacharacters and regex operators cannot be used to bypass search filters.
 *
 * <p><b>CWE-94: Improper Control of Generation of Code</b> <br>
 * <b>OWASP A03:2021 – Injection</b>
 *
 * @author Percussion Code Review - Security Hardening
 */
@DisplayName("PSSearchPatternService Security Tests")
class PSSearchPatternServiceSecurityTest {

  private PSSearchPatternService service;

  @BeforeEach
  void setUp() {
    service = new PSSearchPatternService();
  }

  @Nested
  @DisplayName("Positive Tests - Legitimate Usage")
  class PositiveTests {

    @Test
    @DisplayName("Should match exact substring in simple pattern")
    void shouldMatchExactSubstring() {
      List<String> items = List.of("hello.txt", "world.txt", "test.txt");
      List<String> results = service.filterContentByNamePattern(items, ".txt");

      assertEquals(
          3,
          results.size(),
          "All items with .txt extension should match literal dot followed by txt");
      assertTrue(results.contains("hello.txt"));
      assertTrue(results.contains("world.txt"));
      assertTrue(results.contains("test.txt"));
    }

    @Test
    @DisplayName("Should match partial substring in middle of name")
    void shouldMatchPartialSubstring() {
      List<String> items = List.of("article_main.html", "article_sidebar.html", "page_main.html");
      List<String> results = service.filterContentByNamePattern(items, "article");

      assertEquals(2, results.size(), "Only items containing 'article' should match");
      assertTrue(results.contains("article_main.html"));
      assertTrue(results.contains("article_sidebar.html"));
      assertFalse(results.contains("page_main.html"));
    }

    @Test
    @DisplayName("Should match empty pattern against all items")
    void shouldMatchEmptyPatternAgainstAllItems() {
      List<String> items = List.of("item1", "item2", "item3");
      List<String> results = service.filterContentByNamePattern(items, "");

      assertEquals(items.size(), results.size(), "Empty pattern should match all items");
    }

    @Test
    @DisplayName("Should match whitespace-only pattern without throwing exception")
    void shouldHandleWhitespaceOnlyPattern() {
      List<String> items = List.of("item1", "item2");
      List<String> results = service.filterContentByNamePattern(items, "   ");

      assertEquals(items.size(), results.size(), "Whitespace pattern should match all items");
    }

    @Test
    @DisplayName("Should return empty list for empty collection")
    void shouldReturnEmptyListForEmptyCollection() {
      List<String> results = service.filterContentByNamePattern(List.of(), "search");

      assertTrue(results.isEmpty(), "Empty input should return empty output");
    }

    @Test
    @DisplayName("Should match common file extensions (.pdf, .doc, .html, etc)")
    void shouldMatchFileExtensions() {
      // Test .pdf extension
      assertTrue(
          service.matchesContentPattern("document.pdf", ".pdf"), "Should match .pdf extension");
      // Test .doc extension
      assertTrue(
          service.matchesContentPattern("report.doc", ".doc"), "Should match .doc extension");
      // Test .html extension
      assertTrue(
          service.matchesContentPattern("index.html", ".html"), "Should match .html extension");
      // Test .xml extension
      assertTrue(
          service.matchesContentPattern("config.xml", ".xml"), "Should match .xml extension");
      // Test .json extension
      assertTrue(
          service.matchesContentPattern("data.json", ".json"), "Should match .json extension");
    }

    @Test
    @DisplayName("Should support case-sensitive matching by default")
    void shouldSupportCaseSensitiveMatching() {
      String itemName = "MyDocument";
      assertTrue(service.matchesContentPattern(itemName, "MyDocument"), "Exact case should match");
      assertFalse(
          service.matchesContentPattern(itemName, "mydocument"), "Different case should not match");
    }

    @Test
    @DisplayName("Should support case-insensitive matching when requested")
    void shouldSupportCaseInsensitiveMatching() {
      String itemName = "MyDocument";
      assertTrue(
          service.matchesContentPatternIgnoreCase(itemName, "mydocument"),
          "Case-insensitive should match regardless of case");
      assertTrue(
          service.matchesContentPatternIgnoreCase(itemName, "MYDOCUMENT"),
          "Case-insensitive should match uppercase");
      assertTrue(
          service.matchesContentPatternIgnoreCase(itemName, "MyDocument"),
          "Case-insensitive should match original case");
    }
  }

  @Nested
  @DisplayName("Negative Tests - Regex Injection Prevention")
  class NegativeSecurityTests {

    @Test
    @DisplayName("Should prevent wildcard injection (.*)")
    void shouldPreventWildcardInjection() {
      List<String> items = List.of("test.txt", "test.doc", "admin.txt");

      // Attack attempt: User tries to use .* to match any character
      // With proper escaping, .* should be treated as literal dot followed by asterisk
      List<String> results = service.filterContentByNamePattern(items, ".*");

      // Should match the LITERAL string ".*", not "any characters"
      assertTrue(results.isEmpty(), "Literal .* should not match any of these items");
    }

    @Test
    @DisplayName("Should prevent character class injection ([...])")
    void shouldPreventCharacterClassInjection() {
      List<String> items = List.of("admin.txt", "user.txt", "test.txt");

      // Attack attempt: Use [au] to match 'a' or 'u'
      // With proper escaping, [au] should be treated as literal brackets
      List<String> results = service.filterContentByNamePattern(items, "[au]");

      // Should only match items with literal [au] string, not 'a' or 'u'
      assertTrue(results.isEmpty(), "Character class should be treated as literal string");
    }

    @Test
    @DisplayName("Should prevent dot metacharacter injection (.)")
    void shouldPreventDotMetacharacterInjection() {
      List<String> items = List.of("txt", "txT", "txt_file", "txt.backup");

      // Attack attempt: Use . to match any single character
      List<String> results = service.filterContentByNamePattern(items, ".");

      // Should only match items containing literal dot
      assertFalse(
          results.contains("txt"), "Literal dot should not match 'txt' (any character pattern)");
      assertFalse(
          results.contains("txT"), "Literal dot should not match 'txT' (any character pattern)");
      assertTrue(
          results.contains("txt.backup"),
          "Literal dot should match 'txt.backup' (contains actual dot)");
    }

    @Test
    @DisplayName("Should prevent pipe operator injection (|)")
    void shouldPreventPipeOperatorInjection() {
      List<String> items = List.of("admin.txt", "user.txt", "guest.txt");

      // Attack attempt: Use | to match 'admin' OR 'user'
      List<String> results = service.filterContentByNamePattern(items, "admin|user");

      // Should only match items containing literal "admin|user" string
      assertTrue(results.isEmpty(), "Pipe operator should be treated as literal character");
    }

    @Test
    @DisplayName("Should prevent quantifier injection (+, *, ?)")
    void shouldPreventQuantifierInjection() {
      List<String> items = List.of("a", "aa", "aaa", "aaaa");

      // Attack attempt: Use 'a+' to match one or more 'a'
      List<String> results = service.filterContentByNamePattern(items, "a+");

      // Should only match items with literal "a+" string
      assertTrue(results.isEmpty(), "Quantifier should be treated as literal characters");
    }

    @Test
    @DisplayName("Should prevent anchor injection (^ and $)")
    void shouldPreventAnchorInjection() {
      List<String> items = List.of("test", "attest", "testing");

      // Attack attempt: Use ^test to match only lines starting with 'test'
      List<String> results = service.filterContentByNamePattern(items, "^test");

      // Should only match items with literal "^test" string
      assertTrue(results.isEmpty(), "Anchors should be treated as literal characters");
    }

    @Test
    @DisplayName("Should prevent parentheses injection (grouping)")
    void shouldPreventParenthesesInjection() {
      List<String> items = List.of("apple", "apply", "test");

      // Attack attempt: Use (app)le to create a group
      List<String> results = service.filterContentByNamePattern(items, "(app)");

      // Should only match items with literal "(app)" string
      assertTrue(
          results.isEmpty(), "Parentheses should be treated as literal characters, not grouping");
    }

    @Test
    @DisplayName("Should prevent backslash escape injection (\\)")
    void shouldPreventBackslashInjection() {
      List<String> items = List.of("a.txt", "atxt");

      // Attack attempt: Use \. to escape the dot
      List<String> results = service.filterContentByNamePattern(items, "\\.");

      // Should treat \. as literal backslash followed by dot
      // Only "a.txt" and similar should potentially match (if they have literal \.)
      // Most likely empty since items don't contain backslashes
      assertTrue(results.isEmpty(), "Backslash should be treated as literal character, not escape");
    }

    @Test
    @DisplayName("Should prevent ReDoS (Regular Expression Denial of Service)")
    void shouldPreventReDoSAttack() {
      // Create a pattern that would cause catastrophic backtracking
      // Example: (a+)+b - when matched against aaaaaaaac (no final b),
      // exponential backtracking occurs
      String maliciousPattern = "(a+)+";

      // This should NOT cause a timeout or hang due to escaping
      long startTime = System.currentTimeMillis();
      List<String> results =
          service.filterContentByNamePattern(List.of("aaaaaaaaaaaaaaaa"), maliciousPattern);
      long endTime = System.currentTimeMillis();

      // Should complete quickly (under 100ms) even with many 'a's
      long duration = endTime - startTime;
      assertTrue(
          duration < 100, "Pattern matching should complete quickly, not hang on ReDoS attack");
      assertTrue(results.isEmpty(), "Malicious pattern should not match normal items");
    }

    @Test
    @DisplayName("Should handle null pattern safely without throwing exception")
    void shouldHandleNullPatternSafely() {
      assertThrows(
          IllegalArgumentException.class,
          () -> service.filterContentByNamePattern(List.of("item1"), null),
          "Should throw IllegalArgumentException for null pattern");
    }

    @Test
    @DisplayName("Should handle null itemName in matching safely")
    void shouldHandleNullItemNameInMatchingSafely() {
      assertFalse(
          service.matchesContentPattern(null, "pattern"),
          "Null itemName should return false, not throw");
      assertFalse(
          service.matchesContentPatternIgnoreCase(null, "pattern"),
          "Null itemName (case-insensitive) should return false, not throw");
    }

    @Test
    @DisplayName("Should handle null pattern in matching safely")
    void shouldHandleNullPatternInMatchingSafely() {
      assertFalse(
          service.matchesContentPattern("itemName", null),
          "Null search pattern should return false, not throw");
      assertFalse(
          service.matchesContentPatternIgnoreCase("itemName", null),
          "Null search pattern (case-insensitive) should return false, not throw");
    }

    @Test
    @DisplayName("Should prevent combination attacks mixing multiple metacharacters")
    void shouldPreventCombinationAttacks() {
      List<String> items = List.of("admin123", "admin456", "user789");

      // Complex attack: (admin|user)[0-9]{3}
      List<String> results = service.filterContentByNamePattern(items, "(admin|user)[0-9]{3}");

      // Should not match even though input files match the pattern
      // because the pattern is treated literally
      assertTrue(results.isEmpty(), "Complex regex pattern should be treated as literal string");
    }

    @Test
    @DisplayName("Should preserve legitimate special characters in filenames")
    void shouldPreserveSpecialCharactersInFilenames() {
      // Some systems allow special characters in filenames
      List<String> items =
          List.of("file[2024].txt", "project(final).doc", "data{backup}.xml", "script+test.js");

      // User wants to find files with actual brackets
      List<String> results = service.filterContentByNamePattern(items, "[");

      // Should find the file with literal brackets
      assertTrue(results.contains("file[2024].txt"), "Should find file with literal brackets");
      assertFalse(
          results.contains("project(final).doc"),
          "Parentheses file should not match bracket search");
    }
  }

  @Nested
  @DisplayName("Integration Tests - Real-world Scenarios")
  class IntegrationTests {

    @Test
    @DisplayName("Real scenario: Search content by extension across mixed filenames")
    void shouldSearchByExtensionRealWorld() {
      List<String> contentItems =
          List.of(
              "homepage.html",
              "contact.html",
              "style.css",
              "config.xml",
              "data.json",
              "readme.txt");

      // User searches for HTML content
      List<String> htmlFiles = service.filterContentByNamePattern(contentItems, ".html");

      assertEquals(2, htmlFiles.size());
      assertTrue(htmlFiles.contains("homepage.html"));
      assertTrue(htmlFiles.contains("contact.html"));
    }

    @Test
    @DisplayName("Real scenario: Search with case-insensitive author filter")
    void shouldSearchByAuthorCaseInsensitively() {
      List<String> documents =
          List.of("Smith_Report.pdf", "SMITH_Summary.pdf", "smith_notes.txt", "Jones_Data.pdf");

      List<String> results =
          documents.stream()
              .filter(doc -> service.matchesContentPatternIgnoreCase(doc, "smith"))
              .toList();

      assertEquals(3, results.size(), "Should match all variations of smith");
    }

    @Test
    @DisplayName("Real scenario: Prevent ReDoS through user-provided filename filter")
    void shouldNotHangOnComplexUserInput() {
      String userInput = "(x+x+)+y"; // Known ReDoS pattern

      assertDoesNotThrow(
          () -> {
            service.filterContentByNamePattern(List.of("xxxxxxxxxxxxxxxxxxxxxx"), userInput);
          },
          "Should not throw or hang on ReDoS pattern");
    }
  }
}
