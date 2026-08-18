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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PSVirtualSiteHeadingTocTest {

  @Test
  void emptyOrBlankContentYieldsEmptyToc() {
    assertTrue(PSVirtualSiteHeadingToc.build(null).isEmpty());
    assertEquals("", PSVirtualSiteHeadingToc.build(null).contentHtml());
    assertTrue(PSVirtualSiteHeadingToc.build("").isEmpty());
    assertEquals("", PSVirtualSiteHeadingToc.build("").contentHtml());
    assertTrue(PSVirtualSiteHeadingToc.build("   ").isEmpty());
  }

  @Test
  void noHeadingsOrH1OnlyYieldsEmptyTocAndUnchangedContent() {
    String para = "<p>No headings here.</p>";
    PSVirtualSiteHeadingToc.Result noHeadings = PSVirtualSiteHeadingToc.build(para);
    assertTrue(noHeadings.isEmpty());
    assertEquals("", noHeadings.tocHtml());
    assertEquals(para, noHeadings.contentHtml());

    String h1Only = "<h1>Page title</h1><p>Body</p>";
    PSVirtualSiteHeadingToc.Result h1 = PSVirtualSiteHeadingToc.build(h1Only);
    assertTrue(h1.isEmpty());
    assertEquals(h1Only, h1.contentHtml());
  }

  @Test
  void h2AndH3GetStableIdsAndNestedToc() {
    String html =
        "<h2>Overview</h2><p>A</p><h3>Details</h3><p>B</p><h2>Next steps</h2><p>C</p>";
    PSVirtualSiteHeadingToc.Result result = PSVirtualSiteHeadingToc.build(html);
    assertFalse(result.isEmpty());
    assertTrue(result.contentHtml().contains("id=\"overview\""), result.contentHtml());
    assertTrue(result.contentHtml().contains("id=\"details\""), result.contentHtml());
    assertTrue(result.contentHtml().contains("id=\"next-steps\""), result.contentHtml());

    String toc = result.tocHtml();
    assertTrue(toc.contains("class=\"vs-toc\""), toc);
    assertTrue(toc.contains("aria-label=\"On this page\""), toc);
    assertTrue(toc.contains("href=\"#overview\""), toc);
    assertTrue(toc.contains("href=\"#details\""), toc);
    assertTrue(toc.contains("href=\"#next-steps\""), toc);
    assertTrue(toc.contains(">Overview</a>"), toc);
    assertTrue(toc.contains(">Details</a>"), toc);
    int overview = toc.indexOf("href=\"#overview\"");
    int innerOl = toc.indexOf("<ol>", overview + 1);
    int details = toc.indexOf("href=\"#details\"");
    int next = toc.indexOf("href=\"#next-steps\"");
    assertTrue(overview >= 0 && innerOl > overview && details > innerOl && next > details, toc);
  }

  @Test
  void existingSafeIdsArePreserved() {
    String html = "<h2 id=\"custom-id\">Overview</h2><h3>Notes</h3>";
    PSVirtualSiteHeadingToc.Result result = PSVirtualSiteHeadingToc.build(html);
    assertTrue(result.contentHtml().contains("id=\"custom-id\""), result.contentHtml());
    assertTrue(result.tocHtml().contains("href=\"#custom-id\""), result.tocHtml());
    assertTrue(result.tocHtml().contains("href=\"#notes\""), result.tocHtml());
  }

  @Test
  void duplicateTitlesGetUniqueSuffixes() {
    String html = "<h2>Install</h2><h2>Install</h2><h3>Install</h3>";
    PSVirtualSiteHeadingToc.Result result = PSVirtualSiteHeadingToc.build(html);
    assertTrue(result.contentHtml().contains("id=\"install\""), result.contentHtml());
    assertTrue(result.contentHtml().contains("id=\"install-1\""), result.contentHtml());
    assertTrue(result.contentHtml().contains("id=\"install-2\""), result.contentHtml());
    assertTrue(result.tocHtml().contains("href=\"#install\""), result.tocHtml());
    assertTrue(result.tocHtml().contains("href=\"#install-1\""), result.tocHtml());
    assertTrue(result.tocHtml().contains("href=\"#install-2\""), result.tocHtml());
  }

  @Test
  void unsafeExistingIdIsReplacedWithSlug() {
    String html = "<h2 id=\"bad id\">Safe Title</h2>";
    PSVirtualSiteHeadingToc.Result result = PSVirtualSiteHeadingToc.build(html);
    assertFalse(result.contentHtml().contains("id=\"bad id\""), result.contentHtml());
    assertTrue(result.contentHtml().contains("id=\"safe-title\""), result.contentHtml());
    assertTrue(result.tocHtml().contains("href=\"#safe-title\""), result.tocHtml());
  }

  @Test
  void slugifyIsStableAndSafe() {
    assertEquals("hello-world", PSVirtualSiteHeadingToc.slugify("Hello World"));
    assertEquals("cafe", PSVirtualSiteHeadingToc.slugify("Café"));
    assertEquals("section", PSVirtualSiteHeadingToc.slugify("   "));
    assertEquals("section", PSVirtualSiteHeadingToc.slugify("***"));
    assertEquals("a-b", PSVirtualSiteHeadingToc.slugify("A  --  B"));
  }

  @Test
  void tocLabelsAreHtmlEscaped() {
    String html = "<h2>Use &lt;script&gt; tags</h2>";
    PSVirtualSiteHeadingToc.Result result = PSVirtualSiteHeadingToc.build(html);
    assertTrue(result.tocHtml().contains("&lt;script&gt;"), result.tocHtml());
    assertFalse(result.tocHtml().contains("<script>"), result.tocHtml());
  }

  @Test
  void orphanH3IsEmittedAsTopLevel() {
    String html = "<h3>Only three</h3>";
    PSVirtualSiteHeadingToc.Result result = PSVirtualSiteHeadingToc.build(html);
    assertFalse(result.isEmpty());
    assertTrue(result.contentHtml().contains("id=\"only-three\""), result.contentHtml());
    assertTrue(result.tocHtml().contains("href=\"#only-three\""), result.tocHtml());
    assertTrue(result.tocHtml().contains("<li><a href=\"#only-three\">Only three</a></li>"), result.tocHtml());
  }
}
