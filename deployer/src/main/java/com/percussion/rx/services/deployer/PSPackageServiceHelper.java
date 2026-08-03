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

import com.percussion.rx.config.PSConfigServiceLocator;
import com.percussion.rx.services.deployer.PSPkgUiResponse.PSPkgUiResponseType;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.IPSPkgInfoService;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageAction;
import com.percussion.services.pkginfo.utils.PSPkgHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Static helper class for package service.
 *
 * @author bjoginipally
 */
public class PSPackageServiceHelper {

  /** Default constructor for use via static methods. */
  public PSPackageServiceHelper() {}

  /**
   * Checks for the package validation and returns the results as {@link PSPkgUiResponse}.
   *
   * @param pkgName The name of the package that needs to be validated.
   * @return Either failure or success {@link PSPkgUiResponse} object.
   */
  public static PSPkgUiResponse getValidationResults(String pkgName) throws PSNotFoundException {
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var pkgInfo = pkgService.findPkgInfo(pkgName);
    if (pkgInfo == null) {
      return new PSPkgUiResponse(
          PSPkgUiResponseType.FAILURE,
          "Failed to find the package with the supplied name " + pkgName);
    }
    var missingPkgs = getMissingPackages(pkgInfo);
    var message = new StringBuilder();
    if (!missingPkgs.isEmpty()) {
      message
          .append("<b>Missing Packages</b><br/> The following ")
          .append("dependent packages are either not installed successfully ")
          .append("or uninstalled.<br/>");
      missingPkgs.forEach(pkg -> message.append("<br/>").append(pkg));
    }
    var results = PSPkgHelper.validatePackage(pkgInfo.getGuid());
    if (!results.isEmpty()) {
      message.setLength(0);
      message
          .append("<b>Modified Design Objects</b><br/> The following objects ")
          .append("have been modified outside of allowed configuration.<br/>");
      results.forEach(obj -> message.append("<br/>").append(obj));
    }
    var cfgSrvc = PSConfigServiceLocator.getConfigService();
    var cfgValErrors = cfgSrvc.validateConfiguartion(pkgName);
    if (!cfgValErrors.isEmpty()) {
      message.setLength(0);
      message.append("<b>Configuration Verification Results</b><br/>");
      cfgValErrors.forEach(obj -> message.append("<br/>").append(obj.getValidationMsg()));
    }
    var resp =
        new PSPkgUiResponse(
            PSPkgUiResponseType.SUCCESS, "No conflicts found during the package verification.");
    if (StringUtils.isNotBlank(message.toString())) {
      resp = new PSPkgUiResponse(PSPkgUiResponseType.FAILURE, message.toString());
    }
    return resp;
  }

  /**
   * Creates a list missing dependent packages for the supplied package and returns them.
   *
   * @param pkgInfo The object {@link PSPkgInfo} for which the dependencies need to be calculated.
   * @return List of the names of dependent package never null, may be empty.
   */
  private static List<String> getMissingPackages(PSPkgInfo pkgInfo) throws PSNotFoundException {
    var missingPkgs = new ArrayList<String>();
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var depGuids = pkgService.findDependentPkgGuids(pkgInfo.getGuid());
    for (var guid : depGuids) {
      var info = pkgService.loadPkgInfo(guid);
      if (!info.isSuccessfullyInstalled() || info.getLastAction().equals(PackageAction.UNINSTALL)) {
        missingPkgs.add(info.getPackageDescriptorName());
      }
    }
    return missingPkgs;
  }

