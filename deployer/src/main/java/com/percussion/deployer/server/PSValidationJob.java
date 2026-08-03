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

import com.percussion.deployer.client.PSDeploymentManager;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.objectstore.PSImportDescriptor;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.server.job.IPSJobErrors;
import com.percussion.server.job.PSJobException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;

/**
 * Job to validate all packages in an import descriptor. Results are saved on the server and may be
 * retrieved using the archive ref.
 */
public class PSValidationJob extends PSDeployJob {

  /** Default constructor. */
  public PSValidationJob() {}

  /**
   * Restores the import descriptor from the supplied document, and validates that the user is
   * authorized to perform this job. Saves the security token from the request to use for subsequent
   * operations during the run method. <br>
   * See Base class for more info.
   *
   * @param id the job id.
   * @param descriptor the document holding the import descriptor, may not be <code>null</code>.
   * @param req the request that initialized the job, may not be <code>null</code>.
   * @param initParams the initialization parameters, may be <code>null</code>.
   * @throws PSAuthenticationFailedException if the user cannot be authenticated.
   * @throws PSAuthorizationException if the user is not authorized to run this job.
   * @throws PSJobException if the descriptor cannot be parsed.
   */
  @Override
  public void init(int id, Document descriptor, PSRequest req, Properties initParams)
      throws PSAuthenticationFailedException, PSAuthorizationException, PSJobException {
    Objects.requireNonNull(descriptor, "descriptor may not be null");
    Objects.requireNonNull(req, "req may not be null");

    super.init(id, req, initParams);

    try {
      m_descriptor = new PSImportDescriptor(descriptor.getDocumentElement());
      var pkgList = new ArrayList<PSDeployableElement>();
      for (var importPkg : m_descriptor.getImportPackageList()) {
        pkgList.add(importPkg.getPackage());
      }
      initDepCount(pkgList.iterator(), false);
    } catch (PSUnknownNodeTypeException | PSDeployException e) {
      throw new PSJobException(IPSJobErrors.INVALID_JOB_DESCRIPTOR, e.getLocalizedMessage());
    }
  }

  /** Runs this validation job. Validates all packages and saves the results. */
  @Override
  public void doRun() {
    try {
      validate(m_descriptor, this, getSecurityToken());

      if (!isCancelled()) {
        // Write out the descriptor with the results using the archive ref
        var doc = PSXmlDocumentBuilder.createXmlDocument();
        PSXmlDocumentBuilder.replaceRoot(doc, m_descriptor.toXml(doc));

        var resultsFile =
            new File(
                PSDeploymentHandler.getValidationDir(),
                m_descriptor.getArchiveInfo().getArchiveRef() + ".xml");
        resultsFile.getParentFile().mkdirs();
        resultsFile.deleteOnExit();

        try (var out = new FileOutputStream(resultsFile)) {
          PSXmlDocumentBuilder.write(doc, out);
        }

        setStatus(100);
        setStatusMessage(PSDeploymentManager.getBundle().getString("completed"));
      }
    } catch (Exception ex) {
      setStatusMessage("error: " + ex.toString());
      setStatus(-1);
      LogManager.getLogger(getClass()).error("Error validating Deployer package", ex);
    } finally {
      setCompleted();
    }
  }

  /**
   * Standalone validation method used by server-side services.
   *
   * @param descriptor The import descriptor to validate, not {@code null}
   * @param jobHandle Job handle to record status, not {@code null}
   * @param tok Security token representing current user session, not {@code null}.
   * @throws PSDeployException If there are any errors.
   * @throws PSNotFoundException if a referenced dependency cannot be found.
   */
  public void validate(PSImportDescriptor descriptor, IPSJobHandle jobHandle, PSSecurityToken tok)
      throws PSDeployException, PSNotFoundException {
    Validate.notNull(descriptor);
    Validate.notNull(jobHandle);
    Validate.notNull(tok);

    PSDbmsHelper dbmsHelper = null;
    PSDependencyManager dm = null;

    try {
      var bundle = PSDeploymentManager.getBundle();
      setStatusMessage(bundle.getString("init"));

      // Enable cache for non-system schema
      dbmsHelper = PSDbmsHelper.getInstance();
      dbmsHelper.enableSchemaCache();

      // Get the archive ref
      var info = descriptor.getArchiveInfo();

      // Walk the packages and validate
      var dh = PSDeploymentHandler.getInstance();
      dm = (PSDependencyManager) dh.getDependencyManager();

      // Enable dependency cache
      dm.setIsDependencyCacheEnabled(true);

      // Generate the id map
      var sourceDb = info.getRepositoryInfo();
      var importList = descriptor.getImportPackageList();

      var th = new PSTransformsHandler(tok, sourceDb.getDbmsIdentifier(), importList);

      setStatusMessage(bundle.getString("generatingIdMap"));

      // Get the transformed id map
      var idMap = th.getIdMap();

      // Save the transformed id map
      dh.getIdMapMgr().saveIdMap(idMap);

      var valCtx = new PSValidationCtx(jobHandle, descriptor, idMap);
      valCtx.setValidateAncestors(descriptor.isAncestorValidationEnabled());

      for (var pkg : descriptor.getImportPackageList()) {
        if (isCancelled()) break;
        var de = pkg.getPackage();
        valCtx.addPackage(pkg);
        var msg = MessageFormat.format(bundle.getString("processing"), de.getDisplayIdentifier());
        setStatusMessage(msg);
        var dv = new PSDependencyValidator(tok, de, valCtx, descriptor.getName());
        pkg.setValidationResults(dv.validate());
      }

      if (!isCancelled()) {
        // Write out the descriptor with the results using the archive ref
        var doc = PSXmlDocumentBuilder.createXmlDocument();
        PSXmlDocumentBuilder.replaceRoot(doc, descriptor.toXml(doc));

        var resultsFile =
            new File(PSDeploymentHandler.VALIDATION_RESULTS_DIR, info.getArchiveRef() + ".xml");
        resultsFile.getParentFile().mkdirs();
        resultsFile.deleteOnExit();

        try (var out = new FileOutputStream(resultsFile)) {
          PSXmlDocumentBuilder.write(doc, out);
        }

        setStatus(100);
        setStatusMessage(bundle.getString("completed"));
      }
    } catch (Exception ex) {
      setStatusMessage("error: " + ex.toString());
      setStatus(-1);
      LogManager.getLogger(getClass()).error("Error validating archive", ex);
    } finally {
      // Disable non-system schema cache before releasing job lock
      if (dbmsHelper != null) {
        dbmsHelper.disableSchemaCache();
      }
      // Disable dependency cache before releasing job lock
      if (dm != null) {
        dm.setIsDependencyCacheEnabled(false);
      }
      setCompleted();
    }
  }

  @Override
  protected String getJobType() {
    return "Validate Import Descriptor";
  }

  /**
   * The import descriptor supplied to the {@code init()} method, never {@code null} or modified
   * after that.
   */
  private PSImportDescriptor m_descriptor;
}
