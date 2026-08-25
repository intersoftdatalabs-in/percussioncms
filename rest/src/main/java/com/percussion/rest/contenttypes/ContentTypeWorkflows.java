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

package com.percussion.rest.contenttypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Full-replace body for Content Type allowed-workflow associations (CD-08).
 *
 * <p>Jackson root wrap is {@code ContentTypeWorkflows} ({@code WRAP_ROOT_VALUE} / {@code
 * UNWRAP_ROOT_VALUE}). {@code allowedWorkflows} is required (empty list clears associations).
 * {@code defaultWorkflow} is optional.
 */
@XmlRootElement(name = "ContentTypeWorkflows")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content type allowed-workflow association replace body")
public class ContentTypeWorkflows {

  @Schema(
      required = true,
      description =
          "Allowed workflows (full replace). Empty list clears associations. Each entry needs"
              + " name or guid of an existing workflow.")
  private List<NamedObjectRef> allowedWorkflows;

  @Schema(description = "Optional default workflow (name or guid). Included in the allowed list.")
  private NamedObjectRef defaultWorkflow;

  public ContentTypeWorkflows() {}

  public List<NamedObjectRef> getAllowedWorkflows() {
    return allowedWorkflows;
  }

  public void setAllowedWorkflows(List<NamedObjectRef> allowedWorkflows) {
    this.allowedWorkflows = allowedWorkflows;
  }

  public NamedObjectRef getDefaultWorkflow() {
    return defaultWorkflow;
  }

  public void setDefaultWorkflow(NamedObjectRef defaultWorkflow) {
    this.defaultWorkflow = defaultWorkflow;
  }
}
