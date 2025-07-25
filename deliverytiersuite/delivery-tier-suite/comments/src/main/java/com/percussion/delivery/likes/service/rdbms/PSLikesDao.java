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

package com.percussion.delivery.likes.service.rdbms;

import com.percussion.delivery.likes.data.IPSLikes;
import com.percussion.delivery.likes.services.IPSLikesDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DAO implementation for PSLikes using Hibernate.
 * Thread-safe, Google Java Style, and Java 11 features.
 */
@Repository
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public class PSLikesDao implements IPSLikesDao {

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Deletes likes by their IDs.
     *
     * @param ids collection of like IDs
     * @throws Exception if deletion fails
     */
    public void delete(Collection<String> ids) throws Exception {
        var longIds = ids.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        var session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaDelete<PSLikes> deleteQuery = builder.createCriteriaDelete(PSLikes.class);
        Root<PSLikes> root = deleteQuery.from(PSLikes.class);
        deleteQuery.where(root.get("id").in(longIds));
        session.createQuery(deleteQuery).executeUpdate();
    }

    private Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * Finds likes for a given site.
     *
     * @param siteName the site name
     * @return list of likes
     * @throws Exception if query fails
     */
    public List<IPSLikes> findLikesForSite(String siteName) throws Exception {
        var session = getSession();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<IPSLikes> criteriaQuery = criteriaBuilder.createQuery(IPSLikes.class);
        Root<PSLikes> root = criteriaQuery.from(PSLikes.class);
        criteriaQuery.select(root).where(criteriaBuilder.like(root.get("site"), siteName));
        return session.createQuery(criteriaQuery).getResultList();
    }

    /**
     * Saves a list of likes.
     *
     * @param likes list of likes to save
     * @throws Exception if save fails
     */
    public void save(List<IPSLikes> likes) throws Exception {
        var session = getSession();
        int i = 0;
        for (var like : likes) {
            session.saveOrUpdate(like);
            if (++i % 50 == 0) {
                session.flush();
                session.clear();
            }
        }
    }

    /**
     * Finds likes by site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return list of matching likes
     * @throws Exception if query fails
     */
    public List<IPSLikes> find(String site, String likeId, String type) throws Exception {
        var session = getSession();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<IPSLikes> criteriaQuery = criteriaBuilder.createQuery(IPSLikes.class);
        Root<PSLikes> root = criteriaQuery.from(PSLikes.class);
        criteriaQuery.select(root).where(
                criteriaBuilder.and(
                        criteriaBuilder.like(root.get("site"), site),
                        criteriaBuilder.equal(root.get("type"), type),
                        criteriaBuilder.equal(root.get("likeId"), likeId)
                )
        );
        return session.createQuery(criteriaQuery).getResultList();
    }

    /**
     * Saves a single like.
     *
     * @param like the like to save
     * @throws Exception if save fails
     */
    public void save(IPSLikes like) throws Exception {
        var hlike = new PSLikes(like);
        hlike.setLikeId(like.getLikeId());
        getSession().saveOrUpdate(hlike);
        like.setLikeId(hlike.getLikeId());
    }

    /**
     * Creates a new PSLikes instance.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return new PSLikes instance
     * @throws Exception if creation fails
     */
    public IPSLikes create(String site, String likeId, String type) throws Exception {
        return PSLikes.of(site, likeId, type);
    }

    /**
     * Decrements the total likes for a given site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return new total after decrement
     * @throws Exception if update fails
     */
    public int decrementTotal(String site, String likeId, String type) throws Exception {
        return incDecTotal(site, likeId, type, false);
    }

    /**
     * Increments the total likes for a given site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return new total after increment
     * @throws Exception if update fails
     */
    public int incrementTotal(String site, String likeId, String type) throws Exception {
        return incDecTotal(site, likeId, type, true);
    }

    /**
     * Helper method to increment or decrement total likes.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @param isInc true to increment, false to decrement
     * @return new total
     * @throws Exception if update fails
     */
    int incDecTotal(String site, String likeId, String type, boolean isInc) throws Exception {
        var existing = find(site, likeId, type);
        if (!existing.isEmpty()) {
            var like = existing.get(0);
            var count = like.getTotal();
            if (isInc || count > 0) {
                var session = getSession();
                var newTotal = isInc ? count + 1 : count - 1;
                like.setTotal(newTotal);
                session.saveOrUpdate(like);
                return newTotal;
            }
        }
        return 0;
    }
}
