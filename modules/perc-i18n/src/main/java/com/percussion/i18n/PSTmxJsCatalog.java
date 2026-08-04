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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.i18n;

import com.percussion.security.validation.XSSValidation;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the accepted TMX key/value catalog and JS/JSON payloads that {@code tmx.jsp} emits for the
 * WebUI (and other clients). Extracted from the JSP so the {@code sys_lang=hi-in} / {@code
 * prefix=perc.ui.} path can be regression-tested without a servlet container (GH-1611).
 *
 * <p>Escape failures must never surface as HTTP 500 on the catalog endpoint: a single bad segment
 * is replaced with a safe fallback escape rather than aborting the whole map.
 */
public final class PSTmxJsCatalog {

  private PSTmxJsCatalog() {
    // utility
  }

  /**
   * Whether {@code key} matches any of the configured prefixes (same rules as {@code tmx.jsp}).
   *
   * @param prefixes non-null array of prefix strings; null elements are ignored
   * @param key message key; {@code null} is never accepted
   * @return {@code true} if the key should be included in the catalog
   */
  public static boolean accept(String[] prefixes, String key) {
    if (key == null || prefixes == null) {
      return false;
    }
    for (String prefix : prefixes) {
      if (prefix != null && key.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Collects accepted raw (unescaped) messages for the given locale and prefix filter — the same
   * map {@code tmx.jsp} builds before mode-specific emit.
   *
   * @param bundle resource bundle, never {@code null}
   * @param language BCP-47 locale (e.g. {@code hi-in}); null/empty uses the bundle default chain
   * @param prefixParam comma-delimited prefixes, or {@code *} for all keys; null/empty defaults to
   *     {@code javascript.} (legacy JSP default)
   * @return ordered map of raw key → raw value; never {@code null} (empty when no keys / null
   *     iterator)
   */
  public static Map<String, String> collectAccepted(
      PSTmxResourceBundle bundle, String language, String prefixParam) {
    Objects.requireNonNull(bundle, "bundle");
    String prefix = prefixParam;
    if (prefix == null || prefix.isEmpty()) {
      prefix = "javascript.";
    }
    String[] prefixes = prefix.split(",");
    boolean acceptAll = "*".equals(prefix);

    Map<String, String> accepted = new LinkedHashMap<>();
    Iterator<String> keys = bundle.getKeys(language);
    if (keys == null) {
      return accepted;
    }
    while (keys.hasNext()) {
      String key = keys.next();
      if (key == null) {
        continue;
      }
      if (!acceptAll && !accept(prefixes, key)) {
        continue;
      }
      String val = bundle.getString(key, language);
      accepted.put(key, val == null ? "" : val);
    }
    return accepted;
  }

  /**
   * Escapes a string for embedding inside a double-quoted JavaScript / JSON string literal. Never
   * returns {@code null}; never throws for ordinary catalog text (including Devanagari, quotes,
   * newlines, and backslashes).
   *
   * @param input may be {@code null}
   * @return escaped string, never {@code null}
   */
  public static String escapeJs(String input) {
    if (input == null || input.isEmpty()) {
      return input == null ? "" : input;
    }
    try {
      String escaped = XSSValidation.escapeJavaScript(input);
      return escaped == null ? "" : escaped;
    } catch (RuntimeException ex) {
      // Defensive: catalog endpoint must stay HTTP 200 even if an edge value upsets the escaper.
      return fallbackEscape(input);
    }
  }

  /**
   * Emits comma-separated {@code "key": "value"} entries suitable for a JS object literal body
   * (without surrounding braces). Stable iteration order of {@code accepted} is preserved.
   *
   * @param accepted raw key/value map; never {@code null}
   * @return object-entry source; never {@code null}
   */
  public static String toJsObjectEntries(Map<String, String> accepted) {
    Objects.requireNonNull(accepted, "accepted");
    StringBuilder sb = new StringBuilder(Math.max(64, accepted.size() * 32));
    boolean first = true;
    for (Map.Entry<String, String> e : accepted.entrySet()) {
      if (e.getKey() == null) {
        continue;
      }
      if (!first) {
        sb.append(',');
      }
      first = false;
      sb.append('"')
          .append(escapeJs(e.getKey()))
          .append("\": \"")
          .append(escapeJs(e.getValue()))
          .append('"');
    }
    return sb.toString();
  }

  /**
   * Emits {@code {"tmxmessages": {"k":"v",...}}} JSON used by {@code tmx.jsp} {@code mode=json}.
   *
   * @param accepted raw key/value map; never {@code null}
   * @return JSON document; never {@code null}
   */
  public static String toJsonDocument(Map<String, String> accepted) {
    Objects.requireNonNull(accepted, "accepted");
    StringBuilder sb = new StringBuilder(Math.max(64, accepted.size() * 32));
    sb.append("{\"tmxmessages\": {");
    boolean first = true;
    for (Map.Entry<String, String> e : accepted.entrySet()) {
      if (e.getKey() == null) {
        continue;
      }
      if (!first) {
        sb.append(',');
      }
      first = false;
      sb.append('"')
          .append(escapeJs(e.getKey()))
          .append("\": \"")
          .append(escapeJs(e.getValue()))
          .append('"');
    }
    sb.append("}}");
    return sb.toString();
  }

  /**
   * Minimal fallback when {@link XSSValidation#escapeJavaScript(String)} fails. Escapes backslash,
   * quotes, and common control characters only.
   */
  static String fallbackEscape(String input) {
    StringBuilder sb = new StringBuilder(input.length() + 16);
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      switch (c) {
        case '\\':
          sb.append("\\\\");
          break;
        case '"':
          sb.append("\\\"");
          break;
        case '\'':
          sb.append("\\'");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '\u2028':
          sb.append("\\u2028");
          break;
        case '\u2029':
          sb.append("\\u2029");
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }
}
