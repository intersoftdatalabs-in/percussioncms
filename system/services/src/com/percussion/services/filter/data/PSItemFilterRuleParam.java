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
package com.percussion.services.filter.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidHelper;
import com.percussion.utils.xml.IPSXmlSerialization;
import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a single parameter for the rule definition.
 *
 * <p>Registered under package element name {@code parameters} via {@code
 * PSXmlSerializationHelper.addType}. Production package {@code *.filterDef} wire typically uses the
 * parent {@code params} string map instead of nested parameter beans.
 *
 * @author dougrand
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE,
      region = "PSItemFilterRuleParam")
@Table(name = "PSX_ITEM_FILTER_RULE_PARAM")
@JacksonXmlRootElement(localName = "parameters")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"id", "name", "value"})
public class PSItemFilterRuleParam implements Serializable
{
   /**
    * Serial id identifies versions of serialized data
    */
   private static final long serialVersionUID = 1L;

   /**
    * Primary key
    */
   @Id
   @Column(name = "FILTER_RULE_PARAM_ID")
   Long id;

   /**
    * Hibernate version column
    */
   @Version
   @Column(name = "VERSION", nullable = false)
   Integer version = 0;

   /**
    * The name for the given parameter
    */
   @Basic
   @Column(name = "NAME", nullable = false)
   String name;

   /**
    * The value for the given parameter
    */
   @Basic
   @Column(name = "VALUE", nullable = false)
   String value;

   /**
    * The parent rule definition that this parameter is associated with
    */
   @ManyToOne(targetEntity = PSItemFilterRuleDef.class)
   @JoinColumn(name = "FILTER_RULE_ID", nullable = false, insertable = false, updatable = false)
   PSItemFilterRuleDef ruleDef;

   /**
    * Tracks client-side construction semantics where id generation is deferred.
    */
   @Transient
   private transient boolean clientSide;

   /**
    * Default ctor
    */
   public PSItemFilterRuleParam()
   {
      // Required by Hibernate. Avoid service lookups during entity bootstrap.
   }

   public PSItemFilterRuleParam(boolean clientSide)
   {
      this.clientSide = clientSide;
      if(!clientSide)
      {
         initializeIdIfNeeded(true);
      }
   }

   /**
    * Assign an identifier right before insert if one was not explicitly set.
    */
   @PrePersist
   protected void ensureIdForPersist()
   {
      initializeIdIfNeeded(true);
   }

   private void initializeIdIfNeeded(boolean includeClientSide)
   {
      if (id == null && (includeClientSide || !clientSide))
      {
         id = PSGuidHelper.generateNextLong(PSTypeEnum.INTERNAL);
      }
   }

   /**
    * @return Returns the id.
    */
   @JsonProperty
   public Long getId()
   {
      initializeIdIfNeeded(false);
      return id;
   }

   /**
    * @param id primary key, may be {@code null} before persist
    */
   public void setId(Long id)
   {
      this.id = id;
   }

   /**
    * @return Returns the name.
    */
   @JsonProperty
   public String getName()
   {
      return name;
   }

   /**
    * @param name The name to set.
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return Returns the value.
    */
   @JsonProperty
   public String getValue()
   {
      return value;
   }

   /**
    * @param value The value to set.
    */
   public void setValue(String value)
   {
      this.value = value;
   }

   /**
    * @return Returns the ruleDef.
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public PSItemFilterRuleDef getRuleDef()
   {
      return ruleDef;
   }

   /**
    * @param ruleDef The ruleDef to set.
    */
   @JsonIgnore
   public void setRuleDef(PSItemFilterRuleDef ruleDef)
   {
      this.ruleDef = ruleDef;
   }

   /**
    * Hibernate version — not part of design-object XML wire.
    *
    * @return version, may be {@code null}
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Integer getVersion()
   {
      return version;
   }

   @JsonIgnore
   public void setVersion(Integer version)
   {
      this.version = version;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSItemFilterRuleParam)) return false;
      PSItemFilterRuleParam that = (PSItemFilterRuleParam) o;
      return Objects.equals(name, that.name) &&
              Objects.equals(ruleDef, that.ruleDef);
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, ruleDef);
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSItemFilterRuleParam{");
      sb.append("id=").append(id);
      sb.append(", version=").append(version);
      sb.append(", name='").append(name).append('\'');
      sb.append(", value='").append(value).append('\'');
      sb.append(", ruleDef=").append(ruleDef);
      sb.append('}');
      return sb.toString();
   }
}
