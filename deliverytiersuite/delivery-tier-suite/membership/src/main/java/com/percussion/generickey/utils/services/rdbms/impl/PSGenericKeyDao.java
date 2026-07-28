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
package com.percussion.generickey.utils.services.rdbms.impl;

import com.percussion.generickey.data.IPSGenericKey;
import com.percussion.generickey.services.IPSGenericKeyDao;
import com.percussion.generickey.services.PSGenericKeyExistsException;
import com.percussion.generickey.utils.data.rdbms.impl.PSGenericKey;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link IPSGenericKeyDao}. Maps {@link PSGenericKey} rows to {@link
 * IPSGenericKey} via Hibernate criteria queries on the injected {@link SessionFactory}.
 *
 * @author leonardohildt
 */
@Transactional
public class PSGenericKeyDao implements IPSGenericKeyDao {

  /** The Hibernate session factory, may be {@code null} before Spring wires it. */
  private SessionFactory sessionFactory;

  /**
   * Constructs a new DAO with the supplied session factory.
   *
   * @param sessionFactory the Hibernate session factory, may not be <code>null</code>.
   */
  @Autowired
  public PSGenericKeyDao(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /** Default constructor for Spring property injection (backward compatibility). */
  public PSGenericKeyDao() {}

  /**
   * Sets the Hibernate session factory. Retained for legacy bean definitions using property
   * injection.
   *
   * @param sessionFactory the Hibernate session factory, may not be <code>null</code>.
   */
  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /* (non-Javadoc)
   * @see com.percussion.generickey.services.IPSGenericKeyDao#createKey()
   */
  @Override
  public IPSGenericKey createKey() {
    IPSGenericKey key = new PSGenericKey();
    return key;
  }

  @Override
  public IPSGenericKey findByResetKey(String resetKey) {
    Validate.notEmpty(resetKey);
    Session session = getSession();
    try {
      IPSGenericKey genericKey = null;
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<PSGenericKey> criteriaQuery = criteriaBuilder.createQuery(PSGenericKey.class);
      Root<PSGenericKey> root = criteriaQuery.from(PSGenericKey.class);
      criteriaQuery.select(root).where(criteriaBuilder.like(root.get("genericKey"), resetKey));
      List<PSGenericKey> result = session.createQuery(criteriaQuery).getResultList();

      if (!result.isEmpty()) {
        if (result.size() > 1) {
          // this would be a bug
          throw new IllegalStateException(
              "More than one generic key entry found for pwdResetKey: " + resetKey);
        }

        genericKey = (IPSGenericKey) result.get(0);
      }

      return genericKey;
    } finally {
      // session.close();
    }
  }

  @Override
  public void saveKey(IPSGenericKey resetKey) throws Exception {
    Validate.notNull(resetKey);
    Session session = getSession();
    try {
      validateNewKey(resetKey.getGenericKey(), session);
      session.persist(resetKey);
      session.flush();
    } finally {
      // session.close();
    }
  }

  @Override
  public void deleteKey(IPSGenericKey resetKey) throws Exception {
    Validate.notNull(resetKey);
    Session session = getSession();
    try {
      session.remove(resetKey);
      session.flush();
    } finally {
      // session.close();
    }
  }

  private Session getSession() {
    return sessionFactory.getCurrentSession();
  }

  /**
   * Validate a generic key with the supplied resetKey does not already exist
   *
   * @param resetKey Assumed not <code>null</code> or empty.
   * @param session The session to use, assumed not <code>null</code>.
   * @throws PSGenericKeyExistsException if a generic key with the same value already exists.
   */
  private void validateNewKey(String resetKey, Session session) throws PSGenericKeyExistsException {
    if (findGenericKey(resetKey, session) != null) {
      throw new PSGenericKeyExistsException(resetKey);
    }
  }

  /**
   * Helper method to find the key by reset key w/in a session.
   *
   * @param resetKey Assumed not <code>null</code> or empty.
   * @param session Assumed not <code>null</code>.
   * @return The reset key, or <code>null</code> if not found.
   */
  private IPSGenericKey findGenericKey(String resetKey, Session session) {
    IPSGenericKey genericKey = null;

    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<PSGenericKey> criteriaQuery = criteriaBuilder.createQuery(PSGenericKey.class);
    Root<PSGenericKey> root = criteriaQuery.from(PSGenericKey.class);
    criteriaQuery.select(root).where(criteriaBuilder.like(root.get("genericKey"), resetKey));
    List<PSGenericKey> result = session.createQuery(criteriaQuery).getResultList();

    if (!result.isEmpty()) {
      if (result.size() > 1) {
        // this would be a bug
        throw new IllegalStateException(
            "More than one generic key entry found for genericKey: " + resetKey);
      }

      genericKey = (IPSGenericKey) result.get(0);
    }

    return genericKey;
  }
}
