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
package com.percussion.services.filter.impl;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.filter.data.PSItemFilterRuleDef;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.system.utils.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.PSInvalidXmlException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.intsof.percussioncms.auditlog.codes.CatalogErrorCodes;
import com.intsof.percussioncms.auditlog.codes.FilterServiceErrorCodes;

/**
 * Filter service manager performs CRUD operations on item filters.
 * 
 * @author dougrand
 * 
 */
@Transactional
@PSBaseBean("sys_filtermanager")
public class PSFilterManager
      implements
         IPSFilterService
{
   @PersistenceContext
   private EntityManager entityManager;

   private Session getSession(){
      return entityManager.unwrap(Session.class);
   }

   /**
    * Logger for filter manager
    */
   private static final Logger log = LogManager.getLogger(PSFilterManager.class);

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#createFilter(java.lang.String,
    *      java.lang.String)
    */
   public IPSItemFilter createFilter(String name, String description)
   {
      if (StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }

      return new PSItemFilter(name, description);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#loadFilter(java.util.List)
    */
   public List<IPSItemFilter> loadFilter(List<IPSGuid> ids) 
      throws PSNotFoundException
   {
      if (ids == null || ids.isEmpty())
      {
         throw new IllegalArgumentException("ids may not be null or empty");
      }
      List<IPSItemFilter> rval = new ArrayList<>();
      for (IPSGuid g : ids)
      {
         rval.add(loadFilter(g));
      }
      return rval;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#findFilterByName(java.lang.String)
    */
   public IPSItemFilter findFilterByName(String name) throws PSFilterException
   {
      if (StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      Session session = getSession();
      // Natural-id load is preferred; it can return null on miss (and rarely on stale
      // natural-id cache). Fall back to a name query before treating as missing —
      // package reinstall relies on finding existing unique NAME rows.
      PSItemFilter filter = session.bySimpleNaturalId(PSItemFilter.class).load(name);
      if (filter == null)
      {
         filter = session
               .createQuery("from PSItemFilter f where f.name = :name", PSItemFilter.class)
               .setParameter("name", name)
               .uniqueResult();
      }
      if (filter == null)
      {
         throw new PSFilterException(FilterServiceErrorCodes.FILTER_MISSING, name);
      }
      return filter;
   }   

   /* (non-Javadoc)
    * @see com.percussion.services.filter.IPSFilterService#findFilterByID(
    * com.percussion.utils.guid.IPSGuid)
    */
   public IPSItemFilter findFilterByID(IPSGuid id) throws PSNotFoundException {
      return loadUnmodifiableFilter(id);
   }


   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#findAllFilters()
    */
   public List<IPSItemFilter> findAllFilters()
   {
      Session s = getSession();

       Query<PSItemFilter> q = s.createQuery("from PSItemFilter", PSItemFilter.class);
       return q.list().stream().map(f -> (IPSItemFilter) f).toList();
   }
   public IPSItemFilter findFilterByAuthType(String authtype)
         throws PSFilterException
   {
      if (authtype == null)
         throw new IllegalArgumentException("authtype may not be null");

      int authTypeInt;
      try {
         authTypeInt = Integer.parseInt(authtype);
      } catch (NumberFormatException e) {
         throw new PSFilterException(FilterServiceErrorCodes.AUTHTYPE_MISSING, authtype);
      }

      Session s = getSession();

       Query<PSItemFilter> q = s.createQuery("from PSItemFilter where legacy_authtype = :authtype", PSItemFilter.class)
               .setParameter("authtype", authTypeInt);
       List<PSItemFilter> results = q.list();
       if (results.isEmpty())
       {
          throw new PSFilterException(
                FilterServiceErrorCodes.AUTHTYPE_MISSING, authtype);
       }
       return results.get(0);
   }

   @Override
   public List<com.percussion.utils.guid.IPSGuid> applyFilter(IPSItemFilter filter, java.util.List<com.percussion.utils.guid.IPSGuid> contentIds, java.util.Map<String, Object> params)
   {
      Objects.requireNonNull(filter, "filter cannot be null");
      Objects.requireNonNull(contentIds, "contentIds cannot be null");

      java.util.List<com.percussion.services.filter.data.PSFilterItem> items = contentIds.stream()
          .map(id -> new com.percussion.services.filter.data.PSFilterItem(id, null, null))
          .toList();

      java.util.Map<String, String> stringParams = null;
      if (params != null) {
         stringParams = new java.util.HashMap<>();
         for (java.util.Map.Entry<String, Object> e : params.entrySet()) {
            stringParams.put(e.getKey(), e.getValue() == null ? null : e.getValue().toString());
         }
      }

      try {
         java.util.List<com.percussion.services.filter.IPSFilterItem> filterItems = new java.util.ArrayList<>(items);
         java.util.List<com.percussion.services.filter.IPSFilterItem> filtered = filter.filter(filterItems, stringParams);
         return filtered.stream().map(com.percussion.services.filter.IPSFilterItem::getItemId).toList();
      } catch (PSFilterException e) {
         throw new RuntimeException(e);
      }
   }
   @Transactional
   public void saveFilter(IPSItemFilter filter)
   {
      persistOrMergeFilter(filter, true);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#saveFilter(java.util.List)
    */
   @Transactional
   public void saveFilter(List<IPSItemFilter> filters)
   {
      if (filters == null)
         throw new IllegalArgumentException("filters may not be null");

      // Single flush after the batch — avoid flush-per-item when saveFilter(List)
      // is used (package install and bulk admin). Single-item save still flushes.
      for (IPSItemFilter filter : filters)
      {
         persistOrMergeFilter(filter, false);
      }
      getSession().flush();
   }

   /**
    * Persist or merge a filter. When {@code flush} is true, flush immediately so
    * constraint / optimistic-lock failures surface in this call (package install path).
    *
    * @param filter never {@code null}
    * @param flush whether to flush after the write
    */
   private void persistOrMergeFilter(IPSItemFilter filter, boolean flush)
   {
      if (filter == null)
         throw new IllegalArgumentException("filter may not be null");

      PSItemFilter f = (PSItemFilter) filter;

      Session session = getSession();
      try
      {
         if (f.getVersion() == null)
         {
            session.persist(f);
         }
         else
         {
            PSItemFilter current = null;
            try
            {
               current = (PSItemFilter) findFilterByName(f.getName());
            }
            catch (PSFilterException e)
            {
               // Only FILTER_MISSING means "insert this row". Any other filter-service
               // error must not be treated as missing — that masks DB failures and
               // produces confusing unique-constraint / UnexpectedRollbackException later.
               if (e.getErrorCode() != FilterServiceErrorCodes.FILTER_MISSING.numericCode())
               {
                  log.error("Exception finding item filter {}. Error: {}",
                          f.getName(),
                          PSExceptionUtils.getMessageForLog(e));
                  throw new RuntimeException(
                        "Failed to look up filter by name: " + f.getName(), e);
               }
            }
            if (current != null)
            {
               // Domain-merge package/edited fields onto the managed row when f is a
               // different instance. If f already is the managed row (same reference),
               // skip — caller already applied changes.
               if (current != f)
               {
                  current.merge(filter);
               }
               // Ensure persistence context tracks the managed entity (no-op if already managed)
               session.merge(current);
            } else
               session.persist(f);
         }
         if (flush)
         {
            // Surface constraint / optimistic-lock failures now (not only at outer commit)
            session.flush();
         }
      }
      catch (RuntimeException e)
      {
         log.error("Problem saving filter: {}. Error: {}" ,
                 filter.getName(),
                 PSExceptionUtils.getMessageForLog(e));
         throw e;
      }
      catch (Exception e)
      {
         log.error("Problem saving filter: {}. Error: {}" ,
                 filter.getName(),
                 PSExceptionUtils.getMessageForLog(e));
         throw new RuntimeException(e);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#deleteFilter(com.percussion.services.filter.IPSItemFilter)
    */
   public void deleteFilter(IPSItemFilter filter)
   {
      if (filter == null)
      {
         throw new IllegalArgumentException("filter may not be null");
      }

      getSession().remove(filter);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#deleteFilter(java.util.List)
    */
   public void deleteFilter(List<IPSItemFilter> filters)
   {
      if (filters == null)
      {
         throw new IllegalArgumentException("filters may not be null");
      }

      for (IPSItemFilter filter : filters)
      {
         deleteFilter(filter);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.filter.IPSFilterService#createRuleDef(java.lang.String,
    *      java.util.Map)
    */
   public IPSItemFilterRuleDef createRuleDef(String rule,
         Map<String, String> params)
   {
      if (rule == null)
      {
         throw new IllegalArgumentException("rule may not be null");
      }
      if (params == null)
      {
         throw new IllegalArgumentException("params may not be null");
      }
      IPSItemFilterRuleDef rval = new PSItemFilterRuleDef();
      rval.setRule(rule);
      for (Map.Entry<String, String> entry : params.entrySet())
      {
         rval.setParam(entry.getKey(), entry.getValue());
      }
      return rval;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.catalog.IPSCataloger#getTypes()
    */
   public PSTypeEnum[] getTypes()
   {
      return new PSTypeEnum[]
      {PSTypeEnum.ITEM_FILTER};
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.catalog.IPSCataloger#getSummaries(com.percussion.services.catalog.PSTypeEnum)
    */
   public List<IPSCatalogSummary> getSummaries(PSTypeEnum type)
   {
      List<IPSCatalogSummary> rval = new ArrayList<>();

      Session s = getSession();

         if (type.getOrdinal() == PSTypeEnum.ITEM_FILTER.getOrdinal())
         {
            Query<PSItemFilter> q = s.createQuery("from PSItemFilter", PSItemFilter.class);
            List<PSItemFilter> results = q.list();
            for (PSItemFilter f : results)
            {
               rval.add(new PSObjectSummary(f.getGUID(), f.getName(), f.getName(), f.getDescription()));
            }
         }

      return rval;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.catalog.IPSCataloger#loadByType(com.percussion.services.catalog.PSTypeEnum,
    *      java.lang.String)
    */
   @Transactional
   public void loadByType(PSTypeEnum type, String item)
         throws PSCatalogException
   {
      try
      {
         if (type.equals(PSTypeEnum.ITEM_FILTER))
         {
            IPSGuid guid = PSXmlSerializationHelper.getIdFromXml(
                  PSTypeEnum.ITEM_FILTER, item);
            IPSItemFilter temp;
            List<IPSGuid> guids = new ArrayList<>();
            guids.add(guid);
            temp = loadFilter(guids).get(0);

            temp.fromXML(item);
            saveFilter(temp);
         }
         else
         {
            throw new PSCatalogException(CatalogErrorCodes.UNKNOWN_TYPE, type
                  .toString());
         }
      }
      catch (IOException | PSNotFoundException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.IO, e, type);
      }
      catch (SAXException | PSInvalidXmlException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.XML, e, item);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.catalog.IPSCataloger#saveByType(com.percussion.utils.guid.IPSGuid)
    */
   public String saveByType(IPSGuid id) throws PSCatalogException
   {
      try
      {

         if (id.getType() == PSTypeEnum.ITEM_FILTER.getOrdinal())
         {
            List<IPSGuid> ids = new ArrayList<>();
            ids.add(id);
            IPSItemFilter temp = loadFilter(ids).get(0);
            return temp.toXML();
         }
         else
         {
            PSTypeEnum type = PSTypeEnum.valueOf(id.getType());
            throw new PSCatalogException(CatalogErrorCodes.UNKNOWN_TYPE, type);
         }
      }
      catch (IOException | PSNotFoundException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.IO, e, id);
      }
      catch (SAXException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.TOXML, e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSFilterService#findFiltersByName(String)
    */
   public List<IPSItemFilter> findFiltersByName(String name)
   {
      Session s = getSession();
      FilterNameLookup lookup = classifyFilterNameLookup(name);
      if (lookup == FilterNameLookup.ALL)
      {
         Query<PSItemFilter> q =
             s.createQuery("from PSItemFilter order by name", PSItemFilter.class);
         q.setCacheable(true);
         return new ArrayList<>(q.list());
      }
      if (lookup == FilterNameLookup.LIKE)
      {
         Query<PSItemFilter> q =
             s.createQuery(
                 "from PSItemFilter f where f.name like :pattern order by name",
                 PSItemFilter.class);
         q.setParameter("pattern", name);
         q.setCacheable(true);
         return new ArrayList<>(q.list());
      }
      try
      {
         return List.of(findFilterByName(name));
      }
      catch (PSFilterException e)
      {
         // Expected miss for create uniqueness / catalog lookup — not an error.
         log.debug("No item filter named {}", name);
         return List.of();
      }
   }

   /**
    * How {@link #findFiltersByName(String)} should query. Blank or {@code %}
    * lists all filters. A pattern containing {@code %} uses SQL {@code LIKE}.
    * Anything else is an exact name: missing names return empty, they must not
    * fall through to the full catalog (that made every new filter look like a
    * duplicate to {@code createItemFilters}).
    */
   enum FilterNameLookup
   {
      ALL,
      LIKE,
      EXACT
   }

   static FilterNameLookup classifyFilterNameLookup(String name)
   {
      if (StringUtils.isBlank(name) || "%".equals(name))
      {
         return FilterNameLookup.ALL;
      }
      if (name.indexOf('%') >= 0)
      {
         return FilterNameLookup.LIKE;
      }
      return FilterNameLookup.EXACT;
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSFilterService#loadFilter(IPSGuid)
    */
   public IPSItemFilter loadFilter(IPSGuid id) throws PSNotFoundException
   {
      Session session = getSession();

         IPSItemFilter filter =  session.get(PSItemFilter.class,
               id.longValue());
         if (filter == null)
            throw new PSNotFoundException(id);

         return filter;

   }

   public IPSItemFilter loadUnmodifiableFilter(IPSGuid id)
         throws PSNotFoundException
   {
      if (id == null)
      {
         throw new IllegalArgumentException("id may not be null");
      }

      return loadFilter(id);
   }
}
