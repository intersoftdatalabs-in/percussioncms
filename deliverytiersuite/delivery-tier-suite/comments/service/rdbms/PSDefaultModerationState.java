// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
 * ...existing code...
 */
package com.percussion.delivery.comments.service.rdbms;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.percussion.delivery.comments.data.IPSDefaultModerationState;

/**
 * Entity to store default moderation state for comments service.
 * @author erikserating
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSComments2")
@Table(name = "PERC_DEFAULT_MODERATION_STATE")
public class PSDefaultModerationState implements IPSDefaultModerationState {

    @Id
    private String site;

    @Basic
    private String defaultState;

    public PSDefaultModerationState() {
        // Default constructor
    }

    public PSDefaultModerationState(String site, String defaultState) {
        if (StringUtils.isBlank(site))
            throw new IllegalArgumentException("site cannot be null or empty.");
        if (StringUtils.isBlank(defaultState))
            throw new IllegalArgumentException("defaultState cannot be null or empty.");
        this.site = site;
        this.defaultState = defaultState;
    }

    @Override
    public String getSite() {
        return site;
    }

    @Override
    public void setSite(String site) {
        this.site = site;
    }

    @Override
    public String getDefaultState() {
        return defaultState;
    }

    @Override
    public void setDefaultState(String defaultState) {
        this.defaultState = defaultState;
    }
}
// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
 * ...existing code...
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
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data access object for comments.
 * @author erikserating
 */
public class PSCommentsDao extends HibernateDaoSupport implements IPSCommentsDao {

    @Transactional
    public List<IPSComment> find(PSCommentCriteria criteria) throws Exception {
        var session = getSession();
        var queryCriteria = session.createCriteria(PSComment.class);
        prepareCriteria(criteria, queryCriteria);
        return queryCriteria.list();
    }

    @Transactional
    public Set<String> findSitesForCommentIds(Collection<String> ids) {
        var longIds = ids.stream().map(Long::valueOf).collect(Collectors.toList());
        var selectComments = "select site from PSComment where id in (:idList)";
        var siteNames = (List<String>) this.getHibernateTemplate().findByNamedParam(selectComments, "idList", longIds);
        return new HashSet<>(siteNames);
    }

    @Transactional
    public void save(IPSComment comment) throws Exception {
        var hComment = new PSComment(comment);
        hComment.setId(comment.getId());
        getHibernateTemplate().saveOrUpdate(hComment);
        comment.setId(hComment.getId());
    }

    @Transactional
    public void delete(Collection<String> commentIds) {
        var longIds = commentIds.stream().map(Long::valueOf).collect(Collectors.toList());
        var session = getSession();
        session.createQuery("delete from PSComment where id in (:commentIds)")
                .setParameterList("commentIds", longIds)
                .executeUpdate();
    }

    @Transactional
    public void moderate(Collection<String> commentIds, APPROVAL_STATE newApprovalState) throws Exception {
        var longIds = commentIds.stream().map(Long::valueOf).collect(Collectors.toList());
        var session = getSession();
        try {
            var updateQueryString = "update PSComment com set approvalState = :newApprovalState "
                    + "where com.id in (:idList) ";
            var updateQuery = session.createQuery(updateQueryString);
            updateQuery.setParameter("newApprovalState", newApprovalState.toString());
            updateQuery.setParameterList("idList", longIds);
            updateQuery.executeUpdate();
        } finally {
            session.flush();
        }
    }

    @Transactional(readOnly = true)
    public List<PSPageInfo> findPagesWithComments(String site) throws Exception {
        var session = getSession();
        try {
            var stringQuery = "select pagePath, approvalState, count(*), viewed from PSComment "
                    + "where site = :site group by pagePath, approvalState, viewed ";
            var query = session.createQuery(stringQuery);
            query.setParameter("site", site);
            var result = query.list();
            var pages = new ArrayList<PSPageInfo>();
            for (var r : result) {
                var row = (Object[]) r;
                pages.add(new PSPageInfo((String) row[0], (String) row[1], (Long) row[2], (Boolean) row[3]));
            }
            return pages;
        } finally {
            // session.close();
        }
    }

