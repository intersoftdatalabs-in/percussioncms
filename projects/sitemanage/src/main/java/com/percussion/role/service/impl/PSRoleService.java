/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.error.PSExceptionUtils;
import com.percussion.itemmanagement.service.impl.PSWorkflowHelper;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.role.data.PSRole;
import com.percussion.role.service.IPSRoleService;
import com.percussion.security.IPSTypedPrincipal;
import com.percussion.security.PSSecurityCatalogException;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.security.data.PSBackEndRole;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
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
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.springframework.util.StringUtils.trimWhitespace;

/**
 * 
 * See the interface for documentation.
 */
@Path("/role")
@Component("roleService")
@Lazy
public class PSRoleService implements IPSRoleService
{
    private static final Logger log = LogManager.getLogger(PSRoleService.class);

    private IPSUserService userService;
    private PSBackEndRoleManagerFacade backEndRoleMgr;
    private IPSWorkflowService wfService;
    private IPSMetadataService mdService;
    private IPSRoleMgr roleMgr;
    
    /**
     * A list of pre-defined roles by the system, which should not be exposed to
     * end user.
     */
    public static final List<String> SYSTEM_ROLES = new ArrayList<>();
    static
    {
        SYSTEM_ROLES.add("System");
        SYSTEM_ROLES.add("Default");
    }

    /**
     * Contributor role.
     */
    protected  static final String CONTRIBUTOR_ROLE = "Contributor";

    /**
     * The default roles that are always added to the user.
     */
    public  static final List<String> DEFAULT_ROLES = asList("Default");

    /**
     * Default imported user roles. The roles set to an imported user.
     */
    public static final List<String> DEFAULT_IMPORTED_USER_ROLES = new ArrayList<>();
    static
    {
        DEFAULT_IMPORTED_USER_ROLES.addAll(DEFAULT_ROLES);
        DEFAULT_IMPORTED_USER_ROLES.add(CONTRIBUTOR_ROLE);
    }

