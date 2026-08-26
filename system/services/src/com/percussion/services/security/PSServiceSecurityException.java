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
package com.percussion.services.security;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;
import com.intsof.percussioncms.auditlog.codes.ServiceSecurityErrorCodes;

/**
 * Modern security exception class with enhanced error context and factory methods for Java 11.
 * This exception indicates security-related errors within the Percussion CMS security subsystem
 * with comprehensive error handling capabilities and OWASP-compliant patterns.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Factory methods for common security error scenarios</li>
 * <li>Optional-based safe access for error context information</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>OWASP-compliant error handling without sensitive data exposure</li>
 * <li>Immutable error context with comprehensive logging support</li>
 * </ul>
 *
 * <p>This exception uses resource bundle-based internationalized messages
 * from {@code PSSecurityErrorStringBundle.properties} and provides enhanced
 * error handling capabilities with modern Java patterns.</p>
 */
public class PSServiceSecurityException extends PSBaseException {

    private static final long serialVersionUID = 4892025838027092729L;

    /**
     * Optional security context associated with this exception.
     */
    private final String securityContext;

    /**
     * Optional object ID associated with this security exception.
     */
    private final IPSGuid objectId;

    /**
     * Creates a security exception with the specified message code.
     *
     * @param msgCode the message code for resource bundle lookup
     */
    public PSServiceSecurityException(int msgCode) {
        super(msgCode);
        this.securityContext = null;
        this.objectId = null;
    }

    /**
     * Creates a security exception with message code and arguments.
     *
     * @param msgCode the message code for resource bundle lookup
     * @param arrayArgs the arguments for message formatting, may be empty
     */
    public PSServiceSecurityException(int msgCode, Object... arrayArgs) {
        super(msgCode, arrayArgs);
        this.securityContext = null;
        this.objectId = null;
    }

    /**
     * Creates a security exception with message code, cause, and arguments.
     *
     * @param msgCode the message code for resource bundle lookup
     * @param cause the underlying cause of this exception
     * @param arrayArgs the arguments for message formatting, may be empty
     */
    public PSServiceSecurityException(int msgCode, Throwable cause, Object... arrayArgs) {
        super(msgCode, cause, arrayArgs);
        this.securityContext = null;
        this.objectId = null;
    }

    /**
     * Typed construction from a catalogued {@link IPSErrorCode}.
     *
     * @param code catalogued error code, never {@code null}
     * @param arrayArgs the arguments for message formatting, may be empty
     */
    public PSServiceSecurityException(IPSErrorCode code, Object... arrayArgs) {
        super(code, arrayArgs);
        this.securityContext = null;
        this.objectId = null;
    }

