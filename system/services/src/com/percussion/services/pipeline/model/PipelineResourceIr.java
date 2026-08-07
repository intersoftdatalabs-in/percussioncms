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

package com.percussion.services.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One request resource (classic {@code PSDataSet}) in pipeline IR form.
 *
 * <p>Kind values: {@link #KIND_QUERY}, {@link #KIND_UPDATE}, {@link #KIND_CONTENT_EDITOR}, {@link
 * #KIND_UNKNOWN}.
 */
public class PipelineResourceIr {

  public static final String KIND_QUERY = "QUERY";
  public static final String KIND_UPDATE = "UPDATE";
  public static final String KIND_CONTENT_EDITOR = "CONTENT_EDITOR";
  public static final String KIND_UNKNOWN = "UNKNOWN";

  private String name;
  private String description;
  private String kind = KIND_UNKNOWN;
  private String requestPage;
  private String transactionMode;
  private String pipeName;
  private PipelineStagesIr stages = new PipelineStagesIr();

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

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind != null ? kind : KIND_UNKNOWN;
  }

  public String getRequestPage() {
    return requestPage;
  }

  public void setRequestPage(String requestPage) {
    this.requestPage = requestPage;
  }

  public String getTransactionMode() {
    return transactionMode;
  }

  public void setTransactionMode(String transactionMode) {
    this.transactionMode = transactionMode;
  }

  public String getPipeName() {
    return pipeName;
  }

  public void setPipeName(String pipeName) {
    this.pipeName = pipeName;
  }

  public PipelineStagesIr getStages() {
    return stages;
  }

  public void setStages(PipelineStagesIr stages) {
    this.stages = stages != null ? stages : new PipelineStagesIr();
  }

  /**
   * Ordered inventory of present stage kinds for assertions and catalog UIs.
   *
   * @return non-null list of stage keys (e.g. {@code pageTank}, {@code mapper})
   */
  public List<String> presentStageInventory() {
    List<String> out = new ArrayList<>();
    PipelineStagesIr s = stages != null ? stages : new PipelineStagesIr();
    if (s.getPageTank() != null && s.getPageTank().isPresent()) {
      out.add("pageTank");
    }
    if (s.getBackendTank() != null && s.getBackendTank().isPresent()) {
      out.add("backendTank");
    }
    if (s.getMapper() != null && s.getMapper().isPresent()) {
      out.add("mapper");
    }
    if (s.getSelector() != null && s.getSelector().isPresent()) {
      out.add("selector");
    }
    if (s.getPager() != null && s.getPager().isPresent()) {
      out.add("pager");
    }
    if (s.getUpdater() != null && s.getUpdater().isPresent()) {
      out.add("updater");
    }
    return out;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineResourceIr that)) {
      return false;
    }
    return Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(kind, that.kind)
        && Objects.equals(requestPage, that.requestPage)
        && Objects.equals(transactionMode, that.transactionMode)
        && Objects.equals(pipeName, that.pipeName)
        && Objects.equals(stages, that.stages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, kind, requestPage, transactionMode, pipeName, stages);
  }
}
