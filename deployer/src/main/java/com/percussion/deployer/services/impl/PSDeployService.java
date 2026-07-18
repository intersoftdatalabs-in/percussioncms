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

package com.percussion.deployer.services.impl;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.server.IPSServiceDependencyHandler;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyManager;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.deployer.server.dependencies.PSDependencyHandler;
import com.percussion.deployer.server.dependencies.PSFilterDefDependencyHandler;
import com.percussion.deployer.server.dependencies.PSSiteDefDependencyHandler;
import com.percussion.deployer.server.dependencies.PSTemplateDefDependencyHandler;
import com.percussion.deployer.server.dependencies.PSVariantDefDependencyHandler;
import com.percussion.deployer.services.IPSDeployService;
import com.percussion.deployer.services.PSDeployServiceException;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * An MSM related service which delineates the transaction boundaries for specific assembly
 * elements. Handles deserialization, transformation, and persistence of deployment objects.
 */
@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = Exception.class)
public class PSDeployService implements IPSDeployService {
  private SessionFactory sessionFactory;

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * A helper method for installing dependency files within a transaction boundary. Handles
   * deserialization, applying transforms, persisting/updating via HibernateTransactionManager.
   */
  @Override
  public void deserializeAndSaveSite(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler,
      IPSSite s,
      Integer ver)
      throws PSDeployServiceException {
    Objects.requireNonNull(tok, "security token may not be null");
    Objects.requireNonNull(archive, "archive may not be null");
    Objects.requireNonNull(dep, "dependency may not be null");
    Objects.requireNonNull(depFile, "deserialization file may not be null");
    Objects.requireNonNull(ctx, "dependency context may not be null");
    Objects.requireNonNull(depHandler, "dependency handler may not be null");

    var dh = (PSSiteDefDependencyHandler) depHandler;
    try {
      var desSite = dh.generateSiteFromFile(tok, archive, depFile, s, ctx);
      dh.transformSiteData(tok, dep, ctx, desSite);
      dh.saveDeserializedObject(desSite, ver);
    } catch (PSDeployException e) {
      throw new PSDeployServiceException(e);
    }
  }

  /**
   * A helper method for installing dependency files within a transaction boundary. Handles
   * deserialization, applying transforms, persisting/updating via HibernateTransactionManager.
   */
  @Override
  public void deserializeAndSaveTemplate(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler,
      PSAssemblyTemplate t,
      Integer ver,
      HashMap<Long, Integer> bVer)
      throws PSDeployServiceException {
    var dh = (PSTemplateDefDependencyHandler) depHandler;
    try {
      t = dh.generateTemplateFromFile(archive, depFile, t, ctx);
      dh.doTransforms(t, ctx, dep);
      PSTemplateDefDependencyHandler.saveTemplate(t, ver, bVer);
    } catch (PSDeployException e) {
      throw new PSDeployServiceException(e);
    }
  }

  /**
   * A helper method for installing dependency files within a transaction boundary. Handles
   * deserialization, applying transforms, persisting/updating via HibernateTransactionManager.
   */
  @Override
  public void deserializeAndSaveFilter(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler)
      throws PSDeployServiceException {
    Objects.requireNonNull(tok, "security token may not be null");
    Objects.requireNonNull(archive, "archive may not be null");
    Objects.requireNonNull(dep, "dependency may not be null");
    Objects.requireNonNull(depFile, "deserialization file may not be null");
    Objects.requireNonNull(ctx, "dependency context may not be null");
    Objects.requireNonNull(depHandler, "dependency handler may not be null");

    var dh = (PSFilterDefDependencyHandler) depHandler;
    try {
      // Always deserialize package payload into a fresh (non-managed) object.
      var packageFilter = dh.generateFilterFromFile(tok, archive, dep, depFile, ctx, null);
      packageFilter = dh.doTransforms(packageFilter, dep, ctx, true);

      IPSFilterService filterSvc = PSFilterServiceLocator.getFilterService();

      // Prefer match by natural id (name). Package dep ids often do not match a prior
      // install's GUID; treating that as "new" then persist hits NAME unique constraint
      // only at flush → UnexpectedRollbackException with no app-level exception.
      com.percussion.services.filter.IPSItemFilter existingByName = null;
      try {
        existingByName = filterSvc.findFilterByName(packageFilter.getName());
      } catch (com.percussion.services.filter.PSFilterException e) {
        // FILTER_MISSING is expected for true first install
        existingByName = null;
      }

      if (existingByName != null) {
        List<IPSGuid> ids = new ArrayList<>();
        ids.add(existingByName.getGUID());
        try {
          // Reload for a managed instance we will domain-merge into
          existingByName = filterSvc.loadFilter(ids).get(0);
        } catch (PSNotFoundException e) {
          throw new PSDeployException(
              IPSDeploymentErrors.UNEXPECTED_ERROR,
              "Could not load the existing filter: " + packageFilter.getName());
        }
        PSItemFilter managed = (PSItemFilter) existingByName;
        Integer lver = managed.getVersion();
        // Domain-merge package fields onto the managed row; keep its version untouched.
        try {
          managed.merge(packageFilter);
        } catch (com.percussion.services.filter.PSFilterException e) {
          throw new PSDeployException(
              IPSDeploymentErrors.UNEXPECTED_ERROR,
              e,
              "Could not merge package filter onto existing filter: " + packageFilter.getName());
        }
        // Save with DB version (update path in PSFilterManager)
        dh.saveFilter(managed, lver);
      } else {
        // True insert: null version selects persist path in PSFilterManager
        dh.saveFilter(packageFilter, null);
      }
    } catch (PSDeployException e) {
      throw new PSDeployServiceException(e);
    }
  }

  /**
   * A helper method for installing dependency files within a transaction boundary. Handles
   * deserialization, applying transforms, persisting/updating via HibernateTransactionManager.
   */
  @Override
  public void deserializeAndSaveVariant(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler,
      PSAssemblyTemplate t,
      Integer ver)
      throws PSDeployServiceException {
    try {
      var dh = (PSVariantDefDependencyHandler) depHandler;
      var th =
          (PSTemplateDefDependencyHandler)
              PSDependencyManager.getInstance()
                  .getDependencyHandler(PSTemplateDefDependencyHandler.DEPENDENCY_TYPE);

      t = th.generateTemplateFromFile(archive, depFile, t, ctx);
      t = dh.doTransforms(t, ctx, dep);
      PSTemplateDefDependencyHandler.saveTemplate(t, ver, new HashMap<>());
    } catch (PSDeployException e) {
      throw new PSDeployServiceException(e);
    }
  }

  /**
   * See {@link IPSDeployService#installDependencyFiles(PSSecurityToken, PSArchiveHandler,
   * PSDependency, PSImportCtx, IPSServiceDependencyHandler)} for details.
   */
  @Override
  public void installDependencyFiles(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSImportCtx ctx,
      IPSServiceDependencyHandler service)
      throws PSDeployServiceException {
    Objects.requireNonNull(tok, "tok may not be null");
    Objects.requireNonNull(archive, "archive may not be null");
    Objects.requireNonNull(dep, "dep may not be null");
    Objects.requireNonNull(ctx, "ctx may not be null");
    Objects.requireNonNull(service, "service may not be null");

    try {
      service.doInstallDependencyFiles(tok, archive, dep, ctx);
    } catch (PSDeployException | PSNotFoundException e) {
      throw new PSDeployServiceException(e);
    }
  }
}
