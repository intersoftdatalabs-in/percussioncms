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

import com.percussion.webservices.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jws.WebService;
import javax.security.auth.login.LoginException;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Optional;

// Generated request/response types from webservices module
import com.percussion.webservices.securityservices.LoadCommunitiesRequest;
import com.percussion.webservices.securityservices.LoadRolesRequest;
import com.percussion.webservices.securityservices.LoginRequest;
import com.percussion.webservices.securityservices.LoginResponse;
import com.percussion.webservices.securityservices.LogoutRequest;
import com.percussion.webservices.securityservices.RefreshSessionRequest;
import com.percussion.webservices.securityservices.FilterByRuntimeVisibilityRequest;
import com.percussion.webservices.securityservices.FilterByRuntimeVisibilityResponse;


/**
 * Server side implementations for web services defined in rhythmyx.wsdl
 * for operations defined in the securitySOAP bindings.
 *
 * <p>Modernized for Java 11 with enhanced exception handling, Optional usage,
 * and improved logging. Uses JAX-WS annotations for contemporary SOAP implementation.
 */
@WebService(endpointInterface = "com.percussion.webservices.securityservices.Security")
public class SecuritySOAPImpl extends PSBaseSOAPImpl implements com.percussion.webservices.securityservices.Security {

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
    public com.percussion.webservices.securityservices.LoadCommunitiesResponse loadCommunities(
        LoadCommunitiesRequest loadCommunitiesRequest) throws com.percussion.webservices.securityservices.InvalidSessionFaultMessage, com.percussion.webservices.securityservices.NotAuthorizedFaultMessage {

        var serviceName = "loadCommunities";
        logger.debug("Loading communities for request: {}", loadCommunitiesRequest);

        try {
            try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securityservices.InvalidSessionFaultMessage(e.toString(), e); }
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var communities = service.loadCommunities(loadCommunitiesRequest.getName());

            com.percussion.webservices.securityservices.LoadCommunitiesResponse resp =
                new com.percussion.webservices.securityservices.LoadCommunitiesResponse();

            // converted communities may be null or empty; convert and add to response list
            resp.getPSCommunity().addAll(
                java.util.Arrays.asList(convert(com.percussion.webservices.security.data.PSCommunity[].class, communities))
            );

            return resp;
        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
            return new com.percussion.webservices.securityservices.LoadCommunitiesResponse(); // Never reached
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.securityservices.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
            return new com.percussion.webservices.securityservices.LoadCommunitiesResponse(); // Never reached
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
    public com.percussion.webservices.securityservices.LoadRolesResponse loadRoles(LoadRolesRequest loadRolesRequest)
        throws com.percussion.webservices.securityservices.NotAuthorizedFaultMessage,
               com.percussion.webservices.securityservices.InvalidSessionFaultMessage {

        var serviceName = "loadRoles";
        logger.debug("Loading roles for request: {}", loadRolesRequest);

        try {
            try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securityservices.InvalidSessionFaultMessage(e.toString(), e); }
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var roles = service.loadRoles(loadRolesRequest.getName());

            com.percussion.webservices.securityservices.LoadRolesResponse resp = new com.percussion.webservices.securityservices.LoadRolesResponse();
            resp.getPSRole().addAll(java.util.Arrays.asList(convert(com.percussion.webservices.security.data.PSRole[].class, roles)));
            return resp;
        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
            return new com.percussion.webservices.securityservices.LoadRolesResponse(); // Never reached
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
            return new com.percussion.webservices.securityservices.LoadRolesResponse(); // Never reached
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
    public LoginResponse login(LoginRequest loginRequest) throws com.percussion.webservices.securityservices.ContractViolationFaultMessage, com.percussion.webservices.securityservices.NotAuthenticatedFaultMessage {

        var serviceName = "login";
        logger.debug("Login attempt for user: {}", loginRequest.getUsername());

        try {
            var service = PSSecurityWsLocator.getSecurityWebservice();
            var servletRequest = getServletRequest().orElse(null);
            var servletResponse = getServletResponse().orElse(null);

            if (servletRequest == null || servletResponse == null) {
                throw new RuntimeException("Servlet context not available for login");
            }

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
            LoginResponse resp = new LoginResponse();
            resp.setPSLogin(convertedLogin);
            return resp;

        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securityservices.ContractViolationFaultMessage(cv.toString(), cv); }
            return null;
        } catch (IOException | ServletException | LoginException | PSInternalRequestCallException e) {
            logger.warn("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw new com.percussion.webservices.securityservices.NotAuthenticatedFaultMessage(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during login for user: {}", loginRequest.getUsername(), e);
            throw new RuntimeException(e);
        }

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
        throws com.percussion.webservices.securityservices.InvalidSessionFaultMessage, com.percussion.webservices.securityservices.ContractViolationFaultMessage {

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
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securityservices.ContractViolationFaultMessage(cv.toString(), cv); }
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
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
        throws com.percussion.webservices.securityservices.InvalidSessionFaultMessage, com.percussion.webservices.securityservices.ContractViolationFaultMessage {

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
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securityservices.ContractViolationFaultMessage(cv.toString(), cv); }
        } catch (LoginException e) {
            logger.warn("Session refresh failed for session: {}", sessionId, e);
            throw new com.percussion.webservices.securityservices.InvalidSessionFaultMessage(e.getLocalizedMessage(), e);
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
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
    public FilterByRuntimeVisibilityResponse filterByRuntimeVisibility(FilterByRuntimeVisibilityRequest filterByRuntimeVisibilityRequest)
        throws com.percussion.webservices.securityservices.InvalidSessionFaultMessage, com.percussion.webservices.securityservices.ContractViolationFaultMessage {

        var serviceName = "filterByRuntimeVisibility";
        var idsList = Optional.ofNullable(filterByRuntimeVisibilityRequest)
            .map(FilterByRuntimeVisibilityRequest::getId)
            .orElse(java.util.Collections.emptyList());
        var inputCount = idsList.size();
        logger.debug("Filtering {} IDs by runtime visibility", inputCount);

        try {
            String session;
            try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securityservices.InvalidSessionFaultMessage(e.toString(), e); }
            var service = PSSecurityWsLocator.getSecurityWebservice();

            if (idsList.isEmpty()) {
                logger.debug("No IDs provided, returning empty response");
                FilterByRuntimeVisibilityResponse emptyResp = new FilterByRuntimeVisibilityResponse();
                emptyResp.setIds(new FilterByRuntimeVisibilityResponse.Ids());
                return emptyResp;
            }

            // Convert List<Long> to List<IPSGuid>
            var guidList = idsList.stream()
                .map(id -> PSGuidUtils.makeGuid(id, com.percussion.services.catalog.PSTypeEnum.NODEDEF))
                .collect(java.util.stream.Collectors.toList());

            var filteredGuids = service.filterByRuntimeVisibility(guidList);
            var filteredIdsLong = PSGuidUtils.toLongArray(filteredGuids);

            FilterByRuntimeVisibilityResponse resp = new FilterByRuntimeVisibilityResponse();
            FilterByRuntimeVisibilityResponse.Ids idWrapper = new FilterByRuntimeVisibilityResponse.Ids();
            idWrapper.getId().addAll(java.util.Arrays.asList(filteredIdsLong));
            resp.setIds(idWrapper);

            logger.debug("Filtered {} IDs down to {} visible IDs", inputCount, idWrapper.getId().size());

            return resp;

        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securityservices.ContractViolationFaultMessage(cv.toString(), cv); }
            FilterByRuntimeVisibilityResponse emptyResp = new FilterByRuntimeVisibilityResponse();
            emptyResp.setIds(new FilterByRuntimeVisibilityResponse.Ids());
            return emptyResp; // Never reached
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
            FilterByRuntimeVisibilityResponse emptyResp = new FilterByRuntimeVisibilityResponse();
            emptyResp.setIds(new FilterByRuntimeVisibilityResponse.Ids());
            return emptyResp; // Never reached
        }
    }
}
