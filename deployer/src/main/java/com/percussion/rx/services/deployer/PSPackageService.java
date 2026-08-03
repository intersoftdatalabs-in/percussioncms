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

import com.percussion.rx.config.IPSConfigService;
import com.percussion.rx.config.PSConfigServiceLocator;
import com.percussion.rx.config.data.PSConfigStatus.ConfigStatus;
import com.percussion.rx.services.deployer.PSPkgUiResponse.PSPkgUiResponseType;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.IPSPkgInfoService;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageAction;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Business layer package service that calls to the lower level CRUD package service allowing
 * exposure to clients via REST.
 *
 * @author erikserating
 */
@Service(value = "packageService")
public class PSPackageService {

  /** Default constructor for use by Spring. */
  /**
   * REST endpoint.
   */
  public PSPackageService() {}

  /** REST endpoint: returns all packages. */
  /**
   * REST endpoint.
   */
  @GET
  @Path("/packages")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPackages getAllPackages() {
    var packages = new PSPackages();
    var pInfos = getPkgService().findAllPkgInfos();
    for (var pinfo : pInfos) {
      if (pinfo.isCreated()) continue;
      var pkg = new PSPackage();
      pkg.setName(pinfo.getPackageDescriptorName());
      pkg.setDesc(pinfo.getPackageDescription());
      pkg.setPublisher(pinfo.getPublisherName());
      pkg.setVersion(pinfo.getPackageVersion());
      if (!pinfo.isCreated()) pkg.setInstalldate(pinfo.getLastActionDate());
      pkg.setInstaller(pinfo.getLastActionByUser());
      pkg.setPackageStatus(getInstalledStatus(pinfo));
      pkg.setConfigStatus(getConfiguredStatus(pinfo));
      pkg.setCategory(pinfo.isSystem() ? SYSTEM : USER);
      boolean isPkgLocked = !pinfo.isEditable();
      pkg.setLockStatus(isPkgLocked ? PACKAGE_LOCKED : PACKAGE_UNLOCKED);
      packages.add(pkg);
    }
    return packages;
  }

  /**
   * REST endpoint.
   */
  @GET
  @Path("/reapplyVisibility")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPkgUiResponse reapplyVisibility(@QueryParam("packageNames") String packageNames) {
    if (StringUtils.isBlank(packageNames)) {
      return new PSPkgUiResponse(
          PSPkgUiResponseType.FAILURE,
          "Skipping the reapplying of visibility settings as packageNames parameter value is"
              + " empty");
    }
    try {
      return PSPackageServiceHelper.applyPackageVisibility(packageNames);
    } catch (Exception e) {
      return new PSPkgUiResponse(PSPkgUiResponseType.FAILURE, e.getLocalizedMessage());
    }
  }

  /**
   * REST endpoint.
   */
  @GET
  @Path("/reapplyConfigs")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPkgUiResponse reapplyConfiguration(@QueryParam("packageNames") String packageNames) {
    if (StringUtils.isBlank(packageNames)) {
      return new PSPkgUiResponse(
          PSPkgUiResponseType.FAILURE,
          "Skipping the reapplying of configuration settings as packageNames parameter value is"
              + " empty");
    }
    try {
      return PSPackageServiceHelper.applyConfiguartion(packageNames);
    } catch (Exception e) {
      return new PSPkgUiResponse(PSPkgUiResponseType.FAILURE, e.getLocalizedMessage());
    }
  }

  /**
   * REST endpoint.
   */
  @GET
  @Path("/packageCommunities")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPackageCommunities getPackageCommunities() {
    var pkgComms = new PSPackageCommunities();
    var pkgInfomap = PSPackageServiceHelper.getPkgGuidNameMap();
    var commsMap = getPkgCommsMap(pkgInfomap);
    for (var guid : pkgInfomap.keySet()) {
      var pkgComm = new PSPackageCommunity(pkgInfomap.get(guid), commsMap.get(guid));
      pkgComms.add(pkgComm);
    }
    return pkgComms;
  }

  /**
   * Gets the package / communities association.
   *
   * @param pkgInfomap the package ID/name map, assumed not null.
   * @return the map that maps the package ID to its associated communities. The associated
   *     communities is a comma delimited string of community names.
   */
  private Map<IPSGuid, String> getPkgCommsMap(Map<IPSGuid, String> pkgInfomap) {
    var srv = PSConfigServiceLocator.getConfigService();
    var result = new HashMap<IPSGuid, String>();
    for (var pkg : pkgInfomap.entrySet()) {
      var names = srv.loadCommunityVisibility(pkg.getValue());
      result.put(pkg.getKey(), PSPackageServiceHelper.getStringFromList(names));
    }
    return result;
  }

