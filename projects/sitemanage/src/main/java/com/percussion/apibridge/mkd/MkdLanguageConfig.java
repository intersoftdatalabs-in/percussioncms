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

package com.percussion.apibridge.mkd;

import com.percussion.security.PSEncryptProperties;
import com.percussion.security.PSEncryptor;
import com.percussion.server.PSServer;
import com.percussion.utils.io.PathUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads {@code perc.mkd.language.*} / {@code perc.mkd.gcm.*} from server.properties.
 *
 * <p>Token values use the product {@code ENC(...)} convention via {@link PSEncryptProperties}.
 */
public final class MkdLanguageConfig {

  private static final Logger log = LogManager.getLogger(MkdLanguageConfig.class);

  public static final String PROP_ENABLED = "perc.mkd.language.enabled";
  public static final String PROP_ROLES = "perc.mkd.language.roles";
  public static final String PROP_GCM_HOST = "perc.mkd.gcm.host";
  public static final String PROP_GCM_PORT = "perc.mkd.gcm.port";
  public static final String PROP_GCM_URL = "perc.mkd.gcm.url";
  public static final String PROP_GCM_TOKEN = "perc.mkd.gcm.token";
  public static final String PROP_GCM_GROUP = "perc.mkd.gcm.group";
  public static final String PROP_GCM_FROM = "perc.mkd.gcm.from";

  public static final int DEFAULT_GCM_PORT = 1119;

  private MkdLanguageConfig() {}

  public static boolean isEnabled() {
    return Boolean.parseBoolean(
        StringUtils.trimToEmpty(PSServer.getProperty(PROP_ENABLED, "false")));
  }

  /**
   * Role gate when master switch is on.
   *
   * <ul>
   *   <li>Empty / missing → feature off for all (caller should WARN about Translations_Team)
   *   <li>{@code *} → all authenticated users
   *   <li>Comma-separated names → user must match at least one
   * </ul>
   */
  public static List<String> configuredRoles() {
    String raw = StringUtils.trimToEmpty(PSServer.getProperty(PROP_ROLES, ""));
    if (raw.isEmpty()) {
      return List.of();
    }
    if ("*".equals(raw)) {
      return List.of("*");
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  public static boolean rolesAllowAll() {
    List<String> roles = configuredRoles();
    return roles.size() == 1 && "*".equals(roles.get(0));
  }

  public static boolean rolesEmpty() {
    return configuredRoles().isEmpty();
  }

  public static String gcmHost() {
    return StringUtils.trimToNull(PSServer.getProperty(PROP_GCM_HOST, ""));
  }

  public static int gcmPort() {
    String p = StringUtils.trimToNull(PSServer.getProperty(PROP_GCM_PORT, ""));
    if (p == null) {
      return DEFAULT_GCM_PORT;
    }
    try {
      return Integer.parseInt(p);
    } catch (NumberFormatException e) {
      log.warn(
          "Invalid {} value '{}'; using default port {}", PROP_GCM_PORT, p, DEFAULT_GCM_PORT);
      return DEFAULT_GCM_PORT;
    }
  }

  /** Optional GCM/NNTP-style URL for {@code GcmClient.connectUrl} — not HTTP REST. */
  public static String gcmUrl() {
    return StringUtils.trimToNull(PSServer.getProperty(PROP_GCM_URL, ""));
  }

  /** Project group path (SDK routes leaves). */
  public static String gcmGroup() {
    return StringUtils.trimToNull(PSServer.getProperty(PROP_GCM_GROUP, ""));
  }

  public static String gcmFrom() {
    return StringUtils.trimToNull(PSServer.getProperty(PROP_GCM_FROM, ""));
  }

  /**
   * Decrypt PAT from server.properties. Does not log the value.
   *
   * @return plaintext token or null when the property is missing/blank
   * @throws IllegalStateException when the property is present as {@code ENC(...)} but decryption
   *     fails (wrong key / secure dir) — distinct from "not configured"
   */
  public static String gcmTokenPlain() {
    String raw = StringUtils.trimToNull(PSServer.getProperty(PROP_GCM_TOKEN, ""));
    if (raw == null) {
      return null;
    }
    if (raw.regionMatches(true, 0, "ENC(", 0, 4) && raw.endsWith(")")) {
      try {
        String secureDir =
            PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR);
        // Strip ENC(...) wrapper then decrypt with product encryptor.
        return PSEncryptProperties.decryptProperty(raw, null, secureDir, null);
      } catch (Exception e) {
        log.error("Failed to decrypt {}", PROP_GCM_TOKEN, e);
        throw new IllegalStateException(
            PROP_GCM_TOKEN
                + " is set as ENC(...) but could not be decrypted (check secure key /"
                + " secure dir)",
            e);
      }
    }
    return raw;
  }

  public static boolean userInAllowedRoles(List<String> userRoles) {
    if (rolesEmpty()) {
      return false;
    }
    if (rolesAllowAll()) {
      return true;
    }
    if (userRoles == null || userRoles.isEmpty()) {
      return false;
    }
    List<String> allowed = configuredRoles();
    for (String ur : userRoles) {
      if (ur == null) {
        continue;
      }
      for (String a : allowed) {
        if (a.equalsIgnoreCase(ur.trim())) {
          return true;
        }
      }
    }
    return false;
  }

  public static void warnIfEnabledWithoutRoles() {
    if (isEnabled() && rolesEmpty()) {
      log.warn(
          "perc.mkd.language.enabled=true but perc.mkd.language.roles is empty — "
              + "i18n corrections are OFF for all users. Add at least one role "
              + "(recommended: create a Translations_Team role and add interested users), "
              + "or set roles=* to allow all authenticated users.");
    }
  }

  public static String summaryForLog() {
    return String.format(
        Locale.ROOT,
        "enabled=%s roles=%s host=%s port=%d groupSet=%s tokenSet=%s",
        isEnabled(),
        configuredRoles(),
        gcmHost(),
        gcmPort(),
        gcmGroup() != null,
        gcmTokenPlain() != null);
  }
}
