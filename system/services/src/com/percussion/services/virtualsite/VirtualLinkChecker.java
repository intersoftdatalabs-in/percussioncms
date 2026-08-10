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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight broken-link detection for Virtual Site Markdown bodies.
 *
 * <p>Recognizes:
 *
 * <ul>
 *   <li>Markdown links {@code [text](target)} where target is a relative {@code .md}/{@code .html}
 *       path or {@code id:stable-id}
 *   <li>Skips {@code http(s):}, {@code mailto:}, anchors-only {@code #frag}
 * </ul>
 */
public final class VirtualLinkChecker {

  private static final Pattern MD_LINK =
      Pattern.compile("\\[[^\\]]*\\]\\(([^)]+)\\)");

  private VirtualLinkChecker() {}

  /**
   * @param siteKey site key
   * @param versionId version of the source page
   * @param sourcePath relative source path of the page
   * @param markdownBody body after frontmatter
   * @param idsInVersion map of stableId → published path for this version
   * @param publishedPaths set of published paths (forward-slash) for this version
   * @return list of problem descriptions
   */
  public static List<String> checkPage(
      String siteKey,
      String versionId,
      String sourcePath,
      String markdownBody,
      Map<String, String> idsInVersion,
      Set<String> publishedPaths) {
    List<String> problems = new ArrayList<>();
    if (markdownBody == null || markdownBody.isEmpty()) {
      return problems;
    }
    Matcher m = MD_LINK.matcher(markdownBody);
    while (m.find()) {
      String raw = m.group(1).trim();
      // strip optional title in quotes
      int space = indexOfUnquotedSpace(raw);
      if (space > 0) {
        raw = raw.substring(0, space).trim();
      }
      if (raw.startsWith("<") && raw.endsWith(">")) {
        raw = raw.substring(1, raw.length() - 1);
      }
      String target = stripFragment(raw);
      if (target.isEmpty() || isExternal(target)) {
        continue;
      }
      if (target.toLowerCase(Locale.ROOT).startsWith("id:")) {
        String id = target.substring(3).trim();
        if (!idsInVersion.containsKey(id)) {
          problems.add(
              sourcePath
                  + ": missing id target '"
                  + id
                  + "' (version "
                  + versionId
                  + ", site "
                  + siteKey
                  + ")");
        }
        continue;
      }
      // relative path — resolve against source directory using simple join (not OS Path — URL-ish)
      String resolved = resolveRelative(sourcePath, target);
      if (resolved.toLowerCase(Locale.ROOT).endsWith(".md")) {
        resolved = resolved.substring(0, resolved.length() - 3) + ".html";
      }
      if (!publishedPaths.contains(resolved)) {
        problems.add(sourcePath + ": missing path target '" + target + "' → " + resolved);
      }
    }
    return problems;
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

  private static String stripFragment(String target) {
    int hash = target.indexOf('#');
    return hash >= 0 ? target.substring(0, hash) : target;
  }

  private static boolean isExternal(String target) {
    String lower = target.toLowerCase(Locale.ROOT);
    return lower.startsWith("http://")
        || lower.startsWith("https://")
        || lower.startsWith("mailto:")
        || lower.startsWith("//");
  }

  static String resolveRelative(String sourcePath, String target) {
    String src = sourcePath.replace('\\', '/');
    String tgt = target.replace('\\', '/');
    if (tgt.startsWith("/")) {
      return tgt.substring(1);
    }
    int slash = src.lastIndexOf('/');
    String dir = slash >= 0 ? src.substring(0, slash + 1) : "";
    String combined = dir + tgt;
    return normalizePosix(combined);
  }

  static String normalizePosix(String path) {
    String[] parts = path.split("/");
    List<String> stack = new ArrayList<>();
    for (String p : parts) {
      if (p.isEmpty() || ".".equals(p)) {
        continue;
      }
      if ("..".equals(p)) {
        if (!stack.isEmpty()) {
          stack.remove(stack.size() - 1);
        }
        continue;
      }
      stack.add(p);
    }
    return String.join("/", stack);
  }

  /** Convenience for unique path set. */
  public static Set<String> pathSet(Iterable<String> paths) {
    Set<String> set = new LinkedHashSet<>();
    for (String p : paths) {
      set.add(p.replace('\\', '/'));
    }
    return set;
  }
}
