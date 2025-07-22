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
package com.percussion.process;

import java.util.Objects;

/**
 * Base exception for exceptions thrown from the process package.
 */
public class PSProcessException extends Exception
{
   /**
    * Constructs the exception from the specified message
    *
    * @param msg the message to wrap in the exception, may not be
    * {@code null}
    * @throws IllegalArgumentException if msg is {@code null}
    */
   public PSProcessException(String msg)
   {
      super(Objects.requireNonNull(msg, "message cannot be null"));
   }

   /**
    * Constructs the exception from the specified message and cause
    *
    * @param msg the message to wrap in the exception, may not be
    * {@code null}
    * @param cause the underlying cause of this exception, may be {@code null}
    * @throws IllegalArgumentException if msg is {@code null}
    */
   public PSProcessException(String msg, Throwable cause)
   {
      super(Objects.requireNonNull(msg, "message cannot be null"), cause);
   }

   /**
    * Constructs the exception from the specified cause
    *
    * @param cause the underlying cause of this exception, may not be {@code null}
    * @throws IllegalArgumentException if cause is {@code null}
    */
   public PSProcessException(Throwable cause)
   {
      super(Objects.requireNonNull(cause, "cause cannot be null"));
   }
}
