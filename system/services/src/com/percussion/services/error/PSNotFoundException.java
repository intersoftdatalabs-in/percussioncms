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
package com.percussion.services.error;

import com.percussion.error.PSException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Modern Java 11 exception for object not found scenarios.
 *
 * <p>This exception is thrown when attempting to locate an object by ID, name, or other
 * criteria, but the specified object does not exist in the system. It provides both
 * traditional exception handling and modern Java 11 patterns with Optional support
 * and enhanced validation.
 *
 * <p>The exception supports multiple construction patterns:
 * <ul>
 *   <li><strong>By GUID:</strong> When searching for objects by their unique identifier</li>
 *   <li><strong>By name and type:</strong> When searching for objects by name within a specific type</li>
 *   <li><strong>By numeric ID:</strong> When working with legacy numeric identifiers</li>
 *   <li><strong>Direct message:</strong> For custom error scenarios</li>
 * </ul>
 *
 * <p>All constructors include comprehensive validation and provide meaningful error messages
 * following Java 11 best practices.
 *
 * @author Yu-Bing Chen
 * @since Java 11 Modernization
 */
public class PSNotFoundException extends PSException {

    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Message key for GUID-based not found errors.
     */
    private static final String MSG_KEY_NOT_FOUND_BY_ID = "service.exception@NotFoundById";

    /**
     * Message key for name-based not found errors.
     */
    private static final String MSG_KEY_NOT_FOUND_BY_NAME = "service.exception@NotFoundByName";

    /**
     * Message key for numeric ID-based not found errors.
     */
    private static final String MSG_KEY_NOT_FOUND_BY_NUMERIC_ID = "service.exception@NotFoundByNumericId";

    /**
     * The GUID that was not found, if applicable.
     */
    private final IPSGuid notFoundGuid;

    /**
     * The name that was not found, if applicable.
     */
    private final String notFoundName;

    /**
     * The type of object that was not found, if applicable.
     */
    private final PSTypeEnum notFoundType;

    /**
     * The numeric ID that was not found, if applicable.
     */
    private final Integer notFoundNumericId;

    /**
     * Creates an exception for an object that cannot be found by GUID.
     *
     * @param id the GUID of the non-existent object, must not be null
     * @throws IllegalArgumentException if id is null
     */
    public PSNotFoundException(IPSGuid id) {
        Objects.requireNonNull(id, "GUID cannot be null");

        this.notFoundGuid = id;
        this.notFoundName = null;
        this.notFoundType = PSTypeEnum.valueOf(id.getType());
        this.notFoundNumericId = null;

        var args = new Object[]{id.longValue(), notFoundType != null ? notFoundType.getDisplayName() : "Unknown"};
        super.setMsgKeyAndArgs(MSG_KEY_NOT_FOUND_BY_ID, args);
    }

    /**
     * Creates an exception for an object that cannot be found by numeric ID.
     *
     * @param id the numeric ID of the non-existent object, must be positive
     * @throws IllegalArgumentException if id is not positive
     */
    public PSNotFoundException(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Numeric ID must be positive: " + id);
        }

        this.notFoundGuid = null;
        this.notFoundName = null;
        this.notFoundType = null;
        this.notFoundNumericId = id;

