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
package com.percussion.itemmanagement.workflow;

import java.util.Collection;

/**
 * Decides whether an EditorHost workflow reassignment is allowed (#4861).
 *
 * <p>The requested id must be a positive integer, different from the item's current workflow, and
 * present in the content type's associated workflow ids. Anything else is a failure — callers must
 * not report success.
 */
public final class ItemWorkflowAssignmentRules {

  public enum Reason {
    OK,
    BLANK,
    MALFORMED,
    UNCHANGED,
    FORBIDDEN
  }

  private ItemWorkflowAssignmentRules() {}

  /**
   * @param requested raw workflow id from the client
   * @param currentWorkflowId the item's current workflow app id
   * @param allowedWorkflowIds content-type associations; {@code null} allows nothing
   */
  public static Reason decide(
      String requested, int currentWorkflowId, Collection<Integer> allowedWorkflowIds) {
    if (requested == null || requested.isBlank()) {
      return Reason.BLANK;
    }
    final int id;
    try {
      id = Integer.parseInt(requested.trim());
    } catch (NumberFormatException ex) {
      return Reason.MALFORMED;
    }
    if (id <= 0) {
      return Reason.MALFORMED;
    }
    if (id == currentWorkflowId) {
      return Reason.UNCHANGED;
    }
    if (allowedWorkflowIds == null || !allowedWorkflowIds.contains(id)) {
      return Reason.FORBIDDEN;
    }
    return Reason.OK;
  }
}
