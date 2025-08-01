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

package com.percussion.services.publisher;

import java.util.Objects;
import java.util.Optional;

/**
 * Runtime wrapper for publisher exceptions to enable unchecked exception handling
 * in functional programming contexts. This class provides modern Java 11 patterns
 * for exception wrapping and unwrapping with enhanced safety and validation.
 *
 * @author Percussion Software
 */
public class PSRuntimePublisherException extends RuntimeException {

   private static final long serialVersionUID = 1L;

   private final PSPublisherException publisherException;

   /**
    * Creates a new runtime publisher exception wrapping a checked publisher exception.
    *
    * @param publisherException the checked exception to wrap, never {@code null}
    * @throws IllegalArgumentException if publisherException is null
    */
   public PSRuntimePublisherException(PSPublisherException publisherException) {
      super(Objects.requireNonNull(publisherException, "Publisher exception cannot be null").getMessage(),
            publisherException);
      this.publisherException = publisherException;
   }

   /**
    * Creates a new runtime publisher exception with additional context message.
    *
    * @param message additional context message, may be {@code null}
    * @param publisherException the checked exception to wrap, never {@code null}
    * @throws IllegalArgumentException if publisherException is null
    */
   public PSRuntimePublisherException(String message, PSPublisherException publisherException) {
      super(message, Objects.requireNonNull(publisherException, "Publisher exception cannot be null"));
      this.publisherException = publisherException;
   }

   /**
    * Gets the wrapped publisher exception.
    *
    * @return the wrapped exception, never {@code null}
    */
   public PSPublisherException getPublisherException() {
      return publisherException;
   }

   /**
    * Gets the wrapped publisher exception safely with Optional wrapper.
    *
    * @return Optional containing the wrapped exception, always present for this class
    */
   public Optional<PSPublisherException> getPublisherExceptionSafely() {
      return Optional.of(publisherException);
   }

   /**
    * Gets the error code from the wrapped publisher exception.
    *
    * @return Optional containing the error code, or empty if not available
    */
   public Optional<Integer> getErrorCode() {
      return Optional.ofNullable(publisherException.getErrorCode());
   }

   /**
    * Gets error arguments from the wrapped publisher exception.
    *
    * @return Optional containing the error arguments, or empty if not available
    */
   public Optional<Object[]> getErrorArguments() {
      return Optional.ofNullable(publisherException.getErrorArguments());
   }

   /**
    * Factory method to create a runtime exception from a checked publisher exception.
    *
    * @param cause the checked exception to wrap, never {@code null}
    * @return new runtime publisher exception
    * @throws IllegalArgumentException if cause is null
    */
   public static PSRuntimePublisherException wrap(PSPublisherException cause) {
      return new PSRuntimePublisherException(cause);
   }

   /**
    * Factory method to create a runtime exception with context message.
    *
    * @param message additional context message, may be {@code null}
    * @param cause the checked exception to wrap, never {@code null}
    * @return new runtime publisher exception
    * @throws IllegalArgumentException if cause is null
    */
   public static PSRuntimePublisherException wrap(String message, PSPublisherException cause) {
      return new PSRuntimePublisherException(message, cause);
   }

   /**
    * Unwraps this runtime exception to get the original checked exception.
    *
    * @return the wrapped checked exception, never {@code null}
    */
   public PSPublisherException unwrap() {
      return publisherException;
   }

   @Override
   public String toString() {
      return String.format("%s[wrapping: %s]",
         getClass().getSimpleName(),
         publisherException.toString());
   }
}
