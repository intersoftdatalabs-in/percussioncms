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
 * Content-editor system definition command-handler stylesheet associations (CD-16).
 *
 * <p>Jackson root wrap is {@code SystemDefStylesheets}. GET lists handlers. PUT is a full replace
 * of the handler set: omitted handlers are removed; a blank {@code href} removes that handler. At
 * least one handler with a valid href is required (object-store XML).
 */
@XmlRootElement(name = "SystemDefStylesheets")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "System-def command handler stylesheet associations (CD-16)")
public class SystemDefStylesheets {

  @Schema(
      required = true,
      description =
          "Command handler associations. PUT: full replace. Empty or all-blank hrefs are 400.")
  private List<SystemDefCommandHandlerStylesheet> handlers = new ArrayList<>();

  @Schema(description = "Structured capability notes vs full Workbench. GET always present.")
  private List<DesignGap> designGaps = new ArrayList<>();

  public SystemDefStylesheets() {}

  public List<SystemDefCommandHandlerStylesheet> getHandlers() {
    return handlers;
  }

  public void setHandlers(List<SystemDefCommandHandlerStylesheet> handlers) {
    this.handlers = handlers != null ? handlers : new ArrayList<>();
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
