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
package com.percussion.itemmanagement.service.impl;

/**
 * HTTP status for Explorer toolbar workflow transitions (#4723 / parent #4530).
 *
 * <p>A trigger that is not in the item's allowed list is forbidden (403). A
 * transition that requires a comment and has a blank comment is a conflict
 * (409) so the client can ask again without treating the item as transitioned.</p>
 */
public final class PSItemWorkflowTransitionGate {

  public static final int ALLOWED = 200;
  public static final int FORBIDDEN = 403;
  public static final int COMMENT_REQUIRED = 409;

  private PSItemWorkflowTransitionGate() {}

  /**
   * @param triggerAllowed whether the trigger is in the current state's allowlist
   * @param commentRequired whether the transition's comment policy is required
   * @param comment operator comment; blank counts as missing
   * @return {@link #FORBIDDEN}, {@link #COMMENT_REQUIRED}, or {@link #ALLOWED}
   */
  public static int httpStatus(boolean triggerAllowed, boolean commentRequired, String comment) {
    if (!triggerAllowed) {
      return FORBIDDEN;
    }
    if (commentRequired && isBlank(comment)) {
      return COMMENT_REQUIRED;
    }
    return ALLOWED;
  }

  private static boolean isBlank(String comment) {
    return comment == null || comment.isBlank();
  }
}
