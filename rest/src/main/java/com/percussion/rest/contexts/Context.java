// REFACTORED: CP-JAVA11
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

package com.percussion.rest.contexts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import com.percussion.rest.locationscheme.LocationScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

/**
 * Represents a publishing Context in Percussion CMS.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits {@code name}
 * and nested {@code defaultScheme} when set. Optional-returning getters historically serialized as
 * empty/present beans or dropped fields under {@code @JsonInclude(NON_NULL)} (issue #3412 / #3388).
 */
@XmlRootElement(name = "Context")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a publishing Context")
public class Context {

  @Schema(
      name = "id",
      description = "The unique identifier for the Context. Use string value to persist the id.")
  private Guid id;

  @Schema(name = "version", description = "Ignored")
  private Integer version;

  @Schema(
      name = "name",
      description = "The unique name for the publishing Context",
      required = true)
  private String name;

  @Schema(
      name = "description",
      description = "Human-friendly description for the publishing Context")
  private String description;

  @Schema(
      name = "defaultScheme",
      description = "The default Location Scheme to use when publishing to this Context")
  private LocationScheme defaultScheme;

  @Schema(
      name = "locationSchemes",
      description = "The list of Location Schemes configured for this publishing Context")
  private List<LocationScheme> locationSchemes;

  public Context() {}

  public Context(
      Guid id,
      Integer version,
      String name,
      String description,
      LocationScheme defaultScheme,
      List<LocationScheme> locationSchemes) {
    this.id = id;
    this.version = version;
    this.name = name;
    this.description = description;
    this.defaultScheme = defaultScheme;
    this.locationSchemes = locationSchemes;
  }

  public Guid getId() {
    return id;
  }

  public void setId(Guid id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
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

  public LocationScheme getDefaultScheme() {
    return defaultScheme;
  }

  public void setDefaultScheme(LocationScheme defaultScheme) {
    this.defaultScheme = defaultScheme;
  }

  public List<LocationScheme> getLocationSchemes() {
    return locationSchemes;
  }

  public void setLocationSchemes(List<LocationScheme> locationSchemes) {
    this.locationSchemes = locationSchemes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Context)) return false;
    var that = (Context) o;
    return Objects.equals(id, that.id)
        && Objects.equals(version, that.version)
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(defaultScheme, that.defaultScheme)
        && Objects.equals(locationSchemes, that.locationSchemes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version, name, description, defaultScheme, locationSchemes);
  }

  @Override
  public String toString() {
    return "Context{"
        + "id="
        + id
        + ", version="
        + version
        + ", name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", defaultScheme="
        + defaultScheme
        + ", locationSchemes="
        + locationSchemes
        + '}';
  }
}
