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
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only design detail for one classic XML Application / pipeline package.
 *
 * <p>Does not expose pipe IR, start/stop, or mapper definitions (later slices).
 */
@XmlRootElement(name = "ApplicationDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pipeline application detail with data set catalog")
public class ApplicationDetail {

  private Integer id;
  private String name;
  private String description;
  private Boolean enabled;
  private Boolean hidden;
  private String appRoot;
  private String appType;
  private String version;
  private List<ApplicationDataSetSummary> dataSets = new ArrayList<>();
  private List<String> designGaps = new ArrayList<>();

  public ApplicationDetail() {}

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public String getAppRoot() {
    return appRoot;
  }

  public void setAppRoot(String appRoot) {
    this.appRoot = appRoot;
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

  public List<ApplicationDataSetSummary> getDataSets() {
    return dataSets;
  }

  public void setDataSets(List<ApplicationDataSetSummary> dataSets) {
    this.dataSets = dataSets != null ? dataSets : new ArrayList<>();
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
