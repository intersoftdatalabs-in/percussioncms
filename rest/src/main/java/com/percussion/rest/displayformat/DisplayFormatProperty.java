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

// REFACTORED: CP-JAVA11

package com.percussion.rest.displayformat;

import com.percussion.rest.ValueList;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

/**
 * Represents a property of a DisplayFormat.
 * Properties may be multi-valued or single-valued.
 */
@Schema(
    name = "DisplayFormatProperty",
    description = "Represents a property of a DisplayFormat. Properties may be multi valued or single valued."
)
public class DisplayFormatProperty {

    @Schema(name = "propertyId", description = "The id for this property.")
    private String propertyId;

    @Schema(name = "propertyName", description = "The unique Name for this property.")
    private String propertyName;

    @Schema(name = "propertyValue", description = "For a single value property, the value of the property")
    private String propertyValue;

    @Schema(name = "description", description = "An optional description of this property's purpose")
    private String description;

    @Schema(name = "propertyValues", description = "For a multi value property, the list of current values of the property")
    private ValueList propertyValues;

    public DisplayFormatProperty() {
        // Default constructor
    }

    public Optional<String> getPropertyId() {
        return Optional.ofNullable(propertyId);
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public Optional<String> getPropertyName() {
        return Optional.ofNullable(propertyName);
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Optional<String> getPropertyValue() {
        return Optional.ofNullable(propertyValue);
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Optional<ValueList> getPropertyValues() {
        return Optional.ofNullable(propertyValues);
    }

    public void setPropertyValues(ValueList propertyValues) {
        this.propertyValues = propertyValues;
    }
}
