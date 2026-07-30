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

package com.ibm.cadf.model;

import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.util.TimeStampUtils;

/**
 * Single step in a CADF event reporter chain. Each step records the role of the reporter (e.g.,
 * {@code observer}, {@code modifier}, {@code relay}), the reporter resource or id, and the
 * timestamp at which this step was recorded. {@link #isValid()} verifies the role and timestamp are
 * well-formed.
 */
public class Reporterstep extends CADFType {

  private static final long serialVersionUID = 1L;

  /** The reporter role, may be {@code null}. */
  private String role;

  /** The reporter resource, may be {@code null}. */
  private Resource reporter;

  /** The alternate reporter id, may be {@code null}. */
  private String reporterId;

  /** The step timestamp, may be {@code null}. */
  private String reporterTime;

  /**
   * Constructs a reporter step with the supplied role, resource, alternate id, and timestamp.
   *
   * @param role the reporter role tag, never {@code null}.
   * @param reporter the reporter resource, may be {@code null} when {@code reporterId} is supplied.
   * @param reporterId the alternate reporter id, may be {@code null}.
   * @param reporterTime the step timestamp (ISO-8601), may be {@code null}.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Reporterstep(String role, Resource reporter, String reporterId, String reporterTime)
      throws CADFException {
    super();
    this.role = role;
    this.reporter = reporter;
    this.reporterId = reporterId;
    this.reporterTime = reporterTime;
  }

  /**
   * Returns the reporter role.
   *
   * @return the role, may be {@code null}.
   */
  public String getRole() {
    return role;
  }

  /**
   * Sets the reporter role.
   *
   * @param role the role, may be {@code null}.
   */
  public void setRole(String role) {
    this.role = role;
  }

  /**
   * Returns the reporter resource.
   *
   * @return the reporter, may be {@code null}.
   */
  public Resource getReporter() {
    return reporter;
  }

  /**
   * Sets the reporter resource.
   *
   * @param reporter the reporter, may be {@code null}.
   */
  public void setReporter(Resource reporter) {
    this.reporter = reporter;
  }

  /**
   * Returns the alternate reporter id.
   *
   * @return the id, may be {@code null}.
   */
  public String getReporterId() {
    return reporterId;
  }

  /**
   * Sets the alternate reporter id.
   *
   * @param reporterId the id, may be {@code null}.
   */
  public void setReporterId(String reporterId) {
    this.reporterId = reporterId;
  }

  /**
   * Returns the step timestamp.
   *
   * @return the timestamp, may be {@code null}.
   */
  public String getReporterTime() {
    return reporterTime;
  }

  /**
   * Sets the step timestamp.
   *
   * @param reporterTime the timestamp, may be {@code null}.
   */
  public void setReporterTime(String reporterTime) {
    this.reporterTime = reporterTime;
  }

  /**
   * Validates that the role is a known CADF role and the timestamp is well-formed.
   *
   * @return {@code true} when both checks pass.
   */
  @Override
  public boolean isValid() {
    return isValidReporterRoles(role) && TimeStampUtils.isValid(reporterTime);
  }
}
