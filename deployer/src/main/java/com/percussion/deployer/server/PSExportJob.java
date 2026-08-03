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
package com.percussion.deployer.server;

import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.client.PSCollectionDependencySuppressor;
import com.percussion.deployer.client.PSDeploymentManager;
import com.percussion.deployer.objectstore.PSArchive;
import com.percussion.deployer.objectstore.PSArchiveDetail;
import com.percussion.deployer.objectstore.PSArchiveInfo;
import com.percussion.deployer.objectstore.PSDependencyTreeContext;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.objectstore.PSDescriptor;
import com.percussion.deployer.objectstore.PSExportDescriptor;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSDeployException;
import com.percussion.rx.config.IPSConfigService;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.server.job.IPSJobErrors;
import com.percussion.server.job.PSJobException;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.system.utils.PSFormatVersion;
import java.io.File;
import java.text.MessageFormat;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;

/**
 * Job to create a deployment archive from an export descriptor. Archive created will be named using
 * the export descriptor name.
 */
public class PSExportJob extends PSDeployJob {

  /** Default constructor for use by the job framework. */
  public PSExportJob() {}

  /**
   * Restores the export descriptor from the supplied document, and validates that the user is
   * authorized to perform this job. Saves the security token from the request to use for subsequent
   * operations during the run method. <br>
   * See {@link com.percussion.server.job.PSJobRunner#init(int, Document, PSRequest, Properties)
   * PSJobRunner.init()} for more info.
   */
  @Override
  public void init(int id, Document descriptor, PSRequest req, Properties initParams)
      throws PSAuthenticationFailedException, PSAuthorizationException, PSJobException {
    if (descriptor == null) {
      throw new IllegalArgumentException("descriptor may not be null");
    }
    if (req == null) {
      throw new IllegalArgumentException("req may not be null");
    }

    super.init(id, req, initParams);

    try {
      m_descriptor = new PSExportDescriptor(descriptor.getDocumentElement());
      if (m_descriptor.getPackages().hasNext()) {
        initDepCount(m_descriptor.getPackages());
      }
    } catch (PSDeployException | PSUnknownNodeTypeException e) {
      throw new PSJobException(IPSJobErrors.INVALID_JOB_DESCRIPTOR, e.getMessage());
    }

    m_serverVersion = new PSFormatVersion("com.percussion.util");
  }

