// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.services.pkginfo.utils;

import com.percussion.rx.config.PSConfigServiceLocator;
import com.percussion.rx.design.PSDesignModelUtils;
import com.percussion.rx.services.deployer.PSPackageVisibility;
import com.percussion.server.PSServer;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.IPSPkgInfoService;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgElement;
import com.percussion.util.IOTools;
import com.percussion.util.PSOsTool;
import com.percussion.utils.guid.IPSGuid;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/** Utility class for detecting modifications to packaged elements. */
public class PSPkgHelper {
  /**
   * Detects modifications to packaged elements made outside configuration.
   *
   * @param guid The package info GUID, not null.
   * @return Set of modified design objects, never null.
   */
  public static Set<String> validatePackage(IPSGuid guid) throws PSNotFoundException {
    if (guid == null) {
      throw new IllegalArgumentException("guid may not be null");
    }
    var comms = getCommunityVisibility(guid);
    var pkgElemGuids = getPkgInfoService().findPkgElementGuids(guid);
    return pkgElemGuids.stream()
        .map(getPkgInfoService()::findPkgElement)
        .filter(Objects::nonNull)
        .flatMap(pkgElem -> validatePkgElement(pkgElem, comms).stream())
        .collect(Collectors.toSet());
  }

  /**
   * Updates each element version in the supplied package with the current version.
   *
   * @param pkgName The package name, not blank.
   */
  public static void updatePkgElementVersions(String pkgName) throws PSNotFoundException {
    if (!enabled) return;
    if (StringUtils.isBlank(pkgName)) {
      throw new IllegalArgumentException("pkgName may not be blank");
    }
    var pkgInfo = getPkgInfoService().findPkgInfo(pkgName);
    if (pkgInfo == null) {
      throw new RuntimeException(
          MessageFormat.format("Failed to get the package info for {0}", pkgName));
    }
    var pkgElemGuids = getPkgInfoService().findPkgElementGuids(pkgInfo.getGuid());
    for (var pkgElemGuid : pkgElemGuids) {
      updatePkgElementVersion(pkgElemGuid, true);
    }
  }

  /**
   * Detects modifications to packaged elements by package name.
   *
   * @param pkgName The package name, not blank.
   * @return See {@link #validatePackage(IPSGuid)}.
   */
  public static Set<String> validatePackage(String pkgName) throws PSNotFoundException {
    if (!enabled) return new HashSet<>();
    if (StringUtils.isBlank(pkgName)) {
      throw new IllegalArgumentException("pkgName may not be blank");
    }
    var pkgInfo = getPkgInfoService().findPkgInfo(pkgName);
    if (pkgInfo == null) {
      throw new RuntimeException(
          MessageFormat.format("Failed to get the package info for {0}", pkgName));
    }
    return validatePackage(pkgInfo.getGuid());
  }

  /**
   * Updates the version for the supplied package element.
   *
   * @param id The package element GUID, not null.
   * @param forceUpdate If true, always update version.
   */
  private static void updatePkgElementVersion(IPSGuid id, boolean forceUpdate)
      throws PSNotFoundException {
    var pkgElem = getPkgInfoService().loadPkgElementModifiable(id);
    Long version;
    try {
      version = getVersion(pkgElem);
    } catch (IOException e) {
      throw new RuntimeException(
          MessageFormat.format(
              "Failed to get the design object version for package element with guid {0}", id),
          e);
    }
    if (version == null) return;
    if (pkgElem.getVersion() != OBJECT_MODIFIED_VERSION || forceUpdate) {
      pkgElem.setVersion(version);
      getPkgInfoService().savePkgElement(pkgElem);
    }
  }

  /**
   * Updates the corresponding element versions for the supplied Design Objects.
   *
   * @param ids The Design Object GUIDs, not null.
   */
  public static void updatePkgElementVersions(Collection<IPSGuid> ids) throws PSNotFoundException {
    if (!enabled) return;
    if (ids == null) throw new IllegalArgumentException("ids may not be null");
    for (var id : ids) {
      var elemId = id;
      var type = PSTypeEnum.valueOf(id.getType());
      if (PSIdNameHelper.isSupported(type)) {
        var name = PSDesignModelUtils.getName(id);
        if (name != null) {
          elemId = PSIdNameHelper.getGuid(name, type);
        }
      }
      var pkgElem = getPkgInfoService().findPkgElementByObject(elemId);
      if (pkgElem == null) continue;
      updatePkgElementVersion(pkgElem.getGuid(), false);
    }
  }

