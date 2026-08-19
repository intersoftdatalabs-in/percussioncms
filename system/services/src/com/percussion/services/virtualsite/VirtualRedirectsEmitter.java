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
package com.percussion.services.virtualsite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Writes portable Virtual Site redirect artifacts: static HTML pages plus {@code redirects.json}
 * next to the assembled site. Publish copies these as ordinary files (not {@code _meta}).
 */
public final class VirtualRedirectsEmitter {

  public static final String REDIRECTS_MAP_FILE = "redirects.json";

  private VirtualRedirectsEmitter() {}

  /**
   * Emit redirect HTML and a redirects map under {@code outputRoot}.
   *
   * @param redirects validated redirects (empty is a no-op)
   * @param outputRoot already barrier-checked build output root
   * @param alreadyWritten hrefs already emitted for pages/assets (collision check)
   * @return relative output paths written (forward-slash hrefs)
   */
  static List<String> emit(
      List<VirtualRedirect> redirects, Path outputRoot, Collection<String> alreadyWritten)
      throws IOException, VirtualSiteException {
    if (redirects == null || redirects.isEmpty()) {
      return List.of();
    }
    Path safeOut = PSVirtualSiteBuildService.requireSafeBuildRoot(outputRoot);
    List<String> written = new ArrayList<>();
    for (VirtualRedirect redirect : redirects) {
      String href = VirtualRedirectsLoader.toOutputHref(redirect.from());
      if (containsHref(alreadyWritten, href) || containsHref(written, href)) {
        throw new VirtualSiteException(
            "Redirect from '" + redirect.from() + "' would overwrite built file " + href);
      }
      Path outFile = PSVirtualSiteBuildService.resolveHref(safeOut, href);
      Path parent = outFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent); // codeql[java/path-injection]
      }
      Files.writeString(
          outFile, redirectHtml(redirect.to()), StandardCharsets.UTF_8); // codeql[java/path-injection]
      written.add(href);
    }
    Path map = safeOut.resolve(REDIRECTS_MAP_FILE); // codeql[java/path-injection]
    Files.writeString(map, toJsonMap(redirects), StandardCharsets.UTF_8); // codeql[java/path-injection]
    written.add(REDIRECTS_MAP_FILE);
    return written;
  }

  static String redirectHtml(String to) {
    String escaped = PSVirtualSiteLayoutRenderer.htmlEscape(to);
    return "<!DOCTYPE html>\n"
        + "<html lang=\"en\">\n"
        + "<head>\n"
        + "<meta charset=\"utf-8\">\n"
        + "<title>Moved</title>\n"
        + "<meta http-equiv=\"refresh\" content=\"0;url="
        + escaped
        + "\">\n"
        + "<link rel=\"canonical\" href=\""
        + escaped
        + "\">\n"
        + "</head>\n"
        + "<body>\n"
        + "<p>This page has moved to <a href=\""
        + escaped
        + "\">"
        + escaped
        + "</a>.</p>\n"
        + "</body>\n"
        + "</html>\n";
  }

  static String toJsonMap(List<VirtualRedirect> redirects) {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");
    for (int i = 0; i < redirects.size(); i++) {
      VirtualRedirect r = redirects.get(i);
      sb.append("  {\"from\":")
          .append(jsonQuote(r.from()))
          .append(",\"to\":")
          .append(jsonQuote(r.to()))
          .append(",\"status\":")
          .append(r.status())
          .append('}');
      if (i + 1 < redirects.size()) {
        sb.append(',');
      }
      sb.append('\n');
    }
    sb.append("]\n");
    return sb.toString();
  }

  private static boolean containsHref(Collection<String> hrefs, String href) {
    if (hrefs == null || href == null) {
      return false;
    }
    for (String existing : hrefs) {
      if (existing != null && href.equals(existing.replace('\\', '/'))) {
        return true;
      }
    }
    return false;
  }

  private static String jsonQuote(String s) {
    String value = s == null ? "" : s;
    StringBuilder sb = new StringBuilder(value.length() + 2);
    sb.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\' -> sb.append("\\\\");
        case '"' -> sb.append("\\\"");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
    return sb.toString();
  }
}
