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

import com.percussion.data.PSInternalRequestCallException;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.rest.Status;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.errors.DirectoryUserImportErrorException;
import com.percussion.rest.errors.DirectoryUserImportInvalidNameException;
import com.percussion.rest.errors.UnexpectedException;
import com.percussion.rest.errors.UnknownUserException;
import com.percussion.rest.errors.UnsupportedUserTypeException;
import com.percussion.rest.users.IUserAdaptor;
import com.percussion.rest.users.User;
import com.percussion.server.PSRequest;
import com.percussion.server.PSUserSession;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSExternalUser;
import com.percussion.user.data.PSImportedUser.ImportStatus;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.user.service.IPSUserService;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceStatus.ServiceStatus;
import com.percussion.user.service.IPSUserService.PSImportUsers;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** Adaptor for managing users in Percussion CMS. */
@PSSiteManageBean
@Lazy
public class UserAdaptor extends SiteManageAdaptorBase implements IUserAdaptor {

  private static final Logger log = LogManager.getLogger(UserAdaptor.class);

  @Autowired private IPSSecurityWs securityWs;

  @Autowired private IPSSecurityDesignWs securityDesignWs;

  @Autowired private IPSSystemWs systemWs;

  @Autowired private IPSGuidManager guidManager;

  @Autowired
  public UserAdaptor(IPSUserService userService, IPSItemWorkflowService itemWorkflowService) {
    super(userService, itemWorkflowService);
  }

  @Override
  public User getUser(URI baseURI, String userName) throws BackendException {
    try {
      var user = userService.find(userName);
      if (user == null) {
        throw new UnknownUserException();
      }
      var ret = new User();
      ret.setUserName(user.getName());
      ret.setEmailAddress(user.getEmail());
      ret.setUserType(user.getProviderType().name());
      ret.setRoles(user.getRoles());

      String communityId = null;
      String communityName = null;
      PSRequest req = PSSecurityFilter.getCurrentRequest();
      PSUserSession userSession = null;
      List<String> userCommunities = null;
      var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
      var puser = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

      if (req != null) {
        userSession = req.getUserSession();
        communityName = userSession.getUserCurrentCommunity();
        communityId = userSession.getCommunityId(req, communityName);
        userCommunities = userSession.getUserCommunities(req);
      }

      if (!StringUtils.isEmpty(communityId)) {
        IPSGuid guid = guidManager.makeGuid(Long.parseLong(communityId), PSTypeEnum.COMMUNITY_DEF);
        var guids = new ArrayList<IPSGuid>();
        guids.add(guid);
        var comms = securityDesignWs.loadCommunities(guids, false, true, session, puser);
        ret.setSelectedCommunity(ApiUtils.convertPSCommunity(comms.get(0)));
      }

      // Load up the available communities for this user
      if (userCommunities != null) {
        List<IPSGuid> availGuids =
            userCommunities.stream()
                .map(s -> new PSGuid(PSTypeEnum.COMMUNITY_DEF, Integer.parseInt(s)))
                .collect(Collectors.toCollection(ArrayList::new));
        var psCommunities =
            securityDesignWs.loadCommunities(availGuids, false, true, session, puser);
        if (psCommunities != null && !psCommunities.isEmpty()) {
          ret.setUserCommunities(ApiUtils.convertPSCommunities(psCommunities));
        }
      }
      return ret;
    } catch (PSDataServiceException | PSInternalRequestCallException | PSErrorResultsException e) {
      throw new BackendException(e);
    }
  }

