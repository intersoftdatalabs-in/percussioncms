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

import com.percussion.delivery.metadata.IPSCookieConsent;
import com.percussion.delivery.metadata.IPSCookieConsentDao;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate-backed implementation of {@link IPSCookieConsentDao}. Reads and writes cookie-consent
 * entries through JPA criteria queries wrapped in Spring-managed transactions.
 *
 * @author chriswright
 */
@Repository
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public class PSCookieConsentDao implements IPSCookieConsentDao {

  /** No-arg constructor required by Spring. The session factory is injected via the setter. */
  public PSCookieConsentDao() {}

  private SessionFactory sessionFactory;

  /**
   * Sets the Hibernate {@link SessionFactory} used by this DAO.
   *
   * @param sessionFactory the session factory to use; may not be {@code null}.
   */
  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  private static final Logger log = LogManager.getLogger(PSCookieConsentDao.class);

  @Override
  public void save(Collection<PSDbCookieConsent> consents) {
    Validate.notNull(consents, "Cookie consent object cannot be null");

    if (consents.isEmpty()) return;

    try {
      Session session = getSession();

      int i = 0;

      for (PSDbCookieConsent consent : consents) {
        session.merge(consent);
        if (++i % 50 == 0) {
          session.flush();
          session.clear();
          if (Thread.currentThread().isInterrupted()) {
            return;
          }
        }
      }
    } catch (Exception e) {
      log.error(
          "Error when saving cookie consent entry. Error: {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  @Transactional
  @Override
  public Collection<IPSCookieConsent> getAllCookieConsentStats() {

    Collection<IPSCookieConsent> consents = new ArrayList<>();

    try {
      Session session = getSession();
      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<PSDbCookieConsent> query = cb.createQuery(PSDbCookieConsent.class);
      Root<PSDbCookieConsent> root = query.from(PSDbCookieConsent.class);
      query.select(root);

      List<PSDbCookieConsent> result = session.createQuery(query).getResultList();

      for (IPSCookieConsent res : result) {
        consents.add(res);
      }
    } catch (Exception e) {
      log.error(
          "Error retrieving list of cookie consent entries from database. Error: {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    return consents;
  }

  @Transactional
  @Override
  public Collection<IPSCookieConsent> getAllCookieStatsForSite(String siteName) {
    Collection<IPSCookieConsent> consents = new ArrayList<>();
    try {
      Session session = getSession();
      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<PSDbCookieConsent> query = cb.createQuery(PSDbCookieConsent.class);
      Root<PSDbCookieConsent> root = query.from(PSDbCookieConsent.class);
      query.select(root).where(cb.equal(root.get("siteName"), siteName));

      List<PSDbCookieConsent> result = session.createQuery(query).getResultList();

      for (IPSCookieConsent res : result) {
        consents.add(res);
      }
    } catch (Exception e) {
      log.error(
          "Error retrieving list of cookie consent entries from database. Error: {}",
          PSExceptionUtils.getMessageForLog(e));
    }

    return consents;
  }

  @Transactional
  @Override
  public void deleteAll() throws Exception {
    try {
      Session session = getSession();

      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaDelete<PSDbCookieConsent> deleteQuery =
          builder.createCriteriaDelete(PSDbCookieConsent.class);
      deleteQuery.from(PSDbCookieConsent.class);
      session.createMutationQuery(deleteQuery).executeUpdate();

    } catch (Exception e) {
      throw new Exception("Error deleting cookie consent entries from DB.", e);
    }
  }

  @Transactional
  @Override
  public void deleteForSite(String siteName) throws Exception {
    try {
      Session session = getSession();

      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaDelete<PSDbCookieConsent> deleteQuery =
          builder.createCriteriaDelete(PSDbCookieConsent.class);
      Root<PSDbCookieConsent> root = deleteQuery.from(PSDbCookieConsent.class);
      deleteQuery.where(builder.like(root.get("siteName"), siteName));
      session.createMutationQuery(deleteQuery).executeUpdate();

    } catch (Exception e) {
      throw new Exception("Error deleting cookie consent entries for site: " + siteName, e);
    }
  }

  @Transactional
  @Override
  public Map<String, Integer> getTotalsForAllSites() throws Exception {
    try {
      Map<String, Integer> results = new HashMap<>();
      Session session = getSession();

      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<PSDbCookieConsent> criteriaQuery =
          criteriaBuilder.createQuery(PSDbCookieConsent.class);
      Root<PSDbCookieConsent> root = criteriaQuery.from(PSDbCookieConsent.class);
      criteriaQuery.select(root);

      List<PSDbCookieConsent> cookieConsents = session.createQuery(criteriaQuery).getResultList();

      for (PSDbCookieConsent cookieConsent : cookieConsents) {
        String s = cookieConsent.getSiteName();
        Integer c = results.get(s);
        if (c == null) {
          c = Integer.valueOf(1);
        } else {
          c = c + 1;
        }
        results.put(s, c);
      }

      return results;
    } catch (Exception e) {
      throw new Exception("Error getting total cookie consents", e);
    }
  }

  @Transactional
  @Override
  public Map<String, Integer> getTotalsForSite(String siteName) throws Exception {
    try {
      Map<String, Integer> results = new HashMap<>();

      Session session = getSession();
      CriteriaBuilder cb = session.getCriteriaBuilder();

      // First query: get distinct service names for the site
      CriteriaQuery<String> serviceQuery = cb.createQuery(String.class);
      Root<PSDbCookieConsent> serviceRoot = serviceQuery.from(PSDbCookieConsent.class);
      serviceQuery
          .select(serviceRoot.get("serviceName"))
          .distinct(true)
          .where(cb.equal(serviceRoot.get("siteName"), siteName));

      List<String> serviceNames = session.createQuery(serviceQuery).getResultList();

      // Second query: count entries for each service name
      for (String sName : serviceNames) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<PSDbCookieConsent> countRoot = countQuery.from(PSDbCookieConsent.class);
        countQuery
            .select(cb.count(countRoot))
            .where(
                cb.and(
                    cb.equal(countRoot.get("serviceName"), sName),
                    cb.equal(countRoot.get("siteName"), siteName)));

        Long count = session.createQuery(countQuery).getSingleResult();
        results.put(sName, count.intValue());
      }

      return results;
    } catch (Exception e) {

      log.error(
          "Error getting cookie consent entries for site: {} Error: {}",
          siteName,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new Exception("Error getting cookie consent entries for site: " + siteName, e);
    }
  }

  @Transactional
  @Override
  public void updateOldSiteName(String oldSiteName, String newSiteName) throws Exception {
    Session session = getSession();

    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

    CriteriaUpdate<PSDbCookieConsent> criteriaUpdate =
        criteriaBuilder.createCriteriaUpdate(PSDbCookieConsent.class);
    Root<PSDbCookieConsent> root = criteriaUpdate.from(PSDbCookieConsent.class);
    criteriaUpdate
        .set(root.get("siteName"), newSiteName)
        .where(criteriaBuilder.equal(root.get("siteName"), oldSiteName));
    session.createMutationQuery(criteriaUpdate).executeUpdate();
  }

  private Session getSession() {
    return sessionFactory.getCurrentSession();
  }
}
