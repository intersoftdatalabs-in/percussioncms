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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Builds an in-page heading TOC (h2–h3) from assembled Markdown HTML and assigns stable fragment
 * ids when a heading does not already have a safe {@code id}.
 */
public final class PSVirtualSiteHeadingToc {

  private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z][A-Za-z0-9._:-]*");

  private PSVirtualSiteHeadingToc() {}

  /**
   * Annotates h2/h3 headings with fragment ids and builds a {@code ${toc}} HTML fragment.
   *
   * @param contentHtml assembled page body (CommonMark HTML fragment); may be null
   * @return annotated content plus TOC HTML (empty string when there are no h2/h3 headings)
   */
  public static Result build(String contentHtml) {
    if (contentHtml == null || contentHtml.isBlank()) {
      return new Result(contentHtml == null ? "" : contentHtml, "");
    }
    Document doc = Jsoup.parseBodyFragment(contentHtml);
    doc.outputSettings().prettyPrint(false);
    Elements headings = doc.body().select("h2, h3");
    if (headings.isEmpty()) {
      return new Result(contentHtml, "");
    }

    Set<String> usedIds = new LinkedHashSet<>();
    List<Heading> collected = new ArrayList<>();
    for (Element heading : headings) {
      String text = heading.text().trim();
      String id = resolveId(heading.id(), text, usedIds);
      heading.attr("id", id);
      collected.add(new Heading("h3".equalsIgnoreCase(heading.tagName()) ? 3 : 2, id, text));
    }
    return new Result(doc.body().html(), renderToc(collected));
  }

  static String slugify(String text) {
    if (text == null || text.isBlank()) {
      return "section";
    }
    String normalized = Normalizer.normalize(text, Normalizer.Form.NFKD);
    StringBuilder sb = new StringBuilder(normalized.length());
    boolean pendingHyphen = false;
    for (int i = 0; i < normalized.length(); i++) {
      char c = normalized.charAt(i);
      if (Character.getType(c) == Character.NON_SPACING_MARK) {
        continue;
      }
      if (Character.isLetterOrDigit(c)) {
        if (pendingHyphen && sb.length() > 0) {
          sb.append('-');
        }
        sb.append(Character.toLowerCase(c));
        pendingHyphen = false;
      } else if (c == '_' || c == '-' || Character.isWhitespace(c)) {
        pendingHyphen = sb.length() > 0;
      }
    }
    return sb.length() == 0 ? "section" : sb.toString();
  }

  private static String resolveId(String existing, String text, Set<String> usedIds) {
    String candidate = existing != null ? existing.trim() : "";
    if (!SAFE_ID.matcher(candidate).matches()) {
      candidate = slugify(text);
    }
    return uniquify(candidate, usedIds);
  }

  private static String uniquify(String base, Set<String> usedIds) {
    String id = base;
    int n = 1;
    while (usedIds.contains(id.toLowerCase(Locale.ROOT))) {
      id = base + "-" + n;
      n++;
    }
    usedIds.add(id.toLowerCase(Locale.ROOT));
    return id;
  }

  private static String renderToc(List<Heading> headings) {
    StringBuilder sb = new StringBuilder();
    sb.append("<nav class=\"vs-toc\" aria-label=\"On this page\">\n<ol>\n");
    boolean openH2 = false;
    boolean openH3List = false;
    for (Heading heading : headings) {
      if (heading.level == 2) {
        if (openH3List) {
          sb.append("</ol>\n");
          openH3List = false;
        }
        if (openH2) {
          sb.append("</li>\n");
        }
        appendLinkItem(sb, heading, false);
        openH2 = true;
      } else if (!openH2) {
        appendLinkItem(sb, heading, true);
      } else {
        if (!openH3List) {
          sb.append("\n<ol>\n");
          openH3List = true;
        }
        appendLinkItem(sb, heading, true);
      }
    }
    if (openH3List) {
      sb.append("</ol>\n");
    }
    if (openH2) {
      sb.append("</li>\n");
    }
    sb.append("</ol>\n</nav>\n");
    return sb.toString();
  }

  private static void appendLinkItem(StringBuilder sb, Heading heading, boolean close) {
    sb.append("<li><a href=\"#")
        .append(PSVirtualSiteLayoutRenderer.htmlEscape(heading.id))
        .append("\">")
        .append(PSVirtualSiteLayoutRenderer.htmlEscape(heading.text))
        .append("</a>");
    if (close) {
      sb.append("</li>\n");
    }
  }

  /**
   * TOC generation result.
   *
   * @param contentHtml page body with stable heading ids (or the original fragment when none)
   * @param tocHtml {@code <nav class="vs-toc">} fragment, or empty when there are no h2/h3 headings
   */
  public record Result(String contentHtml, String tocHtml) {
    public Result {
      contentHtml = contentHtml != null ? contentHtml : "";
      tocHtml = tocHtml != null ? tocHtml : "";
    }

    public boolean isEmpty() {
      return tocHtml.isBlank();
    }
  }

  private record Heading(int level, String id, String text) {}
}