  @Override
  public User updateOrCreateUser(URI baseURI, User user) throws BackendException {
    try {
      PSUser newUser;
      boolean isNewUser = false;

      try {
        var findUsers = userService.getUserNames(user.getUserName());
        if (findUsers.getUsers().contains(user.getUserName())) {
          newUser = userService.find(user.getUserName());
        } else {
          newUser = new PSUser();
          isNewUser = true;
        }
      } catch (Throwable t) {
        newUser = new PSUser();
        isNewUser = true;
      }

      newUser.setName(user.getUserName());
      newUser.setRoles(user.getRoles());

      if (user.getEmailAddress() != null) {
        newUser.setEmail(user.getEmailAddress());
      }

      // newUser.setRoles(user.getRoles()); // Already set above

      // This block of code is pretty goofy. Too many user-related objects. Stuck with it for now.
      if (!isNewUser) {
        newUser = userService.update(newUser);
      } else {
        if (StringUtils.equalsIgnoreCase(
            user.getUserType(), PSUserProviderType.INTERNAL.name())) {
          newUser = userService.create(newUser);
        } else if (StringUtils.equalsIgnoreCase(
            user.getUserType(), PSUserProviderType.DIRECTORY.name())) {
          var newUsers = new PSImportUsers();
          var dirUsers = new ArrayList<PSExternalUser>();
          dirUsers.add(new PSExternalUser(user.getUserName()));
          newUsers.setExternalUsers(dirUsers);
          var importUsers = userService.importDirectoryUsers(newUsers);

          if (importUsers != null) {
            var impU = importUsers.get(0);

            // Handle new imports and treat duplicates as if they should be updates
            if (impU.getStatus() == ImportStatus.SUCCESS
                || impU.getStatus() == ImportStatus.DUPLICATE) {
              newUser.setEmail(user.getEmailAddress());
              newUser.setName(user.getUserName());
              newUser.setProviderType(PSUserProviderType.DIRECTORY);
              newUser.setRoles(user.getRoles());
              newUser = userService.update(newUser);
            } else if (impU.getStatus() == ImportStatus.ERROR) {
              throw new DirectoryUserImportErrorException();
            } else if (impU.getStatus() == ImportStatus.INVALID) {
              throw new DirectoryUserImportInvalidNameException();
            } else {
              throw new UnexpectedException();
            }
          } else {
            // Import failed with no error or results - meaning something ate an exception it
            // shouldn't have.
            throw new UnexpectedException();
          }
        } else {
          // Just in case we add a new user type / Security provider and this code hasn't been
          // updated.
          throw new UnsupportedUserTypeException();
        }
      }

      return copyUser(newUser, new User());
    } catch (PSDataServiceException e) {
      throw new BackendException(e);
    }
  }

  private User copyUser(PSUser pu, User u) {
    u.setUserName(pu.getName());
    u.setRoles(pu.getRoles());
    u.setEmailAddress(pu.getEmail());
    u.setUserType(pu.getProviderType().name());
    return u;
  }

  @Override
  public void deleteUser(URI baseURI, String userName) throws BackendException {
    try {
      userService.delete(userName);
    } catch (PSDataServiceException e) {
      throw new BackendException(e);
    }
  }

  @Override
  public List<String> findUsers(URI baseURI, String pattern) throws BackendException {
    try {
      return userService.getUserNames(pattern).getUsers();
    } catch (PSDataServiceException e) {
      throw new BackendException(e);
    }
  }

  @Override
  public Status checkDirectoryStatus() {
    var ret = new Status(404, "Not Found");
    try {
      var psStatus = userService.checkDirectoryService();
      if (psStatus.getStatus() == ServiceStatus.ENABLED) {
        ret.setStatusCode(200);
        ret.setMessage(psStatus.getStatus().name());
      } else {
        ret.setStatusCode(404);
        ret.setMessage(psStatus.getStatus().name());
      }
    } catch (Exception e) {
      ret.setStatusCode(500);
      ret.setMessage(e.getMessage());
    }
    return ret;
  }

  @Override
  public List<String> searchDirectory(String pattern) {
    var users = userService.findUsersFromDirectoryService(pattern);
    if (users != null) {
      return users.stream().map(PSExternalUser::getName).collect(Collectors.toList());
    }
    return new ArrayList<>();
  }
}
