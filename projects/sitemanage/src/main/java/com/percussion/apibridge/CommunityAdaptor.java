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
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityList;
import com.percussion.rest.communities.CommunityRole;
import com.percussion.rest.communities.CommunityRoleList;
import com.percussion.rest.communities.CommunityVisibilityList;
import com.percussion.rest.communities.ICommunityAdaptor;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.system.IPSSystemWs;
import jakarta.ws.rs.WebApplicationException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean
public class CommunityAdaptor implements ICommunityAdaptor {

  private static final Logger log = LogManager.getLogger(CommunityAdaptor.class);

  @Autowired private IPSSecurityDesignWs securityDesignWs;

  @Autowired private IPSSystemWs systemWs;

  /***
   * Create one or more communities by name and return the results
   * @param names
   *
   */
  @Override
  public CommunityList createCommunities(List<String> names) {
    CommunityList ret;
    var communities = new ArrayList<Community>();

    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    List<PSCommunity> ps_communities;
    try {
      ps_communities = securityDesignWs.createCommunities(names, session, user);
    } catch (IllegalArgumentException e) {
      throw mapCreateNameFailure(names, e);
    }
    // Persist immediately (Workbench Finish / slots create+save). Do not
    // round-trip REST DTOs through saveCommunities — convertCommunity +
    // getRenamedCommunities/loadCommunities NPEs on unsaved stubs.
    try {
      securityDesignWs.saveCommunities(ps_communities, true, session, user);
    } catch (RuntimeException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException(communityAlreadyExistsMessage(names), 409);
      }
      throw new IllegalStateException("Failed to persist communities", e);
    }

    for (var c : ps_communities) {
      communities.add(ApiUtils.convertPSCommunity(c));
    }

    ret = new CommunityList(communities);

