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

package com.percussion.rest.locales;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only CMS locale detail.
 *
 * <p>Create/edit/delete and auto-translation settings are later slices.
 */
@XmlRootElement(name = "LocaleDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CMS locale detail")
public class LocaleDetail extends LocaleSummary {

  /** Exact-match RXLOCALEFORMAT row when present; null if none stored for this language string. */
  private LocaleFormatSummary format;

  private List<String> designGaps = new ArrayList<>();

  public LocaleDetail() {}

  public LocaleFormatSummary getFormat() {
    return format;
  }

  public void setFormat(LocaleFormatSummary format) {
    this.format = format;
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
