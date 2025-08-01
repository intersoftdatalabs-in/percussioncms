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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author erikserating
 *
 */
public class PSCommentsDao extends HibernateDaoSupport implements IPSCommentsDao
{

    /*
     * (non-Javadoc)
     *
     * @see
     * com.percussion.delivery.comments.service.rdbms.IPSCommentDao#find(com
     * .percussion.delivery.comments.data.PSCommentCriteria, boolean)
     */
    @Transactional
    public List<IPSComment> find(PSCommentCriteria criteria) throws Exception
    {
        var session = getSession();
        var queryCriteria = session.createCriteria(PSComment.class);
        prepareCriteria(criteria, queryCriteria);
        return queryCriteria.list();
    }


    @Transactional
    public Set<String> findSitesForCommentIds(Collection<String> ids)
    {
        var longIds = ids.stream()
            .map(Long::valueOf)
            .collect(Collectors.toList());

        String selectComments = "select site from PSComment where id in (:idList)";
        var siteNames = (List<String>) this.getHibernateTemplate()
            .findByNamedParam(selectComments, "idList", longIds);

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
    public void save(IPSComment comment) throws Exception
    {
        var hComment = new PSComment(comment);
        hComment.setId(comment.getId());
        getHibernateTemplate().saveOrUpdate(hComment);
        comment.setId(hComment.getId());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.percussion.delivery.comments.service.rdbms.IPSCommentDao#delete(java
     * .util.Collection)
     */
    @Transactional
    public void delete(Collection<String> commentIds)
    {
        var longIds = commentIds.stream()
            .map(Long::valueOf)
            .collect(Collectors.toList());

        var session = getSession();
        session.createQuery("delete from PSComment where id in (:commentIds)")
            .setParameterList("commentIds", longIds)
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
    public void moderate(Collection<String> commentIds, APPROVAL_STATE newApprovalState) throws Exception
    {
        var longIds = commentIds.stream()
            .map(Long::valueOf)
            .collect(Collectors.toList());

        var session = getSession();
        try
        {
            String updateQueryString = "update PSComment com set approvalState = :newApprovalState "
                    + "where com.id in (:idList) ";

            var updateQuery = session.createQuery(updateQueryString);
            updateQuery.setParameter("newApprovalState", newApprovalState.toString());
            updateQuery.setParameterList("idList", longIds);
            updateQuery.executeUpdate();
        }
        finally
        {
            session.flush();
        }
    }

    @Transactional(readOnly = true)
    public List<PSPageInfo> findPagesWithComments(String site) throws Exception
    {
        var session = getSession();

        try
        {
            var stringQuery = "select pagePath, approvalState, count(*), viewed " +
                             "from PSComment " +
                             "where site = :site " +
                             "group by pagePath, approvalState, viewed ";

            var query = session.createQuery(stringQuery);
            query.setParameter("site", site);

            var result = query.list();
            var pages = new ArrayList<PSPageInfo>();

            for(var r : result) {
                var objArray = (Object[])r;
                pages.add(new PSPageInfo(
                    (String)objArray[0],
                    (String)objArray[1],
                    (Long)objArray[2],
                    (Boolean)objArray[3]
                ));
            }

            return pages;
        }
        finally
        {
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
        var defaultState = new PSDefaultModerationState(sitename, state.toString());
        session.saveOrUpdate(defaultState);
        session.flush();
    }

    /**
     * Prepares the Hibernate Criteria object according to the settings in
     * PSCommentCriteria object.
     *
     * @param criteria The comment criteria. Must not be <code>null</code>.
     * @param queryCriteria The Hibernate Criteria object.
     */
    private void prepareCriteria(PSCommentCriteria criteria, Criteria queryCriteria) {
        var ands = Restrictions.conjunction();
        var ors = (Conjunction)null;

        // Username
        if (!StringUtils.isEmpty(criteria.getUsername())) {
            ands.add(Restrictions.eq("username", criteria.getUsername()).ignoreCase());
        }

        // Pagepath
        if (!StringUtils.isEmpty(criteria.getPagepath())) {
            ands.add(Restrictions.eq("pagePath", criteria.getPagepath()).ignoreCase());
        }

        // Tag
        if (!StringUtils.isEmpty(criteria.getTag())) {
            queryCriteria.createAlias("commentTags", "tag");
            ands.add(Restrictions.eq("tag.name", criteria.getTag()).ignoreCase());
        }

        // Approval state
        if (criteria.getState() != null) {
            ands.add(Restrictions.eq("approvalState", criteria.getState().toString()));
        }

        // Site
        if (!StringUtils.isEmpty(criteria.getSite())) {
            ands.add(Restrictions.eq("site", criteria.getSite()).ignoreCase());
        }

        // Viewed
        if (criteria.isViewed() != null) {
            ands.add(Restrictions.eq("viewed", criteria.isViewed()));
        }

        // Moderated
        if (criteria.isModerated() != null) {
            ands.add(Restrictions.eq("moderated", criteria.isModerated()));
        }

        // Last comment Id
        if (criteria.getLastCommentId() != null) {
            ors = Restrictions.conjunction();
            ors.add(Restrictions.eq("id", Long.valueOf(criteria.getLastCommentId())));
            ors.add(Restrictions.eq("site", criteria.getSite()).ignoreCase());

            if (!StringUtils.isEmpty(criteria.getPagepath())) {
                ors.add(Restrictions.eq("pagePath", criteria.getPagepath()).ignoreCase());
            }
        }

        if (ors != null) {
            queryCriteria.add(Restrictions.or(ands, ors));
        } else {
            queryCriteria.add(ands);
        }

        // Sorting
        if (criteria.getSort() != null && criteria.getSort().getSortBy() != null) {
            var field = PSCommentsService.SORTBY_FIELD_MAPPING.get(criteria.getSort().getSortBy());
            var isAscending = criteria.getSort().isAscending();

            if (isAscending) {
                queryCriteria.addOrder(Order.asc(field));
            } else {
                queryCriteria.addOrder(Order.desc(field));
            }
        } else {
            // By default, sort by CREATEDATE in descending order
            queryCriteria.addOrder(Order.desc(PSCommentsService.SORTBY_FIELD_MAPPING.get(SORTBY.CREATEDDATE)));
        }

        // Max results
        if (criteria.getMaxResults() > 0) {
            queryCriteria.setMaxResults(criteria.getMaxResults());
        }

        // Start index
        if (criteria.getStartIndex() > 0) {
            queryCriteria.setFirstResult(criteria.getStartIndex());
        }

        // Unique entities
        queryCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
    }

    private Session getSession() {
        return getSessionFactory().getCurrentSession();
    }
}
