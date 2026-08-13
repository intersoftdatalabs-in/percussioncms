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
package com.percussion.rest.sites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class VirtualSitePreviewHtmlTest {

  @Test
  public void rewriteRootRelativeHrefAndCssUrl() {
    String html =
        "<a href=\"/8.2/index.html\">Home</a>"
            + "<img src='/assets/logo.png'/>"
            + "<style>body{background:url(/assets/bg.png)}</style>"
            + "<a href=\"//cdn.example/x\">ext</a>";
    byte[] out =
        VirtualSitePreviewHtml.rewriteRootRelative(
            html.getBytes(StandardCharsets.UTF_8), "/services/sites/Help/virtual/preview");
    String text = new String(out, StandardCharsets.UTF_8);
    assertTrue(text.contains("href=\"/services/sites/Help/virtual/preview/8.2/index.html\""), text);
    assertTrue(text.contains("src='/services/sites/Help/virtual/preview/assets/logo.png'"), text);
    assertTrue(text.contains("url(/services/sites/Help/virtual/preview/assets/bg.png)"), text);
    assertTrue(text.contains("href=\"//cdn.example/x\""), text);
  }

  @Test
  public void rewriteRejectsUnsafePrefix() {
    String html = "<a href=\"/8.2/index.html\">x</a>";
    byte[] out =
        VirtualSitePreviewHtml.rewriteRootRelative(
            html.getBytes(StandardCharsets.UTF_8), "../evil");
    assertEquals(html, new String(out, StandardCharsets.UTF_8));
  }

  @Test
  public void previewPrefixJoinsBaseAndEncodedName() {
    assertEquals(
        "/services/sites/Help/virtual/preview",
        VirtualSitePreviewHtml.previewPrefix("/services/", "Help"));
    assertEquals(
        "/Rhythmyx/services/sites/Help%20Docs/virtual/preview",
        VirtualSitePreviewHtml.previewPrefix("/Rhythmyx/services", "Help Docs"));
    assertFalse(VirtualSitePreviewHtml.previewPrefix("/services/", "Help").endsWith("/"));
  }
}
