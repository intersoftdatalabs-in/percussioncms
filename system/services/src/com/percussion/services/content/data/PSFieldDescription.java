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
package com.percussion.services.content.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import java.io.IOException;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single field description.
 *
 * <p>Design-object XML root is {@code field-description}. Nested under content-type summaries via
 * package element {@code field-description} (issue #1921 / epic #505).
 */
@JacksonXmlRootElement(localName = "field-description")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"exportable", "name", "type"})
public class PSFieldDescription {
  /** The field name, may be <code>null</code>, never empty. */
  private String name;

  /** The field data type, may be <code>null</code>, never empty. */
  private String type;

  private Boolean exportable;

  /** Default constructor. */
  public PSFieldDescription() {}

  /**
   * Construct a new field description for the supplied parameters.
   *
   * @param name the name of the new field, not <code>null</code> or empty.
   * @param type the type of the new field, not <code>null</code> or empty.
   */
  public PSFieldDescription(String name, String type) {
    setName(name);
    setType(type);
  }

  public PSFieldDescription(String name, String type, Boolean exportable) {
    setName(name);
    setType(type);
    setExportable(exportable);
  }

  /**
   * Get the field name.
   *
   * @return the field name, may be <code>null</code>, never empty.
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /**
   * Set a new field name.
   *
   * @param name the new field name, not <code>null</code> or empty.
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException("name cannot be null or empty");

    this.name = name;
  }

  /**
   * Get the field data type.
   *
   * @return the field data type, may be <code>null</code>, never empty.
   */
  @JsonProperty
  public String getType() {
    return type;
  }

  /**
   * Set a new field data type.
   *
   * @param type the new field data type, not <code>null</code> or empty.
   */
  public void setType(String type) {
    if (StringUtils.isBlank(type))
      throw new IllegalArgumentException("type cannot be null or empty");

    this.type = PSFieldTypeEnum.valueOf(type).toString();
  }

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean getExportable() {
    return exportable;
  }

  public void setExportable(Boolean exportable) {
    this.exportable = exportable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSFieldDescription)) return false;
    PSFieldDescription that = (PSFieldDescription) o;
    return Objects.equals(getName(), that.getName())
        && Objects.equals(getType(), that.getType())
        && Objects.equals(getExportable(), that.getExportable());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getType(), getExportable());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSFieldDescription{");
    sb.append("name='").append(name).append('\'');
    sb.append(", type='").append(type).append('\'');
    sb.append(", exportable=").append(exportable);
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

  /** The field data type enumeration. */
  public enum PSFieldTypeEnum {
    TEXT,
    DATE,
    NUMBER,
    BINARY
  }
}
