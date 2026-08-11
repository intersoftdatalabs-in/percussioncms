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
package com.percussion.rest.sites;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Wire DTO for Virtual Site property bag keys stored on a CMS Site.
 *
 * <p>Property names (system layer):
 *
 * <ul>
 *   <li>{@code virtual.sourceKind}
 *   <li>{@code virtual.rootPath}
 *   <li>{@code virtual.configFile}
 *   <li>{@code virtual.siteKey}
 * </ul>
 *
 * <p>Blank / missing {@code sourceKind} (or value {@code repository}) means a traditional repository
 * Site. Phase 1 virtual adapter: {@code git-filesystem}.
 */
@XmlRootElement(name = "VirtualSiteProperties")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "VirtualSiteProperties",
    description =
        "Virtual Site source configuration (virtual.sourceKind, virtual.rootPath,"
            + " virtual.configFile, virtual.siteKey)")
public class VirtualSiteProperties {

  @Schema(
      description =
          "Adapter wire name. Phase 1: git-filesystem. Blank or repository = traditional Site.",
      example = "git-filesystem")
  private String sourceKind;

  @Schema(
      description =
          "Filesystem path to the Virtual Site root (absolute or install-relative). Required when"
              + " sourceKind is virtual.",
      example = "C:/workspaces/product-docs")
  private String rootPath;

  @Schema(
      description = "Config file name under rootPath. Optional; default _config.yaml.",
      example = "_config.yaml")
  private String configFile;

  @Schema(
      description =
          "Participant registry key. Optional; defaults to the site name when unset.",
      example = "product-docs")
  private String siteKey;

  @Schema(
      description =
          "True when sourceKind is a non-blank virtual adapter (not repository). Read-only on"
              + " responses; ignored on write.",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Boolean virtual;

  public VirtualSiteProperties() {
    // Default constructor for JAX-RS / Jackson
  }

  public Optional<String> getSourceKind() {
    return Optional.ofNullable(sourceKind);
  }

  public void setSourceKind(String sourceKind) {
    this.sourceKind = sourceKind;
  }

  public Optional<String> getRootPath() {
    return Optional.ofNullable(rootPath);
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public Optional<String> getConfigFile() {
    return Optional.ofNullable(configFile);
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  public Optional<String> getSiteKey() {
    return Optional.ofNullable(siteKey);
  }

  public void setSiteKey(String siteKey) {
    this.siteKey = siteKey;
  }

  public Boolean getVirtual() {
    return virtual;
  }

  public void setVirtual(Boolean virtual) {
    this.virtual = virtual;
  }
}
