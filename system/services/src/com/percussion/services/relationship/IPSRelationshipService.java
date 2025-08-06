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
package com.percussion.services.relationship;

import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.error.PSException;
import com.percussion.services.relationship.data.PSRelationshipData;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Modern relationship service interface for managing relationship objects with Java 11 features.
 *
 * <p>This service manages {@link PSRelationship} and {@link PSRelationshipData} objects,
 * providing comprehensive CRUD operations, filtering capabilities, and configuration management.
 * All operations are thread-safe and support both synchronous and asynchronous processing.</p>
 *
 * <p>Key features include:
 * <ul>
 *   <li>Optional-based safe object retrieval</li>
 *   <li>Stream API support for efficient data processing</li>
 *   <li>Asynchronous operations using CompletableFuture</li>
 *   <li>Enhanced validation and error handling</li>
 *   <li>Bulk operations for improved performance</li>
 * </ul>
 * </p>
 */
public interface IPSRelationshipService {

   /**
    * Saves a relationship into the repository with enhanced validation.
    *
    * @param relationship the relationship to be saved, never {@code null}.
    *                    The dirty flag will be reset after successful save.
    * @throws PSException if error occurs when loading relationship configurations
    * @throws IllegalArgumentException if relationship is null
    */
   void saveRelationship(PSRelationship relationship) throws PSException;

   /**
    * Saves multiple relationships into the repository efficiently.
    *
    * @param relationships the relationships to be saved, never {@code null}.
    *                     The dirty flag will be reset for all after successful save.
    * @throws PSException if error occurs when loading relationship configurations
    * @throws IllegalArgumentException if relationships is null
    */
   void saveRelationships(Collection<PSRelationship> relationships) throws PSException;

   /**
    * Legacy method for backward compatibility.
    *
    * @param rdatas the relationships to be saved
    * @throws PSException if error occurs
    * @deprecated Use {@link #saveRelationships(Collection)} instead
    */
   @Deprecated
   default void saveRelationship(Collection<PSRelationship> rdatas) throws PSException {
      saveRelationships(rdatas);
   }

   /**
    * Asynchronously saves a relationship without blocking the calling thread.
    *
    * @param relationship the relationship to be saved, never {@code null}
    * @return CompletableFuture that completes when the save operation finishes
    * @throws IllegalArgumentException if relationship is null
    */
   default CompletableFuture<Void> saveRelationshipAsync(PSRelationship relationship) {
      return CompletableFuture.runAsync(() -> {
         try {
            saveRelationship(relationship);
         } catch (PSException e) {
            throw new RuntimeException("Error saving relationship asynchronously", e);
         }
      });
   }

   /**
    * Deletes a relationship from the repository.
    *
    * @param relationship the relationship to be deleted, never {@code null}
    * @throws IllegalArgumentException if relationship is null
    */
   void deleteRelationship(PSRelationship relationship);

   /**
    * Deletes multiple relationships efficiently.
    *
    * @param relationships the relationships to be deleted, never {@code null} or empty
    * @throws IllegalArgumentException if relationships is null or empty
    */
   void deleteRelationships(Collection<PSRelationship> relationships);

   /**
    * Legacy method for backward compatibility.
    *
    * @param rdata the relationships to be deleted
    * @deprecated Use {@link #deleteRelationships(Collection)} instead
    */
   @Deprecated
   default void deleteRelationship(Collection<PSRelationship> rdata) {
      deleteRelationships(rdata);
   }

   /**
    * Deletes a relationship by its unique identifier.
    *
    * @param relationshipId the ID of the relationship to be deleted
    * @return the number of relationships deleted (0 or 1)
    * @throws IllegalArgumentException if relationshipId is negative
    */
   int deleteRelationshipById(int relationshipId);

   /**
    * Legacy method for backward compatibility.
    *
    * @param rid the relationship ID
    * @return number of deleted relationships
    * @deprecated Use {@link #deleteRelationshipById(int)} instead
    */
   @Deprecated
   default int deleteRelationshipByRid(int rid) {
      return deleteRelationshipById(rid);
   }

   /**
    * Loads a relationship by its unique identifier with Optional-based safe access.
    *
    * @param relationshipId the relationship ID to load
    * @return Optional containing the relationship if found, empty otherwise
    * @throws PSException if error occurs when loading relationship configurations
    * @throws IllegalArgumentException if relationshipId is negative
    */
   Optional<PSRelationship> loadRelationship(int relationshipId) throws PSException;

