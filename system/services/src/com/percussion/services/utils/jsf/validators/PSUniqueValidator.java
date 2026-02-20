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
package com.percussion.services.utils.jsf.validators;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates that the provided value is unique within a collection of existing values.
 * This validator uses modern Java 11 features for enhanced performance and type safety.
 * <p>
 * The validator supports both case-sensitive and case-insensitive string comparisons,
 * and provides flexible value retrieval through the IPSUniqueValidatorValueProvider interface.
 *
 * @author Andriy Palamarchuk
 */
public class PSUniqueValidator extends PSBaseValidator {

    private static final String DEFAULT_DUPLICATE_MESSAGE_KEY = "validator.unique.duplicate";
    private static final String DEFAULT_PROVIDER_MISSING_KEY = "validator.unique.provider.missing";

    /**
     * The value provider that supplies the collection of existing values
     */
    private IPSUniqueValidatorValueProvider valueProvider;

    /**
     * Whether to perform case-sensitive comparison for string values
     */
    private boolean caseSensitive = true;

    /**
     * Custom message key for duplicate value errors
     */
    private String duplicateMessageKey = DEFAULT_DUPLICATE_MESSAGE_KEY;

    @Override
    public void validate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {

        Objects.requireNonNull(context, "FacesContext cannot be null");
        Objects.requireNonNull(component, "UIComponent cannot be null");

        // Skip validation for null or empty values (let required validator handle this)
        if (!isValidValue(value, "value")) {
            logValidationSuccess("value", value);
            return;
        }

        var provider = getValueProvider();
        if (provider == null) {
            logValidationFailure("value", value, "Value provider not specified");
            fail(FacesMessage.SEVERITY_ERROR, DEFAULT_PROVIDER_MISSING_KEY);
            return;
        }

        var existingValues = provider.getAllValues();
        if (existingValues == null || existingValues.isEmpty()) {
            logValidationSuccess("value", value);
            return;
        }

        if (isDuplicateValue(value, existingValues)) {
            logValidationFailure("value", value, "Duplicate value found");
            fail(FacesMessage.SEVERITY_ERROR, duplicateMessageKey, safeToString(value));
        } else {
            logValidationSuccess("value", value);
        }
    }

    /**
     * Check if the given value is a duplicate within the existing values collection
     *
     * @param value the value to check for duplicates
     * @param existingValues the collection of existing values
     * @return {@code true} if value is a duplicate, {@code false} otherwise
     */
    private boolean isDuplicateValue(Object value, Collection<? extends Object> existingValues) {
        if (value instanceof String) {
            String stringValue = (String) value;
            return isDuplicateString(stringValue, existingValues);
        } else {
            return isDuplicateObject(value, existingValues);
        }
    }

    /**
     * Check for string duplicate with case sensitivity consideration
     *
     * @param stringValue the string value to check
     * @param existingValues the collection of existing values
     * @return {@code true} if string is a duplicate, {@code false} otherwise
     */
    private boolean isDuplicateString(String stringValue, Collection<? extends Object> existingValues) {
        return existingValues.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .anyMatch(existing -> isStringMatch(stringValue, existing));
    }

    /**
     * Check for object duplicate using equals comparison
     *
     * @param value the object value to check
     * @param existingValues the collection of existing values
     * @return {@code true} if object is a duplicate, {@code false} otherwise
     */
    private boolean isDuplicateObject(Object value, Collection<? extends Object> existingValues) {
        return existingValues.stream()
            .anyMatch(existing -> Objects.equals(value, existing));
    }

    /**
     * Compare two strings based on case sensitivity setting
     *
     * @param value1 the first string to compare
     * @param value2 the second string to compare
     * @return {@code true} if strings match, {@code false} otherwise
     */
    private boolean isStringMatch(String value1, String value2) {
        if (caseSensitive) {
            return StringUtils.equals(value1, value2);
        } else {
            return StringUtils.equalsIgnoreCase(value1, value2);
        }
    }

    /**
     * Get the value provider
     *
     * @return the value provider, may be {@code null}
     */
    public IPSUniqueValidatorValueProvider getValueProvider() {
        return valueProvider;
    }

    /**
     * Set the value provider with validation
     *
     * @param valueProvider the value provider to set, not {@code null}
     * @throws IllegalArgumentException if valueProvider is null
     */
    public void setValueProvider(IPSUniqueValidatorValueProvider valueProvider) {
        this.valueProvider = Objects.requireNonNull(valueProvider,
            "Value provider cannot be null");
    }

    /**
     * Check if string comparison is case sensitive
     *
     * @return {@code true} if case sensitive, {@code false} otherwise
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Set whether string comparison should be case sensitive
     *
     * @param caseSensitive {@code true} for case sensitive comparison
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Get the custom message key for duplicate value errors
     *
     * @return the message key, never {@code null}
     */
    public String getDuplicateMessageKey() {
        return duplicateMessageKey;
    }

    /**
     * Set a custom message key for duplicate value errors
     *
     * @param duplicateMessageKey the message key to use, not {@code null} or empty
     * @throws IllegalArgumentException if messageKey is invalid
     */
    public void setDuplicateMessageKey(String duplicateMessageKey) {
        if (StringUtils.isBlank(duplicateMessageKey)) {
            throw new IllegalArgumentException("Duplicate message key cannot be null or empty");
        }
        this.duplicateMessageKey = duplicateMessageKey;
    }

    /**
     * Get the count of existing values from the provider
     *
     * @return the count of existing values, or 0 if provider is null
     */
    public int getExistingValuesCount() {
        return Optional.ofNullable(valueProvider)
            .map(IPSUniqueValidatorValueProvider::getAllValues)
            .map(Collection::size)
            .orElse(0);
    }

    /**
     * Check if the validator is properly configured
     *
     * @return {@code true} if validator is configured, {@code false} otherwise
     */
    public boolean isConfigured() {
        return valueProvider != null &&
               StringUtils.isNotBlank(duplicateMessageKey);
    }

    @Override
    public String toString() {
        return String.format("%s[caseSensitive=%s, existingValues=%d, configured=%s]",
            getValidatorName(), caseSensitive, getExistingValuesCount(), isConfigured());
    }
}
