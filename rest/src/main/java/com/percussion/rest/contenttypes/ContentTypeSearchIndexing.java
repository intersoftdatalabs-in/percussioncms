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
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Type-level search indexing flag for a Content Type (CD-10).
 *
 * <p>Maps Workbench Properties {@code Enable searching for this Content Type} (root mapper
 * field-set {@code isUserSearchable}). Default is on. Distinct from per-field {@code searchable}
 * on PUT detail / local-field create.
 *
 * <p>Jackson root wrap is {@code ContentTypeSearchIndexing} ({@code WRAP_ROOT_VALUE} /
 * {@code UNWRAP_ROOT_VALUE}).
 */
@XmlRootElement(name = "ContentTypeSearchIndexing")
@JsonRootName("ContentTypeSearchIndexing")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content type type-level search indexing flag")
public class ContentTypeSearchIndexing {

  @Schema(
      required = true,
      description =
          "When true, items of this content type may be indexed for search. Default is true"
              + " (Workbench default-on). Does not change per-field searchable.")
  private Boolean searchIndexing;

  public ContentTypeSearchIndexing() {}

  public ContentTypeSearchIndexing(boolean searchIndexing) {
    this.searchIndexing = searchIndexing;
  }

  public Boolean getSearchIndexing() {
    return searchIndexing;
  }

  public void setSearchIndexing(Boolean searchIndexing) {
    this.searchIndexing = searchIndexing;
  }
}
