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
package com.percussion.pagemanagement.data;

import com.percussion.share.data.PSDataItemSummarySingleFolderPath;
import java.util.List;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

import java.util.ArrayList;
/**
 * The base class for all page-related classes. Contains summary information for a page.
 *
 * @author YuBingChen
 * @author adamgent
 */
public class PSPageSummary extends PSDataItemSummarySingleFolderPath {

  private static final long serialVersionUID = 3197862964060713693L;

  @NotNull @NotBlank private String title;

  @NotNull @NotBlank private String templateId;

  @NotNull @NotBlank private String linkTitle;

  private String noindex;
  private String author;
  private ArrayList<String> tags;
  private String templateContentMigrationVersion = "0";
  private boolean migrationEmptyWidgetFlag = false;
  private String description;

  /**
   * Gets the meta tag noindex of the Page. If set to "true" noindex meta tag will be added to page.
   *
   * @return the noindex
   */
  public String getNoindex() {
    return noindex;
  }

  /**
   * Sets the meta tag noindex of the Page. If set to "true" noindex meta tag will be added to page.
   *
   * @param noindex the noindex value
   */
  public void setNoindex(String noindex) {
    this.noindex = noindex;
  }

  /**
   * Gets the meta tag description of the Page.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the meta tag description of the Page.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the ID of the template used to render the Page.
   *
   * @return the template ID, never {@code null} or empty.
   */
  public String getTemplateId() {
    return templateId;
  }

  /**
   * Sets the ID of the template used to render the Page.
   *
   * @param templateId the template ID, never {@code null} or empty.
   */
  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  /**
   * Gets the title of the Page.
   *
   * @return the title, never {@code null} or empty.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the link title, never {@code null} or empty.
   *
   * @return the link title
   */
  public String getLinkTitle() {
    return linkTitle;
  }

  /**
   * Sets the title of the Page.
   *
   * @param title the title of the Page, never {@code null} or empty.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Sets the link title for the page.
   *
   * @param linkTitle the link title for the page, never {@code null} or empty.
   */
  public void setLinkTitle(String linkTitle) {
    this.linkTitle = linkTitle;
  }

  /**
   * Gets the author.
   *
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Sets the author.
   *
   * @param author the author to set
   */
  public void setAuthor(String author) {
    this.author = author;
  }

  /**
   * Gets the tags for the page.
   *
   * @return the tags
   */
  public List<String> getTags() {
    return tags;
  }

  /**
   * Sets the page tags.
   *
   * @param tags the page tags to set
   */
  @SuppressWarnings("unchecked")
  public void setTags(List<String> tags) {
    if (tags == null) {
      this.tags = null;
    } else if (tags instanceof ArrayList) {
      this.tags = (ArrayList<String>) tags;
    } else {
      this.tags = new ArrayList<>(tags);
    }
  }

  /**
   * Gets the content migration version this page was last saved with.
   *
   * @return the version, "0" if the page has never had content migration applied to it.
   */
  public String getTemplateContentMigrationVersion() {
    return templateContentMigrationVersion;
  }

  /**
   * Sets the content migration version, see {@link #getTemplateContentMigrationVersion()}.
   *
   * @param version the version to set, not {@code null}, non-numeric values are ignored.
   */
  public void setTemplateContentMigrationVersion(String version) {
    this.templateContentMigrationVersion = version;
  }

  /**
   * Flag to indicate whether content migration failed to migrate content into all the widgets or
   * not.
   *
   * @return true if the migration leaves an empty widget, otherwise false.
   */
  public boolean isMigrationEmptyWidgetFlag() {
    return migrationEmptyWidgetFlag;
  }

  /**
   * Sets the flag to indicate whether content migration failed to migrate content into all the
   * widgets or not.
   *
   * @param migrationEmptyWidgetFlag true if migration leaves an empty widget
   */
  public void setMigrationEmptyWidgetFlag(boolean migrationEmptyWidgetFlag) {
    this.migrationEmptyWidgetFlag = migrationEmptyWidgetFlag;
  }
}
