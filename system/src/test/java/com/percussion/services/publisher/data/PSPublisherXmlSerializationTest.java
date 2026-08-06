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
package com.percussion.services.publisher.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSEdition;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / round-trip tests for publisher design objects under the Jackson-backed {@code
 * PSXmlSerializationHelper} (issue #1919, epic #505). Offline only — no live CMS.
 */
class PSPublisherXmlSerializationTest {

  @Test
  void contentListWriteEmitsNestedParamsAndSuppressesVersionMaps() throws Exception {
    PSContentList original = sampleContentList();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "content-list"), "root: " + xml);
    assertTrue(containsTag(xml, "generator-arguments"), xml);
    assertTrue(containsTag(xml, "content-list-generator-param"), xml);
    assertTrue(containsTag(xml, "expander-arguments"), xml);
    assertTrue(containsTag(xml, "template-expander-param"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(containsTag(xml, "filter-id"), xml);
    assertTrue(containsTag(xml, "content-list-type"), xml);
    assertTrue(containsTag(xml, "edition-type"), xml);
    assertTrue(xml.contains("sample_clist"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertFalse(containsTag(xml, "generator-params"), "map form suppressed: " + xml);
    assertFalse(containsTag(xml, "expander-params"), "map form suppressed: " + xml);
    assertFalse(xml.matches("(?s).*<filter(\\s|>).*"), "filter object suppressed: " + xml);
  }

  @Test
  void contentListWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleContentList().toXML();
    String golden =
        loadResource("com/percussion/services/publisher/data/ps-content-list-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void contentListRoundTripRestoresScalarsParamsAndFilter() throws Exception {
    PSContentList original = sampleContentList();
    String xml = original.toXML();

    PSContentList restored = new PSContentList();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getUrl(), restored.getUrl());
    assertEquals(original.getGenerator(), restored.getGenerator());
    assertEquals(original.getExpander(), restored.getExpander());
    assertEquals(original.getEditionType(), restored.getEditionType());
    assertEquals(original.getContentListType(), restored.getContentListType());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(original.getFilterId().toString(), restored.getFilterId().toString());
    assertEquals(
        sortedParams(original.getGeneratorParams()), sortedParams(restored.getGeneratorParams()));
    assertEquals(
        sortedParams(original.getExpanderParams()), sortedParams(restored.getExpanderParams()));
  }

  @Test
  void contentListFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <content-list-id>501</content-list-id>
          <content-list-type>NORMAL</content-list-type>
          <description>Legacy list</description>
          <edition-type>AUTOMATIC</edition-type>
          <expander>Java/global/percussion/system/sys_TemplateExpand</expander>
          <expander-arguments>
            <template-expander-param>
              <name>template</name>
              <value>rffSnTitleLink</value>
            </template-expander-param>
          </expander-arguments>
          <filter-id>0-7-11</filter-id>
          <generator>Java/global/percussion/system/sys_SearchGenerator</generator>
          <generator-arguments>
            <content-list-generator-param>
              <name>query</name>
              <value>//*</value>
            </content-list-generator-param>
          </generator-arguments>
          <guid>0-21-501</guid>
          <name>legacy_clist</name>
          <url>../sys_cxSupport/contentlist.xml</url>
        </null>
        """;

    PSContentList restored = new PSContentList();
    restored.fromXML(legacy);

    assertEquals("legacy_clist", restored.getName());
    assertEquals("Legacy list", restored.getDescription());
    assertEquals(501L, restored.getContentListId());
    assertEquals(IPSContentList.Type.NORMAL, restored.getContentListType());
    assertEquals(PSEditionType.AUTOMATIC, restored.getEditionType());
    assertEquals("0-7-11", restored.getFilterId().toString());
    assertEquals("//*", restored.getGeneratorParams().get("query"));
    assertEquals("rffSnTitleLink", restored.getExpanderParams().get("template"));
  }

  @Test
  void editionWriteSuppressesGuidNameAndVersion() throws Exception {
    PSEdition original = sampleEdition();
    String xml = original.toXML();

    assertNotNull(xml);
    assertTrue(containsTag(xml, "edition"), xml);
    assertTrue(containsTag(xml, "display-title"), xml);
    assertTrue(containsTag(xml, "comment"), xml);
    assertTrue(containsTag(xml, "edition-type"), xml);
    assertTrue(containsTag(xml, "site-id"), xml);
    assertTrue(containsTag(xml, "pub-server-id"), xml);
    assertTrue(containsTag(xml, "priority"), xml);
    assertTrue(containsTag(xml, "id"), xml);
    assertFalse(containsTag(xml, "guid"), "historical guid suppressed: " + xml);
    assertFalse(containsTag(xml, "version"), xml);
    assertFalse(xml.matches("(?s).*<name(\\s|>).*"), "name alias of display-title: " + xml);
    assertTrue(xml.contains("Publish Site"), xml);
  }

  @Test
  void editionWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleEdition().toXML();
    String golden = loadResource("com/percussion/services/publisher/data/ps-edition-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void editionRoundTripRestoresScalars() throws Exception {
    PSEdition original = sampleEdition();
    String xml = original.toXML();

    PSEdition restored = new PSEdition();
    restored.fromXML(xml);

    assertEquals(original.getId(), restored.getId());
    assertEquals(original.getDisplayTitle(), restored.getDisplayTitle());
    assertEquals(original.getComment(), restored.getComment());
    assertEquals(original.getEditionType(), restored.getEditionType());
    assertEquals(original.getPriority(), restored.getPriority());
    assertEquals(original.getSiteId().toString(), restored.getSiteId().toString());
    assertEquals(original.getPubServerId().toString(), restored.getPubServerId().toString());
  }

  @Test
  void editionFromXmlAcceptsLegacyNullRootAndNullSite() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <comment>c</comment>
          <display-title>E1</display-title>
          <edition-type>NORMAL</edition-type>
          <id>9001</id>
          <priority>HIGH</priority>
        </null>
        """;

    PSEdition restored = new PSEdition();
    restored.fromXML(legacy);

    assertEquals(9001L, restored.getId());
    assertEquals("E1", restored.getDisplayTitle());
    assertEquals("c", restored.getComment());
    assertEquals(PSEditionType.NORMAL, restored.getEditionType());
    assertEquals(IPSEdition.Priority.HIGH, restored.getPriority());
    assertNull(restored.getSiteId());
    assertNull(restored.getPubServerId());
  }

  @Test
  void deliveryTypeWriteAndRoundTrip() throws Exception {
    PSDeliveryType original = sampleDeliveryType();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "delivery-type"), xml);
    assertTrue(containsTag(xml, "bean-name"), xml);
    assertTrue(containsTag(xml, "unpublishing-requires-assembly"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(xml.contains("filesystem"), xml);

    String golden =
        loadResource("com/percussion/services/publisher/data/ps-delivery-type-golden.xml");
    assertLogicalXmlParity(golden, xml);

    PSDeliveryType restored = new PSDeliveryType();
    restored.fromXML(xml);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getBeanName(), restored.getBeanName());
    assertEquals(
        original.isUnpublishingRequiresAssembly(), restored.isUnpublishingRequiresAssembly());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
  }

  @Test
  void deliveryTypeFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <bean-name>sys_fileDeliveryHandler</bean-name>
          <description>d</description>
          <guid>0-112-7</guid>
          <name>filesystem</name>
          <unpublishing-requires-assembly>false</unpublishing-requires-assembly>
        </null>
        """;
    PSDeliveryType restored = new PSDeliveryType();
    restored.fromXML(legacy);
    assertEquals("filesystem", restored.getName());
    assertEquals("sys_fileDeliveryHandler", restored.getBeanName());
    assertFalse(restored.isUnpublishingRequiresAssembly());
    assertEquals(7L, restored.getGUID().getUUID());
  }

  private static PSContentList sampleContentList() {
    PSContentList list = new PSContentList();
    list.setGUID(new PSGuid(PSTypeEnum.CONTENT_LIST, 501L));
    list.setName("sample_clist");
    list.setDescription("Sample content list");
    list.setUrl("../sys_cxSupport/contentlist.xml?sys_deliverytype=filesystem");
    list.setGenerator("Java/global/percussion/system/sys_SearchGenerator");
    list.setExpander("Java/global/percussion/system/sys_TemplateExpand");
    list.setEditionType(PSEditionType.AUTOMATIC);
    list.setContentListType(IPSContentList.Type.NORMAL);
    list.setFilterId(new PSGuid(PSTypeEnum.ITEM_FILTER, 11L));

    PSContentListGeneratorParam g = new PSContentListGeneratorParam();
    g.setId(1L);
    g.setName("query");
    g.setValue("//*[jcr:primaryType='percPage']");
    g.setContentList(list);

    PSTemplateExpanderParam e = new PSTemplateExpanderParam();
    e.setId(2L);
    e.setName("template");
    e.setValue("perc.page");
    e.setContentList(list);

    list.getGeneratorArguments().add(g);
    list.getExpanderArguments().add(e);
    return list;
  }

  private static PSEdition sampleEdition() {
    PSEdition edition = new PSEdition();
    edition.setId(9001L);
    edition.setDisplayTitle("Publish Site");
    edition.setComment("Full site publish");
    edition.setEditionType(PSEditionType.NORMAL);
    edition.setPriority(IPSEdition.Priority.MEDIUM);
    edition.setSiteId(new PSGuid(PSTypeEnum.SITE, 301L));
    edition.setPubServerId(new PSGuid(PSTypeEnum.PUBLISHING_SERVER, 401L));
    return edition;
  }

  private static PSDeliveryType sampleDeliveryType() {
    PSDeliveryType dt = new PSDeliveryType();
    dt.setGUID(new PSGuid(PSTypeEnum.DELIVERY_TYPE, 7L));
    dt.setName("filesystem");
    dt.setDescription("Publish to the filesystem");
    dt.setBeanName("sys_fileDeliveryHandler");
    dt.setUnpublishingRequiresAssembly(false);
    return dt;
  }

  private static Map<String, String> sortedParams(Map<String, String> in) {
    return new TreeMap<>(in == null ? new HashMap<>() : in);
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSPublisherXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
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
    java.util.List<Node> eChildren = significantChildren(expected);
    java.util.List<Node> aChildren = significantChildren(actual);
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

  private static java.util.List<Node> significantChildren(Element el) {
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

  private static String summarize(java.util.List<Node> nodes) {
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
