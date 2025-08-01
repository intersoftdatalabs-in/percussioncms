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
package com.percussion.services.pubserver;

import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.ToDoVulnerability;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data Access Object for publishing server operations with modern Java 11 patterns.
 * Provides comprehensive CRUD operations for publishing servers with enhanced type safety
 * and Optional-based safe access methods.
 *
 * @author Percussion Software
 */
public interface IPSPubServerDao {

   /**
    * Publishing server property constants with comprehensive coverage.
    */
   String PUBLISH_SERVER_NAME_PROPERTY = "serverName";
   String PUBLISH_FOLDER_PROPERTY = "folder";
   String PUBLISH_SERVER_IP_PROPERTY = "serverip";
   String PUBLISH_USER_ID_PROPERTY = "userid";
   String PUBLISH_PORT_PROPERTY = "port";
   String PUBLISH_PASSWORD_PROPERTY = "password";
   String PUBLISH_SECURE_FTP_PROPERTY = "secure";
   String PUBLISH_SID_PROPERTY = "sid";
   String PUBLISH_SCHEMA_PROPERTY = "schema";
   String PUBLISH_DATABASE_NAME_PROPERTY = "database";
   String PUBLISH_DATABASE_SERVER_NAME = "server";
   String PUBLISH_OWNER_PROPERTY = "owner";
   String PUBLISH_DEFAULT_SERVER_PROPERTY = "defaultServer";
   String PUBLISH_PRIVATE_KEY_PROPERTY = "privateKey";
   String PUBLISH_OWN_SERVER_PROPERTY = "ownServer";
   String PUBLISH_DRIVER_PROPERTY = "driver";
   String PUBLISH_RESOURCES_PROPERTY = "resources";
   String PUBLISH_FORMAT_PROPERTY = "format";
   String PUBLISH_EC2_REGION = "region";
   String PUBLISH_AS3_BUCKET_PROPERTY = "bucketlocation";
   String PUBLISH_AS3_SECURITYKEY_PROPERTY = "securitykey";
   String PUBLISH_AS3_ACCESSKEY_PROPERTY = "accesskey";
   String PUBLISH_AS3_USE_ASSUME_ROLE = "useAssumeRole";
   String PUBLISH_AS3_ARN_ROLE = "ARNRole";
   String PUBLISH_RELATED_PROPERTY = "publishRelatedItems";
   String PUBLISH_SERVER_PROPERTY = "publishServer";
   String PUBLISH_SECURE_SITE_CONF = "publishSecureSiteConfigOnExactPath";

   /**
    * Set of all property names for validation and utility operations.
    */
   Set<String> ALL_PROPERTY_NAMES = Set.of(
      PUBLISH_SERVER_NAME_PROPERTY, PUBLISH_FOLDER_PROPERTY, PUBLISH_SERVER_IP_PROPERTY,
      PUBLISH_USER_ID_PROPERTY, PUBLISH_PORT_PROPERTY, PUBLISH_PASSWORD_PROPERTY,
      PUBLISH_SECURE_FTP_PROPERTY, PUBLISH_SID_PROPERTY, PUBLISH_SCHEMA_PROPERTY,
      PUBLISH_DATABASE_NAME_PROPERTY, PUBLISH_DATABASE_SERVER_NAME, PUBLISH_OWNER_PROPERTY,
      PUBLISH_DEFAULT_SERVER_PROPERTY, PUBLISH_PRIVATE_KEY_PROPERTY, PUBLISH_OWN_SERVER_PROPERTY,
      PUBLISH_DRIVER_PROPERTY, PUBLISH_RESOURCES_PROPERTY, PUBLISH_FORMAT_PROPERTY,
      PUBLISH_EC2_REGION, PUBLISH_AS3_BUCKET_PROPERTY, PUBLISH_AS3_SECURITYKEY_PROPERTY,
      PUBLISH_AS3_ACCESSKEY_PROPERTY, PUBLISH_AS3_USE_ASSUME_ROLE, PUBLISH_AS3_ARN_ROLE,
      PUBLISH_RELATED_PROPERTY, PUBLISH_SERVER_PROPERTY, PUBLISH_SECURE_SITE_CONF
   );

