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
package com.percussion.services.publisher.data;

// Generated Dec 16, 2005 4:46:50 PM by Hibernate Tools 3.1.0 beta1JBIDERC2

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.IOException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Persistent representation of a publishing edition, capturing the configuration of a single
 * publishing unit-of-work: site, delivery type, content lists, and assembly / publish
 * parameters.
 *
 * @see IPSEdition
 *
 * <p>Design-object XML root is {@code edition}. Jackson opt-in property surface (issue #1919 / epic
 * #505). Historical Betwixt suppressed {@code guid}; identity uses {@code id}. {@code name} is an
 * alias of {@code display-title} and is omitted on write.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSEdition")
@Table(name = "RXEDITION")
@JacksonXmlRootElement(localName = "edition")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "comment",
  "displayTitle",
  "editionType",
  "id",
  "priority",
  "pubServerId",
  "siteId"
})
public class PSEdition implements IPSCatalogItem, IPSEdition, Cloneable
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   private long editionid = -1L;
   
   @SuppressWarnings("unused")
   @Version
   private Integer version;

   @Basic
   private String displaytitle;

   @Basic
   private String editioncomment;

   @Basic
   private String editiontype;

   @Basic
   private Long destsite;

   @Basic
   private Integer priority;

   @Basic
   private Long pubserver;

   // Constructors

   /** default constructor */
   public PSEdition() {
   }

   /**
    * minimal constructor
    * 
    * @param editionid
    */
   public PSEdition(Integer editionid) {
      this.editionid = editionid;
   }

   // Property accessors
   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#getId()
    */
   @JsonProperty
   public long getId()
   {
      return this.editionid;
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#setId(java.lang.Integer)
    */
   public void setId(long id)
   {
      this.editionid = id;
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#getDisplayTitle()
    */
   @JsonProperty("display-title")
   public String getDisplayTitle()
   {
      return this.displaytitle;
   }
   
   /*
    *  (non-Javadoc)
    * @see com.percussion.services.publisher.IPSEdition#getName()
    */
   @JsonIgnore
   public String getName()
   {
      return getDisplayTitle();
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#setDisplayTitle(java.lang.String)
    */
   public void setDisplayTitle(String displayTitle)
   {
      this.displaytitle = displayTitle;
   }
   
   /*
    *  (non-Javadoc)
    * @see com.percussion.services.publisher.IPSEdition#setName(java.lang.String)
    */
   public void setName(String name)
   {
      setDisplayTitle(name);
   }

   @Override
   public void setNameImpl(String name)
   {
      setName(name);
   }

   @Override
   public void copyImpl(IPSEdition other)
   {
      if (other == null)
         throw new IllegalArgumentException("other may not be null");
      setDisplayTitle(other.getDisplayTitle());
      setComment(other.getComment());
      setEditionType(other.getEditionType());
      setSiteId(other.getSiteId());
      // copy priority if available
      try {
         setPriority(other.getPriority());
      } catch (Exception ignored) {
         // preserve current priority if not available
      }
      setPubServerId(other.getPubServerId());
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#getComment()
    */
   @JsonProperty
   public String getComment()
   {
      return this.editioncomment;
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#setComment(java.lang.String)
    */
   public void setComment(String comment)
   {
      this.editioncomment = comment;
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#getEditionType()
    */
   @JsonProperty("edition-type")
   public PSEditionType getEditionType()
   {
      try
      {
         int et = Integer.parseInt(editiontype);
         return PSEditionType.valueOf(et);
      }
      catch (Exception e)
      {
         return PSEditionType.AUTOMATIC;
      }
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#setEditionType(com.percussion.services.publisher.data.PSEditionType)
    */
   public void setEditionType(PSEditionType editionType)
   {
      if (editionType == null)
      {
         throw new IllegalArgumentException("editionType may not be null");
      }
      this.editiontype = Integer.toString(editionType.getTypeId());
   }

   @Override
   public void setEditionTypeImpl(PSEditionType editionType)
   {
      setEditionType(editionType);
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#getDestSite()
    */
   @JsonProperty("site-id")
   public IPSGuid getSiteId()
   {
      if (this.destsite == null)
         return null;
      
      return new PSGuid(PSTypeEnum.SITE, this.destsite);
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#setDestSite(java.lang.Integer)
    */
   public void setSiteId(IPSGuid siteId)
   {
      // Null-safe for design-object XML restore when site is unset (issue #1919).
      this.destsite = siteId == null ? null : siteId.longValue();
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.publisher.IPSEdition#getPubServerId()
    */
   @JsonProperty("pub-server-id")
   public IPSGuid getPubServerId()
   {
      if (this.pubserver == null)
         return null;
      
      return new PSGuid(PSTypeEnum.PUBLISHING_SERVER, this.pubserver);
   }
   
   @JsonIgnore
   public IPSGuid getPubServerOrSiteId()
   {
      return pubserver == null ? getSiteId() : getPubServerId();
   }
   
   /*
    * (non-Javadoc)
    * @see com.percussion.services.publisher.IPSEdition#setPubServerId(com.percussion.utils.guid.IPSGuid)
    */
   public void setPubServerId(IPSGuid serverId)
   {
      // Null-safe for design-object XML restore when pub server is unset (issue #1919).
      pubserver = serverId == null ? null : serverId.longValue();
   }
   
   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#getPriority()
    */
   @JsonProperty
   public Priority getPriority()
   {
      if (priority == null)
         return Priority.LOWEST;
      
      return convertIntToPriority(priority.intValue());
   }

   /**
    * Converts an integer to priority.
    * 
    * @param pvalue the priority in integer value. The value may be higher
    * or lower than the {@link Priority#HIGHEST} or {@link Priority#LOWEST}.
    *  
    * @return the priority, never <code>null</code>.
    */
   private Priority convertIntToPriority(int pvalue)
   {
      if (pvalue >= Priority.HIGHEST.getValue())
         return Priority.HIGHEST;
      
      if (pvalue == Priority.HIGH.getValue())
         return Priority.HIGH;
      if (pvalue == Priority.MEDIUM.getValue())
         return Priority.MEDIUM;
      if (pvalue == Priority.LOW.getValue())
         return Priority.LOW;

      return Priority.LOWEST;
      
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.publisher.data.IPSEdition#setPriority(java.lang.Integer)
    */
   public void setPriority(Priority ePriority)
   {
      if (ePriority == null)
      {
         throw new IllegalArgumentException("ePriority may not be null");
      }
      this.priority = ePriority.getValue();
   }

   @Override
   public void setPriorityImpl(Priority priority)
   {
      setPriority(priority);
   }

   /**
    * A convenient method. It is the same as {@link #setPriority(Priority)},
    * but it accept the integer value of the priority.
    * 
    * @param pvalue the integer value of the priority.
    */
   public void setPriorityInt(int pvalue)
   {
      this.priority = convertIntToPriority(pvalue).getValue();
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.IPSEdition#getGUID()
    */
   @JsonIgnore
   @IPSXmlSerialization(suppress=true)
   public IPSGuid getGUID()
   {
      // Offline-safe assemble (historical design XML suppressed guid; uses id).
      return new PSGuid(PSTypeEnum.EDITION, editionid);
   }

   /* (non-Javadoc)
    * @see com.percussion.services.catalog.IPSCatalogItem#setGUID(com.percussion.utils.guid.IPSGuid)
    */
   public void setGUID(IPSGuid guid)
   {
      if (guid == null)
         throw new IllegalArgumentException("guid may not be null");
      
      // Allow overwrite on design-object XML restore (BeanUtils + Jackson).
      editionid = guid.getUUID();
   }

   /**
    * Get the hibernate version information for this object.
    * 
    * @return returns the version, may be <code>null</code>.
    */
   @JsonIgnore
   @IPSXmlSerialization(suppress = true)
   public Integer getVersion()
   {
      return version;
   }
   
   /**
    * Modifies the hibernate version information for this object.
    * 
    * @param version The version to set.
    * 
    * @throws IllegalStateException if an attempt is made to set a previously
    * set version to a non-<code>null</code> value.
    */
   public void setVersion(Integer version) 
   {
      if (this.version != null && version != null)
         throw new IllegalStateException("Version can only be set once");
      
      this.version = version;
   }
   
   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object b)
   {
      return EqualsBuilder.reflectionEquals(this, b);
   }

   /* (non-Javadoc)
    * @see com.percussion.services.publisher.data.IPSEdition#hashCode()
    */
   @Override
   public int hashCode()
   {
      return (int)editionid;
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSEdition{");
      sb.append("editionid=").append(editionid);
      sb.append(", version=").append(version);
      sb.append(", displaytitle='").append(displaytitle).append('\'');
      sb.append(", editioncomment='").append(editioncomment).append('\'');
      sb.append(", editiontype='").append(editiontype).append('\'');
      sb.append(", destsite=").append(destsite);
      sb.append(", priority=").append(priority);
      sb.append(", pubserver=").append(pubserver);
      sb.append('}');
      return sb.toString();
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#clone()
    */
   @Override
   public Object clone() throws CloneNotSupportedException
   {
      return super.clone();
   }
   
   /* (non-Javadoc)
    * @see com.percussion.services.catalog.IPSCatalogItem#fromXML(java.lang.String)
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      PSXmlSerializationHelper.readFromXML(xmlsource, this);
   }
   
   /* (non-Javadoc)
    * @see com.percussion.services.catalog.IPSCatalogItem#toXML()
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }
   
   /*
    * (non-Javadoc)
    * @see com.percussion.services.publisher.IPSEdition#copy(com.percussion.services.publisher.IPSEdition)
    */
   public void copy(IPSEdition other)
   {
      if (other == null)
         throw new IllegalArgumentException("other may not be null.");
      if (!(other instanceof PSEdition))
         throw new IllegalArgumentException(
               "other must be instance of PSEdition");
      PSEdition src = (PSEdition) other;
      destsite = src.destsite;
      displaytitle = src.displaytitle;
      editioncomment = src.editioncomment;
      editiontype = src.editiontype;
      priority = src.priority;
   }
}
