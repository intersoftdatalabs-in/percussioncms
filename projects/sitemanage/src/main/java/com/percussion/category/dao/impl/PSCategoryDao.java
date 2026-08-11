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

package com.percussion.category.dao.impl;

import com.percussion.category.dao.IPSCategoryDao;
import com.percussion.services.contentmgr.impl.IPSContentRepository;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of {@link IPSCategoryDao}. Provides methods to delete categories and
 * retrieve page IDs by category.
 *
 * @author chriswright
 */
@Transactional
@Repository("categoryDao")
public class PSCategoryDao implements IPSCategoryDao {

  @PersistenceContext private EntityManager entityManager;

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  @Autowired private IPSContentRepository contentRepository;

  @Autowired private IPSIdMapper idMapper;

  private static final Logger log = LogManager.getLogger(PSCategoryDao.class);

  private PSCategoryDao() {
    // For Spring
  }

  /** {@inheritDoc} */
  @Override
  public void delete(Set<String> ids, List<IPSGuid> pageIds) {
    log.info("Category IDs to delete: {}", ids);
    var session = getSession();
    try {
      ids.forEach(
          id -> {
            var queryStr = "DELETE FROM PSCategoryEntity WHERE pageCategoriesTree LIKE :id";
            var query = session.createQuery(queryStr);
            query.setParameter("id", "%" + id + "%");
            int result = query.executeUpdate();
            log.info("Deleted {} records for category ID: {}", result, id);
          });
    } catch (HibernateException e) {
      log.error("Error deleting page categories from the database.", e);
    }
    contentRepository.evict(pageIds);
  }

  /** {@inheritDoc} */
  @Override
  public List<Integer> getPageIdsFromCategoryIds(Set<String> ids) {
    log.info("Category IDs to retrieve page IDs for: {}", ids);
    var session = getSession();
    var pageIds = new ArrayList<Integer>();
    for (var id : ids) {
      var queryStr = "SELECT DISTINCT id FROM PSCategoryEntity WHERE pageCategoriesTree LIKE :id";
      var query = session.createQuery(queryStr);
      query.setParameter("id", "%" + id + "%");
      try {
        List<?> result = query.list();
        if (result != null) {
          pageIds.addAll(
              result.stream()
                  .filter(Integer.class::isInstance)
                  .map(Integer.class::cast)
                  .collect(Collectors.toList()));
        }
      } catch (HibernateException e) {
        log.error("Error executing category query to get page IDs for category ID {}.", id, e);
      }
    }
    log.info("Page IDs returned from the category IDs: {}", pageIds);
    return pageIds;
  }
}
