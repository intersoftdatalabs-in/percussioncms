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

package com.percussion.auditlog;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Audit event emitted whenever content stored in the CMS changes — covering CRUD, recycling, and
 * page publish/removal scheduling. Carries the content id and GUID so downstream consumers can
 * locate the affected item.
 */
public class PSContentEvent extends AbstractEvent {

  /** Tag identifying the integer content id of the affected item. */
  public static final String CONTENTID_TAG = "//percussion/contentid";

  /** Tag identifying the GUID of the affected item. */
  public static final String GUID_TAG = "//percussion/guid";

  /** Resource URI for the content observer. */
  public static final String CONTENT_OBSERVER = "service/bss/cms/content";

  /** Enumerates the content lifecycle actions recorded by {@link PSContentEvent}. */
  public enum ContentEventActions {
    /** A new content item was created. */
    create,
    /** An existing content item was updated. */
    update,
    /** An existing content item was moved to the recycle bin. */
    recycle,
    /** An existing content item was removed permanently. */
    delete,
    /** A page publish operation was scheduled. */
    pagePublishSchedule,
    /** A page removal operation was scheduled. */
    pageRemovalSchedule
  }

  private String contentId;
  private String guid;

  /**
   * Constructs a content event pre-populated from the supplied servlet request and metadata.
   *
   * @param guid the GUID of the affected content item, never {@code null}.
   * @param contentId the integer content id of the affected content item as a string, may be {@code
   *     null} when not applicable.
   * @param path the repository path of the affected content item, never {@code null}.
   * @param action the content lifecycle action being recorded, never {@code null}.
   * @param request the HTTP request that triggered the event, never {@code null}.
   * @param outcome the outcome of the action, never {@code null}.
   */
  public PSContentEvent(
      String guid,
      String contentId,
      String path,
      ContentEventActions action,
      HttpServletRequest request,
      PSActionOutcome outcome) {
    this.guid = guid;
    this.contentId = contentId;
    this.setPath(path);
    this.action = action;
    this.setTargetUsername(request.getRemoteUser());
    this.setAgentName(request.getHeader("User-Agent"));
    this.setInitiatorIP(request.getRemoteAddr());
    this.setOutcome(outcome.name());
  }

  private ContentEventActions action;

  /**
   * Returns the integer content id of the affected item as a string.
   *
   * @return the content id, may be {@code null} when not set.
   */
  public String getContentId() {
    return contentId;
  }

  /**
   * Sets the integer content id of the affected item.
   *
   * @param contentId the content id as a string, may be {@code null}.
   */
  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  /**
   * Returns the GUID of the affected content item.
   *
   * @return the GUID, may be {@code null} when not set.
   */
  public String getGuid() {
    return guid;
  }

  /**
   * Sets the GUID of the affected content item.
   *
   * @param guid the GUID, may be {@code null}.
   */
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
   * Returns the action recorded for this event.
   *
   * @return the action, may be {@code null} when not yet set.
   */
  public ContentEventActions getAction() {
    return action;
  }

  /**
   * Sets the action recorded for this event.
   *
   * @param action the action to record, never {@code null}.
   */
  public void setAction(ContentEventActions action) {
    this.action = action;
  }

  /** Constructs an empty event with the content observer pre-assigned. */
  public PSContentEvent() {
    super();

    this.setObserverName(CONTENT_OBSERVER);
  }
}
