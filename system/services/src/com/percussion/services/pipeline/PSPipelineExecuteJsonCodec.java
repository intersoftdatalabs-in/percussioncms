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

import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** JSON encode/decode for pipeline execute request/response DTOs. */
public final class PSPipelineExecuteJsonCodec {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();

  private PSPipelineExecuteJsonCodec() {}

  public static String toJson(PipelineExecuteResult result) throws PSPipelineIrException {
    Objects.requireNonNull(result, "result");
    try {
      return MAPPER.writeValueAsString(result);
    } catch (JacksonException e) {
      throw new PSPipelineIrException("Failed to encode pipeline execute result as JSON", e);
    }
  }

  public static PipelineExecuteRequest requestFromJson(String json) throws PSPipelineIrException {
    Objects.requireNonNull(json, "json");
    try {
      PipelineExecuteRequest req = MAPPER.readValue(json, PipelineExecuteRequest.class);
      return req != null ? req : PipelineExecuteRequest.empty();
    } catch (JacksonException e) {
      throw new PSPipelineIrException("Failed to decode pipeline execute request JSON", e);
    }
  }

  public static PipelineExecuteResult resultFromJson(String json) throws PSPipelineIrException {
    Objects.requireNonNull(json, "json");
    try {
      PipelineExecuteResult result = MAPPER.readValue(json, PipelineExecuteResult.class);
      if (result == null) {
        throw new PSPipelineIrException("JSON decoded to null pipeline execute result");
      }
      return result;
    } catch (JacksonException e) {
      throw new PSPipelineIrException("Failed to decode pipeline execute result JSON", e);
    }
  }
}
