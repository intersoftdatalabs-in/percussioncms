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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.Optional;

/** Request object for allowed workflow transitions. */
@XmlRootElement
public class AllowedWorkflowTransitionsRequest {

  private int[] contentIds;
  private int[] assignmentTypeIds;

  public AllowedWorkflowTransitionsRequest() {}

  public Optional<int[]> getContentIds() {
    return Optional.ofNullable(contentIds);
  }

  public void setContentIds(int[] contentIds) {
    this.contentIds = contentIds;
  }

  public Optional<int[]> getAssignmentTypeIds() {
    return Optional.ofNullable(assignmentTypeIds);
  }

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
