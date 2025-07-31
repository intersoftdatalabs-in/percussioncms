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

import com.percussion.delivery.metadata.IPSBlogPostVisit;
import com.percussion.delivery.metadata.IPSBlogPostVisitDao;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Repository
@Scope("singleton")
public class PSBlogPostVisitDao implements IPSBlogPostVisitDao {

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private static final Logger log = LogManager.getLogger(PSBlogPostVisitDao.class);
    private static final PSHashCalculator hashCalculator = new PSHashCalculator();

    @Override
    public void delete(Collection<String> pagepaths) {
        // Not implemented; see README for migration notes.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String pagepath) {
        // Not implemented; see README for migration notes.
        throw new UnsupportedOperationException();
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void save(Collection<IPSBlogPostVisit> visits) {
        Validate.notNull(visits, "visits cannot be null");
        if (visits.isEmpty()) return;

        var session = getSession();
        var dbVisits = convertToDbVisits(visits);
        int i = 0;
        for (var visit : dbVisits) {
            session.saveOrUpdate(visit);
            if (++i % 50 == 0) {
                session.flush();
                session.clear();
                if (Thread.currentThread().isInterrupted()) return;
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void save(IPSBlogPostVisit visit) {
        save(Collections.singletonList(visit));
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
    public List<String> getTopVisitedPages(String sectionPath, int days, int limit, String sortOrder) {
        var session = getSession();
        var cal = Calendar.getInstance();
        if (days != -1) cal.add(Calendar.DAY_OF_YEAR, -days);
        var fromDate = cal.getTime();

        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(String.class);
        var root = criteriaQuery.from(PSDbBlogPostVisit.class);

        Predicate dateCriteria = days != -1
            ? criteriaBuilder.greaterThanOrEqualTo(root.get("hitDate"), fromDate)
            : criteriaBuilder.lessThanOrEqualTo(root.get("hitDate"), fromDate);

        sectionPath = sectionPath + "%";
        criteriaQuery.select(root.get("pagepath"))
            .where(criteriaBuilder.and(criteriaBuilder.like(root.get("pagepath"), sectionPath), dateCriteria));
        criteriaQuery.groupBy(root.get("pagepath"));
        if (sortOrder == null || sortOrder.equalsIgnoreCase("desc")) {
            criteriaQuery.orderBy(criteriaBuilder.desc(criteriaBuilder.sum(root.get("hitCount"))));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.asc(criteriaBuilder.sum(root.get("hitCount"))));
        }

        return session.createQuery(criteriaQuery).setMaxResults(limit).getResultList();
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
    public List<PSDbBlogPostVisit> findBlogPostVisit(String pagepath) {
        Validate.notEmpty(pagepath, "pagepath cannot be null nor empty");
        var session = getSession();
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSDbBlogPostVisit.class);
        var root = criteriaQuery.from(PSDbBlogPostVisit.class);
        criteriaQuery.select(root).where(criteriaBuilder.like(root.get("pagepath"), pagepath));
        return session.createQuery(criteriaQuery).getResultList();
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
    public PSDbBlogPostVisit findBlogPostVisitByDate(String pagepath, Date date) {
        Validate.notEmpty(pagepath, "pagepath cannot be null nor empty");
        var session = getSession();
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSDbBlogPostVisit.class);
        var root = criteriaQuery.from(PSDbBlogPostVisit.class);
        criteriaQuery.select(root).where(criteriaBuilder.and(
            criteriaBuilder.like(root.get("pagepath"), pagepath),
            criteriaBuilder.equal(root.get("hitDate"), date)
        ));
        var results = session.createQuery(criteriaQuery).getResultList();
        return (results == null || results.isEmpty()) ? null : results.get(0);
    }

    private Collection<String> getPagepathHashes(Collection<String> pagepaths) {
        var pagepathHashes = new ArrayList<String>();
        pagepaths.forEach(pp -> pagepathHashes.add(hashCalculator.calculateHash(pp)));
        return pagepathHashes;
    }

    private Collection<PSDbBlogPostVisit> convertToDbVisits(Collection<IPSBlogPostVisit> visits) {
        Validate.notNull(visits, "list of visits cannot be null");
        var result = new ArrayList<PSDbBlogPostVisit>();
        for (var visit : visits) {
            var dbVisit = findBlogPostVisitByDate(visit.getPagepath(), visit.getHitDate());
            if (dbVisit != null) {
                dbVisit.setHitCount(dbVisit.getHitCount().add(visit.getHitCount()));
            } else {
                dbVisit = new PSDbBlogPostVisit(visit.getPagepath(), visit.getHitDate(), visit.getHitCount());
            }
            result.add(dbVisit);
        }
        return result;
    }

    @Override
    @Transactional
    public void updatePostsAfterSiteRename(String prevSiteName, String newSiteName) throws Exception {
        var session = getSession();
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSDbBlogPostVisit.class);
        var root = criteriaQuery.from(PSDbBlogPostVisit.class);
        criteriaQuery.select(root).where(criteriaBuilder.like(root.get("pagepath"), "%/" + prevSiteName + "/%"));
        var results = session.createQuery(criteriaQuery).getResultList();
        for (var visit : results) {
            visit.setPagepath(visit.getPagepath().replaceAll(prevSiteName, newSiteName));
            session.saveOrUpdate(visit);
        }
    }

    private Session getSession() {
        return sessionFactory.getCurrentSession();
    }
}
