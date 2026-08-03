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
package com.percussion.deployer.server.uninstall;

import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.rx.config.PSConfigServiceLocator;
import com.percussion.rx.design.IPSDesignModel;
import com.percussion.rx.design.IPSDesignModelFactory;
import com.percussion.rx.design.PSDesignModelFactoryLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageAction;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageActionStatus;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.util.*;

/** Uninstalls elements of the supplied package and updates the status of package info. */
public class PSPackageUninstaller {

  /** Default constructor. */
  public PSPackageUninstaller() {}

  /**
   * Uninstalls all the elements of a package and updates the package info service with the
   * uninstalled status. SYSTEM packages will not be uninstalled.
   *
   * @param packageNames must not be <code>null</code>.
   * @return list of uninstall results, returns a success message if the uninstall succeeds,
   *     otherwise one message for the failure of each package element.
   */
  public List<IPSUninstallResult> uninstallPackages(List<String> packageNames)
      throws PSNotFoundException {
    return uninstallPackages(packageNames, false);
  }

  /**
   * Uninstalls all the elements of a package and updates the package info service with the
   * uninstalled status. SYSTEM packages will not be uninstalled.
   *
   * @param packageNames must not be <code>null</code>.
   * @param isRevertEntry <code>true</code> if the package marked for REVERT in InstallPackages.xml
   * @return list of uninstall results, returns a success message if the uninstall succeeds,
   *     otherwise one message for the failure of each package element.
   */
  public List<IPSUninstallResult> uninstallPackages(
      List<String> packageNames, boolean isRevertEntry) throws PSNotFoundException {
    if (packageNames == null) {
      throw new IllegalArgumentException("packageNames must not be null");
    }

    var pkgPair = loadPackages(packageNames);
    var messages = new ArrayList<>(pkgPair.getSecond());
    var pkgInfos = pkgPair.getFirst();
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();

    for (var pkgInfo : pkgInfos) {
      var msgs = uninstallPackage(pkgInfo);

      if (msgs.isEmpty()) {
        var result =
            new PSUninstallResult(
                pkgInfo.getPackageDescriptorName(),
                IPSUninstallResult.PSUninstallResultType.SUCCESS);
        result.setMessage("Uninstalled successfully.");
        messages.add(result);
      } else {
        messages.addAll(msgs);
      }

      var pkgInfoModifiable = pkgService.loadPkgInfoModifiable(pkgInfo.getGuid());

      for (var msg : msgs) {
        if (msg.getPackageName().equals(pkgInfoModifiable.getPackageDescriptorName())
            && !isRevertEntry) {
          pkgInfoModifiable.setLastAction(PackageAction.INSTALL_CREATE);
          pkgInfoModifiable.setLastActionDate(new Date());
          pkgInfoModifiable.setLastActionStatus(PackageActionStatus.SUCCESS);
          pkgService.savePkgInfo(pkgInfoModifiable);
          return messages;
        }
      }

      pkgInfoModifiable.setLastAction(PackageAction.UNINSTALL);
      pkgInfoModifiable.setLastActionDate(new Date());
      pkgInfoModifiable.setLastActionStatus(
          msgs.isEmpty() ? PackageActionStatus.SUCCESS : PackageActionStatus.FAIL);
      pkgService.savePkgInfo(pkgInfoModifiable);

      var deps = pkgService.loadPkgDependencies(pkgInfoModifiable.getGuid(), true);
      deps.forEach(dep -> pkgService.deletePkgDependency(dep.getId()));
    }

    return messages;
  }

  /**
   * Loads the packages and makes {@link IPSUninstallResult} objects for the packages that do not
   * have the {@link PSPkgInfo} objects as well as for SYSTEM packages. Returns the result as {@link
   * PSPair} of package info objects and result objects.
   *
   * @param packageNames list of package names for which the package info objects needs to be
   *     loaded, assumed not <code>null</code>.
   * @return The pair of list of {@link PSPkgInfo}and list of {@link IPSUninstallResult} object,
   *     either the first or second list may be empty but never <code>null</code>.
   */
  private PSPair<List<PSPkgInfo>, List<IPSUninstallResult>> loadPackages(
      List<String> packageNames) {
    var messages = new ArrayList<IPSUninstallResult>();
    var pkgInfos = new ArrayList<PSPkgInfo>();
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();

    for (var pkgName : packageNames) {
      var pkgInfo = pkgService.findPkgInfo(pkgName);
      if (pkgInfo == null) {
        var msg = new PSUninstallResult(pkgName, IPSUninstallResult.PSUninstallResultType.INFO);
        msg.setMessage(
            "Skipped uninstalling the supplied package as no package exists with the name: "
                + pkgName);
        messages.add(msg);
      } else if (pkgInfo.isSystem()) {
        var msg = new PSUninstallResult(pkgName, IPSUninstallResult.PSUninstallResultType.WARN);
        msg.setMessage(pkgName + " is a SYSTEM package. SYSTEM packages cannot be uninstalled.");
        messages.add(msg);
      } else {
        pkgInfos.add(pkgInfo);
      }
    }

    return new PSPair<>(pkgInfos, messages);
  }

