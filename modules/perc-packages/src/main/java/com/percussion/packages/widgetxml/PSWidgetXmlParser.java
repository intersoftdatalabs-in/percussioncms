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

package com.percussion.packages.widgetxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
 * Parses legacy Widget definition XML ({@code <Widget>…</Widget>}) into {@link PSWidgetXmlModel}.
 *
 * <p>Uses a hardened {@link DocumentBuilderFactory} (no external DTDs / schemas / XInclude). Paths
 * are read via portable NIO {@link Files}.
 */
public final class PSWidgetXmlParser {

  private PSWidgetXmlParser() {
    // utility
  }

  /**
   * Parse Widget XML from a UTF-8 file.
   *
   * @param path non-null path to a {@code .xml} widget definition
   * @return parsed model
   * @throws PSWidgetXmlException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSWidgetXmlModel parse(Path path) throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(path, "path");
    String xml = Files.readString(path, StandardCharsets.UTF_8);
    PSWidgetXmlModel model = parse(xml);
    Path fileName = path.getFileName();
    if (fileName != null) {
      model.setSourceFileName(fileName.toString());
    }
    return model;
  }

  /**
   * Parse Widget XML from a string.
   *
   * @param xml non-null document text
   * @return parsed model
   * @throws PSWidgetXmlException on parse failure
   */
  public static PSWidgetXmlModel parse(String xml) throws PSWidgetXmlException {
    Objects.requireNonNull(xml, "xml");
    if (xml.isBlank()) {
      throw new PSWidgetXmlException("Widget XML is empty");
    }
    try {
      Document doc = parseDocument(new InputSource(new StringReader(xml)));
      return fromDocument(doc, null);
    } catch (PSWidgetXmlException e) {
      throw e;
    } catch (Exception e) {
      throw new PSWidgetXmlException("Failed to parse Widget XML: " + e.getMessage(), e);
    }
  }

