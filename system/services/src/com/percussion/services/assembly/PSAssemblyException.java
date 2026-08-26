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
package com.percussion.services.assembly;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;

import java.util.Objects;
import java.util.Optional;

/**
 * Exception thrown during assembly operations with enhanced Java 11 support.
 *
 * <p>This exception provides comprehensive error handling for assembly operations
 * including template processing, content assembly, slot processing, and variable binding.
 *
 * <p>Common assembly error scenarios:
 * <ul>
 *   <li>Template not found or invalid</li>
 *   <li>Content item loading failures</li>
 *   <li>Slot content finder errors</li>
 *   <li>Variable binding issues</li>
 *   <li>Assembly plugin execution failures</li>
 * </ul>
 *
 * <p>Key features:
 * <ul>
 *   <li>Enhanced null safety with Objects.requireNonNull()</li>
 *   <li>Optional-based safe message access</li>
 *   <li>Improved cause chain handling</li>
 *   <li>Modern exception construction patterns</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public class PSAssemblyException extends PSBaseException {

   /**
    * Legacy error code used when an unexpected exception occurs during
    * assembly.  Restored for backward compatibility; new callers should
    * prefer more specific codes or simply throw with a message.
    *
    * @deprecated retained only for compatibility with existing code.
    */
   @Deprecated
   public static final int UNEXPECTED_ASSEMBLY_ERROR = 5;

   /**
    * Serial version UID for serialization compatibility.
    */
   private static final long serialVersionUID = 3256726182123680309L;

   /**
    * Construct an assembly exception with the specified error code and arguments.
    *
    * @param msgCode message code used to lookup message
    * @param arrayArgs arguments for message formatting, may be {@code null}
    */
   public PSAssemblyException(int msgCode, Object... arrayArgs) {
      super(msgCode, arrayArgs);
   }

   /**
    * Construct an assembly exception with the specified error code, cause, and arguments.
    *
    * @param msgCode message code used to lookup message
    * @param cause original exception, may be {@code null}
    * @param arrayArgs arguments for message formatting, may be {@code null}
    */
   public PSAssemblyException(int msgCode, Throwable cause, Object... arrayArgs) {
      super(msgCode, cause, arrayArgs);
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode}.
    *
    * @param code catalogued error code, never {@code null}
    * @param arrayArgs arguments for message formatting, may be {@code null}
    */
   public PSAssemblyException(IPSErrorCode code, Object... arrayArgs) {
      super(code, arrayArgs);
   }

   /**
    * Typed construction with a cause.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause original exception, may be {@code null}
    * @param arrayArgs arguments for message formatting, may be {@code null}
    */
   public PSAssemblyException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
      super(code, cause, arrayArgs);
   }

   /**
    * Create an assembly exception with enhanced error context.
    *
    * @param msgCode the error message code
    * @param cause the underlying cause, not {@code null}
    * @param context additional context information
    * @param arrayArgs the arguments for message formatting
    * @return a new PSAssemblyException with enhanced context
    */
   public static PSAssemblyException withContext(int msgCode, Throwable cause,
                                                String context, Object... arrayArgs) {
      Objects.requireNonNull(cause, "cause cannot be null");

      var exception = new PSAssemblyException(msgCode, cause, arrayArgs);
      if (context != null && !context.trim().isEmpty()) {
         exception.addSuppressed(new RuntimeException("Assembly Context: " + context));
      }
      return exception;
   }

   /**
    * Create an assembly exception for template-related errors.
    *
    * @param msgCode the error message code
    * @param templateName the name of the template that caused the error
    * @param arrayArgs additional arguments for message formatting
    * @return a new PSAssemblyException with template context
    */
   public static PSAssemblyException forTemplate(int msgCode, String templateName, Object... arrayArgs) {
      var exception = new PSAssemblyException(msgCode, arrayArgs);
      if (templateName != null && !templateName.trim().isEmpty()) {
         exception.addSuppressed(new RuntimeException("Template: " + templateName));
      }
      return exception;
   }

   /**
    * Create an assembly exception for content item-related errors.
    *
    * @param msgCode the error message code
    * @param itemPath the path of the content item that caused the error
    * @param arrayArgs additional arguments for message formatting
    * @return a new PSAssemblyException with item context
    */
   public static PSAssemblyException forContentItem(int msgCode, String itemPath, Object... arrayArgs) {
      var exception = new PSAssemblyException(msgCode, arrayArgs);
      if (itemPath != null && !itemPath.trim().isEmpty()) {
         exception.addSuppressed(new RuntimeException("Content Item: " + itemPath));
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
      return "com.percussion.services.assembly.PSAssemblyErrorStringBundle";
   }
}
