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

package com.percussion.validation;

/** 
 * The exception to be thrown by the constraints when a validation error occurs.
 *
 * @see ValidationFramework
 * @see ValidationConstraint
 */
public class ValidationException extends Exception {

   /**
    * Serial version UID for serialization compatibility.
    */
   private static final long serialVersionUID = 1L;

   /**
    * Constructs the exception with no message.
    */
   public ValidationException() {
      super();
   }
   
   /**
    * Constructs the exception with specified message.
    * 
    * @param message the message to set, may be {@code null}
    */
   public ValidationException(String message) {
      super(message);
   }

   /**
    * Constructs the exception with specified message and cause.
    *
    * @param message the message to set, may be {@code null}
    * @param cause the cause of this exception, may be {@code null}
    */
   public ValidationException(String message, Throwable cause) {
      super(message, cause);
   }

   /**
    * Constructs the exception with specified cause.
    *
    * @param cause the cause of this exception, may be {@code null}
    */
   public ValidationException(Throwable cause) {
      super(cause);
   }
}
