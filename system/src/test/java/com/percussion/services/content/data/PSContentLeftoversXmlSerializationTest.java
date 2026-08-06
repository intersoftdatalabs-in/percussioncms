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
package com.percussion.services.content.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / round-trip tests for content-domain leftovers under the Jackson-backed {@code
 * PSXmlSerializationHelper} (issue #1921, epic #505). Offline only — no live CMS.
 *
 * <p>Covers: {@link PSAutoTranslation}, {@link PSContentTypeSummary} / {@link
 * PSContentTypeSummaryChild}, {@link PSFieldDescription}, {@link PSFolderProperty}, {@link
 * PSItemStatus}.
 */
class PSContentLeftoversXmlSerializationTest {

  @Test
  void autoTranslationWriteSuppressesCatalogAliasesAndOptionals() throws Exception {
    PSAutoTranslation original = sampleAutoTranslation();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "auto-translation"), xml);
    assertTrue(containsTag(xml, "content-type-id"), xml);
    assertTrue(containsTag(xml, "locale"), xml);
    assertTrue(xml.contains("en-us"), xml);
    assertFalse(containsTag(xml, "version"), xml);
    assertFalse(containsTag(xml, "key"), xml);
    assertFalse(containsTag(xml, "guid"), "shared auto-translations guid suppressed: " + xml);
    assertFalse(containsTag(xml, "community-id-optional"), xml);
    assertFalse(containsTag(xml, "content-type-id-optional"), xml);
    assertFalse(containsTag(xml, "locale-optional"), xml);
    assertFalse(xml.matches("(?s).*<name(\\s|>).*"), "catalog name suppressed: " + xml);
    assertFalse(xml.matches("(?s).*<label(\\s|>).*"), "catalog label suppressed: " + xml);
    assertFalse(
        xml.matches("(?s).*<description(\\s|>).*"), "catalog description suppressed: " + xml);
  }

  @Test
  void autoTranslationWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleAutoTranslation().toXML();
    String golden =
        loadResource("com/percussion/services/content/data/ps-auto-translation-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void autoTranslationRoundTripRestoresScalars() throws Exception {
    PSAutoTranslation original = sampleAutoTranslation();
    String xml = original.toXML();

    PSAutoTranslation restored = new PSAutoTranslation();
    restored.fromXML(xml);

    assertEquals(original.getContentTypeId(), restored.getContentTypeId());
    assertEquals(original.getLocale(), restored.getLocale());
    assertEquals(original.getWorkflowId(), restored.getWorkflowId());
    assertEquals(original.getCommunityId(), restored.getCommunityId());
    assertEquals(original.getContentTypeName(), restored.getContentTypeName());
    assertEquals(original.getWorkflowName(), restored.getWorkflowName());
    assertEquals(original.getCommunityName(), restored.getCommunityName());
  }

  @Test
  void fieldDescriptionRoundTripAndGolden() throws Exception {
    PSFieldDescription original =
        new PSFieldDescription("sys_title", PSFieldDescription.PSFieldTypeEnum.TEXT.name(), true);
    String xml = original.toXML();
    assertTrue(containsTag(xml, "field-description"), xml);
    assertTrue(xml.contains("sys_title"), xml);
    assertTrue(xml.contains("TEXT"), xml);

    String golden =
        loadResource("com/percussion/services/content/data/ps-field-description-golden.xml");
    assertLogicalXmlParity(golden, xml);

    PSFieldDescription restored = new PSFieldDescription();
    restored.fromXML(xml);
    assertEquals(original, restored);
  }

  @Test
  void contentTypeSummaryWriteNestedTypesAndRoundTrip() throws Exception {
    PSContentTypeSummary original = sampleContentTypeSummary();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "content-type-summary"), xml);
    assertTrue(containsTag(xml, "field-description"), xml);
    assertTrue(containsTag(xml, "content-type-summary-child"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(xml.contains("0-2-311") || xml.contains("rxGeneric"), xml);

    String golden =
        loadResource("com/percussion/services/content/data/ps-content-type-summary-golden.xml");
    assertLogicalXmlParity(golden, xml);

    PSContentTypeSummary restored = new PSContentTypeSummary();
    restored.fromXML(xml);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getGuid().toString(), restored.getGuid().toString());
    assertEquals(original.getFields(), restored.getFields());
    assertEquals(original.getChildren(), restored.getChildren());
  }

  @Test
  void contentTypeSummaryChildStandaloneRoundTrip() throws Exception {
    PSContentTypeSummaryChild original = new PSContentTypeSummaryChild("rx_body");
    original.addField(
        new PSFieldDescription("body", PSFieldDescription.PSFieldTypeEnum.TEXT.name()));
    String xml = original.toXML();
    assertTrue(containsTag(xml, "content-type-summary-child"), xml);
    assertTrue(containsTag(xml, "field-description"), xml);

    PSContentTypeSummaryChild restored = new PSContentTypeSummaryChild();
    restored.fromXML(xml);
    assertEquals(original, restored);
  }

  @Test
  void folderPropertyRoundTripAndGolden() throws Exception {
    PSFolderProperty original =
        new PSFolderProperty(1001L, 1L, 50L, "sys_pubFileName", "index.html", "Publish file name");
    String xml = original.toXML();
    assertTrue(containsTag(xml, "folder-property"), xml);
    assertTrue(containsTag(xml, "content-id"), xml);
    assertTrue(containsTag(xml, "property-name"), xml);
    assertFalse(containsTag(xml, "content-id-optional"), xml);
    assertFalse(containsTag(xml, "property-name-optional"), xml);

    String golden =
        loadResource("com/percussion/services/content/data/ps-folder-property-golden.xml");
    assertLogicalXmlParity(golden, xml);

    PSFolderProperty restored = new PSFolderProperty();
    restored.fromXML(xml);
    assertEquals(original, restored);
  }

  @Test
  void itemStatusRoundTripAndGolden() throws Exception {
    PSItemStatus original = new PSItemStatus(335, true, true, 2L, "Draft", 3L, "Quick Edit");
    String xml = original.toXML();
    assertTrue(containsTag(xml, "item-status"), xml);
    assertTrue(containsTag(xml, "id"), xml);
    assertTrue(containsTag(xml, "did-checkout"), xml);
    assertTrue(xml.contains("Quick Edit"), xml);

    String golden = loadResource("com/percussion/services/content/data/ps-item-status-golden.xml");
    assertLogicalXmlParity(golden, xml);

    PSItemStatus restored = new PSItemStatus();
    restored.fromXML(xml);
    assertEquals(original, restored);
  }

  @Test
  void itemStatusAllowsNullStateNamesButRejectsEmpty() {
    PSItemStatus status = new PSItemStatus(42);
    // null is intentional: no transition / Jackson absent optional elements
    status.setFromState(null);
    status.setToState(null);
    assertNull(status.getFromState());
    assertNull(status.getToState());

    // Contract uses StringUtils.isEmpty: reject "" only (not whitespace-only)
    assertThrows(IllegalArgumentException.class, () -> status.setFromState(""));
    assertThrows(IllegalArgumentException.class, () -> status.setToState(""));
  }

  @Test
  void itemStatusWithoutTransitionRoundTripsNullStates() throws Exception {
    PSItemStatus original = new PSItemStatus(100, true, false, null, null, null, null);
    String xml = original.toXML();
    PSItemStatus restored = new PSItemStatus();
    restored.fromXML(xml);
    assertEquals(original.getId(), restored.getId());
    assertTrue(restored.isDidCheckout());
    assertFalse(restored.isDidTransition());
    assertNull(restored.getFromState());
    assertNull(restored.getToState());
    assertNull(restored.getFromStateId());
    assertNull(restored.getToStateId());
  }

  private static PSAutoTranslation sampleAutoTranslation() {
    PSAutoTranslation at = PSAutoTranslation.of(311L, "en-us", 6L, 1001L);
    at.setContentTypeName("rxGeneric");
    at.setWorkflowName("Simple Workflow");
    at.setCommunityName("Default");
    return at;
  }

  private static PSContentTypeSummary sampleContentTypeSummary() {
    PSContentTypeSummary sum = new PSContentTypeSummary();
    sum.setName("rxGeneric");
    sum.setDescription("Generic page content type");
    sum.setGuid(new PSGuid(PSTypeEnum.NODEDEF, 311L));
    sum.addField(
        new PSFieldDescription("sys_title", PSFieldDescription.PSFieldTypeEnum.TEXT.name(), true));
    sum.addField(
        new PSFieldDescription(
            "sys_contentstart", PSFieldDescription.PSFieldTypeEnum.DATE.name(), false));
    PSContentTypeSummaryChild child = new PSContentTypeSummaryChild("rx_body");
    child.addField(new PSFieldDescription("body", PSFieldDescription.PSFieldTypeEnum.TEXT.name()));
    sum.addChild(child);
    return sum;
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSContentLeftoversXmlSerializationTest.class
            .getClassLoader()
            .getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static void assertLogicalXmlParity(String expectedXml, String actualXml)
      throws Exception {
    Document expected = parseXml(stripXmlDeclaration(expectedXml));
    Document actual = parseXml(stripXmlDeclaration(actualXml));
    assertElementTreeEquals(expected.getDocumentElement(), actual.getDocumentElement(), "/");
  }

  private static String stripXmlDeclaration(String xml) {
    String s = Objects.requireNonNull(xml).trim();
    if (s.startsWith("<?xml")) {
      int end = s.indexOf("?>");
      if (end >= 0) {
        s = s.substring(end + 2).trim();
      }
    }
    while (s.startsWith("<!--")) {
      int end = s.indexOf("-->");
      if (end < 0) {
        break;
      }
      s = s.substring(end + 3).trim();
    }
    return s;
  }

  private static Document parseXml(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new java.io.StringReader(xml)));
  }

  private static void assertElementTreeEquals(Element expected, Element actual, String path) {
    assertEquals(expected.getTagName(), actual.getTagName(), "tag at " + path);
    List<Node> eChildren = significantChildren(expected);
    List<Node> aChildren = significantChildren(actual);
    assertEquals(
        eChildren.size(),
        aChildren.size(),
        "child count at "
            + path
            + " expected="
            + summarize(eChildren)
            + " actual="
            + summarize(aChildren));
    for (int i = 0; i < eChildren.size(); i++) {
      Node en = eChildren.get(i);
      Node an = aChildren.get(i);
      if (en.getNodeType() == Node.TEXT_NODE) {
        assertEquals(en.getTextContent().trim(), an.getTextContent().trim(), "text at " + path);
      } else {
        assertElementTreeEquals(
            (Element) en, (Element) an, path + "/" + ((Element) en).getTagName() + "[" + i + "]");
      }
    }
  }

  private static List<Node> significantChildren(Element el) {
    NodeList nl = el.getChildNodes();
    java.util.ArrayList<Node> out = new java.util.ArrayList<>();
    boolean hasElementChild = false;
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
        hasElementChild = true;
        break;
      }
    }
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        out.add(n);
      } else if (n.getNodeType() == Node.TEXT_NODE && !hasElementChild) {
        String t = n.getTextContent();
        if (t != null && !t.trim().isEmpty()) {
          out.add(n);
        }
      }
    }
    return out;
  }

  private static String summarize(List<Node> nodes) {
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < nodes.size(); i++) {
      if (i > 0) {
        b.append(',');
      }
      Node n = nodes.get(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        b.append(((Element) n).getTagName());
      } else {
        b.append("#text");
      }
    }
    return b.append(']').toString();
  }
}
