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
package com.percussion.role.service.impl;

import static org.springframework.util.StringUtils.trimWhitespace;

import com.percussion.itemmanagement.service.impl.PSWorkflowHelper;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.role.data.PSRole;
import com.percussion.role.service.IPSRoleService;
import com.percussion.security.IPSTypedPrincipal;
import com.percussion.security.PSSecurityCatalogException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.service.PSCollectionUtils;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSAbstractBeanValidator;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.user.data.PSUserList;
import com.percussion.user.service.IPSUserService;
import com.percussion.user.service.impl.PSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.service.impl.PSBackEndRoleManagerFacade;
import com.percussion.utils.string.PSStringUtils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** See the interface for documentation. */
@Path("/role")
@Component("roleService")
@Lazy
public class PSRoleService implements IPSRoleService {

  private static final Logger log = LogManager.getLogger(PSRoleService.class);

  private IPSUserService userService;
  private PSBackEndRoleManagerFacade backEndRoleMgr;
  private IPSWorkflowService wfService;
  private IPSMetadataService mdService;
  private IPSRoleMgr roleMgr;

  public static final List<String> SYSTEM_ROLES = List.of("System", "Default");
  protected static final String CONTRIBUTOR_ROLE = "Contributor";
  public static final List<String> DEFAULT_ROLES = List.of("Default");
  public static final List<String> DEFAULT_IMPORTED_USER_ROLES =
      List.of("Default", CONTRIBUTOR_ROLE);

  @Autowired
  public PSRoleService(
      IPSUserService userService,
      IPSBackEndRoleMgr backEndRoleMgr,
      IPSWorkflowService wfService,
      IPSMetadataService mdService,
      IPSRoleMgr roleMgr) {
    this.userService = userService;
    this.backEndRoleMgr = new PSBackEndRoleManagerFacade(backEndRoleMgr);
    this.wfService = wfService;
    this.mdService = mdService;
    this.roleMgr = roleMgr;
  }

  @POST
  @Path("/create")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRole create(PSRole role) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfNull("create", "role", role);

    var roleName = trimWhitespace(role.getName());
    role.setName(roleName);

    doValidation(role, true);

    backEndRoleMgr.createRole(role.getName(), role.getDescription());
    setHomepage(role.getName(), role.getHomepage());
    wfService.addWorkflowRole(null, roleName);

