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
package com.percussion.services.security.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.data.IPSCloneTuner;
import com.percussion.services.guidmgr.PSGuidHelper;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
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
import jakarta.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Persist a single community definition including all role associations.
 *
 * <p>Design-object XML root is {@code community}. Historical {@code PSCommunity.betwixt} hides
 * {@code roleAssociations} / {@code siteAssociations}; Jackson honors that via {@link JsonIgnore}
 * on {@link #getRoleAssociations()} (issue #1889 / epic #505). Role membership is wired as scalar
 * {@code roles} long ids ({@link #getRoles()} / {@link #setRoles(Collection)}).
 */
@Entity
@DynamicUpdate
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSCommunity")
@Table(name = "RXCOMMUNITY")
@JacksonXmlRootElement(localName = "community")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"description", "guid", "name", "roles"})
public class PSCommunity implements Serializable, IPSCatalogSummary, 
   IPSCatalogItem, IPSCloneTuner
{
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = -7790532918814938767L;

   /**
    * The unique community id, can only be initialized once.
    */
   @Id
   @Column(name = "COMMUNITYID", nullable = false)
   private long id = UNINITIALIZED_ID;

   /**
    * The object version.
    */
   @Version
   @Column(name = "VERSION")
   private Integer version;

   /**
    * The community name, never <code>null</code> or empty in correctly
    * initialized objects. Unique accross all other defined communities.
    */
   @Basic
   @Column(name = "NAME", nullable = false, unique = true, length = 50)
   private String name;

   /**
    * The community description, may be <code>null</code> or empty.
    */
   @Basic
   @Column(name = "DESCRITPION", nullable = true, length = 255)
   private String description;

   /**
    * A set with all associated roles, never <code>null</code>, may be empty.
    */
   @OneToMany(targetEntity = PSCommunityRoleAssociation.class, 
      cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
   @JoinColumn(name = "COMMUNITYID", insertable = false, updatable = false)
   @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "Community_Roles")
   @Fetch(FetchMode.SUBSELECT)
   private Set<PSCommunityRoleAssociation> roleAssociations = 
      new HashSet<>();

   /**
    * Default constructor should only be used for serialization.
    */
   public PSCommunity()
   {
   }

   /**
    * Construct a new community for the supplied parameters.
    * 
    * @param name the name of the new community, not <code>null</code> or
    *    empty.
    * @param description the description for the new community, may be
    *    <code>null</code> or empty.
    */
   public PSCommunity(String name, String description)
   {
      setId(PSGuidHelper.generateNextLong(PSTypeEnum.COMMUNITY_DEF));
      setName(name);
      setDescription(description);
   }
   
   /**
    * Performs a shallow copy, merging entries from the supplied source, 
    * ignores id and version.
    * 
    * @param source the community to merge with, not <code>null</code>.
    */
   public void merge(PSCommunity source)
   {
      if (source == null)
         throw new IllegalArgumentException("source cannot be null");
      
      setName(source.getName());
      setDescription(source.getDescription());
      setRoleAssociations(source.getRoleAssociations());
   }

   /**
    * Get the uniqe id of this community.
    * 
    * @return the unique id.
    */
   @IPSXmlSerialization(suppress=true)
   @JsonIgnore
   public long getId()
   {
      return id;
   }

   /**
    * Set a new unique id for this community.
    * 
    * @param id the new unique id, can only be set once, not changeable 
    *    afterwards.
    */
   /**
    * Set the unique id. BeanUtils property-copy after Jackson deserialize may re-apply id after
    * {@link #setGUID(IPSGuid)}; allow overwrite for design-object XML restore (parity with
    * {@code PSKeyword#setId}, issue #1889).
    */
   public void setId(long id)
   {
      this.id = id;
   }

   /**
    * Get the object version.
    * 
    * @return the object version, <code>null</code> if not initialized yet.
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Integer getVersion()
   {
      return version;
   }

   /**
    * Set the object version. The version can only be set once in the life cycle
    * of this object.
    * 
    * @param version the version of the object, must be >= 0.
    */
   public void setVersion(Integer version)
   {
      if (this.version != null && version != null)
         throw new IllegalStateException("version can only be initialized once");

      if (version != null && version.intValue() < 0)
         throw new IllegalArgumentException("version must be >= 0");

      this.version = version;
   }

   /**
    * Get the community name.
    * 
    * @return the community name, never <code>null</code> or empty.
    */
   @JsonProperty
   public String getName()
   {
      return name;
   }

   /**
    * Set a new community name.
    * 
    * @param name the new name, not <code>null</code> or empty.
    */
   public void setName(String name)
   {
      if (StringUtils.isBlank(name))
         throw new IllegalArgumentException("name cannot be null or empty");

      this.name = name;
   }

   /**
    * Get the community description.
    * 
    * @return the community description, may be <code>null</code> or empty.
    */
   @JsonProperty
   public String getDescription()
   {
      return description;
   }

   /**
    * Set a new community description.
    * 
    * @param description the new community description, may be 
    *    <code>null</code> or empty.
    */
   public void setDescription(String description)
   {
      this.description = description;
   }

   @Override
   public boolean equals(Object b)
   {
      if (!(b instanceof PSCommunity))
         return false;
      
      if (this == b)
         return true;
      
      PSCommunity other = (PSCommunity) b;
      boolean isEquals = new EqualsBuilder()
         .append(id, other.id)
         .append(name, other.name)
         .append(description, other.description)
         .isEquals();
      
      // need to test entries "by hand" due to issues with proxied objects not
      // working as expected (hibnerate)
      if (isEquals)
      {
         Set<PSCommunityRoleAssociation> roles = 
            new HashSet<>(roleAssociations);
         Set<PSCommunityRoleAssociation> otherRoles = 
            new HashSet<>(other.roleAssociations);
         isEquals = roles.equals(otherRoles);
      }
      
      return isEquals;
   }

   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .append(id)
         .append(name)
         .append(description)
         .append(roleAssociations)
         .toHashCode();
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSCommunity{");
      sb.append("id=").append(id);
      sb.append(", version=").append(version);
      sb.append(", name='").append(name).append('\'');
      sb.append(", description='").append(description).append('\'');
      sb.append(", roleAssociations=").append(roleAssociations);
      sb.append('}');
      return sb.toString();
   }

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getGUID()
    */
   /**
    * Catalog GUID. Jackson emits/reads string form via shared {@code IPSGuid} converter in {@code
    * PSJacksonXmlSerializationHelper}.
    */
   @JsonProperty("guid")
   public IPSGuid getGUID()
   {
      return new PSGuid(PSTypeEnum.COMMUNITY_DEF, id);
   }

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getType()
    */
   @JsonIgnore
   public String getType()
   {
      return PSTypeEnum.COMMUNITY_DEF.name();
   }

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getLabel()
    */
   @JsonIgnore
   public String getLabel()
   {
      return getName();
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#setGUID(IPSGuid)
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException
   {
      if (newguid == null)
         throw new IllegalArgumentException("newguid may not be null");

      // Allow overwrite on design-object XML restore (BeanUtils + Jackson); same pattern as
      // PSKeyword#setGUID (issue #1889).
      id = newguid.longValue();
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#fromXML(String)
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      PSXmlSerializationHelper.readFromXML(xmlsource, this);
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#toXML()
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }

   /**
    * Get all community role associations defined for this community. The
    * returned collection may be modified but no change to the underlying
    * association will be made until the corresponding
    * {@link #setRoleAssociations(Collection) method} is called with the new
    * data.
    *
    * <p>Suppressed from design XML (historical {@code PSCommunity.betwixt}
    * {@code hide property="roleAssociations"}). Wire form uses {@link #getRoles()}.
    * 
    * @return a collection with all defined community role associations, never
    *    <code>null</code>, may be empty.
    */
   @JsonIgnore
   public Collection<IPSGuid> getRoleAssociations()
   {
      Collection<IPSGuid> associations = new ArrayList<>();

      for (PSCommunityRoleAssociation a : roleAssociations)
         associations.add(new PSGuid(PSTypeEnum.ROLE, a.getRoleId()));

      return associations;
   }

   /**
    * Set the new role associations.
    * 
    * @param associations the new role associations for this community, not
    *    <code>null</code>, may be empty.
    */
   public void setRoleAssociations(Collection<IPSGuid> associations)
   {
      if (associations == null)
         throw new IllegalArgumentException("associations cannot be null");
      
      roleAssociations.clear();
      for (IPSGuid association : associations)
         addRoleAssociation(association);
   }

   /**
    * Add a single role association to the set, if the association already
    * exists this call will have no effect.
    * 
    * @param roleId the id of the role to associate with this community, not
    *    <code>null</code>.
    */
   public void addRoleAssociation(IPSGuid roleId)
   {
      if (roleId == null)
         throw new IllegalArgumentException("roleId cannot be null");

      // Use host/type/uuid assemble so BeanUtils property-copy order (roles before guid/id)
      // cannot throw "Type does not match" on a partially-initialized community (issue #1889).
      long communityUuid = id == UNINITIALIZED_ID ? 0L : (id & 0xFFFFFFFFL);
      roleAssociations.add(
          new PSCommunityRoleAssociation(
              new PSGuid(0L, PSTypeEnum.COMMUNITY_DEF, communityUuid), roleId));
   }
   
   /**
    * Add a role for serialization (Betwixt adder). Jackson uses {@link #setRoles(Collection)}.
    *
    * <p>Design XML may store either a bare role UUID or a composite {@link IPSGuid#longValue()};
    * both are normalized to a ROLE-typed guid (issue #1889).
    *
    * @param rid an id that corresponds to a guid
    */
   @JsonIgnore
   public void addRole(long rid)
   {
      long uuid = rid & 0xFFFFFFFFL;
      addRoleAssociation(new PSGuid(PSTypeEnum.ROLE, uuid));
   } 
   
   /**
    * Get the roles for design-object XML serialization (scalar role ids).
    *
    * @return the roles for serialization, never <code>null</code> but may be empty
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "roles")
   @JacksonXmlProperty(localName = "long")
   public Collection<Long> getRoles()
   {
      Collection<IPSGuid> ras = getRoleAssociations();
      Collection<Long> rval = new ArrayList<>();
      for(IPSGuid ra : ras)
      {
         rval.add(ra.longValue());
      }
      // Stable order for design-object XML / golden parity
      return rval.stream().sorted().collect(java.util.stream.Collectors.toList());
   }

   /**
    * Set roles from design-object XML (Jackson collection restore).
    *
    * @param roles role long ids, may be {@code null} or empty
    */
   public void setRoles(Collection<Long> roles)
   {
      roleAssociations.clear();
      if (roles == null)
      {
         return;
      }
      for (Long rid : roles)
      {
         if (rid != null)
         {
            addRole(rid.longValue());
         }
      }
   }
   
   /**
    * Remove the association to the identified role. Does nothing if no
    * association exists for the supplied role id.
    * 
    * @param roleId the id of the role for which to remove the association,
    *    not <code>null</code>.
    */
   public void removeRoleAssociation(IPSGuid roleId)
   {
      if (roleId == null)
         throw new IllegalArgumentException("roleId cannot be null");
      
      for (PSCommunityRoleAssociation association : roleAssociations)
      {
         if (association.getRoleId() == roleId.longValue())
         {
            roleAssociations.remove(association);
            break;
         }
      }
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.data.IPSCloneTuner#tuneClone(java.lang.Object,
    * long)
    */
   public Object tuneClone(long newId)
   {
      id = newId;
      for (PSCommunityRoleAssociation assoc : roleAssociations)
      {
         assoc.setCommunityId(id);
      }
      return this;
   }

   /**
    * Constant to indicate an id is not initialized or invalid.
    */
   public static final long UNINITIALIZED_ID = -1;
}
