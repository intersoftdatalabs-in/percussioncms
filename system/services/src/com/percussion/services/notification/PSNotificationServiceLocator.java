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

// REFACTORED: CP-JAVA11
package com.percussion.services.notification;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Optional;

/**
 * Thread-safe service locator for the notification service using modern Java 11 patterns.
 *
 * <p>This utility class provides access to the notification service implementation through
 * Spring bean configuration. It uses double-checked locking for thread-safe singleton
 * initialization and provides both traditional and Optional-based access methods.</p>
 *
 * @author dougrand
 */
public final class PSNotificationServiceLocator {

   private static volatile IPSNotificationService notificationService;

   /**
    * Private constructor to prevent instantiation of this utility class.
    */
   private PSNotificationServiceLocator() {
      // Utility class - no instances allowed
   }

   /**
    * Gets the notification service using thread-safe lazy initialization.
    *
    * @return the notification service, never {@code null}
    * @throws PSMissingBeanConfigurationException if there's a problem with the
    *         Spring configuration or the bean cannot be found
    */
   public static IPSNotificationService getNotificationService() throws PSMissingBeanConfigurationException {
      var localRef = notificationService;
      if (localRef == null) {
         synchronized (PSNotificationServiceLocator.class) {
            localRef = notificationService;
            if (localRef == null) {
               notificationService = localRef = (IPSNotificationService) PSBaseServiceLocator.getBean("sys_notificationService");
            }
         }
      }
      return localRef;
   }

   /**
    * Safely gets the notification service wrapped in an Optional.
    * This method catches any configuration exceptions and returns an empty Optional.
    *
    * @return Optional containing the notification service, empty if configuration fails
    */
   public static Optional<IPSNotificationService> getNotificationServiceSafely() {
      try {
         return Optional.of(getNotificationService());
      } catch (PSMissingBeanConfigurationException e) {
         // Log the error if needed and return empty Optional
         return Optional.empty();
      }
   }

   /**
    * Checks if the notification service is available and properly configured.
    *
    * @return {@code true} if the service is available, {@code false} otherwise
    */
   public static boolean isServiceAvailable() {
      return getNotificationServiceSafely().isPresent();
   }
}
