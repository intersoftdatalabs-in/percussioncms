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
package com.percussion.delivery.comments.service.rdbms;

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;
import com.percussion.delivery.comments.data.IPSDefaultModerationState;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentSort.SORTBY;
import com.percussion.delivery.comments.data.PSPageInfo;
import com.percussion.delivery.comments.services.IPSCommentsDao;
import com.percussion.delivery.comments.services.PSCommentsService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * RDBMS-backed implementation of {@link IPSCommentsDao}. Performs all comment persistence through
 * Hibernate against the delivery tier database.
 *
 * @author erikserating
 */
@Repository
public class PSCommentsDao implements IPSCommentsDao {

  /** Hibernate session factory used by this DAO. */
  private SessionFactory sessionFactory;

  /** Default no-arg constructor required by Spring for bean instantiation. */
  public PSCommentsDao() {}

  /**
   * Injects the Hibernate {@link SessionFactory} used by this DAO.
   *
   * @param sessionFactory the session factory to use, must not be {@code null}.
   */
  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  private Session getSession() {
    return sessionFactory.getCurrentSession();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.delivery.comments.service.rdbms.IPSCommentDao#find(com
   * .percussion.delivery.comments.data.PSCommentCriteria, boolean)
   */
  @Transactional
  public List<IPSComment> find(PSCommentCriteria criteria) throws Exception {
    Session session = getSession();

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<PSComment> cq = cb.createQuery(PSComment.class);
    Root<PSComment> root = cq.from(PSComment.class);

    List<Predicate> predicates = new ArrayList<>();

    // Username
    if (!StringUtils.isEmpty(criteria.getUsername()))
      predicates.add(
          cb.equal(cb.lower(root.get("username")), criteria.getUsername().toLowerCase()));

    // Pagepath
    if (!StringUtils.isEmpty(criteria.getPagepath()))
      predicates.add(
          cb.equal(cb.lower(root.get("pagePath")), criteria.getPagepath().toLowerCase()));

    // Tag
    if (!StringUtils.isEmpty(criteria.getTag())) {
      // Create a subquery to filter comments by tag
      jakarta.persistence.criteria.Subquery<Long> subquery = cq.subquery(Long.class);
      jakarta.persistence.criteria.Root<PSCommentTag> subRoot = subquery.from(PSCommentTag.class);
      subquery.select(subRoot.get("comment").get("id"));
      subquery.where(cb.equal(cb.lower(subRoot.get("name")), criteria.getTag().toLowerCase()));
      predicates.add(root.get("id").in(subquery));
    }

    // Approval state
    if (criteria.getState() != null)
      predicates.add(cb.equal(root.get("approvalState"), criteria.getState().toString()));

    // Site
    if (!StringUtils.isEmpty(criteria.getSite()))
      predicates.add(cb.equal(cb.lower(root.get("site")), criteria.getSite().toLowerCase()));

    // Viewed
    if (criteria.isViewed() != null)
      predicates.add(cb.equal(root.get("viewed"), criteria.isViewed()));

    // Moderated
    if (criteria.isModerated() != null)
      predicates.add(cb.equal(root.get("moderated"), criteria.isModerated()));

    // Last comment Id
    if (criteria.getLastCommentId() != null) {
      // ors = Restrictions.conjunction();
      // ors.add(Restrictions.eq("id", Long.valueOf(criteria.getLastCommentId())));
      // ors.add(Restrictions.eq("site", criteria.getSite()).ignoreCase());
      // if (!StringUtils.isEmpty(criteria.getPagepath()))
      // ors.add(Restrictions.eq("pagePath", criteria.getPagepath()).ignoreCase());
    }

    cq.where(predicates.toArray(new Predicate[0]));

    // Sorting
    if (criteria.getSort() != null && criteria.getSort().getSortBy() != null) {
      String field = PSCommentsService.SORTBY_FIELD_MAPPING.get(criteria.getSort().getSortBy());
      boolean isAscending = criteria.getSort().isAscending();

      if (isAscending) cq.orderBy(cb.asc(root.get(field)));
      else cq.orderBy(cb.desc(root.get(field)));
    } else {
      // By default, sort by CREATEDATE in descending order
      cq.orderBy(cb.desc(root.get(PSCommentsService.SORTBY_FIELD_MAPPING.get(SORTBY.CREATEDDATE))));
    }

    // Max results and start index
    Query<PSComment> query = session.createQuery(cq);
    if (criteria.getMaxResults() > 0) {
      query.setMaxResults(criteria.getMaxResults());
    }
    if (criteria.getStartIndex() > 0) {
      query.setFirstResult(criteria.getStartIndex());
    }

    List<PSComment> results = query.getResultList();
    List<IPSComment> ipsResults = new ArrayList<>();
    for (PSComment comment : results) {
      ipsResults.add(comment);
    }
    return ipsResults;
  }