  /**
   * Determines if the specified package element has been modified outside allowed configuration.
   *
   * @param pkgElem The package element, not null.
   * @param comms The set of community names, may be null.
   * @return List of warnings for modified objects, never null.
   */
  public static List<String> validatePkgElement(PSPkgElement pkgElem, Collection<String> comms)
      throws PSNotFoundException {
    if (pkgElem == null) {
      throw new IllegalArgumentException("pkgElem may not be null");
    }
    var warnList = new ArrayList<String>();
    var objGuid = pkgElem.getObjectGuid();
    var objType = PSTypeEnum.valueOf(pkgElem.getObjectType());
    if (objType.equals(PSTypeEnum.ACL)) return warnList;
    final String objName =
        PSIdNameHelper.isSupported(objType)
            ? PSIdNameHelper.getName(objGuid)
            : PSDesignModelUtils.getName(objGuid);
    Long version;
    try {
      version = getVersion(pkgElem);
    } catch (IOException e) {
      throw new RuntimeException(
          MessageFormat.format(
              "Failed to get the design object version for package element guid {0}",
              pkgElem.getGuid()),
          e);
    }
    if (version != null && !version.equals(pkgElem.getVersion())) {
      warnList.add(objName + '(' + objType.getDisplayName() + ')');
    }
    if (comms != null) {
      var commWarn = validateCommunityVisibility(objGuid, objName, objType.getDisplayName(), comms);
      if (commWarn != null) warnList.add(commWarn);
    }
    if (pkgElem.getVersion() != OBJECT_MODIFIED_VERSION && !warnList.isEmpty()) {
      var pElem = getPkgInfoService().loadPkgElementModifiable(pkgElem.getGuid());
      pElem.setVersion(OBJECT_MODIFIED_VERSION);
      getPkgInfoService().savePkgElement(pElem);
    }
    return warnList;
  }

  /** Validates the community visibility of the given Design Object. */
  private static String validateCommunityVisibility(
      IPSGuid id, String name, String type, Collection<String> commsVisibility) {
    var vis = new PSPackageVisibility();
    if (!vis.validatePkgCommunities(id, commsVisibility)) {
      return name + "(" + type + ") does not match the configuration of community visibility.";
    }
    return null;
  }

  /** Gets the current version of the design object for the supplied package element. */
  private static Long getVersion(PSPkgElement pkgElem) throws IOException {
    Long version = null;
    var objGuid = pkgElem.getObjectGuid();
    var objType = PSTypeEnum.valueOf(objGuid.getType());
    if (PSIdNameHelper.isSupported(objType)) {
      var objName = PSIdNameHelper.getName(objGuid);
      if (objName != null) {
        version = PSDesignModelUtils.getVersion(objType, objName);
        if (version == null && (objName.startsWith("/") || objName.startsWith("\\"))) {
          if (objName.trim().length() > 1) objName = objName.substring(1);
        }
        if (version == null) {
          if (PSOsTool.isUnixPlatform()) objName = objName.replace('\\', '/');
          var file = new File(PSServer.getRxDir(), objName);
          if (file.exists()) version = IOTools.getChecksum(file);
        }
      }
    } else {
      version = PSDesignModelUtils.getVersion(objGuid);
    }
    return version;
  }

  /** Used by config service unit tests to enable/disable package helper. */
  /**
   * REST endpoint.
   */
  public static void setEnabled(boolean enabledFlag) {
    enabled = enabledFlag;
  }

  /** Returns the package info service, initializing if necessary. */
  private static IPSPkgInfoService getPkgInfoService() {
    if (pkgInfoSvc == null) {
      pkgInfoSvc = PSPkgInfoServiceLocator.getPkgInfoService();
    }
    return pkgInfoSvc;
  }

  /** Loads the community visibility for the supplied package. */
  private static Collection<String> getCommunityVisibility(IPSGuid guid)
      throws PSNotFoundException {
    var pkg = getPkgInfoService().loadPkgInfo(guid);
    var srv = PSConfigServiceLocator.getConfigService();
    return srv.loadCommunityVisibility(pkg.getPackageDescriptorName());
  }

  /** Constant indicating a package element has been modified outside allowed configuration. */
  /**
   * REST endpoint.
   */
  public static final long OBJECT_MODIFIED_VERSION = -1L;

  /** The package info service, may be null. */
  private static IPSPkgInfoService pkgInfoSvc;

  /** Flag to enable/disable package helper. Defaults to true. */
  private static boolean enabled = true;
}
