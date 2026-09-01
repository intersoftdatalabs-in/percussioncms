/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import com.percussion.cms.objectstore.PSSearch;
import com.percussion.rest.Guid;
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityNewSearchDefaults;
import com.percussion.rest.communities.CommunityNewSearchDefaultsDesignLockException;
import com.percussion.rest.communities.CommunityNewSearchRef;
import com.percussion.rest.communities.ICommunityAdaptor;
import com.percussion.rest.communities.ICommunityNewSearchDefaultsAdaptor;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Community CX new-search defaults (UI-09). Reads and writes the Workbench {@code cxNewSearch}
 * property on design searches through {@link IPSUiDesignWs} — the same path SOAP / Workbench uses.
 * Does not create or delete searches.
 */
@PSSiteManageBean
@Lazy
public class CommunityNewSearchDefaultsAdaptor implements ICommunityNewSearchDefaultsAdaptor {

  private static final Logger log = LogManager.getLogger(CommunityNewSearchDefaultsAdaptor.class);

  static final String ADMIN_REQUIRED =
      "Admin role required to read or update community new-search defaults";

  static final String LOCK_REQUIRED =
      "Could not update new-search defaults; design lock required or held by another user";

  /** Max path-token length for community id / name / GUID (path-injection surface). */
  static final int MAX_COMMUNITY_KEY_LENGTH = 256;

  private final IPSUiDesignWs designWs;
  private final ICommunityAdaptor communityAdaptor;
  private final BooleanSupplier adminChecker;
  private final IPSUserService userService;

  /**
   * Production constructor. Missing {@link IPSUserService} fails at context load.
   *
   * @param designWs UI design web service (search load/save)
   * @param communityAdaptor community lookup by id/name/GUID
   * @param userService current-user Admin gate
   */
  @Autowired
  public CommunityNewSearchDefaultsAdaptor(
      IPSUiDesignWs designWs, ICommunityAdaptor communityAdaptor, IPSUserService userService) {
    this(designWs, communityAdaptor, null, userService);
  }

  /** Package-visible for unit tests. {@code null} adminChecker uses {@link #isCurrentUserAdmin()}. */
  CommunityNewSearchDefaultsAdaptor(
      IPSUiDesignWs designWs, ICommunityAdaptor communityAdaptor, BooleanSupplier adminChecker) {
    this(designWs, communityAdaptor, adminChecker, null);
  }

  private CommunityNewSearchDefaultsAdaptor(
      IPSUiDesignWs designWs,
      ICommunityAdaptor communityAdaptor,
      BooleanSupplier adminChecker,
      IPSUserService userService) {
    this.designWs = designWs;
    this.communityAdaptor = communityAdaptor;
    this.userService = userService;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public CommunityNewSearchDefaults getDefaults(String communityIdOrName) {
    requireAdmin();
    Community community = resolveCommunity(communityIdOrName);
    if (community == null) {
      return null;
    }
    int communityId = communityNumericId(community);
    try {
      return toWire(community, filterAssigned(loadAllSearches(false), communityId));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to load community new-search defaults for {}", communityIdOrName, e);
      throw new IllegalStateException("Failed to load community new-search defaults", e);
    }
  }

  @Override
  public CommunityNewSearchDefaults replaceDefaults(
      String communityIdOrName, CommunityNewSearchDefaults body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    Community community = resolveCommunity(communityIdOrName);
    if (community == null) {
      return null;
    }
    int communityId = communityNumericId(community);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSSearch> catalog = loadAllSearches(false);
      List<PSSearch> desired = resolveRequestedSearches(body.getSearches(), catalog);
      List<PSSearch> current = filterAssigned(catalog, communityId);
      Set<String> desiredKeys = identityKeys(desired);
      Set<String> currentKeys = identityKeys(current);

      List<PSSearch> toMutate = new ArrayList<>();
      for (PSSearch search : catalog) {
        if (search == null || search.getGUID() == null) {
          continue;
        }
        String key = identityKey(search);
        boolean want = desiredKeys.contains(key);
        boolean have = currentKeys.contains(key);
        if (want != have) {
          toMutate.add(search);
        }
      }

      if (!toMutate.isEmpty()) {
        List<IPSGuid> ids = new ArrayList<>();
        for (PSSearch s : toMutate) {
          ids.add(s.getGUID());
        }
        List<PSSearch> locked = designWs.loadSearches(ids, true, false, session, user);
        if (locked == null || locked.isEmpty()) {
          throw lockConflict(null);
        }
        for (PSSearch domain : locked) {
          if (domain == null) {
            continue;
          }
          boolean want = desiredKeys.contains(identityKey(domain));
          applyCommunityAssignment(domain, communityId, want);
        }
        designWs.saveSearches(locked, true, session, user);
        catalog = loadAllSearches(false);
      }
      return toWire(community, filterAssigned(catalog, communityId));
    } catch (WebApplicationException
        | IllegalArgumentException
        | CommunityNewSearchDefaultsDesignLockException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      throw lockConflict(e);
    } catch (PSErrorsException e) {
      if (SearchAdaptor.isNotLockedError(e)) {
        throw lockConflict(e);
      }
      log.error("Failed to save community new-search defaults for {}", communityIdOrName, e);
      throw new IllegalStateException("Failed to save community new-search defaults", e);
    } catch (PSLockErrorException e) {
      throw lockConflict(e);
    } catch (Exception e) {
      log.error("Failed to replace community new-search defaults for {}", communityIdOrName, e);
      throw new IllegalStateException("Failed to replace community new-search defaults", e);
    }
  }

