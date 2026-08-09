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

/** One content-item locale variant (source or translation copy). */
@XmlRootElement(name = "TranslationVariant")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content-item locale variant summary")
public class TranslationVariant {

  @Schema(description = "Legacy content id of this variant")
  private long contentId;

  @Schema(description = "Current revision when known")
  private Integer revision;

  @Schema(description = "Locale language string (e.g. en-us, fr-fr)")
  private String locale;

  @Schema(
      description =
          "Role of this row relative to the requested item: source (the item itself) or"
              + " translation (related locale copy).")
  private String role;

  @Schema(description = "Source content id that this translation was created from, when known")
  private Long sourceContentId;

  public TranslationVariant() {}

  public long getContentId() {
    return contentId;
  }

  public void setContentId(long contentId) {
    this.contentId = contentId;
  }

  public Integer getRevision() {
    return revision;
  }

  public void setRevision(Integer revision) {
    this.revision = revision;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Long getSourceContentId() {
    return sourceContentId;
  }

  public void setSourceContentId(Long sourceContentId) {
    this.sourceContentId = sourceContentId;
  }
}
