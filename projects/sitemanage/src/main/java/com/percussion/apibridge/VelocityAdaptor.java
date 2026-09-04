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

package com.percussion.apibridge;

import com.percussion.rest.velocity.IVelocityAdaptor;
import com.percussion.rest.velocity.VelocitySnippet;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;

/**
 * Built-in Velocity snippet catalog (AS-09 / Appendix C).
 *
 * <p>Returns stable field/slot/misc macro insert texts for template authors. Does not read or write
 * System/User Velocity config files (SY-02).
 */
@PSSiteManageBean
@Lazy
public class VelocityAdaptor implements IVelocityAdaptor {

  static final String CATEGORY_FIELD = "field";
  static final String CATEGORY_SLOT = "slot";
  static final String CATEGORY_MISC = "misc";

  private static final Set<String> ALLOWED_CATEGORIES =
      Set.of(CATEGORY_FIELD, CATEGORY_SLOT, CATEGORY_MISC);

  private final List<VelocitySnippet> catalog;
  private final Map<String, VelocitySnippet> byId;

  public VelocityAdaptor() {
    this(buildBuiltinCatalog());
  }

  /** Package-visible for unit tests. */
  VelocityAdaptor(List<VelocitySnippet> snippets) {
    List<VelocitySnippet> copy = List.copyOf(snippets);
    this.catalog = copy;
    Map<String, VelocitySnippet> index = new LinkedHashMap<>();
    for (VelocitySnippet s : copy) {
      if (s != null && StringUtils.isNotBlank(s.getId())) {
        index.put(s.getId().toLowerCase(Locale.ROOT), s);
      }
    }
    this.byId = Collections.unmodifiableMap(index);
  }

  @Override
  public List<VelocitySnippet> listSnippets() {
    return catalog;
  }

