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

package com.percussion.rest.actions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;

/**
 * Request object for allowed workflow transitions.
 *
 * <p>Wire getters return plain nullable types (not {@code Optional}) so Jackson/CXF JSON emits
 * {@code contentIds} and {@code assignmentTypeIds} as JSON arrays, not Optional beans (issue #3388
 * slice 10 / #3432).
 *
 * <p>Root name is explicit so JAXB/Jettison cannot treat an {@code ActionMenu} envelope as this
 * type on {@code POST /actions} (#4171). Finder POST is {@code /actions/find/transitions}.
 */
@XmlRootElement(name = "allowedWorkflowTransitionsRequest")
@JsonRootName("allowedWorkflowTransitionsRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllowedWorkflowTransitionsRequest {

  /** Content ids to evaluate transitions for. */
  private int[] contentIds;

  /** Assignment type ids. */
  private int[] assignmentTypeIds;

  /** No-op constructor. */
  public AllowedWorkflowTransitionsRequest() {}

  /**
   * Returns the content ids.
   *
   * @return the content ids, or {@code null} if unset
   */
  public int[] getContentIds() {
    return contentIds;
  }

  /**
   * Sets the content ids.
   *
   * @param contentIds the new content ids
   */
  public void setContentIds(int[] contentIds) {
    this.contentIds = contentIds;
  }

  /**
   * Returns the assignment type ids.
   *
   * @return the assignment type ids, or {@code null} if unset
   */
  public int[] getAssignmentTypeIds() {
    return assignmentTypeIds;
  }

  /**
   * Sets the assignment type ids.
   *
   * @param assignmentTypeIds the new assignment type ids
   */
  public void setAssignmentTypeIds(int[] assignmentTypeIds) {
    this.assignmentTypeIds = assignmentTypeIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AllowedWorkflowTransitionsRequest)) return false;
    var that = (AllowedWorkflowTransitionsRequest) o;
    return Arrays.equals(contentIds, that.contentIds)
        && Arrays.equals(assignmentTypeIds, that.assignmentTypeIds);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(contentIds);
    result = 31 * result + Arrays.hashCode(assignmentTypeIds);
    return result;
  }

  @Override
  public String toString() {
    return "AllowedWorkflowTransitionsRequest{"
        + "contentIds="
        + Arrays.toString(contentIds)
        + ", assignmentTypeIds="
        + Arrays.toString(assignmentTypeIds)
        + '}';
  }
}