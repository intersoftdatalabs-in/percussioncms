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

package com.percussion.log;

import com.percussion.design.objectstore.PSLogger;
import com.percussion.utils.testing.UnitTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.security.SecureRandom;

import static org.junit.Assert.assertTrue;

/**
 *   Unit tests for the PSLogHandler class
 */
@Category(UnitTest.class)
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
      assertTrue("Error logging off", !handler.isErrorLoggingEnabled());
      assertTrue("Server start/stop logging off",
         !handler.isServerStartStopLoggingEnabled());
      assertTrue("Application start/stop logging off",
         !handler.isAppStartStopLoggingEnabled());
      assertTrue("Application statistics logging off",
         !handler.isAppStatisticsLoggingEnabled());
      assertTrue("Basic user activity logging off",
         !handler.isBasicUserActivityLoggingEnabled());
      assertTrue("Detailed user activity logging off",
         !handler.isDetailedUserActivityLoggingEnabled());
      assertTrue("Multiple handler logging off",
         !handler.isMultipleHandlerLoggingEnabled());
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
            assertTrue("Error logging on", handler.isErrorLoggingEnabled());
         else
            assertTrue("Error logging off", !handler.isErrorLoggingEnabled());
         if (0 != (options & SERVER_STARTSTOP))
            assertTrue("Server start/stop logging on", handler.isServerStartStopLoggingEnabled());
         else
            assertTrue("Server start/stop logging off", !handler.isServerStartStopLoggingEnabled());
         if (0 != (options & APP_STARTSTOP))
            assertTrue("Application start/stop logging on", handler.isAppStartStopLoggingEnabled());
         else
            assertTrue("Application start/stop logging off", !handler.isAppStartStopLoggingEnabled());
         if (0 != (options & APP_STATS))
            assertTrue("Application statistics logging on", handler.isAppStatisticsLoggingEnabled());
         else
            assertTrue("Application statistics logging off", !handler.isAppStatisticsLoggingEnabled());
         if (0 != (options & BASIC_USER))
            assertTrue("Basic user activity logging on", handler.isBasicUserActivityLoggingEnabled());
         else
            assertTrue("Basic user activity logging off", !handler.isBasicUserActivityLoggingEnabled());
         if (0 != (options & DETAILED_USER))
            assertTrue("Detailed user activity logging on", handler.isDetailedUserActivityLoggingEnabled());
         else
            assertTrue("Detailed user activity logging off", !handler.isDetailedUserActivityLoggingEnabled());
         if (0 != (options & MULTIPLE_HANDLER))
            assertTrue("Multiple handler logging on", handler.isMultipleHandlerLoggingEnabled());
         else
            assertTrue("Multiple handler logging off", !handler.isMultipleHandlerLoggingEnabled());
      } // end for
   }
}
