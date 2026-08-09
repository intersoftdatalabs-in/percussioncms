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

/**
 * Per-item locale variant listing for Explorer P-Trans.
 *
 * <p>Includes the requested item (role {@code source}) and translation-category dependents (role
 * {@code translation}). In-flight translation queue / session content-locale context remain product
 * disposition (not exposed here).
 */
@XmlRootElement(name = "ItemTranslationVariants")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Locale variants related to a content item")
public class ItemTranslationVariants {

  @Schema(description = "Requested content id")
  private long itemId;

  @Schema(description = "Locale of the requested item")
  private String locale;

  @Schema(description = "Source item plus related translation variants")
  private List<TranslationVariant> variants = new ArrayList<>();

  public ItemTranslationVariants() {}

  public long getItemId() {
    return itemId;
  }

  public void setItemId(long itemId) {
    this.itemId = itemId;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public List<TranslationVariant> getVariants() {
    return variants;
  }

  public void setVariants(List<TranslationVariant> variants) {
    this.variants = variants != null ? variants : new ArrayList<>();
  }
}