    /**
     * Typed construction with a cause.
     *
     * @param code catalogued error code, never {@code null}
     * @param cause the underlying cause of this exception
     * @param arrayArgs the arguments for message formatting, may be empty
     */
    public PSServiceSecurityException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
        super(code, cause, arrayArgs);
        this.securityContext = null;
        this.objectId = null;
    }

    /**
     * Private constructor for factory methods with enhanced context.
     *
     * @param msgCode the message code for resource bundle lookup
     * @param cause the underlying cause, may be {@code null}
     * @param securityContext the security context, may be {@code null}
     * @param objectId the object ID context, may be {@code null}
     * @param arrayArgs the arguments for message formatting
     */
    private PSServiceSecurityException(int msgCode, Throwable cause, String securityContext,
                               IPSGuid objectId, Object... arrayArgs) {
        super(msgCode, cause, arrayArgs);
        this.securityContext = securityContext;
        this.objectId = objectId;
    }

    private PSServiceSecurityException(IPSErrorCode code, Throwable cause, String securityContext,
                               IPSGuid objectId, Object... arrayArgs) {
        super(code, cause, arrayArgs);
        this.securityContext = securityContext;
        this.objectId = objectId;
    }

    /**
     * Factory method for access denied errors.
     *
     * @param objectId the ID of the object access was denied to, not {@code null}
     * @param userName the user attempting access, may be {@code null}
     * @return a new PServiceSecurityException instance
     * @throws IllegalArgumentException if objectId is null
     */
    public static PSServiceSecurityException accessDenied(IPSGuid objectId, String userName) {
        Objects.requireNonNull(objectId, "objectId cannot be null");
        var user = userName != null ? userName : "Unknown user";
        return new PSServiceSecurityException(ServiceSecurityErrorCodes.ACCESS_DENIED, null,
                                     "Access denied", objectId, objectId, user);
    }

    /**
     * Factory method for authentication failures.
     *
     * @param userName the user name that failed authentication, may be {@code null}
     * @param reason the reason for authentication failure, may be {@code null}
     * @return a new PServiceSecurityException instance
     */
    public static PSServiceSecurityException authenticationFailed(String userName, String reason) {
        var user = userName != null ? userName : "Unknown user";
        var failureReason = reason != null ? reason : "Authentication failed";
        return new PSServiceSecurityException(ServiceSecurityErrorCodes.AUTHENTICATION_FAILED, null,
                                     "Authentication failure", null, user, failureReason);
    }

    /**
     * Factory method for authorization failures.
     *
     * @param userName the user name, not {@code null}
     * @param operation the operation that was attempted, not {@code null}
     * @return a new PServiceSecurityException instance
     * @throws IllegalArgumentException if userName or operation is null
     */
    public static PSServiceSecurityException authorizationFailed(String userName, String operation) {
        Objects.requireNonNull(userName, "userName cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        return new PSServiceSecurityException(ServiceSecurityErrorCodes.AUTHORIZATION_FAILED, null,
                                     "Authorization failure", null, userName, operation);
    }

    /**
     * Factory method for ACL operation errors.
     *
     * @param operation the ACL operation that failed, not {@code null}
     * @param cause the underlying cause, not {@code null}
     * @param objectId the object ID context, may be {@code null}
     * @return a new PServiceSecurityException instance
     * @throws IllegalArgumentException if operation or cause is null
     */
    public static PSServiceSecurityException aclOperationFailed(String operation, Throwable cause, IPSGuid objectId) {
        Objects.requireNonNull(operation, "operation cannot be null");
        Objects.requireNonNull(cause, "cause cannot be null");
        return new PSServiceSecurityException(ServiceSecurityErrorCodes.ACL_OPERATION_FAILED, cause,
                                     "ACL operation failure", objectId, operation, cause.getMessage());
    }

    /**
     * Factory method for security configuration errors.
     *
     * @param configurationIssue the configuration issue description, not {@code null}
     * @return a new PServiceSecurityException instance
     * @throws IllegalArgumentException if configurationIssue is null
     */
    public static PSServiceSecurityException configurationError(String configurationIssue) {
        Objects.requireNonNull(configurationIssue, "configurationIssue cannot be null");
        return new PSServiceSecurityException(ServiceSecurityErrorCodes.CONFIGURATION_ERROR, null,
                                     "Security configuration error", null, configurationIssue);
    }

    /**
     * Factory method for role management errors.
     *
     * @param roleName the role name that caused the error, not {@code null}
     * @param operation the role operation that failed, not {@code null}
     * @param cause the underlying cause, may be {@code null}
     * @return a new PServiceSecurityException instance
     * @throws IllegalArgumentException if roleName or operation is null
     */
    public static PSServiceSecurityException roleManagementError(String roleName, String operation, Throwable cause) {
        Objects.requireNonNull(roleName, "roleName cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        return new PSServiceSecurityException(ServiceSecurityErrorCodes.ROLE_MANAGEMENT_ERROR, cause,
                                     "Role management error", null, roleName, operation);
    }

    /**
     * Factory method for principal validation errors.
     *
     * @param principalName the principal name, not {@code null}
     * @param validationMessage the validation error message, not {@code null}
     * @return a new PServiceSecurityException instance
     * @throws IllegalArgumentException if principalName or validationMessage is null
     */
    public static PSServiceSecurityException principalValidationError(String principalName, String validationMessage) {
        Objects.requireNonNull(principalName, "principalName cannot be null");
        Objects.requireNonNull(validationMessage, "validationMessage cannot be null");
        return new PSServiceSecurityException(ServiceSecurityErrorCodes.PRINCIPAL_VALIDATION_ERROR, null,
                                     "Principal validation error", null, principalName, validationMessage);
    }

    /**
     * Get the security context associated with this exception.
     *
     * @return an Optional containing the security context if available, empty otherwise
     */
    public Optional<String> getSecurityContext() {
        return Optional.ofNullable(securityContext);
    }

    /**
     * Get the object ID associated with this exception.
     *
     * @return an Optional containing the object ID if available, empty otherwise
     */
    public Optional<IPSGuid> getObjectId() {
        return Optional.ofNullable(objectId);
    }

    /**
     * Check if this exception has security context information.
     *
     * @return {@code true} if security context is available, {@code false} otherwise
     */
    public boolean hasSecurityContext() {
        return securityContext != null;
    }

    /**
     * Check if this exception has object context information.
     *
     * @return {@code true} if object ID is available, {@code false} otherwise
     */
    public boolean hasObjectContext() {
        return objectId != null;
    }

    /**
     * Get a formatted error context string including security and object information if available.
     *
     * @return a formatted context string, never {@code null}
     */
    public String getErrorContext() {
        var context = new StringBuilder();
        if (securityContext != null) {
            context.append("Security Context: ").append(securityContext);
        }
        if (objectId != null) {
            if (context.length() > 0) {
                context.append(", ");
            }
            context.append("Object ID: ").append(objectId);
        }
        return context.length() > 0 ? context.toString() : "No context available";
    }

    /**
     * Get a safe error message that doesn't expose sensitive information (OWASP compliant).
     *
     * @return a safe error message for external display, never {@code null}
     */
    public String getSafeErrorMessage() {
        // Return generic message without sensitive details for OWASP compliance
        return hasSecurityContext()
            ? "Security operation failed: " + securityContext
            : "Security operation failed";
    }

    @Override
    protected String getResourceBundleBaseName() {
        return "com.percussion.services.security.PSSecurityErrorStringBundle";
    }

    @Override
    public String toString() {
        var baseString = super.toString();
        var context = getErrorContext();
        return context.equals("No context available") ? baseString : baseString + " [" + context + "]";
    }
}
