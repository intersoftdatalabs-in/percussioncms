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
package com.percussion.services.contentmgr.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.impl.IPSContentRepository;
import com.percussion.services.contentmgr.impl.PSContentInternalLocator;
import com.percussion.services.contentmgr.impl.PSContentUtils;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Wrapper for content type definitions in Rhythmyx. Additional methods are provided here for
 * Rhythmyx specific information. Most of the JSR-170 information is not provided at this time.
 *
 * <p>Design-object XML root is {@code node-definition}. Jackson opt-in surface matches historical
 * package shape ({@code template-id} items under {@code template-ids}; {@code string} items under
 * {@code workflow-ids}). JCR interface methods that are not part of the design wire form are
 * suppressed (issue #1921 / epic #505).
 *
 * @author dougrand
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSNodeDefinition")
@NaturalIdCache
@Table(name = "CONTENTTYPES")
@JacksonXmlRootElement(localName = "node-definition")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "auto-created",
  "description",
  "hideFromMenu",
  "id",
  "internalName",
  "label",
  "mandatory",
  "name",
  "newRequest",
  "objectType",
  "protected",
  "queryRequest",
  "rawContentType",
  "templateIds",
  "updateRequest",
  "workflowIds"
})
public class PSNodeDefinition implements IPSNodeDefinition {
  static {
    // Register types with XML serializer for read creation of objects
    PSXmlSerializationHelper.addType("variant-guid", PSGuid.class);
  }

   @Id
   @Column(name = "CONTENTTYPEID")
   private Long m_contenttypeid;

   @SuppressWarnings("unused")
   @Version
   @Column(name = "VERSION")
   private Integer m_version = -1;

   @NaturalId(mutable=true)
   @Column(name = "CONTENTTYPENAME", unique=true)
   private String m_name;

   @Basic
   @Column(name = "CONTENTTYPELABEL")
   private String m_label;

   @Basic
   @Column(name = "CONTENTTYPEDESC")
   private String m_description;

   @Basic
   @Column(name = "CONTENTTYPENEWREQUEST")
   private String m_newRequest;

   @Basic
   @Column(name = "CONTENTTYPEQUERYREQUEST")
   private String m_queryRequest;

   @Basic
   @Column(name = "CONTENTTYPEUPDATEREQUEST")
   private String m_updateRequest;

   @Basic
   @Column(name = "OBJECTTYPE")
   private Integer m_objectType;

   @Basic
   @Column(name = "HIDEFROMMENU")
   private Boolean m_hideFromMenu = Boolean.FALSE;

