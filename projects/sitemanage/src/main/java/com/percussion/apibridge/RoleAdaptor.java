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
import com.percussion.role.data.PSRole;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUserList;
import com.percussion.user.service.IPSUserService;
import com.percussion.user.service.impl.PSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
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

  static final String ADMIN_REQUIRED_CREATE = "Admin role required to create a role";

  static final String ADMIN_REQUIRED_UPDATE = "Admin role required to update a role description";

  static final String ADMIN_REQUIRED_HOMEPAGE = "Admin role required to update a role home page";

  static final String ADMIN_REQUIRED_ADD_USER = "Admin role required to add a user to a role";

  static final String ADD_USER_REQUIRED = "User name is required";

  static final String ADD_USER_ONE = "Add exactly one existing user";

  static final String ADD_USER_UNKNOWN = "Unknown user";

  static final String ADD_USER_ALREADY = "User is already a member of this role";

  static final String HOMEPAGE_INVALID = "Role home page is not a known landing page.";

  static final String ADMIN_REQUIRED_DELETE = "Admin role required to delete a role";

  static final int DESCRIPTION_MAX_LENGTH = 255;

  static final String DESCRIPTION_TOO_LONG =
      "The maximum length of a role description is 255 characters.";

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

  /** Package-visible for unit tests. {@code null} userService cannot resolve an existing user. */
  RoleAdaptor(
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

  /**
   * Admin description edit. Copies the stored users and home page so an empty wire user list
   * cannot clear membership, and never sets {@code oldName} (no rename). Missing roles are 404,
   * not creates.
   */
  @Override
  public Role updateRole(URI baseURI, Role role) {
    requireAdmin(ADMIN_REQUIRED_UPDATE);
    if (role == null || StringUtils.isBlank(role.getName())) {
      throw new WebApplicationException("Role name is required", 400);
    }
    role.setName(role.getName().trim());
    var description = normalizeDescription(role.getDescription());
    if (!roleExists(baseURI, role.getName())) {
      throw new WebApplicationException("Role not found", 404);
    }
    try {
      var existing = roleService.find(new PSStringWrapper(role.getName()));
      if (existing == null || StringUtils.isBlank(existing.getName())) {
        throw new WebApplicationException("Role not found", 404);
      }
      var toUpdate = new PSRole();
      toUpdate.setName(existing.getName());
      toUpdate.setDescription(description);
      toUpdate.setHomepage(existing.getHomepage());
      toUpdate.setUsers(existing.getUsers());
      var updated = roleService.update(toUpdate);
      var wire = ApiUtils.convertRole(updated);
      if (wire == null || StringUtils.isBlank(wire.getName())) {
        throw new WebApplicationException("Role update returned no role", 500);
      }
      return wire;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSValidationException e) {
      var status = isNotFound(e) ? 404 : 400;
      throw new WebApplicationException(validationMessage(e), status);
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Admin home-page edit. Copies the stored description and users so the wire body cannot change
   * them or rename the role. A blank {@code homePage} clears the stored value. An unknown
   * non-blank value is HTTP 400 and is not saved. Missing roles are 404, not creates.
   */
  @Override
  public Role updateRoleHomePage(URI baseURI, Role role) {
    requireAdmin(ADMIN_REQUIRED_HOMEPAGE);
    if (role == null || StringUtils.isBlank(role.getName())) {
      throw new WebApplicationException("Role name is required", 400);
    }
    role.setName(role.getName().trim());
    var homepage = normalizeHomePage(role.getHomePage());
    if (!roleExists(baseURI, role.getName())) {
      throw new WebApplicationException("Role not found", 404);
    }
    try {
      var existing = roleService.find(new PSStringWrapper(role.getName()));
      if (existing == null || StringUtils.isBlank(existing.getName())) {
        throw new WebApplicationException("Role not found", 404);
      }
      var toUpdate = new PSRole();
      toUpdate.setName(existing.getName());
      toUpdate.setDescription(existing.getDescription());
      toUpdate.setHomepage(homepage);
      toUpdate.setUsers(existing.getUsers());
      var updated = roleService.update(toUpdate);
      var wire = ApiUtils.convertRole(updated);
      if (wire == null || StringUtils.isBlank(wire.getName())) {
        throw new WebApplicationException("Role update returned no role", 500);
      }
      return wire;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSValidationException e) {
      var status = isNotFound(e) ? 404 : 400;
      throw new WebApplicationException(validationMessage(e), status);
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Admin add of one existing user. Copies the stored description and home page and appends the
   * catalog name of that user. A blank name, more than one name, or an unknown user is HTTP 400
   * and is not saved. A user who is already a member is HTTP 409 and is not saved. Missing roles
   * are 404, not creates. A client description or home page on the body is ignored.
   */
  @Override
  public Role addRoleUser(URI baseURI, Role role) {
    requireAdmin(ADMIN_REQUIRED_ADD_USER);
    if (role == null || StringUtils.isBlank(role.getName())) {
      throw new WebApplicationException("Role name is required", 400);
    }
    role.setName(role.getName().trim());
    var requested = singleUserName(role);
    if (!roleExists(baseURI, role.getName())) {
      throw new WebApplicationException("Role not found", 404);
    }
    var canonical = canonicalUserName(requested);
    try {
      var existing = roleService.find(new PSStringWrapper(role.getName()));
      if (existing == null || StringUtils.isBlank(existing.getName())) {
        throw new WebApplicationException("Role not found", 404);
      }
      var members = new ArrayList<String>();
      if (existing.getUsers() != null) {
        for (String member : existing.getUsers()) {
          if (StringUtils.isNotBlank(member)) {
            members.add(member);
          }
        }
      }
      if (containsUser(members, canonical)) {
        throw new WebApplicationException(ADD_USER_ALREADY, 409);
      }
      members.add(canonical);
      var toUpdate = new PSRole();
      toUpdate.setName(existing.getName());
      toUpdate.setDescription(existing.getDescription());
      toUpdate.setHomepage(existing.getHomepage());
      toUpdate.setUsers(members);
      var updated = roleService.update(toUpdate);
      var wire = ApiUtils.convertRole(updated);
      if (wire == null || StringUtils.isBlank(wire.getName())) {
        throw new WebApplicationException("Role update returned no role", 500);
      }
      return wire;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSValidationException e) {
      var status = isNotFound(e) ? 404 : 400;
      throw new WebApplicationException(validationMessage(e), status);
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Exactly one non-blank user name. A missing or blank list is HTTP 400. More than one non-blank
   * name is HTTP 400 so this write cannot replace the member list.
   */
  private static String singleUserName(Role role) {
    var names = new ArrayList<String>();
    if (role.getUsers() != null) {
      for (String user : role.getUsers()) {
        if (StringUtils.isNotBlank(user)) {
          names.add(user.trim());
        }
      }
    }
    if (names.isEmpty()) {
      throw new WebApplicationException(ADD_USER_REQUIRED, 400);
    }
    if (names.size() != 1) {
      throw new WebApplicationException(ADD_USER_ONE, 400);
    }
    return names.get(0);
  }

  /** Catalog spelling of {@code requested}. Unknown names are HTTP 400 and are not saved. */
  private String canonicalUserName(String requested) {
    if (userService == null) {
      throw new WebApplicationException("User service is not available", 500);
    }
    PSUserList catalog;
    try {
      catalog = userService.getUsers();
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
    if (catalog == null || catalog.getUsers() == null) {
      throw new WebApplicationException(ADD_USER_UNKNOWN, 400);
    }
    for (String known : catalog.getUsers()) {
      if (known != null && known.trim().equalsIgnoreCase(requested)) {
        return known.trim();
      }
    }
    throw new WebApplicationException(ADD_USER_UNKNOWN, 400);
  }

  private static boolean containsUser(List<String> members, String userName) {
    for (String member : members) {
      if (member != null && member.equalsIgnoreCase(userName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Blank or whitespace clears (returns {@code null}). A known type or alias is canonical. Anything
   * else is HTTP 400.
   */
  private static String normalizeHomePage(String homePage) {
    if (StringUtils.isBlank(homePage)) {
      return null;
    }
    var normalized = PSUserService.normalizeHomepageType(homePage);
    if (normalized == null) {
      throw new WebApplicationException(HOMEPAGE_INVALID, 400);
    }
    return normalized;
  }

  /** Trim; blank becomes null (clear). Longer than {@link #DESCRIPTION_MAX_LENGTH} is HTTP 400. */
  private static String normalizeDescription(String description) {
    if (description == null) {
      return null;
    }
    var trimmed = description.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.length() > DESCRIPTION_MAX_LENGTH) {
      throw new WebApplicationException(DESCRIPTION_TOO_LONG, 400);
    }
    return trimmed;
  }

  private static boolean isNotFound(PSValidationException e) {
    var message = e.getMessage();
    return message != null && message.toLowerCase(Locale.ROOT).contains("not found");
  }

  /**
   * Admin create. Blank names and role-service validation failures are HTTP 400. Non-admin is 403.
   */
  @Override
  public Role createRole(URI baseURI, Role role) throws BackendException {
    requireAdmin(ADMIN_REQUIRED_CREATE);
    if (role == null || StringUtils.isBlank(role.getName())) {
      throw new WebApplicationException("Role name is required", 400);
    }
    role.setName(role.getName().trim());
    if (role.getDescription() != null) {
      var description = role.getDescription().trim();
      role.setDescription(description.isEmpty() ? null : description);
    }
    try {
      var created = roleService.create(ApiUtils.convertRole(role));
      var wire = ApiUtils.convertRole(created);
      if (wire == null || StringUtils.isBlank(wire.getName())) {
        throw new WebApplicationException("Role create returned no role", 500);
      }
      return wire;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSValidationException e) {
      throw new WebApplicationException(validationMessage(e), 400);
    } catch (PSDataServiceException e) {
      throw new BackendException(e);
    }
  }

  /** Exact catalog name match. Blank is not defined. Catalog failures are HTTP 500. */
  @Override
  public boolean roleExists(URI baseUri, String roleName) {
    if (StringUtils.isBlank(roleName)) {
      return false;
    }
    if (securityDesignWs == null) {
      throw new WebApplicationException("Security design service is not available", 500);
    }
    var name = roleName.trim();
    List<IPSCatalogSummary> found;
    try {
      found = securityDesignWs.findRoles(name);
    } catch (RuntimeException e) {
      log.error("Failed to look up role '{}'", name, e);
      throw new WebApplicationException(e, 500);
    }
    if (found == null) {
      return false;
    }
    for (IPSCatalogSummary summary : found) {
      if (summary != null && name.equals(summary.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Admin delete of one CMS role. System roles ({@code System}, {@code Default}) are 400.
   * A role that would strand users or that a workflow still assigns is 409 and is not deleted.
   * Directory groups lose only the CMS link — {@link PSRoleService#delete} does not remove the
   * remote directory group.
   */
  @Override
  public void deleteRole(URI baseURI, String roleName) throws BackendException {
    requireAdmin(ADMIN_REQUIRED_DELETE);
    if (StringUtils.isBlank(roleName)) {
      throw new WebApplicationException("Role name is required", 400);
    }
    var name = roleName.trim();
    if (isSystemRoleName(name)) {
      throw new WebApplicationException("Cannot delete system role", 400);
    }
    if (!roleExists(baseURI, name)) {
      throw new WebApplicationException("Role not found", 404);
    }
    try {
      var existing = roleService.find(new PSStringWrapper(name));
      if (existing == null || StringUtils.isBlank(existing.getName())) {
        throw new WebApplicationException("Role not found", 404);
      }
      roleService.validateForDelete(existing);
      roleService.delete(new PSStringWrapper(existing.getName()));
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSValidationException e) {
      throw new WebApplicationException(validationMessage(e), deleteFailureStatus(e));
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  /** {@link PSRoleService#SYSTEM_ROLES} ({@code System} and {@code Default}), case-insensitive. */
  static boolean isSystemRoleName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    for (String system : PSRoleService.SYSTEM_ROLES) {
      if (system != null && system.equalsIgnoreCase(name.trim())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Map role-service validation from delete. Missing roles are 404. Restricted system names are
   * 400. In-use workflows and users who would be unable to log in are 409.
   */
  static int deleteFailureStatus(PSValidationException e) {
    var message = validationMessage(e).toLowerCase(Locale.ROOT);
    if (message.contains("not found")) {
      return 404;
    }
    if (message.contains("cannot delete system role") || message.contains("system use")) {
      return 400;
    }
    return 409;
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
    Map<String, String> homePages = new HashMap<>();
    Map<Long, String> roleIdToName = new HashMap<>();
    List<String> roleNames = new ArrayList<>();
    if (roleSummaries != null) {
      for (IPSCatalogSummary summary : roleSummaries) {
        if (summary == null || StringUtils.isBlank(summary.getName())) {
          continue;
        }
        String name = summary.getName();
        roleNames.add(name);
        // Design-object summaries do not store a description (PSRole#getDescription is
        // always null). Create and update persist the text on the backend role.
        // Home page is metadata, not a catalog-summary field.
        StoredRoleFields stored = storedRoleFields(name);
        String description = summary.getDescription();
        if (StringUtils.isBlank(description)) {
          description = stored.description();
        }
        if (StringUtils.isNotBlank(description)) {
          descriptions.put(name, description.trim());
        }
        if (StringUtils.isNotBlank(stored.homePage())) {
          homePages.put(name, stored.homePage().trim());
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
      entry.setHomePage(homePages.get(name));
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

  /** Description and home page from the backend role, or blanks when the lookup fails. */
  private record StoredRoleFields(String description, String homePage) {
    private static StoredRoleFields empty() {
      return new StoredRoleFields(null, null);
    }
  }

  /**
   * Backend role text for a catalog name. A missing role or a service failure leaves the catalog
   * row in place with no description or home page rather than failing the browse.
   */
  private StoredRoleFields storedRoleFields(String name) {
    if (roleService == null || StringUtils.isBlank(name)) {
      return StoredRoleFields.empty();
    }
    try {
      var found = roleService.find(new PSStringWrapper(name));
      if (found == null) {
        return StoredRoleFields.empty();
      }
      return new StoredRoleFields(found.getDescription(), found.getHomepage());
    } catch (PSDataServiceException | RuntimeException e) {
      log.debug("Role '{}' has no stored description or home page: {}", name, e.getMessage());
      return StoredRoleFields.empty();
    }
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
    requireAdmin(ADMIN_REQUIRED);
  }

  private void requireAdmin(String message) {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(message, Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(message, Response.Status.FORBIDDEN);
    }
  }

  private static String validationMessage(PSValidationException e) {
    if (e != null && StringUtils.isNotBlank(e.getMessage())) {
      return e.getMessage();
    }
    return "Role is not valid";
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
