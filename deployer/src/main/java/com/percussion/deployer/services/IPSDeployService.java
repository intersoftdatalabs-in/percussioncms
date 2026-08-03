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
package com.percussion.deployer.services;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.server.IPSServiceDependencyHandler;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.deployer.server.dependencies.PSDependencyHandler;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.sitemgr.IPSSite;
import java.util.HashMap;

/** Service interface for deployment operations. */
public interface IPSDeployService {

  /**
   * A specific method for installing site files. Deserializes the supplied dependency file from the
   * archive and saves it for the supplied site.
   *
   * @param tok The security token to use, may not be {@code null}.
   * @param archive The archive handler supplying the file, may not be {@code null}.
   * @param dep The dependency being installed, may not be {@code null}.
   * @param depFile The dependency file to install, may not be {@code null}.
   * @param ctx The import context, may not be {@code null}.
   * @param depHandler The dependency handler performing the install, may not be {@code null}.
   * @param s The site the file is associated with, may not be {@code null}.
   * @param ver The version number to associate with the file, may be {@code null}.
   * @throws PSDeployServiceException if there are any errors.
   */
  void deserializeAndSaveSite(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler,
      IPSSite s,
      Integer ver)
      throws PSDeployServiceException;

  /**
   * A specific method for installing Template files. Deserializes the supplied dependency file from
   * the archive and saves it for the supplied assembly template.
   *
   * @param tok The security token to use, may not be {@code null}.
   * @param archive The archive handler supplying the file, may not be {@code null}.
   * @param dep The dependency being installed, may not be {@code null}.
   * @param depFile The dependency file to install, may not be {@code null}.
   * @param ctx The import context, may not be {@code null}.
   * @param depHandler The dependency handler performing the install, may not be {@code null}.
   * @param t The assembly template the file is associated with, may not be {@code null}.
   * @param ver The version number to associate with the file, may be {@code null}.
   * @param bVer Map of base template id to version number used during installation, may not be
   *     {@code null}.
   * @throws PSDeployServiceException if there are any errors.
   */
  void deserializeAndSaveTemplate(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler,
      PSAssemblyTemplate t,
      Integer ver,
      HashMap<Long, Integer> bVer)
      throws PSDeployServiceException;

  /**
   * A custom method for installing filters. Deserializes the supplied dependency file from the
   * archive and saves the filter.
   *
   * @param tok The security token to use, may not be {@code null}.
   * @param archive The archive handler supplying the file, may not be {@code null}.
   * @param dep The dependency being installed, may not be {@code null}.
   * @param depFile The dependency file to install, may not be {@code null}.
   * @param ctx The import context, may not be {@code null}.
   * @param depHandler The dependency handler performing the install, may not be {@code null}.
   * @throws PSDeployServiceException if there are any errors.
   */
  void deserializeAndSaveFilter(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler)
      throws PSDeployServiceException;

  /**
   * A specific method for installing Variant files. Deserializes the supplied dependency file from
   * the archive and saves it for the supplied assembly template variant.
   *
   * @param tok The security token to use, may not be {@code null}.
   * @param archive The archive handler supplying the file, may not be {@code null}.
   * @param dep The dependency being installed, may not be {@code null}.
   * @param depFile The dependency file to install, may not be {@code null}.
   * @param ctx The import context, may not be {@code null}.
   * @param depHandler The dependency handler performing the install, may not be {@code null}.
   * @param t The assembly template the variant belongs to, may not be {@code null}.
   * @param ver The version number to associate with the file, may be {@code null}.
   * @throws PSDeployServiceException if there are any errors.
   */
  void deserializeAndSaveVariant(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSDependencyFile depFile,
      PSImportCtx ctx,
      PSDependencyHandler depHandler,
      PSAssemblyTemplate t,
      Integer ver)
      throws PSDeployServiceException;

  /**
   * Performs the task of installing dependency files. See {@link
   * PSDependencyHandler#installDependencyFiles(PSSecurityToken, PSArchiveHandler, PSDependency,
   * PSImportCtx)} for details.
   *
   * @param tok The security token to use, may not be {@code null}.
   * @param archive The archive handler supplying the files, may not be {@code null}.
   * @param dep The dependency being installed, may not be {@code null}.
   * @param ctx The import context, may not be {@code null}.
   * @param svcDepHandler The service dependency handler which will be invoked to install the files.
   *     May not be {@code null}.
   * @throws PSDeployServiceException if there are any errors.
   */
  void installDependencyFiles(
      PSSecurityToken tok,
      PSArchiveHandler archive,
      PSDependency dep,
      PSImportCtx ctx,
      IPSServiceDependencyHandler svcDepHandler)
      throws PSDeployServiceException;
}
