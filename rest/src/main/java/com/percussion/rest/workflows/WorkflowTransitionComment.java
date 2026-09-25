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
 * Body for setting whether an existing transition requires a comment (slice 38).
 *
 * <p>Jackson root wrap is {@code WorkflowTransitionComment}.
 */
@XmlRootElement(name = "WorkflowTransitionComment")
@Schema(description = "Whether the addressed transition requires a comment")
public class WorkflowTransitionComment {

  @Schema(description = "True when the transition dialog must block an empty comment")
  private boolean commentRequired;

  public boolean isCommentRequired() {
    return commentRequired;
  }

  public void setCommentRequired(boolean commentRequired) {
    this.commentRequired = commentRequired;
  }
}
