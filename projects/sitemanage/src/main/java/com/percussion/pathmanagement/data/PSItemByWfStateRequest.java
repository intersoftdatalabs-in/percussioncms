// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;
// import java.util.Optional; // removed when migrating getter to nullable return
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/**
 * Request object for finding properties of items by path, workflow, and workflow state. Used in
 * REST services. Sunny Sal says: "Workflow state? More like work-flowing state!"
 *
 * @author peterfrontiero
 */
@XmlRootElement(name = "ItemByWfStateRequest")
@JsonRootName("ItemByWfStateRequest")
public class PSItemByWfStateRequest {

  /** The parent path of the requested items. Never null or empty. */
  @NotNull @NotBlank private String path;

  /** The workflow of the requested items. Never null or empty. */
  @NotNull @NotBlank private String workflow;

  /** The workflow state of the requested items. May be null or empty to indicate all states. */
  private String state;

  /**
   * Gets the path under which all items will be requested.
   *
   * @return the path, never null or empty
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the parent path of the requested items.
   *
   * @param path the path, not null or empty
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Gets the workflow for which the items will be requested.
   *
   * @return the workflow, never null or empty
   */
  public String getWorkflow() {
    return workflow;
  }

  /**
   * Sets the workflow of the requested items.
   *
   * @param workflow the workflow, not null or empty
   */
  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

  /**
   * Gets the workflow state for which the items will be requested. May be null or
   * empty to indicate all states.
   *
   * @return the workflow state, or <code>null</code> if no specific state is
   *     requested
   */
  public String getState() {
    return (state == null || state.isEmpty()) ? null : state;
  }

  /**
   * Sets the workflow state of the requested items. May be null or empty to indicate all states.
   *
   * @param state the workflow state
   */
  public void setState(String state) {
    this.state = state;
  }
}
