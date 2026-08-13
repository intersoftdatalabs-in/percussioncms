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
package com.percussion.rest.sites;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites assembled Virtual Site HTML so root-relative {@code href}/{@code src}/{@code url()}
 * values stay under the preview URL prefix (assembler emits site-root paths like {@code
 * /8.2/index.html}).
 */
public final class VirtualSitePreviewHtml {

  /**
   * Match standalone {@code href}/{@code src} attributes, not {@code data-href}, {@code data-src},
   * or {@code xlink:href} (word-boundary {@code \\b} is true between {@code -} / {@code :} and the
   * following letter).
   */
  private static final Pattern ROOT_HREF_SRC =
      Pattern.compile("(?i)(?<![\\w:-])((?:href|src)\\s*=\\s*[\"'])/(?!/)");

  /** Unquoted {@code url(/path)} and quoted {@code url("/path")} / {@code url('/path')}. */
  private static final Pattern ROOT_CSS_URL =
      Pattern.compile("(?i)(url\\(\\s*(?:[\"'])?)/(?!/)");

  private VirtualSitePreviewHtml() {}

  /**
   * Prefix must be an absolute URL path without a trailing slash (for example {@code
   * /services/sites/Help/virtual/preview}).
   *
   * @param html assembled page bytes (UTF-8)
   * @param previewPrefix URL path prefix for this site's preview stream
   * @return rewritten HTML bytes
   */
  public static byte[] rewriteRootRelative(byte[] html, String previewPrefix) {
    if (html == null || html.length == 0) {
      return html == null ? new byte[0] : html;
    }
    String prefix = normalizePrefix(previewPrefix);
    if (prefix.isEmpty()) {
      return html;
    }
    String text = new String(html, StandardCharsets.UTF_8);
    String withHref = ROOT_HREF_SRC.matcher(text).replaceAll("$1" + Matcher.quoteReplacement(prefix) + "/");
    String withUrl = ROOT_CSS_URL.matcher(withHref).replaceAll("$1" + Matcher.quoteReplacement(prefix) + "/");
    return withUrl.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Build the preview URL prefix from the JAX-RS application base path and site name.
   *
   * @param basePath {@link jakarta.ws.rs.core.UriInfo#getBaseUri()} path (may end with {@code /})
   * @param siteNameOrId site name or GUID (decoded)
   * @return prefix without trailing slash
   */
  public static String previewPrefix(String basePath, String siteNameOrId) {
    String base = basePath == null ? "" : basePath.trim();
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    String name = siteNameOrId == null ? "" : siteNameOrId.trim();
    String encoded = encodePathSegment(name);
    return base + "/sites/" + encoded + "/virtual/preview";
  }

  static String normalizePrefix(String previewPrefix) {
    if (previewPrefix == null) {
      return "";
    }
    String p = previewPrefix.trim();
    if (p.endsWith("/")) {
      p = p.substring(0, p.length() - 1);
    }
    if (!p.startsWith("/") || p.contains("..") || p.indexOf('\0') >= 0) {
      return "";
    }
    return p;
  }

  static String encodePathSegment(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(raw.length() + 8);
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (isUnreservedPathChar(c)) {
        sb.append(c);
      } else if (c == ' ') {
        sb.append("%20");
      } else {
        byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
          sb.append('%');
          String hex = Integer.toHexString(b & 0xff).toUpperCase();
          if (hex.length() == 1) {
            sb.append('0');
          }
          sb.append(hex);
        }
      }
    }
    return sb.toString();
  }

  private static boolean isUnreservedPathChar(char c) {
    return (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '-'
        || c == '_'
        || c == '.'
        || c == '~';
  }
}
