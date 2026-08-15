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
import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of {@code POST /sites/{nameOrId}/virtual/publish}: build-then-copy to the Site filesystem
 * publish root ({@code IPSSite.root}).
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson emits scalars, not
 * Optional-bean {@code empty}/{@code present} keys (#3411 / #3388).
 */
@XmlRootElement(name = "VirtualSitePublishResult")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "VirtualSitePublishResult",
    description =
        "Virtual Site publish summary: Site filesystem target, files copied, and build extras")
public class VirtualSitePublishResult {

  @Schema(description = "CMS Site name that was published", example = "Help")
  private String siteName;

  @Schema(description = "Participant registry site key used for the build", example = "product-docs")
  private String siteKey;

  @Schema(
      description = "Absolute filesystem path of the Site publishing location (IPSSite.root)",
      example = "C:/inetpub/wwwroot/help")
  private String publishPath;

  @Schema(
      description = "Absolute filesystem path of the staging build output (tmp/virtual-sites)",
      example = "C:/Rhythmyx/tmp/virtual-sites/product-docs")
  private String buildOutputPath;

  @Schema(description = "Number of Markdown pages assembled to HTML", example = "42")
  private Integer pagesWritten;

  @Schema(description = "Number of regular files copied to the Site publish root", example = "50")
  private Integer filesCopied;

  @Schema(description = "Number of unresolved internal id: / relative Markdown links", example = "0")
  private Integer linkProblemCount;

  @Schema(description = "True when one or more link problems were reported")
  private Boolean hasLinkProblems;

  @Schema(description = "Human-readable link problem lines from the build")
  private List<String> linkProblems = new ArrayList<>();

  public VirtualSitePublishResult() {
    // Default constructor for JAX-RS / Jackson
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public String getSiteKey() {
    return siteKey;
  }

  public void setSiteKey(String siteKey) {
    this.siteKey = siteKey;
  }

  public String getPublishPath() {
    return publishPath;
  }

  public void setPublishPath(String publishPath) {
    this.publishPath = publishPath;
  }

  public String getBuildOutputPath() {
    return buildOutputPath;
  }

  public void setBuildOutputPath(String buildOutputPath) {
    this.buildOutputPath = buildOutputPath;
  }

  public Integer getPagesWritten() {
    return pagesWritten;
  }

  public void setPagesWritten(Integer pagesWritten) {
    this.pagesWritten = pagesWritten;
  }

  public Integer getFilesCopied() {
    return filesCopied;
  }

  public void setFilesCopied(Integer filesCopied) {
    this.filesCopied = filesCopied;
  }

  public Integer getLinkProblemCount() {
    return linkProblemCount;
  }

  public void setLinkProblemCount(Integer linkProblemCount) {
    this.linkProblemCount = linkProblemCount;
  }

  public Boolean getHasLinkProblems() {
    return hasLinkProblems;
  }

  public void setHasLinkProblems(Boolean hasLinkProblems) {
    this.hasLinkProblems = hasLinkProblems;
  }

  public List<String> getLinkProblems() {
    return linkProblems;
  }

  public void setLinkProblems(List<String> linkProblems) {
    this.linkProblems = linkProblems != null ? new ArrayList<>(linkProblems) : new ArrayList<>();
  }
}
