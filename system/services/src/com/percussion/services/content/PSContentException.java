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
package com.percussion.services.content;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;

import java.util.Objects;
import java.util.Optional;

/**
 * Exception thrown by content services with enhanced Java 11 support.
 *
 * <p>This exception provides comprehensive error handling for content operations
 * including keyword management, auto-translations, and folder properties.
 *
 * <p>Key features:
 * <ul>
 *   <li>Enhanced null safety with Objects.requireNonNull()</li>
 *   <li>Optional-based safe message access</li>
 *   <li>Improved cause chain handling</li>
 *   <li>Modern exception construction patterns</li>
 * </ul>
 *
 * @since Java 11 Modernization
 */
public class PSContentException extends PSBaseException {

   /**
    * Serial version UID for serialization compatibility.
    */
   private static final long serialVersionUID = 2203597059355705199L;

   /**
    * Construct a content exception with the specified error code.
    *
    * @param msgCode the error message code
    */
   public PSContentException(int msgCode) {
      super(msgCode);
   }

   /**
    * Construct a content exception with the specified error code and arguments.
    *
    * @param msgCode the error message code
    * @param arrayArgs the arguments for message formatting, may be {@code null}
    */
   public PSContentException(int msgCode, Object... arrayArgs) {
      super(msgCode, arrayArgs);
   }

   /**
    * Construct a content exception with the specified error code, cause, and arguments.
    *
    * @param msgCode the error message code
    * @param cause the underlying cause, may be {@code null}
    * @param arrayArgs the arguments for message formatting, may be {@code null}
    */
   public PSContentException(int msgCode, Throwable cause, Object... arrayArgs) {
      super(msgCode, cause, arrayArgs);
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode}.
    *
    * @param code catalogued error code, never {@code null}
    * @param arrayArgs the arguments for message formatting, may be {@code null}
    */
   public PSContentException(IPSErrorCode code, Object... arrayArgs) {
      super(code, arrayArgs);
   }

   /**
    * Typed construction with a cause.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause the underlying cause, may be {@code null}
    * @param arrayArgs the arguments for message formatting, may be {@code null}
    */
   public PSContentException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
      super(code, cause, arrayArgs);
   }

   /**
    * Create a content exception with enhanced error context.
    *
    * @param msgCode the error message code
    * @param cause the underlying cause, not {@code null}
    * @param context additional context information
    * @param arrayArgs the arguments for message formatting
    * @return a new PSContentException with enhanced context
    */
   public static PSContentException withContext(int msgCode, Throwable cause,
                                               String context, Object... arrayArgs) {
      Objects.requireNonNull(cause, "cause cannot be null");

      var exception = new PSContentException(msgCode, cause, arrayArgs);
      if (context != null && !context.trim().isEmpty()) {
         exception.addSuppressed(new RuntimeException("Context: " + context));
      }
      return exception;
   }

   /**
    * Get the cause of this exception with Optional wrapper for safer access.
    *
    * @return Optional containing the cause if present, empty Optional otherwise
    */
   public Optional<Throwable> getCauseOptional() {
      return Optional.ofNullable(getCause());
   }

   /**
    * Check if this exception has a specific cause type.
    *
    * @param causeType the expected cause type, not {@code null}
    * @return true if the cause is of the specified type, false otherwise
    */
   public boolean hasCauseOfType(Class<? extends Throwable> causeType) {
      Objects.requireNonNull(causeType, "causeType cannot be null");
      return getCauseOptional()
         .map(cause -> causeType.isAssignableFrom(cause.getClass()))
         .orElse(false);
   }

   @Override
   protected String getResourceBundleBaseName() {
      return "com.percussion.services.content.PSContentErrorStringBundle";
   }
}
