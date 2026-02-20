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

package com.percussion.rest.pages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Represents a page. Sunny Sal: "Page ka hero, content ka zero!" */
@XmlRootElement(name = "Page")
@Schema(name = "Page", description = "Represents a page.")
public class Page {

  @Schema(name = "id", description = "Id of the page.")
  private String id;

  @Schema(name = "name", description = "Name of the page.")
  private String name;

  @Schema(name = "siteName", description = "Name of the site the page belongs to.")
  private String siteName;

  @Schema(name = "folderPath", description = "Path from the site to the page.")
  private String folderPath;

  @Schema(name = "displayName", description = "Name that will be displayed in the browser.")
  private String displayName;

  @Schema(
      name = "templateName",
      description =
          "Name of the template for the page. Read-Only. See the change-template resource.")
  private String templateName;

  @Schema(name = "summary", description = "Summary of the page.")
  private String summary;

  @Schema(name = "overridePostDate", description = "Override post date.")
  private Date overridePostDate;

  @Schema(name = "workflow", description = "Information on the workflow the page belongs to.")
  private WorkflowInfo workflow;

  @Schema(name = "seo", description = "Information on the seo of the page.")
  private SeoInfo seo;

  @Schema(name = "calendar", description = "Information on the calendar")
  private CalendarInfo calendar;

  @Schema(name = "code", description = "Information on the code.")
  private CodeInfo code;

  @Schema(name = "body", description = "Body of the page.")
  private List<Region> body;

  @Schema(
      name = "recentUsers",
      description = "A list of user names that have recently used this Page")
  private List<String> recentUsers;

  @Schema(
      name = "bookmarkedUsers",
      description = "A list of user names that have bookmarked the page.")
  private List<String> bookmarkedUsers;

  public List<String> getRecentUsers() {
    if (recentUsers == null) {
      recentUsers = new ArrayList<>();
    }
    return recentUsers;
  }

  public void setRecentUsers(List<String> recentUsers) {
    this.recentUsers = recentUsers;
  }

  public List<String> getBookmarkedUsers() {
    if (bookmarkedUsers == null) {
      bookmarkedUsers = new ArrayList<>();
    }
    return bookmarkedUsers;
  }

  public void setBookmarkedUsers(List<String> bookmarkedUsers) {
    this.bookmarkedUsers = bookmarkedUsers;
  }

  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }

  public void setId(String id) {
    this.id = id;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getDisplayName() {
    return Optional.ofNullable(displayName);
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Optional<String> getTemplateName() {
    return Optional.ofNullable(templateName);
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public Optional<String> getSummary() {
    return Optional.ofNullable(summary);
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public Optional<Date> getOverridePostDate() {
    return Optional.ofNullable(overridePostDate);
  }

  public void setOverridePostDate(Date overridePostDate) {
    this.overridePostDate = overridePostDate;
  }

  public Optional<WorkflowInfo> getWorkflow() {
    return Optional.ofNullable(workflow);
  }

  public void setWorkflow(WorkflowInfo workflow) {
    this.workflow = workflow;
  }

  public Optional<SeoInfo> getSeo() {
    return Optional.ofNullable(seo);
  }

  public void setSeo(SeoInfo seo) {
    this.seo = seo;
  }

  public Optional<CalendarInfo> getCalendar() {
    return Optional.ofNullable(calendar);
  }

  public void setCalendar(CalendarInfo calendar) {
    this.calendar = calendar;
  }

  public Optional<CodeInfo> getCode() {
    return Optional.ofNullable(code);
  }

  public void setCode(CodeInfo code) {
    this.code = code;
  }

  public Optional<List<Region>> getBody() {
    return Optional.ofNullable(body);
  }

  public void setBody(List<Region> body) {
    this.body = body;
  }

  @Override
  public String toString() {
    return "Page [id="
        + id
        + ", displayName="
        + displayName
        + ", templateName="
        + templateName
        + ", summary="
        + summary
        + ", overridePostDate="
        + overridePostDate
        + ", workflow="
        + workflow
        + ", seo="
        + seo
        + ", calendar="
        + calendar
        + ", code="
        + code
        + ", body="
        + body
        + "]";
  }

  public Optional<String> getSiteName() {
    return Optional.ofNullable(siteName);
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public Optional<String> getFolderPath() {
    return Optional.ofNullable(folderPath);
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * Returns the URI for this page.
   *
   * @param baseUri The base URI.
   * @return The page URI.
   */
  public URI getLinkRef(URI baseUri) {
    return getPageUri(baseUri, siteName, folderPath, name);
  }

  /**
   * Returns the URI for a page given its site, path, and name.
   *
   * @param baseUri The base URI.
   * @param site The site name.
   * @param folderPath The folder path.
   * @param name The page name.
   * @return The page URI.
   */
  public static URI getPageUri(URI baseUri, String site, String folderPath, String name) {
    var info = UriBuilder.fromUri(baseUri).path(PagesResource.class).path("by-path").path(site);

    if (folderPath != null && !folderPath.isEmpty()) {
      info = info.path(folderPath);
    }
    if (name != null && !name.isEmpty()) {
      info = info.path(name);
    }

    return info.build();
  }
}