  @Transactional
  public Set<String> findSitesForCommentIds(Collection<String> ids) {
    Collection<Long> longIds = new ArrayList<>(ids.size());
    for (String s : ids) longIds.add(Long.valueOf(s));
    String selectComments = "select site from PSComment where id in (:idList)";
    Session session = getSession();
    Query<String> query = session.createQuery(selectComments, String.class);
    query.setParameter("idList", longIds);
    List<String> siteNames = query.getResultList();
    return new HashSet<>(siteNames);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.delivery.comments.service.rdbms.IPSCommentDao#save(com
   * .percussion.delivery.comments.data.IPSComment)
   */
  @Transactional
  public void save(IPSComment comment) throws Exception {
    PSComment hComment = new PSComment(comment);
    hComment.setId(comment.getId());
    PSComment managed = getSession().merge(hComment);
    comment.setId(managed.getId());
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.delivery.comments.service.rdbms.IPSCommentDao#delete(java
   * .util.Collection)
   */
  @Transactional
  public void delete(Collection<String> commentIds) {
    Collection<Long> longIds = new ArrayList<>(commentIds.size());
    for (String s : commentIds) longIds.add(Long.valueOf(s));
    Session session = getSession();

    session
        .createMutationQuery("delete from PSComment where id in (:commentIds)")
        .setParameter("commentIds", longIds)
        .executeUpdate();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.delivery.comments.service.rdbms.IPSCommentDao#moderate
   * (java.util.Collection,
   * com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE)
   */
  @Transactional
  public void moderate(Collection<String> commentIds, APPROVAL_STATE newApprovalState)
      throws Exception {
    Collection<Long> longIds = new ArrayList<>(commentIds.size());
    for (String s : commentIds) longIds.add(Long.valueOf(s));
    Session session = getSession();
    try {
      String updateQueryString =
          "update PSComment com set approvalState = :newApprovalState "
              + "where com.id in (:idList) ";

      MutationQuery updateQuery = session.createMutationQuery(updateQueryString);
      updateQuery.setParameter("newApprovalState", newApprovalState.toString());
      updateQuery.setParameter("idList", longIds);
      updateQuery.executeUpdate();
    } finally {
      session.flush();
    }
  }

  @Transactional(readOnly = true)
  public List<PSPageInfo> findPagesWithComments(String site) throws Exception {
    Session session = getSession();

    try {
      String stringQuery =
          "select pagePath, approvalState, count(*), viewed "
              + "from PSComment "
              + "where site = :site "
              + "group by pagePath, approvalState, viewed ";

      Query<Object[]> query = session.createQuery(stringQuery, Object[].class);
      query.setParameter("site", site);

      List<Object[]> result = query.getResultList();
      List<PSPageInfo> pages = new ArrayList<>();
      for (Object[] r : result)
        pages.add(new PSPageInfo((String) r[0], (String) r[1], (Long) r[2], (Boolean) r[3]));
      return pages;

    } finally {
      // session.close();
    }
  }

  @Transactional
  public APPROVAL_STATE findDefaultModerationState(String site) {
    Session session = getSession();

    Query<PSDefaultModerationState> query =
        session.createQuery(
            "from PSDefaultModerationState where site = :site", PSDefaultModerationState.class);
    query.setParameter("site", site);
    List<PSDefaultModerationState> result = query.getResultList();
    APPROVAL_STATE state = APPROVAL_STATE.APPROVED;
    if (!result.isEmpty()) {
      state = APPROVAL_STATE.valueOf(result.get(0).getDefaultState());
    }
    return state;
  }

  @Transactional
  public void saveDefaultModerationState(String sitename, APPROVAL_STATE state) {
    Session session = getSession();

    IPSDefaultModerationState st = new PSDefaultModerationState(sitename, state.toString());
    session.merge(st);
    session.flush();
  }
}
