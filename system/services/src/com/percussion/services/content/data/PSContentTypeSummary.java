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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single content type summary.
 *
 * <p>Design-object XML root is {@code content-type-summary}. Nested fields use {@code
 * field-description}; nested children use {@code content-type-summary-child} (issue #1921 / epic
 * #505).
 */
@JacksonXmlRootElement(localName = "content-type-summary")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"children", "description", "fields", "guid", "name"})
public class PSContentTypeSummary {
  /**
   * The name of the content type fo which this represents the summary. Initialized while
   * constructed, never <code>null</code> after that.
   */
  private String contentType;

  /** The content type description, may be <code>null</code>. */
  private String description;

  /**
   * The guid, initialized while constructed, never <code>null</code> after that.
   */
  private IPSGuid m_guid;

  /**
   * A list of fields for this content type, never <code>null</code>, may be empty.
   */
  private List<PSFieldDescription> fields = new ArrayList<>();

  /**
   * A list of children for this content type, never <code>null</code>, may be empty.
   */
  private List<PSContentTypeSummaryChild> children = new ArrayList<>();

  static {
    PSXmlSerializationHelper.addType("field-description", PSFieldDescription.class);
    PSXmlSerializationHelper.addType(
        "content-type-summary-child", PSContentTypeSummaryChild.class);
  }

  /**
   * Get the content type name.
   *
   * @return the content type name, may be <code>null</code>, never empty.
   */
  @JsonProperty
  public String getName() {
    return contentType;
  }

  /**
   * Set a new content type name.
   *
   * @param name the new content type name, not <code>null</code> or empty.
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException("contentType cannot be null or empty");

    this.contentType = name;
  }

  /**
   * Get the description for this content type.
   *
   * @return the content type description, may be <code>null</code> or empty.
   */
  @JsonProperty
  public String getDescription() {
    return description;
  }

  /**
   * Set a new content type description.
   *
   * @param description the new content type description, may be <code>null</code> or empty.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Get the list with all fields.
   *
   * @return the list with all fields, never <code>null</code>, may be empty.
   */
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "fields")
  @JacksonXmlProperty(localName = "field-description")
  public List<PSFieldDescription> getFields() {
    return fields;
  }

  /**
   * Set a new list of fields.
   *
   * @param fields the new list of fields, may be <code>null</code> or empty.
   */
  public void setFields(List<PSFieldDescription> fields) {
    if (fields == null) this.fields = new ArrayList<>();
    else this.fields = fields;
  }

  /**
   * Add a new field. Jackson uses {@link #setFields(List)}; this remains for API callers.
   *
   * @param field the new field to add, not <code>null</code>.
   */
  @JsonIgnore
  public void addField(PSFieldDescription field) {
    if (field == null) throw new IllegalArgumentException("field cannot be null");

    fields.add(field);
  }

  /**
   * Get the list with all children.
   *
   * @return the list with all children, never <code>null</code>, may be empty.
   */
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "children")
  @JacksonXmlProperty(localName = "content-type-summary-child")
  public List<PSContentTypeSummaryChild> getChildren() {
    return children;
  }

  /**
   * Set a new list of children.
   *
   * @param children the new list of children, may be <code>null</code> or empty.
   */
  public void setChildren(List<PSContentTypeSummaryChild> children) {
    if (children == null) this.children = new ArrayList<>();
    else this.children = children;
  }

  /**
   * Add a new child. Jackson uses {@link #setChildren(List)}; this remains for API callers.
   *
   * @param child the new child to add, not <code>null</code>.
   */
  @JsonIgnore
  public void addChild(PSContentTypeSummaryChild child) {
    if (child == null) throw new IllegalArgumentException("child cannot be null");

    children.add(child);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSContentTypeSummary)) return false;
    PSContentTypeSummary that = (PSContentTypeSummary) o;
    return Objects.equals(contentType, that.contentType)
        && Objects.equals(getDescription(), that.getDescription())
        && Objects.equals(m_guid, that.m_guid)
        && Objects.equals(getFields(), that.getFields())
        && Objects.equals(getChildren(), that.getChildren());
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentType, getDescription(), m_guid, getFields(), getChildren());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSContentTypeSummary{");
    sb.append("contentType='").append(contentType).append('\'');
    sb.append(", description='").append(description).append('\'');
    sb.append(", m_guid=").append(m_guid);
    sb.append(", fields=").append(fields);
    sb.append(", children=").append(children);
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

  /**
   * Get the guid of this object.
   *
   * @return The guid, never <code>null</code> if not set.
   */
  @JsonProperty
  public IPSGuid getGuid() {
    return m_guid;
  }

  /**
   * Set this object's guid
   *
   * @param guid The guid, may not be <code>null</code>.
   */
  public void setGuid(IPSGuid guid) {
    m_guid = guid;
  }
}
