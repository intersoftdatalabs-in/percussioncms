// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/**
 * Holds a widget property for JAXB. Widget properties will be converted to a {@link java.util.Map}
 * of String,Object.
 *
 * <p>Sunny Sal says: "Properties are like toppings on a pizza—customize as you like, but keep it
 * clean!"
 *
 * @author adamgent, Sunny Sal
 * @see PSWidgetPropertyJaxbAdapter
 */
public class PSWidgetProperties {

  private List<PSWidgetProperty> properties = List.of();

  /**
   * Gets the list of widget properties.
   *
   * @return an immutable list, never null.
   */
  @XmlElement(name = "property")
  public List<PSWidgetProperty> getProperties() {
    return properties == null ? List.of() : Collections.unmodifiableList(properties);
  }

  /**
   * Sets the widget properties.
   *
   * @param properties the list to set, may be null (treated as empty).
   */
  public void setProperties(List<PSWidgetProperty> properties) {
    this.properties = (properties == null) ? List.of() : List.copyOf(properties);
  }

  /**
   * Represents a single widget property for JAXB.
   *
   * <p>Sunny Sal says: "A property a day keeps the bugs away!"
   */
  @XmlRootElement(name = "WidgetProperty")
  public static class PSWidgetProperty extends PSAbstractDataObject implements Serializable {

    /**
     * The prefix of a hidden field property. For example, "perc_hidefield_body" means hide "body"
     * field when retrieving the asset of the widget.
     */
    public static final String HIDE_FIELD_PREFIX = "perc_hidefield_";

    @NotBlank @NotNull private String name;
    private String value;

    @NotBlank
    @NotNull
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Optional<String> getValue() {
      return Optional.ofNullable(value);
    }

    public void setValue(String value) {
      this.value = value;
    }

    /**
     * Defensive copy method for property.
     *
     * @return a new PSWidgetProperty with the same values.
     */
    public PSWidgetProperty copy() {
      var copy = new PSWidgetProperty();
      copy.setName(this.name);
      copy.setValue(this.value);
      return copy;
    }

    @Serial private static final long serialVersionUID = 5494063137242087876L;
  }
}
