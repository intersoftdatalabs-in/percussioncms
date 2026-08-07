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

import java.util.Objects;

/** Application-level metadata on a pipeline IR document. */
public class PipelineAppMeta {

  private int id;
  private String name;
  private String description;
  private String requestRoot;
  private boolean enabled;
  private boolean hidden;
  private String appType;
  private String version;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

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

  public String getRequestRoot() {
    return requestRoot;
  }

  public void setRequestRoot(String requestRoot) {
    this.requestRoot = requestRoot;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public String getAppType() {
    return appType;
  }

  public void setAppType(String appType) {
    this.appType = appType;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineAppMeta that)) {
      return false;
    }
    return id == that.id
        && enabled == that.enabled
        && hidden == that.hidden
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(requestRoot, that.requestRoot)
        && Objects.equals(appType, that.appType)
        && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, requestRoot, enabled, hidden, appType, version);
  }
}
