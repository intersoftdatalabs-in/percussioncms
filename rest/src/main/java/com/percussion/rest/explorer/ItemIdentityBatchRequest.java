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

package com.percussion.rest.explorer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Request body for {@code POST /rest/explorer/identity/batch} (#6783 / parent #4530).
 *
 * <p>{@link JsonRootName} matches {@code JacksonContextResolver} WRAP/UNWRAP_ROOT_VALUE: the live
 * wire form is {@code {"ItemIdentityBatchRequest": {"ids": [...]}}}. The resource also accepts a
 * flat {@code {"ids": [...]}} body.
 */
@XmlRootElement(name = "ItemIdentityBatchRequest")
@JsonRootName("ItemIdentityBatchRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Row ids whose visibility identity to resolve (bounded batch)")
public class ItemIdentityBatchRequest {

  private List<String> ids;

  public ItemIdentityBatchRequest() {}

  public ItemIdentityBatchRequest(List<String> ids) {
    this.ids = ids;
  }

  @XmlElement
  public List<String> getIds() {
    return ids;
  }

  public void setIds(List<String> ids) {
    this.ids = ids;
  }
}
