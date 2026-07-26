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
package com.percussion.security.validation;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Normalizes absolute URLs and matches them against glob patterns ({@code *} = multi-character
 * wildcard) for allow/block list evaluation (issue #1205).
 */
public final class URLGlobMatcher {

  private URLGlobMatcher() {}

  /**
   * Builds a comparable absolute URL string: {@code scheme://host[:port]/path[?query]} with scheme
   * and host lowercased. Port is omitted when not explicit ({@code -1}).
   */
  public static String normalize(URL url) {
    if (url == null) {
      throw new IllegalArgumentException("url cannot be null");
    }
    String scheme = url.getProtocol() != null ? url.getProtocol().toLowerCase() : "";
    String host = url.getHost() != null ? url.getHost().toLowerCase() : "";
    host = host.replaceAll("[\\[\\]]", "");
    int port = url.getPort();
    String path = url.getPath() != null ? url.getPath() : "";
    if (path.isEmpty()) {
      path = "/";
    }
    String query = url.getQuery();
    StringBuilder sb = new StringBuilder();
    sb.append(scheme).append("://").append(host);
    if (port > 0) {
      sb.append(':').append(port);
    }
    sb.append(path);
    if (query != null && !query.isEmpty()) {
      sb.append('?').append(query);
    }
    return sb.toString();
  }

  /**
   * Returns true if {@code normalizedUrl} matches the glob {@code pattern} ({@code *} matches any
   * sequence including empty). Matching is case-sensitive on the already-normalized string
   * (scheme/host lowercased by {@link #normalize(URL)}).
   */
  public static boolean matches(String pattern, String normalizedUrl) {
    if (pattern == null || normalizedUrl == null) {
      return false;
    }
    String p = pattern.trim();
    if (p.isEmpty() || "*".equals(p)) {
      return false;
    }
    Pattern regex = Pattern.compile(globToRegex(p), Pattern.DOTALL);
    return regex.matcher(normalizedUrl).matches();
  }

  static String globToRegex(String glob) {
    StringBuilder rx = new StringBuilder("^");
    for (int i = 0; i < glob.length(); i++) {
      char c = glob.charAt(i);
      if (c == '*') {
        rx.append(".*");
      } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
        rx.append('\\').append(c);
      } else if (c == '?') {
        rx.append("\\?");
      } else {
        rx.append(c);
      }
    }
    rx.append('$');
    return rx.toString();
  }
}
