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

package com.percussion.recent.dao.impl;

import com.percussion.recent.dao.IPSRecentDao;
import com.percussion.recent.data.PSRecent;
import com.percussion.recent.data.PSRecent.RecentType;
import com.percussion.share.dao.IPSGenericDao.SaveException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import java.util.LinkedList;
import java.util.List;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Hibernate implementation of {@link IPSRecentDao}. */
@Repository("recentDao")
@Transactional
public class PSRecentDao implements IPSRecentDao {

  @PersistenceContext private EntityManager entityManager;

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  public PSRecentDao() {
    // Default constructor
  }

  /** {@inheritDoc} */
  @Override
  public List<PSRecent> find(String user, String siteName, RecentType type) {
    var session = getSession();
    var builder = session.getCriteriaBuilder();
    var criteria = builder.createQuery(PSRecent.class);
    var recent = criteria.from(PSRecent.class);
    var predList = new LinkedList<Predicate>();

    if (user != null) {
      predList.add(builder.equal(recent.get("user"), user));
    }
    if (siteName != null) {
      predList.add(builder.equal(recent.get("siteName"), siteName));
    }
    if (type != null) {
      predList.add(builder.equal(recent.get("type"), type));
    }
    var preds = predList.toArray(new Predicate[0]);
    criteria.where(preds);
    criteria.orderBy(builder.asc(recent.get("order")));
    return entityManager.createQuery(criteria).getResultList();
  }

  /** {@inheritDoc} */
  @Override
  public void saveAll(List<PSRecent> recentList) {
    for (var recent : recentList) {
      getSession().merge(recent);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(PSRecent recent) {
    getSession().remove(recent);
  }

  /** {@inheritDoc} */
  @Override
  public void deleteAll(List<PSRecent> recentList) {
    for (var recent : recentList) {
      getSession().remove(recent);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void save(PSRecent recent) throws SaveException {
    getSession().merge(recent);
  }
}
