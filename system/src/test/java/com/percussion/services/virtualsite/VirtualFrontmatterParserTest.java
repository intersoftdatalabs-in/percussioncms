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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.virtualsite.VirtualFrontmatterParser.Parsed;
import org.junit.jupiter.api.Test;

class VirtualFrontmatterParserTest {

  @Test
  void parsesRequiredFieldsAndBody() throws Exception {
    String text =
        "---\n"
            + "id: install-overview\n"
            + "title: Installation Overview\n"
            + "order: 20\n"
            + "tags: [install, admin]\n"
            + "---\n"
            + "\n"
            + "# Hello\n";
    Parsed p = VirtualFrontmatterParser.parse(text, "8.2", "install.md");
    assertEquals("install-overview", p.frontmatter().id());
    assertEquals("Installation Overview", p.frontmatter().title());
    assertEquals("8.2", p.frontmatter().version());
    assertEquals(20, p.frontmatter().order());
    assertEquals(2, p.frontmatter().tags().size());
    assertTrue(p.body().contains("# Hello"));
  }

  @Test
  void acceptsCrlfLineEndings() throws Exception {
    String text =
        "---\r\n"
            + "id: a\r\n"
            + "title: A\r\n"
            + "---\r\n"
            + "Body\r\n";
    Parsed p = VirtualFrontmatterParser.parse(text, "8.2", "a.md");
    assertEquals("a", p.frontmatter().id());
    assertTrue(p.body().contains("Body"));
  }

  @Test
  void missingIdFails() {
    String text = "---\ntitle: Only title\n---\n";
    assertThrows(
        VirtualSiteException.class,
        () -> VirtualFrontmatterParser.parse(text, "8.2", "x.md"));
  }

  @Test
  void missingFrontmatterFails() {
    assertThrows(
        VirtualSiteException.class,
        () -> VirtualFrontmatterParser.parse("# no fm\n", "8.2", "x.md"));
  }

  @Test
  void sidebarDefaultsTrue() throws Exception {
    String text = "---\nid: x\ntitle: X\n---\n";
    Parsed p = VirtualFrontmatterParser.parse(text, "8.2", "x.md");
    assertTrue(p.frontmatter().sidebar());
    assertFalse(p.frontmatter().deprecated());
  }
}
