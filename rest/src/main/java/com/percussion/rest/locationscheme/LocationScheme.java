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

package com.percussion.rest.locationscheme;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/** Represents a Location Scheme. Sunny Sal: "Location scheme ka hero, publishing ka zero!" */
@XmlRootElement(name = "LocationScheme")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a Location Scheme")
public class LocationScheme {

  @Schema(name = "schemeId", description = "A unique guid for the Location scheme.")
  private Guid schemeId;

  @Schema(name = "name", description = "A unique name for this location scheme.", required = true)
  private String name;

  @Schema(
      name = "description",
      description = "Human friendly description describing the location scheme")
  private String description;

  @Schema(
      name = "template",
      description = "The Template that this location scheme is configured for")
  private long templateId;

  @Schema(
      name = "contentType",
      description = "The Content Type id that this location scheme is configured for")
  private long contentTypeId;

  @Schema(
      name = "context",
      description = "The Publishing Context that this location scheme is linked to")
  private Guid context;

  @Schema(name = "locationSchemeGenerator", description = "The Location Scheme Generator")
  private String locationSchemeGenerator;

  @Schema(name = "parameters", description = "Location Scheme Parameters")
  private LocationSchemeParameterList parameters;

  public LocationScheme() {
    // Default constructor
  }

  public Optional<Guid> getSchemeId() {
    return Optional.ofNullable(schemeId);
  }

  public void setSchemeId(Guid schemeId) {
    this.schemeId = schemeId;
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

  public long getTemplateId() {
    return templateId;
  }

  public void setTemplateId(long templateId) {
    this.templateId = templateId;
  }

  public long getContentTypeId() {
    return contentTypeId;
  }

  public void setContentTypeId(long contentTypeId) {
    this.contentTypeId = contentTypeId;
  }

  public Optional<Guid> getContext() {
    return Optional.ofNullable(context);
  }

  public void setContext(Guid context) {
    this.context = context;
  }

  public Optional<String> getLocationSchemeGenerator() {
    return Optional.ofNullable(locationSchemeGenerator);
  }

  public void setLocationSchemeGenerator(String locationSchemeGenerator) {
    this.locationSchemeGenerator = locationSchemeGenerator;
  }

  public Optional<LocationSchemeParameterList> getParameters() {
    return Optional.ofNullable(parameters);
  }

  public void setParameters(LocationSchemeParameterList parameters) {
    this.parameters = parameters;
  }
}