   /**
    * Set of sensitive properties that require encryption.
    */
   Set<String> SENSITIVE_PROPERTIES = Set.of(
      PUBLISH_PASSWORD_PROPERTY, PUBLISH_PRIVATE_KEY_PROPERTY,
      PUBLISH_AS3_SECURITYKEY_PROPERTY, PUBLISH_AS3_ACCESSKEY_PROPERTY
   );

   @ToDoVulnerability
   @Deprecated
   String ENCRYPTION_KEY = PSLegacyEncrypter.PUBSERVER_ENCRYPTION_KEY;

   /**
    * Create a publish server for the given site with enhanced validation.
    * The created publish server will contain a valid ID and default configuration.
    * The returned object is not persisted to the repository.
    *
    * @param site the site to create server for, never null
    * @return the publish server with valid ID, never null
    * @throws IllegalArgumentException if site is null or invalid
    */
   PSPubServer createServer(IPSSite site);

   /**
    * Find a server object from the cache with safe access.
    * The returned object is read-only and should not be modified.
    *
    * @param serverId the server ID, never null
    * @return Optional containing the server if found, empty otherwise
    * @throws IllegalArgumentException if serverId is null
    */
   Optional<PSPubServer> findPubServer(IPSGuid serverId);

   /**
    * Convenient method to find the server with long ID using safe access.
    * The returned object is read-only and should not be modified.
    *
    * @param serverId the server ID
    * @return Optional containing the server if found, empty otherwise
    */
   Optional<PSPubServer> findPubServer(long serverId);

   /**
    * Load a server object from the cache with guaranteed existence.
    * The returned object is read-only and should not be modified.
    *
    * @param serverId the server ID, never null
    * @return the server, never null
    * @throws PSNotFoundException if the server does not exist
    * @throws IllegalArgumentException if serverId is null
    */
   PSPubServer loadPubServer(IPSGuid serverId) throws PSNotFoundException;

   /**
    * Load a modifiable server object that can be saved back to the repository.
    * This method should be used when you need to update server properties.
    *
    * @param serverId the server ID, never null
    * @return the modifiable server, never null
    * @throws PSNotFoundException if the server does not exist
    * @throws IllegalArgumentException if serverId is null
    */
   PSPubServer loadPubServerModifiable(IPSGuid serverId) throws PSNotFoundException;

   /**
    * Find all publish servers for the specified site with enhanced filtering.
    *
    * @param siteId the site ID, never null
    * @return unmodifiable list of servers for the site, never null but may be empty
    * @throws IllegalArgumentException if siteId is null
    */
   List<PSPubServer> findPubServersBySite(IPSGuid siteId);

   /**
    * Find all publish servers across all sites.
    *
    * @return unmodifiable list of all servers, never null but may be empty
    */
   default List<PSPubServer> findAllPubServers() {
      // Default implementation can be overridden by implementations
      throw new UnsupportedOperationException("findAllPubServers not implemented");
   }

   /**
    * Save or update the publishing server in the database with validation.
    *
    * @param pubServer the publishing server to save, never null
    * @throws IllegalArgumentException if pubServer is null or invalid
    */
   void savePubServer(PSPubServer pubServer);

   /**
    * Delete the publishing server from the database.
    *
    * @param pubServer the pub server to delete, never null
    * @throws IllegalArgumentException if pubServer is null
    */
   void deletePubServer(PSPubServer pubServer);

   /**
    * Check if a property name is sensitive and requires encryption.
    *
    * @param propertyName the property name to check
    * @return true if the property is sensitive, false otherwise
    */
   default boolean isSensitiveProperty(String propertyName) {
      return propertyName != null && SENSITIVE_PROPERTIES.contains(propertyName);
   }

   /**
    * Validate that a property name is recognized.
    *
    * @param propertyName the property name to validate
    * @return true if the property name is valid, false otherwise
    */
   default boolean isValidPropertyName(String propertyName) {
      return propertyName != null && ALL_PROPERTY_NAMES.contains(propertyName);
   }
}
