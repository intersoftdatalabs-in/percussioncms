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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-handler stylesheet association on the content-editor system definition (CD-16).
 *
 * <p>{@code href} is the default stylesheet (last entry in the Workbench collection). Blank {@code
 * href} on PUT removes the handler. {@code conditionals} are GET-only.
 */
@XmlRootElement(name = "SystemDefCommandHandlerStylesheet")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "System-def command handler stylesheet association")
public class SystemDefCommandHandlerStylesheet {

  @Schema(description = "Command handler name (preview, edit, …)", required = true)
  private String commandHandler;

  @Schema(
      description =
          "Default stylesheet href. PUT: required unless clearing the handler. Blank removes."
              + " Allowed: file:../sys_resources/stylesheets/*.xsl or"
              + " file:../rx_resources/stylesheets/*.xsl")
  private String href;

  @Schema(description = "Conditional stylesheet hrefs (GET). PUT ignores and preserves.")
  private List<SystemDefConditionalStylesheet> conditionals;

  public SystemDefCommandHandlerStylesheet() {}

  public String getCommandHandler() {
    return commandHandler;
  }

  public void setCommandHandler(String commandHandler) {
    this.commandHandler = commandHandler;
  }

  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public List<SystemDefConditionalStylesheet> getConditionals() {
    return conditionals;
  }

  public void setConditionals(List<SystemDefConditionalStylesheet> conditionals) {
    this.conditionals = conditionals != null ? conditionals : new ArrayList<>();
  }
}
