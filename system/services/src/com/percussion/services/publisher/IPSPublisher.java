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

package com.percussion.services.publisher;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a publisher from the database. A publisher specifies the
 * parameters that are needed to communicate with a specific publishing hub.
 * This interface provides modern Java 11 patterns for publisher configuration
 * with enhanced validation and safe access methods.
 *
 * @author dougrand
 */
public interface IPSPublisher {

   /**
    * The unique identifier for each publisher in the database, aka the primary key.
    *
    * @return the primary key value, never {@code null} for a persisted publisher
    */
   Integer getId();

   /**
    * Set a new identifier, illegal for a persisted instance.
    *
    * @param publisherid the new publisher id, never {@code null}
    * @throws IllegalArgumentException if publisherid is null
    */
   void setId(Integer publisherid);

   /**
    * The name of the publisher instance.
    *
    * @return the name of the publisher instance, may be {@code null} or empty
    */
   String getName();

   /**
    * Get the name safely with Optional wrapper.
    *
    * @return Optional containing the name, or empty if not set
    */
   default Optional<String> getNameSafely() {
      return Optional.ofNullable(getName())
                     .filter(name -> !name.trim().isEmpty());
   }

   /**
    * Set the name.
    *
    * @param name the name, may be {@code null} or empty
    */
   void setName(String name);

   /**
    * The description of the publisher instance.
    *
    * @return the description of the publisher instance, may be {@code null} or empty
    */
   String getDescription();

   /**
    * Get the description safely with Optional wrapper.
    *
    * @return Optional containing the description, or empty if not set
    */
   default Optional<String> getDescriptionSafely() {
      return Optional.ofNullable(getDescription())
                     .filter(desc -> !desc.trim().isEmpty());
   }

   /**
    * Set the description.
    *
    * @param description the description, may be {@code null} or empty
    */
   void setDescription(String description);

   /**
    * Get the IP address.
    *
    * @return the IP address of the publishing hub, never {@code null} or empty
    */
   String getIpAddress();

   /**
    * Set the IP address.
    *
    * @param ipaddress the new IP address, never {@code null} or empty
    * @throws IllegalArgumentException if ipaddress is null or empty
    */
   void setIpAddress(String ipaddress);

   /**
    * Get the port.
    *
    * @return the port for the publishing hub, never {@code null}
    */
   Integer getPort();

   /**
    * Set the port.
    *
    * @param port the port, never {@code null}
    * @throws IllegalArgumentException if port is null or invalid
    */
   void setPort(Integer port);

   /**
    * The user ID to use with the CMS, used by the publishing hub when
    * requesting the assembled output to publish.
    *
    * @return the user ID, never {@code null} or empty
    */
   String getUserId();

   /**
    * Set the user ID.
    *
    * @param userid the user ID, never {@code null} or empty
    * @throws IllegalArgumentException if userid is null or empty
    */
   void setUserId(String userid);

   /**
    * The password to use with the CMS, used by the publishing hub when
    * requesting the assembled output to publish.
    *
    * @return the password, never {@code null} or empty
    */
   String getPassword();

   /**
    * Set the password.
    *
    * @param password the password, never {@code null} or empty
    * @throws IllegalArgumentException if password is null or empty
    */
   void setPassword(String password);

   /**
    * Get the publishing hub UID. If defined, this will be used when authenticating
    * requests to the publishing hub.
    *
    * @return the publishing hub UID, may be {@code null}
    */
   String getPubuid();

   /**
    * Get the publishing hub UID safely with Optional wrapper.
    *
    * @return Optional containing the hub UID, or empty if not set
    */
   default Optional<String> getPubuidSafely() {
      return Optional.ofNullable(getPubuid())
                     .filter(uid -> !uid.trim().isEmpty());
   }

   /**
    * Set the publishing hub UID.
    *
    * @param pubuid the publishing hub UID, may be {@code null}
    */
   void setPubuid(String pubuid);

   /**
    * Get the publishing hub password. If defined, this will be used when authenticating
    * requests to the publishing hub.
    *
    * @return the publishing hub password, may be {@code null}
    */
   String getPubpw();

   /**
    * Get the publishing hub password safely with Optional wrapper.
    *
    * @return Optional containing the hub password, or empty if not set
    */
   default Optional<String> getPubpwSafely() {
      return Optional.ofNullable(getPubpw())
                     .filter(pw -> !pw.trim().isEmpty());
   }

   /**
    * Set the publishing hub password.
    *
    * @param pubpw the publishing hub password, may be {@code null}
    */
   void setPubpw(String pubpw);

   /**
    * Check if this publisher is fully configured with required settings.
    *
    * @return true if all required fields are properly set
    */
   default boolean isFullyConfigured() {
      return getId() != null &&
             getIpAddress() != null && !getIpAddress().trim().isEmpty() &&
             getPort() != null && getPort() > 0 && getPort() < 65536 &&
             getUserId() != null && !getUserId().trim().isEmpty() &&
             getPassword() != null && !getPassword().trim().isEmpty();
   }

   /**
    * Check if this publisher has hub authentication configured.
    *
    * @return true if both hub UID and password are set
    */
   default boolean hasHubAuthentication() {
      return getPubuidSafely().isPresent() && getPubpwSafely().isPresent();
   }

   /**
    * Validates the port number.
    *
    * @param port the port to validate
    * @return true if the port is valid (1-65535)
    */
   static boolean isValidPort(Integer port) {
      return port != null && port > 0 && port < 65536;
   }

   /**
    * Validates an IP address format (basic validation).
    *
    * @param ipAddress the IP address to validate
    * @return true if the IP address appears valid
    */
   static boolean isValidIpAddress(String ipAddress) {
      if (ipAddress == null || ipAddress.trim().isEmpty()) {
         return false;
      }
      // Basic IPv4 format check - for production, use more robust validation
      return ipAddress.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") ||
             ipAddress.matches("^[a-zA-Z0-9.-]+$"); // Allow hostnames
   }
}
