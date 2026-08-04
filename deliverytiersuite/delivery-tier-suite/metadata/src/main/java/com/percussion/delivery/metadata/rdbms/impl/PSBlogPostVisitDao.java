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

import com.percussion.delivery.metadata.IPSBlogPostVisit;
import com.percussion.delivery.metadata.IPSBlogPostVisitDao;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate-backed implementation of {@link IPSBlogPostVisitDao}. Reads and writes blog-post visit
 * entries through JPA criteria queries wrapped in Spring-managed transactions.
 */
@Repository
@Scope("singleton")
public class PSBlogPostVisitDao implements IPSBlogPostVisitDao {

  /** No-arg constructor required by Spring. The session factory is injected via the setter. */
  public PSBlogPostVisitDao() {}

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

  /** Logger for this class. */
  private static final Logger log = LogManager.getLogger(PSBlogPostVisitDao.class);

  private static PSHashCalculator hashCalculator = new PSHashCalculator();

  public void delete(Collection<String> pagepaths) {

    /**
     * this code is close but not working. Validate.notNull(pagepaths, "pagepaths cannot be null.");
     *
     * <p>Collection<String> pagepathHashes = getPagepathHashes(pagepaths);
     *
     * <p>Session session = getSession(false);
     *
     * <p>// Delete entries String hqlMetadataEntryDelete = "delete from PSDbBlogPostVisit visit " +
     * "where visit.id in (:pagepathHashes)";
     *
     * <p>session.createQuery(hqlMetadataEntryDelete).setParameterList("pagepathHashes",
     * pagepathHashes).executeUpdate();
     */
    throw new UnsupportedOperationException();
  }

  public boolean delete(String pagepath) {
    /**
     * Validate.notEmpty(pagepath, "pagepath cannot be null or empty.");
     *
     * <p>List<PSDbBlogPostVisit> visits = findBlogPostVisit(pagepath); if (visits != null) {
     * for(PSDbBlogPostVisit visit : visits) { getHibernateTemplate().delete(visit); } return true;
     * } return false;
     */
    throw new UnsupportedOperationException();
  }

  @Transactional(isolation = Isolation.READ_UNCOMMITTED)
  public void save(Collection<IPSBlogPostVisit> visits) {
    Validate.notNull(visits, "visits cannot be null");
    if (visits.isEmpty()) return;

    Session session = getSession();

    // Save entries
    int i = 0;
    Collection<PSDbBlogPostVisit> dbVisits = convertToDbVisits(visits);
    for (PSDbBlogPostVisit visit : dbVisits) {

      session.merge(visit);

      // To avoid OutOfMemory exceptions in case of a lot of entries
      // to be added, flush and clear session after 50 newly added
      // entries (this value is the same value set for JDBC batch size
      // in beans.xml file).
      if (++i % 50 == 0) {
        session.flush();
        session.clear();
        if (Thread.currentThread().isInterrupted()) {
          return;
        }
      }
    }
  }

  @Transactional(isolation = Isolation.READ_UNCOMMITTED)
  public void save(IPSBlogPostVisit visit) {
    Collection<IPSBlogPostVisit> visits = Collections.singletonList(visit);
    save(visits);
  }

  @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
  public List<String> getTopVisitedPages(
      String sectionPath, int days, int limit, String sortOrder) {
    Session session = getSession();
    Calendar cal = Calendar.getInstance();
    /*
     *  if -1, means we are looking for events from all time.
     *  we just keep current time and filter below current time.
     */
    if (days != -1) cal.add(Calendar.DAY_OF_YEAR, -days);
    Date fromDate = cal.getTime();

    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
    Root<PSDbBlogPostVisit> root = criteriaQuery.from(PSDbBlogPostVisit.class);

    Predicate dateCriteria = null;
    if (days != -1) {
      dateCriteria = criteriaBuilder.greaterThanOrEqualTo(root.get("hitDate"), fromDate);
    } else {
      dateCriteria = criteriaBuilder.lessThanOrEqualTo(root.get("hitDate"), fromDate);
    }

    sectionPath = sectionPath + "%";

    criteriaQuery
        .select(root.get("pagepath"))
        .where(
            criteriaBuilder.and(
                criteriaBuilder.like(root.get("pagepath"), sectionPath), dateCriteria));
    criteriaQuery.groupBy(root.get("pagepath"));
    if (sortOrder == null || sortOrder.equalsIgnoreCase("desc")) {
      criteriaQuery.orderBy(
          criteriaBuilder.desc(criteriaBuilder.sum(root.get("hitCount"))),
          criteriaBuilder.desc(root.get("pagepath")));
    } else {
      criteriaQuery.orderBy(
          criteriaBuilder.asc(criteriaBuilder.sum(root.get("hitCount"))),
          criteriaBuilder.desc(root.get("pagepath")));
    }

    return session.createQuery(criteriaQuery).setMaxResults(limit).getResultList();
  }

