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

package com.percussion.log;

import com.percussion.design.objectstore.PSLogger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 *   Unit tests for the PSLogHandler class
 */
@Tag("UnitTest")
public class PSLogHandlerTest
{
   public PSLogHandlerTest()
   {
   }

   /**
    *   Tests that all options are initially off
    */
   @Test
   public void TestOptionsInitiallyOff()
   {
      PSLogHandler handler = new PSLogHandler();
      assertFalse(handler.isErrorLoggingEnabled(), "Error logging off");
      assertFalse(handler.isServerStartStopLoggingEnabled(), "Server start/stop logging off");
      assertFalse(handler.isAppStartStopLoggingEnabled(), "Application start/stop logging off");
      assertFalse(handler.isAppStatisticsLoggingEnabled(), "Application statistics logging off");
      assertFalse(handler.isBasicUserActivityLoggingEnabled(), "Basic user activity logging off");
      assertFalse(handler.isDetailedUserActivityLoggingEnabled(), "Detailed user activity logging off");
      assertFalse(handler.isMultipleHandlerLoggingEnabled(), "Multiple handler logging off");
   }

   /**
    *   Randomly turn arguments on and off and check that they are
    *   indeed on or off.
    */
   @Test
   public void TestOptionsEnabling()
   {
      final int ERROR_LOGGING = 1;
      final int SERVER_STARTSTOP = 2;
      final int APP_STARTSTOP = 4;
      final int APP_STATS = 8;
      final int DETAILED_USER = 16;
      final int BASIC_USER = 32;
      final int MULTIPLE_HANDLER = 64;

      PSLogger logger = new PSLogger();
      SecureRandom rand = new SecureRandom();
      int options = 0;

      for (int i = 0; i < 100; i++)
      {
         options = rand.nextInt(ERROR_LOGGING | SERVER_STARTSTOP | APP_STARTSTOP |
            APP_STATS | DETAILED_USER | BASIC_USER | MULTIPLE_HANDLER);

         logger.setErrorLoggingEnabled(0 != (options & ERROR_LOGGING));
         logger.setServerStartStopLoggingEnabled(0 != (options & SERVER_STARTSTOP));
         logger.setAppStartStopLoggingEnabled(0 != (options & APP_STARTSTOP));
         logger.setAppStatisticsLoggingEnabled(0 != (options & APP_STATS));
         logger.setBasicUserActivityLoggingEnabled(0 != (options & BASIC_USER));
         logger.setDetailedUserActivityLoggingEnabled(0 != (options & DETAILED_USER));
         logger.setMultipleHandlerLoggingEnabled(0 != (options & MULTIPLE_HANDLER));

         PSLogHandler handler = new PSLogHandler(logger);

         if (0 != (options & ERROR_LOGGING))
            assertTrue(handler.isErrorLoggingEnabled(), "Error logging on");
         else
            assertFalse(handler.isErrorLoggingEnabled(), "Error logging off");
         if (0 != (options & SERVER_STARTSTOP))
            assertTrue(handler.isServerStartStopLoggingEnabled(), "Server start/stop logging on");
         else
            assertFalse(handler.isServerStartStopLoggingEnabled(), "Server start/stop logging off");
         if (0 != (options & APP_STARTSTOP))
            assertTrue(handler.isAppStartStopLoggingEnabled(), "Application start/stop logging on");
         else
            assertFalse(handler.isAppStartStopLoggingEnabled(), "Application start/stop logging off");
         if (0 != (options & APP_STATS))
            assertTrue(handler.isAppStatisticsLoggingEnabled(), "Application statistics logging on");
         else
            assertFalse(handler.isAppStatisticsLoggingEnabled(), "Application statistics logging off");
         if (0 != (options & BASIC_USER))
            assertTrue(handler.isBasicUserActivityLoggingEnabled(), "Basic user activity logging on");
         else
            assertFalse(handler.isBasicUserActivityLoggingEnabled(), "Basic user activity logging off");
         if (0 != (options & DETAILED_USER))
            assertTrue(handler.isDetailedUserActivityLoggingEnabled(), "Detailed user activity logging on");
         else
            assertFalse(handler.isDetailedUserActivityLoggingEnabled(), "Detailed user activity logging off");
         if (0 != (options & MULTIPLE_HANDLER))
            assertTrue(handler.isMultipleHandlerLoggingEnabled(), "Multiple handler logging on");
         else
            assertFalse(handler.isMultipleHandlerLoggingEnabled(), "Multiple handler logging off");
      } // end for
   }
}
