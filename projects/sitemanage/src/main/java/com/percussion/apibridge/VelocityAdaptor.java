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
    List<VelocitySnippet> list = new ArrayList<>();

    // Field macros
    list.add(
        snippet(
            "field.displayfield",
            "displayfield",
            CATEGORY_FIELD,
            "#displayfield(\"rx:title\")"));
    list.add(snippet("field.field", "field", CATEGORY_FIELD, "#field(\"rx:title\")"));
    list.add(
        snippet(
            "field.datefield",
            "datefield",
            CATEGORY_FIELD,
            "#datefield(\"rx:sys_contentstartdate\", \"MM/dd/yyyy\")"));
    list.add(
        snippet(
            "field.field_if_set",
            "field_if_set",
            CATEGORY_FIELD,
            "#field_if_set(\"<p>\", \"rx:title\", \"</p>\")"));
    list.add(
        snippet(
            "field.datefield_if_set",
            "datefield_if_set",
            CATEGORY_FIELD,
            "#datefield_if_set(\"<p>\", \"rx:sys_contentstartdate\", \"MM/dd/yyyy\", \"</p>\")"));
    list.add(
        snippet(
            "field.fieldLink",
            "fieldLink",
            CATEGORY_FIELD,
            "#fieldLink(\"rx:title\", $sys.template)"));

    // Slot macros
    list.add(
        snippet(
            "slot.slot_simple",
            "slot_simple",
            CATEGORY_SLOT,
            "#slot_simple(\"rffSidNav\")"));
    list.add(
        snippet(
            "slot.slot_wrapped",
            "slot_wrapped",
            CATEGORY_SLOT,
            "#slot_wrapped(\"rffSidNav\", \"<div class=\\\"slot\\\">\", \"</div>\")"));
    list.add(
        snippet(
            "slot.slot",
            "slot",
            CATEGORY_SLOT,
            "#slot(\"rffSidNav\", \"\", \"\", \"\", \"\", $params)"));
    list.add(
        snippet(
            "slot.slot_page",
            "slot_page",
            CATEGORY_SLOT,
            "#slot_page(\"rffList\", \"\", \"\", \"\", \"\", $params, $itemsPerPage, $pageNumber)"));
    list.add(
        snippet(
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
                .trim()));
    list.add(
        snippet(
            "slot.node_slot",
            "node_slot",
            CATEGORY_SLOT,
            "#node_slot($node, \"rffSidNav\", \"\", \"\", \"\", \"\", $params)"));

    // Misc / examples
    list.add(snippet("misc.inner", "inner", CATEGORY_MISC, "#inner()"));
    list.add(
        snippet(
            "misc.children",
            "children",
            CATEGORY_MISC,
            "#children(\"rx:child\", \"rffSnTitleLink\", \"\", \"\", \"\", \"\")"));
    list.add(
        snippet(
            "misc.pager",
            "pager",
            CATEGORY_MISC,
            "#pager($sys.pagecount, $sys.pageno, \"Previous\", \" | \", \"Next\")"));
    list.add(
        snippet(
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
                .trim()));
    list.add(snippet("misc.linkback_head", "linkback_head", CATEGORY_MISC, "#linkback_head()"));
    list.add(
        snippet(
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
                .trim()));
    list.add(
        snippet(
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
                .trim()));

    return List.copyOf(list);
  }

  private static VelocitySnippet snippet(
      String id, String title, String category, String insertText) {
    return new VelocitySnippet(id, title, category, insertText);
  }
}
