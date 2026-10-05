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

package com.percussion.rest.workflows;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Body for setting how many approvals one existing transition requires (slice 70).
 *
 * <p>Jackson root wrap is {@code WorkflowTransitionApprovals}. A negative count is rejected.
 * {@code -1} stored on the transition means each-role approval and is not written by this body.
 */
@XmlRootElement(name = "WorkflowTransitionApprovals")
@Schema(description = "How many approvals the addressed transition requires")
public class WorkflowTransitionApprovals {

  @Schema(description = "Non-negative number of approvals required before the transition runs")
  private Integer approvalsRequired;

  /** Non-negative approval count, or {@code null} when the body omitted it. */
  public Integer getApprovalsRequired() {
    return approvalsRequired;
  }

  /** @param approvalsRequired non-negative whole number, including zero */
  public void setApprovalsRequired(Integer approvalsRequired) {
    this.approvalsRequired = approvalsRequired;
  }
}