    return ret;
  }

  @Override
  public CommunityList findCommunities(String name) {
    var ps_summaries = securityDesignWs.findCommunities(name);
    var communities = new ArrayList<Community>();
    for (var s : ps_summaries) {
      communities.add(
          new Community(
              s.getGUID().longValue(),
              ApiUtils.convertGuid(s.getGUID()),
              s.getName(),
              s.getDescription(),
              s.getLabel()));
    }
    return new CommunityList(communities);
  }

  @Override
  public Community getCommunity(String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    Community summary = resolveSummary(key);
    if (summary == null || summary.getGuid() == null) {
      return null;
    }
    Guid g = summary.getGuid();
    GuidList ids = new GuidList();
    ids.add(g);
    try {
      CommunityList loaded = loadCommunities(ids, false, false);
      if (loaded == null || loaded.isEmpty()) {
        return summary;
      }
      Community detail = loaded.get(0);
      enrichRoleNames(detail);
      return detail;
    } catch (Exception e) {
      log.warn("Could not load community detail for {}: {}", key, e.getMessage());
      return summary;
    }
  }

  @Override
  public CommunityRoleList listAvailableRoles() {
    CommunityRoleList out = new CommunityRoleList();
    List<IPSCatalogSummary> roleSums = securityDesignWs.findRoles(null);
    if (roleSums == null) {
      return out;
    }
    for (IPSCatalogSummary s : roleSums) {
      if (s == null || s.getGUID() == null) {
        continue;
      }
      CommunityRole r = new CommunityRole();
      Guid g = ApiUtils.convertGuid(s.getGUID());
      r.setRoleGuid(g);
      r.setRoleId(s.getGUID().longValue());
      r.setRoleName(s.getName());
      out.add(r);
    }
    out.sort(
        (a, b) ->
            String.CASE_INSENSITIVE_ORDER.compare(
                a.getRoleName() == null ? "" : a.getRoleName(),
                b.getRoleName() == null ? "" : b.getRoleName()));
    return out;
  }

  @Override
  public Community updateCommunityRoles(String idOrName, CommunityRoleList roles) {
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    Community current = getCommunity(idOrName.trim());
    if (current == null || current.getGuid() == null) {
      return null;
    }
    // Replace memberships on the REST DTO then save via design WS (assign/unassign =
    // full set replace; empty list clears all role associations).
    CommunityRoleList next = roles != null ? roles : new CommunityRoleList();
    Guid communityGuid = current.getGuid();
    for (CommunityRole r : next) {
      if (r == null) {
        continue;
      }
      ensureRoleIdentity(r);
      r.setCommunityGuid(communityGuid);
      r.setCommunityId(current.getId());
    }
    current.setRoleList(next);

    CommunityList toSave = new CommunityList();
    toSave.add(current);
    // lock, apply, release
    GuidList ids = new GuidList();
    ids.add(communityGuid);
    try {
      CommunityList locked = loadCommunities(ids, true, true);
      if (locked != null && !locked.isEmpty()) {
        Community lockedComm = locked.get(0);
        lockedComm.setRoleList(next);
        CommunityList saveList = new CommunityList();
        saveList.add(lockedComm);
        saveCommunities(saveList, true);
      } else {
        saveCommunities(toSave, true);
      }
    } catch (Exception e) {
      log.error("Failed to update community roles for {}", idOrName, e);
      throw new IllegalStateException("Failed to update community roles", e);
    }
    return getCommunity(idOrName.trim());
  }

  private Community resolveSummary(String key) {
    // Prefer direct GUID / numeric load — avoid findCommunities("*") catalog scan
    Community byGuid = tryLoadByGuidKey(key);
    if (byGuid != null) {
      return byGuid;
    }
    // Exact name match only (find may support wildcards — do not accept sole non-exact hit)
    CommunityList byName = findCommunities(key);
    if (byName != null) {
      for (Community c : byName) {
        if (c != null && key.equalsIgnoreCase(c.getName())) {
          return c;
        }
      }
    }
    return null;
  }

  /**
   * Resolve a community summary via {@link #loadCommunities} when {@code key} is a numeric uuid or
   * GUID-shaped string. Returns null when the key is not id-like or load fails.
   */
  private Community tryLoadByGuidKey(String key) {
    Guid g = parseCommunityGuid(key);
    if (g == null) {
      return null;
    }
    GuidList ids = new GuidList();
    ids.add(g);
    try {
      CommunityList loaded = loadCommunities(ids, false, false);
      if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
        return loaded.get(0);
      }
    } catch (Exception e) {
      log.debug("Could not load community by guid {}: {}", key, e.getMessage());
    }
    return null;
  }

  private static Guid parseCommunityGuid(String key) {
    try {
      if (StringUtils.isNumeric(key)) {
        return ApiUtils.convertGuid(new PSGuid(PSTypeEnum.COMMUNITY_DEF, Long.parseLong(key)));
      }
      if (key.matches("\\d+-\\d+(-\\d+)?")) {
        PSGuid ps = new PSGuid(key);
        if (ps.getType() == 0) {
          ps = new PSGuid(PSTypeEnum.COMMUNITY_DEF, ps.getUUID());
        }
        return ApiUtils.convertGuid(ps);
      }
    } catch (Exception e) {
      // not a community guid key
    }
    return null;
  }

  /**
   * Ensure each role association has a role GUID. Clients may send {@code roleId} alone; synthesize
   * a ROLE-typed GUID (type/uuid only — avoid stringValue so save does not require GuidMgr). Missing
   * both identity fields is a 400 from the resource. When the client supplies {@code roleGuid} with
   * only {@code stringValue}, normalize {@code type} to ROLE so save does not persist a wrong
   * ordinal.
   */
  static void ensureRoleIdentity(CommunityRole role) {
    if (role.getRoleGuid() != null
        && (role.getRoleGuid().getUuid() != 0
            || StringUtils.isNotBlank(role.getRoleGuid().getStringValue()))) {
      Guid existing = role.getRoleGuid();
      if (existing.getType() != PSTypeEnum.ROLE.getOrdinal()) {
        existing.setType(PSTypeEnum.ROLE.getOrdinal());
      }
      if (role.getRoleId() <= 0 && existing.getUuid() != 0) {
        role.setRoleId(existing.getUuid());
      }
      return;
    }
    if (role.getRoleId() <= 0) {
      throw new IllegalArgumentException("Each role requires roleGuid or roleId");
    }
    Guid g = new Guid();
    g.setHostId(0);
    g.setType(PSTypeEnum.ROLE.getOrdinal());
    g.setUuid(Math.toIntExact(role.getRoleId()));
    g.setLongValue(role.getRoleId());
    role.setRoleGuid(g);
  }

  private void enrichRoleNames(Community detail) {
    if (detail == null || detail.getRoleList() == null) {
      return;
    }
    CommunityRoleList roles = detail.getRoleList();
    if (roles.isEmpty()) {
      return;
    }
    Map<Long, String> namesByUuid = new HashMap<>();
    try {
      List<IPSCatalogSummary> roleSums = securityDesignWs.findRoles(null);
      if (roleSums != null) {
        for (IPSCatalogSummary s : roleSums) {
          if (s != null && s.getGUID() != null) {
            namesByUuid.put(s.getGUID().longValue(), s.getName());
          }
        }
      }
    } catch (Exception e) {
      log.debug("Could not resolve role names: {}", e.getMessage());
      return;
    }
    for (CommunityRole r : roles) {
      if (r == null) continue;
      if (StringUtils.isBlank(r.getRoleName())) {
        String n = namesByUuid.get(r.getRoleId());
        if (n != null) {
          r.setRoleName(n);
        }
      }
    }
  }

  @Override
  public CommunityList loadCommunities(GuidList ids, boolean lock, boolean overrideLock)
      throws PSErrorResultsException {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    var ps_communities =
        securityDesignWs.loadCommunities(
            ApiUtils.convertGuids(ids), lock, overrideLock, session, user);

    return ApiUtils.convertPSCommunities(ps_communities);
  }

  @Override
  public void saveCommunities(CommunityList communities, boolean release) {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    securityDesignWs.saveCommunities(
        ApiUtils.convertCommunityList(communities), release, session, user);
  }

  @Override
  public void deleteCommunities(GuidList ids, boolean ignoreDependencies) {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    try {
      securityDesignWs.deleteCommunities(
          ApiUtils.convertGuids(ids), ignoreDependencies, session, user);
    } catch (PSErrorsException e) {
      throw new WebApplicationException("Community has dependencies", 409);
    }
  }

  static RuntimeException mapCreateNameFailure(List<String> names, IllegalArgumentException e) {
    if (isAlreadyExistsFailure(e)) {
      return new WebApplicationException(communityAlreadyExistsMessage(names), 409);
    }
    return e;
  }

  static String communityAlreadyExistsMessage(List<String> names) {
    String n = "";
    if (names != null) {
      for (String name : names) {
        if (StringUtils.isNotBlank(name)) {
          n = name.trim();
          break;
        }
      }
    }
    return StringUtils.isBlank(n) ? "Community already exists" : "Community already exists: " + n;
  }

  /** Design-WS uniqueness messages end with {@code already exists} (optional period). */
  private static final Pattern ALREADY_EXISTS_TAIL =
      Pattern.compile("(?i)already exists\\.?\\s*$");

  /**
   * True only for a top-level {@link IllegalArgumentException} whose message ends with the design-WS
   * uniqueness phrase. Does not walk causes — wrapped {@code Save failed: ... already exists} stays
   * a 500 rather than a false 409.
   */
  static boolean isAlreadyExistsFailure(Throwable t) {
    if (!(t instanceof IllegalArgumentException)) {
      return false;
    }
    String msg = t.getMessage();
    return msg != null && ALREADY_EXISTS_TAIL.matcher(msg).find();
  }

  @Override
  public CommunityVisibilityList getVisibilityByCommunity(GuidList ids, ObjectTypeEnum type)
      throws PSErrorResultsException, RemoteException {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    var ps_visibilities =
        securityDesignWs.getVisibilityByCommunity(
            ApiUtils.convertGuids(ids), ApiUtils.convertObjectTypeEnum(type), session, user);

    return new CommunityVisibilityList(ApiUtils.convertPSCommunityVisibilities(ps_visibilities));
  }

  @Override
  public void switchCommunity(String name) {
    systemWs.switchCommunity(name);
  }
}
