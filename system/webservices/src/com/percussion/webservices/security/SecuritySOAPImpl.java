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
// REFACTORED: CP-JAVA11, CP-SOAP
package com.percussion.webservices.security;

import com.percussion.data.PSInternalRequestCallException;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthenticatedFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.PSBaseSOAPImpl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jws.WebService;
import javax.security.auth.login.LoginException;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Optional;

/**
 * Server side implementations for web services defined in rhythmyx.wsdl
 * for operations defined in the securitySOAP bindings.
 *
 * <p>Modernized for Java 11 with enhanced exception handling, Optional usage,
 * and improved logging. Uses JAX-WS annotations for contemporary SOAP implementation.
 */
@WebService(endpointInterface = "com.percussion.webservices.security.Security")
public class SecuritySOAPImpl extends PSBaseSOAPImpl implements Security {

    private static final Logger logger = LogManager.getLogger(SecuritySOAPImpl.class);

    /**
     * Load communities based on the provided request criteria.
     *
     * @param loadCommunitiesRequest the request containing community search criteria
     * @return array of communities matching the criteria
     * @throws RemoteException if a system error occurs
     * @throws PSInvalidSessionFault if the session is invalid
     * @throws PSContractViolationFault if the request contract is violated
     * @throws PSNotAuthorizedFault if the user is not authorized
     */
    @Override
    public com.percussion.webservices.security.data.PSCommunity[] loadCommunities(
        LoadCommunitiesRequest loadCommunitiesRequest)
        throws RemoteException, PSInvalidSessionFault, PSContractViolationFault, PSNotAuthorizedFault {

        var serviceName = "loadCommunities";
        logger.debug("Loading communities for request: {}", loadCommunitiesRequest);

        try {
            authenticate();
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var communities = service.loadCommunities(loadCommunitiesRequest.getName());

            return convert(com.percussion.webservices.security.data.PSCommunity[].class, communities);
        } catch (IllegalArgumentException e) {
            handleInvalidContract(e, serviceName);
            return new com.percussion.webservices.security.data.PSCommunity[0]; // Never reached
        } catch (RuntimeException e) {
            handleRuntimeException(e, serviceName);
            return new com.percussion.webservices.security.data.PSCommunity[0]; // Never reached
        }
    }

    /**
     * Load roles based on the provided request criteria.
     *
     * @param loadRolesRequest the request containing role search criteria
     * @return array of roles matching the criteria
     * @throws RemoteException if a system error occurs
     * @throws PSInvalidSessionFault if the session is invalid
     * @throws PSContractViolationFault if the request contract is violated
     * @throws PSNotAuthorizedFault if the user is not authorized
     */
    @Override
    public com.percussion.webservices.security.data.PSRole[] loadRoles(LoadRolesRequest loadRolesRequest)
        throws RemoteException, PSInvalidSessionFault, PSContractViolationFault, PSNotAuthorizedFault {

        var serviceName = "loadRoles";
        logger.debug("Loading roles for request: {}", loadRolesRequest);

        try {
            authenticate();
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var roles = service.loadRoles(loadRolesRequest.getName());

            return convert(com.percussion.webservices.security.data.PSRole[].class, roles);
        } catch (IllegalArgumentException e) {
            handleInvalidContract(e, serviceName);
            return new com.percussion.webservices.security.data.PSRole[0]; // Never reached
        } catch (RuntimeException e) {
            handleRuntimeException(e, serviceName);
            return new com.percussion.webservices.security.data.PSRole[0]; // Never reached
        }
    }

    /**
     * Authenticate a user and establish a session.
     *
     * @param loginRequest the login request containing credentials and client info
     * @return login response with session information
     * @throws RemoteException if a system error occurs
     * @throws PSNotAuthenticatedFault if authentication fails
     * @throws PSContractViolationFault if the request contract is violated
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest)
        throws RemoteException, PSNotAuthenticatedFault, PSContractViolationFault {

        var serviceName = "login";
        logger.debug("Login attempt for user: {}", loginRequest.getUsername());

        try {
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var servletRequest = getServletRequest().orElseThrow(() ->
                new PSNotAuthenticatedFault(0, "Servlet request not available",
                    "No servlet request context for login"));
            var servletResponse = getServletResponse().orElseThrow(() ->
                new PSNotAuthenticatedFault(0, "Servlet response not available",
                    "No servlet response context for login"));

            var login = service.login(
                servletRequest,
                servletResponse,
                loginRequest.getUsername(),
                loginRequest.getPassword(),
                loginRequest.getClientId(),
                loginRequest.getCommunity(),
                loginRequest.getLocaleCode()
            );

            var convertedLogin = convert(
                com.percussion.webservices.security.data.PSLogin.class,
                login
            );

            logger.debug("Login successful for user: {}", loginRequest.getUsername());
            return new LoginResponse(convertedLogin);

        } catch (IllegalArgumentException e) {
            handleInvalidContract(e, serviceName);
        } catch (IOException | ServletException | LoginException | PSInternalRequestCallException e) {
            logger.warn("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw new PSNotAuthenticatedFault(0, e.getLocalizedMessage(),
                ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            logger.error("Unexpected error during login for user: {}", loginRequest.getUsername(), e);
            throw new PSNotAuthenticatedFault(0, e.getLocalizedMessage(),
                ExceptionUtils.getStackTrace(e));
        }

        return null; // Never reached due to exception handling
    }

    /**
     * Logout a user and invalidate their session.
     *
     * @param logoutRequest the logout request containing session information
     * @throws RemoteException if a system error occurs
     * @throws PSInvalidSessionFault if the session is invalid
     * @throws PSContractViolationFault if the request contract is violated
     */
    @Override
    public void logout(LogoutRequest logoutRequest)
        throws RemoteException, PSInvalidSessionFault, PSContractViolationFault {

        var serviceName = "logout";
        var sessionId = Optional.ofNullable(logoutRequest.getSessionId())
            .orElseThrow(() -> new IllegalArgumentException("Session ID cannot be null"));

        logger.debug("Logout request for session: {}", sessionId);

        try {
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var servletRequest = getServletRequest().orElse(null);

            service.logout(servletRequest, sessionId);
            logger.debug("Logout successful for session: {}", sessionId);

        } catch (IllegalArgumentException e) {
            handleInvalidContract(e, serviceName);
        } catch (RuntimeException e) {
            handleRuntimeException(e, serviceName);
        }
    }

