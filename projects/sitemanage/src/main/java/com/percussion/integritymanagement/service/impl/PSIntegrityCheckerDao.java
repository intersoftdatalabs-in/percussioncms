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

// REFACTORED: CP-JAVA11
package com.percussion.integritymanagement.service.impl;

import com.percussion.integritymanagement.data.PSIntegrityStatus;
import com.percussion.integritymanagement.data.PSIntegrityStatus.Status;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidHelper;
import com.percussion.share.dao.IPSGenericDao.SaveException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** DAO implementation for integrity checker persistence. */
@Transactional
@Repository("integrityCheckerDao")
public class PSIntegrityCheckerDao
    implements com.percussion.integritymanagement.service.IPSIntegrityCheckerDao {

  @PersistenceContext private EntityManager entityManager;

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  public PSIntegrityCheckerDao() {
    super();
  }

  @Override
  @Transactional
  public PSIntegrityStatus find(String token) {
    var session = getSession();
    var builder = session.getCriteriaBuilder();
    var criteria = builder.createQuery(PSIntegrityStatus.class);
    var critRoot = criteria.from(PSIntegrityStatus.class);

    if (token != null) {
      criteria.where(builder.equal(critRoot.get("token"), token));
    }
    criteria.orderBy(builder.desc(critRoot.get("startTime")));
    var results = entityManager.createQuery(criteria).getResultList();
    return results.isEmpty() ? null : results.get(0);
  }

  @Override
  @Transactional
  public List<PSIntegrityStatus> find(Status status) {
    var session = getSession();
    var builder = session.getCriteriaBuilder();
    var criteria = builder.createQuery(PSIntegrityStatus.class);
    var critRoot = criteria.from(PSIntegrityStatus.class);

    if (status != null) {
      criteria.where(builder.equal(critRoot.get("status"), status));
    }
    criteria.orderBy(builder.desc(critRoot.get("startTime")));
    return entityManager.createQuery(criteria).getResultList();
  }

  @Override
  @Transactional
  public void delete(PSIntegrityStatus intStatus) {
    var session = getSession();
    try {
      session.delete(intStatus);
    } finally {
      session.flush();
    }
  }

  @Override
  @Transactional
  public void save(PSIntegrityStatus status) throws SaveException {
    var session = getSession();
    try {
      setValidPersistedIds(status);
      session.saveOrUpdate(status);
    } finally {
      session.flush();
    }
  }

  /**
   * Sets the persisted IDs for the properties of the supplied integrity status if needed.
   *
   * @param status the integrity status, assumed not null
   */
  private void setValidPersistedIds(PSIntegrityStatus status) {
    for (var t : status.getTasks()) {
      if (t.getTaskId() == -1L) {
        var nextId = PSGuidHelper.generateNext(PSTypeEnum.INTEGRITY_TASK).longValue();
        t.setTaskId(nextId);
      }
      for (var prop : t.getTaskProperties()) {
        var nextPropId = PSGuidHelper.generateNext(PSTypeEnum.INTEGRITY_TASK_PROPERTY).longValue();
        prop.setTaskPropertyId(nextPropId);
      }
    }
  }
}
