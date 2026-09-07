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

package com.percussion.services.pipeline.model;

import java.util.Locale;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Per-resource HTTP webhook pre/post execute hooks (Slice C). Loopback / local fixture URLs only.
 *
 * <p>Blank URLs mean skip (no invented deliveries). Present URLs are fail-closed SSRF-checked at
 * persist and execute.
 */
public class PipelineWebhookHooksIr {

  public static final String METHOD_POST = "POST";

  private String preUrl;
  private String postUrl;
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

  /** True when at least one hook URL is configured. */
  public boolean isPresent() {
    return StringUtils.isNotBlank(preUrl) || StringUtils.isNotBlank(postUrl);
  }

  /** Normalized HTTP method; default POST. */
  public String resolvedMethod() {
    if (StringUtils.isBlank(httpMethod)) {
      return METHOD_POST;
    }
    return httpMethod.trim().toUpperCase(Locale.ROOT);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineWebhookHooksIr that)) {
      return false;
    }
    return Objects.equals(preUrl, that.preUrl)
        && Objects.equals(postUrl, that.postUrl)
        && Objects.equals(httpMethod, that.httpMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(preUrl, postUrl, httpMethod);
  }
}
