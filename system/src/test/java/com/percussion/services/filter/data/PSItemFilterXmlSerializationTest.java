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
package com.percussion.services.filter.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
 * Golden / round-trip / package-fixture smoke for filter design objects under the Jackson-backed
 * {@code PSXmlSerializationHelper} (issue #1915 / #1892, epic #505). Offline only — no live CMS.
 */
class PSItemFilterXmlSerializationTest {

  @Test
  void writeEmitsItemFilterRuleDefParamsAndSuppressesVersionParentFilter() throws Exception {
    PSItemFilter original = samplePublicFilter();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), "modern write must not emit legacy null root");
    assertTrue(containsTag(xml, "item-filter"), "root item-filter: " + xml);
    assertTrue(containsTag(xml, "rule-defs"), xml);
    assertTrue(containsTag(xml, "rule-def"), "nested package element rule-def: " + xml);
    assertFalse(
        containsTag(xml, "item-filter-rule-def"),
        "must not emit mapped type name item-filter-rule-def as nested: " + xml);
    assertTrue(containsTag(xml, "params"), xml);
    assertTrue(containsTag(xml, "rule-name"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(containsTag(xml, "legacy-authtype-id"), xml);
    assertTrue(xml.contains("perc_public"), xml);
    assertTrue(xml.contains("perc_publicAssetFilter") || xml.contains("TESTRULE"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    // parentFilter object suppressed; parent-filter-id may be empty/null omitted
    assertFalse(xml.matches("(?s).*<parent-filter(\\s|>).*"), "parentFilter object: " + xml);
    // circular filter on rule-def omitted on modern write (package used idref)
    assertFalse(xml.matches("(?s).*<filter(\\s|>).*"), "circular filter on rule-def: " + xml);
  }

  @Test
  void writeMatchesGoldenFixture() throws Exception {
    String xml = samplePublicFilter().toXML();
    String golden = loadResource("com/percussion/services/filter/data/ps-item-filter-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void roundTripRestoresScalarsRulesAndParams() throws Exception {
    PSItemFilter original = samplePublicFilter();
    String xml = original.toXML();

    PSItemFilter restored = new PSItemFilter();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getLegacyAuthtypeId(), restored.getLegacyAuthtypeId());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertNull(restored.getParentFilter());
    assertEquals(original.getRuleDefs().size(), restored.getRuleDefs().size());

    Map<String, String> originalParamsByRule = ruleParamsByName(original);
    Map<String, String> restoredParamsByRule = ruleParamsByName(restored);
    assertEquals(originalParamsByRule.keySet(), restoredParamsByRule.keySet());
    for (String ruleName : originalParamsByRule.keySet()) {
      assertEquals(
          originalParamsByRule.get(ruleName),
          restoredParamsByRule.get(ruleName),
          "params for " + ruleName);
    }
  }

  @Test
  void fromXmlAcceptsLegacyNullRootPackageShape() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null id="1">
          <guid>0-7-11</guid>
          <description>Public Asset Filter.</description>
          <label>perc_public</label>
          <legacy-authtype-id>1</legacy-authtype-id>
          <name>perc_public</name>
          <parent-filter-id/>
          <rule-defs>
            <rule-def id="2">
              <params>
                <sys_context>1</sys_context>
              </params>
              <rule-name>***TESTRULE***</rule-name>
            </rule-def>
          </rule-defs>
        </null>
        """;

    PSItemFilter restored = new PSItemFilter();
    restored.fromXML(legacy);

    assertEquals("perc_public", restored.getName());
    assertEquals("Public Asset Filter.", restored.getDescription());
    assertEquals(1, restored.getLegacyAuthtypeId().intValue());
    assertEquals(11L, restored.getGUID().longValue());
    assertEquals(1, restored.getRuleDefs().size());
    IPSItemFilterRuleDef rule = restored.getRuleDefs().iterator().next();
    assertEquals(PSItemFilterRuleDef.TEST_RULE_NAME, rule.getRuleName());
    assertEquals("1", rule.getParam("sys_context"));
  }

  @Test
  void packageFixturePublicFilterSmokeRestoresScalarsAndRules() throws Exception {
    String packaged = loadResource("com/percussion/services/filter/data/perc_public.filterDef");
    assertTrue(packaged.contains("<item-filter"), packaged);
    assertTrue(packaged.contains("<rule-def"), packaged);
    assertTrue(packaged.contains("idref"), "fixture still uses Betwixt filter idref");

    PSItemFilter restored = new PSItemFilter();
    restored.fromXML(packaged);

    assertEquals("perc_public", restored.getName());
    assertEquals("Public Asset Filter.", restored.getDescription());
    assertEquals(1, restored.getLegacyAuthtypeId().intValue());
    assertNotNull(restored.getGUID());
    // Package guid is host-type-uuid form (750232-7-11); longValue is the UUID bits, not 11.
    assertEquals("750232-7-11", restored.getGUID().toString());
    assertEquals(2, restored.getRuleDefs().size());

    Map<String, String> byName = ruleNamesToEmpty(restored);
    assertTrue(byName.containsKey("Java/global/percussion/itemfilter/perc_publicAssetFilter"));
    assertTrue(byName.containsKey("Java/global/percussion/itemfilter/perc_unscheduledFilter"));
  }

  @Test
  void packageFixtureStagingFilterSmoke() throws Exception {
    String packaged = loadResource("com/percussion/services/filter/data/perc_staging.filterDef");
    PSItemFilter restored = new PSItemFilter();
    restored.fromXML(packaged);

    assertEquals("perc_staging", restored.getName());
    assertEquals(1, restored.getRuleDefs().size());
    assertEquals(
        "Java/global/percussion/itemfilter/perc_stagingFilter",
        restored.getRuleDefs().iterator().next().getRuleName());
  }

  private static PSItemFilter samplePublicFilter() throws Exception {
    PSItemFilter filter = new PSItemFilter();
    filter.setGUID(new PSGuid(PSTypeEnum.ITEM_FILTER, 11L));
    filter.setName("perc_public");
    filter.setDescription("Public Asset Filter.");
    filter.setLegacyAuthtypeId(1);

    PSItemFilterRuleDef publicRule = new PSItemFilterRuleDef(true);
    publicRule.setRule(PSItemFilterRuleDef.TEST_RULE_NAME);
    publicRule.setParam("sys_context", "1");
    filter.addRuleDef(publicRule);

    PSItemFilterRuleDef unscheduled = new PSItemFilterRuleDef(true);
    unscheduled.setRule("***TESTRULE-UNSCHEDULED***");
    filter.addRuleDef(unscheduled);

    return filter;
  }

  private static Map<String, String> ruleParamsByName(PSItemFilter filter) throws Exception {
    Map<String, String> out = new LinkedHashMap<>();
    for (IPSItemFilterRuleDef def : filter.getRuleDefs()) {
      StringBuilder flat = new StringBuilder();
      def.getParams().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> flat.append(e.getKey()).append('=').append(e.getValue()).append(';'));
      out.put(def.getRuleName(), flat.toString());
    }
    return out;
  }

  private static Map<String, String> ruleNamesToEmpty(PSItemFilter filter) throws Exception {
    Map<String, String> out = new LinkedHashMap<>();
    for (IPSItemFilterRuleDef def : filter.getRuleDefs()) {
      out.put(def.getRuleName(), "");
    }
    return out;
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSItemFilterXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
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
