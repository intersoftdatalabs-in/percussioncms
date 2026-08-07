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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Focused unit tests for inline link title resolve + fallback (#2242 / parent #946). Broad
 * Playwright / Vitest residual is #2243.
 */
@Tag("UnitTest")
class PSInlineLinkTitleResolverTest {

  @Test
  @DisplayName("unset config uses type default (asset BC = displaytitle value)")
  void unsetConfig_usesTypeDefault() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("displaytitle", "Asset Display Title");
    fields.put("pagetitle", "Should Not Win");

    assertEquals(
        "Asset Display Title",
        PSInlineLinkTitleResolver.resolve(null, fields, "Asset Display Title"));
    assertEquals(
        "Asset Display Title", PSInlineLinkTitleResolver.resolve("", fields, "Asset Display Title"));
    assertEquals(
        "Asset Display Title",
        PSInlineLinkTitleResolver.resolve("   ", fields, "Asset Display Title"));
  }

  @Test
  @DisplayName("configured field wins when present and non-empty")
  void configuredField_winsWhenPresent() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("displaytitle", "Display");
    fields.put("pagetitle", "Page Title Custom");
    fields.put("resource_link_title", "Link Title");

    assertEquals(
        "Page Title Custom",
        PSInlineLinkTitleResolver.resolve("pagetitle", fields, "Link Title"));
  }

  @Test
  @DisplayName("empty configured field falls back to displaytitle")
  void emptyConfigured_fallsBackToDisplaytitle() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("pagetitle", "   ");
    fields.put("displaytitle", "From Displaytitle");
    fields.put("resource_link_title", "Link Default");

    assertEquals(
        "From Displaytitle",
        PSInlineLinkTitleResolver.resolve("pagetitle", fields, "Link Default"));
  }

  @Test
  @DisplayName("missing configured field falls back to displaytitle")
  void missingConfigured_fallsBackToDisplaytitle() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("displaytitle", "Asset Title");

    assertEquals(
        "Asset Title",
        PSInlineLinkTitleResolver.resolve("nonexistent_field", fields, "type-default"));
  }

  @Test
  @DisplayName("displaytitle empty after failed custom falls through to type default")
  void displaytitleEmpty_usesTypeDefault() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("pagetitle", "");
    fields.put("displaytitle", null);

    assertEquals(
        "Page Link Title",
        PSInlineLinkTitleResolver.resolve("pagetitle", fields, "Page Link Title"));
  }

  @Test
  @DisplayName("configured displaytitle that is empty uses type default (no double-lookup loop)")
  void configuredDisplaytitleEmpty_usesTypeDefault() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("displaytitle", "");

    assertEquals(
        "type-default",
        PSInlineLinkTitleResolver.resolve("displaytitle", fields, "type-default"));
  }

  @Test
  @DisplayName("page type default (resource_link_title) preserved when no config")
  void pageTypeDefault_resourceLinkTitle() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("resource_link_title", "Nav Title");
    fields.put("page_title", "Browser Title");
    fields.put("displaytitle", "Would Be Wrong For Page BC");

    // Unset config must match pre-feature: page.getLinkTitle() passed as typeDefault
    assertEquals(
        "Nav Title",
        PSInlineLinkTitleResolver.resolve(null, fields, "Nav Title"));
  }

  @Test
  @DisplayName("page custom page_title with displaytitle missing uses type default")
  void pageCustomMissingDisplaytitle_usesTypeDefault() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("page_title", "");
    // pages often have no shared displaytitle

    assertEquals(
        "Nav Title",
        PSInlineLinkTitleResolver.resolve("page_title", fields, "Nav Title"));
  }

  @Test
  @DisplayName("null fields map is safe")
  void nullFields_safe() {
    assertEquals("default", PSInlineLinkTitleResolver.resolve("pagetitle", null, "default"));
    assertEquals("", PSInlineLinkTitleResolver.resolve(null, null, null));
  }

  @Test
  @DisplayName("fieldAsString trims and ignores blank")
  void fieldAsString_trims() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("a", "  hi  ");
    fields.put("b", "   ");
    assertEquals("hi", PSInlineLinkTitleResolver.fieldAsString(fields, "a"));
    assertNull(PSInlineLinkTitleResolver.fieldAsString(fields, "b"));
    assertNull(PSInlineLinkTitleResolver.fieldAsString(fields, "missing"));
  }

  @Test
  @DisplayName("fieldAsString matches field name case-insensitively")
  void fieldAsString_caseInsensitiveKey() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("PageTitle", "Mixed Case Key");
    assertEquals("Mixed Case Key", PSInlineLinkTitleResolver.fieldAsString(fields, "pagetitle"));
  }
}