    @Transactional
    public APPROVAL_STATE findDefaultModerationState(String site) {
        var session = getSession();
        var query = session.createQuery("from PSDefaultModerationState where site = :site");
        query.setParameter("site", site);
        var result = query.list();
        var state = APPROVAL_STATE.APPROVED;
        if (!result.isEmpty()) {
            state = APPROVAL_STATE.valueOf(((IPSDefaultModerationState) result.get(0)).getDefaultState());
        }
        return state;
    }

    @Transactional
    public void saveDefaultModerationState(String sitename, APPROVAL_STATE state) {
        var session = getSession();
        var st = new PSDefaultModerationState(sitename, state.toString());
        session.saveOrUpdate(st);
        session.flush();
    }

    /**
     * Prepares the Hibernate Criteria object according to the settings in
     * PSCommentCriteria object.
     *
     * @param criteria The comment criteria. Must not be null.
     * @param queryCriteria The Hibernate Criteria object.
     */
    private void prepareCriteria(PSCommentCriteria criteria, Criteria queryCriteria) {
        var ands = Restrictions.conjunction();
        Conjunction ors = null;

        if (!StringUtils.isEmpty(criteria.getUsername()))
            ands.add(Restrictions.eq("username", criteria.getUsername()).ignoreCase());

        if (!StringUtils.isEmpty(criteria.getPagepath()))
            ands.add(Restrictions.eq("pagePath", criteria.getPagepath()).ignoreCase());

        if (!StringUtils.isEmpty(criteria.getTag())) {
            queryCriteria.createAlias("commentTags", "tag");
            ands.add(Restrictions.eq("tag.name", criteria.getTag()).ignoreCase());
        }

        if (criteria.getState() != null)
            ands.add(Restrictions.eq("approvalState", criteria.getState().toString()));

        if (!StringUtils.isEmpty(criteria.getSite()))
            ands.add(Restrictions.eq("site", criteria.getSite()).ignoreCase());

        if (criteria.isViewed() != null)
            ands.add(Restrictions.eq("viewed", criteria.isViewed()));

        if (criteria.isModerated() != null)
            ands.add(Restrictions.eq("moderated", criteria.isModerated()));

        if (criteria.getLastCommentId() != null) {
            ors = Restrictions.conjunction();
            ors.add(Restrictions.eq("id", Long.valueOf(criteria.getLastCommentId())));
            ors.add(Restrictions.eq("site", criteria.getSite()).ignoreCase());
            if (!StringUtils.isEmpty(criteria.getPagepath()))
                ors.add(Restrictions.eq("pagePath", criteria.getPagepath()).ignoreCase());
        }

        if (ors != null)
            queryCriteria.add(Restrictions.or(ands, ors));
        else
            queryCriteria.add(ands);

        if (criteria.getSort() != null && criteria.getSort().getSortBy() != null) {
            var field = PSCommentsService.SORTBY_FIELD_MAPPING.get(criteria.getSort().getSortBy());
            var isAscending = criteria.getSort().isAscending();
            if (isAscending)
                queryCriteria.addOrder(Order.asc(field));
            else
                queryCriteria.addOrder(Order.desc(field));
        } else {
            queryCriteria.addOrder(Order.desc(PSCommentsService.SORTBY_FIELD_MAPPING.get(SORTBY.CREATEDDATE)));
        }

        if (criteria.getMaxResults() > 0)
            queryCriteria.setMaxResults(criteria.getMaxResults());

        if (criteria.getStartIndex() > 0)
            queryCriteria.setFirstResult(criteria.getStartIndex());

        queryCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
    }

    private Session getSession() {
        return getSessionFactory().getCurrentSession();
    }
}

