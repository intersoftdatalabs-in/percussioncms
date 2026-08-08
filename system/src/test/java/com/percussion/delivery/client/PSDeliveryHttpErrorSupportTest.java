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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryClientException;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for structured delivery HTTP error logging helpers (issue #2354): full URL +
 * method + status without multi-KB Tomcat HTML dumps; actionable rotateKey WARN text.
 */
public class PSDeliveryHttpErrorSupportTest {

  @Test
  public void joinAdminActionUrlAvoidsDoubleSlash() {
    assertEquals(
        "https://localhost:8443/feeds/rss/rotateKey",
        PSDeliveryHttpErrorSupport.joinAdminActionUrl(
            "https://localhost:8443", "/feeds/rss/rotateKey"));
    assertEquals(
        "https://localhost:8443/feeds/rss/rotateKey",
        PSDeliveryHttpErrorSupport.joinAdminActionUrl(
            "https://localhost:8443/", "/feeds/rss/rotateKey"));
    assertEquals(
        "https://localhost:8443/feeds/rss/rotateKey",
        PSDeliveryHttpErrorSupport.joinAdminActionUrl(
            "https://localhost:8443", "feeds/rss/rotateKey"));
  }

  @Test
  public void firstLineSnippetTruncatesHtmlErrorPage() {
    String tomcatHtml =
        "<!doctype html><html><head><title>HTTP Status 405 – Method Not Allowed</title></head>"
            + "<body><h1>HTTP Status 405 – Method Not Allowed</h1>"
            + StringUtilsRepeat("x", 5000)
            + "</body></html>";
    String snippet =
        PSDeliveryHttpErrorSupport.firstLineSnippet(
            tomcatHtml, PSDeliveryHttpErrorSupport.DEFAULT_BODY_SNIPPET_MAX);
    assertFalse(snippet.isEmpty());
    assertTrue(
        snippet.length() <= PSDeliveryHttpErrorSupport.DEFAULT_BODY_SNIPPET_MAX + 3,
        "snippet must not dump multi-KB HTML: len=" + snippet.length());
    assertFalse(snippet.contains("\n"), "snippet must be single-line for log files");
  }

  @Test
  public void firstLineSnippetUsesFirstNonEmptyLine() {
    String body = "\nHTTP Status 405 – Method Not Allowed\nType Status Report\n";
    assertEquals(
        "HTTP Status 405 – Method Not Allowed",
        PSDeliveryHttpErrorSupport.firstLineSnippet(body, 200));
  }

  @Test
  public void formatExecutionErrorIncludesMethodUrlStatusAndShortReason() {
    String msg =
        PSDeliveryHttpErrorSupport.formatExecutionError(
            "PUT",
            "https://localhost:8443/feeds/rss/rotateKey",
            405,
            "HTTP Status 405 – Method Not Allowed\n" + StringUtilsRepeat("y", 3000));
    assertTrue(msg.contains("PUT"));
    assertTrue(msg.contains("https://localhost:8443/feeds/rss/rotateKey"));
    assertTrue(msg.contains("HTTP 405"));
    assertTrue(msg.contains("Method Not Allowed") || msg.contains("405"));
    assertTrue(
        msg.length() < 800,
        "ERROR message must stay short (no multi-KB body): len=" + msg.length());
  }

  @Test
  public void formatRotateKeyFailureWarnIsActionable() {
    String warn =
        PSDeliveryHttpErrorSupport.formatRotateKeyFailureWarn(
            "PUT",
            "https://localhost:8443/feeds/rss/rotateKey",
            405,
            "HTTP Status 405 – Method Not Allowed");
    assertTrue(warn.contains("PUT"));
    assertTrue(warn.contains("https://localhost:8443/feeds/rss/rotateKey"));
    assertTrue(warn.contains("HTTP 405"));
    assertTrue(warn.contains("feeds"));
    assertTrue(warn.contains("deliverymanager"));
    assertTrue(warn.contains("availableServices"));
    assertTrue(warn.contains("encryption keys may be out of sync"));
  }

  @Test
  public void formatRotateKeyFailureWarnHandlesUnknownStatus() {
    String warn =
        PSDeliveryHttpErrorSupport.formatRotateKeyFailureWarn(
            "PUT", "https://localhost:8443/feeds/rss/rotateKey", -1, "Unable to connect");
    assertTrue(warn.contains("no HTTP status") || warn.contains("transport"));
    assertTrue(warn.contains("Unable to connect"));
  }

  @Test
  public void shortReasonFromStructuredExceptionPrefersSnippet() {
    PSDeliveryClientException ex =
        new PSDeliveryClientException(
            "Error when executing method : PUT https://localhost:8443/feeds/rss/rotateKey (HTTP 405): HTTP Status 405",
            405,
            "PUT",
            "https://localhost:8443/feeds/rss/rotateKey",
            "HTTP Status 405 – Method Not Allowed");
    assertEquals(
        "HTTP Status 405 – Method Not Allowed",
        PSDeliveryHttpErrorSupport.shortReasonFromException(ex));
    assertEquals(405, ex.getStatusCode());
    assertEquals("PUT", ex.getHttpMethod());
    assertEquals("https://localhost:8443/feeds/rss/rotateKey", ex.getRequestUrl());
  }

  @Test
  public void firstLineSnippetBlankIsEmpty() {
    assertEquals("", PSDeliveryHttpErrorSupport.firstLineSnippet(null, 100));
    assertEquals("", PSDeliveryHttpErrorSupport.firstLineSnippet("   ", 100));
  }

  /** Avoid pulling in StringUtils.repeat dependency quirks; tiny local helper for tests. */
  private static String StringUtilsRepeat(String s, int n) {
    StringBuilder sb = new StringBuilder(s.length() * n);
    for (int i = 0; i < n; i++) {
      sb.append(s);
    }
    return sb.toString();
  }
}
