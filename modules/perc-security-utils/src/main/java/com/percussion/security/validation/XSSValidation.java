/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import org.apache.commons.text.StringEscapeUtils;

/**
 * Utility class for preventing Cross-Site Scripting (XSS) vulnerabilities (CWE-79).
 *
 * <p>Provides methods to escape HTML/XML content before returning it in REST API responses or
 * rendering it in web contexts. This prevents malicious scripts from being executed in the browser.
 *
 * <p>Sunny Sal says: "Escaping is like wearing a seatbelt—it protects you from a collision with bad
 * actors!"
 *
 * @author Copilot
 * @version 1.0
 * @since 8.1
 */
public final class XSSValidation {

  /** Private constructor to prevent instantiation of utility class. */
  private XSSValidation() {
    throw new AssertionError("XSSValidation is a utility class and cannot be instantiated");
  }

  /**
   * Escapes HTML special characters in the provided string to prevent XSS attacks (CWE-79).
   *
   * <p>This method converts HTML metacharacters into their HTML entity equivalents:
   *
   * <ul>
   *   <li>&lt; becomes &amp;lt;
   *   <li>&gt; becomes &amp;gt;
   *   <li>&amp; becomes &amp;amp;
   *   <li>" becomes &amp;quot;
   *   <li>' becomes &amp;#39;
   * </ul>
   *
   * <p>Usage:
   *
   * <blockquote>
   *
   * <pre>
   * String userInput = request.getParameter("name");
   * String safeOutput = XSSValidation.escapeHtml(userInput);
   * response.getWriter().println("Hello, " + safeOutput);
   * </pre>
   *
   * </blockquote>
   *
   * @param input the string to escape, may be {@code null}
   * @return the HTML-escaped string, or {@code null} if input is {@code null}
   * @see <a href="https://owasp.org/www-community/attacks/xss/">OWASP XSS Prevention</a>
   */
  public static String escapeHtml(String input) {
    if (input == null) {
      return null;
    }
    return StringEscapeUtils.escapeHtml4(input);
  }

  /**
   * Escapes XML special characters in the provided string to prevent XXE and XML injection attacks.
   *
   * <p>This method converts XML metacharacters into their entity equivalents:
   *
   * <ul>
   *   <li>&lt; becomes &amp;lt;
   *   <li>&gt; becomes &amp;gt;
   *   <li>&amp; becomes &amp;amp;
   *   <li>" becomes &amp;quot;
   *   <li>' becomes &amp;#39;
   * </ul>
   *
   * <p>Usage:
   *
   * <blockquote>
   *
   * <pre>
   * String userInput = request.getParameter("content");
   * String safeXml = XSSValidation.escapeXml(userInput);
   * xmlBuilder.append("&lt;content&gt;").append(safeXml).append("&lt;/content&gt;");
   * </pre>
   *
   * </blockquote>
   *
   * @param input the string to escape, may be {@code null}
   * @return the XML-escaped string, or {@code null} if input is {@code null}
   * @see <a href="https://owasp.org/www-community/attacks/xpathinjection/">OWASP XML Injection</a>
   */
  public static String escapeXml(String input) {
    if (input == null) {
      return null;
    }
    return StringEscapeUtils.escapeXml11(input);
  }

  /**
   * Escapes JavaScript special characters in the provided string to prevent XSS attacks in
   * JavaScript context (CWE-79).
   *
   * <p>This method escapes characters that have special meaning in JavaScript string literals:
   *
   * <ul>
   *   <li>" becomes \"
   *   <li>' becomes \'
   *   <li>\ becomes \\
   *   <li>/ becomes \/
   *   <li>newline, tab, and carriage return are escaped
   * </ul>
   *
   * <p>Usage:
   *
   * <blockquote>
   *
   * <pre>
   * String userInput = request.getParameter("message");
   * String safeJs = XSSValidation.escapeJavaScript(userInput);
   * response.getWriter().println("&lt;script&gt;alert('" + safeJs + "');&lt;/script&gt;");
   * </pre>
   *
   * </blockquote>
   *
   * @param input the string to escape, may be {@code null}
   * @return the JavaScript-escaped string, or {@code null} if input is {@code null}
   * @see <a href="https://owasp.org/www-community/attacks/xss/">OWASP XSS Prevention</a>
   */
  public static String escapeJavaScript(String input) {
    if (input == null) {
      return null;
    }
    return StringEscapeUtils.escapeEcmaScript(input);
  }

