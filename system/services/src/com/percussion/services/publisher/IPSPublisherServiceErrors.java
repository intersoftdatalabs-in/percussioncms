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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Publisher service error codes with enhanced validation and lookup utilities.
 * This interface defines standardized error constants for publisher service
 * operations with modern Java 11 patterns for error handling and validation.
 *
 * @author dougrand
 */
public interface IPSPublisherServiceErrors {

   /**
    * Missing content list by name.
    * <p>
    * Arguments: [0] The name of the content list
    */
   int LIST_MISSING = 10;

   /**
    * Invalid query.
    * <p>
    * Arguments: [0] The query
    */
   int BAD_QUERY = 11;

   /**
    * Repository error.
    * <p>
    * Arguments: [0] The repository problem
    */
   int REPOSITORY = 12;

   /**
    * Couldn't load the given site.
    * <p>
    * Arguments: [0] The site GUID
    */
   int SITE_LOAD = 13;

   /**
    * Couldn't load the given extension.
    * <p>
    * Arguments: [0] Extension name, [1] Extension context, [2] Extension interface
    */
   int MISSING_EXTENSION = 14;

   /**
    * A problem occurred while looking up an extension.
    */
   int EXTENSION_LOOKUP = 15;

   /**
    * A problem occurred while retrieving query rows from the generator.
    */
   int ROW_RETRIEVAL = 16;

   /**
    * An unknown database problem while retrieving data.
    */
   int DB = 17;

   /**
    * The filter failed to function properly.
    * <p>
    * Arguments: [0] The filter name
    */
   int FILTER_FAILED = 18;

   /**
    * Alias for FILTER_FAILED to preserve older error identifiers that used
    * a different name for the same condition.
    */
   int FILTER_MALFUNCTION = FILTER_FAILED;

   /**
    * Publishing job failed.
    * <p>
    * Arguments: [0] Job ID, [1] Error details
    */
   int JOB_FAILED = 19;

   /**
    * Content item publishing failed.
    * <p>
    * Arguments: [0] Content ID, [1] Site ID, [2] Error details
    */
   int ITEM_PUBLISH_FAILED = 20;

   /** General runtime error during publishing */
   int RUNTIME_ERROR = 21;

   /** Site is missing */
   int SITE_MISSING = 22;

   /** Publishing context missing */
   int CONTEXT_MISSING = 23;

   /** An unexpected error occurred */
   int UNEXPECTED = 24;

   /** All defined error codes for validation and lookup operations */
   Set<Integer> ALL_ERROR_CODES = Set.of(
      LIST_MISSING,
      BAD_QUERY,
      REPOSITORY,
      SITE_LOAD,
      MISSING_EXTENSION,
      EXTENSION_LOOKUP,
      ROW_RETRIEVAL,
      DB,
      FILTER_FAILED,
      JOB_FAILED,
      ITEM_PUBLISH_FAILED,
      RUNTIME_ERROR,
      SITE_MISSING,
      CONTEXT_MISSING,
      UNEXPECTED
   );

   /** Error code descriptions for enhanced error reporting */
   Map<Integer, String> ERROR_DESCRIPTIONS = Map.ofEntries(
      Map.entry(LIST_MISSING, "Content list not found"),
      Map.entry(BAD_QUERY, "Invalid query syntax"),
      Map.entry(REPOSITORY, "Repository access error"),
      Map.entry(SITE_LOAD, "Site loading failed"),
      Map.entry(MISSING_EXTENSION, "Extension not found"),
      Map.entry(EXTENSION_LOOKUP, "Extension lookup failed"),
      Map.entry(ROW_RETRIEVAL, "Query row retrieval failed"),
      Map.entry(DB, "Database operation failed"),
      Map.entry(FILTER_FAILED, "Content filter failed"),
      Map.entry(JOB_FAILED, "Publishing job failed"),
      Map.entry(ITEM_PUBLISH_FAILED, "Content item publishing failed"),
      Map.entry(RUNTIME_ERROR, "Runtime exception during publishing"),
      Map.entry(SITE_MISSING, "Site not found"),
      Map.entry(CONTEXT_MISSING, "Publishing context missing"),
      Map.entry(UNEXPECTED, "Unexpected error during publishing")
   );

   /**
    * Validates if an error code is defined in this interface.
    *
    * @param errorCode the error code to validate
    * @return true if the error code is valid, false otherwise
    */
   static boolean isValidErrorCode(int errorCode) {
      return ALL_ERROR_CODES.contains(errorCode);
   }

   /**
    * Gets a description for the specified error code.
    *
    * @param errorCode the error code to describe
    * @return Optional containing the description, or empty if code is invalid
    */
   static Optional<String> getErrorDescription(int errorCode) {
      return Optional.ofNullable(ERROR_DESCRIPTIONS.get(errorCode));
   }

   /**
    * Gets all valid error codes defined in this interface.
    *
    * @return immutable set of all error codes
    */
   static Set<Integer> getAllErrorCodes() {
      return ALL_ERROR_CODES;
   }

   /**
    * Checks if an error code indicates a data access problem.
    *
    * @param errorCode the error code to check
    * @return true if the error is related to data access
    */
   static boolean isDataAccessError(int errorCode) {
      return Set.of(REPOSITORY, DB, ROW_RETRIEVAL, SITE_LOAD).contains(errorCode);
   }

   /**
    * Checks if an error code indicates an extension problem.
    *
    * @param errorCode the error code to check
    * @return true if the error is related to extensions
    */
   static boolean isExtensionError(int errorCode) {
      return Set.of(MISSING_EXTENSION, EXTENSION_LOOKUP, FILTER_FAILED).contains(errorCode);
   }

   /**
    * Checks if an error code indicates a publishing operation failure.
    *
    * @param errorCode the error code to check
    * @return true if the error is related to publishing operations
    */
   static boolean isPublishingError(int errorCode) {
      return Set.of(JOB_FAILED, ITEM_PUBLISH_FAILED).contains(errorCode);
   }
}
