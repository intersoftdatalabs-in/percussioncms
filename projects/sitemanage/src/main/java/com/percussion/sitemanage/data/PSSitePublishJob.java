// REFACTORED: CP-JAVA11
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
package com.percussion.sitemanage.data;

import com.percussion.share.data.PSAbstractDataObject;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotEqual;
import net.sf.oval.constraint.NotNull;

/**
 * Represents a publishing job for a site. Contains job metadata and progress information.
 *
 * @author DavidBenua
 */
@XmlRootElement(name = "SitePublishJob")
public class PSSitePublishJob extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  /** Job Id for publishing job. Unique and never null. */
  @NotEqual(value = "0")
  private long jobId;

  /** Site name. */
  @NotBlank @NotNull private String siteName;

  /** Site id. */
  @NotBlank @NotNull private String siteId;

  /** Job status. */
  private String status;

  /** Starting time as formatted string (hh:MM A). */
  private String startTime;

  /** Starting date as formatted string (MM/DD/yyyy). */
  private String startDate;

  /** Id of the server where it is being published. */
  private long pubServerId;

  /** Name of the server where it is being published. */
  private String pubServerName;

  /** Elapsed time in milliseconds. */
  private long elapsedTime;

  /** Total items in this job. */
  private long totalItems;

  /** Completed items in this job. */
  private long completedItems;

  /** Failed items in this job. */
  private long failedItems;

  /** Removed items in this job. */
  private long removedItems;

  /** Indicates if the job is stopping. */
  private Boolean isStopping;

  public long getJobId() {
    return jobId;
  }

  public void setJobId(long jobId) {
    this.jobId = jobId;
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public String getSiteId() {
    return siteId;
  }

  public void setSiteId(String siteId) {
    this.siteId = siteId;
  }

  public Optional<String> getStatus() {
    return Optional.ofNullable(status);
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Optional<String> getStartTime() {
    return Optional.ofNullable(startTime);
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public Optional<String> getStartDate() {
    return Optional.ofNullable(startDate);
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public long getPubServerId() {
    return pubServerId;
  }

  public void setPubServerId(long pubServerId) {
    this.pubServerId = pubServerId;
  }

  public Optional<String> getPubServerName() {
    return Optional.ofNullable(pubServerName);
  }

  public void setPubServerName(String pubServerName) {
    this.pubServerName = pubServerName;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public void setElapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  public long getTotalItems() {
    return totalItems;
  }

  public void setTotalItems(long totalItems) {
    this.totalItems = totalItems;
  }

  public long getCompletedItems() {
    return completedItems;
  }

  public void setCompletedItems(long completedItems) {
    this.completedItems = completedItems;
  }

  public long getFailedItems() {
    return failedItems;
  }

  public void setFailedItems(long failedItems) {
    this.failedItems = failedItems;
  }

  public long getRemovedItems() {
    return removedItems;
  }

  public void setRemovedItems(long removedItems) {
    this.removedItems = removedItems;
  }

  public Optional<Boolean> getIsStopping() {
    return Optional.ofNullable(isStopping);
  }

  public void setIsStopping(Boolean isStopping) {
    this.isStopping = isStopping;
  }
}
