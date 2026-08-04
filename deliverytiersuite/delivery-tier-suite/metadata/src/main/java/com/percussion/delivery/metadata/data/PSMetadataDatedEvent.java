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
package com.percussion.delivery.metadata.data;

import org.apache.commons.lang3.StringUtils;

/**
 * This class contains the structure of the event information. The object is composed of the
 * properties:
 *
 * <ul>
 *   <li>page title
 *   <li>page summary
 *   <li>page start date
 *   <li>page end date
 *   <li>page url
 * </ul>
 *
 * @author rafaelsalis
 */
public class PSMetadataDatedEvent {

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataDatedEvent() {}

  private String title;

  private String summary;

  private String start;

  private String end;

  private String url;

  private boolean allDay = false;

  private String textColor = StringUtils.EMPTY;

  private String textBackground = StringUtils.EMPTY;

  /**
   * Returns the page title.
   *
   * @return the title of the page, never <code>null</code> or empty.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the page title.
   *
   * @param title sets the page title, never <code>null</code> or empty.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the page summary.
   *
   * @return the page summary, it may be <code>null</code> or empty if the page summary is unknown.
   */
  public String getSummary() {
    return summary;
  }

  /**
   * Sets the page summary.
   *
   * @param summary the page summary to set, may be <code>null</code> or empty.
   */
  public void setSummary(String summary) {
    this.summary = summary;
  }

  /**
   * Returns the page start date as a formatted string.
   *
   * @return the page start date, it may be <code>null</code> or empty if the page start date is
   *     unknown.
   */
  public String getStart() {
    return start;
  }

  /**
   * Sets the page start date as a formatted string.
   *
   * @param start sets the page start date, may be <code>null</code> or empty.
   */
  public void setStart(String start) {
    this.start = start;
  }

  /**
   * Returns the page end date as a formatted string.
   *
   * @return the page end date, it may be <code>null</code> or empty if the page end date is
   *     unknown.
   */
  public String getEnd() {
    return end;
  }

  /**
   * Sets the page end date as a formatted string.
   *
   * @param end sets the page end date, may be <code>null</code> or empty.
   */
  public void setEnd(String end) {
    this.end = end;
  }

  /**
   * Returns the page url.
   *
   * @return the page url, never <code>null</code> or empty.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the page url.
   *
   * @param url sets the page url, never <code>null</code> or empty.
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Returns whether this event spans the entire day.
   *
   * @return the all day value.
   */
  public boolean isAllDay() {
    return allDay;
  }

  /**
   * Sets whether this event spans the entire day.
   *
   * @param allDay sets the all day.
   */
  public void setAllDay(boolean allDay) {
    this.allDay = allDay;
  }

  /**
   * Returns the text color used when rendering the event.
   *
   * @return the text color, may be empty but never <code>null</code>.
   */
  public String getTextColor() {
    return textColor;
  }

  /**
   * Sets the text color used when rendering the event.
   *
   * @param textColor sets the text color, may be empty but never <code>null</code>.
   */
  public void setTextColor(String textColor) {
    this.textColor = textColor;
  }

  /**
   * Returns the text background color used when rendering the event.
   *
   * @return the text background, may be empty but never <code>null</code>.
   */
  public String getTextBackground() {
    return textBackground;
  }

  /**
   * Sets the text background color used when rendering the event.
   *
   * @param textBackground sets the text background, may be empty but never <code>null</code>.
   */
  public void setTextBackground(String textBackground) {
    this.textBackground = textBackground;
  }
}
