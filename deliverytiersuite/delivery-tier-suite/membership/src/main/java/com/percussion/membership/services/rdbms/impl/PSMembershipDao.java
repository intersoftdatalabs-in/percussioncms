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
package com.percussion.membership.services.rdbms.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.membership.data.IPSMembership;
import com.percussion.membership.data.IPSMembership.PSMemberStatus;
import com.percussion.membership.data.PSAccountSummary;
import com.percussion.membership.data.rdbms.impl.PSMembership;
import com.percussion.membership.services.IPSMembershipDao;
import com.percussion.membership.services.PSMemberExistsException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository for membership accounts using Hibernate.
 * Sunny Sal: "Hibernate is like Bollywood - lots of drama, but gets the job done!"
 */
public class PSMembershipDao extends HibernateDaoSupport implements IPSMembershipDao {

    private static final Logger log = LogManager.getLogger(PSMembershipDao.class);

    private static final String ACTION_ACTIVATE = "Activate";
    private static final String ACTION_BLOCK = "Block";

    @Override
    @Transactional(readOnly = true)
    public IPSMembership findMemberBySessionId(String sessionId) {
        Validate.notBlank(sessionId, "sessionId may not be empty");
        var session = getSession();
        IPSMembership membership = null;
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSMembership.class);
        var root = criteriaQuery.from(PSMembership.class);
        criteriaQuery.select(root).where(criteriaBuilder.like(root.get("sessionId"), sessionId));
        var result = session.createQuery(criteriaQuery).getResultList();
        if (!result.isEmpty()) {
            if (result.size() > 1) {
                throw new IllegalStateException("More than one membership entry found for sessionID: " + sessionId);
            }
            membership = result.get(0);
        }
        return membership;
    }

    private Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    @Override
    @Transactional
    public IPSMembership findMemberByUserId(String userId) {
        Validate.notBlank(userId, "userId may not be empty");
        var session = getSession();
        return findMember(userId, session);
    }

    @Override
    @Transactional(readOnly = true)
    public IPSMembership findMemberByPwdResetKey(String pwdResetKey) {
        Validate.notBlank(pwdResetKey, "pwdResetKey may not be empty");
        var session = getSession();
        IPSMembership membership = null;
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSMembership.class);
        var root = criteriaQuery.from(PSMembership.class);
        criteriaQuery.select(root).where(criteriaBuilder.like(root.get("pwdResetKey"), pwdResetKey));
        var result = session.createQuery(criteriaQuery).getResultList();
        if (!result.isEmpty()) {
            if (result.size() > 1) {
                throw new IllegalStateException("More than one membership entry found for pwdResetKey: " + pwdResetKey);
            }
            membership = result.get(0);
        }
        return membership;
    }

    @Transactional
    public IPSMembership createMember(String userId, String password, PSMemberStatus status) throws Exception {
        Validate.notBlank(userId, "userId may not be empty");
        Validate.notBlank(password, "password may not be empty");
        Validate.notNull(status, "status must not be null");
        var session = getSession();
        validateNewMember(userId, session);
        IPSMembership membership = new PSMembership();
        membership.setUserId(userId);
        membership.setPassword(password);
        membership.setStatus(status);
        membership.setLastAccessed(new Date());
        return membership;
    }

    @Override
    @Transactional
    public void saveMember(IPSMembership member) throws Exception {
        Validate.notNull(member, "member must not be null");
        var session = getSession();
        if (member.getId().equals("0")) {
            validateNewMember(member.getUserId(), session);
        }
        session.saveOrUpdate(member);
        session.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IPSMembership> findMembers() {
        try {
            var session = getSession();
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(IPSMembership.class);
            var root = criteriaQuery.from(PSMembership.class);
            criteriaQuery.select(root).orderBy(criteriaBuilder.asc(root.get("userId")));
            return session.createQuery(criteriaQuery).getResultList();
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public void changeStatusAccount(PSAccountSummary account) throws Exception {
        Validate.notBlank(account.getEmail().orElse(""), "User email may not be empty");
        Validate.notBlank(account.getAction().orElse(""), "Action may not be empty");
        var session = getSession();
        var member = findMemberByUserId(account.getEmail().orElse(""));
        if (member == null) {
            throw new Exception("Member not found.");
        }
        var action = account.getAction().orElse("");
        if (action.equalsIgnoreCase(ACTION_ACTIVATE)) {
            member.setStatus(PSMemberStatus.Active);
        } else if (action.equalsIgnoreCase(ACTION_BLOCK)) {
            member.setStatus(PSMemberStatus.Blocked);
        } else {
            throw new Exception("Action not allowed.");
        }
        session.saveOrUpdate(member);
        session.flush();
    }

    @Override
    @Transactional
    public void deleteAccount(String email) throws Exception {
        Validate.notBlank(email, "User email may not be empty");
        var session = getSession();
        var member = findMemberByUserId(email);
        if (member == null) {
            throw new Exception("Member not found.");
        }
        var builder = session.getCriteriaBuilder();
        var deleteQuery = builder.createCriteriaDelete(PSMembership.class);
        var root = deleteQuery.from(PSMembership.class);
        deleteQuery.where(root.get("id").in(Long.valueOf(member.getId())));
        session.createQuery(deleteQuery).executeUpdate();
    }

    /**
     * Validate a member with the supplied userId does not already exist
     *
     * @param userId Assumed not null or empty.
     * @param session The session to use, assumed not null.
     * @throws PSMemberExistsException if a member with the same name already exists.
     */
    private void validateNewMember(String userId, Session session) throws PSMemberExistsException {
        if (findMember(userId, session) != null) {
            throw new PSMemberExistsException(userId);
        }
    }

    /**
     * Helper method to find the member by user id w/in a session.
     *
     * @param userId Assumed not null or empty.
     * @param session Assumed not null.
     * @return The member, or null if not found.
     */
    @Transactional
    public IPSMembership findMember(String userId, Session session) {
        IPSMembership membership = null;
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSMembership.class);
        var root = criteriaQuery.from(PSMembership.class);
        var uid = root.get("userId");
        var upper = criteriaBuilder.upper(uid);
        var ctfPredicate = criteriaBuilder.like(upper, userId.toUpperCase());
        criteriaQuery.select(root).where(criteriaBuilder.and(ctfPredicate));
        var result = session.createQuery(criteriaQuery).getResultList();
        if (!result.isEmpty()) {
            if (result.size() > 1) {
                throw new IllegalStateException("More than one membership entry found for userId: " + userId);
            }
            membership = result.get(0);
        }
        return membership;
    }
}
