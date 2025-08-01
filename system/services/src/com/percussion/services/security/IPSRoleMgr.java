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
package com.percussion.services.security;

import com.percussion.security.IPSDirectoryCataloger;
import com.percussion.security.IPSTypedPrincipal;
import com.percussion.services.security.data.PSCatalogerConfig;
import com.percussion.security.IPSPrincipalAttribute;
import com.percussion.security.IPSRoleCataloger;
import com.percussion.security.IPSSubjectCataloger;
import com.percussion.security.PSSecurityCatalogException;
import com.percussion.security.IPSPrincipalAttribute.PrincipalAttributes;
import com.percussion.utils.xml.PSInvalidXmlException;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.security.auth.Subject;

import org.xml.sax.SAXException;

/**
 * Modern role management service providing comprehensive cataloging services for roles and subjects
 * defined by both internal and external catalogers with Java 11 enhancements.
 *
 * <p>This service provides persistence services for external cataloger configurations and
 * supports both synchronous and asynchronous operations for improved performance and
 * thread safety. All methods include enhanced validation and Optional-based safe access.</p>
 *
 * <p>Key features include:
 * <ul>
 *   <li>Optional-based safe access for nullable operations</li>
 *   <li>Stream API support for efficient data processing</li>
 *   <li>Asynchronous operations using CompletableFuture</li>
 *   <li>Enhanced validation with descriptive error messages</li>
 *   <li>Thread-safe operations for concurrent access</li>
 * </ul>
 * </p>
 */
public interface IPSRoleMgr {

   /**
    * Type constant for subject catalogers.
    */
   String SUBJECT_CATALOGER_TYPE = "subjectCataloger";

   /**
    * Convenience method that calls {@link #findUsers(List, String, String) findUsers(names, null, null)}.
    *
    * @param names The names of subjects to find, may be {@code null} or empty
    * @return List of matching subjects, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    */
   default List<Subject> findUsers(List<String> names) throws PSSecurityCatalogException {
      return findUsers(names, null, null);
   }

   /**
    * Convenience method that calls {@link #findUsers(List, String, String, Set) findUsers(names, catalogerName, type, null)}.
    *
    * @param names The names of subjects to find
    * @param catalogerName The cataloger name to query
    * @param type The cataloger type
    * @return List of matching subjects, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    */
   default List<Subject> findUsers(List<String> names, String catalogerName, String type)
         throws PSSecurityCatalogException {
      return findUsers(names, catalogerName, type, null);
   }

   /**
    * Convenience method that calls {@link #findUsers(List, String, String, Set, boolean) findUsers(names, catalogerName, type, supportedTypes, false)}.
    *
    * @param names The names of subjects to find
    * @param catalogerName The cataloger name to query
    * @param type The cataloger type
    * @param supportedTypes The supported attribute types
    * @return List of matching subjects, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    */
   default List<Subject> findUsers(List<String> names, String catalogerName, String type,
         Set<PrincipalAttributes> supportedTypes) throws PSSecurityCatalogException {
      return findUsers(names, catalogerName, type, supportedTypes, false);
   }

   /**
    * Find all matching subjects by querying all subject catalogers and directory providers.
    *
    * @param names The names of subjects to find. May be {@code null} or empty to return all.
    *              Names may contain wildcards ('_' for single char, '%' for any)
    * @param catalogerName The cataloger name to query, may be {@code null} to query all
    * @param type The cataloger type, required if catalogerName is specified
    * @param supportedTypes Only catalogers supporting all specified types will be queried
    * @param throwCatalogerExceptions if {@code true}, throws exceptions immediately;
    *                                if {@code false}, continues with other catalogers on failure
    * @return List of matching subjects, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    * @throws IllegalArgumentException if type is null/empty when catalogerName is specified
    */
   List<Subject> findUsers(List<String> names, String catalogerName, String type,
         Set<PrincipalAttributes> supportedTypes, boolean throwCatalogerExceptions)
         throws PSSecurityCatalogException;

