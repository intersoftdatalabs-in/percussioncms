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
package com.percussion.services.pkginfo.impl;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filestorage.data.PSBinaryMetaKey;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.pkginfo.IPSPkgInfoService;
import com.percussion.services.pkginfo.data.PSPkgDependency;
import com.percussion.services.pkginfo.data.PSPkgElement;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageAction;
import com.percussion.services.ui.data.PSHierarchyNodeProperty;
import com.percussion.util.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This is the primary interface for the Package Information Service.  
 * <p>
 * For more information, consult the documentation in the interface definition
 * file @see IPSPkgInfoService
 *
 */
@PSBaseBean("sys_pkgInfoService")
@Transactional
public class PSPkgInfoService implements IPSPkgInfoService {
    private static final Logger log = LogManager.getLogger(PSPkgInfoService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private Session getSession() {
        return entityManager.unwrap(Session.class);
    }

    // Package Information (PSPkgInfo) service methods

    @Override
    public PSPkgInfo createPkgInfo(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name may not be null or empty string");
        }
        var pkgInfo = new PSPkgInfo();
        var guidMgr = PSGuidManagerLocator.getGuidMgr();
        pkgInfo.setGuid(guidMgr.createGuid(PSTypeEnum.PACKAGE_INFO));
        pkgInfo.setPackageDescriptorName(name);
        return pkgInfo;
    }