  /**
   * Parse Widget XML from a reader.
   *
   * @param reader non-null reader
   * @param sourceFileName optional file name for stem derivation
   * @return parsed model
   * @throws PSWidgetXmlException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSWidgetXmlModel parse(Reader reader, String sourceFileName)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(reader, "reader");
    try {
      Document doc = parseDocument(new InputSource(reader));
      return fromDocument(doc, sourceFileName);
    } catch (PSWidgetXmlException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new PSWidgetXmlException("Failed to parse Widget XML: " + e.getMessage(), e);
    }
  }

  /**
   * Parse Widget XML from an input stream (UTF-8 assumed by the XML decoder).
   *
   * @param in non-null stream
   * @param sourceFileName optional file name for stem derivation
   * @return parsed model
   * @throws PSWidgetXmlException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSWidgetXmlModel parse(InputStream in, String sourceFileName)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(in, "in");
    try {
      Document doc = parseDocument(new InputSource(in));
      return fromDocument(doc, sourceFileName);
    } catch (PSWidgetXmlException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new PSWidgetXmlException("Failed to parse Widget XML: " + e.getMessage(), e);
    }
  }

  private static Document parseDocument(InputSource source)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setExpandEntityReferences(false);
    factory.setXIncludeAware(false);
    // Harden against XXE / external entity expansion.
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory.newDocumentBuilder().parse(source);
  }

  private static PSWidgetXmlModel fromDocument(Document doc, String sourceFileName)
      throws PSWidgetXmlException {
    Element root = doc.getDocumentElement();
    if (root == null || !"Widget".equals(root.getTagName())) {
      throw new PSWidgetXmlException(
          "Expected root element <Widget>, found: "
              + (root == null ? "null" : root.getTagName()));
    }

    PSWidgetXmlModel model = new PSWidgetXmlModel();
    if (sourceFileName != null && !sourceFileName.isBlank()) {
      model.setSourceFileName(sourceFileName);
    }

    Element prefs = firstChildElement(root, "WidgetPrefs");
    if (prefs != null) {
      model.setTitle(attr(prefs, "title"));
      model.setContentTypeName(attr(prefs, "contenttype_name"));
      model.setCategory(attr(prefs, "category"));
      model.setDescription(attr(prefs, "description"));
      model.setAuthor(attr(prefs, "author"));
      model.setThumbnail(attr(prefs, "thumbnail"));
      model.setPreferredEditorWidth(intAttr(prefs, "preferred_editor_width"));
      model.setPreferredEditorHeight(intAttr(prefs, "preferred_editor_height"));
      model.setCreateSharedAsset(boolAttr(prefs, "create_shared_asset"));
      model.setEditableOnTemplate(boolAttr(prefs, "is_editable_on_template"));
      model.setResponsive(boolAttr(prefs, "is_responsive"));
    }

    List<PSWidgetXmlModel.UserPref> userPrefs = new ArrayList<>();
    for (Element el : childElements(root, "UserPref")) {
      userPrefs.add(parseUserPref(el));
    }
    model.setUserPrefs(userPrefs);

    List<PSWidgetXmlModel.CssPref> cssPrefs = new ArrayList<>();
    for (Element el : childElements(root, "CssPref")) {
      cssPrefs.add(parseCssPref(el));
    }
    model.setCssPrefs(cssPrefs);

    List<PSWidgetXmlModel.Resource> resources = new ArrayList<>();
    for (Element el : childElements(root, "Resource")) {
      resources.add(parseResource(el));
    }
    model.setResources(resources);

    Element code = firstChildElement(root, "Code");
    if (code != null) {
      model.setCodeType(attr(code, "type"));
      model.setCodeBody(normalizeBody(textContent(code)));
    }

    Element content = firstChildElement(root, "Content");
    if (content != null) {
      model.setContentType(attr(content, "type"));
      model.setContentBody(normalizeBody(textContent(content)));
    }

    return model;
  }

  private static PSWidgetXmlModel.UserPref parseUserPref(Element el) {
    PSWidgetXmlModel.UserPref pref = new PSWidgetXmlModel.UserPref();
    pref.setName(attr(el, "name"));
    pref.setDisplayName(attr(el, "display_name"));
    pref.setDatatype(attr(el, "datatype"));
    pref.setDefaultValue(attr(el, "default_value"));
    Boolean required = boolAttr(el, "required");
    pref.setRequired(required != null && required);
    List<PSWidgetXmlModel.EnumValue> enums = new ArrayList<>();
    for (Element ev : childElements(el, "EnumValue")) {
      PSWidgetXmlModel.EnumValue enumValue = new PSWidgetXmlModel.EnumValue();
      enumValue.setValue(attr(ev, "value"));
      enumValue.setDisplayValue(attr(ev, "display_value"));
      enums.add(enumValue);
    }
    pref.setEnumValues(enums);
    return pref;
  }

  private static PSWidgetXmlModel.CssPref parseCssPref(Element el) {
    PSWidgetXmlModel.CssPref pref = new PSWidgetXmlModel.CssPref();
    pref.setName(attr(el, "name"));
    pref.setDisplayName(attr(el, "display_name"));
    pref.setDatatype(attr(el, "datatype"));
    pref.setDefaultValue(attr(el, "default_value"));
    return pref;
  }

  private static PSWidgetXmlModel.Resource parseResource(Element el) {
    PSWidgetXmlModel.Resource resource = new PSWidgetXmlModel.Resource();
    resource.setHref(attr(el, "href"));
    resource.setType(attr(el, "type"));
    resource.setPlacement(attr(el, "placement"));
    return resource;
  }

  private static Element firstChildElement(Element parent, String name) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(n.getNodeName())) {
        return (Element) n;
      }
    }
    return null;
  }

  private static List<Element> childElements(Element parent, String name) {
    List<Element> out = new ArrayList<>();
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(n.getNodeName())) {
        out.add((Element) n);
      }
    }
    return out;
  }

  private static String attr(Element el, String name) {
    if (!el.hasAttribute(name)) {
      return null;
    }
    String v = el.getAttribute(name);
    return v.isEmpty() ? null : v;
  }

  private static Integer intAttr(Element el, String name) {
    String v = attr(el, name);
    if (v == null) {
      return null;
    }
    try {
      return Integer.valueOf(v.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Boolean boolAttr(Element el, String name) {
    String v = attr(el, name);
    if (v == null) {
      return null;
    }
    return Boolean.valueOf(v.trim());
  }

  private static String textContent(Element el) {
    // Prefer concatenated text/CDATA children only (ignore nested elements if any).
    StringBuilder sb = new StringBuilder();
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      short type = n.getNodeType();
      if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
        sb.append(n.getNodeValue());
      }
    }
    if (sb.length() == 0) {
      // Fallback: DOM textContent (covers unusual nesting).
      return el.getTextContent();
    }
    return sb.toString();
  }

  /**
   * Normalize body text for cross-platform golden parity: CRLF/CR → LF, strip leading/trailing
   * whitespace (pretty-printed CDATA often starts with newline + indent), preserve internal
   * relative indentation between non-blank lines.
   */
  static String normalizeBody(String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.replace("\r\n", "\n").replace('\r', '\n');
    // stripLeading/stripTrailing remove all leading/trailing whitespace including newlines.
    return s.stripLeading().stripTrailing();
  }
}
