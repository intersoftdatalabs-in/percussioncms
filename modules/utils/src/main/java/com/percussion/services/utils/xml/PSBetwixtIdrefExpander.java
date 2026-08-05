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
package com.percussion.services.utils.xml;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Pre-read expander for Apache Commons Betwixt graph-identity {@code idref} attributes.
 *
 * <p>Historical package dumps (ACL {@code *.aclDef}, etc.) share repeated graph nodes by writing a
 * full element with {@code id="N"} once, then later empty siblings as {@code <ps-permission
 * idref="N"/>}. Jackson XML does not resolve Betwixt idrefs; without expansion, only fully-inlined
 * permission blocks restore and package install can silently drop permissions (issue #1899 / epic
 * #505).
 *
 * <p><strong>Product decision:</strong> expand on read (not rewrite every package file). Applied on
 * the Jackson deserialize path via {@link PSJacksonXmlSerializationHelper#readFromXml(String,
 * Class)}.
 *
 * <p>Algorithm:
 *
 * <ol>
 *   <li>Fast-path: if the payload has no {@code idref} substring, return unchanged.
 *   <li>Index every element that has an {@code id} attribute and is not itself an idref stub.
 *   <li>For each element with {@code idref}, remove the attribute and deep-copy attributes/children
 *       from the referenced definition while keeping the local element name (so {@code <first-owner
 *       idref="…"/>} can materialize children of a {@code <typed-principal id="…">}).
 * </ol>
 *
 * <p>Unresolved idrefs are left as empty stubs (same as pre-expansion Jackson behavior).
 */
public final class PSBetwixtIdrefExpander {

  private static final Logger log = LogManager.getLogger(PSBetwixtIdrefExpander.class);

  /** Substring probe — case-sensitive attribute name as emitted by Betwixt. */
  private static final String IDREF_PROBE = "idref";

  private PSBetwixtIdrefExpander() {
    // utility
  }

  /**
   * Expand Betwixt {@code idref} graph references into full element copies.
   *
   * @param xmlString XML payload, may be blank
   * @return expanded XML, or the original string when no expansion is needed / possible
   * @throws IOException if the document cannot be parsed or written after expansion
   */
  public static String expandIdrefs(String xmlString) throws IOException {
    if (StringUtils.isBlank(xmlString) || !xmlString.contains(IDREF_PROBE)) {
      return xmlString;
    }

    Document doc;
    try {
      doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(xmlString), false);
    } catch (SAXException e) {
      throw new IOException("Failed to parse XML for Betwixt idref expansion", e);
    }
    if (doc == null || doc.getDocumentElement() == null) {
      return xmlString;
    }

    Map<String, Element> definitions = new HashMap<>();
    collectDefinitions(doc.getDocumentElement(), definitions);

    List<Element> idrefNodes = new ArrayList<>();
    collectIdrefNodes(doc.getDocumentElement(), idrefNodes);

    if (idrefNodes.isEmpty()) {
      return xmlString;
    }

    int expanded = 0;
    int unresolved = 0;
    for (Element ref : idrefNodes) {
      String refId = ref.getAttribute(IDREF_PROBE);
      if (StringUtils.isBlank(refId)) {
        continue;
      }
      Element def = definitions.get(refId);
      if (def == null) {
        unresolved++;
        log.debug(
            "Unresolved Betwixt idref='{}' on element <{}>; leaving empty stub",
            refId,
            ref.getTagName());
        continue;
      }
      materializeFromDefinition(ref, def);
      expanded++;
    }

    if (expanded == 0) {
      return xmlString;
    }
    if (unresolved > 0) {
      log.debug(
          "Betwixt idref expansion: expanded={}, unresolved={} (unresolved left as empty stubs)",
          expanded,
          unresolved);
    }

    return PSXmlDocumentBuilder.toString(doc);
  }

  /**
   * Whether {@code element} is an idref stub (has {@code idref} and is not a definition).
   *
   * @param element never {@code null}
   * @return true when the element declares {@code idref}
   */
  static boolean isIdrefStub(Element element) {
    return element != null && element.hasAttribute(IDREF_PROBE);
  }

  private static void collectDefinitions(Element element, Map<String, Element> definitions) {
    if (element.hasAttribute("id") && !element.hasAttribute(IDREF_PROBE)) {
      String id = element.getAttribute("id");
      if (StringUtils.isNotBlank(id) && !definitions.containsKey(id)) {
        definitions.put(id, element);
      }
    }
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        collectDefinitions((Element) child, definitions);
      }
    }
  }

  private static void collectIdrefNodes(Element element, List<Element> idrefNodes) {
    if (element.hasAttribute(IDREF_PROBE)) {
      idrefNodes.add(element);
    }
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        collectIdrefNodes((Element) child, idrefNodes);
      }
    }
  }

  /**
   * Copy definition attributes (except {@code id}/{@code idref}) and deep-clone children onto the
   * idref element. Keeps the local tag name of {@code target}.
   */
  private static void materializeFromDefinition(Element target, Element definition) {
    target.removeAttribute(IDREF_PROBE);

    NamedNodeMap attrs = definition.getAttributes();
    if (attrs != null) {
      for (int i = 0; i < attrs.getLength(); i++) {
        Attr attr = (Attr) attrs.item(i);
        String name = attr.getName();
        if ("id".equals(name) || IDREF_PROBE.equals(name)) {
          continue;
        }
        if (!target.hasAttribute(name)) {
          target.setAttribute(name, attr.getValue());
        }
      }
    }

    // Drop any pre-existing children (idref stubs are empty in package dumps)
    while (target.hasChildNodes()) {
      target.removeChild(target.getFirstChild());
    }

    NodeList defChildren = definition.getChildNodes();
    for (int i = 0; i < defChildren.getLength(); i++) {
      Node cloned = defChildren.item(i).cloneNode(true);
      target.appendChild(cloned);
    }
  }
}
