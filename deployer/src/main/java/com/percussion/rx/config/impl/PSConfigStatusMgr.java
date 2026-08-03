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

package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigStatusMgr;
import com.percussion.rx.config.data.PSConfigStatus;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.system.utils.PSBaseBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link IPSConfigStatusMgr} backed by JPA. Stores and retrieves
 * per-package configuration application status.
 */
@Transactional
@PSBaseBean("sys_configStatusMgr")
public class PSConfigStatusMgr implements IPSConfigStatusMgr {

  /** Default constructor for use by Spring. */
  public PSConfigStatusMgr() {}

  @PersistenceContext private EntityManager entityManager;

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  @Override
  public PSConfigStatus createConfigStatus(String configName) {
    if (StringUtils.isBlank(configName))
      throw new IllegalArgumentException("configName must not be blank");
    var cs = new PSConfigStatus();
    IPSGuidManager gmgr = PSGuidManagerLocator.getGuidMgr();
    int stid = gmgr.createId("CONFIG_STATUS_ID");
    cs.setStatusId(stid);
    cs.setConfigName(configName);
    return cs;
  }

  @Override
  public void saveConfigStatus(PSConfigStatus obj) {
    if (obj == null) throw new IllegalArgumentException("obj may not be null");
    getSession().merge(obj);
  }

  @Override
  public PSConfigStatus loadConfigStatus(long statusID) throws PSNotFoundException {
    return loadConfigStatusModifiable(statusID);
  }

  @Override
  public PSConfigStatus loadConfigStatusModifiable(long statusID) throws PSNotFoundException {
    Session session = getSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<PSConfigStatus> criteria = builder.createQuery(PSConfigStatus.class);
    Root<PSConfigStatus> critRoot = criteria.from(PSConfigStatus.class);
    criteria.where(builder.equal(critRoot.get("statusid"), statusID));
    var cfgStatus = entityManager.createQuery(criteria).getSingleResult();

    if (cfgStatus == null) {
      var msg = "Failed to find config status for supplied status id ({0})";
      throw new PSNotFoundException(MessageFormat.format(msg, statusID));
    }
    return cfgStatus;
  }

  @Override
  public List<PSConfigStatus> findConfigStatus(String nameFilter) {
    if (StringUtils.isBlank(nameFilter))
      throw new IllegalArgumentException("nameFilter may not be null or empty string");

    Session session = getSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<PSConfigStatus> criteria = builder.createQuery(PSConfigStatus.class);
    Root<PSConfigStatus> critRoot = criteria.from(PSConfigStatus.class);
    criteria.where(
        builder.like(builder.lower(critRoot.get("configName")), nameFilter.toLowerCase()));
    criteria.orderBy(
        builder.asc(critRoot.get("configName")), builder.desc(critRoot.get("dateApplied")));
    return entityManager.createQuery(criteria).getResultList();
  }

  @Override
  public List<PSConfigStatus> findLatestConfigStatus(String nameFilter) {
    if (StringUtils.isBlank(nameFilter))
      throw new IllegalArgumentException("nameFilter may not be null or empty string");

    var resultList = new ArrayList<PSConfigStatus>();
    var cfgStatusList = findConfigStatus(nameFilter);
    if (!cfgStatusList.isEmpty()) {
      var currPkg = cfgStatusList.get(0);
      resultList.add(currPkg);
      for (var status : cfgStatusList) {
        if (!currPkg.getConfigName().equalsIgnoreCase(status.getConfigName())) {
          resultList.add(status);
          currPkg = status;
        }
      }
    }
    return resultList;
  }

  @Override
  public void deleteConfigStatus(long statusID) throws PSNotFoundException {
    var cfgStatus = loadConfigStatusModifiable(statusID);
    getSession().remove(cfgStatus);
  }

  @Override
  public void deleteConfigStatus(String nameFilter) {
    if (StringUtils.isBlank(nameFilter))
      throw new IllegalArgumentException("nameFilter may not be null or empty string");

    Session sess = getSession();
    CriteriaBuilder builder = sess.getCriteriaBuilder();
    CriteriaQuery<PSConfigStatus> criteria = builder.createQuery(PSConfigStatus.class);
    Root<PSConfigStatus> critRoot = criteria.from(PSConfigStatus.class);
    criteria.where(
        builder.like(builder.lower(critRoot.get("configName")), nameFilter.toLowerCase()));
    criteria.orderBy(
        builder.asc(critRoot.get("configName")), builder.asc(critRoot.get("dateApplied")));
    entityManager.createQuery(criteria).getResultList().forEach(sess::remove);
  }

  @Override
  public PSConfigStatus findLastSuccessfulConfigStatus(String configName) {
    if (StringUtils.isBlank(configName))
      throw new IllegalArgumentException("configName may not be null or empty string");
    Session session = getSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<PSConfigStatus> criteria = builder.createQuery(PSConfigStatus.class);
    Root<PSConfigStatus> critRoot = criteria.from(PSConfigStatus.class);
    criteria.where(
        builder.equal(critRoot.get("configName"), configName),
        builder.equal(critRoot.get("status"), PSConfigStatus.ConfigStatus.SUCCESS));
    criteria.orderBy(builder.desc(critRoot.get("dateApplied")));
    entityManager.createQuery(criteria).setMaxResults(1);
    var cfgList = entityManager.createQuery(criteria).getResultList();
    return cfgList.isEmpty() ? null : cfgList.get(0);
  }
}
