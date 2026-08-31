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
package com.percussion.services.content.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a single auto translation definition.
 *
 * <p>Design-object XML root is {@code auto-translation} (PS/IPS strip + hyphenation). Jackson
 * opt-in surface suppresses catalog aliases, version, composite key, and {@link Optional} accessors
 * (issue #1921 / epic #505).
 *
 * @since Java 11 Modernization
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSAutoTranslation")
@Table(name = "PSX_AUTOTRANSLATION")
@IdClass(PSAutoTranslationPK.class)
@JacksonXmlRootElement(localName = "auto-translation")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "communityId",
  "communityName",
  "contentTypeId",
  "contentTypeName",
  "locale",
  "workflowId",
  "workflowName"
})
public class PSAutoTranslation implements IPSCatalogSummary, Serializable, IPSCatalogItem {

  /** Serial version UID for serialization compatibility. */
  private static final long serialVersionUID = -7673907948835742673L;

  @Id
  @Column(name = "CONTENTTYPEID", nullable = false)
  private long contentTypeId;

  /**
   * The content type name for this auto translation, initialized while constructed, never {@code
   * null} or empty.
   */
  @Transient private String contentTypeName;

  @Basic
  @Column(name = "WORKFLOWID", nullable = false)
  private long workflowId;

  /**
   * The workflow name for this auto translation, initialized while constructed, never {@code null}
   * or empty.
   */
  @Transient private String workflowName;

  @Basic
  @Column(name = "COMMUNITYID", nullable = false)
  private long communityId;

  /**
   * The community name for this auto translation, initialized while constructed, never {@code null}
   * or empty.
   */
  @Transient private String communityName;

  /**
   * The locale code for this auto translation, initialized while constructed, never {@code null} or
   * empty.
   */
  @Id
  @Column(name = "LOCALE", nullable = false)
  private String locale;

  @Version
  @Column(name = "VERSION")
  private Integer m_version = null;

  /**
   * Default constructor required for JPA/Hibernate.
   *
   * <p>Use factory methods or parameterized constructors for creating new instances.
   */
  public PSAutoTranslation() {
    // Required by JPA
  }

  /**
   * Create a new auto translation with specified parameters using modern validation.
   *
   * @param contentTypeId the content type ID, must be &gt; 0
   * @param locale the locale code, not {@code null} or empty
   * @param workflowId the workflow ID, must be &gt; 0
   * @param communityId the community ID, must be &gt; 0
   * @throws IllegalArgumentException if validation fails
   */
  public PSAutoTranslation(long contentTypeId, String locale, long workflowId, long communityId) {
    if (contentTypeId <= 0) {
      throw new IllegalArgumentException("contentTypeId must be > 0");
    }
    if (StringUtils.isBlank(locale)) {
      throw new IllegalArgumentException("locale cannot be null or empty");
    }
    if (workflowId <= 0) {
      throw new IllegalArgumentException("workflowId must be > 0");
    }
    if (communityId <= 0) {
      throw new IllegalArgumentException("communityId must be > 0");
    }

    this.contentTypeId = contentTypeId;
    this.locale = locale;
    this.workflowId = workflowId;
    this.communityId = communityId;
  }

  /**
   * Create a new auto translation using factory method with enhanced validation.
   *
   * @param contentTypeId the content type ID, must be &gt; 0
   * @param locale the locale code, not {@code null} or empty
   * @param workflowId the workflow ID, must be &gt; 0
   * @param communityId the community ID, must be &gt; 0
   * @return a new PSAutoTranslation instance
   * @throws IllegalArgumentException if validation fails
   */
  public static PSAutoTranslation of(
      long contentTypeId, String locale, long workflowId, long communityId) {
    return new PSAutoTranslation(contentTypeId, locale, workflowId, communityId);
  }

  /**
   * All auto translations are represented by a single GUID for locking purposes.
   *
   * @return the GUID, never {@code null}
   */
  public static IPSGuid getAutoTranslationsGUID() {
    return new PSGuid(PSTypeEnum.AUTO_TRANSLATIONS, 0);
  }

