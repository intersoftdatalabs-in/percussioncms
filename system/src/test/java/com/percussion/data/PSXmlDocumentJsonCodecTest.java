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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Golden encode / decode / round-trip tests for classic app XML ↔ JSON mapping. */
class PSXmlDocumentJsonCodecTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  static Stream<String> fixtures() {
    return Stream.of("flat", "attrs", "repeated", "empty-and-text");
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void encode_matchesGolden(String name) throws Exception {
    Document doc = loadXml(name);
    String actual = PSXmlDocumentJsonCodec.toJson(doc);
    JsonNode expected = JSON.readTree(loadResourceText(name + ".json"));
    JsonNode actualNode = JSON.readTree(actual);
    assertEquals(expected, actualNode, "encode for " + name);
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void decode_matchesStructure(String name) throws Exception {
    String json = loadResourceText(name + ".json");
    Document fromJson = PSXmlDocumentJsonCodec.fromJson(json);
    Document fromXml = loadXml(name);
    assertDocumentsSemanticallyEqual(fromXml, fromJson, name);
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void roundTrip_documentToJsonToDocument(String name) throws Exception {
    Document original = loadXml(name);
    String json = PSXmlDocumentJsonCodec.toJson(original);
    Document roundTrip = PSXmlDocumentJsonCodec.fromJson(json);
    assertDocumentsSemanticallyEqual(original, roundTrip, name);
  }

  @Test
  void emptyJson_throws() {
    assertThrows(PSConversionException.class, () -> PSXmlDocumentJsonCodec.fromJson(""));
    assertThrows(PSConversionException.class, () -> PSXmlDocumentJsonCodec.fromJson("   "));
  }

  @Test
  void invalidJson_throws() {
    assertThrows(PSConversionException.class, () -> PSXmlDocumentJsonCodec.fromJson("{not json"));
  }

  @Test
  void multiRootJson_throws() {
    assertThrows(
        PSConversionException.class,
        () -> PSXmlDocumentJsonCodec.fromJson("{\"A\":1,\"B\":2}"));
  }

  @Test
  void invalidElementName_throws() {
    assertThrows(
        PSConversionException.class,
        () -> PSXmlDocumentJsonCodec.fromJson("{\"1bad\":{\"x\":\"y\"}}"));
    assertThrows(
        PSConversionException.class,
        () -> PSXmlDocumentJsonCodec.fromJson("{\"Root\":{\"@\": \"x\"}}"));
  }

  @Test
  void numberAndBooleanLeaves_stringifyOnDecode() throws Exception {
    Document doc =
        PSXmlDocumentJsonCodec.fromJson("{\"Root\":{\"n\":42,\"flag\":true,\"s\":\"x\"}}");
    Element root = doc.getDocumentElement();
    assertEquals("42", textOfChild(root, "n"));
    assertEquals("true", textOfChild(root, "flag"));
    assertEquals("x", textOfChild(root, "s"));
  }

  private static Document loadXml(String name) throws Exception {
    try (InputStream in = openResource(name + ".xml")) {
      assertNotNull(in, "missing fixture " + name + ".xml");
      return PSXmlDocumentBuilder.createXmlDocument(
          new InputStreamReader(in, StandardCharsets.UTF_8), false);
    }
  }

  private static String loadResourceText(String fileName) throws Exception {
    try (InputStream in = openResource(fileName)) {
      assertNotNull(in, "missing fixture " + fileName);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
    }
  }

  private static InputStream openResource(String fileName) {
    return PSXmlDocumentJsonCodecTest.class.getResourceAsStream(
        "/com/percussion/data/json-codec/" + fileName);
  }

  private static void assertDocumentsSemanticallyEqual(Document expected, Document actual, String label) {
    assertNotNull(expected.getDocumentElement(), label + " expected root");
    assertNotNull(actual.getDocumentElement(), label + " actual root");
    assertElementsEqual(expected.getDocumentElement(), actual.getDocumentElement(), label);
  }

  private static void assertElementsEqual(Element expected, Element actual, String path) {
    assertEquals(expected.getNodeName(), actual.getNodeName(), path + " name");

    // attributes
    assertEquals(
        expected.getAttributes().getLength(),
        actual.getAttributes().getLength(),
        path + " attr count");
    for (int i = 0; i < expected.getAttributes().getLength(); i++) {
      String name = expected.getAttributes().item(i).getNodeName();
      assertEquals(
          expected.getAttribute(name), actual.getAttribute(name), path + "/@" + name);
    }

    // child elements and text (ignore pure whitespace text)
    java.util.List<Node> expKids = significantChildren(expected);
    java.util.List<Node> actKids = significantChildren(actual);
    assertEquals(expKids.size(), actKids.size(), path + " child count");
    for (int i = 0; i < expKids.size(); i++) {
      Node e = expKids.get(i);
      Node a = actKids.get(i);
      assertEquals(e.getNodeType(), a.getNodeType(), path + " child type " + i);
      if (e.getNodeType() == Node.ELEMENT_NODE) {
        assertElementsEqual((Element) e, (Element) a, path + "/" + e.getNodeName());
      } else {
        assertEquals(e.getNodeValue(), a.getNodeValue(), path + " text");
      }
    }
  }

  private static java.util.List<Node> significantChildren(Element el) {
    java.util.List<Node> out = new java.util.ArrayList<>();
    NodeList kids = el.getChildNodes();
    for (int i = 0; i < kids.getLength(); i++) {
      Node n = kids.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        out.add(n);
      } else if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
        String t = n.getNodeValue();
        if (t != null && !t.trim().isEmpty()) {
          out.add(n);
        }
      }
    }
    return out;
  }

  private static String textOfChild(Element parent, String name) {
    NodeList list = parent.getElementsByTagName(name);
    assertTrue(list.getLength() >= 1, "missing " + name);
    Element e = (Element) list.item(0);
    return e.getTextContent();
  }
}
