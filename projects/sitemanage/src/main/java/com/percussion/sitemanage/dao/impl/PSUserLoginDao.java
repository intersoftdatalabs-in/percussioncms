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
package com.percussion.sitemanage.dao.impl;

import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.percussion.cms.IPSConstants;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.user.data.PSUserLogin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author DavidBenua
 */
@Transactional
@Repository("userLoginDao")
public class PSUserLoginDao implements IPSUserLoginDao {

  @PersistenceContext private EntityManager entityManager;

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  private static final Logger log = LogManager.getLogger(IPSConstants.SECURITY_LOG);

  /* (non-Javadoc)
   * Legacy delete that used to be declared on IPSGenericDao.  The current
   * interface now uses `remove` instead, so this method is kept for
   * backward-compatibility and is **not** annotated with @Override.
   */
  public void delete(String name) throws IPSGenericDao.DeleteException {
    String emsg;
    var session = getSession();
    try {
      var login = session.get(PSUserLogin.class, name);
      log.debug("deleting userlogin for {}", login);
      if (login == null) {
        emsg = "Attempt to delete non-existent user " + name;
        log.warn(emsg);
        return;
      }
      session.remove(login);
    } catch (HibernateException he) {
      auditUser(name, UserAuditKind.DELETE, AuditOutcome.FAILURE);
      emsg = "database error " + he.getMessage();
      log.error(emsg);
      throw new IPSGenericDao.DeleteException(emsg, he);
    } finally {
      session.flush();
    }
  }

  // new methods to satisfy IPSGenericDao
  @Override
  public void remove(PSUserLogin object) throws PSDataServiceException {
    if (object == null) {
      throw new IllegalArgumentException("object must not be null");
    }
    remove(object.getUserid());
  }

  @Override
  public void remove(String name) throws PSDataServiceException {
    try {
      delete(name);
    } catch (IPSGenericDao.DeleteException e) {
      // DeleteException is a PSDataServiceException subclass
      throw e;
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.share.dao.IPSGenericDao#find(java.io.Serializable)
   */
  @Override
  public PSUserLogin find(String id) throws IPSGenericDao.LoadException {
    String emsg;
    var session = getSession();
    PSUserLogin result = null;
    try {
      result = session.get(PSUserLogin.class, id);
      if (result == null) {
        emsg = "no such user " + id;
        log.debug(emsg);
      }
    } catch (HibernateException he) {
      emsg = "database error " + he.getMessage();
      log.error(emsg);
      throw new IPSGenericDao.LoadException(emsg, he);
    }
    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.share.dao.IPSUserLoginDao#findByName(java.lang.String)
   */
  @Override
  public List<PSUserLogin> findByName(String name) throws IPSGenericDao.LoadException {
    String emsg;
    var session = getSession();
    List<PSUserLogin> results = new ArrayList<>();
    try {
      var builder = session.getCriteriaBuilder();
      var criteria = builder.createQuery(PSUserLogin.class);
      var critRoot = criteria.from(PSUserLogin.class);
      criteria.where(builder.equal(builder.lower(critRoot.get("userid")), name.toLowerCase()));
      results = entityManager.createQuery(criteria).getResultList();
    } catch (HibernateException he) {
      emsg = "database error " + he.getMessage();
      log.error(emsg);
      throw new IPSGenericDao.LoadException(emsg, he);
    }
    return results;
  }

  /* (non-Javadoc)
   * @see com.percussion.share.dao.IPSGenericDao#findAll()
   */
  @Override
  public List<PSUserLogin> findAll() throws com.percussion.share.dao.IPSGenericDao.LoadException {
    String emsg;
    var session = getSession();
    List<PSUserLogin> results = new ArrayList<>();
    try {
      var builder = session.getCriteriaBuilder();
      var criteria = builder.createQuery(PSUserLogin.class);
      var critRoot = criteria.from(PSUserLogin.class);
      criteria.orderBy(builder.asc(critRoot.get("userid")));
      results = entityManager.createQuery(criteria).getResultList();
    } catch (HibernateException he) {
      emsg = "database error " + he.getMessage();
      log.error(emsg);
      throw new IPSGenericDao.LoadException(emsg, he);
    }
    return results;
  }

  /* (non-Javadoc)
   * @see com.percussion.share.dao.IPSGenericDao#save(java.lang.Object)
   */
  @Override
  public PSUserLogin save(PSUserLogin login)
      throws com.percussion.share.dao.IPSGenericDao.SaveException {
    String emsg;
    var session = getSession();
    try {
      var uid = login.getUserid();
      var l2 = session.get(PSUserLogin.class, uid);
      if (l2 == null) {
        emsg = "Attempt to modify non-existent user " + uid;
        log.error(emsg);
        throw new IPSGenericDao.SaveException(emsg);
      }
      l2.setPassword(login.getPassword());
      session.merge(l2);
    } catch (HibernateException he) {
      auditUser(login.getUserid(), UserAuditKind.UPDATE, AuditOutcome.FAILURE);
      emsg = "database error " + he.getMessage();
      log.error(emsg);
      throw new IPSGenericDao.SaveException(emsg, he);
    } finally {
      session.flush();
    }
    return login;
  }

  /* (non-Javadoc)
   * @see com.percussion.sitemanage.dao.IPSUserLoginDao#create(com.percussion.sitemanage.data.PSUserLogin)
   */
  @Override
  public PSUserLogin create(PSUserLogin login)
      throws com.percussion.share.dao.IPSGenericDao.SaveException {
    String emsg;
    var session = getSession();
    try {
      session.persist(login);
      auditUser(login.getUserid(), UserAuditKind.CREATE, AuditOutcome.SUCCESS);
    } catch (HibernateException he) {
      emsg = "database error " + he.getMessage();
      log.error(emsg);
      auditUser(login.getUserid(), UserAuditKind.CREATE, AuditOutcome.FAILURE);
      throw new IPSGenericDao.SaveException(emsg, he);
    } finally {
      session.flush();
    }
    return login;
  }

  private enum UserAuditKind {
    CREATE,
    UPDATE,
    DELETE
  }

  private void auditUser(String targetUser, UserAuditKind kind, AuditOutcome outcome) {
    try {
      var current = PSSecurityFilter.getCurrentRequest();
      var servletRequest = current != null ? current.getServletRequest() : null;
      switch (kind) {
        case CREATE -> PSSystemAuditLogger.userCreate(servletRequest, outcome, targetUser);
        case UPDATE ->
            PSSystemAuditLogger.userUpdate(servletRequest, outcome, targetUser, "login-dao");
        case DELETE -> PSSystemAuditLogger.userDelete(servletRequest, outcome, targetUser);
      }
    } catch (Exception e) {
      log.error("Failed to write user audit event: {}", e.getMessage());
      log.debug(e);
    }
  }
}