  /**
   * Escapes CSV (Comma-Separated Values) special characters in the provided string to prevent CSV
   * injection attacks.
   *
   * <p>Usage:
   *
   * <blockquote>
   *
   * <pre>
   * String userInput = request.getParameter("data");
   * String safeCsv = XSSValidation.escapeCsv(userInput);
   * csvBuilder.append(safeCsv).append(",");
   * </pre>
   *
   * </blockquote>
   *
   * @param input the string to escape, may be {@code null}
   * @return the CSV-escaped string, or {@code null} if input is {@code null}
   */
  public static String escapeCsv(String input) {
    if (input == null) {
      return null;
    }
    return StringEscapeUtils.escapeCsv(input);
  }

  /**
   * Removes all HTML/XML tags from the provided string, leaving only plain text.
   *
   * <p>Usage:
   *
   * <blockquote>
   *
   * <pre>
   * String htmlInput = "&lt;p&gt;Hello &lt;strong&gt;World&lt;/strong&gt;&lt;/p&gt;";
   * String plainText = XSSValidation.stripHtmlTags(htmlInput);
   * // Result: "Hello World"
   * </pre>
   *
   * </blockquote>
   *
   * @param input the HTML string to clean, may be {@code null}
   * @return the plain text with all tags removed, or {@code null} if input is {@code null}
   */
  public static String stripHtmlTags(String input) {
    if (input == null) {
      return null;
    }
    // Remove all HTML tags using regex
    return input.replaceAll("<[^>]*>", "");
  }

  /**
   * Validates that the provided string does not contain common XSS payloads.
   *
   * <p>This is a simple check for obviously malicious patterns. It is NOT a comprehensive XSS
   * detection mechanism. Always use proper escaping in addition to this validation.
   *
   * <p>Usage:
   *
   * <blockquote>
   *
   * <pre>
   * String userInput = request.getParameter("name");
   * if (!XSSValidation.containsSuspiciousPatterns(userInput)) {
   *   // Process input safely after escaping
   *   String safeOutput = XSSValidation.escapeHtml(userInput);
   * } else {
   *   // Reject input
   *   response.sendError(400, "Invalid input");
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param input the string to check, may be {@code null}
   * @return {@code true} if the input contains suspicious XSS patterns, {@code false} otherwise
   */
  public static boolean containsSuspiciousPatterns(String input) {
    if (input == null) {
      return false;
    }

    input = input.toLowerCase();

    // Check for common XSS patterns
    return input.contains("<script")
        || input.contains("javascript:")
        || input.contains("onerror=")
        || input.contains("onload=")
        || input.contains("onclick=")
        || input.contains("onmouseover=")
        || input.contains("onfocus=")
        || input.contains("onchange=")
        || input.contains("onsubmit=")
        || input.contains("onkeydown=")
        || input.contains("onkeyup=")
        || input.contains("<iframe")
        || input.contains("<object")
        || input.contains("<embed")
        || input.contains("<img")
        || input.contains("vbscript:")
        || input.contains("data:text/html");
  }

  /**
   * Maximum length accepted for a JSONP callback name (defense-in-depth against oversized parameter
   * abuse).
   */
  public static final int MAX_JSONP_CALLBACK_LENGTH = 128;

  /**
   * Allow-list for JSONP callback function names: a JavaScript identifier optionally dotted (e.g.
   * {@code jQuery123}, {@code angular.callbacks._0}). Rejects anything that could break out of
   * {@code callback(...)} and inject script (alert #595 / T044).
   */
  private static final java.util.regex.Pattern SAFE_JSONP_CALLBACK =
      java.util.regex.Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*$");

  /**
   * Validates and returns a safe JSONP callback name, or {@code null} if the input is not a legal
   * callback identifier. Callers should treat {@code null} as "emit plain JSON" (no padding). Never
   * concatenate an unsanitized query parameter into script output.
   *
   * @param callback raw {@code jsoncallback} query parameter, may be {@code null} or blank
   * @return the original callback if it matches the allow-list; otherwise {@code null}
   */
  public static String sanitizeJsonpCallback(String callback) {
    if (callback == null) {
      return null;
    }
    String trimmed = callback.trim();
    if (trimmed.isEmpty() || trimmed.length() > MAX_JSONP_CALLBACK_LENGTH) {
      return null;
    }
    if (!SAFE_JSONP_CALLBACK.matcher(trimmed).matches()) {
      return null;
    }
    return trimmed;
  }
}