  @Override
  public VelocitySnippet findSnippetById(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    return byId.get(id.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Appendix C inventory from {@code docs/developer-module/workbench-functional-inventory.md} plus
   * insert text aligned with shipped {@code sys_assembly.vm} macros.
   */
  static List<VelocitySnippet> buildBuiltinCatalog() {
    CatalogEntry[] entries = {
      // Field macros
      new CatalogEntry(
          "field.displayfield", "displayfield", CATEGORY_FIELD, "#displayfield(\"rx:title\")"),
      new CatalogEntry("field.field", "field", CATEGORY_FIELD, "#field(\"rx:title\")"),
      new CatalogEntry(
          "field.datefield",
          "datefield",
          CATEGORY_FIELD,
          "#datefield(\"rx:sys_contentstartdate\", \"MM/dd/yyyy\")"),
      new CatalogEntry(
          "field.field_if_set",
          "field_if_set",
          CATEGORY_FIELD,
          "#field_if_set(\"<p>\", \"rx:title\", \"</p>\")"),
      new CatalogEntry(
          "field.datefield_if_set",
          "datefield_if_set",
          CATEGORY_FIELD,
          "#datefield_if_set(\"<p>\", \"rx:sys_contentstartdate\", \"MM/dd/yyyy\", \"</p>\")"),
      new CatalogEntry(
          "field.fieldLink",
          "fieldLink",
          CATEGORY_FIELD,
          "#fieldLink(\"rx:title\", $sys.template)"),
      // Slot macros
      new CatalogEntry(
          "slot.slot_simple", "slot_simple", CATEGORY_SLOT, "#slot_simple(\"rffSidNav\")"),
      new CatalogEntry(
          "slot.slot_wrapped",
          "slot_wrapped",
          CATEGORY_SLOT,
          "#slot_wrapped(\"rffSidNav\", \"<div class=\\\"slot\\\">\", \"</div>\")"),
      new CatalogEntry(
          "slot.slot", "slot", CATEGORY_SLOT, "#slot(\"rffSidNav\", \"\", \"\", \"\", \"\", $params)"),
      new CatalogEntry(
          "slot.slot_page",
          "slot_page",
          CATEGORY_SLOT,
          "#slot_page(\"rffList\", \"\", \"\", \"\", \"\", $params, $itemsPerPage, $pageNumber)"),
      new CatalogEntry(
          "slot.raw_slot_loop",
          "Raw slot loop",
          CATEGORY_SLOT,
          """
          #initslot("rffSidNav", $params)
          #foreach($relresult in $sys.currentslot.relresults)
          #slotItem($relresult)
          #end
          #endslot("rffSidNav")
          """
              .trim()),
      new CatalogEntry(
          "slot.node_slot",
          "node_slot",
          CATEGORY_SLOT,
          "#node_slot($node, \"rffSidNav\", \"\", \"\", \"\", \"\", $params)"),
      // Misc / examples
      new CatalogEntry("misc.inner", "inner", CATEGORY_MISC, "#inner()"),
      new CatalogEntry(
          "misc.children",
          "children",
          CATEGORY_MISC,
          "#children(\"rx:child\", \"rffSnTitleLink\", \"\", \"\", \"\", \"\")"),
      new CatalogEntry(
          "misc.pager",
          "pager",
          CATEGORY_MISC,
          "#pager($sys.pagecount, $sys.pageno, \"Previous\", \" | \", \"Next\")"),
      new CatalogEntry(
          "misc.sample_html_page_skeleton",
          "Sample HTML page skeleton",
          CATEGORY_MISC,
          """
          <!DOCTYPE html>
          <html>
          <head>
            <meta charset="UTF-8"/>
            <title>#displayfield("rx:title")</title>
            #linkback_head()
          </head>
          <body>
            <h1>#field("rx:title")</h1>
            #slot_simple("rffList")
          </body>
          </html>
          """
              .trim()),
      new CatalogEntry("misc.linkback_head", "linkback_head", CATEGORY_MISC, "#linkback_head()"),
      new CatalogEntry(
          "misc.lclamp_global_template_sample",
          "L-clamp global template sample",
          CATEGORY_MISC,
          """
          ## L-clamp: bind page-level content then assemble slots
          #set($pageTitle = $sys.item.getProperty("rx:title").String)
          <div class="page">
            <header><h1>$pageTitle</h1></header>
            <main>#slot_simple("rffList")</main>
          </div>
          """
              .trim()),
      new CatalogEntry(
          "misc.nav_samples",
          "Breadcrumbs / top nav / left nav samples",
          CATEGORY_MISC,
          """
          ## Breadcrumbs
          <nav class="breadcrumbs">#slot_simple("rffBreadcrumb")</nav>
          ## Top navigation
          <nav class="top-nav">#slot_simple("rffTopNav")</nav>
          ## Left navigation
          <aside class="left-nav">#slot_simple("rffSidNav")</aside>
          """
              .trim()),
    };

    List<VelocitySnippet> list = new ArrayList<>(entries.length);
    for (CatalogEntry entry : entries) {
      list.add(snippet(entry.id(), entry.title(), entry.category(), entry.insertText()));
    }
    return List.copyOf(list);
  }

  /**
   * Builds a catalog entry with validation: non-blank trimmed id, title, and insert text; category
   * must be one of {@link #CATEGORY_FIELD}, {@link #CATEGORY_SLOT}, or {@link #CATEGORY_MISC}
   * (case-insensitive).
   */
  static VelocitySnippet snippet(String id, String title, String category, String insertText) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("snippet id must not be blank");
    }
    if (StringUtils.isBlank(title)) {
      throw new IllegalArgumentException("snippet title must not be blank");
    }
    if (StringUtils.isBlank(insertText)) {
      throw new IllegalArgumentException("snippet insertText must not be blank");
    }
    if (StringUtils.isBlank(category)) {
      throw new IllegalArgumentException("snippet category must not be blank");
    }
    String normalizedCategory = category.trim().toLowerCase(Locale.ROOT);
    if (!ALLOWED_CATEGORIES.contains(normalizedCategory)) {
      throw new IllegalArgumentException(
          "snippet category must be one of " + ALLOWED_CATEGORIES + ", got: " + category);
    }
    return new VelocitySnippet(id.trim(), title.trim(), normalizedCategory, insertText.trim());
  }

  private record CatalogEntry(String id, String title, String category, String insertText) {}
}
