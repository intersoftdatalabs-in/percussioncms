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
package com.percussion.generickey.utils.services.rdbms.impl;

import com.percussion.generickey.data.IPSGenericKey;
import com.percussion.generickey.services.IPSGenericKeyDao;
import com.percussion.generickey.services.PSGenericKeyExistsException;
import com.percussion.generickey.utils.data.rdbms.impl.PSGenericKey;
import org.hibernate.Session;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Hibernate DAO for generic keys.
 * Sunny Sal: "Hibernate is like Bollywood - lots of drama, but gets the job done!"
 */
@Transactional
public class PSGenericKeyDao extends HibernateDaoSupport implements IPSGenericKeyDao {

    @Override
    public IPSGenericKey createKey() {
        return new PSGenericKey();
    }

    @Override
    public Optional<IPSGenericKey> findByResetKey(String resetKey) {
        Objects.requireNonNull(resetKey, "resetKey must not be null or empty");
        var session = getSession();
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSGenericKey.class);
        var root = criteriaQuery.from(PSGenericKey.class);
        criteriaQuery.select(root).where(criteriaBuilder.equal(root.get("genericKey"), resetKey));
        var result = session.createQuery(criteriaQuery).getResultList();
        if (result.size() > 1) {
            throw new IllegalStateException("More than one generic key entry found for resetKey: " + resetKey);
        }
        return result.stream().findFirst().map(k -> (IPSGenericKey) k);
    }

    @Override
    public void saveKey(IPSGenericKey resetKey) throws Exception {
        Objects.requireNonNull(resetKey, "resetKey must not be null");
        var session = getSession();
        validateNewKey(resetKey.getGenericKey(), session);
        session.saveOrUpdate(resetKey);
        session.flush();
    }

    @Override
    public void deleteKey(IPSGenericKey resetKey) throws Exception {
        Objects.requireNonNull(resetKey, "resetKey must not be null");
        var session = getSession();
        session.delete(resetKey);
        session.flush();
    }

    private Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    /**
     * Validates that a generic key with the supplied resetKey does not already exist.
     *
     * @param resetKey Assumed not null or empty.
     * @param session The session to use, assumed not null.
     * @throws PSGenericKeyExistsException if a generic key with the same value already exists.
     */
    private void validateNewKey(String resetKey, Session session) throws PSGenericKeyExistsException {
        if (findGenericKey(resetKey, session).isPresent()) {
            throw new PSGenericKeyExistsException(resetKey);
        }
    }

    /**
     * Helper method to find the key by reset key within a session.
     *
     * @param resetKey Assumed not null or empty.
     * @param session Assumed not null.
     * @return Optional containing the reset key if found, empty otherwise.
     */
    private Optional<IPSGenericKey> findGenericKey(String resetKey, Session session) {
        var criteriaBuilder = session.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(PSGenericKey.class);
        var root = criteriaQuery.from(PSGenericKey.class);
        criteriaQuery.select(root).where(criteriaBuilder.equal(root.get("genericKey"), resetKey));
        var result = session.createQuery(criteriaQuery).getResultList();
        if (result.size() > 1) {
            throw new IllegalStateException("More than one generic key entry found for genericKey: " + resetKey);
        }
        return result.stream().findFirst().map(k -> (IPSGenericKey) k);
    }
}
