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
package com.percussion.metadata.dao.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate-based implementation of {@link com.percussion.metadata.dao.IPSMetadataDao}. Sunny Sal
 * says: "Hibernate: because even your metadata wants to nap!"
 */
@PSSiteManageBean("metadataDao")
@Transactional
public class PSMetadataDao implements com.percussion.metadata.dao.IPSMetadataDao {

  @PersistenceContext private EntityManager entityManager;

  private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  @Override
  public PSMetadata create(PSMetadata data) throws IPSGenericDao.SaveException {
    var session = getSession();
    try {
      session.persist(data);
    } catch (HibernateException e) {
      var msg = "create(PSMetadata data) database error " + e.getMessage();
      log.error(msg);
      throw new IPSGenericDao.SaveException(msg, e);
    } finally {
      try {
        session.flush();
      } catch (Exception e) {
        log.error("Error releasing session in create: {}", PSExceptionUtils.getMessageForLog(e));
      }
    }
    return data;
  }

  @Override
  public void delete(String key) throws IPSGenericDao.DeleteException, IPSGenericDao.LoadException {
    var data = find(key);
    if (data == null) {
      log.warn("delete(String key) Attempted to delete non-existent metadata entry.");
      return;
    }
    delete(data);
  }

  @Override
  public void delete(PSMetadata data) throws IPSGenericDao.DeleteException {
    var session = getSession();
    try {
      session.remove(data);
    } catch (HibernateException e) {
      var msg = "delete(PSMetadata data) database error " + e.getMessage();
      log.error(msg);
      throw new IPSGenericDao.DeleteException(msg, e);
    } finally {
      try {
        session.flush();
      } catch (Exception e) {
        log.error("Error releasing session in delete: {}", PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  @Override
  public PSMetadata save(PSMetadata data) throws IPSGenericDao.SaveException {
    var session = getSession();
    try {
      var key = data.getKey();
      var existing = session.find(PSMetadata.class, key);
      if (existing == null) {
        var emsg = "Attempt to modify non-existent record " + key;
        log.error(emsg);
        throw new IPSGenericDao.SaveException(emsg);
      }
      existing.setData(data.getData());
      session.merge(existing);
    } catch (HibernateException he) {
      var emsg = "save(PSMetadata data) database error " + he.getMessage();
      log.error(emsg);
      throw new IPSGenericDao.SaveException(emsg, he);
    } finally {
      try {
        session.flush();
      } catch (Exception e) {
        log.error("Error releasing session in save: {}", PSExceptionUtils.getMessageForLog(e));
      }
    }
    return data;
  }

  @Override
  public PSMetadata find(String key) throws IPSGenericDao.LoadException {
    var session = getSession();
    try {
      return session.find(PSMetadata.class, key);
    } catch (HibernateException e) {
      var msg = "find(String key) database error " + e.getMessage();
      log.error(msg);
      throw new IPSGenericDao.LoadException(msg, e);
    }
  }

  @Override
  @Transactional
  public Collection<PSMetadata> findByPrefix(String prefix) throws IPSGenericDao.LoadException {
    var session = getSession();
    try {
      return session
          .createQuery("from PSMetadata where lower(key) like :p", PSMetadata.class)
          .setParameter("p", prefix.toLowerCase() + "%")
          .list();
    } catch (HibernateException he) {
      var emsg = "findByPrefix(String prefix) database error " + he.getMessage();
      log.error(emsg);
      throw new IPSGenericDao.LoadException(emsg, he);
    }
  }
}
