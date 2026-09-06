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

package com.percussion.services.pipeline.http;

import com.percussion.security.validation.URLValidation;
import com.percussion.services.pipeline.PSPipelineIrException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * SSRF fail-closed URL guard for pipeline HTTP datasources: http(s) loopback only, no userinfo,
 * no cloud/metadata hosts, no open redirects off loopback.
 *
 * <p>The bundled local fixture URL {@link #BUNDLED_FIXTURE_URL} is a portable loopback token
 * resolved from the classpath (no live internet).
 */
public final class PSPipelineHttpUrl {

  /** Portable local fixture URL (no port). Resolved from classpath, not fetched from the wire. */
  public static final String BUNDLED_FIXTURE_URL = "http://127.0.0.1/pipeline-http-fixture";

  /** Path of {@link #BUNDLED_FIXTURE_URL}. */
  public static final String BUNDLED_FIXTURE_PATH = "/pipeline-http-fixture";

  static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

  private PSPipelineHttpUrl() {}

  /**
   * Validate operator-configured HTTP datasource URL.
   *
   * @return validated URL, never {@code null}
   */
  public static URL requireSafe(String urlString) throws PSPipelineIrException {
    if (StringUtils.isBlank(urlString)) {
      throw new PSPipelineIrException("HTTP datasource URL is required");
    }
    String raw = urlString.trim();
    if (raw.indexOf('\0') >= 0) {
      throw new PSPipelineIrException("HTTP datasource URL must not contain NUL");
    }
    URI parsed;
    try {
      parsed = new URI(raw);
    } catch (URISyntaxException e) {
      throw new PSPipelineIrException("HTTP datasource URL is not a valid URL", e);
    }
    String protocol = parsed.getScheme();
    if (protocol == null
        || (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol))) {
      throw new PSPipelineIrException(
          "HTTP datasource URL must be http or https (loopback/local fixture only)");
    }
    if (parsed.getUserInfo() != null && !parsed.getUserInfo().isBlank()) {
      throw new PSPipelineIrException(
          "HTTP datasource URL must not contain userinfo (no credentials in the URL)");
    }
    URL validated;
    try {
      validated = URLValidation.validateURLString(raw);
    } catch (MalformedURLException e) {
      throw new PSPipelineIrException("HTTP datasource URL is not a valid URL", e);
    } catch (IllegalArgumentException | SecurityException e) {
      throw new PSPipelineIrException(
          "HTTP datasource URL rejected (SSRF fail-closed): " + e.getMessage(), e);
    }
    if (!isLiteralLoopback(validated.getHost())) {
      throw new PSPipelineIrException(
          "HTTP datasource URL must be loopback or the local fixture (no cloud hosts). Rejected host: "
              + validated.getHost());
    }
    return validated;
  }

  /** True when the URL is the bundled classpath fixture (any loopback host, no port required). */
  public static boolean isBundledFixture(URL url) {
    if (url == null) {
      return false;
    }
    if (!isLiteralLoopback(url.getHost())) {
      return false;
    }
    String path = url.getPath();
    if (path == null) {
      return false;
    }
    String normalized = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    return BUNDLED_FIXTURE_PATH.equals(normalized);
  }

  static boolean isLiteralLoopback(String host) {
    if (host == null || host.isBlank()) {
      return false;
    }
    String normalized = host.trim().toLowerCase(Locale.ROOT).replace("[", "").replace("]", "");
    return LOOPBACK_HOSTS.contains(normalized)
        || LOOPBACK_HOSTS.contains(host.trim().toLowerCase(Locale.ROOT));
  }

  static URI toRequestUri(URL validated) throws PSPipelineIrException {
    String protocol = "https".equalsIgnoreCase(validated.getProtocol()) ? "https" : "http";
    String path = validated.getPath();
    if (path == null || path.isBlank()) {
      path = "/";
    }
    try {
      return new URI(
          protocol, null, validated.getHost(), validated.getPort(), path, validated.getQuery(), null);
    } catch (URISyntaxException e) {
      throw new PSPipelineIrException("HTTP datasource URL could not be rebuilt as a request URI", e);
    }
  }

  static String redact(URL url) {
    if (url == null) {
      return "";
    }
    try {
      return toRequestUri(url).toString();
    } catch (PSPipelineIrException e) {
      return url.getProtocol() + "://" + url.getHost();
    }
  }
}
