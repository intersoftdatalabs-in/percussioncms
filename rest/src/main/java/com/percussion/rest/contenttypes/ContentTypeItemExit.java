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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * One item-level content-type extension call (input/output translation, validation, or pipe
 * pre/post exit).
 *
 * <p>{@code condition} is a read-only apply-when summary on GET. PUT reconstructs the extension
 * call from {@code extension} (FQN) plus literal {@code parameters}; apply-when is not written.
 */
@XmlRootElement(name = "ContentTypeItemExit")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Item-level content type extension call")
public class ContentTypeItemExit {

  @Schema(
      description =
          "Fully-qualified extension ref (handler/context/name), e.g."
              + " Java/global/percussion/generic/sys_ToUpperCase. Required on PUT.")
  private String extension;

  @Schema(description = "Short extension name (GET convenience; ignored on PUT when extension is set)")
  private String name;

  @Schema(description = "Extension call parameters (literal values)")
  private List<ContentTypeItemExitParam> parameters = new ArrayList<>();

  @Schema(
      description =
          "Read-only apply-when condition summary. Null when the exit always runs. Not writable.")
  private String condition;

  @Schema(description = "Max errors to stop for this conditional exit (item translations/validations)")
  private Integer maxErrorsToStop;

  @Schema(description = "Human-readable call summary (GET); ignored on PUT")
  private String summary;

  public ContentTypeItemExit() {}

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ContentTypeItemExitParam> getParameters() {
    return parameters;
  }

  public void setParameters(List<ContentTypeItemExitParam> parameters) {
    this.parameters = parameters != null ? parameters : new ArrayList<>();
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public Integer getMaxErrorsToStop() {
    return maxErrorsToStop;
  }

  public void setMaxErrorsToStop(Integer maxErrorsToStop) {
    this.maxErrorsToStop = maxErrorsToStop;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }
}
