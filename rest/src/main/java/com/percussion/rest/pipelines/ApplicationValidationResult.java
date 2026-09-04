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

package com.percussion.rest.pipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation / problems summary for one classic XML Application / pipeline package.
 *
 * <p>Returned by Admin {@code GET /services/pipelines/{idOrName}/validation}. Empty {@link
 * #problems} with {@link #valid}{@code true} means object-store validation reported no errors or
 * warnings.
 */
@XmlRootElement(name = "ApplicationValidationResult")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pipeline application validation / problems summary")
public class ApplicationValidationResult {

  /** Numeric application id when known. */
  private Integer id;

  /** Trusted catalog application name. */
  private String name;

  /** {@code true} when there are no ERROR-severity problems. */
  private Boolean valid;

  /** Count of ERROR-severity problems. */
  private Integer errorCount;

  /** Count of WARNING-severity problems. */
  private Integer warningCount;

  /** Collected problems (errors and warnings). */
  private List<ApplicationValidationProblem> problems = new ArrayList<>();

  public ApplicationValidationResult() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getValid() {
    return valid;
  }

  public void setValid(Boolean valid) {
    this.valid = valid;
  }

  public Integer getErrorCount() {
    return errorCount;
  }

  public void setErrorCount(Integer errorCount) {
    this.errorCount = errorCount;
  }

  public Integer getWarningCount() {
    return warningCount;
  }

  public void setWarningCount(Integer warningCount) {
    this.warningCount = warningCount;
  }

  public List<ApplicationValidationProblem> getProblems() {
    return problems;
  }

  public void setProblems(List<ApplicationValidationProblem> problems) {
    this.problems = problems != null ? problems : new ArrayList<>();
  }
}