    /**
     * Refresh an existing user session to extend its validity.
     *
     * @param refreshSessionRequest the refresh request containing session information
     * @throws RemoteException if a system error occurs
     * @throws PSInvalidSessionFault if the session is invalid
     * @throws PSContractViolationFault if the request contract is violated
     */
    @Override
    public void refreshSession(RefreshSessionRequest refreshSessionRequest)
        throws RemoteException, PSInvalidSessionFault, PSContractViolationFault {

        var serviceName = "refreshSession";
        var sessionId = Optional.ofNullable(refreshSessionRequest.getSessionId())
            .orElseThrow(() -> new IllegalArgumentException("Session ID cannot be null"));

        logger.debug("Session refresh request for session: {}", sessionId);

        try {
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var servletRequest = getServletRequest().orElse(null);

            service.refreshSession(servletRequest, sessionId);
            logger.debug("Session refresh successful for session: {}", sessionId);

        } catch (IllegalArgumentException e) {
            handleInvalidContract(e, serviceName);
        } catch (LoginException e) {
            logger.warn("Session refresh failed for session: {}", sessionId, e);
            throw new PSInvalidSessionFault(0, e.getLocalizedMessage(),
                ExceptionUtils.getStackTrace(e));
        } catch (RuntimeException e) {
            handleRuntimeException(e, serviceName);
        }
    }

    /**
     * Filter content IDs by runtime visibility permissions.
     *
     * @param ids array of content IDs to filter
     * @return response containing filtered IDs that are visible to the current user
     * @throws RemoteException if a system error occurs
     * @throws PSInvalidSessionFault if the session is invalid
     * @throws PSContractViolationFault if the request contract is violated
     */
    @Override
    public FilterByRuntimeVisibilityResponse filterByRuntimeVisibility(long[] ids)
        throws RemoteException, PSInvalidSessionFault, PSContractViolationFault {

        var serviceName = "filterByRuntimeVisibility";
        var inputCount = Optional.ofNullable(ids).map(array -> array.length).orElse(0);
        logger.debug("Filtering {} IDs by runtime visibility", inputCount);

        try {
            authenticate();
            var service = PSSecurityWsLocator.getSecurityWebservice();

            if (ids == null || ids.length == 0) {
                logger.debug("No IDs provided, returning empty response");
                return new FilterByRuntimeVisibilityResponse(new long[0]);
            }

            // Convert long[] to List<IPSGuid> using available PSGuidUtils methods
            var guidList = java.util.Arrays.stream(ids)
                .mapToObj(id -> PSGuidUtils.makeGuid(id, com.percussion.services.catalog.PSTypeEnum.NODEDEF))
                .collect(java.util.stream.Collectors.toList());

            var filteredGuids = service.filterByRuntimeVisibility(guidList);
            var filteredIdsLong = PSGuidUtils.toLongArray(filteredGuids);

            // Convert Long[] to long[] for the response
            var filteredIds = java.util.Arrays.stream(filteredIdsLong)
                .mapToLong(Long::longValue)
                .toArray();

            logger.debug("Filtered {} IDs down to {} visible IDs", inputCount, filteredIds.length);

            return new FilterByRuntimeVisibilityResponse(filteredIds);

        } catch (IllegalArgumentException e) {
            handleInvalidContract(e, serviceName);
            return new FilterByRuntimeVisibilityResponse(new long[0]); // Never reached
        } catch (RuntimeException e) {
            handleRuntimeException(e, serviceName);
            return new FilterByRuntimeVisibilityResponse(new long[0]); // Never reached
        }
    }
}
