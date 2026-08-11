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

package com.percussion.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Structured design-capability gap on Developer detail payloads (REST-GAPS-01).
 *
 * <p>Replaces free-text {@code designGaps} string entries so clients can group, link, and
 * optionally i18n by stable {@link #code}. {@link #message} remains English human text for this
 * slice (locale packs may map codes later).
 *
 * <p><strong>Wire shape:</strong> JSON objects {@code { "code": "...", "message": "..." }} — not
 * bare strings. Peers that still emit string arrays will migrate in follow-up slices.
 */
@XmlRootElement(name = "DesignGap")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured design capability gap (code + message)")
public class DesignGap {

  @Schema(
      description = "Stable machine-readable gap code for SPA grouping / docs links",
      example = "CT_FIELD_RULE_EXPR")
  private String code;

  @Schema(
      description = "Human-readable English message (i18n via code may follow later)",
      example = "Field rule expressions are not exposed")
  private String message;

  public DesignGap() {}

  public DesignGap(String code, String message) {
    this.code = code;
    this.message = message;
  }

  /** Factory for adaptors / tests. */
  public static DesignGap of(String code, String message) {
    return new DesignGap(code, message);
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DesignGap that)) {
      return false;
    }
    return Objects.equals(code, that.code) && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message);
  }

  @Override
  public String toString() {
    return "DesignGap{code='" + code + "', message='" + message + "'}";
  }
}
