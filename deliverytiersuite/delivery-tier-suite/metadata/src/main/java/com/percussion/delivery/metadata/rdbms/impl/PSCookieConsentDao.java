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

import com.percussion.delivery.metadata.IPSCookieConsent;
import com.percussion.delivery.metadata.IPSCookieConsentDao;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author chriswright
 *
 */
@Repository
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public class PSCookieConsentDao implements IPSCookieConsentDao {

    private SessionFactory sessionFactory;

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
            var session = getSession();
            int i = 0;
            for (var consent : consents) {
                session.saveOrUpdate(consent);
                if (++i % 50 == 0) {
                    session.flush();
                    session.clear();
                    if (Thread.currentThread().isInterrupted()) return;
                }
            }
        } catch (Exception e) {
            log.error("Error when saving cookie consent entry. Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    @Override
    @Transactional
    public Collection<IPSCookieConsent> getAllCookieConsentStats() {
        var consents = new ArrayList<IPSCookieConsent>();
        try {
            var session = getSession();
            var crit = session.createCriteria(PSDbCookieConsent.class);
            @SuppressWarnings("unchecked")
            var result = crit.list();
            consents.addAll(result);
        } catch (Exception e) {
            log.error("Error retrieving list of cookie consent entries from database. Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return consents;
    }

    @Override
    @Transactional
    public Collection<IPSCookieConsent> getAllCookieStatsForSite(String siteName) {
        var consents = new ArrayList<IPSCookieConsent>();
        try {
            var session = getSession();
            var crit = session.createCriteria(PSDbCookieConsent.class);
            crit.add(Restrictions.eq("siteName", siteName));
            @SuppressWarnings("unchecked")
            var result = crit.list();
            consents.addAll(result);
        } catch (Exception e) {
            log.error("Error retrieving list of cookie consent entries from database. Error: {}", PSExceptionUtils.getMessageForLog(e));
        }
        return consents;
    }

    @Override
    @Transactional
    public void deleteAll() throws Exception {
        try {
            var session = getSession();
            var builder = session.getCriteriaBuilder();
            var deleteQuery = builder.createCriteriaDelete(PSDbCookieConsent.class);
            deleteQuery.from(PSDbCookieConsent.class);
            session.createQuery(deleteQuery).executeUpdate();
        } catch (Exception e) {
            throw new Exception("Error deleting cookie consent entries from DB.", e);
        }
    }

    @Override
    @Transactional
    public void deleteForSite(String siteName) throws Exception {
        try {
            var session = getSession();
            var builder = session.getCriteriaBuilder();
            var deleteQuery = builder.createCriteriaDelete(PSDbCookieConsent.class);
            var root = deleteQuery.from(PSDbCookieConsent.class);
            deleteQuery.where(builder.like(root.get("siteName"), siteName));
            session.createQuery(deleteQuery).executeUpdate();
        } catch (Exception e) {
            throw new Exception("Error deleting cookie consent entries for site: " + siteName, e);
        }
    }

    @Override
    @Transactional
    public Map<String, Integer> getTotalsForAllSites() throws Exception {
        try {
            var results = new HashMap<String, Integer>();
            var session = getSession();
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(PSDbCookieConsent.class);
            var root = criteriaQuery.from(PSDbCookieConsent.class);
            criteriaQuery.select(root);
            var cookieConsents = session.createQuery(criteriaQuery).getResultList();
            for (var cookieConsent : cookieConsents) {
                var s = cookieConsent.getSiteName();
                var c = results.get(s);
                results.put(s, c == null ? 1 : c + 1);
            }
            return results;
        } catch (Exception e) {
            throw new Exception("Error getting total cookie consents", e);
        }
    }

    @Override
    @Transactional
    public Map<String, Integer> getTotalsForSite(String siteName) throws Exception {
        try {
            var results = new HashMap<String, Integer>();
            var session = getSession();
            var crit = session.createCriteria(PSDbCookieConsent.class);
            crit.add(Restrictions.eq("siteName", siteName));
            crit.setProjection(Projections.projectionList().add(Projections.property("serviceName")));
            @SuppressWarnings("unchecked")
            var serviceNames = crit.list();
            for (var sName : serviceNames) {
                crit = session.createCriteria(PSDbCookieConsent.class);
                crit.setProjection(Projections.rowCount());
                crit.add(Restrictions.eq("serviceName", sName));
                crit.add(Restrictions.eq("siteName", siteName));
                @SuppressWarnings("unchecked")
                var res = crit.list();
                results.put(siteName, res.get(0).intValue());
            }
            return results;
        } catch (Exception e) {
            log.error("Error getting cookie consent entries for site: {} Error: {}", siteName, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new Exception("Error getting cookie consent entries for site: " + siteName, e);
        }
    }

    @Override
    @Transactional
    public void updateOldSiteName(String oldSiteName, String newSiteName) throws Exception {
        var session = getSession();
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaUpdate = criteriaBuilder.createCriteriaUpdate(PSDbCookieConsent.class);
        var root = criteriaUpdate.from(PSDbCookieConsent.class);
        criteriaUpdate.set(root.get("siteName"), newSiteName)
            .where(criteriaBuilder.equal(root.get("siteName"), oldSiteName));
        session.createQuery(criteriaUpdate).executeUpdate();
    }

    private Session getSession() {
        return sessionFactory.getCurrentSession();
    }
}
