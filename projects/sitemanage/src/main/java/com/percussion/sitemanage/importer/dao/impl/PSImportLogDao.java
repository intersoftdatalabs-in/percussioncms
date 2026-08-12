// REFACTORED: CP-JAVA11
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
package com.percussion.sitemanage.importer.dao.impl;

import static com.percussion.services.utils.orm.PSDataCollectionHelper.MAX_IDS;

import com.percussion.cms.IPSConstants;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.sitemanage.importer.dao.IPSImportLogDao;
import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for import log entries. Sunny Sal says: "If you can't log it, you can't debug
 * it!"
 */
@Repository("importLogDao")
@Transactional
public class PSImportLogDao implements IPSImportLogDao {

  @PersistenceContext private EntityManager entityManager;

  private IPSGuidManager guidMgr;

  private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);

  /** Key used to generate local content IDs. */
  private static final String LOG_ENTRY_KEY = "PSX_IMPORTLOGENTRY";

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  @Override
  @Transactional
  public void save(PSImportLogEntry logEntry) throws IPSGenericDao.SaveException {
    Validate.notNull(logEntry, "Log entry must not be null");
    if (logEntry.getLogEntryId() == -1) {
      logEntry.setLogEntryId(guidMgr.createId(LOG_ENTRY_KEY));
    }
    var session = getSession();
    try {
      session.merge(logEntry);
    } catch (HibernateException e) {
      var msg = "Database error: " + e.getMessage();
      log.error(msg, e);
      throw new IPSGenericDao.SaveException(msg, e);
    } finally {
      session.flush();
    }
  }

  @Override
  public List<PSImportLogEntry> findAll(String objectId, String type) {
    Validate.notNull(type, "Type must not be null");
    var session = getSession();
    var query =
        session.createQuery(
            "from PSImportLogEntry where objectId = :objectId and objectType = :objectType",
            PSImportLogEntry.class);
    query.setParameter("objectId", objectId);
    query.setParameter("objectType", type);
    return query.list();
  }

  @Override
  public void delete(PSImportLogEntry logEntry) throws IPSGenericDao.SaveException {
    Validate.notNull(logEntry, "Log entry must not be null");
    var session = getSession();
    try {
      session.remove(logEntry);
    } catch (HibernateException e) {
      var msg = "Database error: " + e.getMessage();
      log.error(msg, e);
      throw new IPSGenericDao.SaveException(msg, e);
    } finally {
      session.flush();
    }
  }

  @Override
  public PSImportLogEntry findLogEntryById(long pageLogId) {
    var session = getSession();
    return session.find(PSImportLogEntry.class, pageLogId);
  }

  @Override
  public List<Long> findLogIdsForObjects(List<String> objectIds, String type) {
    Validate.notNull(objectIds, "Object IDs must not be null");
    Validate.notNull(type, "Type must not be null");
    if (objectIds.isEmpty()) {
      return Collections.emptyList();
    }
    var results = new ArrayList<Long>();
    if (objectIds.size() < MAX_IDS) {
      results.addAll(findLogIdsByObjectIds(objectIds, type));
    } else {
      // Paginate the query to avoid Oracle's IN clause limit
      for (var i = 0; i < objectIds.size(); i += MAX_IDS) {
        var end = Math.min(i + MAX_IDS, objectIds.size());
        results.addAll(findLogIdsByObjectIds(objectIds.subList(i, end), type));
      }
    }
    // Sort using streams for Sunny Sal style!
    return results.stream().sorted().collect(Collectors.toList());
  }

  private List<Long> findLogIdsByObjectIds(List<String> objectIds, String type) {
    var session = getSession();
    var query =
        session.createQuery(
            "select e.logEntryId from PSImportLogEntry e where e.objectId in (:objectIds) and"
                + " e.objectType = :objectType",
            Long.class);
    query.setParameterList("objectIds", objectIds);
    query.setParameter("objectType", type);
    return query.list();
  }

  @Autowired
  public final void setGuidManager(IPSGuidManager guidMgr) {
    this.guidMgr = guidMgr;
  }
}