  /**
   * REST endpoint.
   */
  @GET
  @Path("/communityPackages")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSCommunityPackages getCommunityPackages() {
    return PSPackageServiceHelper.getCommunityPackages();
  }

  /**
   * REST endpoint.
   */
  @POST
  @Path("/updatePackageCommunities")
  @Consumes("application/x-www-form-urlencoded")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPkgUiResponse postUpdatePackageCommunities(
      @QueryParam("packageName") String packageName,
      @QueryParam("selectedComms") String selectedComms) {
    var response = new PSPkgUiResponse(PSPkgUiResponseType.SUCCESS, "");
    try {
      var commList = getListFromString(selectedComms);
      updatePkgComms(packageName, commList, true);
    } catch (Exception e) {
      response.setType(PSPkgUiResponseType.FAILURE);
      response.setMessage(e.getLocalizedMessage());
    }
    return response;
  }

  /**
   * REST endpoint.
   */
  @POST
  @Path("/updateCommunityPackages")
  @Consumes("application/x-www-form-urlencoded")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPkgUiResponse postUpdateCommunityPackages(
      @QueryParam("communityName") String communityName,
      @QueryParam("selectedPkgs") String selectedPkgs) {
    var response = new PSPkgUiResponse(PSPkgUiResponseType.SUCCESS, "");
    try {
      updateComPkgs(communityName, selectedPkgs);
    } catch (Exception e) {
      response.setType(PSPkgUiResponseType.FAILURE);
      response.setMessage(e.getLocalizedMessage());
    }
    return response;
  }

  /**
   * REST endpoint.
   */
  @POST
  @Path("/uninstallPackage")
  @Consumes("application/x-www-form-urlencoded")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSUninstallMessages postUninstallPackage(@QueryParam("packageName") String packageNames)
      throws PSNotFoundException {
    var msgs = new PSUninstallMessages();
    var pkgUninstall = new PSPackageUninstall();
    msgs.setMessages(pkgUninstall.uninstallPackages(packageNames));
    return msgs;
  }

  /**
   * REST endpoint.
   */
  @POST
  @Path("/checkPackageDependencies")
  @Consumes("application/x-www-form-urlencoded")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSUninstallMessages postCheckPackageDependencies(
      @QueryParam("packageName") String packageName) throws PSNotFoundException {
    var msgs = new PSUninstallMessages();
    var pkgUninstall = new PSPackageUninstall();
    msgs.setMessages(pkgUninstall.checkPackageDepedencies(packageName));
    return msgs;
  }

  /**
   * REST endpoint.
   */
  @GET
  @Path("/validationResults")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPkgUiResponse getValidationResults(@QueryParam("packageName") String packageName) {
    try {
      return PSPackageServiceHelper.getValidationResults(packageName);
    } catch (Exception e) {
      return new PSPkgUiResponse(PSPkgUiResponseType.FAILURE, e.getLocalizedMessage());
    }
  }

  /**
   * REST endpoint.
   */
  @GET
  @Path("serverTimeout")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPkgUiResponse getServerTimeout() {
    int sto = PSServer.getServerConfiguration().getUserSessionTimeout();
    return new PSPkgUiResponse(PSPkgUiResponseType.SUCCESS, String.valueOf(sto));
  }

  /**
   * REST endpoint.
   */
  @POST
  @Path("/convertPackage")
  @Consumes("application/x-www-form-urlencoded")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public PSPkgUiResponse postConvertPackage(@QueryParam("packageName") String packageName) {
    try {
      var cs = new PSConvertToSource();
      var result = cs.convert(packageName);
      var type = result.getFirst() ? PSPkgUiResponseType.SUCCESS : PSPkgUiResponseType.FAILURE;
      var message = result.getSecond();
      return new PSPkgUiResponse(type, message);
    } catch (Exception e) {
      ms_logger.error("error converting package", e);
      return new PSPkgUiResponse(PSPkgUiResponseType.FAILURE, e.getLocalizedMessage());
    }
  }

