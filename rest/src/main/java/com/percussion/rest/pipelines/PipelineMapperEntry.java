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
 * One document ↔ backend mapping row for a native pipeline mapper write (MAP-01 / MAP-02 /
 * MAP-09). The ordered list on {@link PipelineMapper} is the replace unit: order is preserved,
 * rows can be added, removed, or reordered.
 */
@XmlRootElement(name = "PipelineMapperEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One document field to backend field mapping row")
public class PipelineMapperEntry {

  /** Document-side field (JSON path / response property). Required, non-blank. */
  @Schema(
      description = "Document-side field projected by execute",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String documentField;

  /** Backend-side field (column, extension output, or adapter field). Required, non-blank. */
  @Schema(
      description = "Backend-side source field",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String backend;

  /**
   * Backend source kind: {@code COLUMN}, {@code EXTENSION}, or {@code OTHER}. Blank normalizes
   * to {@code OTHER} on persist; anything else is a 400.
   */
  @Schema(description = "Backend source kind (COLUMN, EXTENSION, or OTHER)")
  private String backendKind;

  public String getDocumentField() {
    return documentField;
  }

  public void setDocumentField(String documentField) {
    this.documentField = documentField;
  }

  public String getBackend() {
    return backend;
  }

  public void setBackend(String backend) {
    this.backend = backend;
  }

  public String getBackendKind() {
    return backendKind;
  }

  public void setBackendKind(String backendKind) {
    this.backendKind = backendKind;
  }
}
