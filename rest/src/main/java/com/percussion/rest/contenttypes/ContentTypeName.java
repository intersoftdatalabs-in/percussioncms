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
 * New internal name for a Content Type rename (CD-01).
 *
 * <p>Jackson root wrap is {@code ContentTypeName} ({@code WRAP_ROOT_VALUE} /
 * {@code UNWRAP_ROOT_VALUE}).
 */
@XmlRootElement(name = "ContentTypeName")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content type rename payload")
public class ContentTypeName {

  @Schema(
      required = true,
      description =
          "New internal name. Must be unique (case-insensitive), with no spaces or wildcards.")
  private String name;

  public ContentTypeName() {}

  public ContentTypeName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
