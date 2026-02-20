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

   /** Argument missing for filter processing (generic) */
   int ARGUMENT_MISSING = 6;

   /** Parameter combination error: auth type or filter expected */
   int PARAMS_AUTHTYPE_OR_FILTER = 7;

   /** A probable cycle detected in filter rule graph */
   int PROBABLE_CYCLE = 8;

   /** All defined error codes for validation and lookup operations */
   Set<Integer> ALL_ERROR_CODES = Set.of(
      FILTER_MISSING,
      AUTHTYPE_MISSING,
      RULE_MISSING,
      DATABASE,
      RULE_ARGUMENT_MISSING,
      ARGUMENT_MISSING,
      PARAMS_AUTHTYPE_OR_FILTER,
      PROBABLE_CYCLE
   );

   /** Error code descriptions for enhanced error reporting */
   Map<Integer, String> ERROR_DESCRIPTIONS = Map.ofEntries(
      Map.entry(FILTER_MISSING, "Filter not found"),
      Map.entry(AUTHTYPE_MISSING, "Unknown authentication type"),
      Map.entry(RULE_MISSING, "Filter rule not found"),
      Map.entry(DATABASE, "Database operation failed"),
      Map.entry(RULE_ARGUMENT_MISSING, "Required rule parameter missing"),
      Map.entry(ARGUMENT_MISSING, "Required argument missing"),
      Map.entry(PARAMS_AUTHTYPE_OR_FILTER, "Missing auth type or filter parameter"),
      Map.entry(PROBABLE_CYCLE, "Probable cycle detected in filter rules")
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