  /**
   * Runs this export job. Creates an archive file and stores all files in it that will be required
   * to deploy the items specified by the descriptor supplied to the <code>init()</code> method.
   */
  @Override
  public void doRun() {
    PSArchive archive = null;
    PSDbmsHelper dbmsHelper = null;
    PSDependencyManager dm = null;
    try {
      var bundle = PSDeploymentManager.getBundle();
      setStatusMessage(bundle.getString("init"));

      // enable cache for non-system schema
      dbmsHelper = PSDbmsHelper.getInstance();
      dbmsHelper.enableSchemaCache();

      // make sure all dependencies are in the descriptor
      var dh = PSDeploymentHandler.getInstance();
      dm = (PSDependencyManager) dh.getDependencyManager();

      // enable cache for dependencies
      dm.setIsDependencyCacheEnabled(true);

      // build a full tree context so included state of any added
      // dependencies are updated
      var treeCtx = new PSDependencyTreeContext();
      m_descriptor
          .getPackages()
          .forEachRemaining(de -> treeCtx.addPackage((PSDeployableElement) de, true));

      // add suppression filter from descriptor to context
      if (m_descriptor.getDepKeysToExclude() != null) {
        var suppressor = new PSCollectionDependencySuppressor(m_descriptor.getDepKeysToExclude());
        treeCtx.setDependencySuppressor(suppressor);
      }

      // now add missing deps
      var iter1 = m_descriptor.getPackages();
      while (iter1.hasNext()) {
        var de = iter1.next();
        if (!isCancelled()) {
          var msg =
              MessageFormat.format(bundle.getString("analyzingDeps"), de.getDisplayIdentifier());
          setStatusMessage(msg);
          try {
            dm.addMissingDependencies(getSecurityToken(), (PSDeployableElement) de, treeCtx, this);
          } catch (PSDeployException e) {
            // wrap checked exception so we can handle it in the outer catch
            throw new RuntimeException(e);
          }
        }
      }

      // create the archive
      var archiveFile =
          new File(
              PSDeploymentHandler.EXPORT_ARCHIVE_DIR,
              m_descriptor.getName() + IPSDeployConstants.ARCHIVE_EXTENSION);
      archiveFile.getParentFile().mkdirs();
      archiveFile.deleteOnExit();

      var category = PSPkgInfo.PackageCategory.USER.name();
      var pkgSvc = PSPkgInfoServiceLocator.getPkgInfoService();
      var pkgInfo = pkgSvc.findPkgInfo(m_descriptor.getName());
      if (pkgInfo != null) {
        category = pkgInfo.getCategory().name();
      }

      var info =
          new PSArchiveInfo(
              m_descriptor.getName(),
              PSServer.getHostName() + ":" + PSServer.getListenerPort(),
              m_serverVersion,
              dbmsHelper.getServerRepositoryInfo(),
              getUserId(),
              category);
      var detail = new PSArchiveDetail(m_descriptor);
      info.setArchiveDetail(detail);
      archive = new PSArchive(archiveFile, info);
      var ah = new PSArchiveHandler(archive);

      handleConfigFiles(dh, archive);

      var iter2 = m_descriptor.getPackages();
      while (iter2.hasNext()) {
        var de = iter2.next();
        if (!isCancelled()) {
          var msg = MessageFormat.format(bundle.getString("processing"), de.getDisplayIdentifier());
          setStatusMessage(msg);
          try {
            dm.addToArchive(getSecurityToken(), (PSDeployableElement) de, ah, this);
          } catch (PSDeployException e) {
            throw new RuntimeException(e);
          }
        }
      }

      ah.close();
      if (!isCancelled()) {
        setStatus(100);
        setStatusMessage(bundle.getString("completed"));
      }
    } catch (Exception ex) {
      setStatusMessage("error: " + ex.getLocalizedMessage());
      setStatus(-1);
      LogManager.getLogger(getClass()).error("Error creating Deployer package", ex);
    } finally {
      // disable non-system schema cache before releasing job lock
      if (dbmsHelper != null) {
        dbmsHelper.disableSchemaCache();
      }
      // disable dependency caching before releasing job lock
      if (dm != null) {
        dm.setIsDependencyCacheEnabled(false);
      }
      setCompleted();
      if (archive != null && !archive.isClosed()) {
        archive.close();
      }
    }
  }

  private void handleConfigFiles(PSDeploymentHandler dh, PSArchive archive)
      throws PSJobException, PSDeployException {
    var hasConfigDef = StringUtils.isNotBlank(m_descriptor.getConfigDefFile());
    var hasLocalConfig = StringUtils.isNotBlank(m_descriptor.getLocalConfigFile());
    File config;
    String configRef;

    if (hasConfigDef) {
      configRef = m_descriptor.getName() + "_" + "configDef";
      config = dh.getConfigTempFile(configRef);
      if (!config.exists() || !config.isFile()) {
        throw new PSJobException(
            IPSJobErrors.CONFIG_FILE_NOT_FOUND, m_descriptor.getConfigDefFile());
      }
      archive.storeFile(
          config, PSDescriptor.getConfigArchiveEntryPath(IPSConfigService.ConfigTypes.CONFIG_DEF));
    }
    if (hasLocalConfig) {
      configRef = m_descriptor.getName() + "_" + "localConfig";
      config = dh.getConfigTempFile(configRef);
      if (!config.exists() || !config.isFile()) {
        throw new PSJobException(
            IPSJobErrors.CONFIG_FILE_NOT_FOUND, m_descriptor.getLocalConfigFile());
      }
      archive.storeFile(
          config,
          PSDescriptor.getConfigArchiveEntryPath(IPSConfigService.ConfigTypes.LOCAL_CONFIG));
    }
  }

  // see base class
  @Override
  protected String getJobType() {
    //   return "Create Archive Job";
    return "Create Package Job";
  }

  /**
   * The export descriptor supplied to the <code>init()</code> method, never <code>null</code> or
   * modified after that.
   */
  private PSExportDescriptor m_descriptor;

  /**
   * Contains the version info of the server on which this job is running, initialized during the
   * <code>init()</code> method, never <code>null</code> or modified after that.
   */
  private PSFormatVersion m_serverVersion;
}
