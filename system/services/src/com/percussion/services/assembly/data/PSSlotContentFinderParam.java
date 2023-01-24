/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.assembly.data;


import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.Objects;

/**
 * Data class for parameters that are associated with a given slot definition
 * and supplied to a slot content finder to customize its behavior.
 * 
 * @author dougrand
 */
@Entity
@NaturalIdCache
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSSlotContentFinderParam" )
@Table(name = "PSX_SLOT_FINDER_PARAM" )
public class PSSlotContentFinderParam
{
   @Id
   @GenericGenerator(name = "id", strategy = "com.percussion.data.utils.PSGuidHibernateGenerator")
   @GeneratedValue(generator = "id")
   @Column(name = "PARAM_ID", nullable = false)
   private long id;
   
   @SuppressWarnings("unused")
   @Version
   private Integer version;
   
   @Basic
   @NaturalId(mutable=true)
   @Column(name = "NAME", unique=true)
   private String name;
   
   @Basic
   private String value;
   
   @ManyToOne(targetEntity = PSTemplateSlot.class)
   @JoinColumn(name = "SLOTID", nullable=false, insertable=false, updatable=false)
   private PSTemplateSlot containingSlot;

   public PSSlotContentFinderParam(PSTemplateSlot psTemplateSlot, String n, String value) {
      this.setContainingSlot(psTemplateSlot);
      this.setName(n);
      this.setValue(value);
   }

   public PSSlotContentFinderParam() {
   }

   /**
    * @return Returns the containingSlot.
    */
   public PSTemplateSlot getContainingSlot()
   {
      return containingSlot;
   }

   /**
    * @param containingSlot The containingSlot to set.
    */
   public void setContainingSlot(PSTemplateSlot containingSlot)
   {
      this.containingSlot = containingSlot;
   }

   /**
    * @return Returns the id.
    */
   public long getId()
   {
      return id;
   }

   /**
    * @param id The id to set.
    */
   public void setId(long id)
   {
      this.id = id;
   }

   /**
    * @return Returns the name.
    */
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

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSSlotContentFinderParam)) return false;
      PSSlotContentFinderParam param = (PSSlotContentFinderParam) o;
      return Objects.equals(name, param.name);
   }

   @Override
   public int hashCode() {
      return Objects.hash(name);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE).append("name", name)
      .append("value", value).toString();
   }
   
   
}
