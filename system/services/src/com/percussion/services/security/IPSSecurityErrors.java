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

import java.util.Map;
import java.util.Set;

/**
 * Error codes for security exceptions with Java 11 modernization.
 * Each message code enumerated here must correspond to a message in the
 * PSSecurityErrorStringBundle.properties resource bundle.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Immutable collections for error metadata</li>
 * <li>Enhanced documentation with proper error context</li>
 * <li>Type-safe error code constants</li>
 * <li>Utility methods for error validation and categorization</li>
 * </ul>
 */
public interface IPSSecurityErrors {

    /**
     * Missing community error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The ID of the missing Community</li>
     * </ul>
     */
    int MISSING_COMMUNITY = 1;

    /**
     * ACL not found for specified ACL ID.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The ACL ID</li>
     * </ul>
     */
    int ACL_NOT_FOUND = 2;

    /**
     * ACL not found for specified object GUID.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The object GUID value</li>
     * <li>Arg 1: The object type name</li>
     * </ul>
     */
    int OBJECT_ACL_NOT_FOUND = 3;

    /**
     * Error saving an ACL.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The ACL GUID</li>
     * <li>Arg 1: The error details</li>
     * </ul>
     */
    int ACL_SAVE_ERROR = 4;

    /**
     * Error deleting an ACL.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The ACL GUID</li>
     * <li>Arg 1: The error details</li>
     * </ul>
     */
    int ACL_DELETE_ERROR = 5;

    /**
     * Access denied exception.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The object GUID</li>
     * <li>Arg 1: The user name</li>
     * </ul>
     */
    int ACCESS_DENIED = 6;

    /**
     * Authentication failed error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The user name</li>
     * <li>Arg 1: The failure reason</li>
     * </ul>
     */
    int AUTHENTICATION_FAILED = 7;

    /**
     * Authorization failed error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The user name</li>
     * <li>Arg 1: The operation attempted</li>
     * </ul>
     */
    int AUTHORIZATION_FAILED = 8;

    /**
     * ACL operation failed error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The operation name</li>
     * <li>Arg 1: The error details</li>
     * </ul>
     */
    int ACL_OPERATION_FAILED = 9;

    /**
     * Security configuration error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The configuration issue description</li>
     * </ul>
     */
    int CONFIGURATION_ERROR = 10;

    /**
     * Role management error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The role name</li>
     * <li>Arg 1: The operation that failed</li>
     * </ul>
     */
    int ROLE_MANAGEMENT_ERROR = 11;

    /**
     * Principal validation error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The principal name</li>
     * <li>Arg 1: The validation message</li>
     * </ul>
     */
    int PRINCIPAL_VALIDATION_ERROR = 12;

    /**
     * Session security error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The session ID</li>
     * <li>Arg 1: The security violation details</li>
     * </ul>
     */
    int SESSION_SECURITY_ERROR = 13;

    /**
     * Security policy violation error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The policy name</li>
     * <li>Arg 1: The violation details</li>
     * </ul>
     */
    int SECURITY_POLICY_VIOLATION = 14;

    /**
     * Immutable set of all defined error codes for validation purposes.
     */
    Set<Integer> ALL_ERROR_CODES = Set.of(
        MISSING_COMMUNITY,
        ACL_NOT_FOUND,
        OBJECT_ACL_NOT_FOUND,
        ACL_SAVE_ERROR,
        ACL_DELETE_ERROR,
        ACCESS_DENIED,
        AUTHENTICATION_FAILED,
        AUTHORIZATION_FAILED,
        ACL_OPERATION_FAILED,
        CONFIGURATION_ERROR,
        ROLE_MANAGEMENT_ERROR,
        PRINCIPAL_VALIDATION_ERROR,
        SESSION_SECURITY_ERROR,
        SECURITY_POLICY_VIOLATION
    );

    /**
     * Error code metadata for enhanced error handling.
     */
    Map<Integer, String> ERROR_DESCRIPTIONS = Map.of(
        MISSING_COMMUNITY, "Missing community",
        ACL_NOT_FOUND, "ACL not found",
        OBJECT_ACL_NOT_FOUND, "Object ACL not found",
        ACL_SAVE_ERROR, "ACL save error",
        ACL_DELETE_ERROR, "ACL delete error",
        ACCESS_DENIED, "Access denied",
        AUTHENTICATION_FAILED, "Authentication failed",
        AUTHORIZATION_FAILED, "Authorization failed",
        ACL_OPERATION_FAILED, "ACL operation failed",
        CONFIGURATION_ERROR, "Security configuration error",
        ROLE_MANAGEMENT_ERROR, "Role management error",
        PRINCIPAL_VALIDATION_ERROR, "Principal validation error",
        SESSION_SECURITY_ERROR, "Session security error",
        SECURITY_POLICY_VIOLATION, "Security policy violation"
    );

    /**
     * Critical security error codes that require immediate attention.
     */
    Set<Integer> CRITICAL_ERRORS = Set.of(
        ACCESS_DENIED,
        AUTHENTICATION_FAILED,
        AUTHORIZATION_FAILED,
        SECURITY_POLICY_VIOLATION,
        SESSION_SECURITY_ERROR
    );

    /**
     * ACL-related error codes.
     */
    Set<Integer> ACL_ERRORS = Set.of(
        ACL_NOT_FOUND,
        OBJECT_ACL_NOT_FOUND,
        ACL_SAVE_ERROR,
        ACL_DELETE_ERROR,
        ACL_OPERATION_FAILED
    );

    /**
     * Configuration-related error codes.
     */
    Set<Integer> CONFIGURATION_ERRORS = Set.of(
        CONFIGURATION_ERROR,
        MISSING_COMMUNITY
    );

    /**
     * Check if the given error code is a valid security error.
     *
     * @param errorCode the error code to validate
     * @return {@code true} if the error code is valid, {@code false} otherwise
     */
    static boolean isValidErrorCode(int errorCode) {
        return ALL_ERROR_CODES.contains(errorCode);
    }

    /**
     * Get the description for the given error code.
     *
     * @param errorCode the error code
     * @return the error description, or "Unknown error" if not found
     */
    static String getErrorDescription(int errorCode) {
        return ERROR_DESCRIPTIONS.getOrDefault(errorCode, "Unknown error");
    }

    /**
     * Check if an error code represents a critical security issue.
     *
     * @param errorCode the error code to check
     * @return {@code true} if the error is critical, {@code false} otherwise
     */
    static boolean isCriticalError(int errorCode) {
        return CRITICAL_ERRORS.contains(errorCode);
    }

    /**
     * Check if an error code is ACL-related.
     *
     * @param errorCode the error code to check
     * @return {@code true} if the error is ACL-related, {@code false} otherwise
     */
    static boolean isAclError(int errorCode) {
        return ACL_ERRORS.contains(errorCode);
    }

    /**
     * Check if an error code is configuration-related.
     *
     * @param errorCode the error code to check
     * @return {@code true} if the error is configuration-related, {@code false} otherwise
     */
    static boolean isConfigurationError(int errorCode) {
        return CONFIGURATION_ERRORS.contains(errorCode);
    }

    /**
     * Get all available error codes as an immutable set.
     *
     * @return immutable set of all error codes
     */
    static Set<Integer> getAllErrorCodes() {
        return ALL_ERROR_CODES;
    }

    /**
     * Get all critical error codes as an immutable set.
     *
     * @return immutable set of critical error codes
     */
    static Set<Integer> getCriticalErrorCodes() {
        return CRITICAL_ERRORS;
    }
}