  Community resolveCommunity(String communityIdOrName) {
    if (communityIdOrName == null) {
      return null;
    }
    String key = communityIdOrName.trim();
    if (!isSafeCommunityKey(key)) {
      return null;
    }
    return communityAdaptor.getCommunity(key);
  }

  List<PSSearch> resolveRequestedSearches(List<CommunityNewSearchRef> refs, List<PSSearch> catalog) {
    List<CommunityNewSearchRef> requested = refs != null ? refs : List.of();
    List<PSSearch> resolved = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (CommunityNewSearchRef ref : requested) {
      if (ref == null) {
        throw new IllegalArgumentException("search ref is required");
      }
      String key = searchRefKey(ref);
      if (StringUtils.isBlank(key)) {
        throw new IllegalArgumentException("search name, id, or guid is required");
      }
      if (!SearchAdaptor.isSafeSearchKey(key)) {
        throw new IllegalArgumentException("Unknown search: " + key);
      }
      PSSearch found = SearchAdaptor.matchLoaded(catalog, key);
      if (found == null) {
        throw new IllegalArgumentException("Unknown search: " + key);
      }
      String identity = identityKey(found);
      if (!seen.add(identity)) {
        throw new IllegalArgumentException("Duplicate search: " + key);
      }
      resolved.add(found);
    }
    return resolved;
  }

  static void applyCommunityAssignment(PSSearch search, int communityId, boolean include) {
    if (search == null) {
      return;
    }
    int[] current = parseCommunityIds(search.getCXNewSearchCommunities());
    LinkedHashSet<Integer> next = new LinkedHashSet<>();
    for (int id : current) {
      next.add(id);
    }
    if (include) {
      next.add(communityId);
    } else {
      next.remove(communityId);
    }
    if (next.size() == current.length && include == containsId(current, communityId)) {
      return;
    }
    search.clearCXNewSearch();
    if (!next.isEmpty()) {
      int[] ids = new int[next.size()];
      int i = 0;
      for (Integer id : next) {
        ids[i++] = id;
      }
      search.setAsCXNewSearch(ids);
    }
  }

