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

// REFACTORED: CP-JAVA11

package com.percussion.apibridge;

import com.percussion.itemmanagement.service.impl.PSWorkflowHelper;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.roles.IRoleAdaptor;
import com.percussion.rest.roles.Role;
import com.percussion.rest.roles.RoleBrowseCatalog;
import com.percussion.rest.roles.RoleBrowseEntry;
import com.percussion.rest.roles.RoleBrowseGroup;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** Adaptor for managing roles in Percussion CMS. */
@PSSiteManageBean
@Lazy
public class RoleAdaptor implements IRoleAdaptor {

  private static final Logger log = LogManager.getLogger(RoleAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to browse the roles catalog";

  private final PSRoleService roleService;
  private final IPSSecurityDesignWs securityDesignWs;
  private final IPSWorkflowService workflowService;
  private final BooleanSupplier adminChecker;
  private final IPSUserService userService;

  /** Production constructor. */
  @Autowired
  public RoleAdaptor(
      PSRoleService roleService,
      IPSSecurityDesignWs securityDesignWs,
      IPSWorkflowService workflowService,
      IPSUserService userService) {
    this(roleService, securityDesignWs, workflowService, null, userService);
  }

  /** Package-visible for unit tests. {@code null} adminChecker uses {@link #isCurrentUserAdmin()}. */
  RoleAdaptor(
      PSRoleService roleService,
      IPSSecurityDesignWs securityDesignWs,
      IPSWorkflowService workflowService,
      BooleanSupplier adminChecker) {
    this(roleService, securityDesignWs, workflowService, adminChecker, null);
  }

  private RoleAdaptor(
      PSRoleService roleService,
      IPSSecurityDesignWs securityDesignWs,
      IPSWorkflowService workflowService,
      BooleanSupplier adminChecker,
      IPSUserService userService) {
    this.roleService = roleService;
    this.securityDesignWs = securityDesignWs;
    this.workflowService = workflowService;
    this.userService = userService;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public Role getRole(URI baseURI, String roleName) throws BackendException {
    try {
      var wrap = new PSStringWrapper();
      wrap.setValue(roleName);
      var pRole = roleService.find(wrap);
      return ApiUtils.convertRole(pRole);
    } catch (PSDataServiceException e) {
      throw new BackendException(e);
    }
  }

  @Override
  public Role updateRole(URI baseURI, Role role) {
    try {
      return ApiUtils.convertRole(roleService.update(ApiUtils.convertRole(role)));
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  @Override
  public Role createRole(URI baseURI, Role role) throws BackendException {
    try {
      return ApiUtils.convertRole(roleService.create(ApiUtils.convertRole(role)));
    } catch (PSDataServiceException e) {
      throw new BackendException(e);
    }
  }

  @Override
  public void deleteRole(URI baseURI, String roleName) throws BackendException {
    try {
      var wrap = new PSStringWrapper(roleName);
      roleService.delete(wrap);
    } catch (PSDataServiceException e) {
      throw new BackendException(e);
    }
  }

  @Override
  public List<Role> findRoles(URI baseURI, String pattern) throws BackendException {
    var roleList = roleService.getRoleMgr().getDefinedRoles();
    return roleList.stream()
        .map(
            s -> {
              try {
                return ApiUtils.convertRole(roleService.find(new PSStringWrapper(s)));
              } catch (PSDataServiceException e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public RoleBrowseCatalog browseRoles(URI baseUri, String groupFilter) {
    requireAdmin();
    RoleBrowseGroup filter = RoleBrowseGroup.fromWire(groupFilter);

    List<IPSCatalogSummary> roleSummaries = securityDesignWs.findRoles(null);
    Map<String, String> descriptions = new HashMap<>();
    Map<Long, String> roleIdToName = new HashMap<>();
    List<String> roleNames = new ArrayList<>();
    if (roleSummaries != null) {
      for (IPSCatalogSummary summary : roleSummaries) {
        if (summary == null || StringUtils.isBlank(summary.getName())) {
          continue;
        }
        String name = summary.getName();
        roleNames.add(name);
        if (StringUtils.isNotBlank(summary.getDescription())) {
          descriptions.put(name, summary.getDescription());
        }
        if (summary.getGUID() != null) {
          roleIdToName.put(summary.getGUID().longValue(), name);
        }
      }
    }

    Map<String, Set<String>> communitiesByRole = loadCommunityMembership(roleIdToName);
    Map<String, Set<String>> workflowsByRole = loadWorkflowMembership();

    List<RoleBrowseEntry> entries = new ArrayList<>();
    for (String name : roleNames) {
      Set<String> communities =
          communitiesByRole.getOrDefault(name, Set.of()).stream()
              .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
      Set<String> workflows =
          workflowsByRole.getOrDefault(name, Set.of()).stream()
              .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

      List<String> groups = new ArrayList<>();
      if (!communities.isEmpty()) {
        groups.add(RoleBrowseGroup.COMMUNITY.getWireValue());
      }
      if (!workflows.isEmpty()) {
        groups.add(RoleBrowseGroup.WORKFLOW.getWireValue());
      }
      if (groups.isEmpty()) {
        groups.add(RoleBrowseGroup.UNASSIGNED.getWireValue());
      }

      if (filter != null && !groups.contains(filter.getWireValue())) {
        continue;
      }

      RoleBrowseEntry entry = new RoleBrowseEntry();
      entry.setName(name);
      entry.setDescription(descriptions.get(name));
      entry.setGroups(groups);
      entry.setCommunities(new ArrayList<>(communities));
      entry.setWorkflows(new ArrayList<>(workflows));
      entries.add(entry);
    }

    entries.sort(
        Comparator.comparing(
            e -> e.getName() == null ? "" : e.getName(), String.CASE_INSENSITIVE_ORDER));

    RoleBrowseCatalog catalog = new RoleBrowseCatalog(entries);
    if (filter != null) {
      catalog.setGroup(filter.getWireValue());
    }
    return catalog;
  }

  /**
   * Build role→community membership via Security Design WS. There is no narrower
   * {@code findCommunityRoleAssociations} projection on {@code IPSSecurityDesignWs};
   * {@code findCommunities(null)} + read-only {@code loadCommunities} is the established
   * API to reach each community's {@code roleAssociations} (same shape Workbench uses).
   */
  private Map<String, Set<String>> loadCommunityMembership(Map<Long, String> roleIdToName) {
    Map<String, Set<String>> out = new HashMap<>();
    List<IPSCatalogSummary> communities = securityDesignWs.findCommunities(null);
    if (communities == null || communities.isEmpty()) {
      return out;
    }
    List<IPSGuid> ids = new ArrayList<>();
    Map<Long, String> communityNames = new HashMap<>();
    for (IPSCatalogSummary summary : communities) {
      if (summary == null || summary.getGUID() == null) {
        continue;
      }
      ids.add(summary.getGUID());
      communityNames.put(summary.getGUID().longValue(), summary.getName());
    }
    if (ids.isEmpty()) {
      return out;
    }

    String session = currentSession();
    String user = currentUser();
    List<PSCommunity> loaded;
    try {
      // Read-only catalog — no design lock.
      loaded = securityDesignWs.loadCommunities(ids, false, false, session, user);
    } catch (PSErrorResultsException e) {
      log.warn("Partial community load while building roles browse catalog: {}", e.getMessage());
      loaded = partialCommunities(e, ids);
    } catch (RuntimeException e) {
      log.error("Failed to load communities for roles browse catalog", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    if (loaded == null) {
      return out;
    }
    for (PSCommunity community : loaded) {
      if (community == null) {
        continue;
      }
      String communityName =
          StringUtils.isNotBlank(community.getName())
              ? community.getName()
              : communityNames.get(community.getGUID() != null ? community.getGUID().longValue() : -1L);
      if (StringUtils.isBlank(communityName) || community.getRoleAssociations() == null) {
        continue;
      }
      for (IPSGuid roleGuid : community.getRoleAssociations()) {
        if (roleGuid == null) {
          continue;
        }
        String roleName = roleIdToName.get(roleGuid.longValue());
        if (StringUtils.isBlank(roleName)) {
          continue;
        }
        out.computeIfAbsent(roleName, k -> new LinkedHashSet<>()).add(communityName);
      }
    }
    return out;
  }

  private List<PSCommunity> partialCommunities(PSErrorResultsException e, List<IPSGuid> ids) {
    List<PSCommunity> partial = new ArrayList<>();
    for (IPSGuid id : ids) {
      try {
        Object result = e.getResults().get(id);
        if (result instanceof PSCommunity community) {
          partial.add(community);
        }
      } catch (RuntimeException ignored) {
        log.debug("Skipping community {} during partial load: {}", id, ignored.getMessage());
      }
    }
    return partial;
  }

  private Map<String, Set<String>> loadWorkflowMembership() {
    Map<String, Set<String>> out = new HashMap<>();
    List<PSWorkflow> workflows = workflowService.findWorkflowsByName(null);
    if (workflows == null) {
      return out;
    }
    for (PSWorkflow workflow : workflows) {
      if (workflow == null || StringUtils.isBlank(workflow.getName())) {
        continue;
      }
      if (PSWorkflowHelper.LOCAL_WORKFLOW_NAME.equals(workflow.getName())) {
        continue;
      }
      List<PSWorkflowRole> wfRoles = workflow.getRoles();
      if (wfRoles == null) {
        continue;
      }
      for (PSWorkflowRole wfRole : wfRoles) {
        if (wfRole == null || StringUtils.isBlank(wfRole.getName())) {
          continue;
        }
        out.computeIfAbsent(wfRole.getName(), k -> new LinkedHashSet<>()).add(workflow.getName());
      }
    }
    return out;
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

  private static String currentSession() {
    Object session = PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    return session == null ? null : session.toString();
  }

  private static String currentUser() {
    Object user = PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
    return user == null ? null : user.toString();
  }
}
