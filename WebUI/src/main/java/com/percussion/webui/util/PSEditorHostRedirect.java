/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Builds React Content Editor SPA redirects for leftover {@code ?view=editor} /
 * {@code perc_linkback_id} / {@code editAsset.jsp} bookmarks (#3473).
 *
 * <p>Does not open leftover Data Flow CE HTML. Data Flow Server remains platform I/O.
 */
public final class PSEditorHostRedirect {

  /** SPA document path (query contract). */
  public static final String SPA_EDITOR = "/cm/app/spa.jsp";

  private PSEditorHostRedirect() {}

  /**
   * Parse a numeric content id from a raw id or GUID {@code host-type-uuid} (last segment).
   *
   * @param raw may be null
   * @return decimal content id, or {@code null} when missing/invalid
   */
  public static String parseContentId(String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.trim();
    if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
      return null;
    }
    if (s.chars().allMatch(Character::isDigit)) {
      return positiveIntToken(s);
    }
    int last = s.lastIndexOf('-');
    if (last >= 0 && last < s.length() - 1) {
      return parseContentId(s.substring(last + 1));
    }
    return null;
  }

  /**
   * Map leftover editor mode tokens onto the React host ({@code edit|view|promote}).
   *
   * @param raw may be null
   * @return never null
   */
  public static String normalizeMode(String raw) {
    if (raw == null) {
      return "edit";
    }
    String n = raw.trim().toLowerCase(Locale.ROOT);
    if (n.isEmpty()) {
      return "edit";
    }
    if ("view".equals(n)
        || "readonly".equals(n)
        || "read-only".equals(n)
        || "read_only".equals(n)) {
      return "view";
    }
    if ("promote".equals(n)) {
      return "promote";
    }
    return "edit";
  }

  /**
   * {@code spa.jsp?entry=editor} Location (optional contentId/mode). Never null.
   *
   * @param proxyURL proxy prefix; may be null
   * @param contentId parsed content id; may be null
   * @param mode already-normalized or raw mode; may be null
   * @return SPA Location starting with {@code /cm/app/spa.jsp?entry=editor}
   */
  public static String buildSpaRedirect(String proxyURL, String contentId, String mode) {
    StringBuilder qs = new StringBuilder("entry=editor");
    String cid = parseContentId(contentId);
    if (cid != null) {
      qs.append("&contentId=").append(urlEncode(cid));
    }
    String normalized = normalizeMode(mode);
    qs.append("&mode=").append(urlEncode(normalized));
    String prefix = proxyURL == null ? "" : proxyURL;
    return prefix + SPA_EDITOR + "?" + qs;
  }

  /**
   * True when {@code lowerPath} is a retired {@code editAsset.jsp} bookmark (app or pages tree).
   *
   * @param lowerPath already lower-cased webapp path; may be null
   * @return true when the path is an app or pages {@code editAsset.jsp}
   */
  public static boolean isRetiredEditAssetJsp(String lowerPath) {
    if (lowerPath == null || !lowerPath.endsWith("/editasset.jsp")) {
      return false;
    }
    return lowerPath.contains("/cm/app/") || lowerPath.contains("/cm/pages/app/");
  }

  private static String positiveIntToken(String digits) {
    try {
      long n = Long.parseLong(digits);
      if (n <= 0L || n > Integer.MAX_VALUE) {
        return null;
      }
      return Long.toString(n);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