  /**
   * Helper method to uninstall one package at a time.
   *
   * @param pkgInfo assumed not <code>null</code>.
   * @return List of IPSUninstallResult objects may be empty, never <code>null</code>. The message
   *     objects are filled in properly by appropriate action.
   */
  private List<IPSUninstallResult> uninstallPackage(PSPkgInfo pkgInfo) throws PSNotFoundException {
    var cfgService = PSConfigServiceLocator.getConfigService();
    cfgService.deApplyConfiguration(pkgInfo.getPackageDescriptorName());

    var messages = new ArrayList<>(deletePackageElements(pkgInfo));
    var wasContentTypeDeleted =
        messages.stream()
            .anyMatch(
                res ->
                    res.getResultType() == IPSUninstallResult.PSUninstallResultType.WARN
                        && res.getMessage().contains("Skipped deletion of package"));

    if (!wasContentTypeDeleted) {
      messages.addAll(deleteConfigFiles(pkgInfo));
    }

    return messages;
  }

  /**
   * Deletes the config files for that are associated with the supplied package and if there are any
   * errors deleting the files, returns them as list of {@link IPSUninstallResult} objects.
   *
   * @param pkgInfo the package info object whose config files need to be deleted, assumed not
   *     <code>null</code>.
   * @return list of {@link IPSUninstallResult}s containing error for each config file that can't be
   *     deleted.
   */
  public List<IPSUninstallResult> deleteConfigFiles(PSPkgInfo pkgInfo) {
    if (pkgInfo == null) {
      throw new IllegalArgumentException("pkgInfo must not be null");
    }

    var messages = new ArrayList<IPSUninstallResult>();
    var cfgService = PSConfigServiceLocator.getConfigService();
    var configName = pkgInfo.getPackageDescriptorName();
    var cfgErrors = cfgService.uninstallConfiguration(configName);

    cfgErrors.forEach(
        (file, exception) -> {
          var res =
              new PSUninstallResult(configName, IPSUninstallResult.PSUninstallResultType.WARN);
          res.setMessage("Failed to uninstall configuration file " + file.getName());
          res.setException(exception);
          messages.add(res);
        });

    return messages;
  }

  /**
   * Deletes the elements of the package, if there are any errors wraps them inside the {@link
   * IPSUninstallResult} objects as warnings and returns the list.
   *
   * @param pkgInfo assumed not <code>null</code>.
   * @return List of IPSUninstallResult objects may be empty, never <code>null</code>.
   */
  private List<IPSUninstallResult> deletePackageElements(PSPkgInfo pkgInfo)
      throws PSNotFoundException {
    var messages = new ArrayList<IPSUninstallResult>();
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var pkgElems = pkgService.findPkgElementGuids(pkgInfo.getGuid());

    for (var guid : pkgElems) {
      var pkgElem = pkgService.loadPkgElement(guid);
      var objGuid = pkgElem.getObjectGuid();

      if (!canIgnoreForUninstall(objGuid)) {
        var res = deleteElement(pkgInfo, objGuid);
        if (res != null) {
          messages.add(res);
        } else {
          pkgService.deletePkgElement(guid);
        }
      }
    }

    return messages;
  }

  /**
   * Helper method to delete the objects, if a design model exists for the supplied object guid
   * type, then uses the design model to delete the object. Deletes the file if the supplied object
   * is file type.
   *
   * @param pkgInfo assumed not <code>null</code>.
   * @param objGuid assumed not <code>null</code>.
   * @return The uninstall result, may be <code>null</code>.
   */
  private IPSUninstallResult deleteElement(PSPkgInfo pkgInfo, IPSGuid objGuid) {
    // obtain design model based on the object's type
    IPSDesignModel model = null;
    if (objGuid != null) {
      PSTypeEnum type = PSTypeEnum.valueOf(objGuid.getType());
      try {
        IPSDesignModelFactory factory = PSDesignModelFactoryLocator.getDesignModelFactory();
        model = factory.getDesignModel(type);
      } catch (PSMissingBeanConfigurationException e) {
        // design model may not exist for this type
      }
    }
    if (model != null) {
      try {
        model.delete(objGuid);
      } catch (Exception e) {
        var res =
            new PSUninstallResult(
                pkgInfo.getPackageDescriptorName(), IPSUninstallResult.PSUninstallResultType.ERROR);
        res.setPackageGuid(pkgInfo.getGuid());
        res.setMessage(e.getLocalizedMessage());
        res.setObjectGuid(objGuid);
        res.setException(e);
        return res;
      }
    }
    return null;
  }

  /**
   * Checks whether the supplied guid can be ignored for deletion.
   *
   * @param guid assumed not <code>null</code>.
   * @return <code>true</code> if it can be ignored for uninstall, otherwise <code>false</code>.
   */
  private boolean canIgnoreForUninstall(IPSGuid guid) {
    var ignoreTypes = List.of("CONFIGURATION", "IMAGE_FILE", "USER_DEPENDENCY");
    String typeName = PSTypeEnum.valueOf(guid.getType()).name();
    return ignoreTypes.contains(typeName);
  }
}
