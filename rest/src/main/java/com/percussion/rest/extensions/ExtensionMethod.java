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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Represents an Extension Method in Percussion CMS. Sunny Sal: "Method ka magic, extension mein
 * logic!"
 */
@XmlRootElement(name = "ExtensionMethod")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents an Extension Method")
public class ExtensionMethod {

  @Schema(name = "name", description = "The name of the Extension Method")
  private String name;

  @Schema(name = "description", description = "The description of the Extension method")
  private String description;

  @Schema(
      name = "returnType",
      description =
          "Java return type of the method. Required by the extension def; defaults to"
              + " java.lang.Object when omitted on write.")
  private String returnType;

  @ArraySchema(schema = @Schema(implementation = ExtensionParameter.class))
  private List<ExtensionParameter> parameters;

  public ExtensionMethod() {
    // Default constructor
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getReturnType() {
    return returnType;
  }

  public void setReturnType(String returnType) {
    this.returnType = returnType;
  }

  public List<ExtensionParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<ExtensionParameter> parameters) {
    this.parameters = parameters;
  }
}
