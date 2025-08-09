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
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A representation of an extension method parameter.
 */
public final class PSExtensionMethodParam implements Serializable {
  /**
   * Compiler generated serial version ID used for serialization.
   */
  private static final long serialVersionUID = 4417119994571577998L;

  /**
   * The parameter name, never {@code null} or empty after construction.
   */
  private String m_name;

  /**
   * The parameter type, never {@code null} or empty after construction.
   */
  private String m_type;

  /**
   * The parameter description, never {@code null} after construction,
   * may be empty.
   */
  private String m_description;

  /**
   * Convenience constructor that calls {@link #PSExtensionMethodParam(String,
   * String, String) PSExtensionMethodParam(name, type, null)}.
   *
   * @param name the parameter name, not {@code null} or empty
   * @param type the parameter type, not {@code null} or empty
   */
  public PSExtensionMethodParam(String name, String type) {
    this(name, type, null);
  }

  /**
   * Construct a new extension method parameter for the supplied parameters.
   *
   * @param name the parameter name, not {@code null} or empty
   * @param type the parameter type, not {@code null} or empty
   * @param description the parameter description, may be {@code null} or empty
   */
  public PSExtensionMethodParam(String name, String type, String description) {
    setName(name);
    setType(type);
    setDescription(description);
  }

  /**
   * Construct an extension method parameter from its xml representation.
   *
   * @param source the source element from which to construct this, not
   *    {@code null}
   * @throws PSExtensionException for any error deserializing the supplied
   *    element
   */
  public PSExtensionMethodParam(Element source) throws PSExtensionException {
    Objects.requireNonNull(source, "source element cannot be null");
    fromXML(source);
  }

  /**
   * Set the parameter name.
   *
   * @param name the new name, not {@code null} or empty
   * @throws IllegalArgumentException if name is {@code null} or empty
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    m_name = name.trim();
  }

  /**
   * Get the parameter name.
   *
   * @return the parameter name, never {@code null} or empty
   */
  public String getName() {
    return m_name;
  }

  /**
   * Set the parameter type.
   *
   * @param type the new type, not {@code null} or empty
   * @throws IllegalArgumentException if type is {@code null} or empty
   */
  public void setType(String type) {
    if (StringUtils.isBlank(type)) {
      throw new IllegalArgumentException("type cannot be null or empty");
    }
    m_type = type.trim();
  }

  /**
   * Get the parameter type.
   *
   * @return the parameter type, never {@code null} or empty
   */
  public String getType() {
    return m_type;
  }

  /**
   * Set the parameter description.
   *
   * @param description the new description, may be {@code null}
   */
  public void setDescription(String description) {
    m_description = StringUtils.defaultString(description);
  }

  /**
   * Get the parameter description.
   *
   * @return the parameter description, never {@code null}, may be empty
   */
  public String getDescription() {
    return m_description;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    var other = (PSExtensionMethodParam) obj;
    return Objects.equals(m_name, other.m_name)
        && Objects.equals(m_type, other.m_type)
        && Objects.equals(m_description, other.m_description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_name, m_type, m_description);
  }

  @Override
  public String toString() {
    return "PSExtensionMethodParam{"
        + "name='"
        + m_name
        + '\''
        + ", type='"
        + m_type
        + '\''
        + ", description='"
        + m_description
        + '\''
        + '}';
  }

  /**
   * Deserialize this parameter from its XML representation.
   *
   * @param source the XML element from which to deserialize this parameter,
   *    not {@code null}
   * @throws PSExtensionException for any error deserializing the supplied
   *    source
   */
  public void fromXML(Element source) throws PSExtensionException {
    Objects.requireNonNull(source, "source element cannot be null");

    try {
      setName(source.getAttribute("name"));
      setType(source.getAttribute("type"));
      setDescription(source.getAttribute("description"));
    } catch (Exception e) {
      throw new PSExtensionException("Failed to deserialize PSExtensionMethodParam", e);
    }
  }

  /**
   * Serialize this parameter to its XML representation.
   *
   * @param doc the document to use for creating new elements, not {@code null}
   * @return the XML element representing this parameter, never {@code null}
   * @throws IllegalArgumentException if doc is {@code null}
   */
  public Element toXML(Document doc) {
    Objects.requireNonNull(doc, "document cannot be null");

    var element = doc.createElement(XML_NAME);
    element.setAttribute("name", m_name);
    element.setAttribute("type", m_type);
    element.setAttribute("description", m_description);

    return element;
  }

  /**
   * The XML element name for this class.
   */
  public static final String XML_NAME = "PSExtensionMethodParam";
}