  @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
  public List<PSDbBlogPostVisit> findBlogPostVisit(String pagepath) {
    Validate.notEmpty(pagepath, "pagepath cannot be null nor empty");

    Session session = getSession();
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<PSDbBlogPostVisit> criteriaQuery =
        criteriaBuilder.createQuery(PSDbBlogPostVisit.class);
    Root<PSDbBlogPostVisit> root = criteriaQuery.from(PSDbBlogPostVisit.class);
    criteriaQuery.select(root).where(criteriaBuilder.like(root.get("pagepath"), pagepath));

    return session.createQuery(criteriaQuery).getResultList();
  }

  /**
   * Finds a blog-post visit by the supplied page path and exact hit date.
   *
   * @param pagepath the page path to match; may not be {@code null} nor empty.
   * @param date the exact hit date to match; may be {@code null}.
   * @return the matching visit, or {@code null} if none was found.
   */
  @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
  public PSDbBlogPostVisit findBlogPostVisitByDate(String pagepath, Date date) {
    Validate.notEmpty(pagepath, "pagepath cannot be null nor empty");

    Session session = getSession();

    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<PSDbBlogPostVisit> criteriaQuery =
        criteriaBuilder.createQuery(PSDbBlogPostVisit.class);
    Root<PSDbBlogPostVisit> root = criteriaQuery.from(PSDbBlogPostVisit.class);
    criteriaQuery
        .select(root)
        .where(
            criteriaBuilder.and(
                criteriaBuilder.like(root.get("pagepath"), pagepath),
                criteriaBuilder.equal(root.get("hitDate"), date)));

    List<PSDbBlogPostVisit> results = session.createQuery(criteriaQuery).getResultList();

    if (results == null || results.isEmpty()) return null;
    return results.get(0);
  }

  private Collection<String> getPagepathHashes(Collection<String> pagepaths) {
    List<String> pagepathHashes = new ArrayList<>();

    for (String pp : pagepaths) {
      pagepathHashes.add(hashCalculator.calculateHash(pp));
    }

    return pagepathHashes;
  }

  private Collection<PSDbBlogPostVisit> convertToDbVisits(Collection<IPSBlogPostVisit> visits) {
    Validate.notNull(visits, "list of visits cannot be null");

    Collection<PSDbBlogPostVisit> result = new ArrayList<>();
    for (IPSBlogPostVisit visit : visits) {
      PSDbBlogPostVisit dbVisit = findBlogPostVisitByDate(visit.getPagepath(), visit.getHitDate());
      if (dbVisit != null) {
        dbVisit.setHitCount(dbVisit.getHitCount().add(visit.getHitCount()));
      } else {
        dbVisit =
            new PSDbBlogPostVisit(visit.getPagepath(), visit.getHitDate(), visit.getHitCount());
      }
      result.add(dbVisit);
    }
    return result;
  }

  @Override
  @Transactional
  public void updatePostsAfterSiteRename(String prevSiteName, String newSiteName) throws Exception {
    Session session = getSession();

    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<PSDbBlogPostVisit> criteriaQuery =
        criteriaBuilder.createQuery(PSDbBlogPostVisit.class);
    Root<PSDbBlogPostVisit> root = criteriaQuery.from(PSDbBlogPostVisit.class);
    criteriaQuery
        .select(root)
        .where(criteriaBuilder.like(root.get("pagepath"), "%/" + prevSiteName + "/%"));

    List<PSDbBlogPostVisit> results = session.createQuery(criteriaQuery).getResultList();

    for (PSDbBlogPostVisit visit : results) {
      visit.setPagepath(
          visit
              .getPagepath()
              .replaceAll(Pattern.quote(prevSiteName), Matcher.quoteReplacement(newSiteName)));
      session.merge(visit);
    }
  }

  private Session getSession() {

    return sessionFactory.getCurrentSession();
  }
}
