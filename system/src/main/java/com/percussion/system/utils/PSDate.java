/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.system.utils;

import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * The PSDate class stores and retrieves one-day information such as year, month, date of month,
 * hour, minute, and second. It is created for two reasons. First, most constructors and public
 * methods of java.util.Date have been deprecated, which are not safe to use in the long run.
 * Instead, java.util.Calendar is recommended by Sun Microsystems. Second, in java.util.Calendar,
 * some settings affects information retrieving, such as get(Calendar.MONTH), unless the users are
 * fully aware of the pitfalls.
 *
 * <p>Note: as for now, in JDK 1.2's java.util.Calendar object, method get(Calendar.MONTH) returns
 * integers from 0 to 11, rather than 1 to 12, which sometimes causes confusion and error.
 *
 * @author Jian Huang
 * @version 2.0
 * @since 1.0
 * @deprecated Consider using {@link java.time.LocalDateTime} for new code
 */
@Deprecated(since = "Java 11 refactoring", forRemoval = false)
public class PSDate {
  private static final int[] DAY_ARRAY = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private int year;
  private int month;
  private int day;
  private int hour;
  private int minute;
  private int second;

  /**
   * Construct a one-day PSDate object. For example, to store 14 o'clock, 23 minutes, and 45 seconds
   * on December 16, 1999, a PSDate object can be created as PSDate myDate = new PSDate(1999, 12,
   * 16, 14, 23, 45); Note: rule validation will be checked, such as month should be between 1 and
   * 12.
   */
  public PSDate(int year, int month, int day, int hour, int minute, int second) {
    super();
    setYearMonthDay(year, month, day);
    setTime(hour, minute, second);
  }

  /**
   * Store the given year, month, and date of month. For example, setYearMonthDay(1999, 12, 16)
   * stores December 16, 1999.
   */
  public void setYearMonthDay(int year, int month, int day) {
    validateMonth(month);
    validateDay(year, month, day);

    this.year = year;
    this.month = month;
    this.day = day;
  }

  /**
   * Get the formatted string of year, month, and date of month. The format follows yyyy-MM-dd
   * model, such as 1999-03-16.
   */
  public String getYearMonthDay() {
    var localDate = LocalDateTime.of(year, month, day, 0, 0, 0);
    return localDate.format(DATE_FORMATTER);
  }

  /**
   * Store the given hour, minute, and second. For example, set(18, 2, 31) stores 18:02:31, which
   * means 6:02:31 PM.
   */
  public void setTime(int hour, int minute, int second) {
    validateHour(hour);
    validateMinute(minute);
    validateSecond(second);

    this.hour = hour;
    this.minute = minute;
    this.second = second;
  }

  /**
   * Get the formatted string of hour, minute, and second. The format follows HH:mm:ss model, such
   * as 18:03:32.
   */
  public String getTime() {
    var localTime = LocalDateTime.of(0, 1, 1, hour, minute, second);
    return localTime.format(TIME_FORMATTER);
  }

  /** Determine whether the input year is a leap year or not. */
  public static boolean isLeapYear(int year) {
    return (year % 400 == 0) || (year % 100 != 0 && year % 4 == 0);
  }

  /** Set the year. */
  public void setYear(int year) {
    this.year = year;
  }

  /** Get the year of this date. */
  public int getYear() {
    return year;
  }

  /** Set the month. */
  public void setMonth(int month) {
    validateMonth(month);
    this.month = month;
  }

  /** Get the month of this date. */
  public int getMonth() {
    return month;
  }

  /** Set the date of the month */
  public void setDateOfMonth(int day) {
    validateDay(this.year, this.month, day);
    this.day = day;
  }

  /** Get the date of the month of this date. */
  public int getDateOfMonth() {
    return day;
  }

  /** Set the hour. */
  public void setHour(int hour) {
    validateHour(hour);
    this.hour = hour;
  }

  /** Get the hour of this date. */
  public int getHour() {
    return hour;
  }

  /** Set the minute. */
  public void setMinute(int minute) {
    validateMinute(minute);
    this.minute = minute;
  }

  /** Get the minute of this date. */
  public int getMinute() {
    return minute;
  }

  /** Set the second. */
  public void setSecond(int second) {
    validateSecond(second);
    this.second = second;
  }

  /** Get the second of this date. */
  public int getSecond() {
    return second;
  }

  /**
   * Return the string representation of this PSDate object with a format. The format follows
   * yyyy-MM-dd HH:mm:ss, such as 1999-02-16 14:08:45
   */
  @Override
  public String toString() {
    var localDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
    return localDateTime.format(DATETIME_FORMATTER);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    var psDate = (PSDate) o;
    return year == psDate.year
        && month == psDate.month
        && day == psDate.day
        && hour == psDate.hour
        && minute == psDate.minute
        && second == psDate.second;
  }

  @Override
  public int hashCode() {
    return Objects.hash(year, month, day, hour, minute, second);
  }

  /**
   * Convert this PSDate to a LocalDateTime object.
   *
   * @return LocalDateTime representation of this PSDate
   */
  public LocalDateTime toLocalDateTime() {
    return LocalDateTime.of(year, month, day, hour, minute, second);
  }

  /**
   * Create a PSDate from a LocalDateTime object.
   *
   * @param localDateTime the LocalDateTime to convert
   * @return new PSDate instance
   */
  public static PSDate fromLocalDateTime(LocalDateTime localDateTime) {
    Objects.requireNonNull(localDateTime, "localDateTime cannot be null");
    return new PSDate(
        localDateTime.getYear(),
        localDateTime.getMonthValue(),
        localDateTime.getDayOfMonth(),
        localDateTime.getHour(),
        localDateTime.getMinute(),
        localDateTime.getSecond());
  }

  private void validateMonth(int month) {
    if (month < 1 || month > 12) ruleViolation("Month must be between 1 and 12, got: " + month);
  }

  private void validateDay(int year, int month, int day) {
    if (month == 2 && isLeapYear(year)) {
      if (day < 1 || day > 29)
        ruleViolation("Day must be between 1 and 29 for February in leap year, got: " + day);
    } else {
      if (day < 1 || day > DAY_ARRAY[month])
        ruleViolation(
            "Day must be between 1 and "
                + DAY_ARRAY[month]
                + " for month "
                + month
                + ", got: "
                + day);
    }
  }

  private void validateHour(int hour) {
    if (hour < 0 || hour >= 24) ruleViolation("Hour must be between 0 and 23, got: " + hour);
  }

  private void validateMinute(int minute) {
    if (minute < 0 || minute >= 60)
      ruleViolation("Minute must be between 0 and 59, got: " + minute);
  }

  private void validateSecond(int second) {
    if (second < 0 || second >= 60)
      ruleViolation("Second must be between 0 and 59, got: " + second);
  }

  private void ruleViolation(String message) {
    throw new IllegalArgumentException(
        ServerErrorCodes.ARGUMENT_ERROR.numericCode() + ": " + message);
  }
}
