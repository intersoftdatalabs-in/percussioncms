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
package com.intsof.percussioncms.auditlog.redact;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Redacts sensitive material from user and log messages (and attribute maps) before dual-write.
 *
 * <p>Applied to <em>both</em> userMessage and logMessage channels.
 */
public final class AuditRedactor {

  public static final String REDACTED = "[REDACTED]";

  private static final int MAX_FIELD_LENGTH = 4000;

  private static final Pattern PASSWORD_KV =
      Pattern.compile(
          "(?i)(password|passwd|pwd|secret|token|api[_-]?key|authorization|bearer)\\s*([:=])\\s*([^\\s,;]+)");

  private static final Pattern JWT =
      Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");

  private static final Pattern BASIC_AUTH = Pattern.compile("(?i)(basic)\\s+[A-Za-z0-9+/=]{8,}");

  private static final Pattern URL_CREDENTIALS =
      Pattern.compile("(?i)(://)([^/@\\s:]+):([^/@\\s]+)@");

  private static final Pattern CONNECTION_PASSWORD =
      Pattern.compile("(?i)(password|pwd)=([^;&\\s]+)");

  /** Redact a single string value. {@code null} becomes empty string after redaction pipeline. */
  public String redact(String input) {
    if (input == null || input.isEmpty()) {
      return input == null ? "" : input;
    }
    String s = input;
    s = PASSWORD_KV.matcher(s).replaceAll("$1$2" + REDACTED);
    s = CONNECTION_PASSWORD.matcher(s).replaceAll("$1=" + REDACTED);
    s = URL_CREDENTIALS.matcher(s).replaceAll("$1" + REDACTED + ":" + REDACTED + "@");
    s = JWT.matcher(s).replaceAll(REDACTED);
    s = BASIC_AUTH.matcher(s).replaceAll("$1 " + REDACTED);
    if (s.length() > MAX_FIELD_LENGTH) {
      s = s.substring(0, MAX_FIELD_LENGTH) + "…";
    }
    return s;
  }

  /** Redact each value in a map; keys are preserved as-is. */
  public Map<String, String> redactAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return Map.of();
    }
    Map<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<String, String> e : attributes.entrySet()) {
      String key = Objects.requireNonNull(e.getKey(), "attribute key");
      if (isSensitiveKey(key)) {
        out.put(key, REDACTED);
      } else {
        out.put(key, redact(e.getValue()));
      }
    }
    return out;
  }

  /** Redact params used for template substitution (passwords never enter messages). */
  public Object[] redactParams(Object... params) {
    if (params == null || params.length == 0) {
      return params == null ? new Object[0] : params;
    }
    Object[] out = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      Object p = params[i];
      if (p == null) {
        out[i] = null;
      } else if (p instanceof String s) {
        out[i] = redact(s);
      } else {
        out[i] = redact(String.valueOf(p));
      }
    }
    return out;
  }

  private static boolean isSensitiveKey(String key) {
    String k = key.toLowerCase();
    return k.contains("password")
        || k.contains("passwd")
        || k.contains("secret")
        || k.contains("token")
        || k.contains("apikey")
        || k.contains("api_key")
        || k.contains("authorization")
        || k.contains("credential");
  }
}
