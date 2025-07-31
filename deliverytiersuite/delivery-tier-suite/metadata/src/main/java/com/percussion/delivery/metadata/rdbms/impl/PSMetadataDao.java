// REFACTORED: CP-JAVA11

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

package com.percussion.delivery.metadata.rdbms.impl;

import com.google.common.collect.Lists;
import com.percussion.delivery.metadata.IPSMetadataDao;
import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import com.percussion.error.PSExceptionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressFBWarnings("UNSAFE_HASH_EQUALS")
@Repository
@Scope("singleton")
public class PSMetadataDao implements IPSMetadataDao {

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private static final Logger log = LogManager.getLogger(PSMetadataDao.class);
    private static final PSHashCalculator hashCalculator = new PSHashCalculator();
    private final Pattern patternToGetDirectoryFromPagepath = Pattern.compile("(.+)/[^/]+");

    @Override
    public void delete(Collection<String> pagepaths) {
        Validate.notNull(pagepaths, "pagepaths cannot be null.");
        var pagepathHashes = getPagepathHashes(pagepaths);

        Transaction tx = null;
        try (var session = getSession()) {
            var hql = "delete from PSDbMetadataEntry where pagepathHash in (:paths)";
            tx = session.beginTransaction();
            var q = session.createQuery(hql);
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

    @Override
    public boolean delete(String pagepath) {
        Validate.notEmpty(pagepath, "pagepath cannot be null or empty.");
        var entry = (PSDbMetadataEntry) findEntry(pagepath);

        if (entry != null) {
            Transaction tx = null;
            try (var session = getSession()) {
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
        try (var session = getSession()) {
            tx = session.beginTransaction();
            var builder = session.getCriteriaBuilder();
            var deleteQuery = builder.createCriteriaDelete(PSDbMetadataEntry.class);
            var root = deleteQuery.from(PSDbMetadataEntry.class);
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

    @Override
    public void deleteAllMetadataEntries() {
        Transaction tx = null;
        try (var session = getSession()) {
            tx = session.beginTransaction();
            var builder = session.getCriteriaBuilder();
            var deleteQuery = builder.createCriteriaDelete(PSDbMetadataProperty.class);
            deleteQuery.from(PSDbMetadataProperty.class);
            session.createQuery(deleteQuery).executeUpdate();

            var builder2 = session.getCriteriaBuilder();
            var deleteQuery2 = builder2.createCriteriaDelete(PSDbMetadataEntry.class);
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

    @Override
    public IPSMetadataEntry findEntry(String pagepath) {
        Validate.notEmpty(pagepath, "pagepath cannot be null nor empty");
        try (var session = getSession()) {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(PSDbMetadataEntry.class);
            var root = criteriaQuery.from(PSDbMetadataEntry.class);
            criteriaQuery.select(root).where(criteriaBuilder.like(root.get("pagepath"), pagepath));
            var resultList = session.createQuery(criteriaQuery).getResultList();
            return resultList.isEmpty() ? null : resultList.get(0);
        }
    }

    @Override
    public List<IPSMetadataEntry> getAllEntries() {
        try (var session = getSession()) {
            session.setHibernateFlushMode(FlushMode.MANUAL);
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(PSDbMetadataEntry.class);
            var root = criteriaQuery.from(PSDbMetadataEntry.class);
            var result = session.createQuery(criteriaQuery).getResultList();
            var entries = new ArrayList<IPSMetadataEntry>();
            if (result != null) {
                entries.addAll(result);
            }
            return entries;
        }
    }

    @Override
    public void save(Collection<IPSMetadataEntry> entries) {
        Validate.notNull(entries, "entries cannot be null");
        if (entries.isEmpty()) return;

        Transaction tx = null;
        try (var session = getSession()) {
            var dbEntries = convertRestEntriesToDb(entries);
            session.setHibernateFlushMode(FlushMode.ALWAYS);
            for (var entry : dbEntries) {
                tx = session.beginTransaction();
                try {
                    session.saveOrUpdate(entry);
                    tx.commit();
                } catch (org.hibernate.NonUniqueObjectException e) {
                    session.merge(entry);
                    tx.commit();
                }
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error(PSExceptionUtils.getMessageForLog(e));
        }
    }

    @Override
    public void save(IPSMetadataEntry entry) {
        Validate.notNull(entry, "entry cannot be null");
        save(Lists.newArrayList(entry));
    }

    @Override
    public boolean hasDirtyEntries(Collection<IPSMetadataEntry> entries) {
        for (var entry : convertRestEntriesToDb(entries)) {
            var existing = (PSDbMetadataEntry) findEntry(entry.getPagepath());
            if (existing != null && isEntryDirty(existing, entry)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getAllIndexedDirectories() {
        try (var session = getSession()) {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(String.class);
            var root = criteriaQuery.from(PSDbMetadataEntry.class);
            criteriaQuery.select(root.get("pagepath"));
            var resultList = session.createQuery(criteriaQuery).getResultList();

            var indexedDirectories = new HashSet<String>();
            for (var dataEntry : resultList) {
                var matcher = patternToGetDirectoryFromPagepath.matcher(dataEntry);
                if (matcher.find()) {
                    indexedDirectories.add(matcher.group(1));
                }
            }
            return indexedDirectories;
        }
    }

    @Override
    public List<String> getAllSites() {
        try (var session = getSession()) {
            var criteria = session.createCriteria(PSDbMetadataEntry.class);
            criteria.setProjection(Projections.distinct(Projections.property("site")));
            return criteria.list();
        }
    }

    private Collection<String> getPagepathHashes(Collection<String> pagepaths) {
        var pagepathHashes = new ArrayList<String>();
        pagepaths.forEach(pp -> pagepathHashes.add(hashCalculator.calculateHash(pp)));
        return pagepathHashes;
    }

    /**
     * Determine if the entries are "dirty", in other words the field values differ.
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
        var props1 = existing.getProperties();
        var props2 = entry.getProperties();

        var propMap = new HashMap<Object, IPSMetadataProperty>();
        props2.forEach(p -> propMap.put(((PSDbMetadataProperty) p).getId(), p));
        for (var prop : props1) {
            var hash1 = ((PSDbMetadataProperty) prop).getHash();
            var p2 = propMap.get(((PSDbMetadataProperty) prop).getId());
            if (p2 == null) return true;
            var hash2 = ((PSDbMetadataProperty) p2).getHash();
            if (!hash1.equals(hash2)) return true;
        }
        return false;
    }

    /**
     * Converts a database-agnostic collection of metadata entries to Hibernate specific ones.
     *
     * @param entries A list of database-agnostic metadata entry objects. Cannot be <code>null</code>, may be empty.
     * @return A list of database specific metadata entry objects. Never <code>null</code>, may be empty.
     */
    private Collection<PSDbMetadataEntry> convertRestEntriesToDb(Collection<IPSMetadataEntry> entries) {
        Validate.notNull(entries, "list of metadata entries cannot be null");
        var result = new ArrayList<PSDbMetadataEntry>();
        for (var metadataEntry : entries) {
            IPSMetadataEntry dbMetadataEntry = null;
            if (metadataEntry.getPagepath() == null) {
                dbMetadataEntry = new PSDbMetadataEntry();
            } else {
                dbMetadataEntry = findEntry(metadataEntry.getPagepath());
            }
            if (dbMetadataEntry == null) {
                dbMetadataEntry = new PSDbMetadataEntry();
            } else {
                dbMetadataEntry.clearProperties();
            }
            if (!(metadataEntry instanceof PSDbMetadataEntry)) {
                dbMetadataEntry.setFolder(metadataEntry.getFolder());
                dbMetadataEntry.setLinktext(metadataEntry.getLinktext());
                dbMetadataEntry.setName(metadataEntry.getName());
                dbMetadataEntry.setPagepath(metadataEntry.getPagepath());
                dbMetadataEntry.setSite(metadataEntry.getSite());
                dbMetadataEntry.setType(metadataEntry.getType());
                for (var metadataProperty : metadataEntry.getProperties()) {
                    PSDbMetadataProperty prop;
                    if (metadataProperty instanceof PSDbMetadataProperty) {
                        prop = (PSDbMetadataProperty) metadataProperty;
                    } else {
                        prop = new PSDbMetadataProperty(metadataProperty.getName(), metadataProperty.getValuetype(),
                                metadataProperty.getValue());
                    }
                    dbMetadataEntry.addProperty(prop);
                }
            } else {
                dbMetadataEntry = (PSDbMetadataEntry) metadataEntry;
            }
            result.add((PSDbMetadataEntry) dbMetadataEntry);
        }
        return result;
    }

    @Override
    public int updateByCategoryProperty(String oldCategoryName, String newCategoryName) {
        int updatedRows = 0;
        if (oldCategoryName == null || newCategoryName == null)
            throw new IllegalArgumentException("Old and New Category Names are required");

        Transaction tx = null;
        try (var session = getSession()) {
            tx = session.beginTransaction();
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaUpdate = criteriaBuilder.createCriteriaUpdate(PSDbMetadataProperty.class);
            var employeeRoot = criteriaUpdate.from(PSDbMetadataProperty.class);
            criteriaUpdate.set(employeeRoot.get("stringvalue"), newCategoryName).where(
                    criteriaBuilder.and(criteriaBuilder.equal(employeeRoot.get("stringvalue"), oldCategoryName),
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