  /**
   * Whether {@code id} is the singleton dummy GUID used to lock/load the full
   * auto-translation set (type {@link PSTypeEnum#AUTO_TRANSLATIONS}, uuid {@code 0}).
   */
  public static boolean isAutoTranslationsSetGuid(IPSGuid id) {
    return id != null
        && id.getType() == PSTypeEnum.AUTO_TRANSLATIONS.getOrdinal()
        && id.getUUID() == 0;
  }

  /**
   * Content-type id as stored on legacy {@code PSX_AUTOTRANSLATION} rows (UUID /
   * lower 32 bits), so typed GUIDs match raw ids such as {@code 1033}.
   */
  public static long persistentContentTypeId(long contentTypeId) {
    return contentTypeId & 0xFFFFFFFFL;
  }

  /**
   * Get the key representation of this object.
   *
   * @return the key, never {@code null}
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public PSAutoTranslationPK getKey() {
    return new PSAutoTranslationPK(contentTypeId, locale);
  }

  /**
   * Composite key using {@link #persistentContentTypeId(long)} so UUID and typed
   * long values of the same content type collide as one row.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public PSAutoTranslationPK getPersistentKey() {
    return new PSAutoTranslationPK(persistentContentTypeId(contentTypeId), locale);
  }

  /**
   * Get the GUID of this object, always the result of {@link #getAutoTranslationsGUID()}.
   *
   * @return the GUID, never {@code null}
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public IPSGuid getGUID() {
    return getAutoTranslationsGUID();
  }

  /**
   * Get the community ID with Optional wrapper for safer access.
   *
   * @return Optional containing the community ID if valid, empty otherwise
   */
  @JsonIgnore
  public Optional<Long> getCommunityIdOptional() {
    return communityId > 0 ? Optional.of(communityId) : Optional.empty();
  }

  /**
   * Get the community ID of this auto translation.
   *
   * @return the community ID
   */
  @JsonProperty
  public long getCommunityId() {
    return communityId;
  }

  /**
   * Set the community ID with enhanced validation.
   *
   * @param communityId the community ID, must be &gt; 0
   * @throws IllegalArgumentException if communityId is &lt;= 0
   */
  public void setCommunityId(long communityId) {
    if (communityId <= 0) {
      throw new IllegalArgumentException("communityId must be > 0");
    }
    this.communityId = communityId;
  }

  /**
   * Get the community name with Optional wrapper.
   *
   * @return Optional containing the community name if non-null, empty otherwise
   */
  @JsonIgnore
  public Optional<String> getCommunityNameOptional() {
    return Optional.ofNullable(communityName);
  }

  /**
   * Get the community name of this auto translation.
   *
   * @return the community name, may be {@code null}
   */
  @JsonProperty
  public String getCommunityName() {
    return communityName;
  }

