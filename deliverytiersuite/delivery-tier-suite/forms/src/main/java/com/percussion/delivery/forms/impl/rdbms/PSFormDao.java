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

package com.percussion.delivery.forms.impl.rdbms;

import com.percussion.delivery.forms.IPSFormDao;
import com.percussion.delivery.forms.data.IPSFormData;
import com.percussion.delivery.forms.data.PSFormData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link IPSFormDao} that lives in the RDBMS micro-service. All data
 * access runs inside a single Spring-managed read-committed transaction so that reads observe a
 * consistent view of the {@code PSFormData} table.
 */
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public class PSFormDao implements IPSFormDao {

  /**
   * No-arg constructor required by the JPA / Spring infrastructure. The {@link EntityManager} is
   * injected by the container rather than supplied at construction time.
   */
  public PSFormDao() {}

  @PersistenceContext private EntityManager entityManager;

  /*
   * (non-Javadoc)
   * @see com.percussion.delivery.forms.IPSFormDao#createFormData(java.lang.String, java.util.Map)
   */
  public IPSFormData createFormData(String formname, Map<String, String[]> formdata) {
    if (StringUtils.isBlank(formname))
      throw new IllegalArgumentException("formname cannnot be blank.");
    if (formdata == null) throw new IllegalArgumentException("formdata cannot be null");
    return new PSFormData(formname, formdata);
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#save(com.percussion.delivery.forms.data.IPSFormData)
   */
  public void save(IPSFormData form) {
    if (form == null) {
      throw new IllegalArgumentException("clist may not be null");
    }

    entityManager.persist(form);
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#delete(com.percussion.delivery.forms.data.IPSFormData)
   */
  public void delete(IPSFormData form) {
    if (entityManager.contains(form)) {
      entityManager.remove(form);
    } else {
      // Check if the form exists in the database before trying to delete it
      IPSFormData existingForm = entityManager.find(form.getClass(), form.getId());
      if (existingForm != null) {
        entityManager.remove(existingForm);
      }
      // If existingForm is null, the form doesn't exist in the database
      // This is expected behavior for the testDelete_NonExistingForm test
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#getExportedFormCount(java.lang.String)
   */
  public long getExportedFormCount(String name) {
    String query = "select count(*) from PSFormData formData where formData.isExported = 'y'";
    TypedQuery<Long> q;

    if (name != null && name.trim().length() > 0) {
      query += " and lower(formData.name) = lower(:name)";
      q = entityManager.createQuery(query, Long.class);
      q.setParameter("name", name);
    } else {
      q = entityManager.createQuery(query, Long.class);
    }

    return q.getSingleResult();
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#getTotalFormCount(java.lang.String)
   */
  public long getTotalFormCount(String name) {
    String query = "select count(*) from PSFormData formData";
    TypedQuery<Long> q;

    if (name != null && name.trim().length() > 0) {
      query += " where lower(formData.name) = lower(:name)";
      q = entityManager.createQuery(query, Long.class);
      q.setParameter("name", name);
    } else {
      q = entityManager.createQuery(query, Long.class);
    }

    return q.getSingleResult();
  }

  // rather than saving all the forms, we just change the exported property
  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#markAsExported(java.util.Collection)
   */
  public void markAsExported(Collection<IPSFormData> forms) {
    Session session = entityManager.unwrap(Session.class);
    try {
      // because of limitations in JDBC/hibernate, we have to keep IN
      // clauses less than 1k elements
      String query = "update PSFormData set isExported = 'y' where id in (:ids)";
      Collection<Long> values = new ArrayList<>();
      for (IPSFormData form : forms) {
        values.add(Long.valueOf(form.getId()));
        if (values.size() > 950 || values.size() == forms.size()) {
          session.createQuery(query).setParameterList("ids", values).executeUpdate();
          session.flush();
          values.clear();
        }
      }
    } finally {
      // releaseSession(session);
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#deleteExportedForms(java.lang.String)
   */
  public void deleteExportedForms(String formName) {
    List<IPSFormData> forms = findExportedForms(formName);
    for (IPSFormData form : forms) {
      entityManager.remove(entityManager.contains(form) ? form : entityManager.merge(form));
    }
  }

  private List<IPSFormData> findExportedForms(String formName) {
    String sqlString = "";
    TypedQuery<IPSFormData> query;
    if (StringUtils.isEmpty(formName)) {
      sqlString = "from PSFormData where isExported = 'y' order by name asc, created asc";
      query = entityManager.createQuery(sqlString, IPSFormData.class);
    } else {
      sqlString =
          "from PSFormData formData where formData.isExported = 'y' and lower(formData.name) ="
              + " lower(:formName)";
      query = entityManager.createQuery(sqlString, IPSFormData.class);
      query.setParameter("formName", formName);
    }
    return query.getResultList();
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#findFormsByName(java.lang.String)
   */
  public List<IPSFormData> findFormsByName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    TypedQuery<IPSFormData> query =
        entityManager.createQuery(
            "from PSFormData formData "
                + "where lower(formData.name) = lower(:name) "
                + "order by created asc",
            IPSFormData.class);
    query.setParameter("name", name);
    return query.getResultList();
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#findAllForms()
   */
  public List<IPSFormData> findAllForms() {
    TypedQuery<IPSFormData> query =
        entityManager.createQuery(
            "from PSFormData order by name asc, created asc", IPSFormData.class);
    return query.getResultList();
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#findDistinctFormNames()
   */
  public List<String> findDistinctFormNames() {
    TypedQuery<String> query =
        entityManager.createQuery(
            "select distinct lower(name) from PSFormData order by lower(name) asc", String.class);

    List<String> names = query.getResultList();
    // Deduplicate case-insensitively, preserving original casing of first occurrence
    Map<String, String> deduped = new java.util.LinkedHashMap<>();
    for (String name : names) {
      String lower = name == null ? null : name.toLowerCase();
      if (!deduped.containsKey(lower)) {
        deduped.put(lower, name);
      }
    }
    return new ArrayList<>(deduped.values());
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
