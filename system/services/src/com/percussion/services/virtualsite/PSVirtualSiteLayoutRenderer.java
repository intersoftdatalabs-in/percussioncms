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

import com.percussion.services.assembly.impl.plugin.PSBindingPlaceholderRenderer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies HTML-first layout templates from {@code _theme/} using {@code ${path}} placeholders
 * (ADR-002 / assembler Phase 1).
 */
public final class PSVirtualSiteLayoutRenderer {

  private PSVirtualSiteLayoutRenderer() {}

  public static String render(
      Path themeDir,
      String layoutFile,
      String siteTitle,
      String pageTitle,
      String description,
      String contentHtml,
      String navHtml,
      String versionLabel,
      String versionSwitcherHtml)
      throws IOException, VirtualSiteException {
    PSVirtualSiteHeadingToc.Result toc = PSVirtualSiteHeadingToc.build(contentHtml);
    Path layout = themeDir.resolve(layoutFile);
    if (!Files.isRegularFile(layout)) {
      // Built-in minimal layout when theme missing
      return defaultLayout(
          siteTitle,
          pageTitle,
          description,
          toc.contentHtml(),
          navHtml,
          toc.tocHtml(),
          versionLabel,
          versionSwitcherHtml);
    }
    String template = Files.readString(layout, StandardCharsets.UTF_8);
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("siteTitle", siteTitle);
    bindings.put("pageTitle", pageTitle);
    bindings.put("description", description != null ? description : "");
    bindings.put("content", toc.contentHtml());
    bindings.put("nav", navHtml);
    bindings.put("toc", toc.tocHtml());
    bindings.put("versionLabel", versionLabel != null ? versionLabel : "");
    bindings.put("versionSwitcher", versionSwitcherHtml != null ? versionSwitcherHtml : "");
    return PSBindingPlaceholderRenderer.render(template, bindings);
  }

  public static String renderNavHtml(List<VirtualNavNode> nodes) {
    StringBuilder sb = new StringBuilder();
    sb.append("<ul class=\"vs-nav\">\n");
    for (VirtualNavNode n : nodes) {
      appendNode(sb, n, 1);
    }
    sb.append("</ul>\n");
    return sb.toString();
  }

  private static void appendNode(StringBuilder sb, VirtualNavNode n, int depth) {
    sb.append("  ".repeat(depth));
    sb.append("<li>");
    if (n.href() != null && !n.href().isBlank()) {
      sb.append("<a href=\"")
          .append(htmlEscape(VirtualMarkdownLinkRewriter.toSiteRootHref(n.href())))
          .append("\">")
          .append(htmlEscape(n.title()))
          .append("</a>");
    } else {
      sb.append(htmlEscape(n.title()));
    }
    if (!n.children().isEmpty()) {
      sb.append("\n").append("  ".repeat(depth)).append("<ul>\n");
      for (VirtualNavNode c : n.children()) {
        appendNode(sb, c, depth + 1);
      }
      sb.append("  ".repeat(depth)).append("</ul>\n").append("  ".repeat(depth));
    }
    sb.append("</li>\n");
  }

  public static String renderVersionSwitcher(VirtualSiteConfig config, String currentVersionId) {
    if (config.versions().size() <= 1) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<select class=\"vs-version\" onchange=\"if(this.value)location.href=this.value\">");
    for (VirtualSiteConfig.VersionSpec v : config.versions()) {
      String href = VirtualMarkdownLinkRewriter.toSiteRootHref(v.path() + "/index.html");
      sb.append("<option value=\"")
          .append(htmlEscape(href))
          .append("\"");
      if (v.id().equals(currentVersionId)) {
        sb.append(" selected");
      }
      sb.append(">").append(htmlEscape(v.label())).append("</option>");
    }
    sb.append("</select>");
    return sb.toString();
  }

  private static String defaultLayout(
      String siteTitle,
      String pageTitle,
      String description,
      String contentHtml,
      String navHtml,
      String tocHtml,
      String versionLabel,
      String versionSwitcherHtml) {
    return "<!DOCTYPE html>\n"
        + "<html lang=\"en\">\n"
        + "<head>\n"
        + "<meta charset=\"utf-8\"/>\n"
        + "<title>"
        + htmlEscape(pageTitle)
        + " — "
        + htmlEscape(siteTitle)
        + "</title>\n"
        + "<meta name=\"description\" content=\""
        + htmlEscape(description)
        + "\"/>\n"
        + "<link rel=\"stylesheet\" href=\"/assets/site.css\"/>\n"
        + "</head>\n"
        + "<body>\n"
        + "<header><h1>"
        + htmlEscape(siteTitle)
        + "</h1>"
        + versionSwitcherHtml
        + "</header>\n"
        + "<div class=\"layout\">\n"
        + "<nav>"
        + navHtml
        + "</nav>\n"
        + "<main>\n"
        + "<p class=\"version\">"
        + htmlEscape(versionLabel)
        + "</p>\n"
        + "<h1>"
        + htmlEscape(pageTitle)
        + "</h1>\n"
        + (tocHtml != null ? tocHtml : "")
        + contentHtml
        + "\n</main>\n"
        + "</div>\n"
        + "</body>\n"
        + "</html>\n";
  }

  static String htmlEscape(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
