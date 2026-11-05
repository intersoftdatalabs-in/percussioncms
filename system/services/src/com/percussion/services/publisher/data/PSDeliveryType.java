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
package com.percussion.services.publisher.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.publisher.IPSDeliveryType;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Persistent representation of a delivery type used by the publishing subsystem to define how
 * assembled content items are delivered to their destination.
 *
 * @see IPSDeliveryType
 *
 * <p>Design-object XML root is {@code delivery-type}. Jackson opt-in property surface (issue #1919 /
 * epic #505).
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSDeliveryType")
@Table(name = "PSX_DELIVERY_TYPE")
@JacksonXmlRootElement(localName = "delivery-type")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"beanName", "description", "guid", "name", "unpublishingRequiresAssembly"})
public class PSDeliveryType implements IPSDeliveryType {
  @Id long id;

  @Basic String name;

  @Basic String description;

  @Basic
  @Column(name = "BEAN_NAME")
  String beanName;

  @Basic
  @Column(name = "UNPUBLISHING_REQUIRES_ASSEMBLY")
  int unpublishingRequiresAssembly;

  /** The default constructor. */
  public PSDeliveryType() {}

  /**
   * @return the id
   */
  @JsonProperty("guid")
  public IPSGuid getGUID() {
    // Offline-safe assemble (avoid GuidManager locator in unit tests).
    return new PSGuid(PSTypeEnum.DELIVERY_TYPE, id);
  }

  /**
   * @param guid the guid to set, never <code>null</code>
   */
  public void setGUID(IPSGuid guid) {
    if (guid == null) {
      throw new IllegalArgumentException("guid may not be null");
    }
    this.id = guid.getUUID();
  }

  /*
   * //see base class method for details
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }

  /*
   * //see base class method for details
   */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /**
   * Get the name of the bean to be used when publishing. This name is used to look up a spring bean
   * on the publisher side of the delivery.
   *
   * @return the beanName, never <code>null</code> or empty.
   */
  @JsonProperty("bean-name")
  public String getBeanName() {
    return beanName;
  }

  /**
   * Set the bean name.
   *
   * @param beanName the beanName to set, never <code>null</code> or empty.
   */
  public void setBeanName(String beanName) {
    if (StringUtils.isBlank(beanName)) {
      throw new IllegalArgumentException("beanName may not be null or empty");
    }
    this.beanName = beanName;
  }

  @Override
  public void setBeanNameImpl(String beanName) {
    setBeanName(beanName);
  }

  @Override
  public void setNameImpl(String name) {
    setName(name);
  }

  /**
   * Get the description that describes this location.
   *
   * @return the description, can be <code>null</code> or empty.
   */
  @JsonProperty
  public String getDescription() {
    return description;
  }

  /**
   * Set the description.
   *
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Get the name of the delivery location.
   *
   * @return the name, never <code>null</code> or empty.
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /**
   * Set the name of the delivery location.
   *
   * @param name the name to set, never <code>null</code> or empty.
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.name = name;
  }

  /**
   * It determines if the item need to be unpublished.
   *
   * @return <code>true</code> if the item must be assembled for the unpublishing case; otherwise
   *     return <code>false</code>.
   */
  @JsonProperty("unpublishing-requires-assembly")
  public boolean isUnpublishingRequiresAssembly() {
    return unpublishingRequiresAssembly == 1;
  }

  /**
   * Set the value, see {@link #isUnpublishingRequiresAssembly()}.
   *
   * @param isUnpublishingRequiresAssembly the unpublishingRequiresAssembly to set.
   */
  public void setUnpublishingRequiresAssembly(boolean isUnpublishingRequiresAssembly) {
    this.unpublishingRequiresAssembly = isUnpublishingRequiresAssembly ? 1 : 0;
  }
}
