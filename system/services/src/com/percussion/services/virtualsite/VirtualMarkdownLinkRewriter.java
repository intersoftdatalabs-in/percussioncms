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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites Markdown link targets for static site emit.
 *
 * <ul>
 *   <li>{@code id:stable-id} → site-root published path ({@code /8.2/...html})
 *   <li>Relative {@code *.md} paths → resolved + {@code .html}
 *   <li>Leaves {@code http(s):}, {@code mailto:}, and bare anchors alone
 * </ul>
 */
public final class VirtualMarkdownLinkRewriter {

  private static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]*)\\]\\(([^)]+)\\)");

  private VirtualMarkdownLinkRewriter() {}

  /**
   * @param markdownBody body after frontmatter
   * @param sourcePath source relative path (e.g. {@code 8.2/index.md})
   * @param idsInVersion stableId → published path (no leading slash)
   * @return body with rewritten link targets
   */
  public static String rewrite(
      String markdownBody, String sourcePath, Map<String, String> idsInVersion) {
    if (markdownBody == null || markdownBody.isEmpty()) {
      return markdownBody == null ? "" : markdownBody;
    }
    Matcher m = MD_LINK.matcher(markdownBody);
    StringBuilder out = new StringBuilder();
    while (m.find()) {
      String text = m.group(1);
      String rawDest = m.group(2).trim();
      String titleSuffix = "";
      int space = indexOfUnquotedSpace(rawDest);
      if (space > 0) {
        titleSuffix = rawDest.substring(space);
        rawDest = rawDest.substring(0, space).trim();
      }
      if (rawDest.startsWith("<") && rawDest.endsWith(">")) {
        rawDest = rawDest.substring(1, rawDest.length() - 1);
      }
      String fragment = "";
      String target = rawDest;
      int hash = rawDest.indexOf('#');
      if (hash >= 0) {
        fragment = rawDest.substring(hash);
        target = rawDest.substring(0, hash);
      }
      String rewritten = rewriteTarget(target, sourcePath, idsInVersion);
      String replacement = "[" + text + "](" + rewritten + fragment + titleSuffix + ")";
      m.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(out);
    return out.toString();
  }

  static String rewriteTarget(
      String target, String sourcePath, Map<String, String> idsInVersion) {
    if (target == null || target.isEmpty()) {
      return target == null ? "" : target;
    }
    String lower = target.toLowerCase(Locale.ROOT);
    if (lower.startsWith("http://")
        || lower.startsWith("https://")
        || lower.startsWith("mailto:")
        || lower.startsWith("//")
        || target.startsWith("#")) {
      return target;
    }
    if (lower.startsWith("id:")) {
      String id = target.substring(3).trim();
      String published = idsInVersion != null ? idsInVersion.get(id) : null;
      if (published == null || published.isBlank()) {
        // leave unresolved; link checker reports separately
        return target;
      }
      return toSiteRootHref(published);
    }
    String resolved = VirtualLinkChecker.resolveRelative(sourcePath, target);
    if (resolved.toLowerCase(Locale.ROOT).endsWith(".md")) {
      resolved = resolved.substring(0, resolved.length() - 3) + ".html";
    }
    return toSiteRootHref(resolved);
  }

  /** Published path without leading slash → site-root absolute href. */
  public static String toSiteRootHref(String publishedPath) {
    if (publishedPath == null || publishedPath.isBlank()) {
      return "/";
    }
    String p = publishedPath.replace('\\', '/');
    if (p.startsWith("/")) {
      return p;
    }
    return "/" + p;
  }

  private static int indexOfUnquotedSpace(String raw) {
    boolean inQuote = false;
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c == '"' || c == '\'') {
        inQuote = !inQuote;
      } else if (!inQuote && Character.isWhitespace(c)) {
        return i;
      }
    }
    return -1;
  }
}
