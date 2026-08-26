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
package com.percussion.services.filter;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;

import java.util.Objects;

/**
 * Exception for problems in filter rules with comprehensive Java 11 modernization.
 * Provides enhanced error handling for filter operations with improved validation
 * and modern exception patterns.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Static factory methods for common filter error scenarios</li>
 * <li>Improved constructor documentation with clear contracts</li>
 * <li>Modern exception chaining patterns</li>
 * </ul>
 *
 * @see IPSFilterServiceErrors for message codes, and the corresponding
 * property bundle for the messages. Each message code documents what, if any,
 * arguments need to be passed.
 * 
 * @author dougrand
 */
public class PSFilterException extends PSBaseException {

   /**
    * Serial ids are required for objects that implement java.io.Serializable
    */
   private static final long serialVersionUID = -1763123318413410377L;

   /**
    * Constructs a filter exception with message code and arguments.
    *
    * @param msgCode the message code for the exception
    * @param arrayArgs the arguments for the exception, may be empty but not {@code null}
    * @throws IllegalArgumentException if arrayArgs is null
    */
   public PSFilterException(int msgCode, Object... arrayArgs) {
      super(msgCode, Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Constructs a filter exception with message code, cause, and arguments.
    *
    * @param msgCode the message code for the exception
    * @param cause the original cause, not {@code null}
    * @param arrayArgs the arguments for the exception, may be empty but not {@code null}
    * @throws IllegalArgumentException if cause or arrayArgs is null
    */
   public PSFilterException(int msgCode, Throwable cause, Object... arrayArgs) {
      super(msgCode,
            Objects.requireNonNull(cause, "cause cannot be null"),
            Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Constructs a filter exception with message code only.
    *
    * @param msgCode the message code for the exception
    */
   public PSFilterException(int msgCode) {
      super(msgCode);
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode}.
    *
    * @param code catalogued error code, never {@code null}
    * @param arrayArgs the arguments for the exception, may be empty but not {@code null}
    */
   public PSFilterException(IPSErrorCode code, Object... arrayArgs) {
      super(code, Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Typed construction with a cause.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause the original cause, not {@code null}
    * @param arrayArgs the arguments for the exception, may be empty but not {@code null}
    */
   public PSFilterException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
      super(code,
            Objects.requireNonNull(cause, "cause cannot be null"),
            Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Creates a filter exception for filter not found scenarios.
    *
    * @param filterName the name of the filter that was not found, not {@code null}
    * @return a new PSFilterException instance
    * @throws IllegalArgumentException if filterName is null
    */
   public static PSFilterException filterNotFound(String filterName) {
      Objects.requireNonNull(filterName, "filterName cannot be null");
      return new PSFilterException(0, "Filter not found", filterName);
   }

   /**
    * Creates a filter exception for invalid filter configuration.
    *
    * @param filterName the name of the invalid filter, not {@code null}
    * @param reason the reason for invalidity, not {@code null}
    * @return a new PSFilterException instance
    * @throws IllegalArgumentException if filterName or reason is null
    */
   public static PSFilterException invalidFilter(String filterName, String reason) {
      Objects.requireNonNull(filterName, "filterName cannot be null");
      Objects.requireNonNull(reason, "reason cannot be null");
      return new PSFilterException(0, "Invalid filter configuration", filterName, reason);
   }

   /**
    * Creates a filter exception for filter operation failures.
    *
    * @param operation the operation that failed, not {@code null}
    * @param cause the underlying cause, not {@code null}
    * @return a new PSFilterException instance
    * @throws IllegalArgumentException if operation or cause is null
    */
   public static PSFilterException operationFailed(String operation, Throwable cause) {
      Objects.requireNonNull(operation, "operation cannot be null");
      Objects.requireNonNull(cause, "cause cannot be null");
      return new PSFilterException(0, cause, "Filter operation failed", operation);
   }

   /**
    * Creates a filter exception for duplicate filter name scenarios.
    *
    * @param filterName the duplicate filter name, not {@code null}
    * @return a new PSFilterException instance
    * @throws IllegalArgumentException if filterName is null
    */
   public static PSFilterException duplicateFilterName(String filterName) {
      Objects.requireNonNull(filterName, "filterName cannot be null");
      return new PSFilterException(0, "Duplicate filter name", filterName);
   }

   /**
    * Creates a filter exception with a descriptive message and cause.
    *
    * @param message the detail message, not {@code null}
    * @param cause the cause of this exception, not {@code null}
    * @return a new PSFilterException instance
    * @throws IllegalArgumentException if message or cause is null
    */
   public static PSFilterException withMessageAndCause(String message, Throwable cause) {
      Objects.requireNonNull(message, "message cannot be null");
      Objects.requireNonNull(cause, "cause cannot be null");
      return new PSFilterException(0, cause, message);
   }

   /**
    * Creates a filter exception with a descriptive message.
    *
    * @param message the detail message, not {@code null}
    * @return a new PSFilterException instance
    * @throws IllegalArgumentException if message is null
    */
   public static PSFilterException withMessage(String message) {
      Objects.requireNonNull(message, "message cannot be null");
      return new PSFilterException(0, message);
   }

   @Override
   protected String getResourceBundleBaseName() {
      return "com.percussion.services.filter.PSFilterErrorStringBundle";
   }
}
