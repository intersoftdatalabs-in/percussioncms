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

package com.percussion.delivery.metadata.rdbms.impl;

import com.google.common.collect.Lists;
import com.percussion.delivery.metadata.IPSMetadataDao;
import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

// TODO: Remove me @SuppressFBWarnings("UNSAFE_HASH_EQUALS")
@Repository
@Scope("singleton")
public class PSMetadataDao implements IPSMetadataDao {

  private SessionFactory sessionFactory;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /** Logger for this class. */
  private static final Logger log = LogManager.getLogger(PSMetadataDao.class);

  private static final PSHashCalculator hashCalculator = new PSHashCalculator();

  private final Pattern patternToGetDirectoryFromPagepath = Pattern.compile("(.+)/[^/]+");

  public void delete(Collection<String> pagepaths) {
    Validate.notNull(pagepaths, "pagepaths cannot be null.");

    Collection<String> pagepathHashes = getPagepathHashes(pagepaths);

    Transaction tx = null;
    try (Session session = getSession()) {
      String hql = "delete from PSDbMetadataEntry  where pagepathHash in (:paths)";
      tx = session.beginTransaction();
      Query q = session.createQuery(hql);
      q.setParameterList("paths", pagepathHashes);
      q.executeUpdate();
      tx.commit();
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  public boolean delete(String pagepath) {
    Validate.notEmpty(pagepath, "pagepath cannot be null or empty.");

    PSDbMetadataEntry entry = (PSDbMetadataEntry) findEntry(pagepath);

    if (entry != null) {
      Transaction tx = null;
      try (Session session = getSession()) {
        tx = session.beginTransaction();
        session.delete(entry);
        tx.commit();
        return true;
      } catch (Exception e) {
        if (tx != null && tx.isActive()) {
          tx.rollback();
        }
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }
    return false;
  }

  @Override
  public void deleteBySite(String prevSiteName, String newSiteName) {
    Validate.notEmpty(prevSiteName, "prevSiteName cannot be null or empty.");
    Validate.notEmpty(newSiteName, "newSiteName cannot be null or empty.");

    log.debug("Removing entries for site: {}", prevSiteName);

    Transaction tx = null;
    try (Session session = getSession()) {
      tx = session.beginTransaction();
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaDelete<PSDbMetadataEntry> deleteQuery =
          builder.createCriteriaDelete(PSDbMetadataEntry.class);
      Root<PSDbMetadataEntry> root = deleteQuery.from(PSDbMetadataEntry.class);
      deleteQuery.where(builder.like(root.get("site"), prevSiteName));
      session.createQuery(deleteQuery).executeUpdate();
      tx.commit();
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      log.error(PSExceptionUtils.getMessageForLog(e));
    }
  }

  public void deleteAllMetadataEntries() {

    Transaction tx = null;
    try (Session session = getSession()) {

      tx = session.beginTransaction();
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaDelete<PSDbMetadataProperty> deleteQuery =
          builder.createCriteriaDelete(PSDbMetadataProperty.class);
      deleteQuery.from(PSDbMetadataProperty.class);
      session.createQuery(deleteQuery).executeUpdate();

      CriteriaBuilder builder2 = session.getCriteriaBuilder();
      CriteriaDelete<PSDbMetadataEntry> deleteQuery2 =
          builder2.createCriteriaDelete(PSDbMetadataEntry.class);
      deleteQuery2.from(PSDbMetadataEntry.class);
      session.createQuery(deleteQuery2).executeUpdate();
      tx.commit();

    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      log.error(PSExceptionUtils.getMessageForLog(e));
    }
  }

  public IPSMetadataEntry findEntry(String pagepath) {
    Validate.notEmpty(pagepath, "pagepath cannot be null nor empty");

    try (Session session = sessionFactory.openSession()) {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<PSDbMetadataEntry> criteriaQuery =
          criteriaBuilder.createQuery(PSDbMetadataEntry.class);
      Root<PSDbMetadataEntry> root = criteriaQuery.from(PSDbMetadataEntry.class);
      criteriaQuery.select(root).where(criteriaBuilder.equal(root.get("pagepath"), pagepath));
      List<PSDbMetadataEntry> resultList = session.createQuery(criteriaQuery).getResultList();
      if (!resultList.isEmpty()) {
        return (IPSMetadataEntry) resultList.get(0);
      } else return null;
    }
  }

  public List<IPSMetadataEntry> getAllEntries() {
    try (Session session = getSession()) {
      session.setHibernateFlushMode(FlushMode.MANUAL);
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<PSDbMetadataEntry> criteriaQuery =
          criteriaBuilder.createQuery(PSDbMetadataEntry.class);
      Root<PSDbMetadataEntry> root = criteriaQuery.from(PSDbMetadataEntry.class);
      List<PSDbMetadataEntry> result = session.createQuery(criteriaQuery).getResultList();
      List<IPSMetadataEntry> entries = new ArrayList<>();
      if (result != null) {
        for (PSDbMetadataEntry e : result) entries.add(e);
      }

      return entries;
    }
  }

  public void save(Collection<IPSMetadataEntry> entries) {
    Validate.notNull(entries, "entries cannot be null");

    if (entries.isEmpty()) return;

    Transaction tx = null;
    try (Session session = getSession()) {
      Collection<PSDbMetadataEntry> dbEntries = convertRestEntriesToDb(entries);
      session.setHibernateFlushMode(FlushMode.ALWAYS);

      tx = session.beginTransaction();
      try {
        // Save entries in a single transaction
        for (PSDbMetadataEntry entry : dbEntries) {
          try {
            // Load existing entry in the SAME session to allow orphanRemoval to take effect
            PSDbMetadataEntry existingEntry = null;
            {
              CriteriaBuilder cb = session.getCriteriaBuilder();
              CriteriaQuery<PSDbMetadataEntry> cq = cb.createQuery(PSDbMetadataEntry.class);
              Root<PSDbMetadataEntry> root = cq.from(PSDbMetadataEntry.class);
              cq.select(root).where(cb.equal(root.get("pagepath"), entry.getPagepath()));
              List<PSDbMetadataEntry> list = session.createQuery(cq).getResultList();
              if (!list.isEmpty()) {
                existingEntry = list.get(0);
              }
            }

            // Build a de-duplicated view of properties based on (name, valuetype, valueHash)
            LinkedHashMap<String, PSDbMetadataProperty> uniq = new LinkedHashMap<>();
            for (IPSMetadataProperty prop : entry.getProperties()) {
              PSDbMetadataProperty dbProp = (PSDbMetadataProperty) prop;
              String key = dbProp.getName() + "|" + dbProp.getValuetype() + "|" + dbProp.getHash();
              // keep first occurrence
              if (!uniq.containsKey(key)) {
                uniq.put(key, dbProp);
              }
            }

            if (existingEntry != null) {
              // Update scalar fields
              existingEntry.setFolder(entry.getFolder());
              existingEntry.setLinktext(entry.getLinktext());
              existingEntry.setName(entry.getName());
              existingEntry.setSite(entry.getSite());
              existingEntry.setType(entry.getType());

              // Replace properties on the MANAGED collection so orphanRemoval deletes old rows
              existingEntry.clearProperties();
              for (PSDbMetadataProperty dbProp : uniq.values()) {
                dbProp.setMetadataEntry(existingEntry);
                existingEntry.addProperty(dbProp);
              }
              // Explicitly merge to ensure scalar updates are persisted
              session.merge(existingEntry);
            } else {
              // New entry: ensure we persist a deduplicated property set
              // Rebuild the entry's properties with the unique set to avoid duplicate rows
              Set<IPSMetadataProperty> dedupProps = new HashSet<>(uniq.values());
              entry.setProperties(dedupProps);
              session.save(entry);
            }

            // Force flush to ensure changes are written to database
            session.flush();
          } catch (org.hibernate.NonUniqueObjectException e) {
            session.merge(entry);
            session.flush();
          }
        }
        tx.commit();
      } catch (Exception e) {
        if (tx != null && tx.isActive()) {
          tx.rollback();
        }
        throw e;
      }
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      log.error(PSExceptionUtils.getMessageForLog(e));
    }
  }

  public void save(IPSMetadataEntry entry) {
    Validate.notNull(entry, "entry cannot be null");

    save(Lists.newArrayList(entry));
  }

  public boolean hasDirtyEntries(Collection<IPSMetadataEntry> entries) {
    PSDbMetadataEntry existing;

    for (PSDbMetadataEntry entry : this.convertRestEntriesToDb(entries)) {
      existing = (PSDbMetadataEntry) findEntry(entry.getPagepath());
      if (existing != null) {
        if (isEntryDirty(existing, entry)) return true;
      }
    }

    return false;
  }

  public Set<String> getAllIndexedDirectories() {

    try (Session session = getSession()) {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
      Root<PSDbMetadataEntry> root = criteriaQuery.from(PSDbMetadataEntry.class);
      criteriaQuery.select(root.get("pagepath"));
      List<String> resultList = session.createQuery(criteriaQuery).getResultList();

      // TODO Instead of using a Pattern and regular expressions here, we may
      // use
      // the Derby built-in regular expression functionality if this is too
      // slow.
      Matcher matcher;
      Set<String> indexedDirectories = new HashSet<>();

      for (String dataEntry : resultList) {
        matcher = patternToGetDirectoryFromPagepath.matcher(dataEntry);
        matcher.find();
        indexedDirectories.add(matcher.group(1));
      }

      return indexedDirectories;
    }
  }

  // @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_UNCOMMITTED)
  public List<String> getAllSites() {
    List<String> msgFromList = null;
    try (Session session = getSession()) {
      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<String> query = cb.createQuery(String.class);
      Root<PSDbMetadataEntry> root = query.from(PSDbMetadataEntry.class);
      query.select(root.get("site")).distinct(true);

      msgFromList = session.createQuery(query).getResultList();
    }
    return msgFromList;
  }

  private Collection<String> getPagepathHashes(Collection<String> pagepaths) {
    List<String> pagepathHashes = new ArrayList<>();

    for (String pp : pagepaths) {
      pagepathHashes.add(hashCalculator.calculateHash(pp));
    }

    return pagepathHashes;
  }

  /**
   * Determine if the entries are "dirty", in other words the field values differ.
   *
   * @param existing assumed not <code>null</code>.
   * @param entry assumed not <code>null</code>.
   * @return <code>true</code> if dirty.
   */
  private boolean isEntryDirty(PSDbMetadataEntry existing, PSDbMetadataEntry entry) {
    if (!existing.equals(entry)) return true;
    int count1 = existing.getPropertyCount();
    int count2 = entry.getPropertyCount();
    if ((count1 != count2) || (count1 + count2 == 0)) return true;
    if (existing.getPropertyCount() + entry.getPropertyCount() == 0) return false;
    Set<IPSMetadataProperty> props1 = existing.getProperties();
    Set<IPSMetadataProperty> props2 = entry.getProperties();

    Map<Object, IPSMetadataProperty> propMap = new HashMap<>();
    for (IPSMetadataProperty p : props2) propMap.put(((PSDbMetadataProperty) p).getId(), p);
    for (IPSMetadataProperty prop : props1) {

      String hash1 = ((PSDbMetadataProperty) prop).getHash();
      IPSMetadataProperty p2 = propMap.get(((PSDbMetadataProperty) prop).getId());
      if (p2 == null) return true;
      String hash2 = ((PSDbMetadataProperty) p2).getHash();
      if (!hash1.equals(hash2)) return true;
    }
    return false;
  }

  /**
   * Converts a database-agnostic collection of metadata entries to Hibernate specific ones.
   *
   * @param entries A list of database-agnostic metadata entry objects. Cannot be <code>null</code>,
   *     may be empty.
   * @return A list of database specific metadata entry objects. Never <code>null</code>, may be
   *     empty.
   */
  private Collection<PSDbMetadataEntry> convertRestEntriesToDb(
      Collection<IPSMetadataEntry> entries) {
    Validate.notNull(entries, "list of metadata entries cannot be null");
    Collection<PSDbMetadataEntry> result = new ArrayList<>();

    for (IPSMetadataEntry metadataEntry : entries) {
      PSDbMetadataEntry dbMetadataEntry = new PSDbMetadataEntry();

      // Always update all fields
      dbMetadataEntry.setFolder(metadataEntry.getFolder());
      dbMetadataEntry.setLinktext(metadataEntry.getLinktext());
      dbMetadataEntry.setName(metadataEntry.getName());
      dbMetadataEntry.setPagepath(metadataEntry.getPagepath());
      dbMetadataEntry.setSite(metadataEntry.getSite());
      dbMetadataEntry.setType(metadataEntry.getType());

      // Always copy properties into fresh DB entities to avoid reusing detached/managed instances
      for (IPSMetadataProperty metadataProperty : metadataEntry.getProperties()) {
        PSDbMetadataProperty propCopy =
            new PSDbMetadataProperty(
                metadataProperty.getName(),
                metadataProperty.getValuetype(),
                metadataProperty.getValue());
        dbMetadataEntry.addProperty(propCopy);
      }

      result.add(dbMetadataEntry);
    }

    return result;
  }

  @Override
  public int updateByCategoryProperty(String oldCategoryName, String newCategoryName) {

    int updatedRows = 0;

    if (oldCategoryName == null || newCategoryName == null)
      throw new IllegalArgumentException("Old and New Category Names are required");

    ;
    Transaction tx = null;
    try (Session session = getSession()) {
      tx = session.beginTransaction();
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

      CriteriaUpdate<PSDbMetadataProperty> criteriaUpdate =
          criteriaBuilder.createCriteriaUpdate(PSDbMetadataProperty.class);
      Root<PSDbMetadataProperty> employeeRoot = criteriaUpdate.from(PSDbMetadataProperty.class);
      criteriaUpdate
          .set(employeeRoot.get("stringvalue"), newCategoryName)
          .where(
              criteriaBuilder.and(
                  criteriaBuilder.equal(employeeRoot.get("stringvalue"), oldCategoryName),
                  criteriaBuilder.equal(employeeRoot.get("name"), "perc:category")));
      updatedRows = session.createQuery(criteriaUpdate).executeUpdate();
      tx.commit();
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return updatedRows;
  }

  private Session getSession() {

    return sessionFactory.openSession();
  }
}
