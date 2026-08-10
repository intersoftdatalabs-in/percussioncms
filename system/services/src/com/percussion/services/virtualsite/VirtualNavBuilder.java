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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a simple hierarchical nav tree from discovered page refs under a version folder. Uses
 * folder segments + frontmatter order; optional config {@code nav} only affects top-level titles.
 */
public final class VirtualNavBuilder {

  private VirtualNavBuilder() {}

  /**
   * Build nav for one version.
   *
   * @param versionPath version directory name as in config (e.g. {@code 8.2})
   * @param refs all refs for this version
   * @param config site config (nav overrides)
   * @return top-level nav nodes
   */
  public static List<VirtualNavNode> build(
      String versionPath, List<VirtualItemRef> refs, VirtualSiteConfig config) {
    // sectionKey (first folder under version, or "" for root pages) -> pages
    Map<String, List<VirtualItemRef>> bySection = new LinkedHashMap<>();
    for (VirtualItemRef ref : refs) {
      Path rel = ref.relativePath();
      // rel is like 8.2/getting-started/install.md
      Path underVersion = stripVersionPrefix(rel, versionPath);
      String section = sectionKey(underVersion);
      bySection.computeIfAbsent(section, k -> new ArrayList<>()).add(ref);
    }

    Map<String, String> navTitles = new LinkedHashMap<>();
    for (VirtualSiteConfig.NavSpec n : config.nav()) {
      navTitles.put(n.id(), n.title());
    }

    List<VirtualNavNode> roots = new ArrayList<>();
    for (Map.Entry<String, List<VirtualItemRef>> e : bySection.entrySet()) {
      String section = e.getKey();
      List<VirtualItemRef> pages = new ArrayList<>(e.getValue());
      pages.sort(
          Comparator.comparingInt(VirtualItemRef::order)
              .thenComparing(VirtualItemRef::title, String.CASE_INSENSITIVE_ORDER));

      if (section.isEmpty()) {
        for (VirtualItemRef p : pages) {
          roots.add(
              new VirtualNavNode(
                  p.title(), p.id(), toHref(p.relativePath()), p.order(), List.of()));
        }
        continue;
      }

      String title =
          navTitles.getOrDefault(section, humanize(section));
      VirtualItemRef index = findIndex(pages, section);
      String href = index != null ? toHref(index.relativePath()) : toHref(pages.get(0).relativePath());
      int order = index != null ? index.order() : pages.get(0).order();
      String sectionId = index != null ? index.id() : section;

      List<VirtualNavNode> children = new ArrayList<>();
      for (VirtualItemRef p : pages) {
        if (index != null && p.id().equals(index.id())) {
          continue;
        }
        children.add(
            new VirtualNavNode(p.title(), p.id(), toHref(p.relativePath()), p.order(), List.of()));
      }
      roots.add(new VirtualNavNode(title, sectionId, href, order, children));
    }

    roots.sort(
        Comparator.comparingInt(VirtualNavNode::order)
            .thenComparing(VirtualNavNode::title, String.CASE_INSENSITIVE_ORDER));
    return roots;
  }

  private static Path stripVersionPrefix(Path rel, String versionPath) {
    String first = rel.getNameCount() > 0 ? rel.getName(0).toString() : "";
    if (first.equals(versionPath) && rel.getNameCount() > 1) {
      return rel.subpath(1, rel.getNameCount());
    }
    if (first.equals(versionPath)) {
      return Path.of("");
    }
    return rel;
  }

  private static String sectionKey(Path underVersion) {
    if (underVersion == null || underVersion.getNameCount() == 0) {
      return "";
    }
    String name = underVersion.getName(0).toString();
    if (underVersion.getNameCount() == 1 && isMarkdown(name)) {
      return ""; // page at version root
    }
    return name;
  }

  private static boolean isMarkdown(String name) {
    return name.toLowerCase(Locale.ROOT).endsWith(".md");
  }

  private static VirtualItemRef findIndex(List<VirtualItemRef> pages, String section) {
    for (VirtualItemRef p : pages) {
      String file = p.relativePath().getFileName().toString().toLowerCase(Locale.ROOT);
      if ("index.md".equals(file)) {
        Path parent = p.relativePath().getParent();
        if (parent != null && parent.getFileName() != null
            && section.equals(parent.getFileName().toString())) {
          return p;
        }
        // version/index.md handled as root
        if (parent != null && parent.getNameCount() >= 1) {
          return p;
        }
      }
    }
    return null;
  }

  /** Convert source relative path to output HTML path using forward slashes. */
  public static String toHref(Path relativeMarkdown) {
    String s = relativeMarkdown.toString().replace('\\', '/');
    if (s.toLowerCase(Locale.ROOT).endsWith(".md")) {
      s = s.substring(0, s.length() - 3) + ".html";
    }
    if (s.toLowerCase(Locale.ROOT).endsWith("/index.html")) {
      // keep index.html for clarity in Phase 1
    }
    return s;
  }

  private static String humanize(String section) {
    String[] parts = section.split("[-_]");
    StringBuilder sb = new StringBuilder();
    for (String p : parts) {
      if (p.isEmpty()) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(Character.toUpperCase(p.charAt(0)));
      if (p.length() > 1) {
        sb.append(p.substring(1));
      }
    }
    return sb.length() == 0 ? section : sb.toString();
  }
}
