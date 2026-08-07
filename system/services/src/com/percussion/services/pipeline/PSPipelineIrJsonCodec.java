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

package com.percussion.services.pipeline;

import com.percussion.services.pipeline.model.PipelineIrDocument;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON encode/decode for {@link PipelineIrDocument} (Jackson tools API used elsewhere in system).
 */
public final class PSPipelineIrJsonCodec {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();

  private PSPipelineIrJsonCodec() {}

  /**
   * Serialize IR to pretty-printed JSON UTF-8 text.
   *
   * @param document never {@code null}
   * @return JSON string, never {@code null}
   */
  public static String toJson(PipelineIrDocument document) throws PSPipelineIrException {
    Objects.requireNonNull(document, "document");
    try {
      return MAPPER.writeValueAsString(document);
    } catch (JacksonException e) {
      throw new PSPipelineIrException("Failed to encode pipeline IR as JSON", e);
    }
  }

  /**
   * Parse IR from JSON text.
   *
   * @param json never {@code null}
   * @return document, never {@code null}
   */
  public static PipelineIrDocument fromJson(String json) throws PSPipelineIrException {
    Objects.requireNonNull(json, "json");
    try {
      PipelineIrDocument doc = MAPPER.readValue(json, PipelineIrDocument.class);
      if (doc == null) {
        throw new PSPipelineIrException("JSON decoded to null pipeline IR document");
      }
      return doc;
    } catch (JacksonException e) {
      throw new PSPipelineIrException("Failed to decode pipeline IR JSON", e);
    }
  }
}
