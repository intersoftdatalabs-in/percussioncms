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
 * Command-handler default application-flow redirect on the content-editor system definition
 * (CD-16).
 *
 * <p>{@code href} is the default redirect path (Href or {@code sys_MakeAbsLink} first text param).
 * Omitted handlers on PUT are removed. Empty {@code href} keeps the handler with an empty default
 * path (Workbench empty MakeAbsLink). {@code conditionals} are GET-only.
 */
@XmlRootElement(name = "SystemDefCommandHandlerRedirect")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "System-def command handler application-flow redirect")
public class SystemDefCommandHandlerRedirect {

  @Schema(description = "Command handler name (modify, relate, …)", required = true)
  private String commandHandler;

  @Schema(
      description =
          "Default redirect href. Relative ../sys_* or ../rx_* CMS app path ending in"
              + " .html/.xml/.jsp, or empty to keep an empty default path. PUT omit removes the"
              + " handler.")
  private String href;

  @Schema(description = "Conditional redirect hrefs (GET). PUT ignores and preserves.")
  private List<SystemDefConditionalRedirect> conditionals;

  public SystemDefCommandHandlerRedirect() {}

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

  public List<SystemDefConditionalRedirect> getConditionals() {
    return conditionals;
  }

  public void setConditionals(List<SystemDefConditionalRedirect> conditionals) {
    this.conditionals = conditionals != null ? conditionals : new ArrayList<>();
  }
}
