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
package com.percussion.services.catalog;

import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A cataloger enumerates types and allows queries for data with enhanced Java 11 support.
 *
 * <p>Each cataloger handles one or more system types and provides functionality to:
 * <ul>
 *   <li>Enumerate supported object types</li>
 *   <li>Query and retrieve object summaries</li>
 *   <li>Load and save objects in XML format</li>
 *   <li>Support deployment and migration operations</li>
 * </ul>
 *
 * <p>The cataloger serves multiple contexts:
 * <ul>
 *   <li><strong>MSM (Multi-Server Manager):</strong> Deploying objects between Rhythmyx installations</li>
 *   <li><strong>Import/Export:</strong> Application-level data transfer operations</li>
 *   <li><strong>System Management:</strong> Object discovery and inventory</li>
 * </ul>
 *
 * <p>Key features:
 * <ul>
 *   <li>Type-safe object enumeration</li>
 *   <li>Optional-based safe navigation</li>
 *   <li>Stream-based data processing</li>
 *   <li>Enhanced error handling</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSCataloger {

   /**
    * Get the system types handled by this cataloger service.
    *
    * <p>Each service should handle one or more types exclusively. The deployment
    * system uses this information to route operations to the appropriate service,
    * particularly during import operations.
    *
    * @return array of handled types, never {@code null}, may be empty
    * @implNote Implementations should return a consistent set of types
    */
   PSTypeEnum[] getTypes();

   /**
    * Get the system types as a Set for enhanced type safety and operations.
    *
    * @return Set of handled types, never {@code null}, may be empty
    */
   default Set<PSTypeEnum> getTypesAsSet() {
      return Set.of(getTypes());
   }

   /**
    * Get the system types as a Stream for functional processing.
    *
    * @return Stream of handled types, never {@code null}
    */
   default Stream<PSTypeEnum> getTypesStream() {
      return Stream.of(getTypes());
   }

   /**
    * Check if this cataloger handles the specified type.
    *
    * @param type the type to check, not {@code null}
    * @return true if this cataloger handles the specified type
    * @throws IllegalArgumentException if type is null
    */
   default boolean handlesType(PSTypeEnum type) {
      if (type == null) {
         throw new IllegalArgumentException("type cannot be null");
      }
      return getTypesAsSet().contains(type);
   }

   /**
    * Get object summaries for the specified type with enhanced error handling.
    *
    * <p>This method queries known objects of the specified type, useful for:
    * <ul>
    *   <li>Export operations to discover available data</li>
    *   <li>Deployment system inventory management</li>
    *   <li>Administrative object browsing</li>
    * </ul>
    *
    * @param type the specific type to query, not {@code null}
    * @return list of summaries for items of the given type, never {@code null}
    *         but may be empty if the type is not handled or no items exist
    * @throws PSCatalogException if there is a problem loading summary data
    * @throws PSNotFoundException if the type cannot be found
    * @throws IllegalArgumentException if type is null
    */
   List<IPSCatalogSummary> getSummaries(PSTypeEnum type)
           throws PSCatalogException, PSNotFoundException;

   /**
    * Get object summaries with Optional wrapper for safer access.
    *
    * @param type the specific type to query, not {@code null}
    * @return Optional containing the list of summaries if successful, empty if error occurs
    */
   default Optional<List<IPSCatalogSummary>> getSummariesOptional(PSTypeEnum type) {
      try {
         return Optional.of(getSummaries(type));
      } catch (PSCatalogException | PSNotFoundException e) {
         return Optional.empty();
      }
   }

   /**
    * Get object summaries as a Stream for functional processing.
    *
    * @param type the specific type to query, not {@code null}
    * @return Stream of summaries, empty if error occurs or no items found
    */
   default Stream<IPSCatalogSummary> getSummariesStream(PSTypeEnum type) {
      return getSummariesOptional(type)
         .map(List::stream)
         .orElse(Stream.empty());
   }

   /**
    * Load an item from XML and store in persistent storage with enhanced validation.
    *
    * <p>This operation may overwrite an existing object in the persistent store.
    * The XML document should be well-formed and conform to the expected schema
    * for the specified type.
    *
    * @param type the type for the incoming item, not {@code null}
    * @param item the XML document string representing the item, not {@code null} or empty
    * @throws PSCatalogException if there's an error saving the item or type isn't handled
    * @throws IllegalArgumentException if parameters are invalid
    */
   void loadByType(PSTypeEnum type, String item) throws PSCatalogException;

   /**
    * Save an item to XML format by GUID with enhanced error handling.
    *
    * <p>The item type is determined from the GUID. The item is retrieved from
    * persistent storage and serialized to XML format suitable for export or
    * deployment operations.
    *
    * @param id the GUID of the item to save, not {@code null}
    * @return the XML document string representing the item, never {@code null} or empty
    * @throws PSCatalogException if there's an error loading the item or type isn't handled
    * @throws IllegalArgumentException if id is null
    */
   String saveByType(IPSGuid id) throws PSCatalogException;

   /**
    * Save an item to XML with Optional wrapper for safer access.
    *
    * @param id the GUID of the item to save, not {@code null}
    * @return Optional containing the XML string if successful, empty if error occurs
    */
   default Optional<String> saveByTypeOptional(IPSGuid id) {
      try {
         return Optional.of(saveByType(id));
      } catch (PSCatalogException e) {
         return Optional.empty();
      }
   }

   /**
    * Check if an item exists in the catalog.
    *
    * @param id the GUID of the item to check, not {@code null}
    * @return true if the item exists and can be saved to XML
    */
   default boolean itemExists(IPSGuid id) {
      return saveByTypeOptional(id).isPresent();
   }
}
