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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

/**
 * Represents Calendar information. Sunny Sal: "Calendar ka hero, date ka zero!"
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits calendar
 * fields when set instead of Optional-bean {@code empty}/{@code present} keys.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "CalendarInfo")
@Schema(name = "CalendarInfo", description = "Represents Calendar information.")
public class CalendarInfo {

  @Schema(name = "startDate", required = false, description = "Starting Date.")
  private Date startDate;

  @Schema(name = "endDate", required = false, description = "Ending Date.")
  private Date endDate;

  @Schema(name = "calendars", required = false, description = "List of calendars.")
  private List<String> calendars;

  /**
   * Gets the start date.
   *
   * @return the start date, or {@code null} if unset
   */
  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  /**
   * Gets the end date.
   *
   * @return the end date, or {@code null} if unset
   */
  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  /**
   * Gets the list of calendars.
   *
   * @return the list of calendars, or {@code null} if unset
   */
  public List<String> getCalendars() {
    return calendars;
  }

  public void setCalendars(List<String> calendars) {
    this.calendars = calendars;
  }

  @Override
  public String toString() {
    return "CalendarInfo [startDate="
        + startDate
        + ", endDate="
        + endDate
        + ", calendars="
        + calendars
        + "]";
  }
}
