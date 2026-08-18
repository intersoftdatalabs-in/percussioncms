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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PSVirtualSiteLayoutRendererTest {

  @TempDir Path tempDir;

  @Test
  void themeBindsTocAndAnnotatesHeadingIds() throws Exception {
    Path theme = tempDir.resolve("theme");
    Files.createDirectories(theme);
    Files.writeString(
        theme.resolve("page.html"),
        "<html><body>${toc}<article>${content}</article></body></html>",
        StandardCharsets.UTF_8);

    String html =
        PSVirtualSiteLayoutRenderer.render(
            theme,
            "page.html",
            "Docs",
            "Page",
            "desc",
            "<h2>Overview</h2><p>Body</p>",
            "<ul></ul>",
            "8.2",
            "");

    assertTrue(html.contains("class=\"vs-toc\""), html);
    assertTrue(html.contains("href=\"#overview\""), html);
    assertTrue(html.contains("id=\"overview\""), html);
    assertTrue(html.contains("<article>"), html);
    assertTrue(html.contains("Overview"), html);
  }

  @Test
  void missingThemeUsesDefaultLayoutWithToc() throws Exception {
    String html =
        PSVirtualSiteLayoutRenderer.render(
            tempDir.resolve("missing-theme"),
            "page.html",
            "Docs",
            "Page",
            "desc",
            "<h2>Overview</h2>",
            "<ul class=\"vs-nav\"></ul>",
            "8.2",
            "");
    assertTrue(html.contains("class=\"vs-toc\""), html);
    assertTrue(html.contains("id=\"overview\""), html);
    assertTrue(html.contains("Docs"), html);
  }

  @Test
  void noHeadingsLeavesTocPlaceholderEmpty() throws Exception {
    Path theme = tempDir.resolve("empty-toc");
    Files.createDirectories(theme);
    Files.writeString(
        theme.resolve("page.html"),
        "START${toc}END<article>${content}</article>",
        StandardCharsets.UTF_8);

    String html =
        PSVirtualSiteLayoutRenderer.render(
            theme,
            "page.html",
            "Docs",
            "Page",
            "",
            "<p>Plain paragraph.</p>",
            "",
            "",
            "");

    assertTrue(html.contains("STARTEND"), html);
    assertFalse(html.contains("vs-toc"), html);
    assertTrue(html.contains("<p>Plain paragraph.</p>"), html);
  }

  @Test
  void renderNavHtmlEscapesTitlesAndHrefs() {
    VirtualNavNode child =
        new VirtualNavNode("Child <x>", "c", "/8.2/child.html", 20, List.of());
    VirtualNavNode root = new VirtualNavNode("Root", "r", "/8.2/index.html", 10, List.of(child));
    String nav = PSVirtualSiteLayoutRenderer.renderNavHtml(List.of(root));
    assertTrue(nav.contains("Child &lt;x&gt;"), nav);
    assertTrue(nav.contains("href=\"/8.2/child.html\""), nav);
    assertFalse(nav.contains("<x>"), nav);
  }
}
