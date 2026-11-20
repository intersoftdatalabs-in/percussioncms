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

package com.percussion.rest.explorer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Visibility identity for one Explorer row (#6783 / parent #4530).
 *
 * <p>The shell evaluator ({@code menuVisibility.ts}) mirrors {@code PSActionVisibilityChecker}
 * hide-on-match semantics, but several contexts can never hide a menu because the facts are not on
 * {@code PSPathItem} rows (#6618): checkout status plus the numeric content / object / workflow /
 * community / assignment ids. This DTO carries exactly those facts for the selected rows so the
 * evaluator can resolve them without per-row lookups.
 *
 * <p>Every fact except {@code id} is nullable: {@code null} means "not known to the server" and the
 * client stays permissive (visible), exactly as before #6517. Unknown or unauthorized ids are
 * omitted from the batch response rather than failing it.
 */
@XmlRootElement(name = "ItemIdentity")
@JsonRootName("ItemIdentity")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    description =
        "Workbench visibility identity for one Explorer row; null facts stay permissive client-side")
public class ItemIdentity {

  /** Row id in pathmanagement / itemmanagement id space (never blank). */
  private String id;

  /**
   * Checkout token the evaluator compares hide-on-match: {@code ""} when nobody holds the lock,
   * {@code "Checked Out By Me"} when the session user holds it, {@code "Checked Out"} when someone
   * else does. Mirrors {@code PSActionVisibilityChecker} checkout-status values.
   */
  private String checkoutStatus;

  /** Numeric content-type id ({@code PSActionVisibilityContext} {@code 3}). */
  private Long contentTypeId;

  /** Numeric object type ({@code PSActionVisibilityContext} {@code 4}). */
  private Integer objectType;

  /** Workflow id ({@code PSActionVisibilityContext} {@code 9}). */
  private Integer workflowId;

  /** Community id ({@code PSActionVisibilityContext} {@code 2}). */
  private Integer communityId;

  /**
   * Assignment-type id ({@code PSActionVisibilityContext} {@code 1}). {@code null} until a server
   * follow-on resolves the per-user workflow assignment; tracked as a residual on #6783.
   */
  private Integer assignmentType;

  /**
   * Publishable type ({@code PSActionVisibilityContext} {@code 10}). {@code null} until a server
   * follow-on resolves it; tracked as a residual on #6783.
   */
  private String publishableType;

  public ItemIdentity() {}

  public ItemIdentity(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCheckoutStatus() {
    return checkoutStatus;
  }

  public void setCheckoutStatus(String checkoutStatus) {
    this.checkoutStatus = checkoutStatus;
  }

  public Long getContentTypeId() {
    return contentTypeId;
  }

  public void setContentTypeId(Long contentTypeId) {
    this.contentTypeId = contentTypeId;
  }

  public Integer getObjectType() {
    return objectType;
  }

  public void setObjectType(Integer objectType) {
    this.objectType = objectType;
  }

  public Integer getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(Integer workflowId) {
    this.workflowId = workflowId;
  }

  public Integer getCommunityId() {
    return communityId;
  }

  public void setCommunityId(Integer communityId) {
    this.communityId = communityId;
  }

  public Integer getAssignmentType() {
    return assignmentType;
  }

  public void setAssignmentType(Integer assignmentType) {
    this.assignmentType = assignmentType;
  }

  public String getPublishableType() {
    return publishableType;
  }

  public void setPublishableType(String publishableType) {
    this.publishableType = publishableType;
  }
}
