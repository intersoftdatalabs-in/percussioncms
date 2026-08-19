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
package com.percussion.webservices;

import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;

import com.percussion.cms.IPSConstants;
import com.percussion.security.PSAuthorizationException;
import com.percussion.services.security.PSServletRequestWrapper;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSErrorResultsFault;
import com.percussion.webservices.faults.PSErrorsFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSLockFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.system.RelationshipCategory;
import com.percussion.webservices.transformation.impl.PSTransformerFactory;

import com.percussion.webservices.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPElement;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This base class implements generic functionality available with every
 * JAX-WS SOAP implementation, modernized for Java 11 and contemporary SOAP standards.
 *
 * <p>Provides common functionality for authentication, session management,
 * exception handling, and attachment processing for SOAP web services.
 */
public class PSBaseSOAPImpl {

    /**
     * The logger for this class.
     */
    protected static final Logger logger = LogManager.getLogger(IPSConstants.WEBSERVICES_LOG);

    /**
     * WebService context for accessing SOAP message context and HTTP servlet objects.
     */
    protected WebServiceContext webServiceContext;

    /**
     * Get the HTTP servlet request associated with the current JAX-WS message.
     *
     * @return the HTTP servlet request wrapped in Optional, empty if not available
     */
    protected Optional<HttpServletRequest> getServletRequest() {
        if (webServiceContext == null) {
            return Optional.empty();
        }

        var messageContext = webServiceContext.getMessageContext();
        var request = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
        return Optional.ofNullable(request);
    }

    /**
     * Get the HTTP servlet response associated with the current JAX-WS message.
     *
     * @return the HTTP servlet response wrapped in Optional, empty if not available
     */
    protected Optional<HttpServletResponse> getServletResponse() {
        if (webServiceContext == null) {
            return Optional.empty();
        }

        var messageContext = webServiceContext.getMessageContext();
        var response = (HttpServletResponse) messageContext.get(MessageContext.SERVLET_RESPONSE);
        return Optional.ofNullable(response);
    }

    /**
     * Get the Rhythmyx session from the SOAP headers using modern JAX-WS APIs.
     *
     * @return the Rhythmyx session supplied with a message SOAP header
     * @throws SOAPException for any error looking up the Rhythmyx session SOAP header
     */
    protected Optional<String> getRhythmyxSession() throws SOAPException {
        if (webServiceContext == null) {
            return Optional.empty();
        }

        var messageContext = webServiceContext.getMessageContext();
        var soapMessage = (SOAPMessage) messageContext.get("jakarta.xml.ws.binding.soapMessage");

        if (soapMessage == null) {
            return Optional.empty();
        }

        var soapHeader = soapMessage.getSOAPHeader();
        if (soapHeader == null) {
            return Optional.empty();
        }

        return extractSessionFromHeader(soapHeader);
    }

