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
package com.percussion.services.system.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single design object dependent.
 *
 * <p>Design-object XML root is {@code dependent}. Jackson opt-in property surface (issue #1993 /
 * epic #505). Identity is scalar {@code id} plus enum-name {@code type}; derived {@code
 * display-type} is omitted from the wire.
 */
@JacksonXmlRootElement(localName = "dependent")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"id", "type"})
public class PSDependent implements Serializable {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = -2436271056885991371L;

  /** The id of the dependent design object. */
  private long id;

  /**
   * The type of the dependent design object, may be <code>null</code>, not empty. Production
   * writers store {@link PSTypeEnum#name()} (e.g. {@code TEMPLATE}, {@code ITEM_FILTER}).
   */
  private String type;

  /** Default constructor. */
  public PSDependent() {}

  /**
   * Get the design object id for this dependent.
   *
   * @return the design object id for this dependent.
   */
  @JsonProperty
  public long getId() {
    return id;
  }

  /**
   * Set the new design object id for this dependent.
   *
   * @param id the new design object id for this dependent.
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Get the dependents type.
   *
   * @return the dependents type, may be <code>null</code>, not empty.
   */
  @JsonProperty
  public String getType() {
    return type;
  }

  /**
   * Get the dependents type in a form suitable for display.
   *
   * @return the dependents display type, may be <code>null</code>, not empty.
   */
  @JsonIgnore
  public String getDisplayType() {
    return PSTypeEnum.valueOf(type).getDisplayName();
  }

  /**
   * Set the dependents type.
   *
   * @param type the new type for this dependent, not <code>null</code> or empty.
   */
  public void setType(String type) {
    if (StringUtils.isBlank(type)) throw new IllegalArgumentException("type cannot be null or empty");

    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSDependent)) return false;
    PSDependent that = (PSDependent) o;
    return getId() == that.getId() && Objects.equals(getType(), that.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getType());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSDependent{");
    sb.append("id=").append(id);
    sb.append(", type='").append(type).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#fromXML(String)
   */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#toXML()
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }
}