    @Autowired
    public PSRoleService(IPSUserService userService, IPSBackEndRoleMgr backEndRoleMgr, IPSWorkflowService wfService, IPSMetadataService mdService, IPSRoleMgr roleMgr)
    {
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
    public PSRole create(PSRole role) throws PSDataServiceException
    {
        PSParameterValidationUtils.rejectIfNull("create", "role", role);
        
        String roleName = trimWhitespace(role.getName());
        role.setName(roleName);
        
        doValidation(role, true);

        backEndRoleMgr.createRole(role.getName(), role.getDescription());
        setHomepage(role.getName(), role.getHomepage());
        wfService.addWorkflowRole(null, roleName);

        try {
            return (!role.getUsers().isEmpty()) ? update(role) : role.clone();
        } catch (CloneNotSupportedException e) {
            throw new PSDataServiceException(e);
        }
    }

    @POST
    @Path("/delete")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void delete(PSStringWrapper strWrapper) throws PSDataServiceException
    {
        PSParameterValidationUtils.rejectIfNull("delete", "strWrapper", strWrapper);
        
        String name = strWrapper.getValue();
        
        checkRole(name);
        if (PSCollectionUtils.containsIgnoringCase(SYSTEM_ROLES, name)) {
            PSParameterValidationUtils.validateParameters("delete").rejectField("name", "Cannot delete system role",
                    name).throwIfInvalid();
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
    public PSRole find(PSStringWrapper strWrapper) throws PSDataServiceException
    {
        PSParameterValidationUtils.rejectIfNull("find", "strWrapper", strWrapper);
        
        return find(strWrapper.getValue());
    }

    /**
     * Finds the role identified by the specified name.
     * 
     * @param name assumed not <code>null</code>.
     * @return the role, never <code>null</code>.
     * @throws PSDataServiceException if the role cannot be found.
     */
    private PSRole find(String name) throws PSDataServiceException
    {
        checkRole(name);

        PSRole existingRole = new PSRole();

        PSBackEndRole beRole = backEndRoleMgr.getRole(name);

        existingRole.setName(name);
        existingRole.setDescription(beRole.getDescription());
        List<String> userNames = getUsers(name);
        existingRole.setUsers(userNames);
        existingRole.setHomepage(getHomepage(name));
        return existingRole;
    }

    /**
     * Returns the users for a supplied role, never <code>null</code> may be empty.
     * @param name role name assumed not <code>null</code>
     * @return list of users.
     */
    private List<String> getUsers(String name) throws PSDataServiceException {
        List<String> userNames = new ArrayList<>();
        try
        {
            Set<IPSTypedPrincipal> users = roleMgr.getRoleMembers(name);
            for (IPSTypedPrincipal user : users)
            {
                userNames.add(user.getName());
            }
        }
        catch (PSSecurityCatalogException e)
        {
            throw new PSDataServiceException(e);
        }
        userNames.removeAll(PSUserService.SYSTEM_USERS);
        Collator coll = Collator.getInstance();
        Collections.sort(userNames, coll);
        return userNames;
    }
    
    @POST
    @Path("/update")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSRole update(PSRole role) throws PSDataServiceException
    {


        String oldRoleName = role.getOldName();

        if (oldRoleName != null && !oldRoleName.equals(role.getName()))
        {

            // check existing role
            checkRole(oldRoleName);
            // check and create new role remove oldname as create calls update
            role.setOldName(null);
            create(role);

            wfService.copyWorkflowToRole(oldRoleName, role.getName());

            //delete old role
            delete(new PSStringWrapper(oldRoleName));

        }
        PSParameterValidationUtils.rejectIfNull("update", "role", role);
        
        String name = role.getName();
        checkRole(name);
        
        doValidation(role, false);
        
        PSBackEndRole beRole = backEndRoleMgr.update(name, role.getDescription());
        setHomepage(role.getName(), role.getHomepage());
        
        List<String> users = new ArrayList<>(role.getUsers());
        
        PSRole existingRole = find(role.getName());
        List<String> existingUsers = existingRole.getUsers();
        
        if (!users.equals(existingUsers))
        {
            users.removeAll(existingUsers);
            if (!users.isEmpty())
            {
                addUsersToRole(name, users);
            }
                        
            existingUsers.removeAll(role.getUsers());
            if (!existingUsers.isEmpty())
            {
                removeUsersFromRole(name, existingUsers);
            }
        }
        
        PSRole updatedRole = new PSRole();
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
    public PSUserList getAvailableUsers(PSRole role) throws PSDataServiceException
    {
        PSParameterValidationUtils.rejectIfNull("getAvailableUsers", "role", role);
        
        PSUserList availableList = new PSUserList();
              
        PSUserList users = userService.getUsers();
        List<String> availableUsers = users.getUsers();
        availableUsers.removeAll(role.getUsers());
               
        availableList.setUsers(availableUsers);
        
        return availableList;
    }
    
    @POST
    @Path("/validateForDelete")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void validateForDelete(PSRole role) throws PSDataServiceException
    {
        PSParameterValidationUtils.rejectIfNull("validateForDelete", "role", role);
        
        String name = role.getName();
        checkRole(name);
        
        String emsg = "";
        List<String> singleRoleUsers = getSingleRoleUsers(role);
        if (!singleRoleUsers.isEmpty())
        {
            emsg = "The following users will be unable to login if role '" + name + "' is deleted: '";
            emsg += PSStringUtils.listToString(singleRoleUsers, "', '") + "'.";
        }
        
        List<String> inUseWorkflows = getInUseWorkflows(role);
        if (!inUseWorkflows.isEmpty())
        {
            if (StringUtils.isNotEmpty(emsg))
            {
                emsg += "<br><br>";
            }
            
            emsg += "Role '" + name + "' is used by the following workflows: '";
            emsg += PSStringUtils.listToString(inUseWorkflows, "', '") + "'.";
        }
        
        if (StringUtils.isNotEmpty(emsg))
        {
            PSValidationErrorsBuilder builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
            builder.reject("validate.role.delete", emsg).throwIfInvalid();
        }
    }
    
    @POST
    @Path("/validateDeleteUsers")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void validateDeleteUsersFromRole(PSUserList userList) throws PSDataServiceException
    {
        PSParameterValidationUtils.rejectIfNull("validateDeleteUsers", "userList", userList);
        
        String emsg = "";
        List<String> singleRoleUsers = getSingleRoleUsers(userList.getUsers());
        if (!singleRoleUsers.isEmpty())
        {
            emsg = "The following users will be unable to login if they are removed from this role: ";
            emsg += PSStringUtils.listToString(singleRoleUsers, ", ") + '.';
            
            PSValidationErrorsBuilder builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
            builder.reject("validate.delete.users", emsg).throwIfInvalid();
        }
    }
    
    /**
     * Validates the specified role. It validates the role object according to
     * its annotation and invokes
     * {@link PSRoleValidator#doValidation(PSRole, PSBeanValidationException)}
     * for additional validation.
     * 
     * @param role the role in question, not <code>null</code>.
     * @param isCreateRole if <code>true</code>, validating creating the role.
     * 
     * @throws PSBeanValidationException if failed to validate the specified
     *             role.
     */
    protected void doValidation(PSRole role, boolean isCreateRole) throws PSValidationException
    {
        log.debug("validating role " + role);
        if(isCreateRole){
            PSRoleValidator validator = new PSRoleValidator(isCreateRole);

            validator.validate(role).throwIfInvalid();
        }

    }

    /**
     * Validates that the role is already in the system.
     *
     * @param name never <code>null</code> or empty.
     */
    protected void checkRole(String name) throws PSValidationException {
        PSParameterValidationUtils.rejectIfBlank("checkRole", "name", name);

        if (!backEndRoleMgr.getRoles().contains(name))
        {
            String emsg = "Role not found " + name;
            log.error(emsg);
            PSValidationErrorsBuilder builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
            builder.reject("no.such.role", emsg).throwIfInvalid();
        }
    }

    /**
     * Validates that the role is already in the system.
     *
     * @param name never <code>null</code> or empty.
     */
    protected void checkNewRole(String name) throws PSValidationException {
        PSParameterValidationUtils.rejectIfBlank("checkRole", "name", name);

        if (backEndRoleMgr.getRoles().contains(name))
        {
            String emsg = "Role " + name + " already exists";
            log.error(emsg);
            PSValidationErrorsBuilder builder = new PSValidationErrorsBuilder(PSRole.class.getCanonicalName());
            builder.reject("no.such.role", emsg).throwIfInvalid();
        }
    }

    /**
     * This is used to validate a {@link PSRole} object before updating an
     * existing role or create a new one.
     * 
     * This invocation of
     * {@link #doValidation(PSRole, PSBeanValidationException)} is indirectly
     * done by {@link PSRoleService#doValidation(PSRole, boolean)}.
     */
    protected class PSRoleValidator extends PSAbstractBeanValidator<PSRole>
    {
        /**
         * It is <code>true</code> if validating {@link PSRole} object for
         * creating a role.
         */
        boolean isCreateRole = false;

        public PSRoleValidator(boolean isCreate)
        {
            this.isCreateRole = isCreate;
        }

        @Override
        protected void doValidation(PSRole role, PSBeanValidationException e) {
            List<String> allUsers = new ArrayList<>();
            try {
                //TODO:  Should this really be getting all users like this?

                // make sure all users exist in the system.
                allUsers = userService.getUsers().getUsers();
            } catch (PSDataServiceException psDataServiceException) {
                log.error("Error listing system users. Error: {}",PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                e.addSuppressed(e);
            }
            if (PSCollectionUtils.containsIgnoringCase(SYSTEM_ROLES, role.getName()))
            {
                e.rejectValue("name", "role.nameRestricted",
                        "That role name is restricted for system use. Please choose a different role name.");
            }
            /*
             * Lets not continue validating if it's already invalid.
             */
            if (e.hasErrors())
            {
                return;
            }

            for(String rl : role.getUsers())
            {
                if (!PSCollectionUtils.containsIgnoringCase(allUsers, rl))
                {
                    String msg = "Cannot add user \"" + rl + "\" because user named \"" + rl + "\" does not exist.";
                    e.rejectValue("users" , "no.such.user", msg);                   
                }
            }

            if (e.hasErrors())
            {
                return;
            }
            
            if (isCreateRole)
            {
                // make sure created role not in the system
                String newName = role.getName();
                PSBackEndRole beRole = backEndRoleMgr.getRole(newName);
                if (beRole != null)
                {
                    String errorMsg = "Cannot create role \"" + newName + "\" because a role named \""
                                    + beRole.getName() + "\" already exists.";
                    String errorMsg2 = "already_exist:"+beRole.getName();
                    log.debug(errorMsg);
                    e.rejectValue("name", "not.create.existing.role", errorMsg2);
                }
            }
            else
            {
                try {
                    cannotRemoveYourselfFromAdminRole(role, e);
                } catch (PSDataServiceException psDataServiceException) {
                    e.addSuppressed(psDataServiceException);
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }
            }
        }

        /**
         * Make sure the user cannot remove him/herself from the "Admin" role.
         * 
         * @param role the modified role, assumed not <code>null</code>.
         * @param e used to collect validation errors.
         */
        private void cannotRemoveYourselfFromAdminRole(PSRole role, PSBeanValidationException e) throws PSDataServiceException {
            if (!role.getName().equals("Admin"))
            {
                return;
            }
            
            if (!role.getUsers().contains(userService.getCurrentUser().getName()))
            {
                String emsg = "Cannot remove yourself from \"Admin\" role.";
                log.debug(emsg);
                e.rejectValue("users", "cannot.remove.user.admin.role", emsg);
            }
        }
    }
    
    /**
     * Finds all users assigned to the specified role which are only assigned to one role (includes system roles).
     * 
     * @param role assumed not <code>null</code>.
     * 
     * @return list of user names, never <code>null</code>, may be empty.
     */
    private List<String> getSingleRoleUsers(PSRole role)
    {
        return getSingleRoleUsers(role.getUsers());
    }
    
    /**
     * Sets the homepage for a role.
     * @param roleName the name of the role must not be blank.
     * @param homepage the homepage to set if <code>null</code> or not a valid home page, then set to Dashboard.
     */
    private void setHomepage(String roleName, String homepage) throws IPSGenericDao.LoadException, IPSGenericDao.SaveException {
    	if(StringUtils.isBlank(roleName)) {
            throw new IllegalArgumentException("roleName must not be blank");
        }
    	if(StringUtils.isBlank(homepage) || !(homepage.equals(HOMEPAGE_TYPE_DASHBOARD) || homepage.equals(HOMEPAGE_TYPE_EDITOR) || 
    	        homepage.equals(HOMEPAGE_TYPE_HOME))) {
            homepage = HOMEPAGE_TYPE_DASHBOARD;
        }
    	String key = META_DATA_HOMEPAGE_PREFIX + roleName;
    	PSMetadata md = mdService.find(key);
    	if(md == null)
    	{
            md = new PSMetadata(key,homepage);
    	}
    	else
    	{
            md.setData(homepage);
    	}
        mdService.save(md);
    }
    
    /**
     * Gets the homepage  of a given role.
     * @param roleName the name of the role must not be blank.
     * @return String never <code>null</code>, if it is not set, returns "Dashboard".
     */
    private String getHomepage(String roleName) throws IPSGenericDao.LoadException {
    	if(StringUtils.isBlank(roleName)) {
            throw new IllegalArgumentException("roleName must not be blank");
        }
    	String key = META_DATA_HOMEPAGE_PREFIX + roleName;
    	PSMetadata md = mdService.find(key);
    	return (md == null? HOMEPAGE_TYPE_DASHBOARD : md.getData());
    }

    /**
     * Finds all users assigned to one role.  This includes system roles.
     * 
     * @param users list of user names, assumed not <code>null</code>.
     * 
     * @return list of user names, never <code>null</code>, may be empty.
     */
    private List<String> getSingleRoleUsers(List<String> users)
    {
        List<String> singleRoleUsers = new ArrayList<>();
        
        for (String user : users)
        {
            try
            {
                if (backEndRoleMgr.getRoles(user).size() == 1)
                {
                    singleRoleUsers.add(user);
                }
            }
            catch (Exception e)
            {
                log.warn("Failed to get roles for user '{}'.",  user);
            }
        }
        
        return singleRoleUsers;
    }
    
    /**
     * Finds all workflows which are using the specified role.  A workflow is using a role if the role has been added
     * to a state of the workflow as anything other than {@link PSAssignmentTypeEnum#READER}.
     * 
     * @param role assumed not <code>null</code>.
     * 
     * @return list of workflow names, never <code>null</code>, may be empty.
     */
    private List<String> getInUseWorkflows(PSRole role)
    {
        List<String> inUseWorkflows = new ArrayList<>();
        
        String name = role.getName();
                
        List<PSWorkflow> workflows = wfService.findWorkflowsByName("");
        for (PSWorkflow workflow : workflows)
        {
            String wfName = workflow.getName();
            if (wfName.equals(PSWorkflowHelper.LOCAL_WORKFLOW_NAME))
            {
                // skip the workflow used for local content
                continue;
            }
            
            boolean roleInUse = false;
            
            List<PSWorkflowRole> wfRoles = workflow.getRoles();
            for (PSWorkflowRole wfRole : wfRoles)
            {
                if (wfRole.getName().equals(name))
                {
                    IPSGuid id = wfRole.getGUID();
                    
                    List<PSState> states = workflow.getStates();
                    for (PSState state : states)
                    {
                        List<PSAssignedRole> asRoles = state.getAssignedRoles();
                        for (PSAssignedRole asRole : asRoles)
                        {
                            if (asRole.getGUID().equals(id))
                            {
                                PSAssignmentTypeEnum asType = asRole.getAssignmentType();
                                if (asType != PSAssignmentTypeEnum.NONE && asType != PSAssignmentTypeEnum.READER)
                                {
                                    inUseWorkflows.add(wfName);
                                    roleInUse = true;
                                    break;
                                }
                            }
                        }
                        
                        if (roleInUse)
                        {
                            break;
                        }
                    }
                    
                    break;
                }
            }
        }
        
        return inUseWorkflows;
    }
    
    /**
     * Adds the specified users to the specified role.
     * 
     * @param roleName assumed not <code>null</code>.
     * @param users list of user names, assumed not <code>null</code>.
     */
    private void addUsersToRole(String roleName, List<String> users)
    {
        for (String user : users)
        {
            List<String> roles = backEndRoleMgr.getRoles(user);
            roles.add(roleName);
            backEndRoleMgr.setRoles(user, roles);
        }
    }
    
    /**
     * Removes the specified users from the specified role.
     * 
     * @param roleName assumed not <code>null</code>.
     * @param users list of user names, assumed not <code>null</code>.
     */
    private void removeUsersFromRole(String roleName, List<String> users)
    {
        for (String user : users)
        {
            List<String> roles = backEndRoleMgr.getRoles(user);
            roles.remove(roleName);
            backEndRoleMgr.setRoles(user, roles);
        }
    }

    @Override
    @GET
    @Path("/userhomepage")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUserHomepage() throws IPSGenericDao.LoadException {

        List<String> userRoles=null;
        try {
            userRoles = userService.getCurrentUser().getRoles();
        } catch (IPSUserService.PSNoCurrentUserException e) {
            log.error("Error getting roles, No Current User! Error:",PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        } catch (PSDataServiceException e) {
            log.error("Error getting roles for current user: {} Error:",PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }

        Set<String> userHomePages = new HashSet<>();
        String homepage = null;
        if(userRoles != null) {
            for (String role : userRoles) {
                userHomePages.add(getHomepage(role));
            }
        }
        if(userHomePages.isEmpty() || userHomePages.contains(HOMEPAGE_TYPE_DASHBOARD)){
            homepage = HOMEPAGE_TYPE_DASHBOARD;
        }
        else if(userHomePages.contains(HOMEPAGE_TYPE_EDITOR)){
            homepage = HOMEPAGE_TYPE_EDITOR;
        }
        else{
            homepage = HOMEPAGE_TYPE_HOME;
        }

        return homepage;
    }

    public IPSRoleMgr getRoleMgr(){
        return roleMgr;
    }

    public void setRoleMgr(IPSRoleMgr roleMgr) {
        this.roleMgr = roleMgr;
    }
    
}
