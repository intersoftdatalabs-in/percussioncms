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

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Modern Java 11 exception for duplicate name scenarios.
 *
 * <p>This exception is thrown when attempting to save an object whose name
 * conflicts with an existing object of the same type. It provides comprehensive
 * context about the duplicate name conflict including the conflicting object's
 * GUID, name, and type information.
 *
 * <p>The exception supports multiple construction patterns:
 * <ul>
 *   <li><strong>GUID and name:</strong> When both the conflicting object's ID and name are known</li>
 *   <li><strong>Name and type:</strong> When only the name and object type are known</li>
 *   <li><strong>Direct message:</strong> For custom error scenarios</li>
 * </ul>
 *
 * <p>All constructors include comprehensive validation and provide meaningful error messages
 * following Java 11 best practices with Optional support and enhanced validation.
 *
 * @author Yu-Bing Chen
 * @since Java 11 Modernization
 */
public class PSDuplicateNameException extends PSRuntimeException {

    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Message key for duplicate name errors with GUID context.
     */
    private static final String MSG_KEY_DUPLICATE_NAME = "service.exception@DuplicateName";

    /**
     * Message key for duplicate name errors with type context only.
     */
    private static final String MSG_KEY_DUPLICATE_NAME_BY_TYPE = "service.exception@DuplicateNameByType";

    /**
     * The GUID of the existing object that has the duplicate name, if known.
     */
    private final IPSGuid conflictingObjectId;

    /**
     * The duplicate name that caused the conflict.
     */
    private final String duplicateName;

    /**
     * The type of object that has the duplicate name.
     */
    private final PSTypeEnum objectType;

    /**
     * Creates an exception for a duplicate name conflict with known GUID and name.
     *
     * @param id the GUID of the existing object with the duplicate name, must not be null
     * @param name the duplicate name that caused the conflict, must not be null or empty
     * @throws IllegalArgumentException if id is null or name is null/empty
     */
    public PSDuplicateNameException(IPSGuid id, String name) {
        super();
        Objects.requireNonNull(id, "GUID cannot be null");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        this.conflictingObjectId = id;
        this.duplicateName = name.trim();
        this.objectType = PSTypeEnum.valueOf(id.getType());

        var args = new Object[]{id.longValue(), objectType != null ? objectType.getDisplayName() : "Unknown", duplicateName};
        setMsgKeyAndArgs(MSG_KEY_DUPLICATE_NAME, args);
    }

    /**
     * Creates an exception for a duplicate name conflict with known name and type.
     *
     * @param name the duplicate name that caused the conflict, must not be null or empty
     * @param type the type of object that has the duplicate name, must not be null
     * @throws IllegalArgumentException if name is null/empty or type is null
     */
    public PSDuplicateNameException(String name, PSTypeEnum type) {
        super();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Objects.requireNonNull(type, "Object type cannot be null");

        this.conflictingObjectId = null;
        this.duplicateName = name.trim();
        this.objectType = type;

        var args = new Object[]{duplicateName, type.getDisplayName()};
        setMsgKeyAndArgs(MSG_KEY_DUPLICATE_NAME_BY_TYPE, args);
    }

    /**
     * Creates an exception with a custom error message.
     *
     * @param errorMsg the custom error message, may be null
     */
    public PSDuplicateNameException(String errorMsg) {
        super(errorMsg);
        this.conflictingObjectId = null;
        this.duplicateName = null;
        this.objectType = null;
    }

    /**
     * Creates an exception with a custom error message and cause.
     *
     * @param errorMsg the custom error message, may be null
     * @param cause the cause of the exception, may be null
     */
    public PSDuplicateNameException(String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.conflictingObjectId = null;
        this.duplicateName = null;
        this.objectType = null;
    }

    /**
     * Gets the GUID of the existing object that has the duplicate name.
     *
     * @return an Optional containing the conflicting object's GUID, or empty if not available
     */
    public Optional<IPSGuid> getConflictingObjectId() {
        return Optional.ofNullable(conflictingObjectId);
    }

    /**
     * Gets the duplicate name that caused the conflict.
     *
     * @return an Optional containing the duplicate name, or empty if not available
     */
    public Optional<String> getDuplicateName() {
        return Optional.ofNullable(duplicateName);
    }

