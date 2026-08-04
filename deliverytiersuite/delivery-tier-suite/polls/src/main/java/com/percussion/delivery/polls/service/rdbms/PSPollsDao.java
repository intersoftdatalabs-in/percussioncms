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

package com.percussion.delivery.polls.service.rdbms;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;
import com.percussion.delivery.polls.services.IPSPollsDao;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

// REFACTORED: CP-JAVA11
/**
 * JPA-backed implementation of {@link IPSPollsDao}. Delegates queries to the JPA {@link
 * EntityManager} for the polls feature (typically injected via Spring).
 */
@Transactional
public class PSPollsDao implements IPSPollsDao {

  /**
   * Default constructor required for Spring's component-scanned instantiation paths; the DAO is not
   * usable in that state because no {@link EntityManager} has been injected.
   */
  public PSPollsDao() {}

  /** Transaction-scoped EntityManager proxy (typically from SharedEntityManagerBean). */
  private EntityManager entityManager;

  /**
   * Setter-injected from Spring XML.
   *
   * @param entityManager the transaction-scoped JPA {@link EntityManager}, not {@code null}.
   */
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public IPSPoll find(String pollName) {
    var session = getSession();
    IPSPoll poll = null;
    try {
      var criteriaBuilder = session.getCriteriaBuilder();
      var criteriaQuery = criteriaBuilder.createQuery(PSPoll.class);
      var root = criteriaQuery.from(PSPoll.class);
      criteriaQuery.select(root).where(criteriaBuilder.like(root.get("pollName"), pollName));
      var resultList = session.createQuery(criteriaQuery).getResultList();
      if (!resultList.isEmpty()) {
        poll = resultList.get(0);
      }
      return poll;
    } finally {
      // session.close();
    }
  }

  @Override
  public void save(IPSPoll poll) {
    var session = getSession();
    try {
      session.merge(poll);
    } finally {
      // session.close();
    }
  }

  @Override
  public IPSPoll findByQuestion(String pollQuestion) {
    var session = getSession();
    IPSPoll poll = null;
    try {
      var criteriaBuilder = session.getCriteriaBuilder();
      var criteriaQuery = criteriaBuilder.createQuery(PSPoll.class);
      var root = criteriaQuery.from(PSPoll.class);
      criteriaQuery.where(criteriaBuilder.like(root.get("pollQuestion"), pollQuestion));
      var resultList = session.createQuery(criteriaQuery).getResultList();
      if (!resultList.isEmpty()) {
        poll = resultList.get(0);
      }
      return poll;
    } finally {
      // session.close();
    }
  }

  @Override
  public IPSPoll createEmptyPoll() {
    return new PSPoll();
  }

  @Override
  public IPSPollAnswer createEmptyAnswer() {
    return new PSPollAnswer();
  }

  /**
   * Returns the underlying Hibernate {@link Session} for the injected {@link EntityManager}.
   *
   * @return the Hibernate session, never {@code null}.
   * @throws IllegalStateException when no {@link EntityManager} has been injected yet.
   */
  private Session getSession() {
    if (entityManager == null) {
      throw new IllegalStateException("EntityManager has not been injected");
    }
    return entityManager.unwrap(Session.class);
  }
}
