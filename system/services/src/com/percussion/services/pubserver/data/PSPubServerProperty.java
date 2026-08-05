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
package com.percussion.services.pubserver.data;

import static com.percussion.services.pubserver.IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY;
import static com.percussion.util.PSBase64Decoder.decode;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.share.data.PSAbstractDataObject;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a single property for the server.
 *
 * <p>Nested package item element is {@code pub-server-property} (registered from {@link
 * PSPubServer}). Design XML exposes {@code name} and raw stored {@code value} (encrypted form for
 * password properties is the stored wire form; issue #1919 / epic #505).
 *
 * @author leonardohildt
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSPubServerProperty")
@Table(name = "PSX_PUBSERVER_PROPERTIES")
@JacksonXmlRootElement(localName = "pub-server-property")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"name", "value"})
public class PSPubServerProperty extends PSAbstractDataObject {
  private static final Logger log = LogManager.getLogger(PSPubServerProperty.class);

  /** */
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "SERVERPROPERTYID")
  private long propertyId = -1L;

  @Basic
  @Column(name = "PUBSERVERID")
  long serverId;

  @Basic
  @Column(name = "PROPERTYNAME")
  private String name;

  @Basic
  @Column(name = "PROPERTYVALUE")
  private String value;

  /** The default constructor. */
  public PSPubServerProperty() {}

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSPubServerProperty)) return false;

    // use "name" & "value" should be enough to avoid same pair more than once to make sure the
    // property names are unique within a PSPubServer
    PSPubServerProperty b = (PSPubServerProperty) obj;
    return new EqualsBuilder().append(name, b.name).append(value, b.value).isEquals();
  }

  @Override
  public int hashCode() {
    // use "name" should be enough to avoid same pair more than once to make sure the property names
    // are unique within a PSPubServer
    return new HashCodeBuilder().append(name).toHashCode();
  }

  /**
   * The database id for this object
   *
   * @return Returns the serverPropertyId, never <code>null</code> after persistence
   */
  @JsonIgnore
  @IPSXmlSerialization(suppress = true)
  public long getPropertyId() {
    return propertyId;
  }

  /**
   * @param propertyId The propertyId to set.
   */
  public void setPropertyId(long propertyId) {
    this.propertyId = propertyId;
  }

  /**
   * The server id for this object
   *
   * @return Returns the serverId, never <code>null</code> after persistence
   */
  @JsonIgnore
  @IPSXmlSerialization(suppress = true)
  public long getServerId() {
    return serverId;
  }

  /**
   * @param serverId The server id to set.
   */
  public void setServerId(long serverId) {
    this.serverId = serverId;
  }

  /**
   * Get the property name
   *
   * @return Returns the property name, never <code>null</code> or empty
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /**
   * @param name The name to set, never <code>null</code> or empty
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.name = name;
  }

  /**
   * Get the value (decrypted when this is a password property).
   *
   * @return Returns the value, may be <code>null</code> or empty.
   */
  @JsonIgnore
  @IPSXmlSerialization(suppress = true)
  public String getValue() {
    if (isEncodedProperty()) {
      try {

        return StringUtils.isEmpty(value)
            ? value
            : PSEncryptor.decryptString(
                PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), value);
      } catch (PSEncryptionException e) {
        return StringUtils.isEmpty(value) ? value : decode(value);
      }
    } else {
      return value;
    }
  }

  /**
   * Raw stored value for design-object XML (does not decrypt password properties).
   *
   * @return may be {@code null}
   */
  @JsonProperty("value")
  public String getValueXml() {
    return value;
  }

  /**
   * @param value The value to set (encrypts when this is a password property).
   */
  public void setValue(String value) {
    if (isEncodedProperty()) {
      try {
        String enc =
            PSEncryptor.encryptString(
                PSServer.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), value);
        this.value = StringUtils.isEmpty(value) ? value : enc;
      } catch (PSEncryptionException e) {
        log.error(
            "Unable to encrypt encoded property: {} Error: {}",
            this.name,
            PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        this.value = value;
      }
    } else {
      this.value = value;
    }
  }

  /**
   * Jackson setter for design-object XML {@code value} — stores the raw wire form without
   * re-encrypting (password properties are already encrypted on the wire).
   *
   * @param value may be {@code null}
   */
  @JsonProperty("value")
  public void setValueXml(String value) {
    this.value = value;
  }

  /**
   * Determines if the property value is encrypted.
   *
   * @return <code>true</code> if the value is encrypted; <code>false</code> otherwise.
   */
  private boolean isEncodedProperty() {
    return PUBLISH_PASSWORD_PROPERTY.equals(name);
  }
}
