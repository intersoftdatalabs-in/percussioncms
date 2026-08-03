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

import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.config.PSConfigServiceLocator;
import com.percussion.security.IPSTypedPrincipal.PrincipalTypes;
import com.percussion.security.shim.acl.NotOwnerException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.security.IPSAcl;
import com.percussion.services.security.IPSAclEntry;
import com.percussion.services.security.PSAclServiceLocator;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.PSTypedPrincipal;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.security.PSSecurityWsLocator;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles the visibility of packages and package elements with the communities. Sunny Sal says:
 * "Visibility is key—just like good comments!"
 *
 * @author bjoginipally
 */
public class PSPackageVisibility {

  /**
   * Gets the communities of the supplied guids.
   *
   * @param guids must not be {@code null}.
   * @return Map of supplied guid and associated communities.
   */
  public Map<IPSGuid, String> getCommunities(List<IPSGuid> guids) {
    if (guids == null) throw new IllegalArgumentException("guids must not be null");
    var objComms = new HashMap<IPSGuid, String>();
    var acls = loadAcl(guids);
    for (int i = 0; i < acls.size(); i++) {
      var acl = acls.get(i);
      var comms = "";
      if (acl != null) {
        comms = getCommunitiesFromAcl(acl);
      }
      objComms.put(guids.get(i), comms);
    }
    return objComms;
  }

  /**
   * Returns the collection of community names associated to the supplied object guid through acls.
   *
   * @param id The guid of the object, must not be {@code null}.
   * @return Collection of community names never {@code null}, may be empty.
   */
  public Collection<String> getCommunities(IPSGuid id) {
    if (id == null) throw new IllegalArgumentException("id must not be null");
    var comms = new HashSet<String>();
    var acls = loadAcl(Collections.singletonList(id));
    if (acls.get(0) != null) {
      comms.addAll(getCommunityListFromAcl(acls.get(0)));
    }
    return comms;
  }

  /**
   * Converts the supplied guids and then loads the ACLs for the converted guids.
   *
   * @param ids list of guids for which the acls needs to be loaded, assumed not {@code null}.
   * @return One ACL for each corresponding object id. Some of the entries may be {@code null} if
   *     the object does not have an ACL. The results are in the same order as the supplied ids.
   */
  private List<IPSAcl> loadAcl(List<IPSGuid> ids) {
    var guids = getConvertedGuids(ids);
    var aclServ = PSAclServiceLocator.getAclService();
    try {
      return aclServ.loadAclsForObjects(guids);
    } catch (Exception e) {
      throw new PSConfigException("Error loading acls", e);
    }
  }

  /**
   * Extracts communities from the supplied Acl.
   *
   * @param acl assumed not {@code null}.
   * @return Comma separated string of communities. May be empty, never {@code null}.
   */
  private String getCommunitiesFromAcl(IPSAcl acl) {
    return getCommunityListFromAcl(acl).stream()
        .collect(Collectors.joining(PSPackageService.NAME_SEPARATOR));
  }

  /**
   * Extracts communities from the supplied Acl.
   *
   * @param acl assumed not {@code null}.
   * @return a list of community names. May be empty, never {@code null}.
   */
  private Collection<String> getCommunityListFromAcl(IPSAcl acl) {
    var comms = new HashSet<String>();
    var entries = acl.entries();
    while (entries != null && entries.hasMoreElements()) {
      var aclEntry = (IPSAclEntry) entries.nextElement();
      if (aclEntry.getTypedPrincipal().isCommunity()) {
        var comName = aclEntry.getPrincipal().getName();
        if (comName.equals(PSTypedPrincipal.ANY_COMMUNITY_ENTRY)) {
          comms.add(PSTypedPrincipal.ANY_COMMUNITY_ENTRY);
          break;
        }
        comms.add(comName);
      }
    }
    return comms;
  }

  /**
   * Returns the list of all community names.
   *
   * @return all communities list never {@code null}.
   */
  public List<String> getAllCommunities() {
    var allComms = new ArrayList<String>();
    var secWs = PSSecurityWsLocator.getSecurityDesignWebservice();
    var sums = secWs.findCommunities(null);
    for (var sum : sums) {
      allComms.add(sum.getName());
    }
    return allComms;
  }

