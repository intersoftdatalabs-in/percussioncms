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
import com.percussion.rest.DesignGap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Field-level validation, visibility, and input/output translation expressions (CD-05–CD-07).
 *
 * <p>Jackson root wrap is {@code ContentTypeFieldRuleExpressions}. GET always returns the four
 * lists (may be empty) plus summary strings when rules exist. PUT is a full replace of {@code
 * validation}, {@code visibility}, {@code inputTranslation}, and {@code outputTranslation} (empty
 * list clears). Requires a held design-session lock.
 *
 * <p>Jackson 3.2 still ships {@link JsonRootName} under {@code com.fasterxml.jackson.annotation}
 * (there is no {@code tools.jackson.annotation} package). {@link
 * com.percussion.rest.JacksonContextResolver} reads this for GET {@code WRAP_ROOT_VALUE}. PUT uses
 * {@link ContentTypeFieldRuleExpressionsJsonReader} so CXF {@code UNWRAP_ROOT_VALUE} cannot drop
 * required lists.
 */
@XmlRootElement(name = "ContentTypeFieldRuleExpressions")
@JsonRootName("ContentTypeFieldRuleExpressions")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Field validation, visibility, and translation expressions (CD-05–07)")
public class ContentTypeFieldRuleExpressions {

  @Schema(description = "Field submit name (path {fieldName})")
  private String fieldName;

  @Schema(
      required = true,
      description =
          "Validation rules. GET: always present (may be []). PUT: required; empty clears.")
  private List<ContentTypeFieldRule> validation;

  @Schema(
      required = true,
      description =
          "Visibility rules. GET: always present (may be []). PUT: required; empty clears."
              + " type=reference is not allowed.")
  private List<ContentTypeFieldRule> visibility;

  @Schema(
      required = true,
      description =
          "Input translation extension calls. GET: always present (may be []). PUT: required;"
              + " empty clears.")
  private List<ContentTypeItemExit> inputTranslation;

  @Schema(
      required = true,
      description =
          "Output translation extension calls. GET: always present (may be []). PUT: required;"
              + " empty clears.")
  private List<ContentTypeItemExit> outputTranslation;

  @Schema(
      description =
          "Max errors before field validation stops. GET: present when validation rules exist."
              + " PUT: omit keeps current (or default 10 when creating rules).")
  private Integer maxErrorsToStop;

  @Schema(description = "Validation error message text. PUT: omit leaves unchanged; empty clears.")
  private String errorMessage;

  @Schema(description = "GET convenience: summary of validation rules (same as detail field row)")
  private String validationExpression;

  @Schema(description = "GET convenience: summary of visibility rules")
  private String visibilityExpression;

  @Schema(description = "GET convenience: summary of input translation calls")
  private String inputTranslationExpression;

  @Schema(description = "GET convenience: summary of output translation calls")
  private String outputTranslationExpression;

  @Schema(description = "Structured capability notes vs full Workbench. GET always present.")
  private List<DesignGap> designGaps = new ArrayList<>();

  public ContentTypeFieldRuleExpressions() {}

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public List<ContentTypeFieldRule> getValidation() {
    return validation;
  }

  public void setValidation(List<ContentTypeFieldRule> validation) {
    this.validation = validation;
  }

  public List<ContentTypeFieldRule> getVisibility() {
    return visibility;
  }

  public void setVisibility(List<ContentTypeFieldRule> visibility) {
    this.visibility = visibility;
  }

  public List<ContentTypeItemExit> getInputTranslation() {
    return inputTranslation;
  }

  public void setInputTranslation(List<ContentTypeItemExit> inputTranslation) {
    this.inputTranslation = inputTranslation;
  }

  public List<ContentTypeItemExit> getOutputTranslation() {
    return outputTranslation;
  }

  public void setOutputTranslation(List<ContentTypeItemExit> outputTranslation) {
    this.outputTranslation = outputTranslation;
  }

  public Integer getMaxErrorsToStop() {
    return maxErrorsToStop;
  }

  public void setMaxErrorsToStop(Integer maxErrorsToStop) {
    this.maxErrorsToStop = maxErrorsToStop;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getValidationExpression() {
    return validationExpression;
  }

  public void setValidationExpression(String validationExpression) {
    this.validationExpression = validationExpression;
  }

  public String getVisibilityExpression() {
    return visibilityExpression;
  }

  public void setVisibilityExpression(String visibilityExpression) {
    this.visibilityExpression = visibilityExpression;
  }

  public String getInputTranslationExpression() {
    return inputTranslationExpression;
  }

  public void setInputTranslationExpression(String inputTranslationExpression) {
    this.inputTranslationExpression = inputTranslationExpression;
  }

  public String getOutputTranslationExpression() {
    return outputTranslationExpression;
  }

  public void setOutputTranslationExpression(String outputTranslationExpression) {
    this.outputTranslationExpression = outputTranslationExpression;
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
