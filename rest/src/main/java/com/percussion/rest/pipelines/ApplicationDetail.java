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
 * Read-only design detail for one classic XML Application / pipeline package.
 *
 * <p>Does not expose pipe IR, start/stop, or mapper definitions (later slices).
 */
@XmlRootElement(name = "ApplicationDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pipeline application detail with data set catalog")
public class ApplicationDetail {

  /** Numeric id of the application. */
  private Integer id;

  /** Application name. */
  private String name;

  /** Free-form description. */
  private String description;

  /** Whether the application is enabled. */
  private Boolean enabled;

  /** Whether the application is hidden from listings. */
  private Boolean hidden;

  /** Application root directory. */
  private String appRoot;

  /** Application type identifier. */
  private String appType;

  /** Application version. */
  private String version;

  /** Data sets defined by the application. */
  private List<ApplicationDataSetSummary> dataSets = new ArrayList<>();

  /** Notes about design-time fields not yet exposed by the API. */
  private List<String> designGaps = new ArrayList<>();

  /** No-op constructor. */
  public ApplicationDetail() {}

  /**
   * Returns the application id.
   *
   * @return the id
   */
  public Integer getId() {
    return id;
  }

  /**
   * Sets the application id.
   *
   * @param id the new id
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Returns the application name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the application name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the application description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the application description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns whether the application is enabled.
   *
   * @return the enabled flag
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether the application is enabled.
   *
   * @param enabled the new enabled flag
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether the application is hidden.
   *
   * @return the hidden flag
   */
  public Boolean getHidden() {
    return hidden;
  }

  /**
   * Sets whether the application is hidden.
   *
   * @param hidden the new hidden flag
   */
  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  /**
   * Returns the application root directory.
   *
   * @return the app root
   */
  public String getAppRoot() {
    return appRoot;
  }

  /**
   * Sets the application root directory.
   *
   * @param appRoot the new app root
   */
  public void setAppRoot(String appRoot) {
    this.appRoot = appRoot;
  }

  /**
   * Returns the application type.
   *
   * @return the app type
   */
  public String getAppType() {
    return appType;
  }

  /**
   * Sets the application type.
   *
   * @param appType the new app type
   */
  public void setAppType(String appType) {
    this.appType = appType;
  }

  /**
   * Returns the application version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the application version.
   *
   * @param version the new version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Returns the application's data sets.
   *
   * @return the data sets
   */
  public List<ApplicationDataSetSummary> getDataSets() {
    return dataSets;
  }

  /**
   * Sets the application's data sets.
   *
   * @param dataSets the new data sets
   */
  public void setDataSets(List<ApplicationDataSetSummary> dataSets) {
    this.dataSets = dataSets != null ? dataSets : new ArrayList<>();
  }

  /**
   * Returns the design-gap notes.
   *
   * @return the design gaps
   */
  public List<String> getDesignGaps() {
    return designGaps;
  }

  /**
   * Sets the design-gap notes.
   *
   * @param designGaps the new design gaps
   */
  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
