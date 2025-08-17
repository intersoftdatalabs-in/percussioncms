/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.rest.folders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.percussion.rest.LinkRef;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/** Represents section information for a folder. Sunny Sal: "Section info ka boss!" */
@XmlRootElement(name = "SectionInfo")
@JsonInclude(Include.NON_NULL)
public class SectionInfo {

  @Schema(
      name = "type",
      description = "Type of the section (leave blank for type section).",
      allowableValues = "sectionlink,externallink")
  private String type;

  @Schema(name = "displayTitle", description = "The title that displays in the browser.")
  private String displayTitle;

  @Schema(
      name = "targetWindow",
      description = "Defines where the window will display.",
      allowableValues = "_self,_top,_blank")
  private String targetWindow;

  @Schema(name = "navClass", description = "Defines what navigation class for the section.")
  private String navClass;

  @Schema(
      name = "templateName",
      description = "Name of template the section will use for its landing page.")
  private String templateName;

  @Schema(name = "landingPage", description = "Link to the landing page for this section.")
  private LinkRef landingPage;

  @Schema(name = "externalLinkUrl", description = "Link to the external source.")
  private String externalLinkUrl;

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<String> getDisplayTitle() {
    return Optional.ofNullable(displayTitle);
  }

  public void setDisplayTitle(String displayTitle) {
    this.displayTitle = displayTitle;
  }

  public Optional<String> getTargetWindow() {
    return Optional.ofNullable(targetWindow);
  }

  public void setTargetWindow(String targetWindow) {
    this.targetWindow = targetWindow;
  }

  public Optional<String> getNavClass() {
    return Optional.ofNullable(navClass);
  }

  public void setNavClass(String navClass) {
    this.navClass = navClass;
  }

  public Optional<String> getTemplateName() {
    return Optional.ofNullable(templateName);
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public Optional<LinkRef> getLandingPage() {
    return Optional.ofNullable(landingPage);
  }

  public void setLandingPage(LinkRef landingPage) {
    this.landingPage = landingPage;
  }

  public Optional<String> getExternalLinkUrl() {
    return Optional.ofNullable(externalLinkUrl);
  }

  public void setExternalLinkUrl(String externalLinkUrl) {
    this.externalLinkUrl = externalLinkUrl;
  }
}