    @Override
    public PSPkgInfo createPkgInfoCopy(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name may not be null or empty string");
        }
        var pkgInfo = createPkgInfo(name);
        var pkgInfo1 = findPkgInfo(name);
        if (pkgInfo1 != null) {
            pkgInfo.setPublisherName(pkgInfo1.getPublisherName());
            pkgInfo.setPublisherUrl(pkgInfo1.getPublisherUrl());
            pkgInfo.setPackageDescription(pkgInfo1.getPackageDescription());
            pkgInfo.setPackageVersion(pkgInfo1.getPackageVersion());
            pkgInfo.setShippedConfigDefinition(pkgInfo1.getShippedConfigDefinition());
            pkgInfo.setLastAction(pkgInfo1.getLastAction());
            pkgInfo.setLastActionByUser(pkgInfo1.getLastActionByUser());
            pkgInfo.setLastActionStatus(pkgInfo1.getLastActionStatus());
            pkgInfo.setType(pkgInfo1.getType());
            pkgInfo.setPackageDescriptorName(pkgInfo1.getPackageDescriptorName());
            pkgInfo.setPackageDescriptorGuid(pkgInfo1.getPackageDescriptorGuid());
            pkgInfo.setCmVersionMinimum(pkgInfo1.getCmVersionMinimum());
            pkgInfo.setCmVersionMaximum(pkgInfo1.getCmVersionMaximum());
        }
        return pkgInfo;
    }

    @Override
    public void savePkgInfo(PSPkgInfo obj) {
        Objects.requireNonNull(obj, "obj may not be null");
        getSession().saveOrUpdate(obj);
    }

    @Override
    public void deletePkgInfo(IPSGuid pkgGuid) {
        deletePkgInfoChildren(pkgGuid);
        deletePkgInfoRow(pkgGuid);
    }

    private void deletePkgInfoRow(IPSGuid id) {
        Objects.requireNonNull(id, "id may not be null");
        var session = getSession();
        var builder = session.getCriteriaBuilder();
        var criteria = builder.createQuery(PSPkgInfo.class);
        var critRoot = criteria.from(PSPkgInfo.class);
        criteria.where(builder.equal(critRoot.get("guid"), id.longValue()));
        var resultList = entityManager.createQuery(criteria).getResultList();
        if (resultList.isEmpty()) {
            return;
        }
        session.delete(resultList.get(0));
    }

    @Override
    public void deletePkgInfo(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name may not be null or empty string");
        }
        var info = findPkgInfo(name);
        if (info != null) {
            deletePkgInfo(info.getGuid());
        }
    }

    @Override
    public PSPkgInfo findPkgInfo(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        var session = getSession();
        var criteria = session.createCriteria(PSPkgInfo.class);
        criteria.add(Restrictions.eq("descriptorName", name).ignoreCase());
        var pkgInfoList = criteria.list();
        return pkgInfoList.isEmpty() ? null : (PSPkgInfo) pkgInfoList.get(0);
    }

    @Override
    public List<PSPkgInfo> findAllPkgInfos() {
        var session = getSession();
        var criteria = session.createCriteria(PSPkgInfo.class);
        return criteria.list();
    }

    @Override
    public PSPkgInfo loadPkgInfo(IPSGuid id) throws PSNotFoundException {
        Objects.requireNonNull(id, "id may not be null");
        return loadPkgInfoModifiable(id);
    }

    @Override
    public PSPkgInfo loadPkgInfoModifiable(IPSGuid id) throws PSNotFoundException {
        Objects.requireNonNull(id, "id may not be null");
        var pkgInfo = getSession().get(PSPkgInfo.class, id.longValue());
        if (pkgInfo == null) {
            throw new PSNotFoundException(id);
        }
        return pkgInfo;
    }

    // Package Information Element (PSPkgElement) service methods

    @Override
    public PSPkgElement createPkgElement(IPSGuid parentId) {
        var pkgElem = new PSPkgElement();
        var guidMgr = PSGuidManagerLocator.getGuidMgr();
        pkgElem.setGuid(guidMgr.createGuid(PSTypeEnum.PACKAGE_ELEMENT));
        pkgElem.setPackageGuid(parentId);
        return pkgElem;
    }

    @Override
    public void savePkgElement(PSPkgElement obj) {
        Objects.requireNonNull(obj, "obj may not be null");
        log.debug("Trying to save PackageElement: {}", obj);
        getSession().saveOrUpdate(obj);
        log.debug("PackageElement save completed for: {}", obj);
    }

    @Override
    public void deletePkgElement(IPSGuid id) {
        Objects.requireNonNull(id, "id may not be null");
        var session = getSession();
        var builder = session.getCriteriaBuilder();
        var criteria = builder.createQuery(PSPkgElement.class);
        var critRoot = criteria.from(PSPkgElement.class);
        criteria.select(critRoot);
        criteria.where(builder.equal(critRoot.get("guid"), id.longValue()));
        var resultList = entityManager.createQuery(criteria).getResultList();
        if (resultList.isEmpty()) {
            return;
        }
        session.delete(resultList.get(0));
    }

    @Override
    public List<IPSGuid> findPkgElementGuids(IPSGuid parentPkgInfoId) {
        Objects.requireNonNull(parentPkgInfoId, "parentPkgInfoId may not be null");
        var session = getSession();
        var criteria = session.createCriteria(PSPkgElement.class);
        criteria.setProjection(Projections.property("guid"));
        criteria.add(Restrictions.eq("packageGuid", parentPkgInfoId.longValue()));
        var longList = criteria.list();
        return ((List<Long>) longList).stream()
                .map(l -> new PSGuid(PSTypeEnum.PACKAGE_ELEMENT, l))
                .collect(Collectors.toList());
    }

    @Override
    public PSPkgElement findPkgElement(IPSGuid id) {
        Objects.requireNonNull(id, "id may not be null");
        var session = getSession();
        var criteria = session.createCriteria(PSPkgElement.class);
        criteria.add(Restrictions.eq("guid", id.longValue()));
        return (PSPkgElement) criteria.uniqueResult();
    }

    @Override
    public PSPkgElement findPkgElementByObject(IPSGuid objId) {
        Objects.requireNonNull(objId, "objId may not be null");
        var session = getSession();
        String query = "select p from PSPkgElement p, PSPkgInfo i where"
                + " p.objectGuid = :objId"
                + " and i.lastAction != :action and p.packageGuid = i.guid";
        var q = session.createQuery(query);
        q.setString("objId", objId.toString());
        q.setString("action", PackageAction.UNINSTALL.name());
        q.setCacheable(true);
        return (PSPkgElement) q.uniqueResult();
    }

    @Override
    public List<PSPkgElement> findPkgElements(IPSGuid parentPkgId) {
        var session = getSession();
        var builder = session.getCriteriaBuilder();
        var criteria = builder.createQuery(PSPkgElement.class);
        var critRoot = criteria.from(PSPkgElement.class);
        criteria.where(builder.equal(critRoot.get("packageGuid"), parentPkgId.longValue()));
        return entityManager.createQuery(criteria).getResultList();
    }

    @Override
    public List<PSPkgElement> loadPkgElements(List<IPSGuid> ids) throws PSNotFoundException {
        Objects.requireNonNull(ids, "ids may not be null");
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("ids may not be empty");
        }
        var idList = ids.stream()
                .peek(id -> {
                    if (id == null) {
                        throw new IllegalArgumentException("ids may not have null entry");
                    }
                })
                .map(IPSGuid::longValue)
                .collect(Collectors.toList());
        var session = getSession();
        var builder = session.getCriteriaBuilder();
        var criteria = builder.createQuery(PSPkgElement.class);
        var critRoot = criteria.from(PSPkgElement.class);
        criteria.select(critRoot);
        criteria.where(critRoot.get("guid").in(idList));
        var pkgElementList = entityManager.createQuery(criteria).getResultList();
        if (pkgElementList.size() != idList.size()) {
            for (long id : idList) {
                var guid = new PSGuid(id);
                if (findPkgElement(guid) == null) {
                    throw new PSNotFoundException(guid);
                }
            }
        }
        return pkgElementList;
    }

    @Override
    public PSPkgElement loadPkgElement(IPSGuid id) throws PSNotFoundException {
        Objects.requireNonNull(id, "id may not be null");
        return loadPkgElementModifiable(id);
    }

    @Override
    public PSPkgElement loadPkgElementModifiable(IPSGuid id) throws PSNotFoundException {
        Objects.requireNonNull(id, "id may not be null");
        var session = getSession();
        var builder = session.getCriteriaBuilder();
        var criteria = builder.createQuery(PSPkgElement.class);
        var critRoot = criteria.from(PSPkgElement.class);
        criteria.select(critRoot);
        criteria.where(builder.equal(critRoot.get("guid"), id.longValue()));
        var result = entityManager.createQuery(criteria).getSingleResult();
        if (result == null) {
            throw new PSNotFoundException(id);
        }
        return (PSPkgElement) result;
    }

    @Override
    public PSPkgDependency createPkgDependency() {
        var pkgDep = new PSPkgDependency();
        var guidMgr = PSGuidManagerLocator.getGuidMgr();
        pkgDep.setId(guidMgr.createId("PKG_DEPENDENCY_ID"));
        return pkgDep;
    }

    @Override
    public List<IPSGuid> findDependentPkgGuids(IPSGuid guid) {
        Objects.requireNonNull(guid, "guid may not be null");
        var session = getSession();
        var builder = session.getCriteriaBuilder();
        var criteria = builder.createQuery(PSPkgDependency.class);
        var critRoot = criteria.from(PSPkgDependency.class);
        criteria.where(builder.equal(critRoot.get("ownerPackageGuid"), guid.longValue()));
        var pkgDeps = entityManager.createQuery(criteria).getResultList();
        return ((List<PSPkgDependency>) pkgDeps).stream()
                .map(PSPkgDependency::getDependentPackageGuid)
                .collect(Collectors.toList());
    }

    @Override
    public List<IPSGuid> findOwnerPkgGuids(IPSGuid guid) {
        Objects.requireNonNull(guid, "guid may not be null");
        var session = getSession();
        var builder = session.getCriteriaBuilder();
        var criteria = builder.createQuery(PSPkgDependency.class);
        var critRoot = criteria.from(PSPkgDependency.class);
        criteria.where(builder.equal(critRoot.get("dependentPackageGuid"), guid.longValue()));
        var pkgDeps = entityManager.createQuery(criteria).getResultList();
        return ((List<PSPkgDependency>) pkgDeps).stream()
                .map(PSPkgDependency::getOwnerPackageGuid)
                .collect(Collectors.toList());
    }

    @Override
    public List<PSPkgDependency> loadPkgDependencies(IPSGuid guid, boolean depType) {
        Objects.requireNonNull(guid, "ownerGuid may not be null");
        return loadPkgDependenciesModifiable(guid, depType);
    }

    @Override
    public List<PSPkgDependency> loadPkgDependenciesModifiable(IPSGuid guid, boolean depType) {
        Objects.requireNonNull(guid, "ownerGuid may not be null");
        var session = getSession();
        var temp = depType ? "ownerPackageGuid" : "dependentPackageGuid";
        var criteria = session.createCriteria(PSPkgDependency.class);
        criteria.add(Restrictions.eq(temp, guid.longValue()));
        return criteria.list();
    }

    @Override
    public void savePkgDependency(PSPkgDependency pkgDependency) {
        Objects.requireNonNull(pkgDependency, "pkgDependency may not be null");
        getSession().saveOrUpdate(pkgDependency);
    }

    @Override
    public void deletePkgDependency(long pkgDepId) {
        var session = getSession();
        var criteria = session.createCriteria(PSPkgDependency.class);
        criteria.add(Restrictions.eq("pkgDependencyId", pkgDepId));
        var pkgDep = (PSPkgDependency) criteria.uniqueResult();
        if (pkgDep == null) {
            return;
        }
        session.delete(pkgDep);
    }

    @Override
    public void deletePkgInfoChildren(IPSGuid pkgGuid) {
        var pkgElemGuids = findPkgElementGuids(pkgGuid);
        pkgElemGuids.forEach(this::deletePkgElement);
        var deps = loadPkgDependencies(pkgGuid, true);
        deps.forEach(dep -> deletePkgDependency(dep.getId()));
    }

    @Override
    public void deletePkgInfoChildren(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name may not be null or empty string");
        }
        var info = findPkgInfo(name);
        if (info != null) {
            deletePkgInfoChildren(info.getGuid());
        }
    }
}
