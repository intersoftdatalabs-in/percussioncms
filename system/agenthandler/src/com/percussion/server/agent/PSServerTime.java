/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.server.agent;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Utility class for server time operations and formatting.
 * Provides thread-safe time utilities using Java 11 time APIs.
 *
 * @since Java 11
 */
public final class PSServerTime {

   private static final DateTimeFormatter DEFAULT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

   private static final DateTimeFormatter ISO_FORMATTER =
      DateTimeFormatter.ISO_LOCAL_DATE_TIME;

   private static Clock clock = Clock.systemDefaultZone();

   /**
    * Private constructor to prevent instantiation of utility class.
    */
   private PSServerTime() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
   }

   /**
    * Gets the current server time as an Instant.
    *
    * @return the current server time
    */
   public static Instant getCurrentTime() {
      return Instant.now(clock);
   }

   /**
    * Gets the current server time as a LocalDateTime.
    *
    * @return the current server time as LocalDateTime
    */
   public static LocalDateTime getCurrentLocalTime() {
      return LocalDateTime.now(clock);
   }

   /**
    * Formats the current time using the default format.
    *
    * @return formatted current time string
    */
   public static String getFormattedCurrentTime() {
      return getCurrentLocalTime().format(DEFAULT_FORMATTER);
   }

   /**
    * Formats the current time using ISO format.
    *
    * @return ISO formatted current time string
    */
   public static String getISOFormattedCurrentTime() {
      return getCurrentLocalTime().format(ISO_FORMATTER);
   }

   /**
    * Formats the given instant using the default format.
    *
    * @param instant the instant to format
    * @return formatted time string, or empty string if instant is null
    */
   public static String formatTime(Instant instant) {
      return Optional.ofNullable(instant)
         .map(i -> LocalDateTime.ofInstant(i, ZoneId.systemDefault()))
         .map(ldt -> ldt.format(DEFAULT_FORMATTER))
         .orElse("");
   }

   /**
    * Formats the given instant using ISO format.
    *
    * @param instant the instant to format
    * @return ISO formatted time string, or empty string if instant is null
    */
   public static String formatTimeISO(Instant instant) {
      return Optional.ofNullable(instant)
         .map(i -> LocalDateTime.ofInstant(i, ZoneId.systemDefault()))
         .map(ldt -> ldt.format(ISO_FORMATTER))
         .orElse("");
   }

   /**
    * Gets the current time in milliseconds since epoch.
    *
    * @return current time in milliseconds
    */
   public static long getCurrentTimeMillis() {
      return getCurrentTime().toEpochMilli();
   }

   /**
    * Calculates the duration between two instants in milliseconds.
    *
    * @param start the start instant
    * @param end the end instant
    * @return duration in milliseconds, or 0 if either instant is null
    */
   public static long getDurationMillis(Instant start, Instant end) {
      if (start == null || end == null) {
         return 0L;
      }
      return Math.abs(end.toEpochMilli() - start.toEpochMilli());
   }

   /**
    * Sets the clock for testing purposes.
    * This method should only be used in test environments.
    *
    * @param testClock the clock to use for testing
    */
   static void setClock(Clock testClock) {
      PSServerTime.clock = testClock != null ? testClock : Clock.systemDefaultZone();
   }

   /**
    * Resets the clock to system default.
    * This method should only be used in test environments.
    */
   static void resetClock() {
      PSServerTime.clock = Clock.systemDefaultZone();
   }
}
