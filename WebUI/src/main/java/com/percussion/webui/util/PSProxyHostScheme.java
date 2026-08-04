/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.webui.util;

import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Resolves the public-facing URL scheme when the CMS is behind a reverse proxy.
 *
 * <p>Dashboard gadget specs are built as absolute URLs using this scheme. A hardcoded {@code http}
 * fallback produces mixed-content URLs on HTTPS front-ends; browsers block them before the request
 * reaches the app server (symptom: "Unable to retrieve specs" with no server log). Prefer the
 * inbound request scheme when {@code proxyScheme} is unset.
 *
 * <p>Used from dashboard JSPs so the rule is unit-testable outside the JSP container.
 */
public final class PSProxyHostScheme {

  private static final Logger log = LogManager.getLogger(PSProxyHostScheme.class);

  private PSProxyHostScheme() {}

  /**
   * Resolve the public scheme for absolute URLs while {@code requestBehindProxy} is active.
   *
   * <p>When {@code configuredProxyScheme} is null/blank, returns the request scheme (HTTPS
   * preserved). When configured and it differs from the request scheme, logs WARN; logs ERROR when
   * request is HTTPS but configured scheme is HTTP (mixed-content risk for gadget specs).
   *
   * @param requestScheme scheme from {@code request.getScheme()}, may be null/blank
   * @param configuredProxyScheme value of {@code server.properties} {@code proxyScheme}, may be
   *     null/blank when unset
   * @return scheme to emit in absolute public URLs; never null (falls back to {@code "http"} only
   *     when both inputs are missing)
   */
  public static String resolveBehindProxy(String requestScheme, String configuredProxyScheme) {
    String request = normalizeScheme(requestScheme);
    String configured = normalizeScheme(configuredProxyScheme);

    if (configured != null && request != null && !configured.equalsIgnoreCase(request)) {
      log.warn(
          "requestBehindProxy is true but proxyScheme '{}' differs from request scheme '{}'. "
              + "Absolute gadget/public URLs will use proxyScheme; mismatches can cause "
              + "mixed-content failures (e.g. dashboard 'Unable to retrieve specs').",
          configured,
          request);
      if ("https".equals(request) && "http".equals(configured)) {
        log.error(
            "proxyScheme is 'http' while the inbound request scheme is 'https'. "
                + "Browsers will block mixed-content gadget specs. "
                + "Set proxyScheme=https in server.properties, or leave proxyScheme unset to "
                + "inherit the request scheme.");
      }
    }

    if (configured != null) {
      return configured;
    }
    if (request != null) {
      return request;
    }
    return "http";
  }

  /**
   * Normalize a scheme token: trim, lower-case (Locale.ROOT), empty → null.
   *
   * @param scheme raw scheme, may be null
   * @return normalized scheme or null
   */
  static String normalizeScheme(String scheme) {
    if (scheme == null) {
      return null;
    }
    String trimmed = scheme.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.toLowerCase(Locale.ROOT);
  }
}