  /**
   * Applies the package visibility on the supplied packages. Returns either a successful {@link
   * PSPkgUiResponse} object or failure object based on the errors occurred while applying the
   * visibility.
   *
   * @param packageNames must not be null.
   * @return A successful {@link PSPkgUiResponse} object or failure object, never null.
   */
  public static PSPkgUiResponse applyPackageVisibility(String packageNames) {
    var response =
        new PSPkgUiResponse(
            PSPkgUiResponseType.SUCCESS, "Successfully reapplied the visibility settings.");
    var pkgNames = packageNames.split(PSPackageService.NAME_SEPARATOR);
    var pkgsPair = getValidPackages(pkgNames);
    var invalidPkgs = pkgsPair.getSecond();
    for (var pkgname : invalidPkgs) {
      ms_logger.warn(
          "No package info object exists with the name "
              + pkgname
              + ". Skipping reapplying of the package visibility");
    }
    var pkgInfos = pkgsPair.getFirst();
    var errorPkgs = new ArrayList<String>();
    var pkgVis = new PSPackageVisibility();
    for (var pinfo : pkgInfos) {
      if (!pinfo.isSuccessfullyInstalled() || pinfo.getLastAction().equals(PackageAction.UNINSTALL))
        continue;
      var errMsg = pkgVis.setPkgCommunities(pinfo);
      if (errMsg != null) errorPkgs.add(pinfo.getPackageDescriptorName());
    }
    if (!errorPkgs.isEmpty()) {
      var msg =
          new StringBuilder(
              "Failed to reapply the visibility settings for the following packages.<br/>");
      errorPkgs.forEach(string -> msg.append("<br/>").append(string));
      response.setMessage(msg.toString());
      response.setType(PSPkgUiResponseType.FAILURE);
    }
    return response;
  }

  /**
   * Applies the package configuration on the supplied packages. Returns either a successful {@link
   * PSPkgUiResponse} object or failure object based on the errors occurred while applying the
   * configuration.
   *
   * @param packageNames must not be null.
   * @return A successful {@link PSPkgUiResponse} object or failure object, never null.
   */
  public static PSPkgUiResponse applyConfiguartion(String packageNames) {
    if (StringUtils.isBlank(packageNames))
      throw new IllegalArgumentException("packageNames must not be blank");
    var response =
        new PSPkgUiResponse(
            PSPkgUiResponseType.SUCCESS, "Successfully reapplied the configuration settings.");
    var pkgNames = packageNames.split(PSPackageService.NAME_SEPARATOR);
    var pkgsPair = getValidPackages(pkgNames);
    var invalidPkgs = pkgsPair.getSecond();
    for (var pkgname : invalidPkgs) {
      ms_logger.warn(
          "No package info object exists with the name "
              + pkgname
              + ". Skipping reapplying of the package visibility");
    }
    var pkgInfos = pkgsPair.getFirst();
    var validPkgs =
        pkgInfos.stream().map(PSPkgInfo::getPackageDescriptorName).collect(Collectors.toList());
    var cfgSrvc = PSConfigServiceLocator.getConfigService();
    var errors = cfgSrvc.applyConfiguration(validPkgs.toArray(new String[0]), false);
    if (!errors.isEmpty()) {
      var msg =
          new StringBuilder(
              "Failed to reapply the configuration settings for the following packages.<br/>");
      errors.forEach(pair -> msg.append("<br/>").append(pair.getFirst()));
      response.setMessage(msg.toString());
      response.setType(PSPkgUiResponseType.FAILURE);
    }
    return response;
  }

  /**
   * Finds the {@link PSPkgInfo} objects for the supplied names and returns a {@link PSPair}, with
   * list of PSPkgInfo objects as the first element and list of package names for which the {@link
   * PSPkgInfo} is not found as second element.
   *
   * @param pkgNames The String array of names of packages for which the {@link PSPkgInfo} objects
   *     needs to be found, assumed not null.
   * @return PSPair of list of packages found and list of package names for which the packages are
   *     not found. The pair and lists are never null may be empty.
   */
  private static PSPair<List<PSPkgInfo>, List<String>> getValidPackages(String[] pkgNames) {
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    var pkgInfos = new ArrayList<PSPkgInfo>();
    var invalidPkgs = new ArrayList<String>();
    for (var pkgname : pkgNames) {
      if (StringUtils.isNotBlank(pkgname)) {
        var info = pkgService.findPkgInfo(pkgname);
        if (info == null) {
          invalidPkgs.add(pkgname);
          continue;
        }
        pkgInfos.add(info);
      }
    }
    return new PSPair<>(pkgInfos, invalidPkgs);
  }

