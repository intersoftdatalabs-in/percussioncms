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

package com.percussion.packages.pagexml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * Parses legacy Page / assembly {@code *.templateDef} XML ({@code <assembly-template>}) into {@link
 * PSPageXmlModel}.
 *
 * <p>Uses a hardened {@link DocumentBuilderFactory} (no external DTDs / schemas / XInclude). Paths
 * are read via portable NIO {@link Files}.
 */
public final class PSPageXmlParser {

  /** Velocity {@code #region("id" …)} macro — first arg is the region / slot id. */
  private static final Pattern REGION_MACRO =
      Pattern.compile("#region\\(\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

  /**
   * Markup container that often wraps a region hole: {@code id="regionId" class="…"}. Captures the
   * class attribute for layout/style hints (region-slot mapping / ADR-003 direction).
   */
  private static final Pattern ID_CLASS =
      Pattern.compile(
          "id\\s*=\\s*\"([^\"]+)\"\\s+class\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

  private static final Pattern CLASS_ID =
      Pattern.compile(
          "class\\s*=\\s*\"([^\"]+)\"\\s+id\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

  private PSPageXmlParser() {
    // utility
  }

  /**
   * Parse a {@code *.templateDef} file as UTF-8.
   *
   * @param path non-null path
   * @return parsed model
   * @throws PSPageXmlException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSPageXmlModel parse(Path path) throws PSPageXmlException, IOException {
    Objects.requireNonNull(path, "path");
    String xml = Files.readString(path, StandardCharsets.UTF_8);
    PSPageXmlModel model = parse(xml);
    Path fileName = path.getFileName();
    if (fileName != null) {
      model.setSourceFileName(fileName.toString());
    }
    return model;
  }

  /**
   * Parse assembly-template XML from a string.
   *
   * @param xml non-null document text
   * @return parsed model
   * @throws PSPageXmlException on parse failure
   */
  public static PSPageXmlModel parse(String xml) throws PSPageXmlException {
    Objects.requireNonNull(xml, "xml");
    if (xml.isBlank()) {
      throw new PSPageXmlException("Page templateDef XML is empty");
    }
    try {
      Document doc = parseDocument(new InputSource(new StringReader(xml)));
      return fromDocument(doc, null);
    } catch (PSPageXmlException e) {
      throw e;
    } catch (Exception e) {
      throw new PSPageXmlException("Failed to parse Page templateDef XML: " + e.getMessage(), e);
    }
  }

  /**
   * Parse from a reader.
   *
   * @param reader non-null reader
   * @param sourceFileName optional file name for stem derivation
   * @return parsed model
   * @throws PSPageXmlException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSPageXmlModel parse(Reader reader, String sourceFileName)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(reader, "reader");
    try {
      Document doc = parseDocument(new InputSource(reader));
      return fromDocument(doc, sourceFileName);
    } catch (PSPageXmlException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new PSPageXmlException("Failed to parse Page templateDef XML: " + e.getMessage(), e);
    }
  }

  /**
   * Parse from an input stream.
   *
   * @param in non-null stream
   * @param sourceFileName optional file name for stem derivation
   * @return parsed model
   * @throws PSPageXmlException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSPageXmlModel parse(InputStream in, String sourceFileName)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(in, "in");
    try {
      Document doc = parseDocument(new InputSource(in));
      return fromDocument(doc, sourceFileName);
    } catch (PSPageXmlException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new PSPageXmlException("Failed to parse Page templateDef XML: " + e.getMessage(), e);
    }
  }

  private static Document parseDocument(InputSource source)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setExpandEntityReferences(false);
    factory.setXIncludeAware(false);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory.newDocumentBuilder().parse(source);
  }

  private static PSPageXmlModel fromDocument(Document doc, String sourceFileName)
      throws PSPageXmlException {
    Element root = doc.getDocumentElement();
    if (root == null || !"assembly-template".equals(root.getTagName())) {
      throw new PSPageXmlException(
          "Expected root element <assembly-template>, found: "
              + (root == null ? "null" : root.getTagName()));
    }

    PSPageXmlModel model = new PSPageXmlModel();
    if (sourceFileName != null && !sourceFileName.isBlank()) {
      model.setSourceFileName(sourceFileName);
    }

    model.setName(childText(root, "name"));
    model.setLabel(childText(root, "label"));
    model.setDescription(childText(root, "description"));
    model.setGuid(childText(root, "guid"));
    model.setAssembler(childText(root, "assembler"));
    model.setOutputFormat(childText(root, "output-format"));
    model.setTemplateType(childText(root, "template-type"));
    model.setMimeType(childText(root, "mime-type"));
    model.setCharset(childText(root, "charset"));
    model.setActiveAssemblyType(childText(root, "active-assembly-type"));
    model.setPublishWhen(childText(root, "publish-when"));
    model.setLocationPrefix(childText(root, "location-prefix"));
    model.setLocationSuffix(childText(root, "location-suffix"));

    Element templateEl = firstChildElement(root, "template");
    String body = templateEl != null ? normalizeBody(textContent(templateEl)) : null;
    model.setTemplateBody(body);

    List<PSPageXmlModel.Binding> bindings = new ArrayList<>();
    Element bindingsEl = firstChildElement(root, "bindings");
    if (bindingsEl != null) {
      for (Element b : childElements(bindingsEl, "binding")) {
        PSPageXmlModel.Binding binding = new PSPageXmlModel.Binding();
        // Common shapes: <binding variable="x" expression="…"/> or nested <variable>/<expression>
        binding.setVariable(firstNonBlank(attr(b, "variable"), attr(b, "name"), childText(b, "variable")));
        binding.setExpression(
            firstNonBlank(attr(b, "expression"), attr(b, "value"), childText(b, "expression")));
        if (binding.getVariable() != null && !binding.getVariable().isBlank()) {
          bindings.add(binding);
        }
      }
    }
    model.setBindings(bindings);

    model.setRegionHoles(extractRegionHoles(body));
    return model;
  }

  /**
   * Discover {@code #region} holes and attach CSS class / layout hints from matching markup {@code
   * id} containers when present.
   */
  static List<PSPageXmlModel.RegionHole> extractRegionHoles(String templateBody) {
    List<PSPageXmlModel.RegionHole> holes = new ArrayList<>();
    if (templateBody == null || templateBody.isBlank()) {
      return holes;
    }

    Map<String, String> idToClass = new LinkedHashMap<>();
    Matcher idClass = ID_CLASS.matcher(templateBody);
    while (idClass.find()) {
      idToClass.putIfAbsent(idClass.group(1), idClass.group(2));
    }
    Matcher classId = CLASS_ID.matcher(templateBody);
    while (classId.find()) {
      idToClass.putIfAbsent(classId.group(2), classId.group(1));
    }

    Set<String> seen = new LinkedHashSet<>();
    Matcher region = REGION_MACRO.matcher(templateBody);
    while (region.find()) {
      String id = region.group(1).trim();
      if (id.isEmpty() || !seen.add(id)) {
        continue;
      }
      PSPageXmlModel.RegionHole hole = new PSPageXmlModel.RegionHole();
      hole.setRegionId(id);
      String css = idToClass.get(id);
      hole.setCssClass(css);
      applyClassHints(hole, css);
      holes.add(hole);
    }
    return holes;
  }

  /**
   * Map CM1 region CSS class tokens onto layout / style hint maps (ADR-003 direction).
   *
   * <ul>
   *   <li>{@code perc-vertical} / {@code perc-horizontal} → {@code layout.orientation}
   *   <li>{@code vspan_N} / {@code hspan_N} → layout span hints
   *   <li>full class string → {@code styles.rootclass}
   * </ul>
   */
  static void applyClassHints(PSPageXmlModel.RegionHole hole, String cssClass) {
    Map<String, Object> layout = new LinkedHashMap<>();
    Map<String, Object> styles = new LinkedHashMap<>();
    if (cssClass != null && !cssClass.isBlank()) {
      styles.put("rootclass", cssClass.trim());
      for (String token : cssClass.trim().split("\\s+")) {
        if (token.isEmpty()) {
          continue;
        }
        String t = token.toLowerCase(Locale.ROOT);
        if ("perc-vertical".equals(t)) {
          layout.put("orientation", "vertical");
        } else if ("perc-horizontal".equals(t)) {
          layout.put("orientation", "horizontal");
        } else if (t.startsWith("vspan_")) {
          layout.put("vspan", token.substring("vspan_".length()));
        } else if (t.startsWith("hspan_")) {
          layout.put("hspan", token.substring("hspan_".length()));
        }
      }
    }
    hole.setLayoutHints(layout);
    hole.setStyleHints(styles);
  }

  /**
   * Normalize body text for cross-platform golden parity: CRLF/CR → LF, strip leading/trailing
   * whitespace.
   */
  static String normalizeBody(String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.replace("\r\n", "\n").replace('\r', '\n');
    return s.stripLeading().stripTrailing();
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

  private static String childText(Element parent, String name) {
    Element el = firstChildElement(parent, name);
    if (el == null) {
      return null;
    }
    String t = textContent(el);
    if (t == null) {
      return null;
    }
    String trimmed = t.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String attr(Element el, String name) {
    if (!el.hasAttribute(name)) {
      return null;
    }
    String v = el.getAttribute(name);
    return v.isEmpty() ? null : v;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v.trim();
      }
    }
    return null;
  }

  private static String textContent(Element el) {
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
      return el.getTextContent();
    }
    return sb.toString();
  }
}
