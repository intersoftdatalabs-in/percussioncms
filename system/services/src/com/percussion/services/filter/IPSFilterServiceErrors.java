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
package com.percussion.services.filter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provides error codes and utilities for messages located in
 * {@code PSFilterErrorStringBundle}. This interface defines standardized
 * error constants for filter service operations with enhanced validation
 * and lookup capabilities.
 */
public interface IPSFilterServiceErrors {

   /**
    * Missing filter
    * <p>
    * The arguments passed in for this message are:
    * <ul>
    * <li>Arg 0: The name of the filter</li>
    * </ul>
    */
   int FILTER_MISSING = 1;

   /**
    * Unknown authtype
    * <p>
    * The arguments passed in for this message are:
    * <ul>
    * <li>Arg 0: The value of the authtype</li>
    * </ul>
    */
   int AUTHTYPE_MISSING = 2;

   /**
    * Filter rule missing
    * <p>
    * The arguments passed in for this message are:
    * <ul>
    * <li>Arg 0: The name of the missing rule</li>
    * </ul>
    */
   int RULE_MISSING = 3;

   /**
    * Database problem while processing a filter - no arguments
    */
   int DATABASE = 4;

   /**
    * Filter rule argument missing
    * <p>
    * The arguments passed in for this message are:
    * <ul>
    * <li>Arg 0: The name of the rule</li>
    * <li>Arg 1: The name of the missing parameter</li>
    * </ul>
    */
   int RULE_ARGUMENT_MISSING = 5;

   /** All defined error codes for validation and lookup operations */
   Set<Integer> ALL_ERROR_CODES = Set.of(
      FILTER_MISSING,
      AUTHTYPE_MISSING,
      RULE_MISSING,
      DATABASE,
      RULE_ARGUMENT_MISSING
   );

   /** Error code descriptions for enhanced error reporting */
   Map<Integer, String> ERROR_DESCRIPTIONS = Map.of(
      FILTER_MISSING, "Filter not found",
      AUTHTYPE_MISSING, "Unknown authentication type",
      RULE_MISSING, "Filter rule not found",
      DATABASE, "Database operation failed",
      RULE_ARGUMENT_MISSING, "Required rule parameter missing"
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
}
