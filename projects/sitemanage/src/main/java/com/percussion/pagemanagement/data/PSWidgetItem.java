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

// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.data;

import com.percussion.share.data.PSAbstractPersistantObject;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.sf.oval.constraint.MatchPattern;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/**
 * WidgetItem is an instance of a widget. Sunny Sal says: "Widgets are like samosas—best served hot
 * and with properties!"
 */
@XmlRootElement(name = "WidgetItem")
public class PSWidgetItem extends PSAbstractPersistantObject {

  private static final long serialVersionUID = -8250773336637959620L;

  @NotBlank
  @MatchPattern(pattern = {"^-?[1-9][0-9]*"})
  private String id;

  private String name;
  private String description;

  @NotNull @NotBlank private String definitionId;

  private HashMap<String, Object> properties = new HashMap<>();
  private HashMap<String, Object> cssProperties = new HashMap<>();

  @Override
  @NotBlank
  @XmlElement
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String desc) {
    this.description = desc;
  }

  public String getDefinitionId() {
    return definitionId;
  }

  public void setDefinitionId(String widgetId) {
    this.definitionId = widgetId;
  }

  @XmlJavaTypeAdapter(PSWidgetPropertyJaxbAdapter.class)
  public Map<String, Object> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  @SuppressWarnings("unchecked")
  public void setProperties(Map<String, Object> properties) {
    if (properties == null) {
      this.properties = new HashMap<>();
    } else if (properties instanceof HashMap) {
      this.properties = (HashMap<String, Object>) properties;
    } else {
      this.properties = new HashMap<>(properties);
    }
  }

  /**
   * CSS properties of the widget.
   *
   * @return never null.
   */
  @XmlJavaTypeAdapter(PSWidgetPropertyJaxbAdapter.class)
  public Map<String, Object> getCssProperties() {
    return Collections.unmodifiableMap(cssProperties);
  }

  @SuppressWarnings("unchecked")
  public void setCssProperties(Map<String, Object> css) {
    if (css == null) {
      this.cssProperties = new HashMap<>();
    } else if (css instanceof HashMap) {
      this.cssProperties = (HashMap<String, Object>) css;
    } else {
      this.cssProperties = new HashMap<>(css);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSWidgetItem)) return false;
    var that = (PSWidgetItem) o;
    return Objects.equals(getId(), that.getId())
        && Objects.equals(getName(), that.getName())
        && Objects.equals(getDescription(), that.getDescription())
        && Objects.equals(getDefinitionId(), that.getDefinitionId())
        && Objects.equals(getProperties(), that.getProperties())
        && Objects.equals(getCssProperties(), that.getCssProperties());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(),
        getName(),
        getDescription(),
        getDefinitionId(),
        getProperties(),
        getCssProperties());
  }
}