   @OneToMany(targetEntity = PSContentTemplateDesc.class, cascade =
   {CascadeType.ALL}, fetch = FetchType.EAGER, orphanRemoval = true)
   @JoinColumn(name = "CONTENTTYPEID")
   @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "object")
   @Fetch(FetchMode. SUBSELECT)
   private Set<PSContentTemplateDesc> m_cvDescriptors;

   @OneToMany(targetEntity = PSContentTypeWorkflow.class, cascade =
   {CascadeType.ALL}, fetch = FetchType.EAGER, orphanRemoval = true)
   @JoinColumn(name = "CONTENTTYPEID")
   @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "object")
   @Fetch(FetchMode. SUBSELECT)
   private Set<PSContentTypeWorkflow> m_ctWfRels;

   /**
    * Provisional association PKs when GuidManager is not configured (offline tests / design tools).
    * Live CMS always has {@link PSGuidManagerLocator#getGuidMgr()}.
    */
   private static final AtomicLong OFFLINE_CTWF_ID = new AtomicLong(0L);

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypes()
    */
   @IPSXmlSerialization(suppress = true)
   public NodeType[] getRequiredPrimaryTypes()
   {
      return new NodeType[] { getDefaultPrimaryType() };
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryType()
    */
   @IPSXmlSerialization(suppress = true)
   public NodeType getDefaultPrimaryType()
   {
      // Note that this looks up the primary type on demand to avoid
      // the cost
      IPSContentRepository repository =
           PSContentInternalLocator.getLegacyRepository();
      try
      {
         return repository.findNodeType(this);
      }
      catch (NoSuchNodeTypeException e)
      {
         return null;
      }
   }

   @Override
   @JsonIgnore
   public String[] getRequiredPrimaryTypeNames()
   {
      NodeType type = getDefaultPrimaryType();
      if (type == null)
      {
         return new String[0];
      }
      return new String[] { type.getName() };
   }

   @Override
   @JsonIgnore
   public String getDefaultPrimaryTypeName()
   {
      NodeType type = getDefaultPrimaryType();
      return type == null ? null : type.getName();
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
    */
   @JsonIgnore
   public boolean allowsSameNameSiblings()
   {
      return false;
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
    */
   @IPSXmlSerialization(suppress = true)
   public NodeType getDeclaringNodeType()
   {
      return getDefaultPrimaryType();
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.ItemDefinition#getName()
    */
   @JsonProperty
   public String getName()
   {
      if (m_name != null)
         return PSContentUtils.externalizeName(m_name).replace(' ', '_');
      else
         return null;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#getInternalName()
    */
   @JsonProperty
   public String getInternalName()
   {
      return m_name;
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
    */
   @JsonProperty("auto-created")
   public boolean isAutoCreated()
   {
      return false;
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
    */
   @JsonProperty("mandatory")
   public boolean isMandatory()
   {
      return false;
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.ItemDefinition#getOnParentVersion()
    */
   @IPSXmlSerialization(suppress = true)
   public int getOnParentVersion()
   {
      throw new UnsupportedOperationException("Not yet supported");
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.jcr.nodetype.ItemDefinition#isProtected()
    */
   @JsonProperty("protected")
   public boolean isProtected()
   {
      return false;
   }

   /**
    * @return Returns the contenttypeid.
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public IPSGuid getGUID()
   {
      return new PSGuid(PSTypeEnum.NODEDEF, m_contenttypeid);
   }

   /**
    * @param guid The contenttypeid to set.
    */
   public void setGUID(IPSGuid guid)
   {
      m_contenttypeid = guid.longValue();
   }

   /**
    * Get the raw content type id, required for some operations.
    *
    * <p>Fails fast when the id has not been set (null {@code m_contenttypeid}). Returning a synthetic
    * {@code 0L} would mask uninitialized-object bugs during design-object XML and runtime use.
    *
    * @return the raw content type id
    * @throws NullPointerException if the content type id has not been set
    */
   @JsonProperty
   public long getRawContentType()
   {
      return m_contenttypeid;
   }

   /**
    * Set raw content type id (design-object XML property {@code raw-content-type}).
    *
    * @param rawContentType the content type id
    */
   public void setRawContentType(long rawContentType)
   {
      m_contenttypeid = rawContentType;
   }

   /**
    * Get id as long, only used for serialization.
    *
    * <p>Fails fast when the id has not been set (null {@code m_contenttypeid}), matching historical
    * Betwixt unboxing behavior. Callers must set {@link #setId(long)} / {@link #setRawContentType(long)}
    * before reading.
    *
    * @return get the id
    * @throws NullPointerException if the content type id has not been set
    */
   @JsonProperty
   public long getId()
   {
      return m_contenttypeid;
   }

   /**
    * Set the new id, only used for serialization
    * 
    * @param id the new id
    */
   public void setId(long id)
   {
      m_contenttypeid = id;
   }

   /**
    * Get the label, which is the value that is shown in the user interface.
    * 
    * @return Returns the label.
    */
   @JsonProperty
   public String getLabel()
   {
      return m_label;
   }

   /**
    * A new label
    * 
    * @param label The label to set, may be <code>null</code> or empty
    */
   public void setLabel(String label)
   {
      m_label = label;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#getDescription()
    */
   @JsonProperty
   public String getDescription()
   {
      return m_description;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#setDescription(java.lang.String)
    */
   public void setDescription(String description)
   {
      m_description = description;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#getHideFromMenu()
    */
   @JsonProperty
   public Boolean getHideFromMenu()
   {
      return m_hideFromMenu;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#setHideFromMenu(java.lang.Boolean)
    */
   public void setHideFromMenu(Boolean hideFromMenu)
   {
      if (hideFromMenu == null)
      {
         throw new IllegalArgumentException("hideFromMenu may not be null");
      }
      m_hideFromMenu = hideFromMenu;
   }

   /**
    * @return Returns the newRequest.
    */
   @JsonProperty
   public String getNewRequest()
   {
      return m_newRequest;
   }

   /**
    * @param newRequest The newRequest to set.
    */
   public void setNewRequest(String newRequest)
   {
      m_newRequest = newRequest;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#getObjectType()
    */
   @JsonProperty
   public Integer getObjectType()
   {
      return m_objectType;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#setObjectType(java.lang.Integer)
    */
   public void setObjectType(Integer objectType)
   {
      m_objectType = objectType;
   }

   /**
    * @return Returns the queryRequest.
    */
   @JsonProperty
   public String getQueryRequest()
   {
      return m_queryRequest;
   }

   /**
    * @param queryRequest The queryRequest to set.
    */
   public void setQueryRequest(String queryRequest)
   {
      m_queryRequest = queryRequest;
   }

   /**
    * @return Returns the updateRequest.
    */
   @JsonProperty
   public String getUpdateRequest()
   {
      return m_updateRequest;
   }

   /**
    * @param updateRequest The updateRequest to set.
    */
   public void setUpdateRequest(String updateRequest)
   {
      m_updateRequest = updateRequest;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#setName(java.lang.String)
    */
   public void setName(String name)
   {
      m_name = PSContentUtils.internalizeName(name);
   }

   /** (non-Javadoc)
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#setInternalName(java.lang.String)
    */
   public void setInternalName(String name)
   {
      if (StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      m_name = name;
   }

   /**
    * Get the actual descriptors that associate a given content type with the
    * templates. Don't use this method directly, use {@link #getVariantGuids()}
    * and related methods instead.
    * 
    * @return the descriptors, may be empty but not <code>null</code>
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Set<PSContentTemplateDesc> getCvDescriptors()
   {
      return m_cvDescriptors;
   }

   /**
    * Set the descriptors. See {@link #getCvDescriptors()} for more information.
    * 
    * @param cvDescriptors The cvDescriptors to set.
    */
   public void setCvDescriptors(Set<PSContentTemplateDesc> cvDescriptors)
   {
      m_cvDescriptors = cvDescriptors;
   }


   /**
    * Get the actual relationships that associate a given content type with the
    * workflow. Don't use this method directly, use {@link #getWorkflowGuids()}
    * and related methods instead.
    * 
    * @return the contenttype workflow relationships, may be empty but not
    * <code>null</code>
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Set<PSContentTypeWorkflow> getCtWfRels()
   {
      return m_ctWfRels;
   }

   /**
    * Set the content type workflow relations. See {@link #getCtWfRels()} for
    * more information.
    * 
    * @param cTWfRels The cTWfRels to set.
    */
   public void setCtWfRels(Set<PSContentTypeWorkflow> cTWfRels)
   {
      m_ctWfRels = cTWfRels;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.catalog.IPSCatalogItem#toXML()
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.services.catalog.IPSCatalogItem#fromXML(java.lang.String)
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      m_version = 0;
      PSXmlSerializationHelper.readFromXML(xmlsource, this);
   }

   /**
    * Get the associated templates as guids, similar to
    * {@link #getTemplateIds()} but returning guids.
    * 
    * @return a set of guids, never <code>null</code>, unmodifiable set is 
    * returned
    * 
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Set<IPSGuid> getVariantGuids()
   {
      Set<IPSGuid> guids = new HashSet<>();
      if (m_cvDescriptors != null)
      {
         for (PSContentTemplateDesc desc : m_cvDescriptors)
         {
            guids.add(desc.getTemplateId());
         }
      }
      return Collections.unmodifiableSet(guids);
   }

   /**
    * Get the associated workflows as guids.
    * 
    * @return a set of guids, never <code>null</code>, unmodifiable set is 
    * returned
    * 
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Set<IPSGuid> getWorkflowGuids()
   {
      Set<IPSGuid> guids = new HashSet<>();
      if (m_ctWfRels != null)
      {
         for (PSContentTypeWorkflow rel : m_ctWfRels)
         {
            guids.add(rel.getWorkflowId());
         }
      }
      return Collections.unmodifiableSet(guids);
   }

   /** (non-Javadoc)
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#addVariantGuid(com.percussion.utils.guid.IPSGuid)
    */
   public void addVariantGuid(IPSGuid guid)
   {
      if (guid == null)
      {
         throw new IllegalArgumentException("guid may not be null");
      }
      if (m_cvDescriptors == null)
      {
         m_cvDescriptors = new HashSet<>();
      }
      IPSContentMgr cmgr = PSContentMgrLocator.getContentMgr();
      
      for (PSContentTemplateDesc desc : m_cvDescriptors)
      {
         if (guid.equals(desc.getTemplateId()))
         {
            return;
         }
      }

      // dont always create a new one, if an association exists, use it
      PSContentTemplateDesc cvDesc = null;
      try
      {
         cvDesc = cmgr.findContentTypeTemplateAssociation(guid, this
               .getGUID());
      }
      catch (RepositoryException e)
      {
      }

      if (cvDesc != null)
         m_cvDescriptors.add(cvDesc);
      else
      {
         // association not found, so add a new one
         PSContentTemplateDesc desc = new PSContentTemplateDesc();
         IPSGuidManager gmgr = PSGuidManagerLocator.getGuidMgr();
         desc.setId(gmgr.createGuid(PSTypeEnum.INTERNAL).longValue());
         desc.setContentTypeId(getGUID());
         desc.setTemplateId(guid);
         m_cvDescriptors.add(desc);
      }
   }

   /** (non-Javadoc)
    * @see com.percussion.services.contentmgr.IPSNodeDefinition#removeVariantGuid(com.percussion.utils.guid.IPSGuid)
    */
   public void removeVariantGuid(IPSGuid guid)
   {
      if (guid == null)
      {
         throw new IllegalArgumentException("guid may not be null");
      }
      PSContentTemplateDesc found = null;
      for (PSContentTemplateDesc desc : m_cvDescriptors)
      {
         if (guid.equals(desc.getTemplateId()))
         {
            found = desc;
            break;
         }
      }
      if (found != null)
      {
         m_cvDescriptors.remove(found);
      }
   }

   /**
    * Set the version. This method is explicitly not exposed in the interface as
    * there are only limited cases where this needs to be used, such as with web
    * services.
    * 
    * @param version the version of the object, must be >= 0.
    */
   public void setVersion(Integer version)
   {
      if (version==null || version==-1) version = 0;
      else if (version < 0)
         throw new IllegalArgumentException("version must be >= 0");

      m_version = version;
   }

   /**
    * Get the version. This method is explicitly not exposed in the interface as
    * there are only limited cases where this needs to be used, such as with web
    * services.
    * 
    * @return The version, may be <code>null</code> if it has not been set.
    */
   @IPSXmlSerialization(suppress=true)
   @JsonIgnore
   public Integer getVersion()
   {
      return m_version;
   }

   /**
    * Method that is used only as part of the internal implementation. Call this
    * method to clear all related descriptors before a deletion.
    * 
    */
   void removeAllVariants()
   {
      m_cvDescriptors.clear();
   }

   /** (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object arg0)
   {
      EqualsBuilder builder = new EqualsBuilder();

      PSNodeDefinition b = (PSNodeDefinition) arg0;

      return builder.append(getGUID(), b.getGUID()).append(getVariantGuids(),
            b.getVariantGuids()).append(getWorkflowGuids(),
            b.getWorkflowGuids()).append(getDescription(), b.getDescription())
            .append(getHideFromMenu(), b.getHideFromMenu()).append(getName(),
                  b.getName()).append(getLabel(), b.getLabel()).append(
                  getNewRequest(), b.getNewRequest()).append(getObjectType(),
                  b.getObjectType()).append(getQueryRequest(),
                  b.getQueryRequest()).append(getUpdateRequest(),
                  b.getUpdateRequest()).isEquals();
   }

   /** (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      return m_name != null ? m_name.hashCode() : 0;
   }

   /**
    * (non-Javadoc)
    *
    * @see Object#toString()
    */
   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSNodeDefinition{");
      sb.append("m_contenttypeid=").append(m_contenttypeid);
      sb.append(", m_version=").append(m_version);
      sb.append(", m_name='").append(m_name).append('\'');
      sb.append(", m_label='").append(m_label).append('\'');
      sb.append(", m_description='").append(m_description).append('\'');
      sb.append(", m_newRequest='").append(m_newRequest).append('\'');
      sb.append(", m_queryRequest='").append(m_queryRequest).append('\'');
      sb.append(", m_updateRequest='").append(m_updateRequest).append('\'');
      sb.append(", m_objectType=").append(m_objectType);
      sb.append(", m_hideFromMenu=").append(m_hideFromMenu);
      sb.append(", m_cvDescriptors=").append(m_cvDescriptors);
      sb.append(", m_ctWfRels=").append(m_ctWfRels);
      sb.append('}');
      return sb.toString();
   }

   /**
    * Get a string representation of GUIDs of the template associations.
    *
    * <p>Package/design XML uses item element {@code template-id} (not mapped type name).
    *
    * @return set of Guid Strings may be empty never <code>null</code>
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "template-ids")
   @JacksonXmlProperty(localName = "template-id")
   public Set<String> getTemplateIds()
   {
      // TreeSet for stable design-object XML order (HashSet iteration is non-deterministic)
      Set<String> ids = new TreeSet<>();
      if (m_cvDescriptors != null && !m_cvDescriptors.isEmpty())
      {
         for (PSContentTemplateDesc desc : m_cvDescriptors)
            ids.add(desc.getTemplateId().toString());
      }
      return ids;
   }

   /**
    * Restore template associations from design-object XML ({@code template-ids}). Replaces any
    * existing {@link #m_cvDescriptors} entries (clear-then-add) so repeated calls do not
    * accumulate. Uses historical {@link #addTemplateId(String)} path (requires content manager for
    * association lookup in live CMS). Offline unit tests exercise write/scalar restore only.
    *
    * @param templateIds may be {@code null} or empty (clears associations)
    */
   public void setTemplateIds(Set<String> templateIds)
   {
      if (m_cvDescriptors == null)
      {
         m_cvDescriptors = new HashSet<>();
      }
      else
      {
         m_cvDescriptors.clear();
      }
      if (templateIds == null || templateIds.isEmpty())
      {
         return;
      }
      for (String tmpId : templateIds)
      {
         addTemplateId(tmpId);
      }
   }

   /**
    * Get a string representation of GUIDs of the workflow associations.
    *
    * <p>Package XML uses nested {@code string} item elements under {@code workflow-ids}.
    *
    * @return set of Guid Strings may be empty never <code>null</code>
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "workflow-ids")
   @JacksonXmlProperty(localName = "string")
   public Set<String> getWorkflowIds()
   {
      // TreeSet for stable design-object XML order
      Set<String> ids = new TreeSet<>();
      if (m_ctWfRels != null && !m_ctWfRels.isEmpty())
      {
         for (PSContentTypeWorkflow ctwf : m_ctWfRels)
            ids.add(ctwf.getWorkflowId().toString());
      }
      return ids;
   }

   /**
    * Jackson collection setter for {@code workflow-ids}. Rebuilds {@link #m_ctWfRels} entries from
    * GUID strings so design-object XML restore retains workflow associations.
    *
    * <p>Replaces any existing associations (clear-then-add). Offline-friendly: creates new {@link
    * PSContentTypeWorkflow} rows without consulting the content manager. Each association primary
    * key is assigned in {@link #addWorkflowGuid(IPSGuid)} (GuidManager when available; provisional
    * offline id otherwise) because {@code PSContentTypeWorkflow.m_ctWfId} has no {@code
    * @GeneratedValue}. Live package install may still re-merge via {@code PSContentTypeHelper}
    * when existing DB rows must be reused. Skips blank entries and de-duplicates by workflow GUID.
    *
    * @param workflowIds may be {@code null} or empty (clears associations)
    */
   public void setWorkflowIds(Set<String> workflowIds)
   {
      if (m_ctWfRels == null)
      {
         m_ctWfRels = new HashSet<>();
      }
      else
      {
         m_ctWfRels.clear();
      }
      if (workflowIds == null || workflowIds.isEmpty())
      {
         return;
      }
      for (String wfId : workflowIds)
      {
         if (StringUtils.isBlank(wfId))
         {
            continue;
         }
         addWorkflowId(wfId);
      }
   }

   /**
    * Add a workflow association from a GUID string (design-object {@code workflow-ids} item).
    *
    * @param wfId string form of the workflow guid, never blank
    */
   @JsonIgnore
   public void addWorkflowId(String wfId)
   {
      if (StringUtils.isBlank(wfId))
      {
         throw new IllegalArgumentException("workflow guid may not be null or empty");
      }
      addWorkflowGuid(new PSGuid(wfId));
   }

   /**
    * Add the given workflow GUID to {@code m_ctWfRels} if not already present. Does not require a
    * live content manager (unlike template association restore). Assigns a non-null association
    * primary key: prefers {@link PSGuidManagerLocator#getGuidMgr()} like {@link
    * #addVariantGuid(IPSGuid)}; when GuidManager is unavailable (offline unit tests / design tools
    * without Spring), uses a provisional monotonic id so cascaded persist never sees a null {@code
    * CONTENTTYPE_WORKFLOW_ID} ({@link PSContentTypeWorkflow} has {@code @Id} without {@code
    * @GeneratedValue}).
    *
    * @param guid workflow guid, never {@code null}
    */
   @JsonIgnore
   public void addWorkflowGuid(IPSGuid guid)
   {
      if (guid == null)
      {
         throw new IllegalArgumentException("guid may not be null");
      }
      if (m_ctWfRels == null)
      {
         m_ctWfRels = new HashSet<>();
      }
      for (PSContentTypeWorkflow rel : m_ctWfRels)
      {
         if (guid.equals(rel.getWorkflowId()))
         {
            return;
         }
      }
      PSContentTypeWorkflow rel = new PSContentTypeWorkflow();
      // PK is application-assigned (no @GeneratedValue) — mirror addVariantGuid.
      Optional<IPSGuidManager> gmgr = PSGuidManagerLocator.getGuidMgrSafely();
      if (gmgr.isPresent())
      {
         rel.setId(gmgr.get().createGuid(PSTypeEnum.INTERNAL).longValue());
      }
      else
      {
         rel.setId(OFFLINE_CTWF_ID.incrementAndGet());
      }
      if (m_contenttypeid != null)
      {
         rel.setContentTypeId(new PSGuid(PSTypeEnum.NODEDEF, m_contenttypeid));
      }
      rel.setWorkflowId(guid);
      m_ctWfRels.add(rel);
   }

   /**
    * Add the Template Guid, represented by a string to the template association
    * aka cvDescriptors
    * 
    * @param tmpId the string form of the guid, never <code>null</code>
    */
   @JsonIgnore
   public void addTemplateId(String tmpId)
   {
      if (StringUtils.isBlank(tmpId))
         throw new IllegalArgumentException("template guid may not be null");
      addVariantGuid(new PSGuid(tmpId));
   }

   /**
    * Add the given template to the cv_descriptors, if not found dont add it.
    * During de-serialization, if we donot do this, we end up creating a new
    * association and give it a new guid. The other side effect ( again, if we
    * dont do it this way...) is that the table gets corrupted with the deleted
    * associations namely the templateid column is set to <NULL> BAAAAD
    * 
    * @param g the template guid that needs to be added to the collection of
    *           cv_descriptors
    */
   private void addTemplateGuidToCollection(IPSGuid g)
   {
      if (g == null)
         throw new IllegalArgumentException("template guid may not be null");
      PSContentTemplateDesc desc = null;
      IPSContentMgr cmgr = PSContentMgrLocator.getContentMgr();
      try
      {
         desc = cmgr.findContentTypeTemplateAssociation(g, this.getGUID());
      }
      catch (RepositoryException e)
      {
      }

      if (desc != null)
         m_cvDescriptors.add(desc);
   }

   /**
    * Given a Collection of template ids as strings, sync them with the existing
    * list of template associations for this NodeDef
    * 
    * @param newT set of string template ids never <code>null</code>, may be
    *           empty
    */
   public void mergeTemplateIds(Set<String> newT)
   {
      if (newT.isEmpty())
         return;
      Set<IPSGuid> newTmps = new HashSet<>();
      for (String t : newT)
         newTmps.add(new PSGuid(t));

      // if the current template set is empty
      if (m_cvDescriptors.isEmpty())
      {
         for (IPSGuid guid : newTmps)
         {
            addTemplateGuidToCollection(guid);
         }
         return;
      }
      // get all existing tmp guids associated with this site
      Set<IPSGuid> curTmps = new HashSet<>();
      for (PSContentTemplateDesc desc : m_cvDescriptors)
         curTmps.add(desc.getTemplateId());

      /**
       * 1. commonTmps = intersection of curTmps, newTmps 2. removeTmps =
       * curTmps - newTmps 3. delete removeTmps from curTmps 4. delete
       * commonTmps from newTmps
       */
      Collection common = CollectionUtils.intersection(curTmps, newTmps);
      Collection remove = CollectionUtils.subtract(curTmps, newTmps);
      curTmps.removeAll(remove);
      newTmps.removeAll(common);
      curTmps.addAll(newTmps);
      m_cvDescriptors.clear();

      for (IPSGuid guid : curTmps)
         addTemplateGuidToCollection(guid);
   }
}
