/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

/** Read-only summary of one data set inside a classic XML Application. */
@XmlRootElement(name = "ApplicationDataSet")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Data set summary within a pipeline application")
public class ApplicationDataSetSummary {

  private String name;
  private String description;
  private String requestPage;
  /** e.g. CONTENT_EDITOR when the dataset is a PSContentEditor, otherwise DATASET */
  private String kind;

  public ApplicationDataSetSummary() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRequestPage() {
    return requestPage;
  }

  public void setRequestPage(String requestPage) {
    this.requestPage = requestPage;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }
}
