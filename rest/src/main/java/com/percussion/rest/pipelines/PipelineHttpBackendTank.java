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
 * Minimum native IR write for Slice C: persist HTTP backend tank adapter type + loopback/local
 * fixture URL on one resource.
 */
@XmlRootElement(name = "PipelineHttpBackendTank")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Native pipeline HTTP backend tank (loopback / local fixture URL only)")
public class PipelineHttpBackendTank {

  /** Adapter type: HTTP (or REST). */
  private String adapterType;

  /** Loopback or bundled local fixture URL. */
  private String url;

  /** HTTP method; GET only in this slice. */
  private String httpMethod;

  public String getAdapterType() {
    return adapterType;
  }

  public void setAdapterType(String adapterType) {
    this.adapterType = adapterType;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }
}
