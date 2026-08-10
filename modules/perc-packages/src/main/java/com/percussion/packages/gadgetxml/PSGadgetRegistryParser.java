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

package com.percussion.packages.gadgetxml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Parses product {@code GadgetRegistry.xml} ({@code <gadgets><group name="…"><gadget …/>}) into
 * {@link PSGadgetRegistryModel}.
 *
 * <p>Uses a hardened {@link DocumentBuilderFactory} (no external DTDs / schemas / XInclude). Paths
 * are read via portable NIO {@link Files}.
 */
public final class PSGadgetRegistryParser {

  private PSGadgetRegistryParser() {
    // utility
  }

  /**
   * Parse registry XML from a UTF-8 file.
   *
   * @param path non-null path to {@code GadgetRegistry.xml}
   * @return parsed model
   * @throws PSGadgetRegistryException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSGadgetRegistryModel parse(Path path)
      throws PSGadgetRegistryException, IOException {
    Objects.requireNonNull(path, "path");
    String xml = Files.readString(path, StandardCharsets.UTF_8);
    PSGadgetRegistryModel model = parse(xml);
    Path fileName = path.getFileName();
    if (fileName != null) {
      model.setSourceFileName(fileName.toString());
    }
    return model;
  }

  /**
   * Parse registry XML from a string.
   *
   * @param xml non-null document text
   * @return parsed model
   * @throws PSGadgetRegistryException on parse failure
   */
  public static PSGadgetRegistryModel parse(String xml) throws PSGadgetRegistryException {
    Objects.requireNonNull(xml, "xml");
    if (xml.isBlank()) {
      throw new PSGadgetRegistryException("Gadget registry XML is empty");
    }
    try {
      Document doc = parseDocument(new InputSource(new StringReader(xml)));
      return fromDocument(doc);
    } catch (PSGadgetRegistryException e) {
      throw e;
    } catch (Exception e) {
      throw new PSGadgetRegistryException(
          "Failed to parse Gadget registry XML: " + e.getMessage(), e);
    }
  }

  static PSGadgetRegistryModel fromDocument(Document doc) throws PSGadgetRegistryException {
    Objects.requireNonNull(doc, "doc");
    Element root = doc.getDocumentElement();
    if (root == null) {
      throw new PSGadgetRegistryException("Gadget registry XML has no document element");
    }
    String rootName = root.getLocalName() != null ? root.getLocalName() : root.getTagName();
    if (!"gadgets".equalsIgnoreCase(rootName)) {
      throw new PSGadgetRegistryException(
          "Expected root element <gadgets>, found <" + rootName + ">");
    }

    PSGadgetRegistryModel model = new PSGadgetRegistryModel();
    NodeList children = root.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element el = (Element) n;
      String tag = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
      if (!"group".equalsIgnoreCase(tag)) {
        continue;
      }
      String groupName = el.getAttribute("name");
      if (groupName == null || groupName.isBlank()) {
        groupName = "Custom";
      }
      NodeList gadgetElems = el.getElementsByTagName("gadget");
      // Also accept no-namespace children listed directly under group.
      if (gadgetElems.getLength() == 0) {
        NodeList groupKids = el.getChildNodes();
        for (int j = 0; j < groupKids.getLength(); j++) {
          Node gn = groupKids.item(j);
          if (gn.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }
          Element ge = (Element) gn;
          String gtag = ge.getLocalName() != null ? ge.getLocalName() : ge.getTagName();
          if ("gadget".equalsIgnoreCase(gtag)) {
            model.addGadget(readGadget(ge, groupName));
          }
        }
      } else {
        for (int j = 0; j < gadgetElems.getLength(); j++) {
          Element ge = (Element) gadgetElems.item(j);
          model.addGadget(readGadget(ge, groupName));
        }
      }
    }

    if (model.getGadgets().isEmpty()) {
      throw new PSGadgetRegistryException("Gadget registry contains no <gadget> entries");
    }
    return model;
  }

  private static PSGadgetRegistryEntry readGadget(Element ge, String groupName)
      throws PSGadgetRegistryException {
    PSGadgetRegistryEntry entry = new PSGadgetRegistryEntry();
    entry.setGroup(groupName);
    String name = ge.getAttribute("name");
    if (name == null || name.isBlank()) {
      throw new PSGadgetRegistryException(
          "Gadget entry in group '" + groupName + "' is missing required name attribute");
    }
    entry.setName(name.trim());
    String baseUri = ge.getAttribute("baseuri");
    if (baseUri == null || baseUri.isBlank()) {
      // Some historic rows used baseUri camelCase — accept both.
      baseUri = ge.getAttribute("baseUri");
    }
    if (baseUri != null) {
      entry.setBaseUri(baseUri.trim());
    }
    String file = ge.getAttribute("file");
    if (file != null && !file.isBlank()) {
      entry.setLegacyDefinitionFile(file.trim());
    }
    return entry;
  }

  static Document parseDocument(InputSource source)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (ParserConfigurationException ignored) {
      // Some JDK / provider combinations reject features; continue with other hardening.
    }
    try {
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    } catch (ParserConfigurationException ignored) {
      // optional
    }
    try {
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    } catch (ParserConfigurationException ignored) {
      // optional
    }
    try {
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    } catch (IllegalArgumentException ignored) {
      // optional
    }
    return factory.newDocumentBuilder().parse(source);
  }
}
