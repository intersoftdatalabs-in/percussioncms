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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VirtualNavBuilderTest {

  @Test
  void toHrefReplacesMarkdownExtension() {
    assertEquals(
        "8.2/getting-started/install.html",
        VirtualNavBuilder.toHref(Path.of("8.2", "getting-started", "install.md")));
  }

  @Test
  void buildsSectionWithChildren() {
    VirtualSiteConfig config =
        new VirtualSiteConfig(
            Path.of("root"),
            "T",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(new VirtualSiteConfig.NavSpec("Getting Started", "getting-started")),
            "k");

    List<VirtualItemRef> refs =
        List.of(
            new VirtualItemRef(
                "getting-started",
                "8.2",
                Path.of("8.2", "getting-started", "index.md"),
                10,
                "Getting Started"),
            new VirtualItemRef(
                "install-overview",
                "8.2",
                Path.of("8.2", "getting-started", "install.md"),
                20,
                "Install"));

    List<VirtualNavNode> nav = VirtualNavBuilder.build("8.2", refs, config);
    assertEquals(1, nav.size());
    assertEquals("Getting Started", nav.get(0).title());
    assertTrue(nav.get(0).children().stream().anyMatch(c -> "install-overview".equals(c.id())));
  }
}
