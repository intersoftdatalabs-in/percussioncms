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
package com.percussion.share.dao;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Utility class for date manipulation.
 *
 * @author peterfrontiero
 */
public class PSDateUtils {

  private static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

  /**
   * ISO 8601 not extended timezone (used for parsing, since APIs don't have that possibility). Eg:
   * 2012-01-13T14:23:05.157-0200
   */
  public static final String ISO_8601_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  /**
   * ISO 8601 extended timezone (this is the correct pattern that should be used). Eg:
   * 2012-01-13T14:23:05.157-02:00
   */
  public static final String ISO_8601_EXTENDED_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

  /**
   * Converts the given date to string.
   *
   * @param date the date in question, may be {@code null}.
   * @return the converted string, empty if the given date is {@code null}.
   */
  public static String getDateToString(Date date) {
    if (date != null) {
      return FastDateFormat.getInstance(ISO_8601_EXTENDED_STRING).format(date);
    }
    return "";
  }

  /**
   * Converts the given string to date.
   *
   * @param date the string in question, may be {@code null}.
   * @return the converted date, or {@code null} if the given string is blank.
   * @throws ParseException if an error occurs parsing the string.
   */
  public static Date getDateFromString(String date) throws ParseException {
    if (!StringUtils.isBlank(date)) {
      // JSON objects may return long milliseconds as time
      try {
        return new Date(Long.parseLong(date));
      } catch (NumberFormatException ignored) {
        // Not a long, continue parsing as date string
      }
      String trimmed = date.trim();
      // Demo-site / search-index values are ISO-8601 with a trailing Z
      // (e.g. 2008-11-02T00:00:00.000Z). SimpleDateFormat pattern
      // ISO_8601_STRING uses RFC-822 Z and does not accept literal Z.
      if (trimmed.endsWith("Z")) {
        try {
          return Date.from(Instant.parse(trimmed));
        } catch (DateTimeException ignored) {
          // Fall through to OffsetDateTime / SimpleDateFormat
        }
      }
      try {
        return Date.from(OffsetDateTime.parse(trimmed).toInstant());
      } catch (DateTimeException ignored) {
        // Fall through to legacy SimpleDateFormat (RFC-822 offset, no colon)
      }
      DateFormat fmt = new SimpleDateFormat(ISO_8601_STRING);
      var format = new StringBuilder(trimmed);
      if (format.length() > 2 && ":".equals(String.valueOf(format.charAt(format.length() - 3)))) {
        format.deleteCharAt(format.length() - 3);
      }
      return fmt.parse(format.toString());
    }
    return null;
  }

  /**
   * Converts a given string to a Date, when the string is not in the ISO standard date format, such
   * as when it's provided by the JCR Node.
   *
   * @param dateStr the date string
   * @return Date as parsed from system string
   */
  public static Date parseSystemDateString(String dateStr) {
    var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    try {
      return dateFormat.parse(dateStr);
    } catch (ParseException e) {
      throw new RuntimeException("Invalid date string " + dateStr, e);
    }
  }

  /**
   * Calculates the number of days between two dates.
   *
   * @param start the start date
   * @param end the end date
   * @return the number of days difference
   * @throws IllegalArgumentException if end is before start
   */
  public static Integer getDaysDiff(Date start, Date end) {
    if (end.before(start)) {
      throw new IllegalArgumentException("The end date must be later than the start date");
    }

    // Reset all hours, mins, and secs to zero on start date
    var startCal = GregorianCalendar.getInstance();
    startCal.setTime(start);
    startCal.set(Calendar.HOUR_OF_DAY, 0);
    startCal.set(Calendar.MINUTE, 0);
    startCal.set(Calendar.SECOND, 0);
    long startTime = startCal.getTimeInMillis();

    // Reset all hours, mins, and secs to zero on end date
    var endCal = GregorianCalendar.getInstance();
    endCal.setTime(end);
    endCal.set(Calendar.HOUR_OF_DAY, 0);
    endCal.set(Calendar.MINUTE, 0);
    endCal.set(Calendar.SECOND, 0);
    long endTime = endCal.getTimeInMillis();

    return Math.toIntExact((endTime - startTime) / MILLISECONDS_IN_DAY);
  }
}
