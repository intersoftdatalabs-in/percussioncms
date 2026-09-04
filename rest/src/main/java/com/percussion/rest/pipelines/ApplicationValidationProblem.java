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

package com.percussion.rest.pipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * One design-time validation problem for a classic XML Application / pipeline package.
 *
 * <p>Produced by Admin {@code GET …/validation} from object-store {@code PSValidatorAdapter} peers
 * (errors and warnings). Stable {@link #code} is the numeric object-store message code as a
 * string; {@link #severity} is {@code ERROR} or {@code WARNING}.
 */
@XmlRootElement(name = "ApplicationValidationProblem")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pipeline application validation problem")
public class ApplicationValidationProblem {

  /** Problem severity: {@code ERROR} or {@code WARNING}. */
  @Schema(description = "Severity: ERROR or WARNING", example = "ERROR")
  private String severity;

  /** Stable machine-readable code (object-store message code as decimal string). */
  @Schema(description = "Stable code (object-store message code)", example = "1301")
  private String code;

  /** Human-readable English message from the object-store catalog. */
  @Schema(description = "Human-readable message")
  private String message;

  /** Optional dataset / requestor resource name when known. */
  @Schema(description = "Optional dataset or resource name")
  private String resource;

  /** Optional component path (class names / ids) for UI navigation. */
  @Schema(description = "Optional component path for UI navigation")
  private String path;

  public ApplicationValidationProblem() {}

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

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