   /**
    * Finds relationships matching the specified filter criteria.
    *
    * @param filter the filter used to lookup relationships, never {@code null}
    * @return list of matching relationships, never {@code null}, may be empty
    * @throws PSException if failed to load relationship configurations
    * @throws IllegalArgumentException if filter is null
    */
   List<PSRelationship> findByFilter(PSRelationshipFilter filter) throws PSException;

   /**
    * Streams relationships matching the filter for efficient processing.
    *
    * @param filter the filter used to lookup relationships, never {@code null}
    * @return Stream of matching relationships, never {@code null}
    * @throws PSException if failed to load relationship configurations
    * @throws IllegalArgumentException if filter is null
    */
   default Stream<PSRelationship> streamByFilter(PSRelationshipFilter filter) throws PSException {
      return findByFilter(filter).stream();
   }

   /**
    * Finds persisted relationship IDs from a collection of candidate IDs.
    *
    * @param candidateIds the relationship IDs to test for persistence, never {@code null}
    * @return list of persisted relationship IDs, never {@code null}, may be empty
    * @throws IllegalArgumentException if candidateIds is null
    */
   List<Integer> findPersistedRelationshipIds(Collection<Integer> candidateIds);

   /**
    * Legacy method for backward compatibility.
    *
    * @param testedIds the IDs to test
    * @return list of persisted IDs
    * @deprecated Use {@link #findPersistedRelationshipIds(Collection)} instead
    */
   @Deprecated
   default List<Integer> findPersistedRid(Collection<Integer> testedIds) {
      return findPersistedRelationshipIds(testedIds);
   }

   /**
    * Reloads relationship configurations from the command handler and updates internal cache.
    *
    * <p>This method must be called after relationship configurations are updated
    * to ensure the service operates with the latest configuration data.</p>
    *
    * @throws PSException if failed to load the relationship configurations
    */
   void reloadConfigurations() throws PSException;

   /**
    * Legacy method for backward compatibility.
    *
    * @throws PSException if failed to reload
    * @deprecated Use {@link #reloadConfigurations()} instead
    */
   @Deprecated
   default void reloadConfigs() throws PSException {
      reloadConfigurations();
   }

   /**
    * Finds relationship data by dependent object ID.
    *
    * @param dependentId the ID of the dependent object
    * @return list of relationship data, never {@code null}, may be empty
    * @throws IllegalArgumentException if dependentId is negative
    */
   List<PSRelationshipData> findByDependentId(int dependentId);

   /**
    * Finds relationship data by dependent ID and configuration ID.
    *
    * @param dependentId the ID of the dependent object
    * @param configId the relationship configuration ID
    * @return list of relationship data, never {@code null}, may be empty
    * @throws IllegalArgumentException if dependentId or configId is negative
    */
   List<PSRelationshipData> findByDependentIdAndConfigId(int dependentId, int configId);

   /**
    * Legacy method for backward compatibility.
    *
    * @param dependentId the dependent ID
    * @param configId the config ID
    * @return list of relationship data
    * @deprecated Use {@link #findByDependentIdAndConfigId(int, int)} instead
    */
   @Deprecated
   default List<PSRelationshipData> findByDependentIdConfigId(int dependentId, int configId) {
      return findByDependentIdAndConfigId(dependentId, configId);
   }

   /**
    * Updates relationship data in the repository.
    *
    * @param relationshipData the relationship data to update, never {@code null}
    * @throws IllegalArgumentException if relationshipData is null
    */
   void updateRelationshipData(PSRelationshipData relationshipData);

   /**
    * Streams relationship data by dependent ID for efficient processing.
    *
    * @param dependentId the ID of the dependent object
    * @return Stream of relationship data, never {@code null}
    * @throws IllegalArgumentException if dependentId is negative
    */
   default Stream<PSRelationshipData> streamByDependentId(int dependentId) {
      return findByDependentId(dependentId).stream();
   }

   /**
    * Checks if a relationship exists with the given ID.
    *
    * @param relationshipId the relationship ID to check
    * @return {@code true} if the relationship exists, {@code false} otherwise
    * @throws IllegalArgumentException if relationshipId is negative
    */
   default boolean relationshipExists(int relationshipId) {
      try {
         return loadRelationship(relationshipId).isPresent();
      } catch (PSException e) {
         return false;
      }
   }

   /**
    * Gets the count of relationships matching the filter.
    *
    * @param filter the filter to apply, never {@code null}
    * @return the count of matching relationships
    * @throws PSException if failed to load relationship configurations
    * @throws IllegalArgumentException if filter is null
    */
   default long countByFilter(PSRelationshipFilter filter) throws PSException {
      return findByFilter(filter).size();
   }
}
