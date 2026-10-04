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

package com.percussion.rest.communities;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Description body for one community (issue #5178).
 *
 * <p>{@code description} is the new community description. Empty or whitespace clears it. Name and
 * role membership are not fields on this body and stay as they were. Jackson root wrap is {@code
 * CommunityDescription}.
 */
@XmlRootElement(name = "CommunityDescription")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Community description body (description only)")
public class CommunityDescription {

  @Schema(
      description =
          "New community description (max 255 characters). Empty or whitespace clears the stored"
              + " description. Name and roles are unchanged.")
  private String description;

  public CommunityDescription() {}

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
