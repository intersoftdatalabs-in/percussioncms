// JAVA_11_REFACTORED: This class has been modernized with Java 11 features by Sunny Sal
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

package com.percussion.services.relationship.impl;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipPropertyData;
import com.percussion.error.PSException;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.relationship.IPSRelationshipService;
import com.percussion.services.relationship.data.PSRelationshipConfigName;
import com.percussion.services.relationship.data.PSRelationshipData;
import com.percussion.system.utils.PSSiteManageBean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

/**
 * This is the Hibernate implementation of the {@code IPSRelationshipService}.
 * This service provides comprehensive relationship management capabilities for
 * the Percussion CMS system, utilizing modern Java 11 features and best practices.
 *
 * @author Original Author
 * @since Java 11 Modernization
 */
@PSSiteManageBean("sys_relationshipService")
@Transactional
public class PSRelationshipService implements IPSRelationshipService {

   /**
    * The maximum number of elements in an IN Clause. This number cannot be
    * bigger than 1000, which is the limit of the IN Clause for Oracle.
    */
   private static final int MAX_NUM_OF_IN_CLAUSE = 999;

   private static final Logger logger = LogManager.getLogger(PSRelationshipService.class);

   @PersistenceContext
   private EntityManager entityManager;

   private Session getSession() {
      return entityManager.unwrap(Session.class);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Optional<PSRelationship> loadRelationship(int id) throws PSException {
      loadConfigs(); // load configs if needed

      var rdata = getSession().get(PSRelationshipData.class, id);
      if (rdata != null && setConfigAddChildProperties(rdata, null, false, false)) {
         return Optional.of(getRelationship(rdata));
      }
      return Optional.empty();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<Integer> findPersistedRid(Collection<Integer> testedIds) {
      if (testedIds == null) {
         throw new IllegalArgumentException("testedIds may not be null.");
      }

      // process all together if less than the max
      if (testedIds.size() < MAX_NUM_OF_IN_CLAUSE) {
         return findPersistedRids(testedIds);
      }

      // otherwise, process the IDs in groups using modern Java streams
      var returnIds = new ArrayList<Integer>();
      var groupIds = new ArrayList<Integer>();

      for (var rid : testedIds) {
         groupIds.add(rid);
         if (groupIds.size() == MAX_NUM_OF_IN_CLAUSE) {
            returnIds.addAll(findPersistedRids(groupIds));
            groupIds.clear();
         }
      }

      // process whatever is left
      if (!groupIds.isEmpty()) {
         returnIds.addAll(findPersistedRids(groupIds));
      }

      return returnIds;
   }

   @Override
   public List<Integer> findPersistedRelationshipIds(Collection<Integer> candidateIds) {
      return findPersistedRid(candidateIds);
   }

   /**
    * The same as {@link #findPersistedRid(Collection)}, except the number
    * of the IDs is assumed less than or equal to {@link #MAX_NUM_OF_IN_CLAUSE}.
    *
    * @param testedIds the IDs to test, never {@code null}
    * @return list of persisted relationship IDs, never {@code null}
    */
   private List<Integer> findPersistedRids(Collection<Integer> testedIds) {
      if (testedIds.isEmpty()) {
         return Collections.emptyList();
      }

      return getSession()
         .createQuery("select r.rid from PSRelationshipData as r where r.rid in (:rids)", Integer.class)
         .setParameterList("rids", testedIds)
         .list();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PSRelationship> findByFilter(PSRelationshipFilter filter) throws PSException {
      List<PSRelationship> rels = null;
      loadConfigs(); // load configs if needed

      // execute the query and load the data
      var sess = getSession();
      var filters = getProcessedFilters(filter);

      for (var f : filters) {
         // query with HQL, which may be slower than straight JDBC
         // qry = new PSHQLQueryHelper(filter, m_configMap,  m_nameMapToId);
         // query with straight JDBC, which should be faster than HQL
         var qry = new PSHQLQueryHelper(f, m_configMap, m_nameMapToId);

         var relsData = qry.executeQuery(sess);

         if (rels == null) {
            rels = postProcessResultList(relsData, qry);
         } else {
            rels.addAll(postProcessResultList(relsData, qry));
         }
      }

      return Optional.ofNullable(rels).orElse(Collections.emptyList());
   }

   /**
    * Gets a list of ready-to-be-processed filters from the specified filter.
    * A ready-to-be-processed filter cannot contain more than {@link #MAX_NUM_OF_IN_CLAUSE}.
    *
    * @param filter the source filter, assumed not {@code null}
    * @return a list of ready-to-be-processed filters, never {@code null} or empty
    */
   private List<PSRelationshipFilter> getProcessedFilters(PSRelationshipFilter filter) {
      var dependents = filter.getDependents();
      if (dependents == null || dependents.size() <= MAX_NUM_OF_IN_CLAUSE) {
         return Collections.singletonList(filter);
      }

      var filters = new ArrayList<PSRelationshipFilter>();
      var deps = new HashSet<PSLocator>();

      for (var loc : dependents) {
         deps.add(loc);
         if (deps.size() >= MAX_NUM_OF_IN_CLAUSE) {
            var f = new PSRelationshipFilter(filter);
            f.setDependents(null); // force to reset the dependents
            f.setDependents(deps);
            filters.add(f);
            deps = new HashSet<>();
         }
      }

      if (!deps.isEmpty()) {
         var f = new PSRelationshipFilter(filter);
         f.setDependents(null); // force to reset the dependents
         f.setDependents(deps);
         filters.add(f);
      }

      return filters;
   }

   /**
    * Post process the supplied query result. It set the related relationship
    * config object for each entity and perform further filtering based on
    * the filter criteria.
    *
    * @param relsData the query result, assumed not <code>null</code>.
    * @param qryHelper the query helper object, used to perform additional
    *   filtering if needed.
    *
    * @return a list of relationship objects that have been processed
    *   successfully. It may be empty, but never <code>null</code>.
    */
   private List<PSRelationship> postProcessResultList(
           List<PSRelationshipData> relsData, IPSQueryHelper qryHelper)
   {
      List<PSRelationship> rels = new ArrayList<>(relsData
              .size());
      boolean filterOwnerRev = qryHelper.mayFilterOwnerRev();
      boolean filterDepedentRev = qryHelper.mayFilterDependentRev();
      for (PSRelationshipData rdata : relsData)
      {
         if (setConfigAddChildProperties(rdata, qryHelper, filterOwnerRev,
                 filterDepedentRev))
         {
            rels.add(getRelationship(rdata));
         }
      }

      return rels;
   }

   /**
    * Creates a relationship data from this object. This is used to save
    * the object in the persistent layer.
    *
    * @return the relationship data, never <code>null</code>.
    */
   private PSRelationshipData getRelationshipData(PSRelationship rel)
   {
      PSRelationshipData data = new PSRelationshipData(
              rel.getId(), rel.getConfig(), rel.getOwner().getId(),
              rel.getOwner().getRevision(),
              rel.getDependent().getId(), rel.getDependent().getRevision());

      data.setPersisted(rel.isPersisted());

      // set user properties
      for (PSRelationshipPropertyData prop : rel.getAllUserProperties())
      {
         setUserProperty(prop, data);
      }

      return data;
   }

   /**
    * Set the supplied user property, which may be either a custom or
    * pre-defined user property.
    *
    * @param prop the to be set user property, never <code>null</code>. It may
    *    be a new or existing user property.
    */
   private void setUserProperty(PSRelationshipPropertyData prop,
                                PSRelationshipData rdata)
   {
      if (prop == null)
         throw new IllegalArgumentException("prop must not be null.");

      PSRelationshipPropertyData myprop = rdata.getProperty(prop.getName()).orElse(null);

      // if cannot find, then try one of the pre-defined user properties
      if (myprop == null)
      {
         try
         {
            // if cannot find, then try one of the pre-defined user properties
            if (prop.getName().equalsIgnoreCase(
                    PSRelationshipConfig.PDU_FOLDERID))
            {
               if (prop.getValue() != null)
                  rdata.setFolderId(Integer.parseInt(prop.getValue()));
               return;
            }

            if (prop.getName().equalsIgnoreCase(
                    PSRelationshipConfig.PDU_INLINERELATIONSHIP))
            {
               rdata.setInlineRelationship(prop.getValue());
               return;
            }

            if (prop.getName().equalsIgnoreCase(PSRelationshipConfig.PDU_WIDGET_NAME))
            {
               rdata.setWidgetName(prop.getValue());
               return;
            }

            if (prop.getName()
                    .equalsIgnoreCase(PSRelationshipConfig.PDU_SITEID))
            {
               if (prop.getValue() != null)
                  rdata.setSiteId(Long.parseLong(prop.getValue()));
               return;
            }

            if (prop.getName()
                    .equalsIgnoreCase(PSRelationshipConfig.PDU_SLOTID))
            {
               if (prop.getValue() != null)
                  rdata.setSlotId(Long.parseLong(prop.getValue()));
               return;
            }

            if (prop.getName().equalsIgnoreCase(
                    PSRelationshipConfig.PDU_SORTRANK))
            {
               if (prop.getValue() != null)
                  rdata.setSortRank(Integer.parseInt(prop.getValue()));
               return;
            }

            if (prop.getName().equalsIgnoreCase(
                    PSRelationshipConfig.PDU_VARIANTID))
            {
               if (prop.getValue() != null)
                  rdata.setVariantId(Long.parseLong(prop.getValue()));
               return;
            }
         }
         catch (NumberFormatException e)
         {
            // it must be one of the pre-defined user properties
            // with a BAD string value for integer.
            // Ignore the bad value, defaults to null
            return;
         }

         // must be a new property, just add
         rdata.addProperty(prop);
      }
      else
      {
         if (! myprop.equals(prop))
         {
            myprop.setPersisted(prop.isPersisted());
            myprop.setValue(prop.getValue());
         }
      }
   }


   /**
    * Set the relationship config for the supplied data, add (additional)
    * child properties, and performs additional filtering, such as filtering by
    * owner revision, dependent revision and child/custom properties if needed.
    *
    * @param rdata the relationship data, assumed not {@code null}.
    * @param qryHelper the query helper object, used to perform additional
    *   filtering if needed. It may be {@code null} if do not perform the
    *   additional filtering process.
    * @param filterOwnerRev {@code true} if need to consider to filter by
    *   owner revision.
    * @param filterDependentRev {@code true} if need to consider to filter by
    *   dependent revision.
    *
    * @return {@code true} if successful done the above;
    *   return {@code false} if cannot find a matching relationship config
    *   for the given relationship data or the child properties does not
    *   match the criteria of the filter.
    */
   private boolean setConfigAddChildProperties(
           PSRelationshipData rdata, IPSQueryHelper qryHelper,
           boolean filterOwnerRev, boolean filterDependentRev)
   {
      var config = m_configMap.get(rdata.getConfigId());
      if (config == null)
      {
         logger.warn("Cannot find relationship config for {}", rdata);
         return false;
      }
      else
      {
         rdata.setConfig(config);
         // filter by owner revision
         if (filterOwnerRev)
         {
            if (config.useOwnerRevision()
                    && rdata.getOwnerRevision() != qryHelper.getFilter()
                    .getOwner().getRevision())
               return false;
         }

         // filter by dependent revision
         if (filterDependentRev)
         {
            if (config.useDependentRevision()
                    && rdata.getDependentRevision() != qryHelper.getFilter()
                    .getDependent().getRevision())
               return false;
         }

         // load the additional properties if needed
         if (!config.getCustomPropertyNames().isEmpty())
         {
            var relProps = findPropertiesByRid(rdata.getId());
            if (qryHelper == null)
               rdata.setProperties(relProps);
            else if (qryHelper.filterCustomProperties(relProps))
               rdata.setProperties(relProps);
            else
               return false; // custom properties does not match criteria
         }
      }
      return true;
   }

   /* (non-Javadoc)
    * @see IPSRelationshipService#saveRelationship(PSRelationship)
    */
   @Transactional
   public void saveRelationship(PSRelationship rel) throws PSException
   {
      loadConfigs(); // load configs if needed

      if (rel == null)
         throw new IllegalArgumentException("rel may not be null");

      if (rel.getId() == -1)
      {
         int id;
         try{
            id = PSRelationshipCommandHandler.getNextId();
         }
         catch (PSCmsException e) {
            e.printStackTrace();
            throw new RuntimeException(e); // should never happen;
         }
         rel.setId(id);
         rel.setPersisted(false);
      }

      PSRelationshipData rdata = getRelationshipData(rel);
      Session sess = getSession();

      // update config id if needed
      if (rdata.getConfigId() == -1)
      {
         Integer configId = m_nameMapToId.get(rdata.getConfig()
                 .getName());

         if (configId == null) // this is not possible
            throw new IllegalStateException(
                    "Unknown relationship configuration name: "
                            + rdata.getConfig().getName());
         rdata.setConfigId(configId);
      }

      // do save
      if (!rdata.isPersisted())
         sess.persist(rdata);
      else
         rdata = (PSRelationshipData) sess.merge(rdata);

      saveOrUpdateRelationshipProperties(rdata, sess);

      rel.setPersisted(true);


   }

   /* (non-Javadoc)
    * @see IPSRelationshipService#saveRelationship(Collection<PSRelationship>)
    */
   @Transactional
   public void saveRelationship(Collection<PSRelationship> rdatas) throws PSException
   {
      if (rdatas == null || rdatas.isEmpty())
         throw new IllegalArgumentException("rdatas may not be null or empty");

      // don't call getHibernateTemplate().saveOrUpdateAll(rdatas), but use
      // saveRelationshipData(PSRelationshipData) instead since it will not
      // do the actual save if it not dirty.
      for (PSRelationship rdata : rdatas)
      {
         saveRelationship(rdata);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void saveRelationships(Collection<PSRelationship> relationships) throws PSException {
      if (relationships == null || relationships.isEmpty())
         throw new IllegalArgumentException("relationships may not be null or empty");
      saveRelationship(relationships);
   }

   /* (non-Javadoc)
    * @see IPSRelationshipService#deleteRelationship(PSRelationship)
    */
   @Transactional
   public void deleteRelationship(PSRelationship rdata)
   {
      if (rdata == null)
         throw new IllegalArgumentException("rdata may not be null");

      deleteRelationshipByRid(rdata.getId());
   }

   /* (non-Javadoc)
    * @see IPSRelationshipService#deleteRelationship(Collection<PSRelationship>)
    */
   @Transactional
   public void deleteRelationship(Collection<PSRelationship> rdatas)
   {
      if (rdatas == null || rdatas.isEmpty())
         throw new IllegalArgumentException("rdatas may not be null or empty");

      for (PSRelationship rdata : rdatas)
         deleteRelationship(rdata);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void deleteRelationships(Collection<PSRelationship> relationships) {
      if (relationships == null || relationships.isEmpty())
         throw new IllegalArgumentException("relationships may not be null or empty");
      for (PSRelationship r : relationships) {
         deleteRelationship(r);
      }
   }

   /* (non-Javadoc)
    * @see IPSRelationshipService#deleteRelationshipById(int)
    */
   @Transactional
   public int deleteRelationshipByRid(int rid)
   {
      //getHibernateTemplate().de.deleteAll(rdatas);
      Session sess = getSession();
      int count;


      MutationQuery sql = sess.createMutationQuery(DELETE_PROPERTIES_BY_RID_HQL);
      sql.setParameter("rid", rid);
      sql.executeUpdate();

      sql = sess.createMutationQuery(DELETE_RELATIONSHIP_BY_RID_HQL);
      sql.setParameter("rid", rid);
      count = sql.executeUpdate();


      return count;
   }

   /**
    * Loads the relationship configurations if it has not been loaded; otherwise
    * do nothing.
    *
    * @throws PSException if failed to load the relationship configurations.
    */
   private void loadConfigs() throws PSException
   {
      if (m_configMap == null || m_nameMapToId == null)
      {
         reloadConfigs();
      }
   }

   /* (non-Javadoc)
    * @see IPSRelationshipService#reloadConfigs()
    */
   public void reloadConfigs() throws PSException
   {
      Map<Integer, PSRelationshipConfig> configMap = null;
      Map<String, Integer> nameMapToId = null;

      // load the configs from repository if has not done yet.
      PSRelationshipCommandHandler.loadConfigs();

      // initialize the m_configMap
      configMap = new HashMap<>();
      nameMapToId = new HashMap<>();

      IPSCmsObjectMgr objMgr = PSCmsObjectMgrLocator.getObjectManager();
      var configNames = objMgr.findAllRelationshipConfigNames().collect(java.util.stream.Collectors.toList());
      PSRelationshipConfig config;
      for (PSRelationshipConfigName cname : configNames)
      {
         config = PSRelationshipCommandHandler.getRelationshipConfig(cname
                 .getName());
         if (config != null)
         {
            configMap.put(cname.getId(), config);
            nameMapToId.put(cname.getName(), cname.getId());
         }
         else
         {
            logger.warn("Cannot find relationship configuration from {}",
                     cname);
         }
      }

      // expecting very infrequent usage of this call, so we don't
      // synchronize the access of these 2 variables.
      m_configMap = configMap;
      m_nameMapToId = nameMapToId;
   }

   @Override
   public void reloadConfigurations() throws PSException {
      reloadConfigs();
   }

   @Override
   public List<PSRelationshipData> findByDependentId(int dependentId) {
      Session session = getSession();

      // Hibernate 6 HQL: property name dependentId (not column dependent_id)
      Query<PSRelationshipData> query = session
              .createQuery(FIND_BY_DEPENDENT_ID_HQL, PSRelationshipData.class)
              .setParameter("dependentId", dependentId);
      return  query.list();
   }

   /**
    * Gets a list of relationship properties for the supplied relationship id
    *
    * @param rid the relationship id.
    *
    * @return a list of relationship properties, never <code>null</code>, but
    * by empty.
    */

   private Collection<PSRelationshipPropertyData> findPropertiesByRid(
           int rid)
   {
      // execute the query and load the data
      Session sess = getSession();

      Query<PSRelationshipPropertyData> qry =
            sess.createQuery(FIND_PROPERTIES_BY_RID_HQL, PSRelationshipPropertyData.class);
      qry.setParameter("rid", rid);
      return  qry.list();
   }

   /**
    * Save or update the supplied properties.
    *
    * @param rdata the to be saved or updated properties, assmed not
    *    <code>null</code>, but may be empty.
    *
    * @param sess the session used to save or update to the repository. Assumed
    *    not <code>null</code> and it is closed/released by the called.
    */
   private void saveOrUpdateRelationshipProperties(
           PSRelationshipData rdata, Session sess)
   {
      if (rdata.getChildProperties().isEmpty())
         return; // do nothing

      for (PSRelationshipPropertyData prop : rdata.getChildProperties())
      {
         prop.setRid(rdata.getId()); // make sure set to correct parent id
         if (!prop.isPersisted()) // not exist in repository
         {
            sess.persist(prop);
            prop.setPersisted(true);
         }
         else
         {
            sess.merge(prop);
         }
      }
   }

   /**
    * Creates a {@link PSRelationship} object from a given relationship data
    * object.
    *
    * @param rdata the relationship data used to create the
    *   {@link PSRelationship} object. Assumed not <code>null</code>.
    *
    * @return the created {@link PSRelationship} object, never <code>null</code>.
    */
   private PSRelationship getRelationship(PSRelationshipData rdata)
   {
      PSRelationship rel = new PSRelationship(rdata.getId(),
              new PSLocator(rdata.getOwnerId(), rdata.getOwnerRevision()),
              new PSLocator(rdata.getDependentId(), rdata.getDependentRevision()),
              rdata.getConfig());
      rel.setPersisted(true);

      // set user properties
      Set<String> pnames = rdata.getConfig().getUserProperties().keySet();
      PSRelationshipPropertyData srcProp;
      for (String pname : pnames)
      {
         srcProp = getUserProperty(rdata, pname);
         if (srcProp != null)
            rel.setUserProperty(srcProp);
      }

      return rel;
   }

   /**
    * Get a specified user property from a relationship data object.
    *
    * @param r the relationship data, assumed not <code>null</code>.
    * @param name the name of the property, never <code>null</code>. This
    *   may be the name of a custom or pre-defined user property.
    *
    * @return the user property, it may be <code>null</code> if cannot find.
    */
   private PSRelationshipPropertyData getUserProperty(PSRelationshipData r,
                                                      String name)
   {
      if (name == null)
         throw new IllegalArgumentException("name may not be null.");

      PSRelationshipPropertyData retProp = r.getProperty(name).orElse(null);
      if (retProp != null)
         return retProp;

      // try the pre-defined user properties
      if (name.equalsIgnoreCase(PSRelationshipConfig.PDU_FOLDERID))
         retProp = getIntProp(PSRelationshipConfig.PDU_FOLDERID, r
                 .getFolderId());

      if (name.equalsIgnoreCase(PSRelationshipConfig.PDU_INLINERELATIONSHIP))
         retProp = new PSRelationshipPropertyData(
                 PSRelationshipConfig.PDU_INLINERELATIONSHIP, r
                 .getInlineRelationship().orElse(null));

      if (name.equalsIgnoreCase(PSRelationshipConfig.PDU_WIDGET_NAME))
         retProp = new PSRelationshipPropertyData(PSRelationshipConfig.PDU_WIDGET_NAME, r.getWidgetName().orElse(null));

      if (name.equalsIgnoreCase(PSRelationshipConfig.PDU_SITEID))
         retProp = getLongProp(PSRelationshipConfig.PDU_SITEID, r.getSiteId());

      if (name.equalsIgnoreCase(PSRelationshipConfig.PDU_SLOTID))
         retProp = getLongProp(PSRelationshipConfig.PDU_SLOTID, r.getSlotId());

      if (name.equalsIgnoreCase(PSRelationshipConfig.PDU_SORTRANK))
         retProp = getIntProp(PSRelationshipConfig.PDU_SORTRANK, r
                 .getSortRank());

      if (name.equalsIgnoreCase(PSRelationshipConfig.PDU_VARIANTID))
         retProp = getLongProp(PSRelationshipConfig.PDU_VARIANTID, r
                 .getVariantId());

      if (retProp != null)
         retProp.setPersisted(r.isPersisted());

      return retProp;
   }

   /**
    * Creates a relationship property from a name and an integer value.
    *
    * @param name the name of the property, assumed not <code>null</code> or
    *    empty.
    * @param value the value of the property. It may be <code>-1</code> if
    *    the value of this property is unknown (or <code>null</code> in the
    *    repository).
    *
    * @return the created relationship property, never <code>null</code>.
    */
   private PSRelationshipPropertyData getIntProp(String name, int value)
   {
      PSRelationshipPropertyData prop;

      if (value == -1)
         prop = new PSRelationshipPropertyData(name, null);
      else
         prop = new PSRelationshipPropertyData(name, String.valueOf(value));

      return prop;
   }

   /**
    * Creates a relationship property from a name and a long value.
    *
    * @param name the name of the property, assumed not <code>null</code> or
    *           empty.
    * @param value the value of the property. It may be <code>-1</code> if the
    *           value of this property is unknown (or <code>null</code> in the
    *           repository).
    *
    * @return the created relationship property, never <code>null</code>.
    */
   private PSRelationshipPropertyData getLongProp(String name, long value)
   {
      PSRelationshipPropertyData prop;

      if (value == -1)
         prop = new PSRelationshipPropertyData(name, null);
      else
         prop = new PSRelationshipPropertyData(name, String.valueOf(value));

      return prop;
   }

   /**
    * It maps the config id to its relationship configuration object. It is
    * initialized by {@link #loadConfigs()}.
    */
   private Map<Integer, PSRelationshipConfig> m_configMap = null;

   /**
    * It maps the config name to its config id. It is initialized by
    * {@link #loadConfigs()}.
    */
   private Map<String, Integer> m_nameMapToId = null;

   /**
    * The logger for this class.
    */
   private static final Logger ms_logger = LogManager.getLogger("RelationshipService");

   /*

   public List<PSRelationshipData> findByCriteria(int ownderId)
   {
      Session sess = getSession();
      try
      {
         List rels = sess.createCriteria(PSRelationshipData.class).add(
               Restrictions.eq("owner_id", Integer.valueOf(ownderId))).list();
         return rels;
      }
      finally
      {
         releaseSession(sess);
      }
   }

   public List<PSRelationshipData> findByJDBC(int ownerId)
   {
      Connection conn = null;
      PreparedStatement stmt = null;
      ResultSet rs = null;
      List<PSRelationshipData> dataList = new ArrayList<PSRelationshipData>();

      Session sess = getSession();
      try
      {
         String qryString = "select r.rid,r.config_id,r.owner_id,r.owner_revision,r.dependent_id,r.dependent_revision,r.slot_id,r.sort_rank,r.variant_id,r.folder_id,r.site_id,r.inline_relationship from rxrhino.dbo.PSX_OBJECTRELATIONSHIP as r where r.owner_id=?";

         conn = sess.connection();
         stmt = PSPreparedStatement.getPreparedStatement(conn, qryString);
         stmt.setInt(1, ownerId);
         rs = stmt.executeQuery();

         PSRelationshipData rdata;
         while (rs.next())
         {
            rdata = new PSRelationshipData();

            rdata.setId(rs.getInt(1));
            rdata.setConfigId(rs.getInt(2));
            rdata.setOwnerId(rs.getInt(3));
            rdata.setOwnerRevision(rs.getInt(4));
            rdata.setDependentId(rs.getInt(5));
            rdata.setDependentRevision(rs.getInt(6));
            rdata.setSlotId(rs.getLong(7));
            rdata.setSortRank(rs.getInt(8));
            rdata.setVariantId(rs.getLong(9));
            rdata.setFolderId(rs.getInt(10));
            rdata.setSiteId(rs.getLong(11));
            rdata.setInlineRelationship(rs.getString(12));

            dataList.add(rdata);
         }

         return dataList;
      }
      catch (SQLException e)
      {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
      finally
      {
         if (null != rs)
            try
            {
               rs.close();
            }
            catch (Exception e)
            {
            };
         if (null != stmt)
            try
            {
               stmt.close();
            }
            catch (Exception e)
            {
            };
         if (conn != null)
         {
            try
            {
               conn.close();
            }
            catch (SQLException e)
            {
               // ignore, should not happen here.
               e.printStackTrace();
            }
         }
         releaseSession(sess);
      }
   }
   */
   @Override
   @Transactional
   public void updateRelationshipData(
           PSRelationshipData rdata)
   {
      Session session = getSession();
      session.merge(rdata);
   }

   @Override
   public List<PSRelationshipData> findByDependentIdConfigId(int dependentId, int configId) {
      Session session = getSession();

      // Hibernate 6 HQL: property names dependentId / configId (not SQL columns)
      Query<PSRelationshipData> query = session
              .createQuery(FIND_BY_DEPENDENT_AND_CONFIG_HQL, PSRelationshipData.class)
              .setParameter("dependentId", dependentId)
              .setParameter("configId", configId);
      return query.list();

   }

   @Override
   public List<PSRelationshipData> findByDependentIdAndConfigId(int dependentId, int configId) {
      return findByDependentIdConfigId(dependentId, configId);
   }

   @Override
   public int deleteRelationshipById(int relationshipId) {
      return deleteRelationshipByRid(relationshipId);
   }

   /** HQL for typed unit tests (issue #3265). */
   public static final String DELETE_PROPERTIES_BY_RID_HQL =
         "delete from PSRelationshipPropertyData p where p.m_rid = :rid";

   public static final String DELETE_RELATIONSHIP_BY_RID_HQL =
         "delete from PSRelationshipData r where r.rid = :rid";

   public static final String FIND_BY_DEPENDENT_ID_HQL =
         "from PSRelationshipData where dependentId = :dependentId";

   public static final String FIND_PROPERTIES_BY_RID_HQL =
         "select r from PSRelationshipPropertyData r where r.m_rid = :rid";

   public static final String FIND_BY_DEPENDENT_AND_CONFIG_HQL =
         "from PSRelationshipData where dependentId = :dependentId and configId = :configId";
}
