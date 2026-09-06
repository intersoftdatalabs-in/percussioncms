/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.problems;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * One design-time validation problem for the Developer Problems panel (Workbench §12.4).
 *
 * <p>{@link #navigateSection} is a Developer SPA section id when a peer editor exists
 * (navigate-to-source). Catalog tokens only — never filesystem paths or JDBC URLs.
 */
@XmlRootElement(name = "DesignProblem")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Developer session design/validation problem (read-only)")
public class DesignProblem {

  private String id;
  private String severity;
  private String code;
  private String message;
  private String objectType;
  private String objectId;
  private String objectName;
  private String location;
  private String navigateSection;

  public DesignProblem() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /** Severity: {@code ERROR} or {@code WARNING}. */
  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /** Developer object kind (often a SPA section id, e.g. {@code content-types}). */
  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  /** Optional field or component path within the open editor. */
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  /** Developer SPA section to open when a peer editor exists; omitted otherwise. */
  public String getNavigateSection() {
    return navigateSection;
  }

  public void setNavigateSection(String navigateSection) {
    this.navigateSection = navigateSection;
  }
}
