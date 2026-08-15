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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSItemCreateSupportTest {

  @Test
  void repositoryFolderPathUsesCmsSlashes() {
    assertEquals("//Sites/Demo", PSItemCreateSupport.toRepositoryFolderPath("/Sites/Demo"));
    assertEquals("//Sites/Demo", PSItemCreateSupport.toRepositoryFolderPath("//Sites/Demo"));
    assertEquals("//Sites/Demo", PSItemCreateSupport.toRepositoryFolderPath("Sites/Demo"));
    assertNull(PSItemCreateSupport.toRepositoryFolderPath("  "));
  }

  @Test
  void sanitizeStripsPathAndAddsHtmlForPages() {
    assertEquals("Hello", PSItemCreateSupport.sanitizeItemName("Hello", "rffEvent"));
    assertEquals("Hello.html", PSItemCreateSupport.sanitizeItemName("Hello", "percPage"));
    assertEquals("file", PSItemCreateSupport.sanitizeItemName("/Sites/x/file", "rffEvent"));
    assertTrue(PSItemCreateSupport.sanitizeItemName(null, "rffEvent").startsWith("New-rffEvent-"));
    assertFalse(PSItemCreateSupport.sanitizeItemName("a/b\\c", "t").contains("/"));
  }

  @Test
  void detectsPageType() {
    assertTrue(PSItemCreateSupport.isPageType("percPage"));
    assertTrue(PSItemCreateSupport.isPageType("perc_page"));
    assertFalse(PSItemCreateSupport.isPageType("rffEvent"));
  }

  @Test
  void siteNameFromFolderPath() {
    assertEquals("Demo", PSItemCreateSupport.siteNameFromFolderPath("/Sites/Demo/Home"));
    assertEquals("Demo", PSItemCreateSupport.siteNameFromFolderPath("//Sites/Demo"));
    assertNull(PSItemCreateSupport.siteNameFromFolderPath("/Folders/Assets"));
  }

  @Test
  void titleFromItemNameStripsHtmlSuffix() {
    assertEquals("New Page", PSItemCreateSupport.titleFromItemName("New Page.html"));
    assertEquals("About", PSItemCreateSupport.titleFromItemName("About"));
    assertEquals("New Page", PSItemCreateSupport.titleFromItemName(null));
  }
}
