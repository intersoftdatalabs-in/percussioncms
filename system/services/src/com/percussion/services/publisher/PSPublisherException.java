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
package com.percussion.services.publisher;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;

import java.util.Objects;

/**
 * Exception thrown by the publisher service with Java 11 modernization.
 * Provides enhanced error handling for publishing operations with improved
 * validation and modern exception patterns.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Improved constructor documentation with clear contracts</li>
 * <li>Modern exception chaining patterns</li>
 * </ul>
 *
 * @author dougrand
 */
public class PSPublisherException extends PSBaseException {

   private static final long serialVersionUID = 1L;

   /**
    * Constructs a publisher exception with message code and arguments.
    *
    * @param msgCode message code, used to lookup the correct text message that
    *                is listed in the corresponding properties
    * @param arrayArgs the arguments to the message code, may be empty but not {@code null}
    * @throws IllegalArgumentException if arrayArgs is null
    */
   public PSPublisherException(int msgCode, Object... arrayArgs) {
      super(msgCode, Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Constructs a publisher exception with message code, cause, and arguments.
    *
    * @param msgCode message code, used to lookup the correct text message that
    *                is listed in the corresponding properties
    * @param cause the original exception cause, not {@code null}
    * @param arrayArgs the arguments to the message code, may be empty but not {@code null}
    * @throws IllegalArgumentException if cause or arrayArgs is null
    */
   public PSPublisherException(int msgCode, Throwable cause, Object... arrayArgs) {
      super(msgCode,
            Objects.requireNonNull(cause, "cause cannot be null"),
            Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Constructs a publisher exception with message code only.
    *
    * @param msgCode message code, used to lookup the correct text message that
    *                is listed in the corresponding properties
    */
   public PSPublisherException(int msgCode) {
      super(msgCode);
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode}.
    *
    * @param code catalogued error code, never {@code null}
    * @param arrayArgs the arguments to the message code, may be empty but not {@code null}
    */
   public PSPublisherException(IPSErrorCode code, Object... arrayArgs) {
      super(code, Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Typed construction with a cause.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause the original exception cause, not {@code null}
    * @param arrayArgs the arguments to the message code, may be empty but not {@code null}
    */
   public PSPublisherException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
      super(code,
            Objects.requireNonNull(cause, "cause cannot be null"),
            Objects.requireNonNull(arrayArgs, "arrayArgs cannot be null"));
   }

   /**
    * Creates a new publisher exception with a descriptive message and cause.
    *
    * @param message the detail message, not {@code null}
    * @param cause the cause of this exception, not {@code null}
    * @return a new PSPublisherException instance
    * @throws IllegalArgumentException if message or cause is null
    */
   public static PSPublisherException withMessageAndCause(String message, Throwable cause) {
      Objects.requireNonNull(message, "message cannot be null");
      Objects.requireNonNull(cause, "cause cannot be null");
      return new PSPublisherException(0, cause, message);
   }

   /**
    * Creates a new publisher exception with a descriptive message.
    *
    * @param message the detail message, not {@code null}
    * @return a new PSPublisherException instance
    * @throws IllegalArgumentException if message is null
    */
   public static PSPublisherException withMessage(String message) {
      Objects.requireNonNull(message, "message cannot be null");
      return new PSPublisherException(0, message);
   }

   @Override
   protected String getResourceBundleBaseName() {
      return "com.percussion.services.publisher.PSPublisherErrorStringBundle";
   }
}
