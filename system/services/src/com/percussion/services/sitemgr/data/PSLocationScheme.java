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
// REFACTORED: CP-JAVA11
package com.percussion.services.sitemgr.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import com.percussion.utils.xml.PSInvalidXmlException;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Location scheme data entity representing location generation rules for content publishing.
 *
 * <p>Design-object XML root is {@code location-scheme}. Nested parameters use element {@code
 * location-scheme-parameter} (registered via {@link PSXmlSerializationHelper#addType}). Jackson
 * opt-in surface (issue #1918 / #1892 / epic #505).
 *
 * @author dougrand (original)
 * @author Sunny Sal (Java 11 refactoring)
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSLocationScheme")
@Table(name = "RXLOCATIONSCHEME")
@JacksonXmlRootElement(localName = "location-scheme")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "contentTypeId",
  "contextId",
  "description",
  "generator",
  "guid",
  "id",
  "name",
  "parameterSet",
  "templateId"
})
public class PSLocationScheme implements IPSLocationScheme, IPSCatalogItem, IPSCatalogIdentifier {

  private static final long serialVersionUID = 1L;

  static {
    PSXmlSerializationHelper.addType(
        "location-scheme-parameter", PSLocationSchemeParameter.class);
  }

  @Id
  @Column(name = "SCHEMEID")
  private long schemeId = -1;

  @Basic
  @Column(name = "SCHEMENAME")
  private String name;

  @Basic
  @Column(name = "DESCRIPTION")
  private String description;

  @Basic
  @Column(name = "GENERATOR")
  private String generator;

  @Basic
  @Column(name = "VARIANTID")
  private Long templateId;

  @Basic
  @Column(name = "CONTENTTYPEID")
  private Long contentTypeId;

  @Basic
  @Column(name = "CONTEXTID")
  private Long contextId;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  @OneToMany(
      targetEntity = PSLocationSchemeParameter.class,
      cascade = {CascadeType.ALL},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @JoinColumn(name = "SCHEMEID", nullable = false)
  @Fetch(FetchMode.SUBSELECT)
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  private Set<PSLocationSchemeParameter> parameters = new HashSet<>();

  /** Cloning flag to track cloned instances */
  private transient boolean isCloned = false;

  /** Default constructor. */
  public PSLocationScheme() {
    // Default constructor
  }

  /**
   * Enhanced copy constructor using Java 11 patterns
   */
  public PSLocationScheme(PSLocationScheme source) {
    Objects.requireNonNull(source, "source cannot be null");

    this.name = source.name;
    this.description = source.description;
    this.generator = source.generator;
    this.templateId = source.templateId;
    this.contentTypeId = source.contentTypeId;
    this.contextId = source.contextId;
    this.version = source.version;
    this.isCloned = true;

    // Deep copy parameters using streams
    this.parameters = new HashSet<>();
    source.parameters.stream()
        .map(
            p -> {
              PSLocationSchemeParameter copy = new PSLocationSchemeParameter();
              copy.setName(p.getName());
              copy.setType(p.getType());
              copy.setValue(p.getValue());
              copy.setSequence(p.getSequence());
              if (p.getParameterId() != null) {
                copy.setParameterId(p.getParameterId());
              }
              copy.setScheme(this);
              return copy;
            })
        .forEach(this.parameters::add);
  }

  @Override
  @JsonProperty("guid")
  public IPSGuid getGUID() {
    return schemeId == -1 ? null : new PSGuid(PSTypeEnum.LOCATION_SCHEME, schemeId);
  }

  @Override
  public void setGUID(IPSGuid guid) throws IllegalStateException {
    Objects.requireNonNull(guid, "guid cannot be null");
    if (guid.getType() != PSTypeEnum.LOCATION_SCHEME.getOrdinal()) {
      throw new IllegalArgumentException("guid must be a location scheme guid");
    }
    // Allow overwrite on design-object XML restore (BeanUtils + Jackson)
    this.schemeId = guid.longValue();
  }

  @JsonProperty
  public long getId() {
    return schemeId;
  }

  public void setId(long id) {
    this.schemeId = id;
  }

  @Override
  @JsonProperty
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @JsonProperty
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  @JsonProperty
  public String getGenerator() {
    return generator;
  }

  @Override
  public void setGenerator(String generator) {
    this.generator = generator;
  }

  @Override
  @JsonProperty
  public Long getTemplateId() {
    return templateId;
  }

  @Override
  public void setTemplateId(Long templateId) {
    this.templateId = templateId;
  }

  @Override
  @JsonProperty
  public Long getContentTypeId() {
    return contentTypeId;
  }

  @Override
  public void setContentTypeId(Long contentTypeId) {
    this.contentTypeId = contentTypeId;
  }

  @Override
  @JsonProperty
  public IPSGuid getContextId() {
    return Optional.ofNullable(contextId)
        .map(id -> new PSGuid(PSTypeEnum.CONTEXT, id))
        .orElse(null);
  }

  @Override
  public void setContextId(IPSGuid contextId) {
    this.contextId = Optional.ofNullable(contextId).map(IPSGuid::longValue).orElse(null);
  }

  /**
   * Parameter set for design-object XML and persistence access.
   *
   * @return a mutable copy of parameters, never null
   */
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "parameter-set")
  @JacksonXmlProperty(localName = "location-scheme-parameter")
  public Set<PSLocationSchemeParameter> getParameterSet() {
    // LinkedHashSet preserves sequence order for deterministic design-object XML / goldens
    return parameters.stream()
        .sorted(
            (a, b) ->
                Integer.compare(
                    a.getSequence() != null ? a.getSequence() : 0,
                    b.getSequence() != null ? b.getSequence() : 0))
        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
  }

  public void setParameterSet(Set<PSLocationSchemeParameter> parameters) {
    this.parameters = Optional.ofNullable(parameters).map(HashSet::new).orElse(new HashSet<>());
    // Re-link parent after XML restore (circular scheme suppressed on write)
    for (PSLocationSchemeParameter p : this.parameters) {
      if (p != null) {
        p.setScheme(this);
      }
    }
  }

  /**
   * Enhanced parameter management using Java 11 patterns
   */
  @JsonIgnore
  public String getParameter(String name) {
    Objects.requireNonNull(name, "name cannot be null");

    return parameters.stream()
        .filter(param -> Objects.equals(param.getName(), name))
        .map(PSLocationSchemeParameter::getValue)
        .findFirst()
        .orElse(null);
  }

  @JsonIgnore
  public void setParameter(String name, String value) {
    Objects.requireNonNull(name, "name cannot be null");

    // Remove existing parameter with same name
    parameters.removeIf(param -> Objects.equals(param.getName(), name));

    // Add new parameter if value is not blank
    if (StringUtils.isNotBlank(value)) {
      var parameter = new PSLocationSchemeParameter();
      parameter.setName(name);
      parameter.setValue(value);
      parameter.setScheme(this);
      parameters.add(parameter);
    }
  }

  @Override
  @JsonIgnore
  public java.util.List<String> getParameterNames() {
    return parameters.stream()
        .sorted((a, b) -> Integer.compare(a.getSequence(), b.getSequence()))
        .map(PSLocationSchemeParameter::getName)
        .collect(java.util.stream.Collectors.toList());
  }

  @Override
  @JsonIgnore
  public String getParameterValue(String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return parameters.stream()
        .filter(p -> Objects.equals(p.getName(), name))
        .map(PSLocationSchemeParameter::getValue)
        .findFirst()
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public String getParameterType(String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return parameters.stream()
        .filter(p -> Objects.equals(p.getName(), name))
        .map(PSLocationSchemeParameter::getType)
        .findFirst()
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public Integer getParameterSequence(String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return parameters.stream()
        .filter(p -> Objects.equals(p.getName(), name))
        .map(PSLocationSchemeParameter::getSequence)
        .findFirst()
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public void setParameter(String name, String type, String value) {
    addParameter(name, 0, type, value);
  }

  @Override
  public void addParameter(String name, int sequence, String type, String value) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(value, "value cannot be null");

    // If exists, update
    for (PSLocationSchemeParameter p : parameters) {
      if (Objects.equals(p.getName(), name)) {
        p.setType(type);
        p.setValue(value);
        p.setSequence(sequence);
        return;
      }
    }

    PSLocationSchemeParameter param = new PSLocationSchemeParameter();
    param.setName(name);
    param.setType(type);
    param.setValue(value);
    param.setSequence(sequence);
    param.setScheme(this);
    parameters.add(param);
  }

  @Override
  public void removeParameter(String name) {
    Objects.requireNonNull(name, "name cannot be null");
    parameters.removeIf(p -> Objects.equals(p.getName(), name));
  }

  /**
   * Check if this is a cloned instance
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public boolean isCloned() {
    return isCloned;
  }

  /**
   * Mark this instance as cloned
   */
  public void setCloned(boolean cloned) {
    this.isCloned = cloned;
  }

  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    var scheme = (PSLocationScheme) obj;
    return new EqualsBuilder()
        .append(schemeId, scheme.schemeId)
        .append(name, scheme.name)
        .append(generator, scheme.generator)
        .append(templateId, scheme.templateId)
        .append(contentTypeId, scheme.contentTypeId)
        .append(contextId, scheme.contextId)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(schemeId)
        .append(name)
        .append(generator)
        .append(templateId)
        .append(contentTypeId)
        .append(contextId)
        .toHashCode();
  }

  @Override
  public void copy(IPSLocationScheme other) {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    this.name = other.getName();
    this.description = other.getDescription();
    this.generator = other.getGenerator();
    this.templateId = other.getTemplateId();
    this.contentTypeId = other.getContentTypeId();
    this.contextId =
        java.util.Optional.ofNullable(other.getContextId())
            .map(com.percussion.utils.guid.IPSGuid::longValue)
            .orElse(null);
    // Copy parameters
    this.parameters.clear();
    for (String paramName : other.getParameterNames()) {
      this.addParameter(
          paramName,
          other.getParameterSequence(paramName),
          other.getParameterType(paramName),
          other.getParameterValue(paramName));
    }
  }

  @Override
  @Deprecated
  @JsonIgnore
  public void setContext(IPSPublishingContext context) {
    // Only apply non-null context objects. BeanUtils property-copy after Jackson deserialize
    // may invoke setContext(null) for the suppressed property and must not wipe contextId
    // already restored via setContextId (issue #1918).
    if (context != null) {
      this.setContextId(context.getGUID());
    }
  }

  @Override
  @Deprecated
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public IPSPublishingContext getContext() {
    // Deprecated API; prefer using getContextId(). Returning null as a
    // minimal implementation to preserve backward compatibility.
    return null;
  }

  @Override
  public void fromXML(String xmlsource)
      throws java.io.IOException, org.xml.sax.SAXException, PSInvalidXmlException {
    // Reset identifier and version to avoid restore issues
    this.schemeId = -1;
    this.version = null;
    this.parameters = new HashSet<>();
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  @Override
  public String toXML() throws java.io.IOException, org.xml.sax.SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    // Create a deep copy using copy constructor
    return new PSLocationScheme(this);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("schemeId", schemeId)
        .append("name", name)
        .append("generator", generator)
        .append("templateId", templateId)
        .append("contentTypeId", contentTypeId)
        .append("contextId", contextId)
        .toString();
  }
}
