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
 * Body for restricting one existing regular transition to exactly one workflow role (slice 72).
 *
 * <p>Jackson root wrap is {@code WorkflowTransitionAllowedRole}. One {@code roleName} replaces
 * allow-all. This body does not add a second role and does not clear the restriction.
 */
@XmlRootElement(name = "WorkflowTransitionAllowedRole")
@Schema(description = "The single workflow role allowed to fire the addressed transition")
public class WorkflowTransitionAllowedRole {

  @Schema(description = "Existing workflow role name. Exactly one. Not the allow-all marker.")
  private String roleName;

  /** Role name, or {@code null} when omitted. */
  public String getRoleName() {
    return roleName;
  }

  /** @param roleName existing workflow role; blank is rejected */
  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }
}
