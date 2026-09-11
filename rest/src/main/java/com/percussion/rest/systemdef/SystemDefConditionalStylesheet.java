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

/**
 * Read-only conditional stylesheet association on a command handler (CD-16). PUT of the handler
 * default {@code href} preserves these rows; rule expressions stay Workbench / later slices.
 */
@XmlRootElement(name = "SystemDefConditionalStylesheet")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Conditional system-def stylesheet association (read-only href)")
public class SystemDefConditionalStylesheet {

  @Schema(description = "Stylesheet request href (file:../sys_resources|rx_resources/stylesheets/*.xsl)")
  private String href;

  public SystemDefConditionalStylesheet() {}

  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }
}
