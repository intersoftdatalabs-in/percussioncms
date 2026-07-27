/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Represents an extension method. */
public final class PSExtensionMethod implements Serializable {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = 1383116678428785411L;

  /** The method name, never {@code null} or empty after construction. */
  private String m_name;

  /** The method description, never {@code null} after construction, may be empty. */
  private String m_description;

  /**
   * The method parameters, never {@code null}, may be empty. The order of the parameters is
   * important for the way a method is called.
   */
  private final List<PSExtensionMethodParam> m_parameters = new ArrayList<>();

  /** The method return type, never {@code null} or empty after construction. */
  private String m_returnType;

  /**
   * Convenience constructor that calls {@link #PSExtensionMethod(String, String, String)
   * PSExtensionMethod(name, returnType, null)}.
   *
   * @param name the method name, not {@code null} or empty
   * @param returnType the method return type, not {@code null} or empty
   */
  public PSExtensionMethod(String name, String returnType) {
    this(name, returnType, null);
  }

  /**
   * Construct a new extension method for the supplied parameters.
   *
   * @param name the method name, not {@code null} or empty
   * @param returnType the method return type, not {@code null} or empty
   * @param description the method description, may be {@code null} or empty
   */
  public PSExtensionMethod(String name, String returnType, String description) {
    setName(name);
    setReturnType(returnType);
    setDescription(description);
  }

  /**
   * Construct a new extension method from its XML representation.
   *
   * @param source the XML element from which to construct this method, not {@code null}
   * @throws PSExtensionException for any error deserializing the supplied source
   */
  public PSExtensionMethod(Element source) throws PSExtensionException {
    Objects.requireNonNull(source, "source element cannot be null");
    fromXML(source);
  }

  /**
   * Set the method name.
   *
   * @param name the new method name, not {@code null} or empty
   * @throws IllegalArgumentException if name is {@code null} or empty
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    m_name = name.trim();
  }

  /**
   * Get the method name.
   *
   * @return the method name, never {@code null} or empty
   */
  public String getName() {
    return m_name;
  }

  /**
   * Set the method description.
   *
   * @param description the new method description, may be {@code null}
   */
  public void setDescription(String description) {
    m_description = StringUtils.defaultString(description);
  }

  /**
   * Get the method description.
   *
   * @return the method description, never {@code null}, may be empty
   */
  public String getDescription() {
    return m_description;
  }

  /**
   * Get all method parameters.
   *
   * @return an iterator over all method parameters in the order they were added, never {@code
   *     null}, may be empty
   */
  public Iterator<PSExtensionMethodParam> getParameters() {
    return m_parameters.iterator();
  }

  /**
   * Add a new parameter to this method.
   *
   * @param parameter the parameter to add, not {@code null}
   * @throws IllegalArgumentException if parameter is {@code null}
   */
  public void addParameter(PSExtensionMethodParam parameter) {
    Objects.requireNonNull(parameter, "parameter cannot be null");

    // Check if parameter with same name already exists
    var existingParam =
        m_parameters.stream()
            .filter(p -> Objects.equals(p.getName(), parameter.getName()))
            .findFirst();

    if (existingParam.isPresent()) {
      throw new IllegalArgumentException(
          "Parameter with name '" + parameter.getName() + "' already exists");
    }

    m_parameters.add(parameter);
  }

  /**
   * Remove the parameter with the specified name.
   *
   * @param name the name of the parameter to remove, not {@code null} or empty
   * @throws IllegalArgumentException if name is {@code null} or empty
   */
  public void removeParameter(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }

    m_parameters.removeIf(param -> Objects.equals(param.getName(), name.trim()));
  }

  /**
   * Set the method return type.
   *
   * @param returnType the new method return type, not {@code null} or empty
   * @throws IllegalArgumentException if returnType is {@code null} or empty
   */
  public void setReturnType(String returnType) {
    if (StringUtils.isBlank(returnType)) {
      throw new IllegalArgumentException("returnType cannot be null or empty");
    }
    m_returnType = returnType.trim();
  }

  /**
   * Get the method return type.
   *
   * @return the method return type, never {@code null} or empty
   */
  public String getReturnType() {
    return m_returnType;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    var other = (PSExtensionMethod) obj;
    return Objects.equals(m_name, other.m_name)
        && Objects.equals(m_description, other.m_description)
        && Objects.equals(m_parameters, other.m_parameters)
        && Objects.equals(m_returnType, other.m_returnType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_name, m_description, m_parameters, m_returnType);
  }

  @Override
  public String toString() {
    return "PSExtensionMethod{"
        + "name='"
        + m_name
        + '\''
        + ", description='"
        + m_description
        + '\''
        + ", parameters="
        + m_parameters
        + ", returnType='"
        + m_returnType
        + '\''
        + '}';
  }

  /**
   * Deserialize this method from its XML representation.
   *
   * @param source the XML element from which to deserialize this method, not {@code null}
   * @throws PSExtensionException for any error deserializing the supplied source
   */
  public void fromXML(Element source) throws PSExtensionException {
    Objects.requireNonNull(source, "source element cannot be null");

    try {
      setName(source.getAttribute("name"));
      // Packages historically serialize lowercase "returntype"; toXML uses "returnType".
      // DOM attribute names are case-sensitive — accept both.
      setReturnType(readReturnTypeAttribute(source));
      setDescription(source.getAttribute("description"));

      var paramNodes = source.getElementsByTagName(PSExtensionMethodParam.XML_NAME);
      for (int i = 0; i < paramNodes.getLength(); i++) {
        var paramElement = (Element) paramNodes.item(i);
        addParameter(new PSExtensionMethodParam(paramElement));
      }
    } catch (Exception e) {
      throw new PSExtensionException("Failed to deserialize PSExtensionMethod", e);
    }
  }

  /**
   * Reads the method return-type attribute from extension XML.
   *
   * <p>Shipped {@code .extension} package files use {@code returntype} (all lowercase). Runtime
   * {@link #toXML(Document)} writes {@code returnType}. Both must deserialize successfully.
   *
   * @param source method element, not {@code null}
   * @return attribute value, may be blank if neither attribute is present
   */
  static String readReturnTypeAttribute(Element source) {
    Objects.requireNonNull(source, "source element cannot be null");
    String returnType = source.getAttribute(XML_ATTR_RETURN_TYPE);
    if (StringUtils.isBlank(returnType)) {
      returnType = source.getAttribute(XML_ATTR_RETURN_TYPE_LEGACY);
    }
    return returnType;
  }

  /**
   * Serialize this method to its XML representation.
   *
   * @param doc the document to use for creating new elements, not {@code null}
   * @return the XML element representing this method, never {@code null}
   * @throws IllegalArgumentException if doc is {@code null}
   */
  public Element toXML(Document doc) {
    Objects.requireNonNull(doc, "document cannot be null");

    var element = doc.createElement(XML_NAME);
    element.setAttribute("name", m_name);
    element.setAttribute(XML_ATTR_RETURN_TYPE, m_returnType);
    element.setAttribute("description", m_description);

    for (var param : m_parameters) {
      element.appendChild(param.toXML(doc));
    }

    return element;
  }

  /** The XML element name for this class. */
  public static final String XML_NAME = "PSExtensionMethod";

  /** Canonical attribute name written by {@link #toXML(Document)}. */
  public static final String XML_ATTR_RETURN_TYPE = "returnType";

  /**
   * Legacy attribute name found in shipped package {@code .extension} files (e.g. pageutils in
   * perc.Baseline).
   */
  public static final String XML_ATTR_RETURN_TYPE_LEGACY = "returntype";
}