    try {
      // codeql[java/xss] justification: JSON/XML DTO via Jackson/JAXB; client HTML-encodes before DOM insert (alert #749)
      return (!role.getUsers().isEmpty()) ? update(role) : role.clone();
    } catch (CloneNotSupportedException e) {
      throw new PSDataServiceException(e);
    }
  }

  @POST
  @Path("/delete")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void delete(PSStringWrapper strWrapper) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfNull("delete", "strWrapper", strWrapper);

    var name = strWrapper.getValue();

    checkRole(name);
    if (PSCollectionUtils.containsIgnoringCase(SYSTEM_ROLES, name)) {
      PSParameterValidationUtils.validateParameters("delete")
          .rejectField("name", "Cannot delete system role", name)
          .throwIfInvalid();
    }

    removeUsersFromRole(name, find(name).getUsers());

    backEndRoleMgr.deleteRole(name);
    mdService.delete(META_DATA_HOMEPAGE_PREFIX + name);
    wfService.removeWorkflowRole(null, name);
  }

  @POST
  @Path("/find")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRole find(PSStringWrapper strWrapper) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfNull("find", "strWrapper", strWrapper);
    return find(strWrapper.getValue());
  }

  private PSRole find(String name) throws PSDataServiceException {
    checkRole(name);

    var existingRole = new PSRole();
    var beRole = backEndRoleMgr.getRole(name);

    existingRole.setName(name);
    existingRole.setDescription(beRole.getDescription());
    existingRole.setUsers(getUsers(name));
    existingRole.setHomepage(getHomepage(name));
    return existingRole;
  }

  private List<String> getUsers(String name) throws PSDataServiceException {
    try {
      var users = roleMgr.getRoleMembers(name);
      var userNames =
          users.stream()
              .map(IPSTypedPrincipal::getName)
              .filter(u -> !PSUserService.SYSTEM_USERS.contains(u))
              .sorted(Collator.getInstance())
              .collect(Collectors.toList());
      return userNames;
    } catch (PSSecurityCatalogException e) {
      throw new PSDataServiceException(e);
    }
  }

  @POST
  @Path("/update")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRole update(PSRole role) throws PSDataServiceException {
    var oldRoleName = role.getOldName();

    if (oldRoleName != null && !oldRoleName.equals(role.getName())) {
      checkRole(oldRoleName);
      role.setOldName(null);
      create(role);
      wfService.copyWorkflowToRole(oldRoleName, (String) role.getName());
      delete(new PSStringWrapper(oldRoleName));
    }
    PSParameterValidationUtils.rejectIfNull("update", "role", role);

    var name = role.getName();
    checkRole(name);

    doValidation(role, false);

    var beRole = backEndRoleMgr.update(name, role.getDescription());
    setHomepage(role.getName(), role.getHomepage());

    var users = new ArrayList<>(role.getUsers());
    var existingRole = find(role.getName());
    var existingUsers = new ArrayList<>(existingRole.getUsers());

    if (!users.equals(existingUsers)) {
      var toAdd =
          users.stream().filter(u -> !existingUsers.contains(u)).collect(Collectors.toList());
      if (!toAdd.isEmpty()) {
        addUsersToRole(name, toAdd);
      }
      var toRemove =
          existingUsers.stream().filter(u -> !users.contains(u)).collect(Collectors.toList());
      if (!toRemove.isEmpty()) {
        removeUsersFromRole(name, toRemove);
      }
    }

    var updatedRole = new PSRole();
    updatedRole.setName(name);
    updatedRole.setDescription(beRole.getDescription());
    updatedRole.setUsers(userService.getUsersByRole(name).getUsers());
    updatedRole.setHomepage(role.getHomepage());
    return updatedRole;
  }

  @POST
  @Path("/availableUsers")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserList getAvailableUsers(PSRole role) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfNull("getAvailableUsers", "role", role);

    var users = userService.getUsers().getUsers();
    var availableUsers =
        users.stream().filter(u -> !role.getUsers().contains(u)).collect(Collectors.toList());

    var availableList = new PSUserList();
    availableList.setUsers(availableUsers);
    return availableList;
  }

  @POST
  @Path("/validateForDelete")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void validateForDelete(PSRole role) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfNull("validateForDelete", "role", role);

    var name = role.getName();
    checkRole(name);

    var emsg = new StringBuilder();
    var singleRoleUsers = getSingleRoleUsers(role);
    if (!singleRoleUsers.isEmpty()) {
      emsg.append("The following users will be unable to login if role '")
          .append(name)
          .append("' is deleted: '")
          .append(PSStringUtils.listToString(singleRoleUsers, "', '"))
          .append("'.");
    }

    var inUseWorkflows = getInUseWorkflows(role);
    if (!inUseWorkflows.isEmpty()) {
      if (emsg.length() > 0) {
        emsg.append("<br><br>");
      }
      emsg.append("Role '")
          .append(name)
          .append("' is used by the following workflows: '")
          .append(PSStringUtils.listToString(inUseWorkflows, "', '"))
          .append("'.");
    }

    if (emsg.length() > 0) {
      var builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
      builder.reject("validate.role.delete", emsg.toString()).throwIfInvalid();
    }
  }

  @POST
  @Path("/validateDeleteUsers")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void validateDeleteUsersFromRole(PSUserList userList) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfNull("validateDeleteUsers", "userList", userList);

    var singleRoleUsers = getSingleRoleUsers(userList.getUsers());
    if (!singleRoleUsers.isEmpty()) {
      var emsg =
          "The following users will be unable to login if they are removed from this role: "
              + PSStringUtils.listToString(singleRoleUsers, ", ")
              + '.';

      var builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
      builder.reject("validate.delete.users", emsg).throwIfInvalid();
    }
  }

  protected void doValidation(PSRole role, boolean isCreateRole) throws PSValidationException {
    log.debug("validating role {}", role);
    if (isCreateRole) {
      var validator = new PSRoleValidator(isCreateRole);
      validator.validate(role).throwIfInvalid();
    }
  }

  protected void checkRole(String name) throws PSValidationException {
    PSParameterValidationUtils.rejectIfBlank("checkRole", "name", name);

    if (!backEndRoleMgr.getRoles().contains(name)) {
      var emsg = "Role not found " + name;
      log.error(emsg);
      var builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
      builder.reject("no.such.role", emsg).throwIfInvalid();
    }
  }

  protected void checkNewRole(String name) throws PSValidationException {
    PSParameterValidationUtils.rejectIfBlank("checkRole", "name", name);

    if (backEndRoleMgr.getRoles().contains(name)) {
      var emsg = "Role " + name + " already exists";
      log.error(emsg);
      var builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
      builder.reject("no.such.role", emsg).throwIfInvalid();
    }
  }

  protected class PSRoleValidator extends PSAbstractBeanValidator<PSRole> {
    boolean isCreateRole = false;

    public PSRoleValidator(boolean isCreate) {
      this.isCreateRole = isCreate;
    }

    @Override
    protected void doValidation(PSRole role, PSBeanValidationException e) {
      List<String> allUsers = new ArrayList<>();
      try {
        allUsers = userService.getUsers().getUsers();
      } catch (PSDataServiceException psDataServiceException) {
        log.error("Error listing system users. Error: {}", PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        e.addSuppressed(e);
      }
      if (PSCollectionUtils.containsIgnoringCase(SYSTEM_ROLES, role.getName())) {
        e.rejectValue(
            "name",
            "role.nameRestricted",
            "That role name is restricted for system use. Please choose a different role name.");
      }
      if (e.hasErrors()) {
        return;
      }
      for (var rl : role.getUsers()) {
        if (!PSCollectionUtils.containsIgnoringCase(allUsers, rl)) {
          var msg =
              "Cannot add user \"" + rl + "\" because user named \"" + rl + "\" does not exist.";
          e.rejectValue("users", "no.such.user", msg);
        }
      }
      if (e.hasErrors()) {
        return;
      }
      if (isCreateRole) {
        var newName = role.getName();
        var beRole = backEndRoleMgr.getRole(newName);
        if (beRole != null) {
          var errorMsg2 = "already_exist:" + beRole.getName();
          log.debug(
              "Cannot create role \"{}\" because a role named \"{}\" already exists.",
              newName,
              beRole.getName());
          e.rejectValue("name", "not.create.existing.role", errorMsg2);
        }
      } else {
        try {
          cannotRemoveYourselfFromAdminRole(role, e);
        } catch (PSDataServiceException psDataServiceException) {
          e.addSuppressed(psDataServiceException);
          log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
      }
    }

    private void cannotRemoveYourselfFromAdminRole(PSRole role, PSBeanValidationException e)
        throws PSDataServiceException {
      if (!role.getName().equals("Admin")) {
        return;
      }
      if (!role.getUsers().contains(userService.getCurrentUser().getName())) {
        var emsg = "Cannot remove yourself from \"Admin\" role.";
        log.debug(emsg);
        e.rejectValue("users", "cannot.remove.user.admin.role", emsg);
      }
    }
  }

  private List<String> getSingleRoleUsers(PSRole role) {
    return getSingleRoleUsers(role.getUsers());
  }

  private void setHomepage(String roleName, String homepage)
      throws IPSGenericDao.LoadException, IPSGenericDao.SaveException {
    if (StringUtils.isBlank(roleName)) {
      throw new IllegalArgumentException("roleName must not be blank");
    }
    if (StringUtils.isBlank(homepage)
        || !(homepage.equals(HOMEPAGE_TYPE_DASHBOARD)
            || homepage.equals(HOMEPAGE_TYPE_EDITOR)
            || homepage.equals(HOMEPAGE_TYPE_HOME))) {
      homepage = HOMEPAGE_TYPE_DASHBOARD;
    }
    var key = META_DATA_HOMEPAGE_PREFIX + roleName;
    var md = mdService.find(key);
    if (md == null) {
      md = new PSMetadata(key, homepage);
    } else {
      md.setData(homepage);
    }
    mdService.save(md);
  }

  private String getHomepage(String roleName) throws IPSGenericDao.LoadException {
    if (StringUtils.isBlank(roleName)) {
      throw new IllegalArgumentException("roleName must not be blank");
    }
    var key = META_DATA_HOMEPAGE_PREFIX + roleName;
    var md = mdService.find(key);
    return (md == null ? HOMEPAGE_TYPE_DASHBOARD : md.getData());
  }

  private List<String> getSingleRoleUsers(List<String> users) {
    return users.stream()
        .filter(
            user -> {
              try {
                return backEndRoleMgr.getRoles(user).size() == 1;
              } catch (Exception e) {
                log.warn("Failed to get roles for user '{}'.", user);
                return false;
              }
            })
        .collect(Collectors.toList());
  }

  private List<String> getInUseWorkflows(PSRole role) {
    var inUseWorkflows = new ArrayList<String>();
    var name = role.getName();
    var workflows = wfService.findWorkflowsByName("");
    for (var workflow : workflows) {
      var wfName = workflow.getName();
      if (wfName.equals(PSWorkflowHelper.LOCAL_WORKFLOW_NAME)) {
        continue;
      }
      boolean roleInUse = false;
      for (var wfRole : workflow.getRoles()) {
        if (wfRole.getName().equals(name)) {
          IPSGuid id = wfRole.getGUID();
          for (var state : workflow.getStates()) {
            for (var asRole : state.getAssignedRoles()) {
              if (asRole.getGUID().equals(id)) {
                var asType = asRole.getAssignmentType();
                if (asType != PSAssignmentTypeEnum.NONE && asType != PSAssignmentTypeEnum.READER) {
                  inUseWorkflows.add(wfName);
                  roleInUse = true;
                  break;
                }
              }
            }
            if (roleInUse) break;
          }
          break;
        }
      }
    }
    return inUseWorkflows;
  }

  private void addUsersToRole(String roleName, List<String> users) {
    for (var user : users) {
      var roles = backEndRoleMgr.getRoles(user);
      roles.add(roleName);
      backEndRoleMgr.setRoles(user, roles);
    }
  }

  private void removeUsersFromRole(String roleName, List<String> users) {
    for (var user : users) {
      var roles = backEndRoleMgr.getRoles(user);
      roles.remove(roleName);
      backEndRoleMgr.setRoles(user, roles);
    }
  }

  @Override
  @GET
  @Path("/userhomepage")
  @Produces(MediaType.TEXT_PLAIN)
  public String getUserHomepage() throws IPSGenericDao.LoadException {
    List<String> userRoles = null;
    try {
      userRoles = userService.getCurrentUser().getRoles();
    } catch (IPSUserService.PSNoCurrentUserException e) {
      log.error(
          "Error getting roles, No Current User! Error:{}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (PSDataServiceException e) {
      log.error(
          "Error getting roles for current user: {} Error:", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    Set<String> userHomePages = new HashSet<>();
    String homepage = null;
    if (userRoles != null) {
      for (var role : userRoles) {
        userHomePages.add(getHomepage(role));
      }
    }
    if (userHomePages.isEmpty() || userHomePages.contains(HOMEPAGE_TYPE_DASHBOARD)) {
      homepage = HOMEPAGE_TYPE_DASHBOARD;
    } else if (userHomePages.contains(HOMEPAGE_TYPE_EDITOR)) {
      homepage = HOMEPAGE_TYPE_EDITOR;
    } else {
      homepage = HOMEPAGE_TYPE_HOME;
    }
    return homepage;
  }

  public IPSRoleMgr getRoleMgr() {
    return roleMgr;
  }

  public void setRoleMgr(IPSRoleMgr roleMgr) {
    this.roleMgr = roleMgr;
  }
}
