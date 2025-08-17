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
package com.percussion.pagemanagement.assembler;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Represents a year in a blog, containing months and entry counts. */
public class PSBlogYear {

  private Integer year;
  private Integer yearCount;
  private List<PSBlogMonth> months;

  /**
   * Constructs a blog year with the specified year. Initializes months with zero counts up to the
   * current month if the year is current.
   *
   * @param year the year
   */
  public PSBlogYear(Integer year) {
    this.year = year;
    this.yearCount = 0;

    var cal = Calendar.getInstance();
    var currentYear = cal.get(Calendar.YEAR);
    var currentMonth = cal.get(Calendar.MONTH);

    var emptyMonths = new ArrayList<PSBlogMonth>();
    var localeMonths = new DateFormatSymbols(Locale.getDefault()).getMonths();
    var indexMonth = localeMonths.length - 2;
    if (currentYear.equals(year)) {
      indexMonth = currentMonth;
    }

    for (int i = indexMonth; i >= 0; i--) {
      var newMonth = new PSBlogMonth(localeMonths[i], 0);
      emptyMonths.add(newMonth);
    }
    this.months = emptyMonths;
  }

  /**
   * Gets the year.
   *
   * @return the year
   */
  public Integer getYear() {
    return year;
  }

  /**
   * Sets the year.
   *
   * @param year the year to set
   */
  public void setYear(Integer year) {
    this.year = year;
  }

  /**
   * Sets the year count.
   *
   * @param yearCount the year count to set
   */
  public void setYearCount(Integer yearCount) {
    this.yearCount = yearCount;
  }

  /**
   * Gets the count for the year.
   *
   * @return the count for the year
   */
  public Integer getYearCount() {
    return yearCount;
  }

  /**
   * Gets the months for this year.
   *
   * @return the list of months
   */
  public List<PSBlogMonth> getMonths() {
    return months;
  }

  /**
   * Sets the months for this year.
   *
   * @param months the months to set
   */
  public void setMonths(List<PSBlogMonth> months) {
    this.months = months;
  }

  /**
   * Adds a month to this year.
   *
   * @param month the month to add
   */
  public void addMonth(PSBlogMonth month) {
    this.months.add(month);
  }
}
