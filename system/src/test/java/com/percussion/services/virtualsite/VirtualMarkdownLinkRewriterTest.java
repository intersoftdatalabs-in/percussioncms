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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class VirtualMarkdownLinkRewriterTest {

  @Test
  void rewritesRelativeMarkdownToSiteRootHtml() {
    String body = "See [Getting Started](getting-started/index.md).";
    String out =
        VirtualMarkdownLinkRewriter.rewrite(body, "8.2/index.md", Map.of());
    assertTrue(out.contains("(/8.2/getting-started/index.html)"), out);
  }

  @Test
  void rewritesStableIdLinks() {
    String body = "Jump [Install](id:install-overview).";
    String out =
        VirtualMarkdownLinkRewriter.rewrite(
            body,
            "8.2/index.md",
            Map.of("install-overview", "8.2/getting-started/install.html"));
    assertTrue(out.contains("(/8.2/getting-started/install.html)"), out);
  }

  @Test
  void leavesExternalAndAnchorsAlone() {
    String body = "[a](https://example.com) [b](#frag) [c](mailto:x@y.z)";
    String out = VirtualMarkdownLinkRewriter.rewrite(body, "8.2/index.md", Map.of());
    assertEquals(body, out);
  }

  @Test
  void preservesTitleAndFragment() {
    String body = "[x](install.md#section \"title\")";
    String out =
        VirtualMarkdownLinkRewriter.rewrite(
            body, "8.2/getting-started/index.md", Map.of());
    assertEquals("[x](/8.2/getting-started/install.html#section \"title\")", out);
  }

  @Test
  void toSiteRootHrefPrefixesSlash() {
    assertEquals("/8.2/index.html", VirtualMarkdownLinkRewriter.toSiteRootHref("8.2/index.html"));
    assertEquals("/a.html", VirtualMarkdownLinkRewriter.toSiteRootHref("/a.html"));
  }
}
