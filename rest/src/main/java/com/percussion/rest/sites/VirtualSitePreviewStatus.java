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
package com.percussion.rest.sites;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Whether a last Virtual Site static build can be previewed from the product UI ({@code GET
 * /sites/{nameOrId}/virtual/preview}).
 *
 * <p>Last-output based for {@code git-filesystem} and {@code csv-filesystem} (not git-only).
 * Missing or failed builds return HTTP 200 with {@code available=false} and a message — not 500.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson emits scalars, not
 * Optional-bean {@code empty}/{@code present} keys (#3411 / #3388).
 */
@XmlRootElement(name = "VirtualSitePreviewStatus")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "VirtualSitePreviewStatus",
    description = "Last Virtual Site build preview availability and assembled home path")
public class VirtualSitePreviewStatus {

  @Schema(description = "True when an assembled index/home file exists under the last output root")
  private Boolean available;

  @Schema(
      description = "Relative home path under the output root (forward slashes)",
      example = "8.2/index.html")
  private String homePath;

  @Schema(
      description = "Absolute filesystem path of the last build output root",
      example = "C:/Rhythmyx/tmp/virtual-sites/product-docs")
  private String outputPath;

  @Schema(description = "Operator-facing reason when available is false")
  private String message;

  public VirtualSitePreviewStatus() {
    // Default constructor for JAX-RS / Jackson
  }

  public Boolean getAvailable() {
    return available;
  }

  public void setAvailable(Boolean available) {
    this.available = available;
  }

  public String getHomePath() {
    return homePath;
  }

  public void setHomePath(String homePath) {
    this.homePath = homePath;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public void setOutputPath(String outputPath) {
    this.outputPath = outputPath;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
