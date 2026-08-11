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
import java.util.Optional;

/**
 * Outcome of a CMS-integrated Virtual Site static build ({@code POST
 * /sites/{nameOrId}/virtual/build}).
 *
 * <p>Maps from system {@code PSVirtualSiteBuildResult}: pages assembled, link-problem summary, and
 * absolute output path. Build may complete successfully while still reporting link problems (HTTP
 * 200 with {@code hasLinkProblems=true}).
 */
@XmlRootElement(name = "VirtualSiteBuildResult")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "VirtualSiteBuildResult",
    description =
        "Virtual Site build summary: output path, pages written, and link-problem details")
public class VirtualSiteBuildResult {

  @Schema(description = "CMS Site name that was built", example = "Help")
  private String siteName;

  @Schema(description = "Participant registry site key used for the build", example = "product-docs")
  private String siteKey;

  @Schema(
      description = "Absolute filesystem path of the static HTML output root (portable Path form)",
      example = "C:/Rhythmyx/tmp/virtual-sites/product-docs")
  private String outputPath;

  @Schema(description = "Number of Markdown pages assembled to HTML", example = "42")
  private Integer pagesWritten;

  @Schema(description = "Number of unresolved internal id: / relative Markdown links", example = "0")
  private Integer linkProblemCount;

  @Schema(description = "True when one or more link problems were reported")
  private Boolean hasLinkProblems;

  @Schema(
      description =
          "Human-readable link problem lines (same content written to outputRoot/link-report.txt)."
              + " May be empty when the build is clean.")
  private List<String> linkProblems = new ArrayList<>();

  @Schema(
      description =
          "Relative paths of written HTML/assets/report files under the output root (for debugging)."
              + " May be truncated by the server when very large.")
  private List<String> writtenFiles = new ArrayList<>();

  public VirtualSiteBuildResult() {
    // Default constructor for JAX-RS / Jackson
  }

  public Optional<String> getSiteName() {
    return Optional.ofNullable(siteName);
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public Optional<String> getSiteKey() {
    return Optional.ofNullable(siteKey);
  }

  public void setSiteKey(String siteKey) {
    this.siteKey = siteKey;
  }

  public Optional<String> getOutputPath() {
    return Optional.ofNullable(outputPath);
  }

  public void setOutputPath(String outputPath) {
    this.outputPath = outputPath;
  }

  public Integer getPagesWritten() {
    return pagesWritten;
  }

  public void setPagesWritten(Integer pagesWritten) {
    this.pagesWritten = pagesWritten;
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

  public List<String> getWrittenFiles() {
    return writtenFiles;
  }

  public void setWrittenFiles(List<String> writtenFiles) {
    this.writtenFiles = writtenFiles != null ? new ArrayList<>(writtenFiles) : new ArrayList<>();
  }
}
