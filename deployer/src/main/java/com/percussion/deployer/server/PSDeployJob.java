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
package com.percussion.deployer.server;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.design.objectstore.PSAclEntry;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSSecurityToken;
import com.percussion.security.PSUserEntry;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.server.PSUserSessionManager;
import com.percussion.server.job.PSJobException;
import com.percussion.server.job.PSJobRunner;

import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Base class for all deployment jobs.
 */
public abstract class PSDeployJob extends PSJobRunner implements IPSJobHandle {
   /**
    * Validates that the user is authorized to perform this job. Saves the
    * security token from the request to use for subsequent operations during
    * the run method.
    * @param id The id used to identify this job. Must be used to then call
    *  to set the job id.
    * @param req The request used to determine the current user's security 
    * permissions. May not be <code>null</code>.
    * @param initParams Set of name-value pairs that this job may require, may
    * be <code>null</code>.
    * 
    * @throws IllegalArgumentException if any param is invalid.
    * @throws PSAuthenticationFailedException if the user cannot be 
    * authenticated.
    * @throws PSAuthorizationException if user is not authorized to run this 
    * job.
    * @throws PSJobException for any other errors.
    */
   public void init(int id, PSRequest req, Properties initParams) 
      throws PSAuthenticationFailedException, PSAuthorizationException, 
         PSJobException {
      if (req == null) {
         throw new IllegalArgumentException("req may not be null");
      }

      m_request = req;
      m_id = id;

      // Must have server admin access and be in Admin role
      m_securityToken = req.getSecurityToken();
      PSServer.checkAccessLevel(req, PSAclEntry.SACE_ADMINISTER_SERVER);

      // Check authorization if a role is defined
      var role = Optional.ofNullable(initParams)
                         .map(params -> params.getProperty(ROLE_PARAM_NAME))
                         .map(String::trim)
                         .orElse("");
      if (!role.isEmpty() && !req.isUserInRole(role)) {
         throw new PSAuthorizationException("Deployment", getJobType(), req.getUserSessionId());
      }

      initUserInfo(req);
   }

   protected void initUserInfo(PSRequest req) {
      m_securityToken = req.getSecurityToken();
      var userEntries = PSUserSessionManager.getUserSession(req).getAuthenticatedUserEntries();
      m_userId = userEntries.length > 0 ? userEntries[0].getName() : "unknown";
      m_userInfoInited = true;
   }

   /**
    * Updates the status of the job.
    *
    * @throws IllegalStateException if {@link #initDepCount(Iterator)} has not
    * been called.
    */
   public void updateStatus(String message) {
      if (message == null || message.trim().isEmpty()) {
         throw new IllegalArgumentException("message may not be null or empty");
      }

      if (m_depTotal == -1) {
         throw new IllegalStateException("initDepCount() has not been called");
      }

      if (m_curDepCount < m_depTotal) { // Don't want to go over, shouldn't happen
         m_curDepCount++;
      }

      var status = Math.min(99, Math.max(1, (m_curDepCount * 100) / m_depTotal));
      setStatus(status);
      setStatusMessage(message);
   }

   /**
    * Walks the supplied packages and initializes the total number of 
    * dependencies that will be processed by this job.
    * 
    * @param pkgs An iterator over one or more <code>PSDependency</code>
    * objects to use to determine the count, may not be <code>null</code>.
    */
   protected void initDepCount(Iterator<PSDependency> pkgs) {
      initDepCount(pkgs, true);
   }

   /**
    * Walks the supplied packages and initializes the total number of 
    * dependencies that will be processed by this job.
    * 
    * @param pkgs An iterator over one or more <code>PSDependency</code>
    * objects to use to determine the count, may not be <code>null</code>.
    * @param includedOnly If <code>true</code>, only included dependencies will
    * be counted, otherwise all dependencies will be counted.
    */
   protected void initDepCount(Iterator<PSDependency> pkgs, boolean includedOnly) {
      if (pkgs == null || !pkgs.hasNext()) {
         throw new IllegalArgumentException("pkgs may not be null or empty");
      }

      m_depTotal = Stream.of(pkgs)
                          .mapToInt(dep -> {
                             var childCount = dep.getChildCount(includedOnly);
                             if (childCount == 0) {
                                var included = includedOnly ? "included " : "";
                                throw new IllegalArgumentException("pkg " + dep.getKey() + " has no " + included + "children");
                             }
                             return childCount;
                          })
                          .sum();
   }

   /**
    * Get the security token extracted from the request supplied to the call to
    * <code>init()</code>.
    * 
    * @return The token, never <code>null</code>.
    * 
    * @throws IllegalStateException If <code>init()</code> has not been called.
    */
   protected PSSecurityToken getSecurityToken() {
      if (!m_userInfoInited) {
         throw new IllegalStateException("userInfo not initialized");
      }
      return m_securityToken;
   }

   /**
    * Gets the name of the user that initiated this job.
    * 
    * @return The name, never <code>null</code> or empty.
    */
   protected String getUserId() {
      return m_userId;
   }

   /**
    * Get the job type to use in error and console messages.
    * 
    * @return The type of the job, never <code>null</code> or empty.
    */   
   protected abstract String getJobType();
