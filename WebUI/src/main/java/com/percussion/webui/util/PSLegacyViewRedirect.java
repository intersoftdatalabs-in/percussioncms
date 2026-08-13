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
package com.percussion.webui.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds bookmark redirects from retired classic JSPs into the SPA app entry.
 *
 * <p>Always forces {@code view=} to the requested SPA key so an incoming {@code view=admin}
 * cannot bypass Design (or other) retirement. Extra query pairs are kept only when name and
 * value are URL-safe; markup, quotes, and CR/LF are dropped (Location + HTML fallback).
 */
public final class PSLegacyViewRedirect {

  /** Relative SPA application root used as the redirect Location prefix. */
  public static final String SPA_APP = "/cm/app/";

  private static final Pattern VIEW_TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,31}");
  private static final Pattern PARAM_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]{0,63}");
  private static final Pattern PARAM_VALUE = Pattern.compile("[A-Za-z0-9._~%+\\-]{0,512}");

  private PSLegacyViewRedirect() {}

  /**
   * Location path + query for the SPA host. Never null. Always starts with {@link #SPA_APP} and
   * includes {@code view=} set to {@code forcedView} (invalid view tokens fall back to {@code
   * home}).
   *
   * @param forcedView SPA {@code view} key (e.g. {@code design}); may be null
   * @param rawQueryString {@code request.getQueryString()}; may be null
   * @return relative Location value (no CR/LF)
   */
  public static String buildLocation(String forcedView, String rawQueryString) {
    String view = sanitizeView(forcedView);
    StringBuilder qs = new StringBuilder("view=").append(view);
    if (rawQueryString != null && !rawQueryString.isEmpty()) {
      for (String rawPair : rawQueryString.split("&", -1)) {
        if (rawPair == null || rawPair.isEmpty()) {
          continue;
        }
        int eq = rawPair.indexOf('=');
        String name = eq < 0 ? rawPair : rawPair.substring(0, eq);
        String value = eq < 0 ? "" : rawPair.substring(eq + 1);
        if (!PARAM_NAME.matcher(name).matches()) {
          continue;
        }
        if ("view".equalsIgnoreCase(name)) {
          continue;
        }
        if (!PARAM_VALUE.matcher(value).matches()) {
          continue;
        }
        qs.append('&').append(name);
        if (eq >= 0) {
          qs.append('=').append(value);
        }
      }
    }
    return SPA_APP + "?" + qs;
  }

  /**
   * HTML-attribute escape for meta-refresh and {@code href} fallbacks. Use the unescaped {@link
   * #buildLocation(String, String)} result for the {@code Location} header.
   *
   * @param value may be null (treated as empty)
   * @return never null
   */
  public static String escapeHtmlAttribute(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    StringBuilder out = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&':
          out.append("&amp;");
          break;
        case '<':
          out.append("&lt;");
          break;
        case '>':
          out.append("&gt;");
          break;
        case '"':
          out.append("&quot;");
          break;
        case '\'':
          out.append("&#39;");
          break;
        default:
          out.append(c);
      }
    }
    return out.toString();
  }

  private static String sanitizeView(String forcedView) {
    if (forcedView == null) {
      return PSDefaultLandingView.VIEW_HOME;
    }
    String trimmed = forcedView.trim();
    if (!VIEW_TOKEN.matcher(trimmed).matches()) {
      return PSDefaultLandingView.VIEW_HOME;
    }
    return trimmed.toLowerCase(Locale.ROOT);
  }
}
