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

package com.ibm.cadf.middleware;

/**
 * Mutable carrier for the resource and actor metadata captured during an audited action. The
 * middleware pulls each field from this object to populate the CADF {@code initiator}/{@code
 * target}/{@code observer} resources when assembling an {@link com.ibm.cadf.model.Event}. All
 * fields are optional and may be {@code null}; the middleware falls back to safe defaults when a
 * value is missing.
 *
 * <p>Setters are {@code final} so subclasses (for example Percussion audit events) may call them
 * from constructors without {@code this-escape} under {@code -Xlint:all}.
 */
public class AuditContext {
  private String targetName;

  private String targetUrl;

  private String targetUsername;

  private String targetEndpointName;

  private String observerName;

  private String initiatorIP;

  private String iniatorName;

  private String agentName;
  private String activity;
  private String guidID;
  private String path;

  /**
   * Returns the repository path of the resource affected by the audited action.
   *
   * @return the path, may be {@code null}.
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the repository path of the resource affected by the audited action.
   *
   * @param path the path, may be {@code null}.
   */
  public final void setPath(String path) {
    this.path = path;
  }

  /**
   * Returns the unique id assigned to the audited action by the originating system.
   *
   * @return the GUID, may be {@code null}.
   */
  public String getGuidID() {
    return guidID;
  }

  /**
   * Sets the unique id assigned to the audited action by the originating system.
   *
   * @param guidID the GUID, may be {@code null}.
   */
  public final void setGuidID(String guidID) {
    this.guidID = guidID;
  }

  /**
   * Returns the activity name attached to the audited action (e.g., {@code "account-revoke"}).
   *
   * @return the activity, may be {@code null}.
   */
  public String getActivity() {
    return activity;
  }

  /**
   * Sets the activity name attached to the audited action.
   *
   * @param activity the activity, may be {@code null}.
   */
  public final void setActivity(String activity) {
    this.activity = activity;
  }

  /**
   * Returns the user-agent string reported by the client that originated the request.
   *
   * @return the agent name, may be {@code null}.
   */
  public String getAgentName() {
    return agentName;
  }

  /**
   * Sets the user-agent string reported by the client that originated the request.
   *
   * @param agentName the agent name, may be {@code null}.
   */
  public final void setAgentName(String agentName) {
    this.agentName = agentName;
  }

  /** Default no-argument constructor for {@link AuditContext}. */
  public AuditContext() {}

  /**
   * Constructs an {@link AuditContext} with observer and target names assigned by direct field
   * writes. Subclass constructors may call this form of {@code super(...)} without invoking
   * overridable methods on a partially constructed instance (avoids {@code this-escape}).
   *
   * @param observerName the observer name, may be {@code null}.
   * @param targetName the target name, may be {@code null}.
   */
  protected AuditContext(String observerName, String targetName) {
    this.observerName = observerName;
    this.targetName = targetName;
  }

  /**
   * Returns the human-readable name of the target resource (e.g., a content item title).
   *
   * @return the target name, may be {@code null}.
   */
  public String getTargetName() {
    return targetName;
  }

  /**
   * Sets the human-readable name of the target resource.
   *
   * @param targetName the target name, may be {@code null}.
   */
  public final void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  /**
   * Returns the URL of the target resource when the audited action is HTTP-based.
   *
   * @return the target URL, may be {@code null}.
   */
  public String getTargetUrl() {
    return targetUrl;
  }

  /**
   * Sets the URL of the target resource when the audited action is HTTP-based.
   *
   * @param targetUrl the target URL, may be {@code null}.
   */
  public final void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
  }

  /**
   * Returns the user name of the account the action targets (e.g., a user being created).
   *
   * @return the target username, may be {@code null}.
   */
  public String getTargetUsername() {
    return targetUsername;
  }

  /**
   * Sets the user name of the account the action targets.
   *
   * @param targetUsername the target username, may be {@code null}.
   */
  public final void setTargetUsername(String targetUsername) {
    this.targetUsername = targetUsername;
  }

  /**
   * Returns the human-readable name of the resource that observed the audit (typically the CMS).
   *
   * @return the observer name, may be {@code null}.
   */
  public String getObserverName() {
    return observerName;
  }

  /**
   * Sets the human-readable name of the resource that observed the audit.
   *
   * @param observerName the observer name, may be {@code null}.
   */
  public final void setObserverName(String observerName) {
    this.observerName = observerName;
  }

  /**
   * Returns the IP address of the user that initiated the audited action.
   *
   * @return the initiator IP, may be {@code null}.
   */
  public String getInitiatorIP() {
    return initiatorIP;
  }

  /**
   * Sets the IP address of the user that initiated the audited action.
   *
   * @param initiatorIP the initiator IP, may be {@code null}.
   */
  public final void setInitiatorIP(String initiatorIP) {
    this.initiatorIP = initiatorIP;
  }

  /**
   * Returns the user name that initiated the audited action.
   *
   * @return the initiator user name, may be {@code null}.
   */
  public String getIniatorName() {
    return iniatorName;
  }

  /**
   * Sets the user name that initiated the audited action.
   *
   * @param iniatorName the initiator user name, may be {@code null}.
   */
  public final void setIniatorName(String iniatorName) {
    this.iniatorName = iniatorName;
  }

  /**
   * Returns the human-readable name attached to the target {@link com.ibm.cadf.model.EndPoint}.
   *
   * @return the target endpoint name, may be {@code null}.
   */
  public String getTargetEndpointName() {
    return targetEndpointName;
  }

  /**
   * Sets the human-readable name attached to the target {@link com.ibm.cadf.model.EndPoint}.
   *
   * @param targetEndpointName the endpoint name, may be {@code null}.
   */
  public final void setTargetEndpointName(String targetEndpointName) {
    this.targetEndpointName = targetEndpointName;
  }
}
