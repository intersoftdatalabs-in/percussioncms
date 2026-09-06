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

package com.percussion.services.pipeline.http;

import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import java.util.List;
import java.util.Map;

/**
 * HTTP backend-tank adapter for native pipeline IR (Slice C). Implementations fetch only
 * loopback/local fixture URLs — never cloud hosts or credentialed URLs.
 */
public interface IPSPipelineHttpAdapter {

  /**
   * GET the resource backend tank URL, parse JSON, and map to document rows.
   *
   * @param resource never {@code null}; must declare HTTP adapter type and a safe URL
   * @param request unused for GET mapping (reserved for later filter params)
   * @return mapped rows, never {@code null} or empty when the fixture has records
   */
  List<Map<String, Object>> query(PipelineResourceIr resource, PipelineExecuteRequest request)
      throws PSPipelineIrException;
}
