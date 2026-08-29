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

package com.percussion.rest.locales;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * One auto-translation setting row (locale × content type) for the CD-18 singleton set.
 *
 * <p>PUT accepts name or id for content type, workflow, and community. Unknown locale or content
 * type is HTTP 400.
 */
@XmlRootElement(name = "AutoTranslationRow")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Auto-translation setting (locale × content type)")
public class AutoTranslationRow {

  private String locale;
  private Long contentTypeId;
  private String contentTypeName;
  private Long workflowId;
  private String workflowName;
  private Long communityId;
  private String communityName;

  public AutoTranslationRow() {}

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public Long getContentTypeId() {
    return contentTypeId;
  }

  public void setContentTypeId(Long contentTypeId) {
    this.contentTypeId = contentTypeId;
  }

  public String getContentTypeName() {
    return contentTypeName;
  }

  public void setContentTypeName(String contentTypeName) {
    this.contentTypeName = contentTypeName;
  }

  public Long getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(Long workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public Long getCommunityId() {
    return communityId;
  }

  public void setCommunityId(Long communityId) {
    this.communityId = communityId;
  }

  public String getCommunityName() {
    return communityName;
  }

  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }
}
