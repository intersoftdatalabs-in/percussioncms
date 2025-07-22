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
package com.percussion.services.pubserver;

import com.percussion.services.pubserver.data.PSPubServerProperty;
import com.percussion.services.pubserver.impl.PSPubServerDao;
import com.percussion.utils.guid.IPSGuid;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents a publishing server with modern Java 11 patterns. The server manager performs all CRUD
 * operations on server objects with enhanced type safety and Optional-based access.
 *
 * @author leonardohildt
 */
public interface IPSPubServer {

   String DEFAULT_DTS = "NONE";

   /**
    * The publishing type with enhanced utility methods. Used to indicate which mechanism
    * to be used to publish to the live site.
    */
   enum PublishType {
      /** Publishing defaults to local filesystem */
      FILESYSTEM("filesystem", "Local filesystem publishing"),

      /** Publishing will be done via FTP */
      FTP("ftp", "FTP publishing"),

      /** Publishing will be done via SFTP */
      SFTP("sftp", "Secure FTP publishing"),

      /** Publishing will be done to database */
      DATABASE("database", "Database publishing");

      private final String value;
      private final String description;

      PublishType(String value, String description) {
         this.value = value;
         this.description = description;
      }

      /**
       * Get the string value for this publish type.
       *
       * @return the string value, never null
       */
      public String getValue() {
         return value;
      }

      /**
       * Get a human-readable description of this publish type.
       *
       * @return the description, never null
       */
      public String getDescription() {
         return description;
      }

      /**
       * Check if this is a filesystem-based publish type.
       *
       * @return true if filesystem
       */
      public boolean isFilesystem() {
         return this == FILESYSTEM;
      }

      /**
       * Check if this is an FTP-based publish type (FTP or SFTP).
       *
       * @return true if FTP or SFTP
       */
      public boolean isFtpBased() {
         return this == FTP || this == SFTP;
      }

      /**
       * Check if this is a database publish type.
       *
       * @return true if database
       */
      public boolean isDatabase() {
         return this == DATABASE;
      }

      /**
       * Find a PublishType by its string value.
       *
       * @param value the string value to search for
       * @return Optional containing the matching PublishType, empty if not found
       */
      public static Optional<PublishType> fromValue(String value) {
         if (value == null) return Optional.empty();

         return Stream.of(values())
            .filter(type -> type.value.equalsIgnoreCase(value))
            .findFirst();
      }
   }

   /**
    * Property names to be encoded for security.
    */
   String[] ENCODED_PROPERTY_NAMES = {PSPubServerDao.PUBLISH_PASSWORD_PROPERTY};

   Set<String> ENCODED_PROPERTY_NAMES_SET = Set.of(ENCODED_PROPERTY_NAMES);

   /**
    * Get the unique id for the server.
    * 
    * @return the guid, never null
    */
   IPSGuid getGUID();

   /**
    * Get the server id for this server.
    * 
    * @return the server id, never null or empty
    */
   long getServerId();

   /**
    * Set the server id.
    * 
    * @param serverId the server id to set
    */
   void setServerId(long serverId);

   /**
    * The server name, never null or empty.
    *
    * @return the server name, never null
    */
   String getName();

   /**
    * Set the server name.
    *
    * @param name the name to set, never null or empty
    */
   void setName(String name);

   /**
    * Get the description that describes this server.
    * 
    * @return Optional containing the description, empty if not set
    */
   Optional<String> getDescription();

   /**
    * Set the description.
    * 
    * @param description the description to set, may be null
    */
   void setDescription(String description);

   /**
    * Get the publish type for the server.
    * 
    * @return the publish type, never null or empty
    */
   String getPublishType();

   /**
    * Get the publish type as an enum for type-safe operations.
    *
    * @return Optional containing the PublishType enum, empty if unknown type
    */
   default Optional<PublishType> getPublishTypeEnum() {
      return PublishType.fromValue(getPublishType());
   }

   /**
    * Returns the publish server URL for the server.
    *
    * @return Optional containing the server URL, empty if not configured
    */
   Optional<String> getPublishServer();

   /**
    * Set the publish type for this server.
    * 
    * @param publishType the publish type to set, never null
    */
   void setPublishType(String publishType);

   /**
    * Set the publish type using the enum for type safety.
    *
    * @param publishType the publish type enum to set, never null
    */
   default void setPublishType(PublishType publishType) {
      setPublishType(publishType.getValue());
   }

   /**
    * Get all properties for this server.
    *
    * @return unmodifiable set of properties, never null
    */
   Set<PSPubServerProperty> getProperties();
   
   /**
    * Retrieves the property from this server that has the given name with safe access.
    * The comparison is made ignoring letter case.
    *
    * @param propertyName the property name to search for, may be blank
    * @return Optional containing the property if found, empty otherwise
    */
   Optional<PSPubServerProperty> getProperty(String propertyName);

   /**
    * Returns the property value without being decoded, with safe access.
    *
    * @param propertyName the property name to search for, may be blank
    * @return Optional containing the property value if it exists, empty otherwise
    */
   Optional<String> getPropertyValue(String propertyName);

   /**
    * Similar to {@link #getPropertyValue(String)} but returns the default value
    * if the property is not found or is empty.
    *
    * @param propertyName the property name to search for, may be blank
    * @param defaultValue the default value to return if property not found
    * @return the property value or default value, never null
    */
   String getPropertyValue(String propertyName, String defaultValue);

   /**
    * Helper method to determine if the server publishes in XML format.
    *
    * @return true if the server publishes in XML format
    */
   boolean isXmlFormat();
   
   /**
    * Helper method to determine if the server publishes to a database.
    *
    * @return true if the server publishes to database
    */
   default boolean isDatabaseType() {
      return getPublishTypeEnum()
         .map(PublishType::isDatabase)
         .orElse(false);
   }

   /**
    * Helper method to determine if the server is publishing to FTP or SFTP.
    *
    * @return true if FTP-based, false otherwise
    */
   default boolean isFtpType() {
      return getPublishTypeEnum()
         .map(PublishType::isFtpBased)
         .orElse(false);
   }

   /**
    * Get the site ID associated with this server.
    *
    * @return the site id, never null
    */
   long getSiteId();

   /**
    * Get the server type.
    *
    * @return Optional containing the server type, empty if not set
    */
   Optional<String> getServerType();

   /**
    * Set the server type.
    *
    * @param serverType the server type to set, may be null
    */
   void setServerType(String serverType);

   /**
    * Determine if this server has been fully published since created or configuration changed.
    *
    * @return true if it has been fully published, false otherwise
    */
   boolean hasFullPublished();
   
   /**
    * Set if this server has been fully published since created or configuration changed.
    *
    * @param hasFullPublished true if it has been fully published, false otherwise
    */
   void setHasFullPublished(boolean hasFullPublished);
}
