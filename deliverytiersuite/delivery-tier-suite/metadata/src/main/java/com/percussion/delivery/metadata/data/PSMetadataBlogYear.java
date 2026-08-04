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

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a year and the list of months with the number of posts for each month. Also each year
 * has the total of posts for the given year.
 *
 * @author leonardohildt
 */
public class PSMetadataBlogYear {
  private Integer year;

  private Integer yearCount;

  private List<PSMetadataBlogMonth> months;

  /**
   * Constructs a year bucket initialised with zero counts and an empty slot for every month up to
   * and including the supplied {@code year}. If {@code year} is the current year only the months up
   * to "now" are created; otherwise all twelve are.
   *
   * @param year the year to initialise, may be <code>null</code>.
   */
  public PSMetadataBlogYear(Integer year) {
    super();
    this.year = year;
    this.yearCount = 0;

    Calendar cal = Calendar.getInstance();
    Integer currentYear = cal.get(Calendar.YEAR);
    Integer currentMonth = cal.get(Calendar.MONTH);

    List<PSMetadataBlogMonth> emptyMonths = new ArrayList<>();
    String[] localeMonths = new DateFormatSymbols(Locale.getDefault()).getMonths();
    Integer indexMonth = localeMonths.length - 2;
    if (currentYear.equals(year)) {
      indexMonth = currentMonth;
    }

    for (int i = indexMonth; i >= 0; i--) {
      PSMetadataBlogMonth newMonth = new PSMetadataBlogMonth(localeMonths[i], 0);
      emptyMonths.add(newMonth);
    }
    ;
    this.months = emptyMonths;
  }

  /**
   * Internal helper that orders months alphabetically (used as a fallback comparator for archive
   * views).
   */
  class MonthOrderBlogsComparator implements Comparator<PSMetadataBlogMonth> {

    /**
     * Compares two months alphabetically by their display name.
     *
     * @param o1 the first month to compare.
     * @param o2 the second month to compare.
     * @return a negative integer, zero or a positive integer following the {@link Comparator}
     *     contract.
     */
    public int compare(PSMetadataBlogMonth o1, PSMetadataBlogMonth o2) {
      return o1.getMonth().compareTo(o2.getMonth());
    }
  }

  /**
   * Returns the year represented by this entry.
   *
   * @return the year value.
   */
  public Integer getYear() {
    return year;
  }

  /**
   * Sets the year represented by this entry.
   *
   * @param year the year to set.
   */
  public void setYear(Integer year) {
    this.year = year;
  }

  /**
   * Sets the year-overall post count for this entry.
   *
   * @param yearCount the year count to set.
   */
  public void setYearCount(Integer yearCount) {
    this.yearCount = yearCount;
  }

  /**
   * Returns the total number of posts that landed in this year.
   *
   * @return the count for the year.
   */
  public Integer getYearCount() {
    return yearCount;
  }

  /**
   * Returns the list of month buckets for this year, each pre-populated with a count of zero.
   *
   * @return the months list, never <code>null</code>, may be empty.
   */
  public List<PSMetadataBlogMonth> getMonths() {
    return months;
  }

  /**
   * Replaces the list of month buckets.
   *
   * @param months the months to set.
   */
  public void setMonths(List<PSMetadataBlogMonth> months) {
    this.months = months;
  }

  /**
   * Adds a single month bucket to the months list.
   *
   * @param month the month to add.
   */
  public void addMonth(PSMetadataBlogMonth month) {
    this.months.add(month);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSMetadataBlogYear)) {
      return false;
    }
    return Objects.equals(((PSMetadataBlogYear) obj).year, this.year);
  }

  @Override
  public int hashCode() {
    return Objects.hash(year);
  }
}
