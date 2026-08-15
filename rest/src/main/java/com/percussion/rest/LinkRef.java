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

package com.percussion.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Wire name/href pair for REST folder, page, and user recent-item links.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits scalars
 * rather than Optional beans ({@code empty}/{@code present}). Matches {@link
 * com.percussion.rest.contenttypes.ContentType} getter style (issue #3430 / #3388).
 */
@XmlRootElement(name = "LinkRef")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "LinkRef")
public class LinkRef {
  @Schema(required = false, description = "link")
  private String name;

  @Schema(required = false, description = "href to section or external source")
  private String href;

  public LinkRef() {}

  @JsonCreator
  public LinkRef(@JsonProperty("name") String name, @JsonProperty("href") String href) {
    this.name = name;
    this.href = href;
  }

  /**
   * @return link name, or {@code null} if unset
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return href, or {@code null} if unset
   */
  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }
}
