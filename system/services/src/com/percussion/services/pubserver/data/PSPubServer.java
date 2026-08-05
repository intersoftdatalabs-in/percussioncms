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
package com.percussion.services.pubserver.data;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notEmpty;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.delivery.service.PSDeliveryInfoServiceLocator;
import com.percussion.delivery.service.impl.PSDeliveryInfoService;
import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.share.data.PSAbstractDataObject;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a publishing server related to a given site.
 *
 * <p>Design-object XML root is {@code pub-server}. Nested property item element is {@code
 * pub-server-property} (registered via {@link PSXmlSerializationHelper#addType}). Jackson opt-in
 * property surface (issue #1919 / epic #505).
 *
 * @author leonardohildt
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSPubServer")
@Table(name = "PSX_PUBSERVER")
@JacksonXmlRootElement(localName = "pub-server")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "description",
  "guid",
  "hasFullPublished",
  "name",
  "properties",
  "publishType",
  "serverId",
  "siteId",
  "siteRenamed",
  "serverTypeXml"
})
public class PSPubServer extends PSAbstractDataObject implements Serializable, IPSCatalogIdentifier, IPSPubServer
{
  static {
    PSXmlSerializationHelper.addType("pub-server-property", PSPubServerProperty.class);
  }

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static final Logger log = LogManager.getLogger(PSPubServer.class);

   @Id
   @Column(name = "PUBSERVERID")
   private long serverId;

   @Column(name = "SITEID")
   private long siteId;

   @Column(name = "NAME")
   private String name;

   @Column(name = "DESCRIPTION")
   private String description;

   @Column(name = "PUBLISHTYPE")
   private String publishType;
   
   @Column(name="SERVERTYPE")
   private String  serverType;
   
   @Basic
   @Column(name="HAS_FULL_PUBLISHED", nullable=true)
   private String hasFullPublished;

    @Basic
    @Column(name="SITERENAMED", nullable=true)
    private String siteRenamed;

   public static final String PRODUCTION= "PRODUCTION";
   public static final String STAGING = "STAGING";
   public static final String LICENSE = "LICENSE";
   
   /**
    * @return the serverType wrapped in an Optional. If unset, defaults to {@link #PRODUCTION}.
    */
   @JsonIgnore
   public java.util.Optional<String> getServerType()
   {
      return java.util.Optional.of(StringUtils.isBlank(serverType) ? PRODUCTION : serverType);
   }

   /**
    * Design-object XML form of server type (defaults to {@link #PRODUCTION} when blank).
    *
    * @return never blank
    */
   @JsonProperty("server-type")
   public String getServerTypeXml()
   {
      return StringUtils.isBlank(serverType) ? PRODUCTION : serverType;
   }

   /**
    * Jackson setter for {@code server-type} (avoids Optional {@link #getServerType()} conflict).
    *
    * @param serverType may be blank (defaults to {@link #PRODUCTION})
    */
   @JsonProperty("server-type")
   public void setServerTypeXml(String serverType)
   {
      setServerType(serverType);
   }

   /**
    * Returns whether this server has been fully published.
    *
    * @return {@code true} if fully published, {@code false} otherwise
    */
   @JsonProperty("has-full-published")
   public boolean hasFullPublished()
   {
      return "yes".equalsIgnoreCase(hasFullPublished) || Boolean.TRUE.toString().equalsIgnoreCase(hasFullPublished);
   }

   /**
    * Set whether this server has been fully published.
    *
    * @param hasFullPublished true if fully published, false otherwise
    */
   public void setHasFullPublished(boolean hasFullPublished)
   {
      this.hasFullPublished = hasFullPublished ? "yes" : "no";
   }
   /***
    *  Test validity of a publishing server type.
    * @param type A publishing server type
    * @return Returns true if the server type is valid.
    */
   private boolean isValidServerType(String type){
      if(isBlank(type))
         return false;
      
      
      if(type.toUpperCase().equals(PRODUCTION)||type.toUpperCase().equals(STAGING)||type.toUpperCase().equals(LICENSE))
         return true;
      
      return false;
   }
   
   /**
    * @param serverType the serverType to set
    */
   public void setServerType(String serverType)
   {
      // default to production
      String srvType = PRODUCTION;
      //if it is not blank make sure it is valid, otherwise throw illegal argument exception
      if (StringUtils.isNotBlank(serverType))
      {
         if(!isValidServerType(serverType))
            throw new IllegalArgumentException("serverType " + serverType + " is not a valid publishing server type");
         srvType = serverType;
      }
      this.serverType = srvType;
   }

   @OneToMany(targetEntity = PSPubServerProperty.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
   @JoinColumn(name = "PUBSERVERID", nullable = false, insertable = false, updatable = false)
   @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSPubServerProperty")
   @Fetch(FetchMode. SUBSELECT)
   private Set<PSPubServerProperty> properties = new HashSet<>();
   
   /**
    * The default constructor.
    */
   public PSPubServer()
   {
   }

   public void setProperties(Set<PSPubServerProperty> properties)
   {
      this.properties = properties != null ? properties : new HashSet<>();
   }

   /**
    * Hibernate / mutator view of properties (unordered set).
    *
    * @return never {@code null}
    */
   @JsonIgnore
   public Set<PSPubServerProperty> getProperties()
   {
      if (properties == null)
      {
         properties = new HashSet<>();
      }
      return properties;
   }

   /**
    * Stable-order property list for design-object XML (sorted by name).
    *
    * @return never {@code null}
    */
   @JsonProperty("properties")
   @JacksonXmlElementWrapper(localName = "properties")
   @JacksonXmlProperty(localName = "pub-server-property")
   public java.util.List<PSPubServerProperty> getPropertiesXml()
   {
      return getProperties().stream()
          .sorted(java.util.Comparator.comparing(
              p -> p.getName() == null ? "" : p.getName(), String.CASE_INSENSITIVE_ORDER))
          .collect(java.util.stream.Collectors.toList());
   }

   /**
    * Restore properties from design-object XML.
    *
    * @param props may be {@code null}
    */
   public void setPropertiesXml(java.util.List<PSPubServerProperty> props)
   {
      setProperties(props == null ? new HashSet<>() : new HashSet<>(props));
   }
   
   /**
    * @return the id
    */
   @JsonProperty("guid")
   public IPSGuid getGUID()
   {
      return new PSGuid(PSTypeEnum.PUBLISHING_SERVER, serverId);
   }

   /**
    * @param guid the id to set, never <code>null</code>
    */
   public void setGUID(IPSGuid guid)
   {
      if (guid == null)
      {
         throw new IllegalArgumentException("guid may not be null");
      }
      this.serverId = guid.getUUID();
   }

   /**
    * The site Id
    * 
    * @return Returns the site id, never <code>null</code>
    */
   @JsonProperty("site-id")
   public long getSiteId()
   {
      return siteId;
   }

   /**
    * @param siteId The site to set, may be <code>null</code> when disconnecting
    */
   public void setSiteId(long siteId)
   {
      this.siteId = siteId;
   }

   /*
    * //see base class method for details
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }

   /*
    * //see base class method for details
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      // Class-based read (not BeanUtils copy): Optional getters for description / serverType
      // prevent BeanUtils property-copy of those scalar fields (issue #1919).
      PSPubServer src =
          (PSPubServer) PSXmlSerializationHelper.readFromXML(xmlsource, PSPubServer.class);
      this.serverId = src.serverId;
      this.siteId = src.siteId;
      this.name = src.name;
      this.description = src.description;
      this.publishType = src.publishType;
      this.serverType = src.serverType;
      this.hasFullPublished = src.hasFullPublished;
      this.siteRenamed = src.siteRenamed;
      setProperties(src.properties != null ? new HashSet<>(src.properties) : new HashSet<>());
   }

   /**
    * Get the name of the publishing server.
    * 
    * @return the server name, never <code>null</code> or empty.
    */
   @JsonProperty
   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      if (isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      this.name = name;

   }

   /**
    * Get the description that describes this server.
    * 
    * @return Optional containing the description if set, otherwise empty.
    */
   @JsonIgnore
   public java.util.Optional<String> getDescription()
   {
      return java.util.Optional.ofNullable(description);
   }

   /**
    * Design-object XML form of description (nullable string).
    *
    * @return may be {@code null}
    */
   @JsonProperty("description")
   public String getDescriptionXml()
   {
      return description;
   }

   /**
    * Jackson setter for {@code description} (avoids Optional {@link #getDescription()} conflict).
    *
    * @param description the description to set
    */
   @JsonProperty("description")
   public void setDescriptionXml(String description)
   {
      this.description = description;
   }

   /**
    * Set the description.
    * 
    * @param description the description to set
    */
   public void setDescription(String description)
   {
      this.description = description;
   }

   /**
    * Get the server id for this server.
    * 
    * @return the server id, never <code>null</code> or empty.
    */
   @JsonProperty("server-id")
   public long getServerId()
   {
      return serverId;
   }

   /**
    * Set the server id.
    * 
    * @param serverId the server id to set
    */
   public void setServerId(long serverId)
   {
      this.serverId = serverId;
   }

   /**
    * Get the publish type for the server.
    * 
    * @return the publish type, never <code>null</code> or empty.
    */
   @JsonProperty("publish-type")
   public String getPublishType()
   {
      return publishType;
   }

   /**
    * Set the publish type for this server.
    * 
    * @param publishType the publish type to set
    */
   public void setPublishType(String publishType)
   {
      this.publishType = publishType;
   }

   /**
    * Adds a specified property. If the property already exist, set the supplied value;
    * otherwise add the property to this publish server.
    * 
    * @param pname the property name, not blank.
    * @param pvalue the property value, may be <code>null</code>.
    */
   public void addProperty(String pname, String pvalue)
   {
      notEmpty(pname);

      java.util.Optional<PSPubServerProperty> pOpt = getProperty(pname);
      if (pOpt.isPresent())
      {
         pOpt.get().setValue(pvalue);
         return;
      }

      PSPubServerProperty p = new PSPubServerProperty();
      p.setServerId(serverId);
      p.setName(pname);
      p.setValue(pvalue);

      properties.add(p);
   }
   
   /* (non-Javadoc)
    * @see com.percussion.services.pubserver.IPSPubServer#getProperty(java.lang.String)
    */
   public java.util.Optional<PSPubServerProperty> getProperty(String propertyName)
   {
      if (isBlank(propertyName) || properties.isEmpty())
      {
         return java.util.Optional.empty();
      }

      for (PSPubServerProperty property : properties)
      {
         if (equalsIgnoreCase(property.getName(), propertyName))
         {
            return java.util.Optional.of(property);
         }
      }
      return java.util.Optional.empty();
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see
    * com.percussion.services.pubserver.IPSPubServer#getPropertyValue(String)
    */
   public java.util.Optional<String> getPropertyValue(String propertyName)
   {
      if (isBlank(propertyName) || properties.isEmpty())
      {
         return java.util.Optional.empty();
      }

      for (PSPubServerProperty property : properties)
      {
         if (equalsIgnoreCase(property.getName(), propertyName))
         {
            return java.util.Optional.ofNullable(property.getValue());
         }
      }
      return java.util.Optional.empty();
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.percussion.services.pubserver.IPSPubServer#getPropertyValue(String,
    * String)
    */
   public String getPropertyValue(String propertyName, String defaultValue)
   {
      return getPropertyValue(propertyName).orElse(defaultValue);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.pubserver.IPSPubServer#isXmlFormat()
    */
   @JsonIgnore
   @IPSXmlSerialization(suppress = true)
   public boolean isXmlFormat()
   {
      return equalsIgnoreCase(
            getPropertyValue(IPSPubServerDao.PUBLISH_FORMAT_PROPERTY, "HTML"),
            "xml");
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.pubserver.IPSPubServer#isDatabaseType()
    */
   @JsonIgnore
   @IPSXmlSerialization(suppress = true)
   public boolean isDatabaseType()
   {
      return getPublishTypeEnum()
         .map(PublishType::isDatabase)
         .orElse(false);
   }

   /**
    * Whether publish type is FTP-based (computed; not design XML).
    */
   @JsonIgnore
   @IPSXmlSerialization(suppress = true)
   public boolean isFtpType()
   {
      return getPublishTypeEnum()
         .map(PublishType::isFtpBased)
         .orElse(false);
   }


    /**
     * Returns whether or not the site has been renamed since the last full publish.
     * @return <code>true</code> if the site has been renamed since last full publish.
     */
    @JsonProperty("site-renamed")
    public boolean getSiteRenamed()
    {
        return "y".equals(siteRenamed);
    }

    /**
     * Sets whether or not the site has been renamed since the last full publish.
     * Should be updated once full publish has been completed to <code>false</code>.
     * @param siteRenamed <code>true</code> if the site has been renamed since last full publish.
     */
    public void setSiteRenamed(boolean siteRenamed)
    {
        this.siteRenamed = siteRenamed ? "y" : "n";
    }

   /**
    * Determine if this and another server publish the same format to the same location
    * 
    * @param otherServer The other server, not <code>null</code>.
    * 
    * @return <code>true</code> if the same, <code>false</code> if different
    */
   public boolean isSamePublish(PSPubServer otherServer)
   {
      Validate.notNull(otherServer);
      
       if (!otherServer.getPublishType().equals(this.getPublishType()))
           return false;
       else if (otherServer.isXmlFormat() != this.isXmlFormat())
           return false;
       else if (!otherServer.getProperties().equals(this.getProperties()))
           return false;
       
       return true;
   }

   @JsonIgnore
   @IPSXmlSerialization(suppress = true)
   public java.util.Optional<String> getPublishServer(){
      PSDeliveryInfoService psDeliveryInfoService = (PSDeliveryInfoService) PSDeliveryInfoServiceLocator.getDeliveryInfoService();
      List<String> adminUrls = psDeliveryInfoService.getAdminUrls(this.serverType);
      String server = this.getPropertyValue(IPSPubServerDao.PUBLISH_SERVER_PROPERTY, null);
      if (server != null && adminUrls.contains(server)){
         return java.util.Optional.of(server);
      } else {
         if (server == null) {
            log.warn("No DTS server is currently configured for the site's default publishing server: '{}'. Defaulting to 'NONE'.", this.name);
         } else if (!server.equalsIgnoreCase(DEFAULT_DTS)) {
            log.warn("Configured DTS server '{}' for publishing server '{}' is not valid or not found in delivery-servers.xml. Defaulting to 'NONE'.", server, this.name);
         }
         addProperty(IPSPubServerDao.PUBLISH_SERVER_PROPERTY, DEFAULT_DTS);
         return java.util.Optional.of(DEFAULT_DTS);
      }
   }

}