    /**
     * Gets the type of object that has the duplicate name.
     *
     * @return an Optional containing the object type, or empty if not available
     */
    public Optional<PSTypeEnum> getObjectType() {
        return Optional.ofNullable(objectType);
    }

    /**
     * Checks if this exception includes information about the conflicting object's GUID.
     *
     * @return true if the conflicting object's GUID is available, false otherwise
     */
    public boolean hasConflictingObjectId() {
        return conflictingObjectId != null;
    }

    /**
     * Checks if this exception includes the duplicate name that caused the conflict.
     *
     * @return true if the duplicate name is available, false otherwise
     */
    public boolean hasDuplicateName() {
        return duplicateName != null;
    }

    /**
     * Checks if this exception includes the object type information.
     *
     * @return true if the object type is available, false otherwise
     */
    public boolean hasObjectType() {
        return objectType != null;
    }

    /**
     * Gets the conflicting object's GUID as a long value.
     *
     * @return an Optional containing the GUID as a long, or empty if not available
     */
    public Optional<Long> getConflictingObjectIdAsLong() {
        return getConflictingObjectId().map(IPSGuid::longValue);
    }

    /**
     * Gets the display name of the object type.
     *
     * @return an Optional containing the object type's display name, or empty if not available
     */
    public Optional<String> getObjectTypeDisplayName() {
        return getObjectType().map(PSTypeEnum::getDisplayName);
    }

    /**
     * Creates a duplicate name exception for the specified GUID and name with validation.
     *
     * @param id the GUID of the existing object with the duplicate name, must not be null
     * @param name the duplicate name that caused the conflict, must not be null or empty
     * @return a new PSDuplicateNameException for the GUID and name
     * @throws IllegalArgumentException if id is null or name is null/empty
     */
    public static PSDuplicateNameException forGuidAndName(IPSGuid id, String name) {
        return new PSDuplicateNameException(id, name);
    }

    /**
     * Creates a duplicate name exception for the specified name and type with validation.
     *
     * @param name the duplicate name that caused the conflict, must not be null or empty
     * @param type the type of object that has the duplicate name, must not be null
     * @return a new PSDuplicateNameException for the name and type
     * @throws IllegalArgumentException if name is null/empty or type is null
     */
    public static PSDuplicateNameException forNameAndType(String name, PSTypeEnum type) {
        return new PSDuplicateNameException(name, type);
    }

    /**
     * Creates a duplicate name exception with a formatted message.
     *
     * @param messageTemplate the message template with placeholders, must not be null
     * @param args the arguments for formatting, may be empty
     * @return a new PSDuplicateNameException with the formatted message
     * @throws IllegalArgumentException if messageTemplate is null
     */
    public static PSDuplicateNameException withFormattedMessage(String messageTemplate, Object... args) {
        Objects.requireNonNull(messageTemplate, "Message template cannot be null");
        var formattedMessage = String.format(messageTemplate, args);
        return new PSDuplicateNameException(formattedMessage);
    }

    /**
     * Creates a duplicate name exception with detailed context information.
     *
     * @param name the duplicate name, must not be null or empty
     * @param type the object type, must not be null
     * @param existingId the GUID of the existing object, may be null
     * @return a new PSDuplicateNameException with appropriate context
     * @throws IllegalArgumentException if name is null/empty or type is null
     */
    public static PSDuplicateNameException withContext(String name, PSTypeEnum type, IPSGuid existingId) {
        if (existingId != null) {
            return new PSDuplicateNameException(existingId, name);
        } else {
            return new PSDuplicateNameException(name, type);
        }
    }

    @Override
    public String toString() {
        var className = getClass().getSimpleName();

        if (duplicateName != null && objectType != null) {
            if (conflictingObjectId != null) {
                return String.format("%s: Duplicate name '%s' for %s (conflicting object ID: %s)",
                    className, duplicateName, objectType.getDisplayName(), conflictingObjectId);
            } else {
                return String.format("%s: Duplicate name '%s' for %s",
                    className, duplicateName, objectType.getDisplayName());
            }
        } else {
            var message = getLocalizedMessage();
            return message != null ? className + ": " + message : className;
        }
    }
}
