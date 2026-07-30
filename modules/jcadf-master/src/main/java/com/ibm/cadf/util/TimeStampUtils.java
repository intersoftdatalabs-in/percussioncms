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

package com.ibm.cadf.util;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Timestamp utilities for CADF events — formats the current time using {@link
 * Constants#DEFAULT_TIME_FORMAT} and validates whether a string is non-empty. All methods are
 * static.
 */
public class TimeStampUtils {

  /** Default no-argument constructor for {@link TimeStampUtils}. */
  public TimeStampUtils() {}

  /**
   * Returns the current time formatted using {@link Constants#DEFAULT_TIME_FORMAT} in the supplied
   * timezone.
   *
   * @param timeZone the Java {@link TimeZone} id (e.g., {@code "UTC"}), never {@code null}.
   * @return the formatted timestamp string.
   */
  public static String getCurrentTime(String timeZone) {

    FastDateFormat format =
        FastDateFormat.getInstance(
            Constants.DEFAULT_TIME_FORMAT, TimeZone.getTimeZone(timeZone), Locale.US);

    return format.format(new Date());
  }

  /**
   * Returns the current time formatted as UTC using {@link Constants#DEFAULT_TIME_FORMAT}.
   *
   * @return the formatted UTC timestamp string.
   */
  public static String getCurrentTime() {
    return getCurrentTime("UTC");
  }

  /**
   * Returns {@code true} when the supplied timestamp string is non-empty.
   *
   * @param timesmap the timestamp string to validate, may be {@code null}.
   * @return {@code true} when {@code timesmap} is non-empty.
   */
  public static boolean isValid(String timesmap) {
    return StringUtils.isNotEmpty(timesmap);
  }
}
