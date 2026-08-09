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
 * Wire request for public create-variant (content-item translation) — SOAP {@code
 * content.NewTranslations} parity.
 *
 * <p>When {@link #locales} is null or empty, the server uses all currently defined system auto
 * translations (same default as SOAP). When provided, only those target locales are created.
 */
@XmlRootElement(name = "CreateTranslationsRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Create content-item locale variants (NewTranslations façade)")
public class CreateTranslationsRequest {

  @Schema(
      description = "Legacy content ids of source items to translate",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<Long> itemIds = new ArrayList<>();

  @Schema(
      description =
          "Target locale language strings (e.g. fr-fr). Empty/null = all system auto-translations.")
  private List<String> locales;

  @Schema(
      description =
          "Translation relationship type name; must be Translation category. Defaults to"
              + " System/Translation when omitted.")
  private String relationshipType;

  @Schema(
      description =
          "When true, turn on revisions for new items immediately. Defaults to false (SOAP"
              + " parity).")
  private Boolean enableRevisions;

  public CreateTranslationsRequest() {}

  public List<Long> getItemIds() {
    return itemIds;
  }

  public void setItemIds(List<Long> itemIds) {
    this.itemIds = itemIds != null ? itemIds : new ArrayList<>();
  }

  public List<String> getLocales() {
    return locales;
  }

  public void setLocales(List<String> locales) {
    this.locales = locales;
  }

  public String getRelationshipType() {
    return relationshipType;
  }

  public void setRelationshipType(String relationshipType) {
    this.relationshipType = relationshipType;
  }

  public Boolean getEnableRevisions() {
    return enableRevisions;
  }

  public void setEnableRevisions(Boolean enableRevisions) {
    this.enableRevisions = enableRevisions;
  }
}
