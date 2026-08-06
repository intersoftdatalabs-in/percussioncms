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
package com.percussion.services.assembly.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / round-trip / package-fixture smoke for assembly design objects under the Jackson-backed
 * {@code PSXmlSerializationHelper} (issue #1891, epic #505).
 *
 * <p>Offline only — no live CMS. Package smoke loads shipped slot/template XML from {@code
 * perc-packages} (copied under test resources).
 */
class PSAssemblyXmlSerializationTest {

  @Test
  void slotWriteEmitsRootAndAssociationElementNames() throws Exception {
    PSTemplateSlot original = sampleNavImageSlot();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), "modern write must not emit legacy null root");
    assertTrue(containsTag(xml, "template-slot"), "root template-slot: " + xml);
    assertTrue(containsTag(xml, "slot-type-association"), "nested association: " + xml);
    assertTrue(containsTag(xml, "content-type-id"), "hyphenated content-type-id: " + xml);
    assertTrue(containsTag(xml, "template-id"), xml);
    assertTrue(containsTag(xml, "slot-id"), xml);
    assertFalse(containsTag(xml, "contenttypeid"), "must not emit unhyphenated write tags: " + xml);
    assertFalse(containsTag(xml, "slot-type-association-pk"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(xml.contains("0-5-513") || xml.contains(">513<"), xml);
    assertTrue(xml.contains("perc.nav.image"), xml);
  }

  @Test
  void slotWriteMatchesGoldenFixture() throws Exception {
    PSTemplateSlot original = sampleNavImageSlot();
    String xml = original.toXML();
    String golden =
        loadResource("com/percussion/services/assembly/data/ps-template-slot-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void slotRoundTripWriteReadRestoresAssociationsAndScalars() throws Exception {
    PSTemplateSlot original = sampleNavImageSlot();
    String xml = original.toXML();

    PSTemplateSlot restored = new PSTemplateSlot();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getLabel(), restored.getLabel());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getFinderName(), restored.getFinderName());
    assertEquals(original.getRelationshipName(), restored.getRelationshipName());
    assertEquals(original.getSlottype(), restored.getSlottype());
    assertEquals(original.isSystemSlot(), restored.isSystemSlot());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());

    PSTemplateTypeSlotAssociation[] expected = original.getSlotTypeAssociations();
    PSTemplateTypeSlotAssociation[] actual = restored.getSlotTypeAssociations();
    assertEquals(expected.length, actual.length);
    assertEquals(313L, actual[0].getContentTypeId());
    assertEquals(550L, actual[0].getTemplateId());
    assertEquals(513L, actual[0].getSlotId());
  }

  @Test
  void packageFixturePercNavImageSlotSmoke() throws Exception {
    String packaged = loadResource("com/percussion/services/assembly/data/perc.nav.image.slotDef");
    assertTrue(packaged.contains("<slotid>") || packaged.contains("<slot-id>"), packaged);

    PSTemplateSlot restored = new PSTemplateSlot();
    restored.fromXML(packaged);

    assertEquals("perc.nav.image", restored.getName());
    assertEquals("Nav Image", restored.getLabel());
    assertEquals(513L, restored.getGUID().longValue());
    PSTemplateTypeSlotAssociation[] assocs = restored.getSlotTypeAssociations();
    assertTrue(assocs != null && assocs.length >= 1, "expected association");
    boolean found = false;
    for (PSTemplateTypeSlotAssociation a : assocs) {
      if (a.getContentTypeId() == 313L && a.getTemplateId() == 550L) {
        found = true;
        break;
      }
    }
    assertTrue(found, "expected CT 313 / template 550");
  }

  @Test
  void templateWriteEmitsBindingsAndSlotIdsElementNames() throws Exception {
    PSAssemblyTemplate original = sampleBoxTemplate();
    String xml = original.toXML();

    assertNotNull(xml);
    assertTrue(containsTag(xml, "assembly-template"), "root: " + xml);
    assertTrue(containsTag(xml, "bindings"), xml);
    assertTrue(containsTag(xml, "binding"), "nested binding item: " + xml);
    assertTrue(containsTag(xml, "template-slot-ids"), xml);
    assertTrue(containsTag(xml, "template-slot-id"), xml);
    assertFalse(
        containsTag(xml, "template-binding"), "must not emit mapped type name for items: " + xml);
    assertFalse(containsTag(xml, "slots"), "must not emit full slot graph: " + xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertTrue(xml.contains("perc.base.Box"), xml);
    assertTrue(xml.contains("$sys.page"), xml);
  }

  @Test
  void templateWriteMatchesGoldenFixture() throws Exception {
    PSAssemblyTemplate original = sampleBoxTemplate();
    String xml = original.toXML();
    String golden =
        loadResource("com/percussion/services/assembly/data/ps-assembly-template-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void templateRoundTripWriteReadRestoresBindingsAndScalars() throws Exception {
    PSAssemblyTemplate original = sampleBoxTemplate();
    String xml = original.toXML();

    PSAssemblyTemplate restored = new PSAssemblyTemplate();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getLabel(), restored.getLabel());
    assertEquals(original.getAssembler(), restored.getAssembler());
    assertEquals(original.getAssemblyUrl(), restored.getAssemblyUrl());
    assertEquals(original.getCharset(), restored.getCharset());
    assertEquals(original.getMimeType(), restored.getMimeType());
    assertEquals(original.getActiveAssemblyType(), restored.getActiveAssemblyType());
    assertEquals(original.getOutputFormat(), restored.getOutputFormat());
    assertEquals(original.getPublishWhen(), restored.getPublishWhen());
    assertEquals(original.getTemplateType(), restored.getTemplateType());
    assertEquals(original.getGlobalTemplateUsage(), restored.getGlobalTemplateUsage());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(original.getTemplate(), restored.getTemplate());

    assertEquals(original.getBindings().size(), restored.getBindings().size());
    assertEquals("$sys.page", restored.getBindings().get(0).getVariable());
    assertEquals("1", restored.getBindings().get(0).getExpression());

    // Slot ids restored via offline placeholder path (no assembly service)
    assertEquals(1, restored.getTemplateSlotIds().size());
    assertEquals(511L, restored.getTemplateSlotIds().get(0));
  }

  @Test
  void packageFixturePercBaseBoxTemplateSmoke() throws Exception {
    String packaged =
        loadResource("com/percussion/services/assembly/data/perc.base.Box.templateDef");
    assertTrue(packaged.contains("<assembly-template"), packaged);

    PSAssemblyTemplate restored = new PSAssemblyTemplate();
    restored.fromXML(packaged);

    assertEquals("perc.base.Box", restored.getName());
    assertEquals("Box", restored.getLabel());
    assertEquals(557L, restored.getGUID().longValue());
    assertEquals(IPSAssemblyTemplate.AAType.Normal, restored.getActiveAssemblyType());
    assertEquals(IPSAssemblyTemplate.OutputFormat.Page, restored.getOutputFormat());
    assertEquals(IPSAssemblyTemplate.PublishWhen.Default, restored.getPublishWhen());
    assertEquals(IPSAssemblyTemplate.TemplateType.Shared, restored.getTemplateType());
    assertEquals(IPSAssemblyTemplate.GlobalTemplateUsage.None, restored.getGlobalTemplateUsage());
    assertNotNull(restored.getBindings());
    assertTrue(restored.getBindings().isEmpty());
    assertTrue(restored.getTemplateSlotIds().isEmpty());
    assertTrue(restored.getTemplate() != null && restored.getTemplate().contains("perc-container"));
  }

  @Test
  void bindingStandaloneRoundTrip() throws Exception {
    PSTemplateBinding original = new PSTemplateBinding(2, "$sys.page", "1");
    original.setId(42L);
    String xml = com.percussion.services.utils.xml.PSXmlSerializationHelper.writeToXml(original);
    // Standalone root uses mapped type name template-binding
    assertTrue(containsTag(xml, "template-binding"), "standalone root: " + xml);
    assertFalse(containsTag(xml, "jexl-script"), "jexl-script suppressed: " + xml);
    assertFalse(containsTag(xml, "version"), xml);

    PSTemplateBinding restored = new PSTemplateBinding();
    com.percussion.services.utils.xml.PSXmlSerializationHelper.readFromXML(xml, restored);
    assertEquals("$sys.page", restored.getVariable());
    assertEquals("1", restored.getExpression());
    assertEquals(2, restored.getExecutionOrder().intValue());
    assertEquals(42L, restored.getId().longValue());
  }

  /** Package archives may emit execution order 0; round-trip must keep it. */
  @Test
  void bindingExecutionOrderZeroRoundTrip() throws Exception {
    PSTemplateBinding original = new PSTemplateBinding(0, "$sys.item", "/*");
    original.setId(7L);
    String xml = com.percussion.services.utils.xml.PSXmlSerializationHelper.writeToXml(original);
    PSTemplateBinding restored = new PSTemplateBinding();
    com.percussion.services.utils.xml.PSXmlSerializationHelper.readFromXML(xml, restored);
    assertEquals(0, restored.getExecutionOrder().intValue());
    assertTrue(restored.getExecutionOrderOptional().isPresent());
    assertEquals(0, restored.getExecutionOrderOptional().get().intValue());
  }

  /** Jackson/MSM restore may pass null to clear associations (same as setFinderArguments). */
  @Test
  void slotTypeAssociationsNullClears() {
    PSTemplateSlot slot = sampleNavImageSlot();
    assertTrue(slot.getSlotTypeAssociations().length > 0);
    slot.setSlotTypeAssociations(null);
    assertEquals(0, slot.getSlotTypeAssociations().length);
  }

  @Test
  void associationStandaloneRoundTrip() throws Exception {
    PSTemplateTypeSlotAssociation original =
        new PSTemplateTypeSlotAssociation(
            new PSGuid(PSTypeEnum.NODEDEF, 313L), new PSGuid(PSTypeEnum.TEMPLATE, 550L), 513L);
    original.setVersion(0);
    String xml = com.percussion.services.utils.xml.PSXmlSerializationHelper.writeToXml(original);
    assertTrue(containsTag(xml, "content-type-id"), xml);
    assertTrue(containsTag(xml, "template-id"), xml);
    assertTrue(containsTag(xml, "slot-id"), xml);

    PSTemplateTypeSlotAssociation restored = new PSTemplateTypeSlotAssociation();
    com.percussion.services.utils.xml.PSXmlSerializationHelper.readFromXML(xml, restored);
    assertEquals(313L, restored.getContentTypeId());
    assertEquals(550L, restored.getTemplateId());
    assertEquals(513L, restored.getSlotId());
  }

  private static PSTemplateSlot sampleNavImageSlot() {
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.setGUID(new PSGuid(PSTypeEnum.SLOT, 513L));
    slot.setName("perc.nav.image");
    slot.setLabel("Nav Image");
    slot.setDescription("navigation image for rollovers");
    slot.setFinderName("Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder");
    slot.setRelationshipName("ActiveAssembly");
    slot.setSlottype(0);
    slot.setSystemSlot(false);
    slot.setVersion(2);
    PSTemplateTypeSlotAssociation assoc =
        new PSTemplateTypeSlotAssociation(
            new PSGuid(PSTypeEnum.NODEDEF, 313L), new PSGuid(PSTypeEnum.TEMPLATE, 550L), 513L);
    assoc.setVersion(0);
    slot.setSlotTypeAssociations(new PSTemplateTypeSlotAssociation[] {assoc});
    return slot;
  }

  private static PSAssemblyTemplate sampleBoxTemplate() {
    PSAssemblyTemplate t = new PSAssemblyTemplate();
    t.setGUID(new PSGuid(PSTypeEnum.TEMPLATE, 557L));
    t.setName("perc.base.Box");
    t.setLabel("Box");
    t.setAssembler("Java/global/percussion/assembly/pageAssembler");
    t.setAssemblyUrl("../assembler/render");
    t.setCharset("UTF-8");
    t.setDescription("");
    t.setMimeType("text/html");
    t.setActiveAssemblyType(IPSAssemblyTemplate.AAType.Normal);
    t.setOutputFormat(IPSAssemblyTemplate.OutputFormat.Page);
    t.setPublishWhen(IPSAssemblyTemplate.PublishWhen.Default);
    t.setTemplateType(IPSAssemblyTemplate.TemplateType.Shared);
    t.setGlobalTemplateUsage(IPSAssemblyTemplate.GlobalTemplateUsage.None);
    t.setTemplate("#perc_templateHeader()##");
    t.setLocationPrefix("");
    t.setLocationSuffix("");
    t.setStyleSheetPath("");

    PSTemplateBinding binding = new PSTemplateBinding(1, "$sys.page", "1");
    binding.setId(99L);
    t.setBindings(List.of(binding));

    // In-memory slot so write emits template-slot-id without assembly service
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.setGUID(new PSGuid(PSTypeEnum.SLOT, 511L));
    slot.setName("perc.nav.slot");
    t.addSlot(slot);
    return t;
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSAssemblyXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Compare logical XML trees: ignore XML declaration, Betwixt graph-identity {@code id}
   * attributes, insignificant whitespace, and HTML comments. Child order is significant. All other
   * attributes are compared (name set + values) so non-identity attribute regressions still fail.
   */
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

  /**
   * Structural element compare used by golden/package parity. Tag names, text, non-{@code id}
   * attributes, and child order are significant. The {@code id} attribute is ignored because legacy
   * Betwixt graph-identity attributes are not part of the product contract under Jackson.
   */
  private static void assertElementTreeEquals(Element expected, Element actual, String path) {
    assertEquals(expected.getTagName(), actual.getTagName(), "tag at " + path);
    assertAttributesEqualIgnoringGraphId(expected, actual, path);
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

  /** Compare attributes except Betwixt graph-identity {@code id}. */
  private static void assertAttributesEqualIgnoringGraphId(
      Element expected, Element actual, String path) {
    Map<String, String> eAttrs = attributesWithoutId(expected);
    Map<String, String> aAttrs = attributesWithoutId(actual);
    assertEquals(eAttrs.keySet(), aAttrs.keySet(), "attribute names at " + path);
    for (Map.Entry<String, String> e : eAttrs.entrySet()) {
      assertEquals(e.getValue(), aAttrs.get(e.getKey()), "attr @" + e.getKey() + " at " + path);
    }
  }

  private static Map<String, String> attributesWithoutId(Element el) {
    Map<String, String> out = new java.util.LinkedHashMap<>();
    var attrs = el.getAttributes();
    if (attrs == null) {
      return out;
    }
    for (int i = 0; i < attrs.getLength(); i++) {
      Node a = attrs.item(i);
      String name = a.getNodeName();
      if ("id".equals(name)) {
        continue;
      }
      out.put(name, a.getNodeValue() == null ? "" : a.getNodeValue());
    }
    return out;
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
