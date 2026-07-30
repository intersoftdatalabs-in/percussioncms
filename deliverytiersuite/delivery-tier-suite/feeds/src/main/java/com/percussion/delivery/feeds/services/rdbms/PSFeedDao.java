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
package com.percussion.delivery.feeds.services.rdbms;

import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.services.IPSConnectionInfo;
import com.percussion.delivery.feeds.services.IPSFeedDao;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * RDBMS-backed implementation of {@link IPSFeedDao} using JPA/Hibernate to manage feed descriptors
 * and connection info in the feeds schema.
 *
 * @author erikserating
 */
@Repository
public class PSFeedDao implements IPSFeedDao {

  private static final Logger log = LogManager.getLogger(PSFeedDao.class);

  /** Default no-arg constructor required by Spring. */
  public PSFeedDao() {}

  private SessionFactory sessionFactory;

  /**
   * Spring constructor that wires the Hibernate session factory.
   *
   * @param sessionFactory the Hibernate session factory, never <code>null</code>
   */
  @Autowired
  public PSFeedDao(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * Finds a single feed descriptor by feed name and site.
   *
   * @param name the feed name, never <code>null</code>
   * @param site the feed site, never <code>null</code>
   * @return the matching descriptor, or <code>null</code> if not found
   */
  @Override
  @Transactional
  public IPSFeedDescriptor find(String name, String site) {

    Session session = getSession();
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<IPSFeedDescriptor> criteriaQuery =
        criteriaBuilder.createQuery(IPSFeedDescriptor.class);
    Root<PSFeedDescriptor> root = criteriaQuery.from(PSFeedDescriptor.class);
    criteriaQuery
        .select(root)
        .where(
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("site"), site),
                criteriaBuilder.equal(root.get("name"), name)));

    List<IPSFeedDescriptor> results = session.createQuery(criteriaQuery).getResultList();

    return results.stream().findFirst().orElse(null);
  }

  private Session getSession() {
    return sessionFactory.getCurrentSession();
  }

  /**
   * Finds all feed descriptors belonging to the specified site.
   *
   * @param site the feed site, never <code>null</code>
   * @return the list of descriptors for the site, never <code>null</code>, may be empty
   */
  @Override
  @Transactional
  public List<IPSFeedDescriptor> findBySite(String site) {

    Session session = getSession();
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<IPSFeedDescriptor> criteriaQuery =
        criteriaBuilder.createQuery(IPSFeedDescriptor.class);
    Root<PSFeedDescriptor> root = criteriaQuery.from(PSFeedDescriptor.class);
    criteriaQuery.where(criteriaBuilder.equal(root.get("site"), site));
    criteriaQuery.select(root);
    return session.createQuery(criteriaQuery).getResultList();
  }

  /**
   * Gets the singleton metadata service connection info record.
   *
   * @return the connection info, or <code>null</code> if no row exists
   */
  @Override
  @Transactional
  public IPSConnectionInfo getConnectionInfo() {

    Session session = getSession();

    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<PSConnectionInfo> criteriaQuery =
        criteriaBuilder.createQuery(PSConnectionInfo.class);
    Root<PSConnectionInfo> root = criteriaQuery.from(PSConnectionInfo.class);
    criteriaQuery.select(root);
    List<PSConnectionInfo> results = session.createQuery(criteriaQuery).getResultList();
    return results.stream().findFirst().orElse(null);
  }

  /**
   * Saves (merges) the singleton metadata service connection info row.
   *
   * @param url the metadata service URL, may be <code>null</code>
   * @param user the metadata service user name, may be <code>null</code>
   * @param pass the metadata service password, may be <code>null</code>
   * @param encrypted <code>true</code> if the password is stored encrypted
   */
  @Override
  @Transactional
  public void saveConnectionInfo(String url, String user, String pass, boolean encrypted) {

    Session session = getSession();
    IPSConnectionInfo info = new PSConnectionInfo(url, user, pass, encrypted);
    session.merge(info);
  }

  /**
   * Saves (merges) the supplied feed descriptors. Per-descriptor errors are logged and skipped.
   *
   * @param descriptors the descriptors to save, never <code>null</code>, may be empty
   */
  @Transactional
  public void saveDescriptors(List<IPSFeedDescriptor> descriptors) {

    Session session = getSession();
    List<IPSFeedDescriptor> prepared = prepareDescriptors(descriptors);
    for (IPSFeedDescriptor p : prepared) {
      try {
        session.merge(p);
      } catch (Exception e) {
        log.error(
            "Skipping feed: {} on site {} with link: {} due to error: {} ",
            p.getName(),
            p.getSite(),
            p.getLink(),
            PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  /**
   * Removes the supplied feed descriptors from storage.
   *
   * @param descriptors the descriptors to remove, never <code>null</code>, may be empty
   */
  @Override
  @Transactional
  public void deleteDescriptors(List<IPSFeedDescriptor> descriptors) {

    Session session = getSession();
    List<IPSFeedDescriptor> prepared = prepareDescriptors(descriptors);
    for (IPSFeedDescriptor p : prepared) {
      session.remove(p);
    }
  }

  /**
   * Retrieves all feed descriptors.
   *
   * @return the list of all descriptors, never <code>null</code>, may be empty
   */
  @Override
  @Transactional
  public List<IPSFeedDescriptor> findAll() {
    Session session = getSession();
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<IPSFeedDescriptor> criteriaQuery =
        criteriaBuilder.createQuery(IPSFeedDescriptor.class);
    Root<PSFeedDescriptor> root = criteriaQuery.from(PSFeedDescriptor.class);
    criteriaQuery.select(root);

    return session.createQuery(criteriaQuery).getResultList();
  }

  private List<IPSFeedDescriptor> prepareDescriptors(List<IPSFeedDescriptor> descriptors) {
    List<IPSFeedDescriptor> prepared = new ArrayList<>(descriptors.size());
    for (IPSFeedDescriptor d : descriptors) {
      prepared.add(new PSFeedDescriptor(d));
    }
    return prepared;
  }
}
