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
package com.percussion.services.workflow;

import java.util.Map;
import java.util.Set;

/**
 * An enumeration of possible error conditions for the workflow service with Java 11 modernization.
 * Each message code enumerated here must correspond to a message in the bundle for this package.
 *
 * <p><strong>Phase 2b bridge:</strong> package-local ints (1–10) are cataloged in {@code
 * com.intsof.percussioncms.auditlog.codes.WorkflowErrorCodes} with explicit {@code isAuditable}
 * (access-denied / invalid-transition / assignment dual-write; load/config noise does not). Int
 * literals remain here so they stay <em>compile-time constants</em>. Prefer {@code
 * WorkflowErrorCodes} / {@code LegacyErrorCodeRegistry} for dual-write decisions; keep this
 * interface for legacy exception constructors and message bundles.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Immutable collections for error metadata</li>
 * <li>Enhanced documentation with proper error context</li>
 * <li>Type-safe error code constants</li>
 * <li>Utility methods for error validation</li>
 * </ul>
 *
 * @author dougrand
 */
public interface IPSWorkflowErrors {

    /**
     * Failed to load workflow.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The workflow ID</li>
     * </ul>
     */
    int WORKFLOW_NOT_FOUND = 1;

    /**
     * Failed to load workflow state.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The state ID</li>
     * <li>Arg 1: The workflow ID</li>
     * </ul>
     */
    int STATE_NOT_FOUND = 2;

    /**
     * Backwards-compatible alias used by older components referencing the error name
     * ERROR_LOADING_WORKFLOW_STATE. This maps to STATE_NOT_FOUND for clarity.
     */
    int ERROR_LOADING_WORKFLOW_STATE = STATE_NOT_FOUND;

    /**
     * Invalid workflow state operation.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The state ID</li>
     * <li>Arg 1: The workflow ID</li>
     * <li>Arg 2: The reason for invalidity</li>
     * </ul>
     */
    int INVALID_STATE = 3;

    /**
     * Workflow operation failed.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The operation name</li>
     * <li>Arg 1: The error details</li>
     * </ul>
     */
    int OPERATION_FAILED = 4;

    /**
     * Workflow validation failed.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The workflow ID</li>
     * <li>Arg 1: The validation message</li>
     * </ul>
     */
    int VALIDATION_FAILED = 5;

    /**
     * Access denied to workflow resource.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The workflow ID</li>
     * <li>Arg 1: The user name</li>
     * </ul>
     */
    int ACCESS_DENIED = 6;

    /**
     * Transition not found.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The transition ID</li>
     * <li>Arg 1: The state ID</li>
     * <li>Arg 2: The workflow ID</li>
     * </ul>
     */
    int TRANSITION_NOT_FOUND = 7;

    /**
     * Invalid transition attempt.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The transition ID</li>
     * <li>Arg 1: The current state ID</li>
     * <li>Arg 2: The reason for invalidity</li>
     * </ul>
     */
    int INVALID_TRANSITION = 8;

    /**
     * Workflow configuration error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The configuration issue description</li>
     * </ul>
     */
    int CONFIGURATION_ERROR = 9;

    /**
     * Content item workflow assignment error.
     * <p>
     * The arguments passed in for this message are:
     * <ul>
     * <li>Arg 0: The content ID</li>
     * <li>Arg 1: The workflow ID</li>
     * <li>Arg 2: The assignment error details</li>
     * </ul>
     */
    int ASSIGNMENT_ERROR = 10;

    /**
     * Immutable set of all defined error codes for validation purposes.
     */
    Set<Integer> ALL_ERROR_CODES = Set.of(
        WORKFLOW_NOT_FOUND,
        STATE_NOT_FOUND,
        INVALID_STATE,
        OPERATION_FAILED,
        VALIDATION_FAILED,
        ACCESS_DENIED,
        TRANSITION_NOT_FOUND,
        INVALID_TRANSITION,
        CONFIGURATION_ERROR,
        ASSIGNMENT_ERROR
    );

    /**
     * Error code metadata for enhanced error handling.
     */
    Map<Integer, String> ERROR_DESCRIPTIONS = Map.of(
        WORKFLOW_NOT_FOUND, "Workflow not found",
        STATE_NOT_FOUND, "Workflow state not found",
        INVALID_STATE, "Invalid workflow state operation",
        OPERATION_FAILED, "Workflow operation failed",
        VALIDATION_FAILED, "Workflow validation failed",
        ACCESS_DENIED, "Access denied to workflow resource",
        TRANSITION_NOT_FOUND, "Workflow transition not found",
        INVALID_TRANSITION, "Invalid workflow transition",
        CONFIGURATION_ERROR, "Workflow configuration error",
        ASSIGNMENT_ERROR, "Content workflow assignment error"
    );

    /**
     * Check if the given error code is a valid workflow error.
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
     * Get all available error codes as an immutable set.
     *
     * @return immutable set of all error codes
     */
    static Set<Integer> getAllErrorCodes() {
        return ALL_ERROR_CODES;
    }
}
