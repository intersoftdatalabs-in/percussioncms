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
package com.percussion.deployer;

import java.util.Objects;

/**
 * The class to represent a source or target element in the element(id) mapping. Can be used as a
 * user object for table cell or elements in the list. It renders the element as
 * 'Name(id)-ParentName(id)' if it has a parent, otherwise 'Name(id)'.
 */
public class PSMappingElement implements Comparable<PSMappingElement> {

  /**
   * Constructs the mapping element with supplied parameters.
   *
   * @param type the element type, may not be {@code null} or empty.
   * @param id the element id, may not be {@code null} or empty.
   * @param name the element name, may not be {@code null} or empty.
   * @throws IllegalArgumentException if any parameter is invalid.
   */
  public PSMappingElement(String type, String id, String name) {
    this.m_type = requireNonEmpty(type, "type");
    this.m_id = requireNonEmpty(id, "id");
    this.m_name = requireNonEmpty(name, "name");
  }

  /**
   * Convenience constructor for derived classes to show an empty element in the combo-box. The
   * derived classes using this constructor should override all or required methods (at least {@code
   * equals()} and {@code toString()}) methods to fit its needs, otherwise using the base-class
   * methods results in {@code java.lang.NullPointerException}s.
   */
  protected PSMappingElement() {}

  /**
   * Sets the parent element of this element.
   *
   * @param type the parent type, may not be {@code null} or empty.
   * @param id the parent id, may not be {@code null} or empty.
   * @param name the parent name, may not be {@code null} or empty.
   * @throws IllegalArgumentException if any parameter is invalid.
   */
  public void setParent(String type, String id, String name) {
    this.m_parentType = requireNonEmpty(type, "type");
    this.m_parentId = requireNonEmpty(id, "id");
    this.m_parentName = requireNonEmpty(name, "name");
  }

  /**
   * Finds whether this element has parent or not.
   *
   * @return {@code true} if it has parent (if {@link #setParent(String, String, String)} is
   *     called), otherwise {@code false}
   */
  public boolean hasParent() {
    return m_parentId != null;
  }

  /**
   * Returns the id of this mapping element.
   *
   * @return the id, never <code>null</code>.
   */
  public String getId() {
    return m_id;
  }

  /**
   * Returns the name of this mapping element.
   *
   * @return the name, never <code>null</code>.
   */
  public String getName() {
    return m_name;
  }

  /**
   * Returns the type of this mapping element.
   *
   * @return the type, never <code>null</code>.
   */
  public String getType() {
    return m_type;
  }

  /**
   * Returns the parent id of this mapping element.
   *
   * @return the parent id, may be <code>null</code>.
   */
  public String getParentId() {
    return m_parentId;
  }

  /**
   * Returns the parent name of this mapping element.
   *
   * @return the parent name, may be <code>null</code>.
   */
  public String getParentName() {
    return m_parentName;
  }

  /**
   * Returns the parent type of this mapping element.
   *
   * @return the parent type, may be <code>null</code>.
   */
  public String getParentType() {
    return m_parentType;
  }

  /**
   * Gets the string representation of this element's parent in the form 'Name(id)' if it has a
   * parent.
   *
   * @return the string, may be {@code null} if it does not have a parent, never empty.
   */
  public String getParentDisplayString() {
    return hasParent() ? m_parentName + "(" + m_parentId + ")" : null;
  }

  /**
   * Gets the string representation of this element as 'Name(id)-ParentName(id)' if it has a parent,
   * otherwise as 'Name(id)'.
   *
   * @return the string, never {@code null} or empty.
   */
  @Override
  public String toString() {
    var displayString = m_name + "(" + m_id + ")";
    if (hasParent()) {
      displayString += "-" + m_parentName + "(" + m_parentId + ")";
    }
    return displayString;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PSMappingElement)) return false;
    var element = (PSMappingElement) obj;
    return Objects.equals(m_id, element.m_id)
        && Objects.equals(m_name, element.m_name)
        && Objects.equals(m_type, element.m_type)
        && Objects.equals(m_parentId, element.m_parentId)
        && Objects.equals(m_parentName, element.m_parentName)
        && Objects.equals(m_parentType, element.m_parentType);
  }

  /** Generates hash code corresponding to {@link #equals(Object)}. */
  @Override
  public int hashCode() {
    return Objects.hash(m_id, m_name, m_type, m_parentId, m_parentName, m_parentType);
  }

  /**
   * Compare's this object {@code toString()} representation lexicographically ignoring case.
   *
   * @throws IllegalArgumentException if obj is {@code null}
   */
  @Override
  public int compareTo(PSMappingElement obj) {
    Objects.requireNonNull(obj, "obj may not be null.");
    return toString().compareToIgnoreCase(obj.toString());
  }

  private static String requireNonEmpty(String value, String paramName) {
    if (value == null || value.trim().isEmpty())
      throw new IllegalArgumentException(paramName + " may not be null or empty.");
    return value;
  }

  // Fields are package-private for testability, but should be considered final after construction.
  private String m_id = null;
  private String m_name;
  private String m_type;
  private String m_parentId = null;
  private String m_parentName = null;
  private String m_parentType = null;
}
