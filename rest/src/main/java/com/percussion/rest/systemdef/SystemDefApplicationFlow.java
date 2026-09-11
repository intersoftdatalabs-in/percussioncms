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

package com.percussion.rest.systemdef;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.DesignGap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Content-editor system definition command-handler application flow (CD-16).
 *
 * <p>Jackson root wrap is {@code SystemDefApplicationFlow}. GET lists handlers. PUT is a full
 * replace of the handler set: omitted handlers are removed. Empty {@code href} keeps the handler
 * with an empty default path. At least one handler is required (object-store XML).
 */
@XmlRootElement(name = "SystemDefApplicationFlow")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "System-def command handler application flow (CD-16)")
public class SystemDefApplicationFlow {

  @Schema(
      required = true,
      description = "Command handler redirects. PUT: full replace. Empty list is 400.")
  private List<SystemDefCommandHandlerRedirect> handlers = new ArrayList<>();

  @Schema(description = "Structured capability notes vs full Workbench. GET always present.")
  private List<DesignGap> designGaps = new ArrayList<>();

  public SystemDefApplicationFlow() {}

  public List<SystemDefCommandHandlerRedirect> getHandlers() {
    return handlers;
  }

  public void setHandlers(List<SystemDefCommandHandlerRedirect> handlers) {
    this.handlers = handlers != null ? handlers : new ArrayList<>();
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
