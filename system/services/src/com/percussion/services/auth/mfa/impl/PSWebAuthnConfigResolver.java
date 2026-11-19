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
package com.percussion.services.auth.mfa.impl;

import com.webauthn4j.data.client.Origin;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Resolves Relying Party ID, allowed origins, and user verification rules from {@code
 * server.properties} and fails closed for unconfigured non-localhost origins.
 */
public final class PSWebAuthnConfigResolver {

  public static final String PROP_RP_ID = "auth.webauthn.rpId";
  public static final String PROP_RP_NAME = "auth.webauthn.rpName";
  public static final String PROP_ALLOWED_ORIGINS = "auth.webauthn.allowedOrigins";
  public static final String PROP_USER_VERIFICATION = "auth.webauthn.userVerification";
  public static final String PROP_PUBLIC_BASE_URL = "auth.publicBaseUrl";

  public record PSWebAuthnRpConfig(
      String rpId, String rpName, Set<Origin> allowedOrigins, boolean registrationUvRequired) {}

  /** Resolves the effective WebAuthn Relying Party config for the current server properties. */
  public PSWebAuthnRpConfig resolve(Properties props, String requestOrigin) {
    Properties safeProps = props != null ? props : new Properties();
    String rpId = StringUtils.trimToEmpty(safeProps.getProperty(PROP_RP_ID)).toLowerCase(Locale.ROOT);
    String rpName =
        StringUtils.defaultIfBlank(
            StringUtils.trimToNull(safeProps.getProperty(PROP_RP_NAME)), "Percussion CMS");
    boolean uvRequired =
        "required"
            .equalsIgnoreCase(
                StringUtils.trimToEmpty(safeProps.getProperty(PROP_USER_VERIFICATION)));

    Set<Origin> configuredOrigins =
        parseOriginsCsv(safeProps.getProperty(PROP_ALLOWED_ORIGINS));
    Origin baseOrigin = parseSingleOrigin(safeProps.getProperty(PROP_PUBLIC_BASE_URL));

    if (!rpId.isEmpty()) {
      return resolveExplicitRp(rpId, rpName, uvRequired, configuredOrigins, baseOrigin, requestOrigin);
    }
    if (baseOrigin != null) {
      return resolveFromPublicBase(rpName, uvRequired, configuredOrigins, baseOrigin);
    }
    return resolveLocalhostFallback(rpName, uvRequired, requestOrigin);
  }

  private static PSWebAuthnRpConfig resolveExplicitRp(
      String rpId,
      String rpName,
      boolean uvRequired,
      Set<Origin> configuredOrigins,
      Origin baseOrigin,
      String requestOrigin) {
    validateRpIdHostname(rpId);
    Set<Origin> origins = new LinkedHashSet<>(configuredOrigins);
    if (origins.isEmpty() && baseOrigin != null) {
      origins.add(baseOrigin);
    }
    if (origins.isEmpty()) {
      Origin req = parseSingleOrigin(requestOrigin);
      if (req != null && isLoopbackHost(rpId) && isLoopbackHost(req.getHost())) {
        origins.add(req);
      }
    }
    if (origins.isEmpty()) {
      throw new IllegalStateException(
          "WebAuthn requires auth.webauthn.allowedOrigins or auth.publicBaseUrl when auth.webauthn.rpId is set.");
    }
    for (Origin origin : origins) {
      validateOriginForRp(origin, rpId);
    }
    return new PSWebAuthnRpConfig(rpId, rpName, Set.copyOf(origins), uvRequired);
  }

  private static PSWebAuthnRpConfig resolveFromPublicBase(
      String rpName, boolean uvRequired, Set<Origin> configuredOrigins, Origin baseOrigin) {
    String rpId = baseOrigin.getHost().toLowerCase(Locale.ROOT);
    Set<Origin> origins = new LinkedHashSet<>(configuredOrigins);
    if (origins.isEmpty()) {
      origins.add(baseOrigin);
    }
    for (Origin origin : origins) {
      validateOriginForRp(origin, rpId);
    }
    return new PSWebAuthnRpConfig(rpId, rpName, Set.copyOf(origins), uvRequired);
  }

  private static PSWebAuthnRpConfig resolveLocalhostFallback(
      String rpName, boolean uvRequired, String requestOrigin) {
    Origin req = parseSingleOrigin(requestOrigin);
    if (req == null || !isLoopbackHost(req.getHost())) {
      throw new IllegalStateException(
          "Passkeys require auth.webauthn.rpId and auth.webauthn.allowedOrigins (or auth.publicBaseUrl) for non-localhost hosts.");
    }
    String rpId = req.getHost().toLowerCase(Locale.ROOT);
    return new PSWebAuthnRpConfig(rpId, rpName, Set.of(req), uvRequired);
  }

  private static Set<Origin> parseOriginsCsv(String raw) {
    Set<Origin> origins = new LinkedHashSet<>();
    if (StringUtils.isBlank(raw)) {
      return origins;
    }
    for (String token : raw.split(",")) {
      String clean = token.trim();
      if (!clean.isEmpty()) {
        Origin parsed = parseSingleOrigin(clean);
        if (parsed == null) {
          throw new IllegalStateException("Invalid origin URI in auth.webauthn.allowedOrigins: " + clean);
        }
        origins.add(parsed);
      }
    }
    return origins;
  }

  public static Origin parseSingleOrigin(String raw) {
    if (StringUtils.isBlank(raw)) {
      return null;
    }
    try {
      URI uri = URI.create(raw.trim());
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if (StringUtils.isBlank(scheme) || StringUtils.isBlank(host)) {
        return null;
      }
      int port = uri.getPort();
      String normalized =
          port > 0
              ? scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT) + ":" + port
              : scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT);
      return Origin.create(normalized);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static void validateRpIdHostname(String rpId) {
    if (rpId.contains("://")
        || rpId.contains("/")
        || rpId.contains(":")
        || rpId.chars().anyMatch(Character::isWhitespace)) {
      throw new IllegalStateException(
          "auth.webauthn.rpId must be a hostname without scheme, port, or path.");
    }
  }

  private static void validateOriginForRp(Origin origin, String rpId) {
    String scheme = StringUtils.trimToEmpty(origin.getScheme()).toLowerCase(Locale.ROOT);
    String host = StringUtils.trimToEmpty(origin.getHost()).toLowerCase(Locale.ROOT);
    if (!"https".equals(scheme) && !("http".equals(scheme) && isLoopbackHost(host) && isLoopbackHost(rpId))) {
      throw new IllegalStateException(
          "WebAuthn origin must use https:// (http:// is allowed for localhost development only): " + origin);
    }
    if (!hostMatchesRpId(host, rpId)) {
      throw new IllegalStateException(
          "WebAuthn origin host '" + host + "' does not match rpId '" + rpId + "'.");
    }
  }

  private static boolean hostMatchesRpId(String host, String rpId) {
    return host.equalsIgnoreCase(rpId)
        || host.toLowerCase(Locale.ROOT).endsWith("." + rpId.toLowerCase(Locale.ROOT));
  }

  public static boolean isLoopbackHost(String host) {
    if (StringUtils.isBlank(host)) {
      return false;
    }
    String clean = host.trim().toLowerCase(Locale.ROOT);
    return "localhost".equals(clean) || "127.0.0.1".equals(clean);
  }
}