  /**
   * Set the community entries for the supplied object guid.
   *
   * @param objectGuid Object guid must not be {@code null}.
   * @param communityNames must not be {@code null} and must be a valid community list.
   * @param clearOtherCommEntries if {@code true} clears other community entries on the design
   *     object.
   */
  public void setCommunities(
      IPSGuid objectGuid, Collection<String> communityNames, boolean clearOtherCommEntries) {
    if (objectGuid == null) throw new IllegalArgumentException("objectGuid must not be null");
    if (communityNames == null)
      throw new IllegalArgumentException("communityNames must not be null");
    if (!communityNames.isEmpty() && !areValidCommunities(communityNames))
      throw new IllegalArgumentException("supplied communityNames are invalid.");
    if (!isVisibilitySupportedType(objectGuid)) return;
    objectGuid = getConvertedGuids(Collections.singletonList(objectGuid)).get(0);

    var aclServ = PSAclServiceLocator.getAclService();
    try {
      var acls = aclServ.loadAclsForObjectsModifiable(Collections.singletonList(objectGuid));
      var acl = acls.get(0);
      if (acl == null) {
        var owner = new PSTypedPrincipal(PSTypedPrincipal.DEFAULT_USER_ENTRY, PrincipalTypes.USER);
        acl = aclServ.createAcl(objectGuid, owner);
        acls.clear();
        acls.add(acl);
      }

      if (clearOtherCommEntries) clearAclEntries(acl, null);

      for (var comm : communityNames) {
        var entry = acl.createEntry(new PSTypedPrincipal(comm, PrincipalTypes.COMMUNITY));
        entry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry(acl.getFirstOwner(), entry);
      }
      aclServ.saveAcls(acls);
    } catch (Exception e) {
      ms_log.debug("Error setting communities...", e);
    }
  }

  /**
   * Validates the community visibility for the given object ID.
   *
   * @param objId the object ID, it may not be {@code null}.
   * @param commsVisibility the list of community names to validate with, never {@code null}, may be
   *     empty.
   * @return {@code true} if the community visibility of the given object matches the supplied
   *     communities or does not have community visibility ACLs; {@code false} otherwise.
   */
  public boolean validatePkgCommunities(IPSGuid objId, Collection<String> commsVisibility) {
    if (objId == null) throw new IllegalArgumentException("objId may not be null.");

    if (!isVisibilitySupportedType(objId)) return true;

    var comms = getCommunities(objId);
    return comms.equals(commsVisibility);
  }

  /**
   * Load and set the package communities to the specified package.
   *
   * @param pkg the package info, it may not be {@code null}.
   * @return {@code null} if successful load and set the communities; otherwise return the error
   *     message should an error occur.
   */
  public String setPkgCommunities(PSPkgInfo pkg) {
    if (pkg == null) throw new IllegalArgumentException("package may not be null.");

    try {
      var srv = PSConfigServiceLocator.getConfigService();
      var comms = srv.loadCommunityVisibility(pkg.getPackageDescriptorName());
      var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
      var pkgElems = pkgService.findPkgElements(pkg.getGuid());
      for (var element : pkgElems) {
        var guid = element.getObjectGuid();
        setCommunities(guid, comms, true);
      }
      return null;
    } catch (Exception e) {
      ms_log.error(
          "Failed to apply community visibility for package: \""
              + pkg.getPackageDescriptorName()
              + "\"",
          e);
      return StringUtils.isBlank(e.getLocalizedMessage()) ? e.toString() : e.getLocalizedMessage();
    }
  }

  /**
   * Checks whether the supplied community list is valid.
   *
   * @param communities assumed not {@code null}.
   * @return {@code true} if all the communities inside the supplied list are valid communities,
   *     otherwise {@code false}.
   */
  private boolean areValidCommunities(Collection<String> communities) {
    var comms = getAllCommunities();
    return comms.containsAll(communities);
  }

  /**
   * Clears the supplied community acl entry if exists from the supplied object guids.
   *
   * @param communityName must not be blank and must be a valid community.
   * @param objectGuids must not be {@code null}.
   */
  public void clearCommunity(String communityName, List<IPSGuid> objectGuids) {
    if (StringUtils.isBlank(communityName))
      throw new IllegalArgumentException("communityName must not be blank");
    if (!areValidCommunities(Collections.singletonList(communityName)))
      throw new IllegalArgumentException("supplied communityName is invalid.");
    if (objectGuids == null) throw new IllegalArgumentException("objectGuids must not be null");
    var aclServ = PSAclServiceLocator.getAclService();
    var guids =
        objectGuids.stream().filter(this::isVisibilitySupportedType).collect(Collectors.toList());
    guids = getConvertedGuids(guids);
    try {
      var acls = aclServ.loadAclsForObjectsModifiable(guids);
      var aclsToSave = new ArrayList<IPSAcl>();
      for (var acl : acls) {
        if (acl == null) continue;
        aclsToSave.add(acl);
        clearAclEntries(acl, communityName);
      }
      aclServ.saveAcls(aclsToSave);
    } catch (Exception e) {
      throw new PSConfigException(e);
    }
  }

