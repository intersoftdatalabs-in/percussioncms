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
import java.util.ArrayList;
import java.util.List;

/**
 * Native IR write for the pipeline mapper stage (MAP-01 create ordered list, MAP-02 query
 * direction, MAP-09 remove/clear/reorder). An empty {@code mappings} list clears the stage
 * (explicit clear, not a no-op). Omitted {@code allowEmptyDocReturn} defaults to {@code false}
 * on persist.
 */
@XmlRootElement(name = "PipelineMapper")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Native pipeline mapper stage (ordered document to backend mappings)")
public class PipelineMapper {

  /** Hard cap to avoid unbounded payloads (peers catalog MAX_LIMIT discipline). */
  static final int MAX_MAPPINGS = 500;

  static final int MAX_FIELD_CHARS = 512;

  /** Ordered mapping rows; empty clears the stage. Never {@code null} after the setter. */
  @Schema(description = "Ordered mapping rows; empty clears the stage")
  private List<PipelineMapperEntry> mappings = new ArrayList<>();

  /** Classic empty-document policy (MAP-07); omitted defaults to {@code false} on persist. */
  @Schema(description = "Return an empty document when no rows match")
  private Boolean allowEmptyDocReturn;

  public List<PipelineMapperEntry> getMappings() {
    return mappings;
  }

  public void setMappings(List<PipelineMapperEntry> mappings) {
    this.mappings = mappings != null ? mappings : new ArrayList<>();
  }

  public Boolean getAllowEmptyDocReturn() {
    return allowEmptyDocReturn;
  }

  public void setAllowEmptyDocReturn(Boolean allowEmptyDocReturn) {
    this.allowEmptyDocReturn = allowEmptyDocReturn;
  }

  /**
   * Fail-fast REST-layer shape checks before the adaptor path guard. Row indexes in messages are
   * 1-based for operator readability. Does not replace adaptor normalization (trimming,
   * backend-kind defaulting).
   */
  public void requireWriteFields() {
    if (mappings == null) {
      throw new IllegalArgumentException("Mapper mappings are required (empty clears the stage)");
    }
    if (mappings.size() > MAX_MAPPINGS) {
      throw new IllegalArgumentException(
          "Mapper mappings exceed the limit of " + MAX_MAPPINGS);
    }
    for (int i = 0; i < mappings.size(); i++) {
      PipelineMapperEntry row = mappings.get(i);
      String where = "Mapping row " + (i + 1);
      if (row == null) {
        throw new IllegalArgumentException(where + " is null");
      }
      String field = row.getDocumentField() != null ? row.getDocumentField().trim() : "";
      String backend = row.getBackend() != null ? row.getBackend().trim() : "";
      if (field.isEmpty()) {
        throw new IllegalArgumentException(where + ": documentField is required");
      }
      if (backend.isEmpty()) {
        throw new IllegalArgumentException(where + ": backend is required");
      }
      if (field.length() > MAX_FIELD_CHARS || backend.length() > MAX_FIELD_CHARS) {
        throw new IllegalArgumentException(where + " exceeds the field length limit");
      }
      String kind = row.getBackendKind() != null ? row.getBackendKind().trim() : "";
      if (!kind.isEmpty()
          && !isSupportedBackendKind(kind)) {
        throw new IllegalArgumentException(
            where + ": backendKind must be COLUMN, EXTENSION, or OTHER");
      }
    }
  }

  /** Case-insensitive allow-list for backend source kinds. */
  static boolean isSupportedBackendKind(String kind) {
    return "COLUMN".equalsIgnoreCase(kind)
        || "EXTENSION".equalsIgnoreCase(kind)
        || "OTHER".equalsIgnoreCase(kind);
  }
}
