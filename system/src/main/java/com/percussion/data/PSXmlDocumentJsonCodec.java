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

import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bidirectional codec between classic XML Application result/input {@link Document}s and JSON.
 *
 * <p>Mapping rules (v1, reversible):
 *
 * <ul>
 *   <li>Document root is preserved as the single top-level JSON object key (root element name).
 *   <li>XML attributes become JSON properties with the {@value #ATTR_PREFIX} prefix.
 *   <li>Text-only elements become JSON strings; empty elements become JSON {@code null}.
 *   <li>Elements with attributes and/or child elements become JSON objects; character data is
 *       stored under {@value #TEXT_KEY} when only trailing text (or text with attributes only).
 *   <li>True mixed content (significant text before or between element children) is stored as an
 *       ordered array under {@value #MIXED_KEY} so encode/decode preserves interleaving.
 *   <li>Repeated same-name sibling elements become a JSON array.
 *   <li>On decode, numbers and booleans are stringified (XML leaf values are always text).
 * </ul>
 */
public final class PSXmlDocumentJsonCodec {

  /** Prefix applied to XML attribute names in JSON object properties. */
  public static final String ATTR_PREFIX = "@";

  /** Property name used for element character data when the element is not text-only. */
  public static final String TEXT_KEY = "#text";

  /**
   * Property name for an ordered array of mixed content parts (strings and single-key element
   * objects) when text and element children interleave.
   */
  public static final String MIXED_KEY = "#mixed";

  /** Maximum nesting depth for encode/decode (hostile payloads). */
  public static final int MAX_DEPTH = 64;

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private static final TypeReference<Map<String, Object>> MAP_TYPE =
      new TypeReference<Map<String, Object>>() {};

  private PSXmlDocumentJsonCodec() {}

  /**
   * Encode a DOM document as compact JSON text (UTF-8 logical string).
   *
   * @param document never {@code null}; must have a document element
   * @return JSON string, never {@code null}
   * @throws PSConversionException if the document cannot be encoded
   */
  public static String toJson(Document document) throws PSConversionException {
    Objects.requireNonNull(document, "document");
    Element root = document.getDocumentElement();
    if (root == null) {
      throw new PSConversionException(DataErrorCodes.NO_DATA_FOR_CONVERSION);
    }
    try {
      Map<String, Object> wrapper = new LinkedHashMap<>();
      wrapper.put(root.getNodeName(), elementToJsonValue(root, 0));
      return MAPPER.writeValueAsString(wrapper);
    } catch (JacksonException e) {
      throw new PSConversionException(
          DataErrorCodes.XML_CONV_EXCEPTION,
          new Object[] {"", "json encode: " + e.getMessage()});
    } catch (IllegalArgumentException e) {
      throw new PSConversionException(
          DataErrorCodes.XML_CONV_EXCEPTION, new Object[] {"", e.getMessage()});
    }
  }

  /**
   * Decode JSON text into a DOM document using the same mapping rules as {@link #toJson}.
   *
   * @param json never {@code null} or blank
   * @return document with a single root element, never {@code null}
   * @throws PSConversionException if the JSON is invalid or does not match the mapping rules
   */
  public static Document fromJson(String json) throws PSConversionException {
    Objects.requireNonNull(json, "json");
    if (json.isBlank()) {
      throw new PSConversionException(
          DataErrorCodes.XML_CONV_EXCEPTION, new Object[] {"", "empty JSON body"});
    }
    return fromJson(new StringReader(json));
  }

  /**
   * Decode JSON from a character stream.
   *
   * @param reader never {@code null}
   * @return document, never {@code null}
   * @throws PSConversionException if decode fails
   */
  public static Document fromJson(Reader reader) throws PSConversionException {
    Objects.requireNonNull(reader, "reader");
    try {
      Map<String, Object> rootMap = MAPPER.readValue(reader, MAP_TYPE);
      if (rootMap == null || rootMap.isEmpty()) {
        throw new IllegalArgumentException("JSON root must be a non-empty object");
      }
      if (rootMap.size() != 1) {
        throw new IllegalArgumentException(
            "JSON root must have exactly one property (the XML root element name)");
      }
      Map.Entry<String, Object> only = rootMap.entrySet().iterator().next();
      String rootName = only.getKey();
      requireXmlName(rootName, "root element");
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = doc.createElement(rootName);
      doc.appendChild(root);
      applyJsonValueToElement(doc, root, only.getValue(), 0);
      return doc;
    } catch (JacksonException e) {
      throw new PSConversionException(
          DataErrorCodes.XML_CONV_EXCEPTION,
          new Object[] {"", "json decode: " + e.getMessage()});
    } catch (IllegalArgumentException e) {
      throw new PSConversionException(
          DataErrorCodes.XML_CONV_EXCEPTION, new Object[] {"", e.getMessage()});
    }
  }

  private static Object elementToJsonValue(Element element, int depth) {
    if (depth > MAX_DEPTH) {
      throw new IllegalArgumentException("XML nesting exceeds maximum depth " + MAX_DEPTH);
    }

    NamedNodeMap attrs = element.getAttributes();
    Map<String, Object> attrMap = new LinkedHashMap<>();
    if (attrs != null) {
      for (int i = 0; i < attrs.getLength(); i++) {
        Attr attr = (Attr) attrs.item(i);
        attrMap.put(ATTR_PREFIX + attr.getName(), attr.getValue());
      }
    }

    // Ordered significant children for mixed-content detection / encoding.
    List<Node> ordered = new ArrayList<>();
    Map<String, List<Element>> childGroups = new LinkedHashMap<>();
    StringBuilder text = new StringBuilder();
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      short type = n.getNodeType();
      if (type == Node.ELEMENT_NODE) {
        Element child = (Element) n;
        ordered.add(child);
        childGroups.computeIfAbsent(child.getNodeName(), k -> new ArrayList<>()).add(child);
      } else if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
        String data = ((Text) n).getData();
        text.append(data);
        if (data != null && !data.trim().isEmpty()) {
          ordered.add(n);
        }
      }
      // ignore comments, PIs, etc.
    }

    String textContent = text.toString();
    boolean hasElementChildren = !childGroups.isEmpty();
    // Collapse pure-whitespace text when we have element children (pretty-printed XML)
    if (hasElementChildren && textContent.trim().isEmpty()) {
      textContent = "";
      ordered.removeIf(
          n ->
              n.getNodeType() == Node.TEXT_NODE
                  || n.getNodeType() == Node.CDATA_SECTION_NODE);
    }

    boolean hasAttrs = !attrMap.isEmpty();
    boolean hasText = !textContent.isEmpty();

    if (!hasAttrs && !hasElementChildren) {
      if (!hasText) {
        return null; // empty element
      }
      return textContent;
    }

    // True mixed content: significant text appears before or between element children.
    boolean needsMixed = false;
    if (hasElementChildren && hasText) {
      int lastElementIndex = -1;
      for (int i = 0; i < ordered.size(); i++) {
        if (ordered.get(i).getNodeType() == Node.ELEMENT_NODE) {
          lastElementIndex = i;
        }
      }
      for (int i = 0; i < lastElementIndex; i++) {
        short t = ordered.get(i).getNodeType();
        if (t == Node.TEXT_NODE || t == Node.CDATA_SECTION_NODE) {
          needsMixed = true;
          break;
        }
      }
    }

    Map<String, Object> obj = new LinkedHashMap<>();
    obj.putAll(attrMap);

    if (needsMixed) {
      List<Object> mixed = new ArrayList<>();
      StringBuilder run = new StringBuilder();
      for (Node n : ordered) {
        if (n.getNodeType() == Node.ELEMENT_NODE) {
          if (run.length() > 0) {
            mixed.add(run.toString());
            run.setLength(0);
          }
          Element child = (Element) n;
          Map<String, Object> wrapper = new LinkedHashMap<>(1);
          wrapper.put(child.getNodeName(), elementToJsonValue(child, depth + 1));
          mixed.add(wrapper);
        } else {
          run.append(((Text) n).getData());
        }
      }
      if (run.length() > 0) {
        mixed.add(run.toString());
      }
      obj.put(MIXED_KEY, mixed);
      return obj;
    }

    for (Map.Entry<String, List<Element>> entry : childGroups.entrySet()) {
      List<Element> group = entry.getValue();
      if (group.size() == 1) {
        obj.put(entry.getKey(), elementToJsonValue(group.get(0), depth + 1));
      } else {
        List<Object> arr = new ArrayList<>(group.size());
        for (Element e : group) {
          arr.add(elementToJsonValue(e, depth + 1));
        }
        obj.put(entry.getKey(), arr);
      }
    }

    if (hasText) {
      obj.put(TEXT_KEY, textContent);
    }

    return obj;
  }

  @SuppressWarnings("unchecked")
  private static void applyJsonValueToElement(
      Document doc, Element element, Object value, int depth) {
    if (depth > MAX_DEPTH) {
      throw new IllegalArgumentException("JSON nesting exceeds maximum depth " + MAX_DEPTH);
    }

    if (value == null) {
      // empty element — nothing to add
      return;
    }

    if (value instanceof String
        || value instanceof Number
        || value instanceof Boolean) {
      element.appendChild(doc.createTextNode(stringifyLeaf(value)));
      return;
    }

    if (value instanceof Map<?, ?> mapRaw) {
      Map<String, Object> map = (Map<String, Object>) mapRaw;
      // Attributes first
      for (Map.Entry<String, Object> e : map.entrySet()) {
        String key = e.getKey();
        if (key == null) {
          throw new IllegalArgumentException("JSON object property name must not be null");
        }
        if (key.startsWith(ATTR_PREFIX)) {
          if (key.length() <= ATTR_PREFIX.length()) {
            throw new IllegalArgumentException("Invalid XML attribute name: empty after " + ATTR_PREFIX);
          }
          String attrName = key.substring(ATTR_PREFIX.length());
          requireXmlAttributeName(attrName);
          element.setAttribute(attrName, stringifyLeaf(e.getValue()));
        }
      }
      // Ordered mixed content takes precedence over named children + trailing #text
      if (map.containsKey(MIXED_KEY)) {
        applyMixedContent(doc, element, map.get(MIXED_KEY), depth);
        return;
      }
      for (Map.Entry<String, Object> e : map.entrySet()) {
        String key = e.getKey();
        if (key == null
            || key.startsWith(ATTR_PREFIX)
            || TEXT_KEY.equals(key)
            || MIXED_KEY.equals(key)) {
          continue;
        }
        appendChildFromJson(doc, element, key, e.getValue(), depth);
      }
      if (map.containsKey(TEXT_KEY)) {
        Object t = map.get(TEXT_KEY);
        if (t != null) {
          element.appendChild(doc.createTextNode(stringifyLeaf(t)));
        }
      }
      return;
    }

    if (value instanceof List<?>) {
      throw new IllegalArgumentException(
          "JSON array cannot be the value of the document root element");
    }

    throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass().getName());
  }

  @SuppressWarnings("unchecked")
  private static void applyMixedContent(
      Document doc, Element element, Object mixedValue, int depth) {
    if (!(mixedValue instanceof List<?> list)) {
      throw new IllegalArgumentException(MIXED_KEY + " must be a JSON array");
    }
    for (Object item : list) {
      if (item == null) {
        continue;
      }
      if (item instanceof String
          || item instanceof Number
          || item instanceof Boolean) {
        element.appendChild(doc.createTextNode(stringifyLeaf(item)));
        continue;
      }
      if (item instanceof Map<?, ?> mapRaw) {
        Map<String, Object> map = (Map<String, Object>) mapRaw;
        if (map.size() != 1) {
          throw new IllegalArgumentException(
              MIXED_KEY + " element entries must be single-key objects");
        }
        Map.Entry<String, Object> only = map.entrySet().iterator().next();
        String name = only.getKey();
        if (name == null
            || name.startsWith(ATTR_PREFIX)
            || TEXT_KEY.equals(name)
            || MIXED_KEY.equals(name)) {
          throw new IllegalArgumentException("Invalid " + MIXED_KEY + " element name: " + name);
        }
        appendChildFromJson(doc, element, name, only.getValue(), depth);
        continue;
      }
      throw new IllegalArgumentException(
          "Unsupported " + MIXED_KEY + " entry type: " + item.getClass().getName());
    }
  }

  @SuppressWarnings("unchecked")
  private static void appendChildFromJson(
      Document doc, Element parent, String name, Object value, int depth) {
    requireXmlName(name, "element");
    if (value instanceof List<?> list) {
      for (Object item : list) {
        Element child = doc.createElement(name);
        parent.appendChild(child);
        applyJsonValueToElement(doc, child, item, depth + 1);
      }
      return;
    }

    Element child = doc.createElement(name);
    parent.appendChild(child);
    applyJsonValueToElement(doc, child, value, depth + 1);
  }

  /**
   * Reject names that are not valid XML Name tokens (or that collide with codec meta keys) before
   * calling {@link Document#createElement(String)}.
   */
  private static void requireXmlName(String name, String role) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Invalid XML " + role + " name: empty");
    }
    if (name.startsWith(ATTR_PREFIX) || TEXT_KEY.equals(name) || MIXED_KEY.equals(name)) {
      throw new IllegalArgumentException("Invalid XML " + role + " name: " + name);
    }
    // Simplified XML Name: NameStartChar then NameChar* (no colons — no namespaces in v1)
    if (!name.matches("[A-Za-z_][A-Za-z0-9_.-]*")) {
      throw new IllegalArgumentException("Invalid XML " + role + " name: " + name);
    }
  }

  private static void requireXmlAttributeName(String name) {
    if (name == null || name.isEmpty() || !name.matches("[A-Za-z_][A-Za-z0-9_.-]*")) {
      throw new IllegalArgumentException("Invalid XML attribute name: " + name);
    }
  }

  private static String stringifyLeaf(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof String s) {
      return s;
    }
    if (value instanceof Boolean || value instanceof Number) {
      return String.valueOf(value);
    }
    // Nested structures as attribute/text are not supported
    if (value instanceof Map || value instanceof List) {
      throw new IllegalArgumentException("Structured JSON cannot be used as an XML leaf value");
    }
    return String.valueOf(value);
  }
}
