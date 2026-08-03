// REFACTORED: CP-JAVA11
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
package com.percussion.rx.services.deployer;

import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.server.PSDeploymentHandler;
import com.percussion.deployer.server.uninstall.PSPackageUninstaller;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageType;
import com.percussion.utils.types.PSPair;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 * Handles converting installed packages to source. Sunny Sal says: "Convert to source, but never
 * convert your code to spaghetti!"
 */
public class PSConvertToSource {

  /** Default constructor for use by Spring. */
  public PSConvertToSource() {}

  /**
   * Converts an installed package to a source package.
   *
   * @param packageName The package name.
   * @return PSPair of Boolean (true if successful) and String (error message, never null or empty).
   * @throws PSNotFoundException if the package is not found.
   */
  public PSPair<Boolean, String> convert(String packageName) throws PSNotFoundException {
    var message = new PSPair<Boolean, String>();

    var pkgSvc = PSPkgInfoServiceLocator.getPkgInfoService();
    var pkgInfo = pkgSvc.findPkgInfo(packageName);

    // Verify Package exists and is valid
    if (pkgInfo == null
        || pkgInfo.getLastAction() == PSPkgInfo.PackageAction.UNINSTALL
        || pkgInfo.getType() == PSPkgInfo.PackageType.DESCRIPTOR) {
      message.setFirst(false);
      message.setSecond(
          "Package " + packageName + " is not a valid/successfully installed Package");
      return message;
    }

    // Check if package is locked
    if (!pkgInfo.isEditable()) {
      message.setFirst(false);
      message.setSecond(
          "Package " + packageName + " is locked and cannot be converted to source package");
      return message;
    }

    // Find Package file
    var packageFile = getPkgFile(packageName);

    // Fail if missing Package
    if (packageFile == null) {
      message.setFirst(false);
      message.setSecond("Cannot find package file for: " + packageName);
      return message;
    }

    var msg = moveDescriptor(packageName);
    if (msg != null) {
      return msg;
    }

    // Delete config files
    var uninstaller = new PSPackageUninstaller();
    uninstaller.deleteConfigFiles(pkgInfo);

    packageFile.delete();

    // Flip DB
    convertDB(packageName);

    // Success!
    message.setFirst(true);
    message.setSecond("Package " + packageName + " successfully converted to source");
    return message;
  }

  /**
   * Moves the descriptor from IMPORT_ARCHIVE_DIR to EXPORT_DESC_DIR.
   *
   * @param packageName the package name, assumed not blank.
   * @return an error message if there is any error, or null if successful.
   */
  private PSPair<Boolean, String> moveDescriptor(String packageName) {
    var message = new PSPair<Boolean, String>();

    // Get the converted descriptor
    var descFile = new File(PSDeploymentHandler.IMPORT_ARCHIVE_DIR, packageName + ".xml");
    if (!descFile.exists()) {
      message.setFirst(false);
      message.setSecond("Cannot find descriptor file: " + descFile.getPath());
      return message;
    }

    // Move the descriptor file to the target (source) location
    var tgtFile = new File(PSDeploymentHandler.EXPORT_DESC_DIR, packageName + ".xml");
    try {
      FileUtils.copyFile(descFile, tgtFile);
    } catch (IOException e) {
      message.setFirst(false);
      message.setSecond("Error saving Descriptor: " + e.getLocalizedMessage());
      return message;
    }
    // Remove the original descriptor file
    descFile.delete();

    return null;
  }

  /**
   * Loads the persisted directory path if it exists.
   *
   * @param pkgName name of package
   * @return the directory path or {@code null} if not found.
   */
  private File getPkgFile(String pkgName) {
    var rxDir = PSServer.getRxDir();
    var candidates =
        new String[] {
          rxDir
              + File.separator
              + "Packages"
              + File.separator
              + "Percussion"
              + File.separator
              + pkgName
              + IPSDeployConstants.ARCHIVE_EXTENSION,
          rxDir
              + File.separator
              + "Packages"
              + File.separator
              + pkgName
              + IPSDeployConstants.ARCHIVE_EXTENSION,
          rxDir
              + File.separator
              + "rx_resources"
              + File.separator
              + "widgets_generated"
              + File.separator
              + pkgName
              + IPSDeployConstants.ARCHIVE_EXTENSION
        };
    for (var path : candidates) {
      var file = new File(path);
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }

  /**
   * Flip the DB pkgInfo type from Package to Descriptor.
   *
   * @param pkgName package name
   * @throws PSNotFoundException if the package is not found.
   */
  protected void convertDB(String pkgName) throws PSNotFoundException {
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var pkgInfo = pkgService.findPkgInfo(pkgName);
    var pkgInfoMod = pkgService.loadPkgInfoModifiable(pkgInfo.getGuid());
    pkgInfoMod.setType(PackageType.DESCRIPTOR);
    pkgService.savePkgInfo(pkgInfoMod);
  }
}
