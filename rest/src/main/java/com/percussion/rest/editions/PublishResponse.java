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

package com.percussion.rest.editions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Stores the information returned from a publish request.
 *
 * <p>Sunny Sal: "Publishing response received, boss!"
 *
 * <p>Wire getters return plain nullable types (not {@code Optional}) so Jackson/CXF JSON emits
 * {@code warningMessage} as a scalar, not an Optional bean (issue #3388 slice 10 / #3432).
 */
@XmlRootElement(name = "EditionPublishResponse")
@JsonRootName("EditionPublishResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublishResponse {

  private String siteName;
  private String status;
  private String delivered;
  private String failures;
  private String warningMessage;
  private long jobid;

  /**
   * @return the name of the site to be published.
   */
  public String getSiteName() {
    return siteName;
  }

  /**
   * @param siteName the name of the site to be published.
   */
  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  /**
   * @return the publishing status, never blank.
   */
  public String getStatus() {
    return status;
  }

  /**
   * @param status the publishing status. May not be blank.
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * @return the number of items delivered, never blank.
   */
  public String getDelivered() {
    return delivered;
  }

  /**
   * @param delivered the number of items delivered. May not be blank.
   */
  public void setDelivered(String delivered) {
    this.delivered = delivered;
  }

  /**
   * @return the number of failures, never blank.
   */
  public String getFailures() {
    return failures;
  }

  /**
   * @param failures the number of failures. May not be blank.
   */
  public void setFailures(String failures) {
    this.failures = failures;
  }

  /**
   * @return the warning message, or {@code null} if unset
   */
  public String getWarningMessage() {
    return warningMessage;
  }

  /**
   * @param warningMessage the warning message.
   */
  public void setWarningMessage(String warningMessage) {
    this.warningMessage = warningMessage;
  }

  /**
   * @return the job id for the publish operation.
   */
  public long getJobid() {
    return jobid;
  }

  /**
   * @param jobid the job id for the publish operation.
   */
  public void setJobid(long jobid) {
    this.jobid = jobid;
  }
}
