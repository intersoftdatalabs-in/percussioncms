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
package com.percussion.delivery.client;

import org.apache.commons.lang3.StringUtils;

/**
 * Helpers for operator-friendly delivery-client failure messages: full action URL + HTTP method +
 * status without dumping multi-KB Tomcat HTML error pages into logs.
 *
 * <p>Used by {@link PSDeliveryClient} and rotateKey / admin push callers such as {@code
 * PSDeliveryInfoService}.
 */
public final class PSDeliveryHttpErrorSupport {

  /** Default max length of a response-body snippet included in ERROR/WARN lines. */
  public static final int DEFAULT_BODY_SNIPPET_MAX = 200;

  /**
   * Actionable operator guidance when security-key rotation push fails. Publish may continue but
   * CMS/DTS keys can drift.
   */
  public static final String ROTATE_KEY_OPERATOR_HINT =
      "Check DTS feeds app on admin port, deliverymanager credentials, and that availableServices"
          + " includes feeds. Publish may continue but encryption keys may be out of sync.";

  private PSDeliveryHttpErrorSupport() {
    // utility
  }

  /**
   * Joins an admin base URL with an action path (e.g. {@code /feeds/rss/rotateKey}) without
   * introducing double slashes.
   *
   * @param adminBase admin host URL from delivery config; may be blank
   * @param actionPath action path; may be blank
   * @return combined URL, never {@code null}
   */
  public static String joinAdminActionUrl(String adminBase, String actionPath) {
    String base = StringUtils.trimToEmpty(adminBase);
    String path = StringUtils.trimToEmpty(actionPath);
    if (base.isEmpty()) {
      return path;
    }
    if (path.isEmpty()) {
      return base;
    }
    boolean baseEndsWithSlash = base.endsWith("/");
    boolean pathStartsWithSlash = path.startsWith("/");
    if (baseEndsWithSlash && pathStartsWithSlash) {
      return base + path.substring(1);
    }
    if (!baseEndsWithSlash && !pathStartsWithSlash) {
      return base + "/" + path;
    }
    return base + path;
  }

  /**
   * Returns a short, single-line snippet of an HTTP response body suitable for logs. Strips CR/LF
   * noise, collapses whitespace for HTML-ish bodies, and caps length so Tomcat error pages do not
   * flood logs.
   *
   * @param body raw response body; may be {@code null}
   * @param maxLen maximum characters; values &lt;= 0 use {@link #DEFAULT_BODY_SNIPPET_MAX}
   * @return snippet, never {@code null} (empty when body blank)
   */
  public static String firstLineSnippet(String body, int maxLen) {
    int limit = maxLen > 0 ? maxLen : DEFAULT_BODY_SNIPPET_MAX;
    if (StringUtils.isBlank(body)) {
      return "";
    }
    String trimmed = body.trim();
    // Prefer the first non-empty line (status text / short reason), not multi-line HTML.
    String firstLine = trimmed;
    int nl = indexOfAnyLineBreak(trimmed);
    if (nl >= 0) {
      firstLine = trimmed.substring(0, nl).trim();
      if (firstLine.isEmpty()) {
        // Leading blank lines (common in some servlet error pages) — take next content line.
        String rest = trimmed.substring(nl).trim();
        int nl2 = indexOfAnyLineBreak(rest);
        firstLine = (nl2 >= 0 ? rest.substring(0, nl2) : rest).trim();
      }
    }
    if (firstLine.isEmpty()) {
      firstLine = trimmed.replace('\r', ' ').replace('\n', ' ').trim();
    }
    // Collapse internal whitespace so HTML dumps stay one short log token.
    firstLine = firstLine.replaceAll("\\s+", " ").trim();
    if (firstLine.length() > limit) {
      return firstLine.substring(0, limit) + "...";
    }
    return firstLine;
  }

  /**
   * Formats the ERROR message thrown/logged when a delivery HTTP call returns a non-success status.
   *
   * @param method HTTP method label (e.g. PUT)
   * @param url full request URL
   * @param statusCode HTTP status code
   * @param responseBody raw body; may be HTML
   * @return message including method, URL, status, and short reason
   */
  public static String formatExecutionError(
      String method, String url, int statusCode, String responseBody) {
    String methodLabel = StringUtils.defaultIfBlank(method, "?");
    String urlLabel = StringUtils.defaultIfBlank(url, "?");
    String reason = firstLineSnippet(responseBody, DEFAULT_BODY_SNIPPET_MAX);
    if (reason.isEmpty()) {
      return String.format(
          "Error when executing method : %s %s (HTTP %d)", methodLabel, urlLabel, statusCode);
    }
    return String.format(
        "Error when executing method : %s %s (HTTP %d): %s",
        methodLabel, urlLabel, statusCode, reason);
  }

  /**
   * Formats an actionable WARN when {@code PUT …/feeds/rss/rotateKey} (or similar) fails.
   *
   * @param method HTTP method (typically PUT)
   * @param fullUrl full action URL
   * @param statusCode HTTP status, or negative when unknown (e.g. connect failure)
   * @param shortReason truncated reason / status text; may be blank
   * @return single-line WARN text for operators
   */
  public static String formatRotateKeyFailureWarn(
      String method, String fullUrl, int statusCode, String shortReason) {
    String methodLabel = StringUtils.defaultIfBlank(method, "PUT");
    String urlLabel = StringUtils.defaultIfBlank(fullUrl, "?");
    String statusPart =
        statusCode > 0 ? String.format("HTTP %d", statusCode) : "no HTTP status (transport error)";
    String reason = StringUtils.trimToEmpty(shortReason);
    if (!reason.isEmpty()) {
      return String.format(
          "Unable to %s %s (%s: %s). %s",
          methodLabel, urlLabel, statusPart, reason, ROTATE_KEY_OPERATOR_HINT);
    }
    return String.format(
        "Unable to %s %s (%s). %s", methodLabel, urlLabel, statusPart, ROTATE_KEY_OPERATOR_HINT);
  }

  /**
   * Derives a short operator-facing reason from a delivery-client exception without re-including
   * multi-line HTML.
   *
   * @param ex failure exception; may be {@code null}
   * @return short reason, never {@code null}
   */
  public static String shortReasonFromException(Throwable ex) {
    if (ex == null) {
      return "";
    }
    if (ex instanceof IPSDeliveryClient.PSDeliveryClientException pce) {
      String structured = firstLineSnippet(pce.getResponseSnippet(), DEFAULT_BODY_SNIPPET_MAX);
      if (!structured.isEmpty()) {
        return structured;
      }
    }
    return firstLineSnippet(ex.getMessage(), DEFAULT_BODY_SNIPPET_MAX);
  }

  private static int indexOfAnyLineBreak(String s) {
    int n = s.indexOf('\n');
    int r = s.indexOf('\r');
    if (n < 0) {
      return r;
    }
    if (r < 0) {
      return n;
    }
    return Math.min(n, r);
  }
}
