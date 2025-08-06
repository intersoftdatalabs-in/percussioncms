/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.utils.jsf.validators;

import com.percussion.i18n.PSI18nUtils;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for validators with modern Java 11 features and enhanced message production.
 * This class provides a foundation for creating robust JSF validators with proper error handling,
 * internationalization support, and comprehensive validation utilities.
 * <p>
 * All validation methods are designed to be null-safe and provide meaningful error messages
 * to improve user experience.
 *
 * @author dougrand
 */
public abstract class PSBaseValidator implements Validator {

    private static final Logger ms_log = LogManager.getLogger(PSBaseValidator.class);

    /**
     * Default severity for validation errors
     */
    protected static final Severity DEFAULT_SEVERITY = FacesMessage.SEVERITY_ERROR;

    /**
     * Generate and throw a faces validation error with default severity
     *
     * @param key a key to look up the error message in the tmx file, not {@code null}
     * @throws ValidatorException to signal the validation problem
     * @throws IllegalArgumentException if key is null or empty
     */
    protected void fail(String key) {
        fail(DEFAULT_SEVERITY, key);
    }

    /**
     * Generate and throw a faces validation error with enhanced validation
     *
     * @param severity the severity of the error, not {@code null}
     * @param key a key to look up the error message in the tmx file, not {@code null}
     * @throws ValidatorException to signal the validation problem
     * @throws IllegalArgumentException if parameters are invalid
     */
    protected void fail(Severity severity, String key) {
        Objects.requireNonNull(severity, "Severity cannot be null");
        validateMessageKey(key);

        var message = getLocalizedMessage(key);
        var facesMessage = new FacesMessage(severity, message, message);

        ms_log.debug("Validation failed with key '{}' and severity '{}'", key, severity);
        throw new ValidatorException(facesMessage);
    }

    /**
     * Generate and throw a faces validation error with parameters for message formatting
     *
     * @param severity the severity of the error, not {@code null}
     * @param key a key to look up the error message in the tmx file, not {@code null}
     * @param params parameters for message formatting, may be {@code null} or empty
     * @throws ValidatorException to signal the validation problem
     * @throws IllegalArgumentException if key or severity is invalid
     */
    protected void fail(Severity severity, String key, Object... params) {
        Objects.requireNonNull(severity, "Severity cannot be null");
        validateMessageKey(key);

        var message = getLocalizedMessage(key, params);
        var facesMessage = new FacesMessage(severity, message, message);

        ms_log.debug("Validation failed with key '{}', severity '{}', and {} parameters",
            key, severity, params != null ? params.length : 0);
        throw new ValidatorException(facesMessage);
    }

    /**
     * Get a localized message with optional parameters
     *
     * @param key the message key, not {@code null} or empty
     * @param params optional parameters for message formatting
     * @return the localized message, never {@code null}
     */
    protected String getLocalizedMessage(String key, Object... params) {
        validateMessageKey(key);

        var baseMessage = PSI18nUtils.getString(key);
        if (params == null || params.length == 0) {
            return baseMessage;
        }

        try {
            return MessageFormat.format(baseMessage, params);
        } catch (Exception e) {
            ms_log.warn("Failed to format message for key '{}' with {} parameters: {}",
                key, params.length, e.getMessage());
            return baseMessage; // Fallback to unformatted message
        }
    }

    /**
     * Validate that a value is not null or empty (for strings)
     *
     * @param value the value to check
     * @param fieldName the name of the field for error reporting
     * @return {@code true} if value is valid, {@code false} otherwise
     */
    protected boolean isValidValue(Object value, String fieldName) {
        if (value == null) {
            ms_log.debug("Validation failed: {} is null", fieldName);
            return false;
        }

        if (value instanceof String) {
            String stringValue = (String) value;
            if (StringUtils.isBlank(stringValue)) {
                ms_log.debug("Validation failed: {} is blank", fieldName);
                return false;
            }
        }

        return true;
    }

    /**
     * Safely convert an object to string with null handling
     *
     * @param value the value to convert, may be {@code null}
     * @return the string representation, never {@code null}
     */
    protected String safeToString(Object value) {
        return Optional.ofNullable(value)
            .map(Object::toString)
            .orElse("");
    }

    /**
     * Check if a string value meets minimum length requirements
     *
     * @param value the string to check, may be {@code null}
     * @param minLength the minimum required length
     * @return {@code true} if value meets minimum length, {@code false} otherwise
     */
    protected boolean hasMinimumLength(String value, int minLength) {
        return StringUtils.length(value) >= minLength;
    }

    /**
     * Check if a string value exceeds maximum length requirements
     *
     * @param value the string to check, may be {@code null}
     * @param maxLength the maximum allowed length
     * @return {@code true} if value is within maximum length, {@code false} otherwise
     */
    protected boolean isWithinMaximumLength(String value, int maxLength) {
        return StringUtils.length(value) <= maxLength;
    }

    /**
     * Validate a message key for proper format and content
     *
     * @param key the message key to validate
     * @throws IllegalArgumentException if key is invalid
     */
    private void validateMessageKey(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Message key cannot be null or empty");
        }
    }

    /**
     * Get the validator name for logging and debugging purposes
     *
     * @return the simple class name of the validator
     */
    protected String getValidatorName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Log validation success for debugging purposes
     *
     * @param fieldName the name of the field that was validated
     * @param value the value that was validated
     */
    protected void logValidationSuccess(String fieldName, Object value) {
        if (ms_log.isTraceEnabled()) {
            ms_log.trace("{} validation succeeded for field '{}' with value: {}",
                getValidatorName(), fieldName, safeToString(value));
        }
    }

    /**
     * Log validation failure for debugging purposes
     *
     * @param fieldName the name of the field that failed validation
     * @param value the value that failed validation
     * @param reason the reason for validation failure
     */
    protected void logValidationFailure(String fieldName, Object value, String reason) {
        ms_log.debug("{} validation failed for field '{}' with value '{}': {}",
            getValidatorName(), fieldName, safeToString(value), reason);
    }
}
