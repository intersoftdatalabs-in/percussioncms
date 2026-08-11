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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Behavioral tests for typed namespace rule lists in {@link PSStylesheetCleanupFilter} after
 * rawtypes cleanup.
 */
@Tag("UnitTest")
class PSStylesheetCleanupFilterTypedTest {

  private PSStylesheetCleanupFilter filter;

  @BeforeEach
  void loadFilter() throws Exception {
    // Use a fresh instance via protected fromXml path on the singleton by reloading
    // the default document content (getInstance may already exist; reconfigure via fromXml).
    filter = PSStylesheetCleanupFilter.getInstance();
    try (ByteArrayInputStream bis =
        new ByteArrayInputStream(FILTER_XML.getBytes(StandardCharsets.UTF_8))) {
      Document doc = PSXmlDocumentBuilder.createXmlDocument(bis, false);
      filter.fromXml(doc.getDocumentElement());
    }
  }

  @Test
  void defaultNamespaceAllowsWildcardElementsAndAttributes() {
    assertTrue(filter.isNSElementAllowed("", "div"));
    assertTrue(filter.isNSAttributeAllowed("", "id"));
    assertTrue(filter.isNSDeclarationAllowed("", "http://www.w3.org/1999/xhtml"));
  }

  @Test
  void xmlNamespaceAllowsOnlyConfiguredAttributes() {
    assertTrue(filter.isNSAttributeAllowed("xml", "lang"));
    assertTrue(filter.isNSAttributeAllowed("xml", "space"));
    assertFalse(filter.isNSAttributeAllowed("xml", "base"));
    assertFalse(filter.isNSElementAllowed("xml", "anything"));
  }

  @Test
  void unknownNamespaceIsRejectedAndPrefixesAreIterable() {
    assertFalse(filter.isNSElementAllowed("x", "foo"));
    Iterator<String> prefixes = filter.getPrefixes();
    assertTrue(prefixes.hasNext());
  }

  private static final String FILTER_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
          + "<stylesheetCleanupFilter>"
          + "<allowedNamespace name=\"\" declAllowed=\"true \" declValue=\"*xhtml*\">"
          + "<allowedElement name=\"*\"/>"
          + "<allowedAttribute name=\"*\"/>"
          + "</allowedNamespace>"
          + "<allowedNamespace name=\"xml\" uri=\"http://www.w3.org\" declAllowed=\"false \">"
          + "<allowedAttribute name=\"lang\"/>"
          + "<allowedAttribute name=\"space\"/>"
          + "</allowedNamespace>"
          + "</stylesheetCleanupFilter>";
}
