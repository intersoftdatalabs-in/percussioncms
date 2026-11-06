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

package com.percussion.utils.date;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Formats and parses HTTP dates.
 */
public class PSDateFormatter {

  /** Creates a date formatter. All methods are static. */
  public PSDateFormatter() {}

  /**
   * Formats a date for use in HTTP headers.
   *
   * @param d the date to format, never <code>null</code>
   * @return the formatted date string, never <code>null</code>
   */
  public static String formatDateForHttp(Date d) {
    return FastDateFormat.getInstance("EEE, d MMM yyyy HH:mm:ss 'GMT'", TimeZone.getTimeZone("GMT"))
        .format(d);
  }

  /**
   * Parses an HTTP header date string.
   *
   * @param d the date string to parse, never <code>null</code>
   * @return the parsed date, never <code>null</code>
   * @throws ParseException if the string cannot be parsed
   */
  public static Date parseDateFromHttp(String d) throws ParseException {
    return FastDateFormat.getInstance("EEE, d MMM yyyy HH:mm:ss 'GMT'", TimeZone.getTimeZone("GMT"))
        .parse(d);
  }
}
