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

package com.percussion.services.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fail-closed sanitizer for pipeline request traces. Passwords, tokens, Authorization values,
 * and credential-shaped strings are replaced with {@link #REDACTED}.
 */
public final class PSPipelineRequestTraceSanitizer {

  public static final String REDACTED = "[REDACTED]";

  private static final int MAX_DEPTH = 8;

  private static final Pattern SENSITIVE_KEY =
      Pattern.compile(
          "(?i)(pass(word|wd)?|pwd|secret|token|authorization|auth(entication)?|api[_-]?key|access[_-]?key|credential)");

  private static final Pattern AUTH_VALUE =
      Pattern.compile("(?i)^(basic|bearer|digest)\\s+\\S+");

  private PSPipelineRequestTraceSanitizer() {}

  /**
   * @return {@code true} when a map/param key must never appear with its original value
   */
  public static boolean isSensitiveKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return SENSITIVE_KEY.matcher(key.trim()).find();
  }

  /**
   * Deep-copy {@code params} with sensitive keys and auth-shaped values redacted.
   *
   * @param params may be {@code null}
   * @return never {@code null}; never the same instance as {@code params}
   */
  public static Map<String, Object> sanitizeParams(Map<String, ?> params) {
    Map<String, Object> out = new LinkedHashMap<>();
    if (params == null || params.isEmpty()) {
      return out;
    }
    for (Map.Entry<String, ?> e : params.entrySet()) {
      String key = e.getKey();
      out.put(key, sanitizeValue(key, e.getValue(), 0));
    }
    return out;
  }

  static Object sanitizeValue(String key, Object value, int depth) {
    if (isSensitiveKey(key)) {
      return REDACTED;
    }
    if (value == null) {
      return null;
    }
    if (depth >= MAX_DEPTH) {
      return REDACTED;
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> nested = new LinkedHashMap<>();
      for (Map.Entry<?, ?> e : map.entrySet()) {
        String childKey = e.getKey() != null ? String.valueOf(e.getKey()) : "";
        nested.put(childKey, sanitizeValue(childKey, e.getValue(), depth + 1));
      }
      return nested;
    }
    if (value instanceof List<?> list) {
      List<Object> nested = new ArrayList<>(list.size());
      for (Object item : list) {
        nested.add(sanitizeValue(key, item, depth + 1));
      }
      return nested;
    }
    if (value instanceof String s) {
      return sanitizeString(s);
    }
    return value;
  }

  static String sanitizeString(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return raw;
    }
    if (AUTH_VALUE.matcher(trimmed).matches()) {
      return REDACTED;
    }
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.startsWith("authorization:") || lower.contains("authorization=")) {
      return REDACTED;
    }
    return raw;
  }
}
