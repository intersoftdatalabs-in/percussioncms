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

// REFACTORED: CP-JAVA11
package com.percussion.services.security;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Optional;

/**
 * Thread-safe service locator for role managers using modern Java 11 patterns.
 *
 * <p>This utility class provides access to both standard and backend role manager
 * implementations through Spring bean configuration. It uses double-checked locking
 * for thread-safe singleton initialization and provides both traditional and
 * Optional-based access methods.</p>
 *
 * @author dougrand
 */
public final class PSRoleMgrLocator {

   private static volatile IPSRoleMgr roleManager;
   private static volatile IPSBackEndRoleMgr backEndRoleManager;

   /**
    * Private constructor to prevent instantiation of this utility class.
    */
   private PSRoleMgrLocator() {
      // Utility class - no instances allowed
   }

   /**
    * Gets the role manager using thread-safe lazy initialization.
    *
    * @return the role manager, never {@code null}
    * @throws PSMissingBeanConfigurationException if there's a problem with the
    *         Spring configuration or the bean cannot be found
    */
   public static IPSRoleMgr getRoleManager() throws PSMissingBeanConfigurationException {
      var localRef = roleManager;
      if (localRef == null) {
         synchronized (PSRoleMgrLocator.class) {
            localRef = roleManager;
            if (localRef == null) {
               roleManager = localRef = (IPSRoleMgr) PSBaseServiceLocator.getBean("sys_roleMgr");
            }
         }
      }
      return localRef;
   }
   
   /**
    * Gets the backend role manager using thread-safe lazy initialization.
    *
    * @return the backend role manager, never {@code null}
    * @throws PSMissingBeanConfigurationException if there's a problem with the
    *         Spring configuration or the bean cannot be found
    */
   public static IPSBackEndRoleMgr getBackEndRoleManager() throws PSMissingBeanConfigurationException {
      var localRef = backEndRoleManager;
      if (localRef == null) {
         synchronized (PSRoleMgrLocator.class) {
            localRef = backEndRoleManager;
            if (localRef == null) {
               backEndRoleManager = localRef = (IPSBackEndRoleMgr) PSBaseServiceLocator.getBean("sys_backEndRoleMgr");
            }
         }
      }
      return localRef;
   }

   /**
    * Safely gets the role manager wrapped in an Optional.
    * This method catches any configuration exceptions and returns an empty Optional.
    *
    * @return Optional containing the role manager, empty if configuration fails
    */
   public static Optional<IPSRoleMgr> getRoleManagerSafely() {
      try {
         return Optional.of(getRoleManager());
      } catch (PSMissingBeanConfigurationException e) {
         // Log the error if needed and return empty Optional
         return Optional.empty();
      }
   }

   /**
    * Safely gets the backend role manager wrapped in an Optional.
    * This method catches any configuration exceptions and returns an empty Optional.
    *
    * @return Optional containing the backend role manager, empty if configuration fails
    */
   public static Optional<IPSBackEndRoleMgr> getBackEndRoleManagerSafely() {
      try {
         return Optional.of(getBackEndRoleManager());
      } catch (PSMissingBeanConfigurationException e) {
         // Log the error if needed and return empty Optional
         return Optional.empty();
      }
   }

   /**
    * Checks if the role manager is available and properly configured.
    *
    * @return {@code true} if the service is available, {@code false} otherwise
    */
   public static boolean isRoleManagerAvailable() {
      return getRoleManagerSafely().isPresent();
   }

   /**
    * Checks if the backend role manager is available and properly configured.
    *
    * @return {@code true} if the service is available, {@code false} otherwise
    */
   public static boolean isBackEndRoleManagerAvailable() {
      return getBackEndRoleManagerSafely().isPresent();
   }
}
