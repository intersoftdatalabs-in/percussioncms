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

package com.percussion.apibridge;

import com.percussion.rest.Guid;
import com.percussion.rest.GuidList;
import com.percussion.rest.ObjectTypeEnum;
import com.percussion.rest.acls.Acl;
import com.percussion.rest.acls.AclList;
import com.percussion.rest.acls.IAclAdaptor;
import com.percussion.rest.acls.TypedPrincipal;
import com.percussion.rest.acls.UserAccessLevel;
import com.percussion.security.PSSecurityException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.security.IPSAcl;
import com.percussion.services.security.IPSAclService;
import com.percussion.services.security.PSAclPersistMerger;
import com.percussion.services.security.PSServiceSecurityException;
import com.percussion.services.security.data.PSAclImpl;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

// REFACTORED: CP-JAVA11
@PSSiteManageBean
@Lazy
public class AclAdaptor implements IAclAdaptor {

  private final Logger log = LogManager.getLogger(this.getClass());

  @Autowired private IPSAclService aclService;

  /***
   * CTOR
   */
  public AclAdaptor() {
    // Left blank
  }

  @Override
  public UserAccessLevel getUserAccessLevel(Guid objectGuid) {
    return ApiUtils.convertPSUserAccessLevel(
        aclService.getUserAccessLevel(ApiUtils.convertGuid(objectGuid)));
  }

  @Override
  public UserAccessLevel calculateUserAccessLevel(String aclGuid) {
    UserAccessLevel ret = null;
    Guid g = null;
    IPSAcl acl = null;

    if (StringUtils.isNotEmpty(aclGuid)) {
      g = new Guid(aclGuid);
    }
    try {
      if (g != null) {
        // IPSAclService no longer exposes a single-load method; use loadAcls and
        // take the first result (or null if list empty).
        var guid = ApiUtils.convertGuid(g);
        try {
          var acls = aclService.loadAcls(List.of(guid));
          acl = acls.isEmpty() ? null : acls.get(0);
        } catch (PSServiceSecurityException e) {
          throw new RuntimeException(e);
        }
      }
    } catch (PSSecurityException e) {
      log.error("Error loading acl {}", aclGuid, e);
    }

    ret = ApiUtils.convertPSUserAccessLevel(aclService.calculateUserAccessLevel(acl));

    return ret;
  }

  @Override
  public Acl createAcl(Guid objGuid, TypedPrincipal owner) {
    return ApiUtils.convertAcl(
        (PSAclImpl)
            aclService.createAcl(
                ApiUtils.convertGuid(objGuid), ApiUtils.convertPrincipalType(owner)));
  }

  @Override
  public AclList loadAcls(GuidList aclGuids) throws PSServiceSecurityException {
    return ApiUtils.convertAcls(aclService.loadAcls(ApiUtils.convertGuids(aclGuids)));
  }

  @Override
  public Acl loadAcl(Guid aclGuid) throws PSServiceSecurityException {
    // IPSAclService no longer exposes a single-load method; delegate to loadAcls
    List<IPSAcl> list = aclService.loadAcls(List.of(ApiUtils.convertGuid(aclGuid)));
    IPSAcl acl = list.isEmpty() ? null : list.get(0);
    return ApiUtils.convertAcl((PSAclImpl) acl);
  }

  @Override
  public AclList loadAclsForObjects(GuidList objectGuids) {
    try {
      return ApiUtils.convertAcls(
          aclService.loadAclsForObjects(ApiUtils.convertGuids(objectGuids)));
    } catch (PSServiceSecurityException e) {
      // wrap since interface doesn't allow checked exception
      throw new RuntimeException(e);
    }
  }

  @Override
  public Acl loadAclForObject(Guid objectGuid) {
    try {
      var ret =
          ApiUtils.convertAcl(
              (PSAclImpl) aclService.loadAclForObject(ApiUtils.convertGuid(objectGuid)));
      if (ret != null) {
        return ret;
      } else {
        throw new NotFoundException();
      }
    } catch (PSServiceSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void saveAcls(AclList aclList) throws PSServiceSecurityException {
    if (aclList == null) {
      aclService.saveAcls(null);
      return;
    }
    List<IPSAcl> toSave = new ArrayList<>();
    for (Acl restAcl : aclList) {
      if (restAcl == null) {
        continue;
      }
      PSAclImpl incoming = ApiUtils.convertAcl(restAcl);
      IPSAcl existing = loadExistingAclForSave(incoming);
      if (existing instanceof PSAclImpl persistent && persistent.getId() != 0) {
        toSave.add(PSAclPersistMerger.mergeOntoExisting(persistent, incoming));
      } else {
        toSave.add(incoming);
      }
    }
    aclService.saveAcls(toSave);
  }

  /**
   * Load the persisted ACL for this object or ACL SYSID so save merges entries
   * onto that identity (#3384).
   */
  private IPSAcl loadExistingAclForSave(PSAclImpl incoming) {
    if (incoming == null) {
      return null;
    }
    IPSGuid objectGuid = incoming.getObjectGuid();
    if (objectGuid != null) {
      try {
        IPSAcl byObject = aclService.loadAclForObjectModifiable(objectGuid);
        if (byObject != null) {
          return byObject;
        }
      } catch (PSServiceSecurityException e) {
        log.debug("No existing ACL for objectGuid {}", objectGuid, e);
      }
    }
    if (incoming.getId() != 0) {
      try {
        IPSGuid aclGuid = incoming.getGUID();
        if (aclGuid != null) {
          List<IPSAcl> loaded = aclService.loadAclsModifiable(List.of(aclGuid));
          if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
            return loaded.get(0);
          }
        }
      } catch (PSServiceSecurityException e) {
        log.debug("No existing ACL for id {}", incoming.getId(), e);
      }
    }
    return null;
  }

  /** Package-visible for unit tests. */
  void setAclService(IPSAclService aclService) {
    this.aclService = aclService;
  }

  @Override
  public void deleteAcl(Guid aclGuid) throws PSServiceSecurityException {
    aclService.deleteAcl(ApiUtils.convertGuid(aclGuid));
  }

  @Override
  public GuidList filterByCommunities(GuidList aclList, List<String> communityNames) {
    if (aclList == null
        || aclList.isEmpty()
        || communityNames == null
        || communityNames.isEmpty()) {
      return new GuidList();
    }
    // load the ACLs to map to object GUIDs
    List<IPSGuid> aclGuids = ApiUtils.convertGuids(aclList);
    List<IPSAcl> acls;
    try {
      acls = aclService.loadAcls(aclGuids);
    } catch (PSServiceSecurityException e) {
      throw new RuntimeException(e);
    }
    // compute object GUIDs visible to communities
    Collection<IPSGuid> visibleObjects =
        aclService.findObjectsVisibleToCommunities(communityNames, null);
    // filter original ACL GUIDs based on object membership
    GuidList result = new GuidList();
    for (IPSAcl acl : acls) {
      if (acl != null && visibleObjects.contains(acl.getObjectGuid())) {
        result.add(ApiUtils.convertGuid(acl.getGUID()));
      }
    }
    return result;
  }

  @Override
  public GuidList findObjectsVisibleToCommunities(
      List<String> communityNames, ObjectTypeEnum objectType) {
    return ApiUtils.convertGuids(
        aclService.findObjectsVisibleToCommunities(
            communityNames, PSTypeEnum.valueOf(objectType.name())));
  }
}