  /**
   * Set the community name.
   *
   * @param communityName the community name, may be {@code null}
   */
  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }

  /**
   * Get the content type ID with Optional wrapper for safer access.
   *
   * @return Optional containing the content type ID if valid, empty otherwise
   */
  @JsonIgnore
  public Optional<Long> getContentTypeIdOptional() {
    return contentTypeId > 0 ? Optional.of(contentTypeId) : Optional.empty();
  }

  /**
   * Get the content type ID of this auto translation.
   *
   * @return the content type ID
   */
  @JsonProperty
  public long getContentTypeId() {
    return contentTypeId;
  }

  /**
   * Set the content type ID with enhanced validation.
   *
   * @param contentTypeId the content type ID, must be &gt; 0
   * @throws IllegalArgumentException if contentTypeId is &lt;= 0
   */
  public void setContentTypeId(long contentTypeId) {
    if (contentTypeId <= 0) {
      throw new IllegalArgumentException("contentTypeId must be > 0");
    }
    this.contentTypeId = contentTypeId;
  }

  /**
   * Get the content type name with Optional wrapper.
   *
   * @return Optional containing the content type name if non-null, empty otherwise
   */
  @JsonIgnore
  public Optional<String> getContentTypeNameOptional() {
    return Optional.ofNullable(contentTypeName);
  }

  /**
   * Get the content type name of this auto translation.
   *
   * @return the content type name, may be {@code null}
   */
  @JsonProperty
  public String getContentTypeName() {
    return contentTypeName;
  }

  /**
   * Set the content type name.
   *
   * @param contentTypeName the content type name, may be {@code null}
   */
  public void setContentTypeName(String contentTypeName) {
    this.contentTypeName = contentTypeName;
  }

  /**
   * Get the locale with Optional wrapper.
   *
   * @return Optional containing the locale if non-null, empty otherwise
   */
  @JsonIgnore
  public Optional<String> getLocaleOptional() {
    return Optional.ofNullable(locale);
  }

  /**
   * Get the locale of this auto translation.
   *
   * @return the locale, never {@code null} or empty
   */
  @JsonProperty
  public String getLocale() {
    return locale;
  }

  /**
   * Set the locale with enhanced validation.
   *
   * @param locale the locale, not {@code null} or empty
   * @throws IllegalArgumentException if locale is null or empty
   */
  public void setLocale(String locale) {
    if (StringUtils.isBlank(locale)) {
      throw new IllegalArgumentException("locale cannot be null or empty");
    }
    this.locale = locale;
  }

  /**
   * Get the workflow ID with Optional wrapper for safer access.
   *
   * @return Optional containing the workflow ID if valid, empty otherwise
   */
  @JsonIgnore
  public Optional<Long> getWorkflowIdOptional() {
    return workflowId > 0 ? Optional.of(workflowId) : Optional.empty();
  }

  /**
   * Get the workflow ID of this auto translation.
   *
   * @return the workflow ID
   */
  @JsonProperty
  public long getWorkflowId() {
    return workflowId;
  }

  /**
   * Set the workflow ID with enhanced validation.
   *
   * @param workflowId the workflow ID, must be &gt; 0
   * @throws IllegalArgumentException if workflowId is &lt;= 0
   */
  public void setWorkflowId(long workflowId) {
    if (workflowId <= 0) {
      throw new IllegalArgumentException("workflowId must be > 0");
    }
    this.workflowId = workflowId;
  }

  /**
   * Get the optimistic concurrency version for this auto translation.
   *
   * @return the version, may be {@code null}
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public Integer getVersion() {
    return this.m_version;
  }

  /**
   * Set the optimistic concurrency version for this auto translation.
   *
   * @param version the version to set
   */
  public void setVersion(Integer version) {
    this.m_version = version;
  }

  /**
   * Get the workflow name with Optional wrapper.
   *
   * @return Optional containing the workflow name if non-null, empty otherwise
   */
  @JsonIgnore
  public Optional<String> getWorkflowNameOptional() {
    return Optional.ofNullable(workflowName);
  }

  /**
   * Get the workflow name of this auto translation.
   *
   * @return the workflow name, may be {@code null}
   */
  @JsonProperty
  public String getWorkflowName() {
    return workflowName;
  }

  /**
   * Set the workflow name.
   *
   * @param workflowName the workflow name, may be {@code null}
   */
  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PSAutoTranslation)) return false;

    var other = (PSAutoTranslation) obj;
    return new EqualsBuilder()
        .append(contentTypeId, other.contentTypeId)
        .append(locale, other.locale)
        .append(workflowId, other.workflowId)
        .append(communityId, other.communityId)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(contentTypeId)
        .append(locale)
        .append(workflowId)
        .append(communityId)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("contentTypeId", contentTypeId)
        .append("locale", locale)
        .append("workflowId", workflowId)
        .append("communityId", communityId)
        .append("contentTypeName", contentTypeName)
        .append("workflowName", workflowName)
        .append("communityName", communityName)
        .toString();
  }

  /* (non-Javadoc)
   * @see com.percussion.services.catalog.IPSCatalogSummary#getName()
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public String getName() {
    return communityName + "-" + contentTypeName + "-" + workflowName + "-" + locale;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.catalog.IPSCatalogSummary#getLabel()
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public String getLabel() {
    return getName();
  }

  /* (non-Javadoc)
   * @see com.percussion.services.catalog.IPSCatalogSummary#getDescription()
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public String getDescription() {
    return getName();
  }

  // see base class
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }

  // see base class
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  // see base class
  public void setGUID(IPSGuid newguid) throws IllegalStateException {
    return;
  }
}