   /**
    * Asynchronously find users without blocking the calling thread.
    *
    * @param names The names of subjects to find
    * @return CompletableFuture containing the list of subjects
    */
   default CompletableFuture<List<Subject>> findUsersAsync(List<String> names) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return findUsers(names);
         } catch (PSSecurityCatalogException e) {
            throw new RuntimeException("Error finding users asynchronously", e);
         }
      });
   }

   /**
    * Stream users for efficient processing.
    *
    * @param names The names of subjects to find
    * @return Stream of subjects, never {@code null}
    */
   default Stream<Subject> streamUsers(List<String> names) {
      try {
         return findUsers(names).stream();
      } catch (PSSecurityCatalogException e) {
         return Stream.empty();
      }
   }

   /**
    * Catalog the members of the specified role.
    *
    * @param roleName The role name, never {@code null} or empty
    * @return Set of role members, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    * @throws IllegalArgumentException if roleName is null or empty
    */
   Set<IPSTypedPrincipal> getRoleMembers(String roleName) throws PSSecurityCatalogException;

   /**
    * Catalog the members of the specified role filtered by principal type.
    *
    * @param roleName The role name, never {@code null} or empty
    * @param type The principal type filter, never {@code null}
    * @return Set of filtered role members, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    * @throws IllegalArgumentException if roleName is null/empty or type is null/UNDEFINED
    */
   Set<IPSTypedPrincipal> getRoleMembers(String roleName, IPSTypedPrincipal.PrincipalTypes type)
         throws PSSecurityCatalogException;

   /**
    * Stream role members for efficient processing.
    *
    * @param roleName The role name to query
    * @return Stream of role members, never {@code null}
    */
   default Stream<IPSTypedPrincipal> streamRoleMembers(String roleName) {
      try {
         return getRoleMembers(roleName).stream();
      } catch (PSSecurityCatalogException e) {
         return Stream.empty();
      }
   }

   /**
    * Get roles for a user from the default back-end role cataloger only.
    *
    * @param user The user to check, may be {@code null} to get all role names
    * @return Set of role names, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    */
   Set<String> getDefaultUserRoles(IPSTypedPrincipal user) throws PSSecurityCatalogException;

   /**
    * Get all Rhythmyx roles that the specified user is a member of.
    *
    * @param user The user to check, may be {@code null} to get all role names
    * @return Set of role names, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    */
   Set<String> getUserRoles(IPSTypedPrincipal user) throws PSSecurityCatalogException;

   /**
    * Get user roles wrapped in Optional for safe access.
    *
    * @param user The user to check
    * @return Optional containing set of role names, empty if error occurs
    */
   default Optional<Set<String>> getUserRolesSafely(IPSTypedPrincipal user) {
      try {
         return Optional.of(getUserRoles(user));
      } catch (PSSecurityCatalogException e) {
         return Optional.empty();
      }
   }

   /**
    * Stream user roles for efficient processing.
    *
    * @param user The user to check
    * @return Stream of role names, never {@code null}
    */
   default Stream<String> streamUserRoles(IPSTypedPrincipal user) {
      return getUserRolesSafely(user).stream().flatMap(Set::stream);
   }

   /**
    * Get groups that the user is a member of.
    *
    * @param user The user to check, never {@code null}
    * @return Set of groups, never {@code null}, may be empty
    * @throws IllegalArgumentException if user is null
    */
   Set<Principal> getUserGroups(IPSTypedPrincipal user);

   /**
    * Find groups matching the specified pattern.
    *
    * @param pattern The pattern for group names ('_' = single char, '%' = any)
    * @param catalogerName The cataloger name, may be {@code null} to query all
    * @param type The cataloger type, required if catalogerName is specified
    * @return List of matching groups, never {@code null}, may be empty
    * @throws PSSecurityCatalogException if there are any errors
    * @throws IllegalArgumentException if type is null/empty when catalogerName is specified
    */
   List<Principal> findGroups(String pattern, String catalogerName, String type)
         throws PSSecurityCatalogException;

   /**
    * Stream groups for efficient processing.
    *
    * @param pattern The pattern for group names
    * @return Stream of groups, never {@code null}
    */
   default Stream<Principal> streamGroups(String pattern) {
      try {
         return findGroups(pattern, null, null).stream();
      } catch (PSSecurityCatalogException e) {
         return Stream.empty();
      }
   }

   /**
    * Get all defined cataloger configurations.
    *
    * @return List of cataloger configurations, never {@code null}, may be empty
    * @throws PSInvalidXmlException if the configuration XML is invalid
    * @throws IOException if there's an error loading the configuration
    * @throws SAXException if the configuration file is malformed
    */
   List<PSCatalogerConfig> getCatalogerConfigs()
         throws PSInvalidXmlException, IOException, SAXException;

   /**
    * Get cataloger configurations wrapped in Optional for safe access.
    *
    * @return Optional containing list of configurations, empty if error occurs
    */
   default Optional<List<PSCatalogerConfig>> getCatalogerConfigsSafely() {
      try {
         return Optional.of(getCatalogerConfigs());
      } catch (PSInvalidXmlException | IOException | SAXException e) {
         return Optional.empty();
      }
   }

   /**
    * Save cataloger configurations, replacing any currently defined.
    * Server restart required for configurations to become active.
    *
    * @param configs The configurations to save, never {@code null}
    * @throws PSInvalidXmlException if the configuration XML is invalid
    * @throws IOException if there's an error saving the configuration
    * @throws SAXException if the configuration file is malformed
    * @throws IllegalArgumentException if configs is null
    */
   void saveCatalogerConfigs(List<PSCatalogerConfig> configs)
         throws PSInvalidXmlException, IOException, SAXException;

   /**
    * Resolve group members, removing resolved groups from the input list.
    *
    * @param groups List of group principals, never {@code null}
    *               Resolved groups will be removed from this list
    * @return List of group members, never {@code null}, may be empty
    * @throws IllegalArgumentException if groups is null
    */
   List<IPSTypedPrincipal> getGroupMembers(Collection<? extends Principal> groups);

   /**
    * Stream group members for efficient processing.
    *
    * @param groups Collection of group principals
    * @return Stream of group members, never {@code null}
    */
   default Stream<IPSTypedPrincipal> streamGroupMembers(Collection<? extends Principal> groups) {
      return getGroupMembers(groups).stream();
   }

   /**
    * Get active external subject catalogers (excludes internal directory catalogers).
    *
    * @return List of external subject catalogers, never {@code null}, may be empty
    */
   List<IPSSubjectCataloger> getSubjectCatalogers();

   /**
    * Get internal directory catalogers.
    *
    * @return List of directory catalogers, never {@code null}, may be empty
    */
   List<IPSDirectoryCataloger> getDirectoryCatalogers();

   /**
    * Check if the cataloger is the default backend cataloger.
    *
    * @param cataloger The cataloger to check, never {@code null}
    * @return {@code true} if it's the default cataloger, {@code false} otherwise
    * @throws IllegalArgumentException if cataloger is null
    */
   boolean isDefaultCataloger(IPSDirectoryCataloger cataloger);

   /**
    * Check if the specified cataloger supports group cataloging.
    *
    * @param catalogerName The cataloger name, never {@code null} or empty
    * @param type The cataloger type, never {@code null} or empty
    * @return {@code true} if groups are supported, {@code false} otherwise
    * @throws IllegalArgumentException if catalogerName or type is null/empty
    */
   boolean supportsGroups(String catalogerName, String type);

   /**
    * Get names of roles defined within Rhythmyx.
    *
    * @return List of role names, never {@code null}, may be empty
    */
   List<String> getDefinedRoles();

   /**
    * Stream defined roles for efficient processing.
    *
    * @return Stream of role names, never {@code null}
    */
   default Stream<String> streamDefinedRoles() {
      return getDefinedRoles().stream();
   }

   /**
    * Get attributes of a role defined within the Rhythmyx backend.
    *
    * @param roleName The role name, never {@code null} or empty
    * @return Set of role attributes, never {@code null}, may be empty
    * @throws IllegalArgumentException if roleName is null or empty
    */
   Set<IPSPrincipalAttribute> getRoleAttributes(String roleName);

   /**
    * Get role attributes wrapped in Optional for safe access.
    *
    * @param roleName The role name to query
    * @return Optional containing set of attributes, empty if role not found
    */
   default Optional<Set<IPSPrincipalAttribute>> getRoleAttributesSafely(String roleName) {
      try {
         Objects.requireNonNull(roleName, "Role name cannot be null");
         if (roleName.trim().isEmpty()) {
            return Optional.empty();
         }
         return Optional.of(getRoleAttributes(roleName));
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Set the list of role catalogers to use.
    *
    * @param catalogers The catalogers list, never {@code null}
    * @throws IllegalArgumentException if catalogers is null
    */
   void setRoleCatalogers(List<IPSRoleCataloger> catalogers);

   /**
    * Set the list of subject catalogers to use.
    *
    * @param catalogers The catalogers list, never {@code null}
    * @throws IllegalArgumentException if catalogers is null
    */
   void setSubjectCatalogers(List<IPSSubjectCataloger> catalogers);

   /**
    * Check if a user exists in any cataloger.
    *
    * @param username The username to check
    * @return {@code true} if user exists, {@code false} otherwise
    */
   default boolean userExists(String username) {
      if (username == null || username.trim().isEmpty()) {
         return false;
      }
      try {
         var users = findUsers(List.of(username));
         return !users.isEmpty();
      } catch (PSSecurityCatalogException e) {
         return false;
      }
   }

   /**
    * Check if a role exists in the system.
    *
    * @param roleName The role name to check
    * @return {@code true} if role exists, {@code false} otherwise
    */
   default boolean roleExists(String roleName) {
      if (roleName == null || roleName.trim().isEmpty()) {
         return false;
      }
      return getDefinedRoles().contains(roleName);
   }
}
