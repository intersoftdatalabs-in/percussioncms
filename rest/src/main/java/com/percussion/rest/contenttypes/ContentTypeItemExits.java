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
import com.percussion.rest.DesignGap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Item-level content-type exits and validations (CD-09).
 *
 * <p>Jackson root wrap is {@code ContentTypeItemExits}. GET always returns the five lists (empty
 * when none). PUT is a full replace of {@code inputTranslations}, {@code outputTranslations}, and
 * {@code validations} (empty list clears). {@code preExits} / {@code postExits} omitted leave pipe
 * extensions unchanged; empty list clears. Apply-when conditions are read-only (see {@code
 * designGaps}).
 */
@XmlRootElement(name = "ContentTypeItemExits")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Item-level content type exits, translations, and validations (CD-09)")
public class ContentTypeItemExits {

  @Schema(
      required = true,
      description =
          "Item input translations. GET: always present (may be []). PUT: required; empty clears.")
  private List<ContentTypeItemExit> inputTranslations;

  @Schema(
      required = true,
      description =
          "Item output translations. GET: always present (may be []). PUT: required; empty clears.")
  private List<ContentTypeItemExit> outputTranslations;

  @Schema(
      required = true,
      description =
          "Item validation exits. GET: always present (may be []). PUT: required; empty clears.")
  private List<ContentTypeItemExit> validations;

  @Schema(
      description =
          "Dataset pipe pre-exits (input data extensions). GET: always present (may be [])."
              + " PUT: omit/null leave unchanged; non-null list full replace (empty clears).")
  private List<ContentTypeItemExit> preExits;

  @Schema(
      description =
          "Dataset pipe post-exits (result data extensions). GET: always present (may be [])."
              + " PUT: omit/null leave unchanged; non-null list full replace (empty clears).")
  private List<ContentTypeItemExit> postExits;

  @Schema(
      description =
          "Max errors before item validation stops. GET: always present. PUT: omit keeps current.")
  private Integer maxErrorsToStopValidation;

  @Schema(
      description =
          "Structured capability notes vs full Workbench (apply-when write). GET always present.")
  private List<DesignGap> designGaps = new ArrayList<>();

  public ContentTypeItemExits() {}

  public List<ContentTypeItemExit> getInputTranslations() {
    return inputTranslations;
  }

  public void setInputTranslations(List<ContentTypeItemExit> inputTranslations) {
    this.inputTranslations = inputTranslations;
  }

  public List<ContentTypeItemExit> getOutputTranslations() {
    return outputTranslations;
  }

  public void setOutputTranslations(List<ContentTypeItemExit> outputTranslations) {
    this.outputTranslations = outputTranslations;
  }

  public List<ContentTypeItemExit> getValidations() {
    return validations;
  }

  public void setValidations(List<ContentTypeItemExit> validations) {
    this.validations = validations;
  }

  public List<ContentTypeItemExit> getPreExits() {
    return preExits;
  }

  public void setPreExits(List<ContentTypeItemExit> preExits) {
    this.preExits = preExits;
  }

  public List<ContentTypeItemExit> getPostExits() {
    return postExits;
  }

  public void setPostExits(List<ContentTypeItemExit> postExits) {
    this.postExits = postExits;
  }

  public Integer getMaxErrorsToStopValidation() {
    return maxErrorsToStopValidation;
  }

  public void setMaxErrorsToStopValidation(Integer maxErrorsToStopValidation) {
    this.maxErrorsToStopValidation = maxErrorsToStopValidation;
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
