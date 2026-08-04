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

/**
 * Represents a year and the list of months with the number of posts for each month.
 *
 * @author leonardohildt
 */
public class PSMetadataBlogMonth {
  private String month;

  private Integer count;

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataBlogMonth() {}

  /**
   * Constructs a fully-populated month entry.
   *
   * @param month the localized month name to display.
   * @param count the number of blog posts that landed in this month.
   */
  public PSMetadataBlogMonth(String month, Integer count) {
    super();
    this.month = month;
    this.count = count;
  }

  /**
   * Returns the localized month name.
   *
   * @return the month, may be {@code null}.
   */
  public String getMonth() {
    return month;
  }

  /**
   * Sets the localized month name.
   *
   * @param month the month to set; may be {@code null}.
   */
  public void setMonth(String month) {
    this.month = month;
  }

  /**
   * Returns the number of blog posts that landed in this month.
   *
   * @return the count, may be {@code null}.
   */
  public Integer getCount() {
    return count;
  }

  /**
   * Sets the number of blog posts that landed in this month.
   *
   * @param count the number of counts to set.
   */
  public void setCount(Integer count) {
    this.count = count;
  }
}
