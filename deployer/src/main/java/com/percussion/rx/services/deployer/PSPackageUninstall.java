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

import com.percussion.deployer.server.PSDeploymentHandler;
import com.percussion.deployer.server.uninstall.IPSUninstallResult;
import com.percussion.deployer.server.uninstall.IPSUninstallResult.PSUninstallResultType;
import com.percussion.rx.design.IPSDesignModel;
import com.percussion.rx.design.PSDesignModelFactoryLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.utils.PSIdNameHelper;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.utils.guid.IPSGuid;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles uninstallation of packages. Sunny Sal says: "Uninstalling packages, but never
 * uninstalling my sense of humor!"
 *
 * @author bjoginipally
 */
public class PSPackageUninstall implements IPSPackageUninstaller {

  /** Default constructor. */
  public PSPackageUninstall() {}

  /**
   * Uninstalls the packages. Creates uninstall messages with the returned results.
   *
   * @param packageNames The {@link PSPackageService#NAME_SEPARATOR} separated list of package
   *     names.
   * @return list of uninstall messages, never {@code null}, may be empty.
   */
  @Override
  public List<PSUninstallMessage> uninstallPackages(String packageNames)
      throws PSNotFoundException {
    return uninstallPackages(packageNames, false);
  }

  @Override
  public List<PSUninstallMessage> uninstallPackages(String packageName, boolean isRevertEntry)
      throws PSNotFoundException {
    var messages = new ArrayList<PSUninstallMessage>();
    var pkgNameList =
        StringUtils.isBlank(packageName)
            ? new ArrayList<String>()
            : List.of(packageName.split(PSPackageService.NAME_SEPARATOR)).stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

    if (pkgNameList.isEmpty()) {
      var msg = new PSUninstallMessage();
      msg.setPackageName("none");
      msg.setType(PSPackageService.WARNING);
      msg.setBody("No packages are supplied for uninstall.");
      messages.add(msg);
      return messages;
    }

    var dh = PSDeploymentHandler.getInstance();
    if (dh == null) {
      var msg = new PSUninstallMessage();
      msg.setPackageName(pkgNameList.isEmpty() ? "none" : pkgNameList.get(0));
      msg.setType(PSPackageService.ERROR);
      msg.setBody(
          "Deployment handler is not initialized. Please ensure the server has fully started.");
      messages.add(msg);
      return messages;
    }
    var results = dh.uninstallPackages(pkgNameList, isRevertEntry);
    for (var result : results) {
      var msg = new PSUninstallMessage();
      msg.setPackageName(result.getPackageName());
      msg.setType(getConvertedType(result.getResultType()));
      msg.setBody(result.getMessage());
      messages.add(msg);
      if (result.getResultType() == IPSUninstallResult.PSUninstallResultType.ERROR)
        ms_logger.error(result.getMessage(), result.getException());
      else if (result.getResultType() == IPSUninstallResult.PSUninstallResultType.WARN)
        ms_logger.warn(result.getMessage(), result.getException());
    }
    return messages;
  }

  /**
   * Converts PSUninstallResultType value to the UI consumable String.
   *
   * @param resultType assumed not {@code null}.
   * @return One of the constants defined in Package Service, never {@code null}.
   */
  private String getConvertedType(PSUninstallResultType resultType) {
    if (resultType == IPSUninstallResult.PSUninstallResultType.SUCCESS)
      return PSPackageService.SUCCESS;
    if (resultType == IPSUninstallResult.PSUninstallResultType.ERROR) return PSPackageService.ERROR;
    if (resultType == IPSUninstallResult.PSUninstallResultType.WARN)
      return PSPackageService.WARNING;
    if (resultType == IPSUninstallResult.PSUninstallResultType.INFO) return PSPackageService.INFO;
    return PSPackageService.NONE;
  }

  /**
   * Checks for dependent packages and elements with dependencies. Creates one message for each kind
   * of dependencies found.
   *
   * @param packageName the name of the package for which dependencies need to be checked.
   * @return list of {@link PSUninstallMessage} messages, will be empty if there are no dependencies
   *     found.
   */
  public List<PSUninstallMessage> checkPackageDepedencies(String packageName)
      throws PSNotFoundException {
    if (StringUtils.isBlank(packageName))
      throw new IllegalArgumentException("packageName must not be blank");
    var messages = new ArrayList<PSUninstallMessage>();
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var pinfo = pkgService.findPkgInfo(packageName);
    if (pinfo == null) {
      var msg = new PSUninstallMessage();
      msg.setType(PSPackageService.WARNING);
      msg.setPackageName(packageName);
      msg.setBody("No Package exists with the supplied name: " + packageName);
      messages.add(msg);
      return messages;
    }
    var depMsg = checkPkgDependencies(pinfo);
    if (depMsg != null) messages.add(depMsg);
    depMsg = checkContentDependencies(pinfo);
    if (depMsg != null) messages.add(depMsg);
    return messages;
  }