    /**
     * Extract session information from SOAP header elements.
     *
     * @param soapHeader the SOAP header to search
     * @return Optional containing session string if found
     */
    private Optional<String> extractSessionFromHeader(SOAPHeader soapHeader) {
        var headerElements = soapHeader.getChildElements();

        while (headerElements.hasNext()) {
            var element = headerElements.next();
            if (!(element instanceof SOAPElement)) {
                continue;
            }

            var soapElement = (SOAPElement) element;
            var localName = soapElement.getElementName().getLocalName();

            // Try header format from Java Axis client
            if ("session".equals(localName)) {
                return Optional.ofNullable(soapElement.getValue());
            }

            // Try header format from Microsoft .NET client
            if ("PSAuthenticationHeader".equals(localName)) {
                var sessionValue = extractSessionFromAuthHeader(soapElement);
                if (sessionValue.isPresent()) {
                    return sessionValue;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Extract session from PSAuthenticationHeader element.
     *
     * @param authHeader the authentication header element
     * @return Optional containing session if found
     */
    private Optional<String> extractSessionFromAuthHeader(SOAPElement authHeader) {
        var children = authHeader.getChildElements();

        while (children.hasNext()) {
            var child = children.next();
            if (child instanceof SOAPElement) {
                var childElement = (SOAPElement) child;
                if ("Session".equals(childElement.getElementName().getLocalName())) {
                    return Optional.ofNullable(childElement.getValue());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Get all message attachments using modern JAX-WS attachment handling.
     *
     * @return a list of attachment parts, never null, may be empty
     */
    protected List<Object> getAttachments() {
        if (webServiceContext == null) {
            return Collections.emptyList();
        }

        var messageContext = webServiceContext.getMessageContext();
        var soapMessage = (SOAPMessage) messageContext.get("jakarta.xml.ws.binding.soapMessage");

        if (soapMessage == null) {
            return Collections.emptyList();
        }

        var attachments = new ArrayList<>();
        var attachmentIterator = soapMessage.getAttachments();

        while (attachmentIterator.hasNext()) {
            attachments.add(attachmentIterator.next());
        }

        return Collections.unmodifiableList(attachments);
    }

    /**
     * Authenticate the current message context using modern Optional-based approach.
     * This retrieves the required Rhythmyx session header from the message context and authenticates it.
     *
     * @return the authenticated Rhythmyx session
     * @throws PSInvalidSessionFault if the current message context does not contain a valid Rhythmyx session
     */
    protected String authenticate() throws PSInvalidSessionFault {
        try {
            var sessionOpt = getRhythmyxSession();
            if (sessionOpt.isEmpty()) {
                var code = WebserviceErrorCodes.MISSING_SESSION;
                logger.debug("Authentication Error: Missing session header");
                throw new PSInvalidSessionFault(code.numericCode(),
                    PSWebserviceErrors.createErrorMessage(code, "Missing session header"),
                    "Missing required Rhythmyx session header");
            }

            var sessionId = sessionOpt.get();
            var request = getServletRequest().orElseThrow(() ->
                new PSInvalidSessionFault(WebserviceErrorCodes.INVALID_SESSION.numericCode(),
                    "Servlet request not available", "No servlet request context"));

            PSSecurityFilter.authenticate(request, sessionId);
            return sessionId;

        } catch (LoginException | SOAPException ex) {
            var code = ex instanceof SOAPException
                ? WebserviceErrorCodes.MISSING_SESSION
                : WebserviceErrorCodes.INVALID_SESSION;

            logger.debug("Authentication Error Code: {}", code, ex);
            throw new PSInvalidSessionFault(code.numericCode(),
                PSWebserviceErrors.createErrorMessage(code, ex.toString()),
                ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * Get the remote user using modern Optional-based approach.
     *
     * @return Optional containing the remote user, empty if not authenticated
     */
    protected Optional<String> getRemoteUser() {
        return getServletRequest()
            .map(request -> {
                if (request instanceof PSServletRequestWrapper) {
                    return ((PSServletRequestWrapper) request).getRemoteUser();
                }
                return request.getRemoteUser();
            });
    }

    /**
     * Converts the supplied source object to the specified type using modern type safety.
     *
     * @param <T> the target type
     * @param type the class type to which to convert the supplied source, not null
     * @param source the object to convert, not null
     * @return the transformed object, never null
     * @throws IllegalArgumentException if source is null
     */
    protected <T> T convert(Class<T> type, Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }

        var factory = PSTransformerFactory.getInstance();
        var converter = factory.getConverter(type);

        return converter.convert(type, source);
    }

    /**
     * Extract the boolean value from the supplied Boolean object with modern Optional handling.
     *
     * @param value the Boolean value from which to extract the boolean value
     * @param defaultValue the default value to be returned if the supplied value is null
     * @return the extracted boolean value or the specified default
     */
    protected boolean extractBooleanValue(Boolean value, boolean defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
    }

    /**
     * Convenience method. Converts IllegalArgumentException to PSContractViolationFault
     * using modern exception handling patterns.
     *
     * @param e the exception, not null
     * @param serviceName the service name, not null
     * @throws PSContractViolationFault the converted exception
     * @throws IllegalArgumentException if parameters are null
     */
    protected void handleInvalidContract(IllegalArgumentException e, String serviceName)
        throws PSContractViolationFault {

        if (e == null) {
            throw new IllegalArgumentException("e may not be null.");
        }
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName may not be null.");
        }

        var code = WebserviceErrorCodes.INVALID_CONTRACT;
        logger.error("SOAP Invalid Contract for service {}", serviceName, e);

        throw new PSContractViolationFault(code.numericCode(),
            PSWebserviceErrors.createErrorMessage(code, serviceName, e.toString()),
            ExceptionUtils.getStackTrace(e));
    }

    /**
     * Converts RuntimeException into the correct fault using modern exception handling.
     * If the cause was a PSAuthorizationException it is converted to a PSNotAuthorizedFault,
     * otherwise it is converted to a RemoteException.
     *
     * @param e the runtime exception to convert, not null
     * @param serviceName the name of the service which caused the supplied exception, not null
     * @throws PSNotAuthorizedFault if the root cause was a PSAuthorizationException
     * @throws RemoteException for all other runtime exceptions
     * @throws IllegalArgumentException if parameters are null
     */
    protected void handleRuntimeException(RuntimeException e, String serviceName)
        throws PSNotAuthorizedFault, RemoteException {

        if (e == null) {
            throw new IllegalArgumentException("e cannot be null");
        }
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName cannot be null");
        }

        if (e.getCause() instanceof PSAuthorizationException) {
            logger.debug("SOAP PSAuthorizationException for service {}", serviceName, e);
            var code = WebserviceErrorCodes.NOT_AUTHORIZED;
            var remoteUser = getRemoteUser().orElse("unknown");

            throw new PSNotAuthorizedFault(code.numericCode(),
                PSWebserviceErrors.createErrorMessage(code, remoteUser, serviceName, e.toString()),
                ExceptionUtils.getStackTrace(e));
        } else {
            logger.error("SOAP RuntimeException for service {}", serviceName, e);
        }

        throw new RemoteException(e.toString(), e);
    }

    /**
     * Convenience method. Converts PSErrorResultsException to PSErrorResultsFault
     * using modern exception handling.
     *
     * @param e the exception to be converted, not null
     * @param serviceName the service name, not null
     * @throws RemoteException if error occurred during conversion
     * @throws PSErrorResultsFault when the supplied exception must be raised as a SOAP fault.
     * @throws IllegalArgumentException if parameters are null
     */
    protected void handleErrorResultsException(PSErrorResultsException e, String serviceName)
        throws RemoteException, PSErrorResultsFault {

        if (e == null) {
            throw new IllegalArgumentException("e may not be null.");
        }
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName may not be null.");
        }

        logger.debug("SOAP PSErrorResultsException for service {}", serviceName, e);
        var fault = convert(PSErrorResultsFault.class, e);
        fault.setService(serviceName);

        throw fault;
    }

    /**
     * Convenience method. Converts PSErrorsException to PSErrorsFault
     * using modern exception handling.
     *
     * @param e the exception to be converted, not null
     * @param serviceName the service name, not null
     * @throws RemoteException if error occurred during conversion
     * @throws PSErrorsFault when the supplied exception must be raised as a SOAP fault.
     * @throws IllegalArgumentException if parameters are null
     */
    protected void handleErrorsException(PSErrorsException e, String serviceName)
        throws RemoteException, PSErrorsFault {

        if (e == null) {
            throw new IllegalArgumentException("e may not be null.");
        }
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName may not be null.");
        }

        logger.debug("SOAP PSErrorsException for service {}", serviceName, e);
        var fault = convert(PSErrorsFault.class, e);
        fault.setService(serviceName);

        throw fault;
    }

    /**
     * Convenience method, converts PSLockErrorException to PSLockFault
     * and throws the converted exception using modern patterns.
     *
     * @param e the exception to be converted, not null
     * @throws PSLockFault if successfully converted the exception
     * @throws RemoteException if failed to convert the exception
     */
    protected void handleLockError(PSLockErrorException e)
        throws PSLockFault, RemoteException {

        logger.debug("SOAP PSLockErrorException", e);
        throw convert(PSLockFault.class, e);
    }

    /**
     * Converts the value of relationship category from webservice to
     * objectstore (which is defined in PSRelationshipConfig.CATEGORY_XXX).
     *
     * @param cat the category to be converted, may be null
     * @return Optional containing the converted category, empty if input is null
     * @throws IllegalArgumentException if the supplied category does not match any pre-defined values
     */
    protected Optional<String> getRelationshipCategory(RelationshipCategory cat) {
        if (cat == null) {
            return Optional.empty();
        }

        var categoryValue = cat == null ? null : cat.toString();

        if ("ActiveAssembly".equalsIgnoreCase(categoryValue) || "activeassembly".equalsIgnoreCase(categoryValue)) {
            return Optional.of(CATEGORY_ACTIVE_ASSEMBLY);
        }
        if ("Folder".equalsIgnoreCase(categoryValue) || "folder".equalsIgnoreCase(categoryValue)) {
            return Optional.of(CATEGORY_FOLDER);
        }
        if ("Promotable".equalsIgnoreCase(categoryValue) || "promotable".equalsIgnoreCase(categoryValue)) {
            return Optional.of(CATEGORY_PROMOTABLE);
        }
        if ("Copy".equalsIgnoreCase(categoryValue) || "copy".equalsIgnoreCase(categoryValue)) {
            return Optional.of(CATEGORY_COPY);
        }

        throw new IllegalArgumentException(
            "Relationship Category must match one of the pre-defined values in RelationshipCategory if not null.");
    }

    // Category constants defined in com.percussion.design.objectstore.PSRelationshipConfig
    private static final String CATEGORY_ACTIVE_ASSEMBLY =
        com.percussion.design.objectstore.PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY;
    private static final String CATEGORY_FOLDER =
        com.percussion.design.objectstore.PSRelationshipConfig.CATEGORY_FOLDER;
    private static final String CATEGORY_PROMOTABLE =
        com.percussion.design.objectstore.PSRelationshipConfig.CATEGORY_PROMOTABLE;
    private static final String CATEGORY_COPY =
        com.percussion.design.objectstore.PSRelationshipConfig.CATEGORY_COPY;
}
