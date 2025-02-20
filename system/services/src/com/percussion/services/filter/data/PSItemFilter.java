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
package com.percussion.services.filter.data;

import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.data.IPSCloneTuner;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSFilterServiceErrors;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.guidmgr.PSGuidHelper;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.xml.sax.SAXException;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Implementation for an item filter, this is a pure mapping object to bring
 * database info into memory.
 * 
 * @author dougrand
 */
@Entity
@SelectBeforeUpdate
@NaturalIdCache(region = "PSItemFilter_NaturalId")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSItemFilter")
@Table(name = "PSX_ITEM_FILTER")
public class PSItemFilter implements IPSItemFilter, IPSCatalogSummary,
   IPSCloneTuner, Serializable
{
   private static final Logger log = LogManager.getLogger(PSItemFilter.class);

   /**
    * 
    */
   private static final long serialVersionUID = -2471257402419543902L;

   static
   {
      // Register types with XML serializer for read creation of objects
      PSXmlSerializationHelper.addType("rule-def", PSItemFilterRuleDef.class);
   }

   /**
    * Primary key for an item filter
    */
   @Id
   private long filter_id;

   /**
    * Hibernate version column
    */
   @SuppressWarnings("unused")
   @Version
   @Column(name = "VERSION", nullable = false)
   private Integer version = 0;

   /**
    * Name of the filter rule, never <code>null</code> or empty after
    * construction
    */
   @Basic
   @NaturalId(mutable = true)
   @Column(name = "NAME", unique=true)
   private String name;

   /**
    * Description of the rule, may be <code>null</code> or empty
    */
   @Basic
   private String description;

   /**
    * The associated authtype, may be <code>null</code>
    */
   @Basic
   private Integer legacy_authtype;

   /**
    * The filter is an agregation of rules to be applied to the items being
    * filtered.
    */
   @OneToMany(targetEntity = PSItemFilterRuleDef.class, cascade =
   {CascadeType.ALL,CascadeType.MERGE}, fetch = FetchType.EAGER, mappedBy = "filter", orphanRemoval = true)
   @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSItemFilter_Rules")
   @Fetch(FetchMode.SUBSELECT)
   private Set<IPSItemFilterRuleDef> rules = new HashSet<>();

   /**
    * Item filters can be changed, this member points to the parent filter,
    * if there is one
    */
   @ManyToOne(targetEntity = PSItemFilter.class, cascade =
   {CascadeType.PERSIST})
   @JoinColumn(name = "PARENT_FILTER_ID")
   private IPSItemFilter parentFilter;

   /**
    * Default ctor for use by Hibernate
    */
   public PSItemFilter() {
      //
   }

   /**
    * Ctor allocates a new id for the new item filter
    *
    * @param name name of the filter, never <code>null</code> or empty
    * @param description the description, optional
    */
   public PSItemFilter(String name, String description,IPSGuid ipsGuid) {
      if (StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      this.name = name;
      this.description = description;
      if(ipsGuid == null) {
         this.filter_id = PSGuidHelper.generateNext(PSTypeEnum.ITEM_FILTER)
                 .longValue();
      }else{
         this.filter_id = ipsGuid.longValue();
      }
   }

   /**
    * Ctor allocates a new id for the new item filter
    * 
    * @param name name of the filter, never <code>null</code> or empty
    * @param description the description, optional
    */
   public PSItemFilter(String name, String description) {
      this(name,description,null);
   }
   
   /**
    * Performs a shallow copy, merging entries from the supplied source, 
    * ignores id and version.
    * 
    * @param source the filter to merge with, not <code>null</code>.
    * @throws PSFilterException 
    */
   public void merge(IPSItemFilter source) throws PSFilterException
   {
      if (source == null)
         throw new IllegalArgumentException("source cannot be null");

      setName(source.getName());
      setDescription(source.getDescription());
      setLegacyAuthtypeId(source.getLegacyAuthtypeId());
      setParentFilter(source.getParentFilter());
      mergeRules(source);
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.catalog.IPSCatalogSummary#getName()
    */
   public String getName()
   {
      return name;
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#setName(java.lang.String)
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return Returns the parentFilter.
    */
   @IPSXmlSerialization(suppress = true)
   public IPSItemFilter getParentFilter()
   {
      return parentFilter;
   }

   /**
    * @param parentFilter The parentFilter to set.
    */
   public void setParentFilter(IPSItemFilter parentFilter)
   {
      this.parentFilter = parentFilter;
   }

   /**
    * Get the parent filter id for xml serialization
    * @return the parent id or <code>null</code>
    */
   public IPSGuid getParentFilterId()
   {
      if (parentFilter != null)
         return parentFilter.getGUID();
      else
         return null;
   }

   /**
    * Set the parent filter id for xml serialization
    * @param parentId the parent id, or <code>null</code>
    */
   public void setParentFilterId(IPSGuid parentId)
   {
      if (parentId == null)
         parentFilter = null;
      else
      {
         IPSFilterService svc = PSFilterServiceLocator.getFilterService();
         List<IPSGuid> ids = new ArrayList<>();
         ids.add(parentId);
         try {
            List<IPSItemFilter> filters = svc.loadFilter(ids);
            parentFilter = filters.get(0);
         } catch (PSNotFoundException e) {
            log.warn("Unable to load parent Item Filter: {} Error: {}",parentId,e.getMessage());
         }
      }
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.catalog.IPSCatalogSummary#getDescription()
    */
   public String getDescription()
   {
      return description;
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#setDescription(java.lang.String)
    */
   public void setDescription(String description)
   {
      this.description = description;
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#getLegacyAuthtypeId()
    */
   public Integer getLegacyAuthtypeId()
   {
      return legacy_authtype;
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#setLegacyAuthtypeId(java.lang.Integer)
    */
   public void setLegacyAuthtypeId(Integer authTypeId)
   {
      legacy_authtype = authTypeId;
   }
   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#setRuleDefs(java.util.Set)
    */
   public void setRuleDefs(Set<IPSItemFilterRuleDef> ruleDefs)
   {
      rules.clear();
      if (ruleDefs != null)
      {
         ruleDefs.forEach(k ->
         {
            k.setFilter(this);
            rules.add(k);
         });

      }
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#getRuleDefs()
    */
   public Set<IPSItemFilterRuleDef> getRuleDefs()
   {
      return rules;
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.catalog.IPSCatalogItem#toXML()
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.catalog.IPSCatalogItem#fromXML(java.lang.String)
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      Object copy = PSXmlSerializationHelper.readFromXML(xmlsource, this);      
      // above convert null to integer 0, reset to work around the issue
      this.legacy_authtype = ((PSItemFilter)copy).legacy_authtype;
   }
   
   /**
    * Get the guid for xml serialization
    * @return the guid, never <code>null</code>
    */
   public IPSGuid getGUID()
   {
      return PSGuidUtils.makeGuid(filter_id, PSTypeEnum.ITEM_FILTER);
   }
   
   /**
    * Set the guid for xml serialization
    * @param newguid the new guid, never <code>null</code>
    * @throws IllegalStateException 
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException
   {
      if (newguid == null)
      {
         throw new IllegalArgumentException("newguid may not be null");
      }
      filter_id = newguid.longValue();
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#addRuleDef(com.percussion.services.filter.IPSItemFilterRuleDef)
    */
   public void addRuleDef(IPSItemFilterRuleDef def)
   {
      def.setFilter(this);
      rules.add(def);
   }

   /**
    *  (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#removeRuleDef(com.percussion.services.filter.IPSItemFilterRuleDef)
    */
   public void removeRuleDef(IPSItemFilterRuleDef def)
   {
      rules.remove(def);
   }
   
   /**
    * @return Returns the version.
    */
   @IPSXmlSerialization(suppress=true)
   public Integer getVersion()
   {
      return version;
   }

   /**
    * @param version The version to set.
    */
   public void setVersion(Integer version)
   {
      this.version = version;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSItemFilter)) return false;
      PSItemFilter that = (PSItemFilter) o;
      return Objects.equals(name, that.name);
   }

   @Override
   public int hashCode() {
      return Objects.hash(name);
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSItemFilter{");
      sb.append("filter_id=").append(filter_id);
      sb.append(", version=").append(version);
      sb.append(", name='").append(name).append('\'');
      sb.append(", description='").append(description).append('\'');
      sb.append(", legacy_authtype=").append(legacy_authtype);
      sb.append(", rules=").append(rules);
      if(parentFilter!=null) {
         sb.append(", parentFilter=").append(parentFilter.getGUID());
      }else{
         sb.append(", parentFilter=null");
      }
      sb.append('}');
      return sb.toString();
   }

   /* (non-Javadoc)
    * @see com.percussion.services.filter.IPSItemFilter#filter(java.util.List, java.util.Map)
    */
   public List<IPSFilterItem> filter(List<IPSFilterItem> items,
         Map<String, String> params) throws PSFilterException
   {
      SortedSet<IPSItemFilterRuleDef> sortedDefs = new TreeSet<>();
      sortedDefs.addAll(rules);

      // Add any parent rules, stop after 100 cycles to avoid tracing a cycle
      // all parent rules are added to allow normal sort behavior to sort all
      // rules at once
      int count = 0;
      IPSItemFilter parent = parentFilter;
      while (parent != null)
      {
         sortedDefs.addAll(parent.getRuleDefs());
         parent = parent.getParentFilter();
         if (count++ > 100)
         {
            throw new PSFilterException(IPSFilterServiceErrors.PROBABLE_CYCLE,
                  name);
         }
      }

      // Run the sorted definitions one at a time, stopping if we run out
      // of items
      Map<String, String> ruleparams = new HashMap<>();
      for (IPSItemFilterRuleDef def : sortedDefs)
      {
         if (items.size() == 0)
            break;

         // Build parameters
         ruleparams.clear();
         ruleparams.putAll(def.getParams());
         if (params != null)
         {
            ruleparams.putAll(params);
         }
         // Run the rule
         items = def.getRule().filter(items, ruleparams);
      }

      return items;
   }

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getLabel()
    */
   public String getLabel()
   {
      return getName();
   }
   
   /* (non-Javadoc)
    * @see com.percussion.services.data.IPSCloneTuner#tuneClone(long)
    */
   public Object tuneClone(long newId)
   {
      filter_id = newId;
      Iterator<IPSItemFilterRuleDef> ruleDefs = rules.iterator();
      while (ruleDefs.hasNext())
      {
         PSItemFilterRuleDef rulDef = (PSItemFilterRuleDef) ruleDefs.next();
         rulDef.setFilter(this);
      }
      //TODO missing anything???
      return this;
   }
   
   /**
    * Given a collection of rules, sync the existing collection with 
    * the new rules. This method will add/subtract to reflect the new
    * collection. It also needs to manage the parameters for each modified
    * rule.
    * 
    * @param src  the filter with edited rules, never <code>null</code>.
    * @throws PSFilterException 
    */
   public void mergeRules(IPSItemFilter src) throws PSFilterException
   {
      if (src == null)
      {
         throw new IllegalArgumentException("src may not be null");
      }
      
      Set<String> sourceRuleNames = new HashSet<>();
      Map<String,IPSItemFilterRuleDef> oldRuleNameMap = 
         new HashMap<>();
      
      for(IPSItemFilterRuleDef def : src.getRuleDefs())
      {
         sourceRuleNames.add(def.getRuleName());
      }

      // Check old rules, remove if they no longer belong. When this loop 
      // is done, oldRuleNamesMap will contain the old rules that are still
      // present
      Iterator<IPSItemFilterRuleDef> iter = getRuleDefs().iterator();
      while(iter.hasNext())
      {
         IPSItemFilterRuleDef def = iter.next();
         if (sourceRuleNames.contains(def.getRuleName()))
         {
            oldRuleNameMap.put(def.getRuleName(),def);
         }
         else
         {
            iter.remove();
         }
      }
      
      // Now, for each new rule def, either add as new, or modify the old
      // to have the same parameters
      for(IPSItemFilterRuleDef def : src.getRuleDefs())
      {
         if (oldRuleNameMap.containsKey(def.getRuleName()))
         {
            PSItemFilterRuleDef existing = (PSItemFilterRuleDef) 
               oldRuleNameMap.get(def.getRuleName());
            Map<String,String> existingParams = existing.getParams();
            Map<String,String> newParams = def.getParams();
            for(String pname : existingParams.keySet())
            {
               String newvalue = newParams.get(pname);
               if (newvalue != null)
               {
                  existing.setParam(pname, newvalue);
               }
               else
               {
                  existing.removeParam(pname);
               }
            }
            for(String pname : newParams.keySet())
            {
               if (! existingParams.containsKey(pname))
               {
                  existing.addParam(pname, newParams.get(pname));
               }
            }
         }
         else
         {
            addRuleDef(def); // New
         }
      }
   }

}
