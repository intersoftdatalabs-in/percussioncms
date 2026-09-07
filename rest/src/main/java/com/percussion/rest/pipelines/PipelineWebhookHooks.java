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
 * Native IR write for Slice C: persist HTTP webhook pre/post execute hook URLs (loopback / local
 * fixture only) on one resource.
 */
@XmlRootElement(name = "PipelineWebhookHooks")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Native pipeline HTTP webhook hooks (loopback / local fixture URL only)")
public class PipelineWebhookHooks {

  /** Pre-execute webhook URL; blank means skip (no delivery). */
  private String preUrl;

  /** Post-execute webhook URL; blank means skip (no delivery). */
  private String postUrl;

  /** HTTP method; POST only in this slice. */
  private String httpMethod;

  public String getPreUrl() {
    return preUrl;
  }

  public void setPreUrl(String preUrl) {
    this.preUrl = preUrl;
  }

  public String getPostUrl() {
    return postUrl;
  }

  public void setPostUrl(String postUrl) {
    this.postUrl = postUrl;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }
}