  /**
   * Creates the community package objects and returns them. Loops through all the successfully
   * installed packages and gets the communities for them. Creates a reverse map of community and
   * packages from them and then creates the {@link PSCommunityPackage} objects and adds them to the
   * return list. Gets all the communities in the system and for the communities that are not
   * covered by the packages creates a PSCommunityPackage object with empty string for packages.
   *
   * @return PSCommunityPackages object never null.
   */
  public static PSCommunityPackages getCommunityPackages() {
    var commPkgs = new PSCommunityPackages();
    var srv = PSConfigServiceLocator.getConfigService();
    var pkgInfomap = getPkgGuidNameMap();
    var pkgCommsMap = new HashMap<String, Collection<String>>();
    for (var pkg : pkgInfomap.values()) {
      var comms = srv.loadCommunityVisibility(pkg);
      pkgCommsMap.put(pkg, comms);
    }
    var commPkgsMap = new HashMap<String, List<String>>();
    for (var pkgName : pkgCommsMap.keySet()) {
      var comms = pkgCommsMap.get(pkgName);
      for (var comm : comms) {
        if (StringUtils.isBlank(comm)) continue;
        commPkgsMap.computeIfAbsent(comm, k -> new ArrayList<>()).add(pkgName);
      }
    }
    var comms = commPkgsMap.keySet();
    for (var comm : comms) {
      commPkgs.add(new PSCommunityPackage(comm, getStringFromList(commPkgsMap.get(comm))));
    }
    var pkgVis = new PSPackageVisibility();
    var allComms = pkgVis.getAllCommunities();
    allComms.removeAll(comms);
    for (var comm : allComms) {
      commPkgs.add(new PSCommunityPackage(comm, ""));
    }
    return commPkgs;
  }

  /**
   * Returns a map of package guid and name map of successfully installed packages.
   *
   * @return never null, may be empty.
   */
  public static Map<IPSGuid, String> getPkgGuidNameMap() {
    var pInfos = getPkgService().findAllPkgInfos();
    var pkgInfomap = new HashMap<IPSGuid, String>();
    for (var pinfo : pInfos) {
      if (pinfo.isSuccessfullyInstalled()
          && !pinfo.getLastAction().equals(PackageAction.UNINSTALL)) {
        pkgInfomap.put(pinfo.getGuid(), pinfo.getPackageDescriptorName());
      }
    }
    return pkgInfomap;
  }

  /**
   * Helper method to return NAME_SEPARATOR string of items in supplied list of Strings
   *
   * @param strList if null or empty returns empty String.
   * @return concatenated String, never null may be blank.
   */
  public static String getStringFromList(Collection<String> strList) {
    if (strList == null || strList.isEmpty()) return "";
    return strList.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.joining(PSPackageService.NAME_SEPARATOR));
  }

  /**
   * Applies the supplied communities on to the package and package elements.
   *
   * @param pkgName the name of an existing package. It may not be blank.
   * @param commList a list of community names. If this is null, then apply all existing communities
   *     to the given package.
   * @param clearOtherEntries if true clears other community entries from the objects and applies
   *     the supplied communities. Otherwise leaves the current communities as is.
   */
  public static void updatePkgCommunities(
      String pkgName, Collection<String> commList, boolean clearOtherEntries) {
    if (StringUtils.isBlank(pkgName))
      throw new IllegalArgumentException("pkgName may not be blank.");
    var pkgVis = new PSPackageVisibility();
    if (commList == null) commList = pkgVis.getAllCommunities();
    var pInfo = getPkgService().findPkgInfo(pkgName);
    if (pInfo == null) {
      throw new RuntimeException("Invalid package: \"" + pkgName + "\".");
    }
    var pkgGuid = pInfo.getGuid();
    pkgVis.setPackageCommunities(pkgGuid, commList, clearOtherEntries);
    // save the community visibility
    var srv = PSConfigServiceLocator.getConfigService();
    srv.saveCommunityVisibility(commList, pkgName, clearOtherEntries);
  }

  /**
   * Returns the package info service.
   *
   * @return the service, never null.
   */
  private static IPSPkgInfoService getPkgService() {
    return PSPkgInfoServiceLocator.getPkgInfoService();
  }

  /** The logger for this class. */
  private static final Logger ms_logger = LogManager.getLogger("PSPackageServiceHelper");
}
