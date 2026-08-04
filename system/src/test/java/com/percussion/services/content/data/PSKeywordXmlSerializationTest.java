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
package com.percussion.services.content.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Golden / round-trip / package-fixture smoke for {@link PSKeyword} and {@link PSKeywordChoice}
 * under the Jackson-backed {@code PSXmlSerializationHelper} (issue #1888, epic #505).
 *
 * <p>Offline only — no live CMS. Package smoke loads shipped {@code .keyword} XML from {@code
 * perc-packages} (copied under test resources).
 */
class PSKeywordXmlSerializationTest {

  @Test
  void writeEmitsChoiceNotKeywordChoiceAndSuppressesVersion() throws Exception {
    PSKeyword original = sampleAdhocKeyword();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), "modern write must not emit legacy null root");
    assertTrue(containsTag(xml, "keyword"), "root keyword: " + xml);
    assertTrue(containsTag(xml, "choice"), "nested package element choice: " + xml);
    assertFalse(
        containsTag(xml, "keyword-choice"),
        "must not emit mapped type name keyword-choice: " + xml);
    assertFalse(xml.contains("secret-must-not"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertTrue(containsTag(xml, "guid"), "guid string form on Jackson path: " + xml);
    assertTrue(xml.contains("0-14-125") || xml.contains(">125<"), xml);
    assertFalse(containsTag(xml, "guid-optional"), xml);
    assertFalse(containsTag(xml, "description-optional"), xml);
    assertFalse(containsTag(xml, "display-string"), xml);
    assertFalse(containsTag(xml, "name-optional"), xml);
    assertFalse(containsTag(xml, "type-enum"), xml);
    assertFalse(containsTag(xml, "type-ordinal"), xml);
    assertFalse(containsTag(xml, "identifier-string"), xml);
    // bare <name>/<type> catalog aliases
    assertFalse(xml.matches("(?s).*<name(\\s|>).*"), "name alias suppressed: " + xml);
    assertFalse(xml.matches("(?s).*<type(\\s|>).*"), "catalog type suppressed: " + xml);
    assertTrue(xml.contains("Adhoc_Type"), xml);
    assertTrue(xml.contains("Anonymous"), xml);
  }

  @Test
  void writeMatchesGoldenFixture() throws Exception {
    PSKeyword original = sampleAdhocKeyword();
    String xml = original.toXML();
    String golden = loadResource("com/percussion/services/content/data/ps-keyword-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void roundTripWriteReadRestoresChoicesAndScalars() throws Exception {
    PSKeyword original = sampleAdhocKeyword();
    String xml = original.toXML();

    PSKeyword restored = new PSKeyword();
    restored.fromXML(xml);

    assertKeywordGraphEquals(original, restored);
  }

  @Test
  void fromXmlAcceptsLegacyNullRootPackageShape() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>  <null id="1">
            <choices>
              <choice id="2">
                <description>Ad Hoc Assignment Anonymous Access</description>
                <label>Anonymous</label>
                <sequence>3</sequence>
                <value>2</value>
              </choice>
              <choice id="3">
                <description>Ad Hoc Assignment Enabled</description>
                <label>Enabled</label>
                <sequence>2</sequence>
                <value>1</value>
              </choice>
            </choices>
            <guid>0-14-125</guid>
            <description>Ad Hoc Assignment Lookups</description>
            <id>125</id>
            <keyword-type>1</keyword-type>
            <label>Adhoc_Type</label>
            <name>Adhoc_Type</name>
            <sequence>1</sequence>
            <type>KEYWORD_DEF</type>
            <value>125</value>
          </null>
        """;

    PSKeyword restored = new PSKeyword();
    restored.fromXML(legacy);

    assertEquals(125L, restored.getId());
    assertEquals("Adhoc_Type", restored.getLabel());
    assertEquals("125", restored.getValue());
    assertEquals("1", restored.getKeywordType());
    assertEquals(1, restored.getSequence().intValue());
    assertEquals("Ad Hoc Assignment Lookups", restored.getDescription());
    assertNotNull(restored.getChoices());
    assertEquals(2, restored.getChoices().size());
    assertEquals("Anonymous", restored.getChoices().get(0).getLabel());
    assertEquals("2", restored.getChoices().get(0).getValue());
    assertEquals(3, restored.getChoices().get(0).getSequence().intValue());
    assertEquals("Enabled", restored.getChoices().get(1).getLabel());
  }

  @Test
  void packageFixtureAdhocTypeKeywordSmoke() throws Exception {
    // Offline package smoke: shipped perc.widget.blogIndexPage Adhoc_Type.keyword shape
    String packaged = loadResource("com/percussion/services/content/data/Adhoc_Type.keyword");
    assertTrue(packaged.contains("<null"), "fixture should keep legacy null root");
    assertTrue(packaged.contains("<choice"), packaged);

    PSKeyword restored = new PSKeyword();
    restored.fromXML(packaged);

    assertEquals(125L, restored.getId());
    assertEquals("Adhoc_Type", restored.getLabel());
    assertEquals("125", restored.getValue());
    assertEquals("1", restored.getKeywordType());
    assertEquals(3, restored.getChoices().size());

    PSKeywordChoice disabled =
        restored.getChoices().stream()
            .filter(c -> "Disabled".equals(c.getLabel()))
            .findFirst()
            .orElseThrow();
    assertEquals("0", disabled.getValue());
    assertEquals(1, disabled.getSequence().intValue());
    assertEquals("Ad Hoc Assignment Disabled", disabled.getDescription());
  }

  @Test
  void choiceStandaloneRoundTrip() throws Exception {
    PSKeywordChoice original =
        PSKeywordChoice.of("2", "Anonymous", "Ad Hoc Assignment Anonymous Access", 3);
    String xml = original.toXML();
    assertTrue(containsTag(xml, "keyword-choice"), "standalone root uses mapped type name: " + xml);

    PSKeywordChoice restored = new PSKeywordChoice();
    restored.fromXML(xml);
    assertEquals(original, restored);
  }

  private static PSKeyword sampleAdhocKeyword() {
    PSKeyword keyword = new PSKeyword("Adhoc_Type", "Ad Hoc Assignment Lookups", "125");
    keyword.setId(125L);
    keyword.setSequence(1);
    keyword.addChoice(
        PSKeywordChoice.of("2", "Anonymous", "Ad Hoc Assignment Anonymous Access", 3));
    keyword.addChoice(PSKeywordChoice.of("1", "Enabled", "Ad Hoc Assignment Enabled", 2));
    keyword.addChoice(PSKeywordChoice.of("0", "Disabled", "Ad Hoc Assignment Disabled", 1));
    return keyword;
  }

  private static void assertKeywordGraphEquals(PSKeyword expected, PSKeyword actual) {
    assertNotNull(actual);
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getLabel(), actual.getLabel());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expected.getKeywordType(), actual.getKeywordType());
    assertEquals(expected.getDescription(), actual.getDescription());
    assertEquals(expected.getSequence(), actual.getSequence());
    List<PSKeywordChoice> eChoices = expected.getChoices();
    List<PSKeywordChoice> aChoices = actual.getChoices();
    assertEquals(eChoices.size(), aChoices.size());
    for (int i = 0; i < eChoices.size(); i++) {
      assertEquals(eChoices.get(i), aChoices.get(i), "choice index " + i);
    }
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSKeywordXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Compare logical XML trees: ignore XML declaration, Betwixt graph-identity {@code id}
   * attributes, insignificant whitespace, and HTML comments.
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
    // Drop leading HTML/XML comments before root
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
    // Ignore graph-identity id attributes (Betwixt); compare child elements + text only
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
