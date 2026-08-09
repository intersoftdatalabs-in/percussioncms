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

package com.percussion.rest.translations;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Result envelope for create-variant (NewTranslations façade). */
@XmlRootElement(name = "CreateTranslationsResult")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Newly created content-item locale variants")
public class CreateTranslationsResult {

  @Schema(description = "Created translation items (persisted, read-only mode)")
  private List<TranslationVariant> created = new ArrayList<>();

  public CreateTranslationsResult() {}

  public CreateTranslationsResult(List<TranslationVariant> created) {
    this.created = created != null ? created : new ArrayList<>();
  }

  public List<TranslationVariant> getCreated() {
    return created;
  }

  public void setCreated(List<TranslationVariant> created) {
    this.created = created != null ? created : new ArrayList<>();
  }
}
