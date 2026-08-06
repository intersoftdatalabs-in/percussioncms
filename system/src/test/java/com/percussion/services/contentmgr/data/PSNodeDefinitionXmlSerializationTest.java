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
package com.percussion.services.contentmgr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / scalar round-trip tests for {@link PSNodeDefinition} under the Jackson-backed {@code
 * PSXmlSerializationHelper} (issue #1921, epic #505). Offline only — no live CMS.
 *
 * <p>Template association restore that calls content-manager locators remains covered by live
 * integration tests ({@code PSContentTypeMgrTest}); this suite pins write shape and scalar restore.
 */
class PSNodeDefinitionXmlSerializationTest {

  @Test
  void writeEmitsPackageShapeAndSuppressesHibernateGraph() throws Exception {
    PSNodeDefinition original = sampleNodeDefinitionWithTemplates();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "node-definition"), xml);
    assertTrue(containsTag(xml, "internal-name"), xml);
    assertTrue(containsTag(xml, "raw-content-type"), xml);
    assertTrue(containsTag(xml, "template-ids"), xml);
    assertTrue(containsTag(xml, "template-id"), "package item name template-id: " + xml);
    assertTrue(containsTag(xml, "workflow-ids"), xml);
    assertTrue(containsTag(xml, "string"), "workflow item name string: " + xml);
    assertFalse(containsTag(xml, "version"), xml);
    assertFalse(containsTag(xml, "cv-descriptors"), xml);
    assertFalse(containsTag(xml, "ct-wf-rels"), xml);
    assertFalse(containsTag(xml, "variant-guids"), xml);
    assertFalse(containsTag(xml, "workflow-guids"), xml);
    assertFalse(containsTag(xml, "required-primary-type-names"), xml);
    assertFalse(containsTag(xml, "default-primary-type-name"), xml);
    assertFalse(containsTag(xml, "guid"), "GUID uses id/raw-content-type on wire: " + xml);
    assertTrue(xml.contains("rffGeneric") || xml.contains("rx:rffGeneric"), xml);
  }

  @Test
  void writeMatchesGoldenFixture() throws Exception {
    String xml = sampleNodeDefinitionWithTemplates().toXML();
    String golden =
        loadResource("com/percussion/services/contentmgr/data/ps-node-definition-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void scalarRoundTripWithoutTemplateRestore() throws Exception {
    PSNodeDefinition original = sampleNodeDefinitionScalarsOnly();
    String xml = original.toXML();

    PSNodeDefinition restored = new PSNodeDefinition();
    restored.fromXML(xml);

    assertEquals(original.getId(), restored.getId());
    assertEquals(original.getInternalName(), restored.getInternalName());
    assertEquals(original.getLabel(), restored.getLabel());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getHideFromMenu(), restored.getHideFromMenu());
    assertEquals(original.getObjectType(), restored.getObjectType());
    assertEquals(original.getNewRequest(), restored.getNewRequest());
    assertEquals(original.getQueryRequest(), restored.getQueryRequest());
    assertEquals(original.getUpdateRequest(), restored.getUpdateRequest());
    assertEquals(original.getRawContentType(), restored.getRawContentType());
    assertTrue(restored.getTemplateIds().isEmpty());
    assertTrue(restored.getWorkflowIds().isEmpty());
  }

  @Test
  void fromXmlAcceptsPackageShapeScalars() throws Exception {
    String packaged =
        """
        <node-definition>
          <auto-created>false</auto-created>
          <description>vTest Node</description>
          <hide-from-menu>false</hide-from-menu>
          <id>911</id>
          <internal-name>rffGeneric</internal-name>
          <label>Generic</label>
          <mandatory>false</mandatory>
          <name>rx:rffGeneric</name>
          <new-request>../psx_cerffGeneric/rffGeneric.html</new-request>
          <object-type>1</object-type>
          <protected>false</protected>
          <query-request>../psx_cerffGeneric/rffGeneric.html</query-request>
          <raw-content-type>911</raw-content-type>
          <update-request/>
        </node-definition>
        """;

    PSNodeDefinition restored = new PSNodeDefinition();
    restored.fromXML(packaged);

    assertEquals(911L, restored.getId());
    assertEquals("rffGeneric", restored.getInternalName());
    assertEquals("Generic", restored.getLabel());
    assertEquals("vTest Node", restored.getDescription());
    assertEquals(Integer.valueOf(1), restored.getObjectType());
    assertEquals(Boolean.FALSE, restored.getHideFromMenu());
    assertEquals("../psx_cerffGeneric/rffGeneric.html", restored.getNewRequest());
  }

  @Test
  void unsetContentTypeIdFailsFastOnGetters() {
    PSNodeDefinition unset = new PSNodeDefinition();
    assertThrows(NullPointerException.class, unset::getId);
    assertThrows(NullPointerException.class, unset::getRawContentType);
  }

  @Test
  void setWorkflowIdsRestoresAssociationsOffline() {
    PSNodeDefinition def = sampleNodeDefinitionScalarsOnly();
    Set<String> ids = new HashSet<>();
    ids.add("0-23-6");
    ids.add("0-23-7");
    def.setWorkflowIds(ids);

    Set<String> restored = def.getWorkflowIds();
    assertEquals(2, restored.size());
    assertTrue(restored.contains("0-23-6"), restored.toString());
    assertTrue(restored.contains("0-23-7"), restored.toString());
    assertEquals(2, def.getWorkflowGuids().size());
    // Association PKs must be non-null (no @GeneratedValue on PSContentTypeWorkflow).
    assertEquals(2, def.getCtWfRels().size());
    for (PSContentTypeWorkflow rel : def.getCtWfRels()) {
      assertNotNull(rel.getId(), "PSContentTypeWorkflow.id must be assigned offline and online");
    }
  }

  @Test
  void setWorkflowIdsReplacesExistingAssociations() {
    PSNodeDefinition def = sampleNodeDefinitionScalarsOnly();
    def.setWorkflowIds(Set.of("0-23-6", "0-23-7"));
    assertEquals(2, def.getWorkflowIds().size());

    def.setWorkflowIds(Set.of("0-23-8"));
    Set<String> restored = def.getWorkflowIds();
    assertEquals(1, restored.size());
    assertTrue(restored.contains("0-23-8"), restored.toString());
    assertNotNull(def.getCtWfRels().iterator().next().getId());
  }

  @Test
  void fromXmlRestoresWorkflowIds() throws Exception {
    String packaged =
        """
        <node-definition>
          <auto-created>false</auto-created>
          <description>vTest Node</description>
          <hide-from-menu>false</hide-from-menu>
          <id>911</id>
          <internal-name>rffGeneric</internal-name>
          <label>Generic</label>
          <mandatory>false</mandatory>
          <name>rx:rffGeneric</name>
          <new-request>../psx_cerffGeneric/rffGeneric.html</new-request>
          <object-type>1</object-type>
          <protected>false</protected>
          <query-request>../psx_cerffGeneric/rffGeneric.html</query-request>
          <raw-content-type>911</raw-content-type>
          <update-request/>
          <workflow-ids>
            <string>0-23-6</string>
          </workflow-ids>
        </node-definition>
        """;

    PSNodeDefinition restored = new PSNodeDefinition();
    restored.fromXML(packaged);

    assertEquals(911L, restored.getId());
    Set<String> workflowIds = restored.getWorkflowIds();
    assertEquals(1, workflowIds.size());
    assertTrue(workflowIds.contains("0-23-6"), workflowIds.toString());
    assertNotNull(restored.getCtWfRels().iterator().next().getId());
  }

  private static PSNodeDefinition sampleNodeDefinitionScalarsOnly() {
    PSNodeDefinition def = new PSNodeDefinition();
    def.setId(911L);
    def.setInternalName("rffGeneric");
    def.setLabel("Generic");
    def.setDescription("vTest Node");
    def.setHideFromMenu(Boolean.FALSE);
    def.setObjectType(1);
    def.setNewRequest("../psx_cerffGeneric/rffGeneric.html");
    def.setQueryRequest("../psx_cerffGeneric/rffGeneric.html");
    def.setUpdateRequest("");
    return def;
  }

  private static PSNodeDefinition sampleNodeDefinitionWithTemplates() {
    PSNodeDefinition def = sampleNodeDefinitionScalarsOnly();

    Set<PSContentTemplateDesc> descs = new HashSet<>();
    for (long tid : new long[] {504L, 502L}) {
      PSContentTemplateDesc desc = new PSContentTemplateDesc();
      desc.setId(tid * 10);
      desc.setContentTypeId(new PSGuid(PSTypeEnum.NODEDEF, 911L));
      desc.setTemplateId(new PSGuid(PSTypeEnum.TEMPLATE, tid));
      descs.add(desc);
    }
    def.setCvDescriptors(descs);

    Set<PSContentTypeWorkflow> wfs = new HashSet<>();
    PSContentTypeWorkflow rel = new PSContentTypeWorkflow();
    rel.setId(1L);
    rel.setContentTypeId(new PSGuid(PSTypeEnum.NODEDEF, 911L));
    rel.setWorkflowId(new PSGuid(PSTypeEnum.WORKFLOW, 6L));
    wfs.add(rel);
    def.setCtWfRels(wfs);
    return def;
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSNodeDefinitionXmlSerializationTest.class
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
