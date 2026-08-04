/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 */
class PSDirectoryWidgetSchemaOrgTest {

  private static final String DIRECTORY_WIDGET_RESOURCE =
      "/Packages/perc.widget.directory/sys__UserDependency--rxconfig/Widgets/percDirectory.xml";

  private static final String PERSON_WIDGET_RESOURCE =
      "/Packages/perc.widget.directory/sys__UserDependency--rxconfig/Widgets/percPerson.xml";

  @Test
  void percDirectoryXml_containsSchemaOrgJsonLdMarkers() throws IOException {
    String xml = readClasspathResource(DIRECTORY_WIDGET_RESOURCE);

    assertTrue(
        xml.contains("http://schema.org"),
        "percDirectory.xml must set JSON-LD @context to schema.org");
    assertTrue(
        xml.contains("application/ld+json"),
        "percDirectory.xml must emit application/ld+json script type");
    assertTrue(
        xml.contains("ItemList"), "percDirectory.xml must use Schema.org ItemList for directory");
    assertTrue(
        xml.contains("perc-directory-schema-"),
        "percDirectory.xml must use multi-instance script id prefix perc-directory-schema-");
    assertTrue(
        xml.contains("setAdditionalHeadContent"),
        "percDirectory.xml must inject JSON-LD via setAdditionalHeadContent");
    assertTrue(
        xml.contains("getJSONObject()"),
        "percDirectory.xml must build JSON-LD with $rx.string.getJSONObject");
    assertTrue(
        xml.contains("getJSONArray()"),
        "percDirectory.xml must build itemListElement with $rx.string.getJSONArray");
    assertTrue(
        xml.contains("\"Person\"") || xml.contains("\"@type\", \"Person\""),
        "percDirectory.xml must type directory entries as Person");
    assertTrue(
        xml.contains("itemListElement"),
        "percDirectory.xml must populate Schema.org itemListElement");
  }

  @Test
  void percPersonXml_stillContainsPeerSchemaOrgMarkers() throws IOException {
    // Guard against accidental package resource packaging breakage while asserting peer parity.
    String xml = readClasspathResource(PERSON_WIDGET_RESOURCE);
    assertTrue(xml.contains("http://schema.org"));
    assertTrue(xml.contains("application/ld+json"));
    assertTrue(xml.contains("setAdditionalHeadContent"));
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
