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
package com.percussion.services.pubserver.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 * Golden / round-trip tests for {@link PSPubServer} / {@link PSPubServerProperty} under the
 * Jackson-backed {@code PSXmlSerializationHelper} (issue #1919, epic #505). Offline only.
 */
class PSPubServerXmlSerializationTest {

  @Test
  void writeEmitsPropertiesAndSuppressesComputedFlags() throws Exception {
    PSPubServer original = samplePubServer();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "pub-server"), "root: " + xml);
    assertTrue(containsTag(xml, "properties"), xml);
    assertTrue(containsTag(xml, "pub-server-property"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(containsTag(xml, "server-type"), xml);
    assertTrue(containsTag(xml, "publish-type"), xml);
    assertTrue(containsTag(xml, "has-full-published"), xml);
    assertTrue(containsTag(xml, "site-renamed"), xml);
    assertTrue(xml.contains("Default_Server"), xml);
    assertTrue(xml.contains("folder"), xml);
    assertFalse(containsTag(xml, "xml-format"), "computed: " + xml);
    assertFalse(containsTag(xml, "database-type"), "computed: " + xml);
    assertFalse(containsTag(xml, "ftp-type"), "computed: " + xml);
    assertFalse(containsTag(xml, "publish-server"), "runtime DTS lookup: " + xml);
  }

  @Test
  void writeMatchesGoldenFixture() throws Exception {
    String xml = samplePubServer().toXML();
    String golden = loadResource("com/percussion/services/pubserver/data/ps-pub-server-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void roundTripRestoresScalarsAndProperties() throws Exception {
    PSPubServer original = samplePubServer();
    String xml = original.toXML();

    PSPubServer restored = new PSPubServer();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescriptionXml(), restored.getDescriptionXml());
    assertEquals(original.getPublishType(), restored.getPublishType());
    assertEquals(original.getServerTypeXml(), restored.getServerTypeXml());
    assertEquals(original.getServerId(), restored.getServerId());
    assertEquals(original.getSiteId(), restored.getSiteId());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(original.hasFullPublished(), restored.hasFullPublished());
    assertEquals(original.getSiteRenamed(), restored.getSiteRenamed());
    assertEquals(propertyMap(original), propertyMap(restored));
  }

  @Test
  void fromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <description>Legacy pub server</description>
          <guid>0-152-401</guid>
          <has-full-published>false</has-full-published>
          <name>Legacy_Server</name>
          <properties>
            <pub-server-property>
              <name>folder</name>
              <value>/sites/legacy</value>
            </pub-server-property>
          </properties>
          <publish-type>filesystem</publish-type>
          <server-id>401</server-id>
          <server-type>PRODUCTION</server-type>
          <site-id>301</site-id>
          <site-renamed>false</site-renamed>
        </null>
        """;

    PSPubServer restored = new PSPubServer();
    restored.fromXML(legacy);

    assertEquals("Legacy_Server", restored.getName());
    assertEquals("Legacy pub server", restored.getDescriptionXml());
    assertEquals("filesystem", restored.getPublishType());
    assertEquals("PRODUCTION", restored.getServerTypeXml());
    assertEquals(401L, restored.getServerId());
    assertEquals(301L, restored.getSiteId());
    assertEquals("/sites/legacy", restored.getPropertyValue("folder").orElse(null));
    assertFalse(restored.hasFullPublished());
    assertFalse(restored.getSiteRenamed());
  }

  private static PSPubServer samplePubServer() {
    PSPubServer server = new PSPubServer();
    server.setGUID(new PSGuid(PSTypeEnum.PUBLISHING_SERVER, 401L));
    server.setServerId(401L);
    server.setSiteId(301L);
    server.setName("Default_Server");
    server.setDescription("Default publishing server");
    server.setPublishType("filesystem");
    server.setServerType(PSPubServer.PRODUCTION);
    server.setHasFullPublished(true);
    server.setSiteRenamed(false);

    PSPubServerProperty folder = new PSPubServerProperty();
    folder.setPropertyId(1L);
    folder.setServerId(401L);
    folder.setName("folder");
    folder.setValue("/sites/default");

    PSPubServerProperty format = new PSPubServerProperty();
    format.setPropertyId(2L);
    format.setServerId(401L);
    format.setName("format");
    format.setValue("HTML");

    server.getProperties().add(folder);
    server.getProperties().add(format);
    return server;
  }

  private static Map<String, String> propertyMap(PSPubServer server) {
    Map<String, String> out = new TreeMap<>();
    for (PSPubServerProperty p : server.getProperties()) {
      out.put(p.getName(), p.getValueXml());
    }
    return out;
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSPubServerXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
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
