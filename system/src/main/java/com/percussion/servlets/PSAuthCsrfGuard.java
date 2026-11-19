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
package com.percussion.servlets;

import com.percussion.server.PSServer;
import com.percussion.services.auth.account.impl.PSAccountSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

/**
 * Origin, {@code Sec-Fetch-Site}, and constant-time session CSRF validation for unauthenticated
 * {@code /auth/*} mutation endpoints (including TLS reverse-proxy deployments).
 */
public class PSAuthCsrfGuard {

  public static final String SESSION_ATTR = "perc.auth.csrfToken";
  public static final String HEADER_NAME = "X-Perc-Auth-Csrf";
  public static final String PARAM_NAME = "authCsrfToken";
  public static final String PROP_WEBAUTHN_ALLOWED_ORIGINS = "auth.webauthn.allowedOrigins";

  private static final int TOKEN_BYTES = 32;
  private static final Set<String> ALLOWED_FETCH_SITES = Set.of("same-origin", "same-site", "none");

  private final Supplier<Properties> serverPropsSupplier;
  private final SecureRandom secureRandom = new SecureRandom();

  public PSAuthCsrfGuard() {
    this(PSAuthCsrfGuard::defaultServerProps);
  }

  PSAuthCsrfGuard(Supplier<Properties> serverPropsSupplier) {
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
  }

  /** Ensures the caller session exists and holds a bound anti-CSRF token. */
  public String ensureSessionToken(HttpServletRequest request) {
    Objects.requireNonNull(request, "request");
    HttpSession session = request.getSession(true);
    Object existing = session.getAttribute(SESSION_ATTR);
    if (existing instanceof String str && StringUtils.isNotBlank(str)) {
      return str;
    }
    byte[] buf = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(buf);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    session.setAttribute(SESSION_ATTR, token);
    return token;
  }

  /**
   * Verifies {@code Sec-Fetch-Site}, canonical {@code Origin}/{@code Referer}, and session-bound
   * CSRF token on state-changing {@code /auth/*} requests.
   */
  public boolean validatePostRequest(HttpServletRequest request) {
    if (request == null) {
      return false;
    }
    if (!isAllowedFetchSite(request.getHeader("Sec-Fetch-Site"))) {
      return false;
    }
    if (!isAllowedOrigin(request)) {
      return false;
    }
    return matchesSessionCsrfToken(request);
  }

  private static boolean isAllowedFetchSite(String rawFetchSite) {
    if (StringUtils.isBlank(rawFetchSite)) {
      return true;
    }
    return ALLOWED_FETCH_SITES.contains(rawFetchSite.trim().toLowerCase(Locale.ROOT));
  }

  private boolean isAllowedOrigin(HttpServletRequest request) {
    String candidate = resolveOriginOrReferer(request);
    if (candidate == null) {
      return true;
    }
    String normalizedCandidate = normalizeOrigin(candidate);
    if (normalizedCandidate == null) {
      return false;
    }
    return collectAllowedOrigins(request).contains(normalizedCandidate);
  }

  private static String resolveOriginOrReferer(HttpServletRequest request) {
    String origin = StringUtils.trimToNull(request.getHeader("Origin"));
    if (origin != null) {
      return origin;
    }
    return StringUtils.trimToNull(request.getHeader("Referer"));
  }

  public Set<String> collectAllowedOrigins(HttpServletRequest request) {
    Set<String> allowed = new LinkedHashSet<>();
    if (request != null) {
      addNormalizedOrigin(allowed, requestOrigin(request));
    }

    Properties props = safeProps(serverPropsSupplier.get());
    addNormalizedOrigin(
        allowed, props.getProperty(PSAccountSecurityService.PROP_PUBLIC_BASE_URL));
    addCommaSeparatedOrigins(allowed, props.getProperty(PROP_WEBAUTHN_ALLOWED_ORIGINS));

    if (request != null
        && PSServer.getServerProps() != null
        && PSServer.isRequestBehindProxy(request)) {
      addNormalizedOrigin(allowed, PSServer.getProxyURL(request, false));
    }
    return allowed;
  }

  private static void addCommaSeparatedOrigins(Set<String> target, String rawCsv) {
    if (StringUtils.isBlank(rawCsv)) {
      return;
    }
    for (String part : rawCsv.split(",")) {
      addNormalizedOrigin(target, part);
    }
  }

  private static String requestOrigin(HttpServletRequest request) {
    String scheme = StringUtils.trimToEmpty(request.getScheme());
    String host = StringUtils.trimToEmpty(request.getServerName());
    int port = request.getServerPort();
    if (scheme.isEmpty() || host.isEmpty()) {
      return null;
    }
    return port > 0 ? scheme + "://" + host + ":" + port : scheme + "://" + host;
  }

  private static void addNormalizedOrigin(Set<String> target, String rawUrl) {
    String normalized = normalizeOrigin(rawUrl);
    if (normalized != null) {
      target.add(normalized);
    }
  }

  public static String normalizeOrigin(String rawUrl) {
    if (StringUtils.isBlank(rawUrl)) {
      return null;
    }
    try {
      URI uri = new URI(rawUrl.trim());
      String scheme = StringUtils.lowerCase(uri.getScheme(), Locale.ROOT);
      String host = StringUtils.lowerCase(uri.getHost(), Locale.ROOT);
      if (StringUtils.isBlank(scheme) || StringUtils.isBlank(host)) {
        return null;
      }
      int port = effectivePort(scheme, uri.getPort());
      return port > 0 ? scheme + "://" + host + ":" + port : scheme + "://" + host;
    } catch (URISyntaxException ex) {
      return null;
    }
  }

  private static int effectivePort(String scheme, int rawPort) {
    if (rawPort <= 0) {
      return -1;
    }
    if ("http".equals(scheme) && rawPort == 80) {
      return -1;
    }
    if ("https".equals(scheme) && rawPort == 443) {
      return -1;
    }
    return rawPort;
  }

  private static boolean matchesSessionCsrfToken(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return false;
    }
    Object expectedObj = session.getAttribute(SESSION_ATTR);
    if (!(expectedObj instanceof String expected) || StringUtils.isBlank(expected)) {
      return false;
    }
    String presented = StringUtils.trimToNull(request.getHeader(HEADER_NAME));
    if (presented == null) {
      presented = StringUtils.trimToNull(request.getParameter(PARAM_NAME));
    }
    if (presented == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
  }

  private static Properties defaultServerProps() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }
}
