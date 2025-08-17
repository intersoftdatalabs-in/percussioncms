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
package com.percussion.services.ui;

import java.util.Set;

/**
 * Error codes and constants for UI service operations with modern Java 11 patterns.
 * Provides comprehensive error definitions for use with the bundle
 * PSUiErrorStringBundle.properties and enhanced error categorization.
 *
 * @author Percussion Software
 */
public interface IPSUiErrors {

   /**
    * Missing hierarchy node error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The id of the missing hierarchy node</TD></TR>
    * </TABLE>
    */
   int MISSING_HIERARCHY_NODE = 1;

   /**
    * Node not found error - alias for MISSING_HIERARCHY_NODE for consistency.
    */
   int NODE_NOT_FOUND = MISSING_HIERARCHY_NODE;

   /**
    * Duplicate node name error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The duplicate node name</TD></TR>
    * <TR><TD>1</TD><TD>The parent node id or "root"</TD></TR>
    * </TABLE>
    */
   int DUPLICATE_NODE_NAME = 2;

   /**
    * Invalid hierarchy operation error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The operation that failed</TD></TR>
    * <TR><TD>1</TD><TD>The reason for failure</TD></TR>
    * </TABLE>
    */
   int INVALID_HIERARCHY_OPERATION = 3;

   /**
    * Node type mismatch error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The node id</TD></TR>
    * <TR><TD>1</TD><TD>The expected node type</TD></TR>
    * <TR><TD>2</TD><TD>The actual node type</TD></TR>
    * </TABLE>
    */
   int NODE_TYPE_MISMATCH = 4;

   /**
    * General operation failed error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The operation that failed</TD></TR>
    * <TR><TD>1</TD><TD>Additional error details</TD></TR>
    * </TABLE>
    */
   int OPERATION_FAILED = 5;

   /**
    * Invalid node name error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The invalid node name</TD></TR>
    * <TR><TD>1</TD><TD>The validation rule that failed</TD></TR>
    * </TABLE>
    */
   int INVALID_NODE_NAME = 6;

   /**
    * Circular reference error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The source node id</TD></TR>
    * <TR><TD>1</TD><TD>The target node id</TD></TR>
    * </TABLE>
    */
   int CIRCULAR_REFERENCE = 7;

   /**
    * Access denied error.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The operation that was denied</TD></TR>
    * <TR><TD>1</TD><TD>The node id or resource</TD></TR>
    * </TABLE>
    */
   int ACCESS_DENIED = 8;

   /**
    * Set of all error codes for validation and utility operations.
    */
   Set<Integer> ALL_ERROR_CODES = Set.of(
      MISSING_HIERARCHY_NODE,
      DUPLICATE_NODE_NAME,
      INVALID_HIERARCHY_OPERATION,
      NODE_TYPE_MISMATCH,
      OPERATION_FAILED,
      INVALID_NODE_NAME,
      CIRCULAR_REFERENCE,
      ACCESS_DENIED
   );

   /**
    * Set of critical error codes that require immediate attention.
    */
   Set<Integer> CRITICAL_ERROR_CODES = Set.of(
      MISSING_HIERARCHY_NODE,
      CIRCULAR_REFERENCE,
      ACCESS_DENIED
   );

   /**
    * Set of validation error codes related to data integrity.
    */
   Set<Integer> VALIDATION_ERROR_CODES = Set.of(
      DUPLICATE_NODE_NAME,
      INVALID_NODE_NAME,
      NODE_TYPE_MISMATCH
   );

   /**
    * Check if an error code is critical.
    *
    * @param errorCode the error code to check
    * @return true if the error code is critical, false otherwise
    */
   static boolean isCriticalError(int errorCode) {
      return CRITICAL_ERROR_CODES.contains(errorCode);
   }

   /**
    * Check if an error code is a validation error.
    *
    * @param errorCode the error code to check
    * @return true if the error code is a validation error, false otherwise
    */
   static boolean isValidationError(int errorCode) {
      return VALIDATION_ERROR_CODES.contains(errorCode);
   }

   /**
    * Check if an error code is valid.
    *
    * @param errorCode the error code to check
    * @return true if the error code is valid, false otherwise
    */
   static boolean isValidErrorCode(int errorCode) {
      return ALL_ERROR_CODES.contains(errorCode);
   }

   /**
    * Get a human-readable description of an error code category.
    *
    * @param errorCode the error code to categorize
    * @return the category description
    */
   static String getErrorCategory(int errorCode) {
      if (isCriticalError(errorCode)) {
         return "Critical Error";
      } else if (isValidationError(errorCode)) {
         return "Validation Error";
      } else if (isValidErrorCode(errorCode)) {
         return "Operation Error";
      } else {
         return "Unknown Error";
      }
   }
}
