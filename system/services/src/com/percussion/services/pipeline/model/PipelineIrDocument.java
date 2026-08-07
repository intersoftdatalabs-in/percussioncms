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
 * Versioned pipeline intermediate representation (IR) root document.
 *
 * <p>JSON-friendly tree: application → resources → pipe stages. See {@code
 * docs/developer-module/pipeline-ir-v1.md}.
 */
public class PipelineIrDocument {

  /** Current IR schema version written by this codebase. */
  public static final String CURRENT_IR_VERSION = "1.0";

  public static final String SOURCE_CLASSIC_IMPORT = "CLASSIC_IMPORT";
  public static final String SOURCE_NATIVE = "NATIVE";

  private String irVersion = CURRENT_IR_VERSION;
  private String source = SOURCE_NATIVE;
  private PipelineAppMeta app = new PipelineAppMeta();
  private List<PipelineResourceIr> resources = new ArrayList<>();

  public String getIrVersion() {
    return irVersion;
  }

  public void setIrVersion(String irVersion) {
    this.irVersion = irVersion;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public PipelineAppMeta getApp() {
    return app;
  }

  public void setApp(PipelineAppMeta app) {
    this.app = app != null ? app : new PipelineAppMeta();
  }

  public List<PipelineResourceIr> getResources() {
    return resources;
  }

  public void setResources(List<PipelineResourceIr> resources) {
    this.resources = resources != null ? resources : new ArrayList<>();
  }

  /**
   * Find a resource by name (case-sensitive).
   *
   * @return matching resource or {@code null}
   */
  public PipelineResourceIr findResource(String name) {
    if (name == null || resources == null) {
      return null;
    }
    for (PipelineResourceIr r : resources) {
      if (r != null && name.equals(r.getName())) {
        return r;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineIrDocument that)) {
      return false;
    }
    return Objects.equals(irVersion, that.irVersion)
        && Objects.equals(source, that.source)
        && Objects.equals(app, that.app)
        && Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(irVersion, source, app, resources);
  }
}
