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
import com.percussion.deployer.server.dependencies.PSFilterInstallUtils;
import com.percussion.deployer.server.dependencies.PSSiteDefDependencyHandler;
import com.percussion.deployer.server.dependencies.PSTemplateDefDependencyHandler;
import com.percussion.deployer.server.dependencies.PSVariantDefDependencyHandler;
import com.percussion.deployer.services.IPSDeployService;
import com.percussion.deployer.services.PSDeployServiceException;
import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.jdbc.PSJdbcConnectionDiagnostics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * An MSM related service which delineates the transaction boundaries for specific assembly
 * elements. Handles deserialization, transformation, and persistence of deployment objects.
 *
 * <p><b>Transaction policy:</b> roll back on any {@link Exception}. The previous {@code
 * noRollbackFor = Exception.class} caused Hibernate/nested {@code RuntimeException} failures to
 * mark the TX rollback-only, after which Spring attempted to <em>commit</em> and only reported
 * {@code UnexpectedRollbackException} — hiding the real cause (filter/keyword package install).
 */
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class PSDeployService implements IPSDeployService {
  private static final Logger log = LogManager.getLogger(PSDeployService.class);

  /** Default constructor for use by Spring. */
  public PSDeployService() {}

  private SessionFactory sessionFactory;

  /**
   * Returns the session factory used by this service.
   *
   * @return the session factory, may be <code>null</code>.
   */
  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  /**
   * Sets the session factory used by this service.
   *
   * @param sessionFactory the session factory, may not be <code>null</code>.
   */
  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * A helper method for installing dependency files within a transaction boundary. Handles
   * deserialization, applying transforms, persisting/updating via HibernateTransactionManager.
   *
   * @param tok the security token, may not be <code>null</code>.
   * @param archive the archive handler, may not be <code>null</code>.
   * @param dep the dependency being installed, may not be <code>null</code>.
   * @param depFile the dependency file, may not be <code>null</code>.
   * @param ctx the import context, may not be <code>null</code>.
   * @param depHandler the dependency handler, may not be <code>null</code>.
   * @param s the site, may not be <code>null</code>.
   * @param ver the version, may be <code>null</code>.
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
    String filterName = null;
    try {
      // Always deserialize package payload into a fresh (non-managed) object.
      var packageFilter = dh.generateFilterFromFile(tok, archive, dep, depFile, ctx, null);
      packageFilter = dh.doTransforms(packageFilter, dep, ctx, true);

      IPSFilterService filterSvc = PSFilterServiceLocator.getFilterService();

      // Prefer match by natural id (name). Package dep ids often do not match a prior
      // install's GUID; treating that as "new" then persist hits NAME unique constraint
      // only at flush → UnexpectedRollbackException with no app-level exception.
      // Only FILTER_MISSING means first install; blank names and other PSFilterException
      // codes must not be swallowed (Kilo review / unique-constraint mask).
      com.percussion.services.filter.IPSItemFilter existingByName = null;
      filterName = packageFilter.getName();
      try {
        existingByName = filterSvc.findFilterByName(filterName);
      } catch (IllegalArgumentException e) {
        throw new PSDeployException(
            DeploymentErrorCodes.UNEXPECTED_ERROR,
            e,
            "Filter package has blank or invalid name while installing dependency "
                + dep.getDependencyId());
      } catch (com.percussion.services.filter.PSFilterException e) {
        if (!PSFilterInstallUtils.isFilterMissingErrorCode(e.getErrorCode())) {
          throw new PSDeployException(
              DeploymentErrorCodes.UNEXPECTED_ERROR,
              e,
              "Failed to resolve existing filter by name: " + filterName);
        }
        // True first install — row not present
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
              DeploymentErrorCodes.UNEXPECTED_ERROR,
              "Could not load the existing filter: " + packageFilter.getName());
        }
        PSItemFilter managed = (PSItemFilter) existingByName;
        Integer lver = managed.getVersion();
        // Domain-merge package fields onto the managed row; keep its version untouched.
        try {
          managed.merge(packageFilter);
        } catch (com.percussion.services.filter.PSFilterException e) {
          throw new PSDeployException(
              DeploymentErrorCodes.UNEXPECTED_ERROR,
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
      logJdbcDiagnosticsOnFilterFailure(dep, filterName, e);
      throw new PSDeployServiceException(e);
    } catch (RuntimeException e) {
      // Hibernate/H2 VALUE keyword failures surface here (Baseline perc_public filter).
      logJdbcDiagnosticsOnFilterFailure(dep, filterName, e);
      throw e;
    }
  }

  private void logJdbcDiagnosticsOnFilterFailure(
      PSDependency dep, String filterName, Exception cause) {
    try (var conn = PSConnectionHelper.getDbConnection()) {
      log.error(
          "Filter dependency install failed (depId={}, filterName={}): {} | JDBC: {}",
          dep != null ? dep.getDependencyId() : null,
          filterName,
          cause != null ? cause.toString() : null,
          PSJdbcConnectionDiagnostics.describeConnection(conn));
    } catch (Exception diagEx) {
      log.error(
          "Filter dependency install failed (depId={}): {}; JDBC diagnostics unavailable: {}",
          dep != null ? dep.getDependencyId() : null,
          cause != null ? cause.toString() : null,
          diagEx.toString());
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
