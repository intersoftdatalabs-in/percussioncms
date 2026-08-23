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
package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSDeployComponentUtils;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSServer;
import com.percussion.services.notification.PSNotificationHelper;
import com.percussion.util.IOTools;
import com.percussion.utils.collections.PSIteratorUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;

/** Base class for handlers that package and install files directly to and from the file system. */
public abstract class PSFileDependencyHandler extends PSDependencyHandler {
  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSFileDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  /**
   * This class returns an empty list. Derrived class should override this method if they support
   * child types. See {@link PSDependencyHandler#getChildDependencies(PSSecurityToken, PSDependency)
   * Base Class} for more info.
   */
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(getType()))
      throw new IllegalArgumentException("dep wrong type");

    return PSIteratorUtils.emptyIterator();
  }

  // see base class
  @Override
  public Iterator<PSDependencyFile> getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null || dep == null || !dep.getObjectType().equals(getType())) {
      throw new IllegalArgumentException("Invalid arguments provided.");
    }

    var depFile =
        new File(
            PSServer.getRxDir().getAbsolutePath(),
            PSDeployComponentUtils.getNormalizedPath(dep.getDependencyId()));
    if (!depFile.exists()) {
      throw new PSDeployException(
          DeploymentErrorCodes.DEP_OBJECT_NOT_FOUND,
          new Object[] {dep.getObjectTypeName(), dep.getDependencyId(), dep.getDisplayName()});
    }

    return List.of(new PSDependencyFile(PSDependencyFile.TYPE_SUPPORT_FILE, depFile)).iterator();
  }

  // see base class
  @Override
  public void installDependencyFiles(
      PSSecurityToken tok, PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
      throws PSDeployException {
    if (tok == null
        || archive == null
        || dep == null
        || ctx == null
        || !dep.getObjectType().equals(getType())) {
      throw new IllegalArgumentException("Invalid arguments provided.");
    }

    Iterator<PSDependencyFile> files = archive.getFiles(dep);
    if (!files.hasNext()) {
      throw new PSDeployException(
          DeploymentErrorCodes.MISSING_DEPENDENCY_FILE,
          new Object[] {
            PSDependencyFile.TYPE_ENUM[PSDependencyFile.TYPE_SUPPORT_FILE],
            dep.getObjectType(),
            dep.getDependencyId(),
            dep.getDisplayName()
          });
    }

    PSDependencyFile depFile = files.next();
    var tgtFile =
        new File(
            PSServer.getRxDir().getAbsolutePath(),
            PSDeployComponentUtils.getNormalizedPath(dep.getDependencyId()));
    var transAction =
        tgtFile.exists()
            ? PSTransactionSummary.ACTION_MODIFIED
            : PSTransactionSummary.ACTION_CREATED;

    var parentDir = tgtFile.getParentFile();
    if (parentDir != null) {
      parentDir.mkdirs();
    }

    tgtFile.setLastModified(System.currentTimeMillis());

    try (var out = new FileOutputStream(tgtFile);
        var in = archive.getFileData(depFile)) {
      IOTools.copyStream(in, out);
    } catch (IOException e) {
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }

    addTransactionLogEntry(
        dep, ctx, tgtFile.getPath(), PSTransactionSummary.TYPE_FILE, transAction);
    PSNotificationHelper.notifyFile(tgtFile);
  }

  // see base class
  @Override
  public Iterator<PSDependency> getDependencies(PSSecurityToken tok) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    return PSIteratorUtils.emptyIterator();
  }

  // see base class
  @Override
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    PSDependency dep = null;

    id = PSDeployComponentUtils.getNormalizedPath(id);
    File depFile = new File(id);
    if (depFile.exists()) dep = createDependency(m_def, id, depFile.getName());

    return dep;
  }

  /**
   * Provides the list of child dependency types this class can discover. Base class returns an
   * empty list. Derrived class should override this method if they support child types.
   *
   * @return An empty iterator, never <code>null</code>.
   */
  @Override
  public Iterator<String> getChildTypes() {
    return PSIteratorUtils.emptyIterator();
  }

  // see base class
  @Override
  public boolean doesDependencyExist(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    return (getDependency(tok, id) != null);
  }
}
