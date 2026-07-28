/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.utils.properties;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifies a logical grouping of properties.
 *
 * @author natechadwick
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "propertygroup")
public class PSPropertyGroupDefinition {

  /** Default constructor. */
  public PSPropertyGroupDefinition() {}

  /** The XML attribute name identifying this property group. */
  @XmlAttribute(required = true)
  private String name;

  /** The display label of this property group in the UI. */
  @XmlAttribute(name = "display_name")
  private String displayName;

  /** Whether the property group should be initially expanded in the UI. */
  @XmlAttribute private boolean expanded;

  /** The help text shown alongside the property group in the UI. */
  @XmlAttribute(name = "help_text")
  private String helpText;

  /** The ordered list of properties contained in this group. */
  @XmlElement private List<PSPropertyDefinition> properties;

  /**
   * When true, the last known state for this property group is expanded.
   *
   * @return <code>true</code> when the group is expanded by default.
   */
  public boolean isExpanded() {
    return expanded;
  }

  /**
   * When true, indicates that this property group should be displayed expanded. When false
   * collapsed.
   *
   * @param expanded <code>true</code> to render the group expanded, <code>false</code> to render
   *     it collapsed.
   */
  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }

  /**
   * Returns the XML attribute name of this property group.
   *
   * @return the group name, never <code>null</code>.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the XML attribute name of this property group.
   *
   * @param name the group name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the help text shown alongside the property group in the UI.
   *
   * @return the help text, may be <code>null</code>.
   */
  public String getHelpText() {
    return helpText;
  }

  /**
   * Sets the help text shown alongside the property group in the UI.
   *
   * @param helpText the help text.
   */
  public void setHelpText(String helpText) {
    this.helpText = helpText;
  }

  /**
   * Returns the display label for this property group.
   *
   * @return the display name, may be <code>null</code>.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the display label for this property group.
   *
   * @param displayName the display name.
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Returns the ordered list of properties contained in this group.
   *
   * @return the live property list; never <code>null</code>, may be empty.
   */
  public List<PSPropertyDefinition> getProperties() {
    if (properties == null) properties = new ArrayList<>();
    return properties;
  }

  /**
   * Sets the ordered list of properties contained in this group.
   *
   * @param properties the property list; may be <code>null</code> in which case an empty list is
   *     substituted on the next call to {@link #getProperties()}.
   */
  public void setProperties(List<PSPropertyDefinition> properties) {
    this.properties = properties;
  }
}