  /**
   * Updates the supplied packages and elements with the given community name. Loops through all
   * other package elements and removes the supplied community entry if exists.
   *
   * @param communityName name of the community must not be null.
   * @param selectedPkgs NAME_SEPARATOR separated list of package names.
   */
  private void updateComPkgs(String communityName, String selectedPkgs) {
    var commList = getListFromString(communityName);
    var pkgs = getListFromString(selectedPkgs);
    for (var pkg : pkgs) {
      if (StringUtils.isBlank(pkg)) continue;
      updatePkgComms(pkg, commList, false);
    }
    var pInfos = getPkgService().findAllPkgInfos();
    var objectGuids = new ArrayList<IPSGuid>();
    for (var info : pInfos) {
      if (pkgs.contains(info.getPackageDescriptorName())) continue;
      var pkgGuid = info.getGuid();
      objectGuids.add(pkgGuid);
      var pkgElems = getPkgService().findPkgElements(pkgGuid);
      for (var element : pkgElems) {
        objectGuids.add(element.getObjectGuid());
      }
    }
    var pkgVis = new PSPackageVisibility();
    pkgVis.clearCommunity(communityName, objectGuids);
  }

  /**
   * Applies the supplied communities on to the package and package elements.
   *
   * @param pkgName the name of an existing package.
   * @param commList a list of community names. If this is null, then apply all existing communities
   *     to the given package.
   * @param clearOtherEntries if true clears other community entries from the objects and applies
   *     the supplied communities. Otherwise leaves the current communities as is.
   */
  private void updatePkgComms(
      String pkgName, Collection<String> commList, boolean clearOtherEntries) {
    if (commList == null) throw new IllegalArgumentException("commList may not be null.");
    PSPackageServiceHelper.updatePkgCommunities(pkgName, commList, clearOtherEntries);
  }

  /**
   * Converts a comma delimited name list (as string) to a list of strings.
   *
   * @param commaList the comma delimited name list, assumed not blank.
   * @return the converted list, never null, may be empty.
   */
  private List<String> getListFromString(String commaList) {
    if (StringUtils.isBlank(commaList)) return Collections.emptyList();
    return Arrays.stream(commaList.split(NAME_SEPARATOR))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  /**
   * Returns the configuration service.
   *
   * @return the service instance, never null.
   */
  private IPSConfigService getConfigService() {
    return PSConfigServiceLocator.getConfigService();
  }

  /**
   * Returns the package info service.
   *
   * @return the service, never null.
   */
  private IPSPkgInfoService getPkgService() {
    return PSPkgInfoServiceLocator.getPkgInfoService();
  }

  /**
   * Determines install status of the package.
   *
   * @param info assumed not null.
   * @return the status code.
   */
  private String getInstalledStatus(PSPkgInfo info) {
    if (info.getLastAction().equals(PackageAction.UNINSTALL)) return UNINSTALLED;
    if (info.isSuccessfullyInstalled()) return SUCCESS;
    return ERROR;
  }

  /**
   * Determines configured status of the package.
   *
   * @param info assumed not null.
   * @return the status code.
   */
  private String getConfiguredStatus(PSPkgInfo info) {
    if (info.isCreated()
        || !info.isSuccessfullyInstalled()
        || info.getLastAction().equals(PackageAction.UNINSTALL)) return NONE;

    var cfgService = PSConfigServiceLocator.getConfigService();
    var cfgs = cfgService.getConfigStatus(info.getPackageDescriptorName());
    if (cfgs.isEmpty() && info.isSuccessfullyInstalled()) return NONE;
    if (cfgs.get(0).getStatus() == ConfigStatus.SUCCESS) return SUCCESS;
    return ERROR;
  }

  // Constants for installed and configured status;
  static final String SUCCESS = "Success";
  static final String ERROR = "Error";
  static final String WARNING = "Warning";
  static final String NONE = "None";
  static final String INFO = "Info";
  static final String UNINSTALLED = "Uninstall";

  // Constants for package categories
  static final String SYSTEM = "System";
  static final String USER = "User";

  // Constants for package lock status
  static final String PACKAGE_LOCKED = "Locked";
  static final String PACKAGE_UNLOCKED = "Unlocked";

  /** Separator used for separating communities. */
  /**
   * REST endpoint.
   */
  public static final String NAME_SEPARATOR = ",";

  /** The logger for this class. */
  private static final Logger ms_logger = LogManager.getLogger("PSPackageService");
}
