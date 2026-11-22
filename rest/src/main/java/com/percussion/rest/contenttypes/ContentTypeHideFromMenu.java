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

/**
 * Hide-from-menu flag for a Content Type design action (CD-01 meta write).
 *
 * <p>Jackson root wrap is {@code ContentTypeHideFromMenu} ({@code WRAP_ROOT_VALUE} /
 * {@code UNWRAP_ROOT_VALUE}).
 */
@XmlRootElement(name = "ContentTypeHideFromMenu")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content type hide-from-menu flag")
public class ContentTypeHideFromMenu {

  @Schema(required = true, description = "When true, the content type is hidden from menus")
  private Boolean hideFromMenu;

  public ContentTypeHideFromMenu() {}

  public ContentTypeHideFromMenu(boolean hideFromMenu) {
    this.hideFromMenu = hideFromMenu;
  }

  public Boolean getHideFromMenu() {
    return hideFromMenu;
  }

  public void setHideFromMenu(Boolean hideFromMenu) {
    this.hideFromMenu = hideFromMenu;
  }
}
