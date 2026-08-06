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

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Security utility for validating JCR query inputs to prevent query injection attacks. JCR (Java
 * Content Repository) queries don't support parameterized queries in the same way as SQL, so this
 * class provides validation for path and content type inputs.
 *
 * <p>Security considerations: - Paths must conform to valid JCR path format (alphanumeric, slashes,
 * hyphens, underscores) - Content types must be valid identifiers (alphanumeric, underscores) -
 * Single quotes are escaped to prevent query injection - Null bytes are rejected to prevent null
 * byte injection
 *
 * <p>CWE-89: SQL Injection (applies to JCR query injection)
 */
public class PSJCRQueryValidator {

  /** Pattern for valid JCR paths: alphanumeric, slashes, hyphens, underscores, single quotes */
  private static final Pattern VALID_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9/'_-]+$");

  /** Pattern for valid JCR content types: alphanumeric and underscores only */
  private static final Pattern VALID_CONTENT_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

  private static final String NULL_BYTE = "\0";

  private PSJCRQueryValidator() {
    // Utility class - no instantiation
  }

  /**
   * Validates and escapes a JCR path for use in query WHERE clause.
   *
   * @param path the path to validate
   * @return the validated and escaped path
   * @throws IllegalArgumentException if the path is invalid or contains injection attempts
   */
  public static String validateAndEscapePath(String path) {
    if (StringUtils.isBlank(path)) {
      throw new IllegalArgumentException("JCR path cannot be null or empty");
    }

    // Detect null byte injection (CWE-158)
    if (path.contains(NULL_BYTE)) {
      throw new IllegalArgumentException(
          "JCR path contains null bytes (null byte injection detected)");
    }

    // Detect URL-encoded null bytes
    if (path.contains("%00")) {
      throw new IllegalArgumentException("JCR path contains URL-encoded null bytes");
    }

    // Validate path format: only allow alphanumeric, slashes, hyphens, underscores, single quotes
    if (!VALID_PATH_PATTERN.matcher(path).matches()) {
      throw new IllegalArgumentException(
          "JCR path contains invalid characters. Only alphanumeric, '/', '-', \"'\", and '_' are"
              + " allowed");
    }

    // Escape single quotes by doubling them (JCR syntax)
    return path.replace("'", "''");
  }

  /**
   * Validates a JCR content type identifier.
   *
   * @param contentType the content type to validate
   * @return true if content type is valid
   */
  public static boolean isValidContentType(String contentType) {
    if (StringUtils.isBlank(contentType)) {
      return false;
    }

    // Reject null bytes
    if (contentType.contains(NULL_BYTE)) {
      return false;
    }

    // Validate format: alphanumeric and underscores only
    return VALID_CONTENT_TYPE_PATTERN.matcher(contentType).matches();
  }

  /**
   * Filters and validates a collection of content types. Silently removes invalid types instead of
   * throwing exceptions.
   *
   * @param contentTypes the collection of content types to filter
   * @return a filtered collection of valid content types
   */
  public static Collection<String> filterValidContentTypes(Collection<String> contentTypes) {
    if (contentTypes == null || contentTypes.isEmpty()) {
      return contentTypes;
    }

    return contentTypes.stream()
        .filter(Objects::nonNull)
        .filter(PSJCRQueryValidator::isValidContentType)
        .collect(Collectors.toList());
  }

  /**
   * Builds a safe JCR query for content retrieval by path and type.
   *
   * @param path the JCR path to query (will be validated and escaped)
   * @param contentTypes optional collection of content type names to filter by
   * @return a safe JCR query string
   * @throws IllegalArgumentException if path is invalid
   */
  public static String buildSafeJCRQuery(String path, Collection<String> contentTypes) {
    // Validate and escape the path
    String safePath = validateAndEscapePath(path);

    // Handle content type filtering
    if (contentTypes == null || contentTypes.isEmpty()) {
      return "select rx:sys_contentid from nt:base where jcr:path like '" + safePath + "/%'";
    }

    // Filter and validate content types
    Collection<String> validTypes = filterValidContentTypes(contentTypes);

    if (validTypes.isEmpty()) {
      throw new IllegalArgumentException("No valid content types provided");
    }

    // Build the FROM clause with validated types
    String fromClause =
        validTypes.stream().map(name -> "rx:" + name).collect(Collectors.joining(", "));

    return "select rx:sys_contentid from "
        + fromClause
        + " where jcr:path like '"
        + safePath
        + "/%'";
  }

  /**
   * Validates and escapes a JCR property name to prevent injection attacks. Property names must be
   * valid identifiers (CMS property references like "rx:propertyName").
   *
   * @param propertyName the property name to validate (may include namespace prefix like "rx:")
   * @return the validated property name (unchanged if valid)
   * @throws IllegalArgumentException if the property name contains invalid characters
   */
  public static String validateAndEscapePropertyName(String propertyName) {
    if (StringUtils.isBlank(propertyName)) {
      throw new IllegalArgumentException("Property name cannot be null or empty");
    }

    // Reject null bytes
    if (propertyName.contains(NULL_BYTE)) {
      throw new IllegalArgumentException("Property name contains null bytes");
    }

    // Property names can contain alphanumerics, underscores, and colons (for namespace)
    // Pattern allows: alphanumeric, underscores, colons for rx:propertyName format
    if (!Pattern.compile("^[a-zA-Z0-9:_]+$").matcher(propertyName).matches()) {
      throw new IllegalArgumentException(
          "Invalid property name. Only alphanumeric, underscores, and colons are allowed");
    }

    return propertyName;
  }

  /**
   * Escapes a value for use in a JCR query WHERE clause. Handles single quote escaping to prevent
   * injection attacks.
   *
   * @param value the value to escape
   * @return the escaped value (safe for use in WHERE clause)
   * @throws IllegalArgumentException if value contains null bytes
   */
  public static String escapeQueryValue(String value) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException("Query value cannot be null or empty");
    }

    // Reject null bytes
    if (value.contains(NULL_BYTE)) {
      throw new IllegalArgumentException("Query value contains null bytes");
    }

    // Escape single quotes by doubling them (JCR/SQL standard)
    return value.replace("'", "''");
  }

  /**
   * Builds a safe WHERE clause for JCR property matching. Validates and escapes both the property
   * name and value to prevent injection.
   *
   * @param propertyName the JCR property name (e.g., "rx:propertyName")
   * @param value the value to match
   * @return a safe WHERE clause fragment
   * @throws IllegalArgumentException if property name or value is invalid
   */
  public static String buildSafeWhereClause(String propertyName, String value) {
    String safeProperty = validateAndEscapePropertyName(propertyName);
    String safeValue = escapeQueryValue(value);
    return safeProperty + "='" + safeValue + "'";
  }
}