  /**
   * Returns {@code true} if the supplied guid type supports visibility setting.
   *
   * @param objectGuid assumed not {@code null}.
   * @return {@code true} if the supplied guid type supported by security list, otherwise {@code
   *     false}.
   */
  private boolean isVisibilitySupportedType(IPSGuid objectGuid) {
    return ms_visibilitySupportedTypes.contains(PSTypeEnum.valueOf(objectGuid.getType()));
  }

  /**
   * Clears supplied community entry or all community acl entries if the communityName is null, from
   * the supplied acl.
   *
   * @param acl assumed not {@code null}.
   * @param communityName if {@code null} all entries are removed, if not specified community entry
   *     is removed.
   */
  private void clearAclEntries(IPSAcl acl, String communityName) throws NotOwnerException {
    var entries = acl.entries();
    var currEntries = new ArrayList<IPSAclEntry>();
    while (entries != null && entries.hasMoreElements()) {
      var aclEntry = (IPSAclEntry) entries.nextElement();
      if (aclEntry.getTypedPrincipal().isCommunity()) {
        if (communityName == null) currEntries.add(aclEntry);
        else if (communityName.equals(aclEntry.getPrincipal().getName())) currEntries.add(aclEntry);
      }
    }
    for (var entry : currEntries) {
      acl.removeEntry(acl.getFirstOwner(), entry);
    }
  }

  /**
   * Utility method to convert supplied guids into guids without host string.
   *
   * @param guids List of guids to convert, must not be {@code null}.
   * @return List of converted guids, never {@code null}.
   */
  public List<IPSGuid> getConvertedGuids(List<IPSGuid> guids) {
    if (guids == null) throw new IllegalArgumentException("guids must not be null");
    var results = new ArrayList<IPSGuid>();
    var gmgr = PSGuidManagerLocator.getGuidMgr();
    for (var guid : guids) {
      if (guid.getHostId() == 0) {
        results.add(guid);
        continue;
      }
      var g =
          gmgr.makeGuid(Long.parseLong(guid.getUUID() + ""), PSTypeEnum.valueOf(guid.getType()));
      results.add(g);
    }
    return results;
  }

  /**
   * Set the community entries for the specified package. It sets the communities for all the
   * elements in the given package.
   *
   * @param pkgGuid the ID of the package, must not be {@code null}.
   * @param communities community names to be set to, must not be {@code null} and must be a valid
   *     community list.
   * @param clearOtherCommEntries if {@code true} clears other community entries on the design
   *     object.
   */
  public void setPackageCommunities(
      IPSGuid pkgGuid, Collection<String> communities, boolean clearOtherCommEntries) {
    var pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
    for (var element : pkgService.findPkgElements(pkgGuid)) {
      var guid = element.getObjectGuid();
      setCommunities(guid, communities, clearOtherCommEntries);
    }
  }

  /** List of type {@link PSTypeEnum} types that support visibility. */
  /**
   * REST endpoint.
   */
  public static List<PSTypeEnum> ms_visibilitySupportedTypes = new ArrayList<>();

  static {
    ms_visibilitySupportedTypes.add(PSTypeEnum.NODEDEF);
    ms_visibilitySupportedTypes.add(PSTypeEnum.DISPLAY_FORMAT);
    ms_visibilitySupportedTypes.add(PSTypeEnum.ACTION);
    ms_visibilitySupportedTypes.add(PSTypeEnum.SEARCH_DEF);
    ms_visibilitySupportedTypes.add(PSTypeEnum.SITE);
    ms_visibilitySupportedTypes.add(PSTypeEnum.TEMPLATE);
    ms_visibilitySupportedTypes.add(PSTypeEnum.VIEW_DEF);
    ms_visibilitySupportedTypes.add(PSTypeEnum.WORKFLOW);
  }

  /** Logger for this class. */
  /**
   * REST endpoint.
   */
  public static final Logger ms_log = LogManager.getLogger("PSPackageVisibility");
}
