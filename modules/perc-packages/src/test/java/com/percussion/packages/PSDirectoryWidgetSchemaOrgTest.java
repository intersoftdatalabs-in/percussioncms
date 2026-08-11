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

package com.percussion.packages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Regression for issue #806: Directory widget package Velocity must emit Schema.org JSON-LD
 * (ItemList of Person entries) via additional head content, matching peer directory package widgets
 * (percPerson / percOrganization / percDepartment).
 *
 * <p>After batch B ship-exit (#2884), markers live under modern {@code widgets/&lt;stem&gt;/}
 * templates (and bindings) rather than committed install Widget XML.
 */
class PSDirectoryWidgetSchemaOrgTest {

  private static final String DIRECTORY_MODERN_TEMPLATE =
      "/Packages/perc.widget.directory/widgets/percDirectory/templates/percDirectorySnippet.vm";

  private static final String DIRECTORY_MODERN_MANIFEST =
      "/Packages/perc.widget.directory/widgets/percDirectory/component-package.json";

  private static final String PERSON_MODERN_TEMPLATE =
      "/Packages/perc.widget.directory/widgets/percPerson/templates/percPersonSnippet.vm";

  private static final String PERSON_MODERN_MANIFEST =
      "/Packages/perc.widget.directory/widgets/percPerson/component-package.json";

  @Test
  void percDirectoryXml_containsSchemaOrgJsonLdMarkers() throws IOException {
    // Template + bindings cover Schema.org markers split across modern package parts.
    String source =
        readClasspathResource(DIRECTORY_MODERN_TEMPLATE)
            + "\n"
            + readClasspathResource(DIRECTORY_MODERN_MANIFEST);

    assertTrue(
        source.contains("http://schema.org"),
        "percDirectory modern sources must set JSON-LD @context to schema.org");
    assertTrue(
        source.contains("application/ld+json"),
        "percDirectory modern sources must emit application/ld+json script type");
    assertTrue(
        source.contains("ItemList"),
        "percDirectory modern sources must use Schema.org ItemList for directory");
    assertTrue(
        source.contains("perc-directory-schema-"),
        "percDirectory modern sources must use multi-instance script id prefix perc-directory-schema-");
    assertTrue(
        source.contains("setAdditionalHeadContent"),
        "percDirectory modern sources must inject JSON-LD via setAdditionalHeadContent");
    assertTrue(
        source.contains("getJSONObject()"),
        "percDirectory modern sources must build JSON-LD with $rx.string.getJSONObject");
    assertTrue(
        source.contains("getJSONArray()"),
        "percDirectory modern sources must build itemListElement with $rx.string.getJSONArray");
    assertTrue(
        source.contains("\"Person\"") || source.contains("\"@type\", \"Person\""),
        "percDirectory modern sources must type directory entries as Person");
    assertTrue(
        source.contains("itemListElement"),
        "percDirectory modern sources must populate Schema.org itemListElement");
  }

  @Test
  void percPersonXml_stillContainsPeerSchemaOrgMarkers() throws IOException {
    // Guard against accidental package resource packaging breakage while asserting peer parity.
    String source =
        readClasspathResource(PERSON_MODERN_TEMPLATE)
            + "\n"
            + readClasspathResource(PERSON_MODERN_MANIFEST);
    assertTrue(source.contains("http://schema.org"));
    assertTrue(source.contains("application/ld+json"));
    assertTrue(source.contains("setAdditionalHeadContent"));
  }

  private static String readClasspathResource(String resourcePath) throws IOException {
    try (InputStream in = PSDirectoryWidgetSchemaOrgTest.class.getResourceAsStream(resourcePath)) {
      assertNotNull(in, "Classpath resource missing: " + resourcePath);
      String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertFalse(content.isBlank(), "Classpath resource empty: " + resourcePath);
      return content;
    }
  }
}