        var args = new Object[]{id};
        super.setMsgKeyAndArgs(MSG_KEY_NOT_FOUND_BY_NUMERIC_ID, args);
    }

    /**
     * Creates an exception for an object that cannot be found by name and type.
     *
     * @param name the lookup name, may be null or empty
     * @param type the type of the object, must not be null
     * @throws IllegalArgumentException if type is null
     */
    public PSNotFoundException(String name, PSTypeEnum type) {
        Objects.requireNonNull(type, "Object type cannot be null");

        this.notFoundGuid = null;
        this.notFoundName = name;
        this.notFoundType = type;
        this.notFoundNumericId = null;

        var args = new Object[]{name != null ? name : "<null>", type.getDisplayName()};
        super.setMsgKeyAndArgs(MSG_KEY_NOT_FOUND_BY_NAME, args);
    }

    /**
     * Creates an exception with a custom error message.
     *
     * @param errorMsg the custom error message, may be null
     */
    public PSNotFoundException(String errorMsg) {
        super(errorMsg);
        this.notFoundGuid = null;
        this.notFoundName = null;
        this.notFoundType = null;
        this.notFoundNumericId = null;
    }

    /**
     * Creates an exception with a custom error message and cause.
     *
     * @param errorMsg the custom error message, may be null
     * @param cause the cause of the exception, may be null
     */
    public PSNotFoundException(String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.notFoundGuid = null;
        this.notFoundName = null;
        this.notFoundType = null;
        this.notFoundNumericId = null;
    }

    /**
     * Gets the GUID that was not found.
     *
     * @return an Optional containing the GUID, or empty if not applicable
     */
    public Optional<IPSGuid> getNotFoundGuid() {
        return Optional.ofNullable(notFoundGuid);
    }

    /**
     * Gets the name that was not found.
     *
     * @return an Optional containing the name, or empty if not applicable
     */
    public Optional<String> getNotFoundName() {
        return Optional.ofNullable(notFoundName);
    }

    /**
     * Gets the type of object that was not found.
     *
     * @return an Optional containing the type, or empty if not applicable
     */
    public Optional<PSTypeEnum> getNotFoundType() {
        return Optional.ofNullable(notFoundType);
    }

    /**
     * Gets the numeric ID that was not found.
     *
     * @return an Optional containing the numeric ID, or empty if not applicable
     */
    public Optional<Integer> getNotFoundNumericId() {
        return Optional.ofNullable(notFoundNumericId);
    }

    /**
     * Checks if this exception was caused by a GUID-based lookup failure.
     *
     * @return true if the exception was caused by a failed GUID lookup, false otherwise
     */
    public boolean isGuidBasedFailure() {
        return notFoundGuid != null;
    }

    /**
     * Checks if this exception was caused by a name-based lookup failure.
     *
     * @return true if the exception was caused by a failed name lookup, false otherwise
     */
    public boolean isNameBasedFailure() {
        return notFoundName != null;
    }

    /**
     * Checks if this exception was caused by a numeric ID-based lookup failure.
     *
     * @return true if the exception was caused by a failed numeric ID lookup, false otherwise
     */
    public boolean isNumericIdBasedFailure() {
        return notFoundNumericId != null;
    }

    /**
     * Creates a not found exception for the specified GUID with validation.
     *
     * @param id the GUID of the non-existent object, must not be null
     * @return a new PSNotFoundException for the GUID
     * @throws IllegalArgumentException if id is null
     */
    public static PSNotFoundException forGuid(IPSGuid id) {
        return new PSNotFoundException(id);
    }

    /**
     * Creates a not found exception for the specified name and type with validation.
     *
     * @param name the lookup name, may be null or empty
     * @param type the type of the object, must not be null
     * @return a new PSNotFoundException for the name and type
     * @throws IllegalArgumentException if type is null
     */
    public static PSNotFoundException forNameAndType(String name, PSTypeEnum type) {
        return new PSNotFoundException(name, type);
    }

    /**
     * Creates a not found exception for the specified numeric ID with validation.
     *
     * @param id the numeric ID of the non-existent object, must be positive
     * @return a new PSNotFoundException for the numeric ID
     * @throws IllegalArgumentException if id is not positive
     */
    public static PSNotFoundException forNumericId(int id) {
        return new PSNotFoundException(id);
    }

    /**
     * Creates a not found exception with a formatted message.
     *
     * @param messageTemplate the message template with placeholders, must not be null
     * @param args the arguments for formatting, may be empty
     * @return a new PSNotFoundException with the formatted message
     * @throws IllegalArgumentException if messageTemplate is null
     */
    public static PSNotFoundException withFormattedMessage(String messageTemplate, Object... args) {
        Objects.requireNonNull(messageTemplate, "Message template cannot be null");
        var formattedMessage = String.format(messageTemplate, args);
        return new PSNotFoundException(formattedMessage);
    }

    @Override
    public String toString() {
        var className = getClass().getSimpleName();
        var message = getLocalizedMessage();

        if (notFoundGuid != null) {
            return String.format("%s: Object with GUID %s (type: %s) not found",
                    className, notFoundGuid, notFoundType.getDisplayName());
        } else if (notFoundName != null) {
            return String.format("%s: Object with name '%s' (type: %s) not found",
                    className, notFoundName, notFoundType.getDisplayName());
        } else if (notFoundNumericId != null) {
            return String.format("%s: Object with ID %d not found", className, notFoundNumericId);
        } else {
            return message != null ? className + ": " + message : className;
        }
    }
}
