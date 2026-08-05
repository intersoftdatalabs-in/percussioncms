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
package com.percussion.services.ui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
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
 * Golden / round-trip tests for workbench hierarchy design objects under the Jackson-backed {@code
 * PSXmlSerializationHelper} (issue #1920, epic #505). Offline only — no live CMS.
 */
class PSHierarchyNodeXmlSerializationTest {

  @Test
  void hierarchyNodeWriteShapeAndGolden() throws Exception {
    PSHierarchyNode original = sampleNode();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "hierarchy-node"), "root: " + xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(containsTag(xml, "name"), xml);
    assertTrue(containsTag(xml, "type"), xml);
    assertTrue(containsTag(xml, "parent-id"), xml);
    assertTrue(containsTag(xml, "properties"), xml);
    assertTrue(xml.contains("FOLDER"), xml);
    assertTrue(xml.contains("Sites"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertFalse(containsTag(xml, "type-int"), "ordinal form suppressed: " + xml);
    assertFalse(containsTag(xml, "label"), "catalog label alias suppressed: " + xml);
    assertFalse(containsTag(xml, "description"), "catalog description suppressed: " + xml);

    String golden = loadResource("com/percussion/services/ui/data/ps-hierarchy-node-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void hierarchyNodeRoundTripRestoresScalarsAndProperties() throws Exception {
    PSHierarchyNode original = sampleNode();
    String xml = original.toXML();

    PSHierarchyNode restored = new PSHierarchyNode();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getNodeType(), restored.getNodeType());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(original.getParentId().toString(), restored.getParentId().toString());
    assertEquals(original.getProperties(), restored.getProperties());
  }

  @Test
  void hierarchyNodeFromXmlAcceptsLegacyNullRootAndRootParent() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <guid>0-32-200</guid>
          <name>RootFolder</name>
          <properties>
            <icon>folder</icon>
          </properties>
          <type>FOLDER</type>
        </null>
        """;

    PSHierarchyNode restored = new PSHierarchyNode();
    restored.fromXML(legacy);

    assertEquals("RootFolder", restored.getName());
    assertEquals(PSHierarchyNode.NodeType.FOLDER, restored.getNodeType());
    assertEquals(200L, restored.getGUID().getUUID());
    assertNull(restored.getParentId());
    assertEquals("folder", restored.getProperty("icon"));
  }

  @Test
  void hierarchyNodePropertyWriteShapeAndGolden() throws Exception {
    PSHierarchyNodeProperty original = sampleProperty();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "hierarchy-node-property"), xml);
    assertTrue(containsTag(xml, "node-id"), xml);
    assertTrue(containsTag(xml, "name"), xml);
    assertTrue(containsTag(xml, "value"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertFalse(containsTag(xml, "parent-id"), "historical betwixt parent-id not used: " + xml);
    assertFalse(containsTag(xml, "parent-guid"), "historical betwixt parentGuid not used: " + xml);
    assertTrue(xml.contains("displayOrder"), xml);

    String golden =
        loadResource("com/percussion/services/ui/data/ps-hierarchy-node-property-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void hierarchyNodePropertyRoundTrip() throws Exception {
    PSHierarchyNodeProperty original = sampleProperty();
    String xml = original.toXML();

    PSHierarchyNodeProperty restored = new PSHierarchyNodeProperty();
    restored.fromXML(xml);

    assertEquals(original.getNodeId(), restored.getNodeId());
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getValue(), restored.getValue());
  }

  @Test
  void hierarchyNodePropertyFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <name>legacyKey</name>
          <node-id>99</node-id>
          <value>legacyVal</value>
        </null>
        """;

    PSHierarchyNodeProperty restored = new PSHierarchyNodeProperty();
    restored.fromXML(legacy);

    assertEquals(99L, restored.getNodeId());
    assertEquals("legacyKey", restored.getName());
    assertEquals("legacyVal", restored.getValue());
  }

  @Test
  void hierarchyNodeSetTypeIsOneShotStrict() {
    PSDesignGuid guid = new PSDesignGuid(new PSGuid(PSTypeEnum.HIERARCHY_NODE, 1L));
    PSHierarchyNode node =
        new PSHierarchyNode("n", guid, PSHierarchyNode.NodeType.FOLDER);
    assertThrows(
        IllegalStateException.class, () -> node.setType(PSHierarchyNode.NodeType.FOLDER));
    assertThrows(
        IllegalStateException.class, () -> node.setType(PSHierarchyNode.NodeType.PLACEHOLDER));
    assertThrows(IllegalArgumentException.class, () -> node.setType(null));
  }

  @Test
  void hierarchyNodeSetNodeTypeRejectsNullButAllowsOverwrite() {
    PSHierarchyNode node = new PSHierarchyNode();
    assertThrows(IllegalArgumentException.class, () -> node.setNodeType(null));
    node.setNodeType(PSHierarchyNode.NodeType.FOLDER);
    assertEquals(PSHierarchyNode.NodeType.FOLDER, node.getNodeType());
    // Jackson restore path may re-assign on a fresh instance; not one-shot.
    node.setNodeType(PSHierarchyNode.NodeType.PLACEHOLDER);
    assertEquals(PSHierarchyNode.NodeType.PLACEHOLDER, node.getNodeType());
  }

  @Test
  void hierarchyNodeSetVersionOneShotWithNullClear() {
    PSHierarchyNode node = new PSHierarchyNode();
    node.setVersion(0);
    assertEquals(0, node.getVersion());
    assertThrows(IllegalStateException.class, () -> node.setVersion(1));
    // Null clears so design WS can re-init (peer: PSKeyword / PSTemplateSlot).
    node.setVersion(null);
    assertNull(node.getVersion());
    node.setVersion(2);
    assertEquals(2, node.getVersion());
    assertThrows(IllegalArgumentException.class, () -> {
      node.setVersion(null);
      node.setVersion(-1);
    });
  }

  @Test
  void hierarchyNodeGetPropertiesDoesNotReassignField() {
    PSHierarchyNode node = new PSHierarchyNode();
    Map<String, String> first = node.getProperties();
    assertTrue(first instanceof TreeMap);
    assertSame(first, node.getProperties());
    Map<String, String> incoming = new HashMap<>();
    incoming.put("b", "2");
    incoming.put("a", "1");
    node.setProperties(incoming);
    Map<String, String> after = node.getProperties();
    assertTrue(after instanceof TreeMap);
    assertEquals("1", after.get("a"));
    assertSame(after, node.getProperties());
  }

  @Test
  void hierarchyNodePropertySetVersionOneShotWithNullClear() {
    PSHierarchyNodeProperty prop =
        new PSHierarchyNodeProperty("k", "v", new PSGuid(PSTypeEnum.HIERARCHY_NODE, 1L));
    prop.setVersion(0);
    assertThrows(IllegalStateException.class, () -> prop.setVersion(1));
    prop.setVersion(null);
    assertNull(prop.getVersion());
    prop.setVersion(3);
    assertEquals(3, prop.getVersion());
  }

  private static PSHierarchyNode sampleNode() {
    // Use design guid so host/type/uuid form matches production workbench nodes.
    PSDesignGuid nodeGuid = new PSDesignGuid(new PSGuid(PSTypeEnum.HIERARCHY_NODE, 200L));
    PSDesignGuid parentGuid = new PSDesignGuid(new PSGuid(PSTypeEnum.HIERARCHY_NODE, 100L));

    PSHierarchyNode node = new PSHierarchyNode("Sites", nodeGuid, PSHierarchyNode.NodeType.FOLDER);
    node.setParentId(parentGuid);
    node.addProperty("displayOrder", "10");
    node.addProperty("icon", "sites");
    return node;
  }

  private static PSHierarchyNodeProperty sampleProperty() {
    PSGuid parent = new PSGuid(PSTypeEnum.HIERARCHY_NODE, 200L);
    return new PSHierarchyNodeProperty("displayOrder", "10", parent);
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSHierarchyNodeXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
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
