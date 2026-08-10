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

/** Read-only summary of one data set inside a classic XML Application. */
@XmlRootElement(name = "ApplicationDataSet")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Data set summary within a pipeline application")
public class ApplicationDataSetSummary {

  /** Data set name. */
  private String name;

  /** Free-form description. */
  private String description;

  /** Request page URL of the data set. */
  private String requestPage;

  /**
   * Data set kind, e.g. {@code CONTENT_EDITOR} when the data set is a PSContentEditor, otherwise
   * {@code DATASET}.
   */
  private String kind;

  /** No-op constructor. */
  public ApplicationDataSetSummary() {}

  /**
   * Returns the data set name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the data set name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the data set description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the data set description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the request page URL.
   *
   * @return the request page
   */
  public String getRequestPage() {
    return requestPage;
  }

  /**
   * Sets the request page URL.
   *
   * @param requestPage the new request page
   */
  public void setRequestPage(String requestPage) {
    this.requestPage = requestPage;
  }

  /**
   * Returns the data set kind.
   *
   * @return the kind
   */
  public String getKind() {
    return kind;
  }

  /**
   * Sets the data set kind.
   *
   * @param kind the new kind
   */
  public void setKind(String kind) {
    this.kind = kind;
  }
}
