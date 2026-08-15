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
 * Optional body for {@code POST /sites/{nameOrId}/virtual/build}.
 *
 * <p>When omitted or empty, the server chooses a default output directory under the CMS install
 * {@code tmp/virtual-sites/} tree (or the JVM temp directory when the install root is unavailable).
 *
 * <p>Wire getters return plain {@code String} (not {@code Optional}) so Jackson emits scalars, not
 * Optional-bean {@code empty}/{@code present} keys (#3411 / #3388).
 */
@XmlRootElement(name = "VirtualSiteBuildRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "VirtualSiteBuildRequest",
    description = "Optional Virtual Site build request (output path override)")
public class VirtualSiteBuildRequest {

  @Schema(
      description =
          "Filesystem directory for static HTML output. Absolute path preferred. When blank, the"
              + " server uses {install}/tmp/virtual-sites/{siteKey} (portable NIO Path).",
      example = "C:/tmp/product-docs-site")
  private String outputRoot;

  public VirtualSiteBuildRequest() {
    // Default constructor for JAX-RS / Jackson
  }

  public String getOutputRoot() {
    return outputRoot;
  }

  public void setOutputRoot(String outputRoot) {
    this.outputRoot = outputRoot;
  }
}
