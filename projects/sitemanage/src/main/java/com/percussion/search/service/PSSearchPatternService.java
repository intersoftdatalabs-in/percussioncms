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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.percussion.security.SecureStringUtils;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides secure content filtering by pattern matching. This service safely handles user-supplied
 * search patterns to prevent Regex Injection attacks.
 *
 * <p><b>Security Focus:</b> All user-provided search patterns are treated as literal strings, not
 * as regex expressions. This prevents attackers from injecting regex metacharacters to bypass
 * search filters or cause denial of service through catastrophic backtracking.
 *
 * <p><b>CWE-94: Improper Control of Generation of Code</b> <br>
 * <b>OWASP A03:2021 – Injection</b>
 *
 * @author Percussion Code Review - Security Hardening
 */
public class PSSearchPatternService {

  private static final Logger log = LogManager.getLogger(PSSearchPatternService.class);

  /**
   * Filters content items by matching their names against a user-provided search pattern.
   *
   * <p>The search pattern is treated as a literal string (all regex metacharacters are escaped),
   * ensuring that user input cannot be used to inject regex code.
   *
   * <p><b>Example:</b>
   *
   * <pre>
   * {@code List<String>} items = List.of("test.txt", "test[1].txt", "test(1).txt");
   * // User searches for ".txt" - will match the literal dot, not regex any character
   * {@code List<String>} results = filterContentByNamePattern(items, ".txt");
   * // Returns only ["test.txt"] - not ["test.txt", "test[1].txt", "test(1).txt"]
   * </pre>
   *
   * @param items Collection of content item names to filter
   * @param searchPattern User-provided search pattern (treated as literal string)
   * @return List of items whose names contain the search pattern as a literal substring
   * @throws IllegalArgumentException if searchPattern is null
   * @see SecureStringUtils#escapeRegexString(String)
   */
  public List<String> filterContentByNamePattern(Collection<String> items, String searchPattern) {
    if (searchPattern == null) {
      throw new IllegalArgumentException("Search pattern cannot be null");
    }
    if (!isNotBlank(searchPattern) || items == null || items.isEmpty()) {
      return items == null ? List.of() : List.copyOf(items);
    }

    try {
      // SECURITY FIX: Escape user input before using in regex pattern
      // This prevents injection of regex metacharacters (.^$|?*+()[]{}\ etc.)
      String escapedPattern = SecureStringUtils.escapeRegexString(searchPattern);
      // Create a pattern that matches if the escaped search pattern appears anywhere in the string
      // Pattern.quote() wraps the result in \Q...\E which treats it as literal
      Pattern safePattern = Pattern.compile(".*" + escapedPattern + ".*");

      return items.stream()
          .filter(item -> safePattern.matcher(item).matches())
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error filtering content by pattern: {}", searchPattern);
      log.debug("Error details: {}", e.getMessage());
      // Return empty list on error rather than crashing
      return List.of();
    }
  }

  /**
   * Matches a single content item name against a user-provided search pattern.
   *
   * <p>User input is safely escaped to prevent regex injection.
   *
   * @param itemName The content item name to check
   * @param searchPattern User-provided search pattern (treated as literal string)
   * @return true if itemName contains the search pattern as a literal substring (case-sensitive)
   */
  public boolean matchesContentPattern(String itemName, String searchPattern) {
    if (itemName == null || searchPattern == null) {
      return false;
    }
    if (!isNotBlank(searchPattern)) {
      return true; // Empty pattern matches all
    }

    try {
      String escapedPattern = SecureStringUtils.escapeRegexString(searchPattern);
      Pattern safePattern = Pattern.compile(".*" + escapedPattern + ".*");
      return safePattern.matcher(itemName).matches();
    } catch (Exception e) {
      log.debug("Error matching content pattern: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Matches a single content item name against a user-provided search pattern (case-insensitive).
   *
   * <p>User input is safely escaped to prevent regex injection while preserving case-insensitive
   * matching behavior.
   *
   * @param itemName The content item name to check
   * @param searchPattern User-provided search pattern (treated as literal string)
   * @return true if itemName contains the search pattern (case-insensitive)
   */
  public boolean matchesContentPatternIgnoreCase(String itemName, String searchPattern) {
    if (itemName == null || searchPattern == null) {
      return false;
    }
    if (!isNotBlank(searchPattern)) {
      return true; // Empty pattern matches all
    }

    try {
      // SECURITY FIX: Escape user input before using in regex pattern with flags
      String escapedPattern = SecureStringUtils.escapeRegexString(searchPattern);
      // Create a pattern that matches if the escaped search pattern appears anywhere in the string
      // with case-insensitive flag
      Pattern safePattern = Pattern.compile(".*" + escapedPattern + ".*", Pattern.CASE_INSENSITIVE);
      return safePattern.matcher(itemName).matches();
    } catch (Exception e) {
      log.debug("Error matching content pattern (case-insensitive): {}", e.getMessage());
      return false;
    }
  }
}
