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

package com.percussion.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit tests for {@link PSExtensionMethod} XML round-trip, including legacy package attribute
 * spelling {@code returntype} (lowercase) used by shipped {@code .extension} files such as
 * pageutils in perc.Baseline.
 */
public class PSExtensionMethodTest {

  @Test
  public void testFromXmlAcceptsCanonicalReturnTypeAttribute() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSExtensionMethod.XML_NAME);
    el.setAttribute("name", "itemLink");
    el.setAttribute(PSExtensionMethod.XML_ATTR_RETURN_TYPE, "java.lang.String");
    el.setAttribute("description", "canonical");

    PSExtensionMethod method = new PSExtensionMethod(el);
    assertEquals("itemLink", method.getName());
    assertEquals("java.lang.String", method.getReturnType());
    assertEquals("canonical", method.getDescription());
  }

  @Test
  public void testFromXmlAcceptsLegacyLowercaseReturntypeAttribute() throws Exception {
    // Matches shipped package XML: returntype="java.util.List"
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSExtensionMethod.XML_NAME);
    el.setAttribute("name", "getProcessedCategories");
    el.setAttribute(PSExtensionMethod.XML_ATTR_RETURN_TYPE_LEGACY, "java.util.List");
    el.setAttribute("description", "legacy package spelling");

    PSExtensionMethod method = new PSExtensionMethod(el);
    assertEquals("getProcessedCategories", method.getName());
    assertEquals("java.util.List", method.getReturnType());
  }

  @Test
  public void testFromXmlPrefersCanonicalWhenBothPresent() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSExtensionMethod.XML_NAME);
    el.setAttribute("name", "both");
    el.setAttribute(PSExtensionMethod.XML_ATTR_RETURN_TYPE, "java.lang.String");
    el.setAttribute(PSExtensionMethod.XML_ATTR_RETURN_TYPE_LEGACY, "java.util.List");

    PSExtensionMethod method = new PSExtensionMethod(el);
    assertEquals("java.lang.String", method.getReturnType());
  }

  @Test
  public void testFromXmlMissingReturnTypeFails() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSExtensionMethod.XML_NAME);
    el.setAttribute("name", "broken");
    // neither returnType nor returntype

    PSExtensionException ex =
        assertThrows(PSExtensionException.class, () -> new PSExtensionMethod(el));
    assertTrue(
        ex.getCause() instanceof IllegalArgumentException
            || (ex.getMessage() != null && ex.getMessage().contains("Failed to deserialize")));
  }

  @Test
  public void testToXmlRoundTripUsesCanonicalAttribute() throws Exception {
    PSExtensionMethod source =
        new PSExtensionMethod("productVersion", "java.lang.String", "version string");
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = source.toXML(doc);

    assertEquals("java.lang.String", el.getAttribute(PSExtensionMethod.XML_ATTR_RETURN_TYPE));
    assertTrue(
        el.getAttribute(PSExtensionMethod.XML_ATTR_RETURN_TYPE_LEGACY).isEmpty(),
        "toXML should write canonical returnType only");

    PSExtensionMethod restored = new PSExtensionMethod(el);
    assertEquals(source.getName(), restored.getName());
    assertEquals(source.getReturnType(), restored.getReturnType());
    assertEquals(source.getDescription(), restored.getDescription());
  }

  @Test
  public void testReadReturnTypeAttributeHelpers() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSExtensionMethod.XML_NAME);
    el.setAttribute(PSExtensionMethod.XML_ATTR_RETURN_TYPE_LEGACY, "boolean");
    assertEquals("boolean", PSExtensionMethod.readReturnTypeAttribute(el));
  }
}
