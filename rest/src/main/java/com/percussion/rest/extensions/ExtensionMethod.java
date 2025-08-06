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

package com.percussion.rest.extensions;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Optional;

/**
 * Represents an Extension Method in Percussion CMS.
 * Sunny Sal: "Method ka magic, extension mein logic!"
 */
@XmlRootElement(name = "ExtensionMethod")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents an Extension Method")
public class ExtensionMethod {

    @Schema(name = "name", description = "The name of the Extension Method")
    private String name;

    @Schema(name = "description", description = "The description of the Extension method")
    private String description;

    @ArraySchema(schema = @Schema(implementation = ExtensionParameter.class))
    private List<ExtensionParameter> parameters;

    public ExtensionMethod() {
        // Default constructor
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Optional<List<ExtensionParameter>> getParameters() {
        return Optional.ofNullable(parameters);
    }

    public void setParameters(List<ExtensionParameter> parameters) {
        this.parameters = parameters;
    }
}