  static List<PSSearch> filterAssigned(List<PSSearch> catalog, int communityId) {
    List<PSSearch> out = new ArrayList<>();
    if (catalog == null) {
      return out;
    }
    String comm = String.valueOf(communityId);
    for (PSSearch search : catalog) {
      if (search != null && search.isCXNewSearch(comm)) {
        out.add(search);
      }
    }
    out.sort(
        Comparator.comparing(
            PSSearch::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  static CommunityNewSearchDefaults toWire(Community community, List<PSSearch> assigned) {
    CommunityNewSearchDefaults out = new CommunityNewSearchDefaults();
    if (community != null) {
      out.setCommunityGuid(community.getGuid());
      out.setCommunityId(community.getId());
      out.setCommunityName(community.getName());
    }
    List<CommunityNewSearchRef> refs = new ArrayList<>();
    if (assigned != null) {
      for (PSSearch search : assigned) {
        if (search != null) {
          refs.add(toRef(search));
        }
      }
    }
    out.setSearches(refs);
    return out;
  }

  static CommunityNewSearchRef toRef(PSSearch search) {
    CommunityNewSearchRef ref = new CommunityNewSearchRef();
    if (search.getGUID() != null) {
      ref.setGuid(copyGuid(search.getGUID()));
    }
    ref.setId(search.getId());
    ref.setName(search.getName());
    ref.setLabel(search.getLabel());
    return ref;
  }

  static int communityNumericId(Community community) {
    if (community.getGuid() != null && community.getGuid().getUuid() != 0) {
      return community.getGuid().getUuid();
    }
    long id = community.getId();
    if (id < Integer.MIN_VALUE || id > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("community id is out of range");
    }
    return (int) id;
  }

  static boolean isSafeCommunityKey(String key) {
    if (key == null || key.isBlank() || key.length() > MAX_COMMUNITY_KEY_LENGTH) {
      return false;
    }
    for (int i = 0; i < key.length(); i++) {
      if (Character.isISOControl(key.charAt(i))) {
        return false;
      }
    }
    return !key.contains("..") && key.indexOf('/') < 0 && key.indexOf('\\') < 0;
  }

  static int[] parseCommunityIds(String[] raw) {
    if (raw == null || raw.length == 0) {
      return new int[0];
    }
    List<Integer> ids = new ArrayList<>();
    for (String s : raw) {
      if (StringUtils.isBlank(s)) {
        continue;
      }
      String t = s.trim();
      if ("n".equalsIgnoreCase(t)) {
        continue;
      }
      if ("y".equalsIgnoreCase(t)) {
        ids.add(PSSearch.ANY_COMMUNITY_ID);
        continue;
      }
      try {
        ids.add(Integer.parseInt(t));
      } catch (NumberFormatException e) {
        // skip non-numeric property leftovers
      }
    }
    int[] out = new int[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      out[i] = ids.get(i);
    }
    return out;
  }

  private static boolean containsId(int[] ids, int communityId) {
    for (int id : ids) {
      if (id == communityId) {
        return true;
      }
    }
    return false;
  }

  private static String searchRefKey(CommunityNewSearchRef ref) {
    Guid g = ref.getGuid();
    if (g != null) {
      if (StringUtils.isNotBlank(g.getStringValue())) {
        return g.getStringValue().trim();
      }
      if (StringUtils.isNotBlank(g.getUntypedString())) {
        return g.getUntypedString().trim();
      }
      if (g.getUuid() != 0) {
        return String.valueOf(g.getUuid());
      }
    }
    if (StringUtils.isNotBlank(ref.getName())) {
      return ref.getName().trim();
    }
    if (ref.getId() != 0) {
      return String.valueOf(ref.getId());
    }
    return null;
  }

  private static Set<String> identityKeys(List<PSSearch> searches) {
    Set<String> keys = new LinkedHashSet<>();
    if (searches == null) {
      return keys;
    }
    for (PSSearch s : searches) {
      if (s != null) {
        keys.add(identityKey(s));
      }
    }
    return keys;
  }

  static String identityKey(PSSearch search) {
    if (search.getGUID() != null && StringUtils.isNotBlank(search.getGUID().toString())) {
      return search.getGUID().toString();
    }
    if (StringUtils.isNotBlank(search.getName())) {
      return "name:" + search.getName().toLowerCase();
    }
    return "id:" + search.getId();
  }

  private List<PSSearch> loadAllSearches(boolean lock) throws PSErrorException, PSErrorResultsException {
    List<IPSCatalogSummary> summaries = designWs.findSearches(null, null);
    if (summaries == null || summaries.isEmpty()) {
      return List.of();
    }
    List<IPSGuid> guids = new ArrayList<>();
    for (IPSCatalogSummary sum : summaries) {
      if (sum != null && sum.getGUID() != null) {
        guids.add(sum.getGUID());
      }
    }
    if (guids.isEmpty()) {
      return List.of();
    }
    List<PSSearch> loaded =
        designWs.loadSearches(guids, lock, false, currentSession(), currentUser());
    return loaded != null ? loaded : List.of();
  }

  private static Guid copyGuid(IPSGuid guid) {
    Guid g = new Guid();
    g.setHostId(guid.getHostId());
    g.setLongValue(guid.longValue());
    g.setStringValue(guid.toString());
    g.setType(guid.getType());
    g.setUuid(guid.getUUID());
    g.setUntypedString(guid.toStringUntyped());
    return g;
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      log.warn("IPSUserService not available; defaulting admin check to deny");
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for community new-search default write",
          Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  private static CommunityNewSearchDefaultsDesignLockException lockConflict(Throwable cause) {
    if (cause == null) {
      return new CommunityNewSearchDefaultsDesignLockException(LOCK_REQUIRED);
    }
    return new CommunityNewSearchDefaultsDesignLockException(LOCK_REQUIRED, cause);
  }
}