  /**
   * Finds all dependent packages and creates a message.
   *
   * @param pinfo The package info object for which dependencies need to be checked, assumed not
   *     {@code null}.
   * @return A message for all dependent packages, may be {@code null} if no dependencies found.
   */
  private PSUninstallMessage checkPkgDependencies(PSPkgInfo pinfo) throws PSNotFoundException {
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var guids = pkgService.findOwnerPkgGuids(pinfo.getGuid());
    if (!guids.isEmpty()) {
      var depPkgs =
          guids.stream()
              .map(
                  guid -> {
                    try {
                      return pkgService.loadPkgInfo(guid).getPackageDescriptorName();
                    } catch (PSNotFoundException e) {
                      ms_logger.warn("Could not load dependent package info for guid: {}", guid, e);
                      return "unknown";
                    }
                  })
              .collect(Collectors.joining("<br/>", "<br/>", ""));
      var msgT =
          "Package ({0}) is a dependency for other packages installed on the system. If you remove"
              + " package ({1}), these packages may not work correctly. {2}";
      var args =
          new Object[] {
            pinfo.getPackageDescriptorName(), pinfo.getPackageDescriptorName(), depPkgs
          };
      var msg = new PSUninstallMessage();
      msg.setPackageName(pinfo.getPackageDescriptorName());
      msg.setType(PSPackageService.WARNING);
      msg.setBody(MessageFormat.format(msgT, args));
      return msg;
    }
    return null;
  }

  /**
   * Checks whether the elements of the supplied package have any dependencies.
   *
   * @param pinfo The package info object for which element dependencies need to be checked, assumed
   *     not {@code null}.
   * @return A message for all objects that have dependencies, may be {@code null} if no
   *     dependencies found.
   */
  private PSUninstallMessage checkContentDependencies(PSPkgInfo pinfo) throws PSNotFoundException {
    var objGuids = getPackageObjectGuids(pinfo);
    var sysSrvc = PSSystemServiceLocator.getSystemService();
    var depObjs = sysSrvc.findDependencies(objGuids);
    var factory = PSDesignModelFactoryLocator.getDesignModelFactory();
    var objNames = new StringBuilder();
    for (int i = 0; i < depObjs.size(); i++) {
      if (!depObjs.get(i).getDependents().isEmpty()) {
        var objGuid = objGuids.get(i);
        if (PSIdNameHelper.isSupported(PSTypeEnum.valueOf(objGuid.getType()))) {
          objNames.append("<br />").append(PSIdNameHelper.getName(objGuid));
        } else {
          IPSDesignModel model = factory.getDesignModel(PSTypeEnum.valueOf(objGuid.getType()));
          objNames.append("<br />").append(model.guidToName(objGuid));
        }
      }
    }
    if (StringUtils.isNotBlank(objNames.toString())) {
      var msgT =
          "The package {0} includes design objects that are currently being used. These design"
              + " objects will not be removed when the package is uninstalled. {1}";
      var args = new Object[] {pinfo.getPackageDescriptorName(), objNames.toString()};
      var msg = new PSUninstallMessage();
      msg.setPackageName(pinfo.getPackageDescriptorName());
      msg.setType(PSPackageService.WARNING);
      msg.setBody(MessageFormat.format(msgT, args));
      return msg;
    }
    return null;
  }

  /**
   * Returns the object guids of the supplied package.
   *
   * @param pkgInfo assumed not {@code null}.
   * @return List of IPSGuids of objects of the supplied package, never {@code null}, may be empty.
   */
  private List<IPSGuid> getPackageObjectGuids(PSPkgInfo pkgInfo) throws PSNotFoundException {
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var pkgElems = pkgService.findPkgElementGuids(pkgInfo.getGuid());
    var objGuids = new ArrayList<IPSGuid>();
    for (var guid : pkgElems) {
      var pkgElem = pkgService.loadPkgElement(guid);
      objGuids.add(pkgElem.getObjectGuid());
    }
    return objGuids;
  }

  /** The logger for this class. */
  private static final Logger ms_logger = LogManager.getLogger("PSPackageUninstall");
}
