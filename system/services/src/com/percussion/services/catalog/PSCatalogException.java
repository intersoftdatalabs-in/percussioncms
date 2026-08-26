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
package com.percussion.services.catalog;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Exception thrown during cataloging operations with enhanced Java 11 support.
 *
 * <p>This exception provides specific error handling for catalog service operations
 * including object enumeration, XML serialization/deserialization, GUID management,
 * and type-specific processing.
 *
 * <p>Common catalog error scenarios:
 * <ul>
 *   <li>Object not found during lookup operations</li>
 *   <li>Type mismatch or unsupported type errors</li>
 *   <li>XML serialization/deserialization failures</li>
 *   <li>GUID validation and assignment errors</li>
 *   <li>Service configuration and availability issues</li>
 * </ul>
 *
 * <p>Key features:
 * <ul>
 *   <li>Enhanced null safety with Objects.requireNonNull()</li>
 *   <li>Optional-based safe message access</li>
 *   <li>Factory methods for specific error types</li>
 *   <li>Improved cause chain handling</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public class PSCatalogException extends PSBaseException {

   /**
    * Serial version UID for serialization compatibility.
    */
   private static final long serialVersionUID = 1L;

   /**
    * Constructs a new catalog exception with the specified message code and arguments.
    *
    * @param msgCode the error message code
    * @param arrayArgs optional arguments for the error message
    */
   public PSCatalogException(int msgCode, Object... arrayArgs) {
      super(msgCode, arrayArgs);
   }

   /**
    * Constructs a new catalog exception with the specified message code, cause, and arguments.
    *
    * @param msgCode the error message code
    * @param cause the underlying cause of this exception
    * @param arrayArgs optional arguments for the error message
    */
   public PSCatalogException(int msgCode, Throwable cause, Object... arrayArgs) {
      super(msgCode, cause, arrayArgs);
   }

   /**
    * Constructs a new catalog exception with the specified message code.
    *
    * @param msgCode the error message code
    */
   public PSCatalogException(int msgCode) {
      super(msgCode);
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode}.
    *
    * @param code catalogued error code, never {@code null}
    * @param arrayArgs optional arguments for the error message
    */
   public PSCatalogException(IPSErrorCode code, Object... arrayArgs) {
      super(code, arrayArgs);
   }

   /**
    * Typed construction with a cause.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause the underlying cause of this exception
    * @param arrayArgs optional arguments for the error message
    */
   public PSCatalogException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
      super(code, cause, arrayArgs);
   }

   /**
    * Create a catalog exception with enhanced error context.
    *
    * @param msgCode the error message code
    * @param cause the underlying cause, not {@code null}
    * @param context additional context information
    * @param arrayArgs the arguments for message formatting
    * @return a new PSCatalogException with enhanced context
    */
   public static PSCatalogException withContext(int msgCode, Throwable cause,
                                               String context, Object... arrayArgs) {
      Objects.requireNonNull(cause, "cause cannot be null");

      var exception = new PSCatalogException(msgCode, cause, arrayArgs);
      if (context != null && !context.trim().isEmpty()) {
         exception.addSuppressed(new RuntimeException("Catalog Context: " + context));
      }
      return exception;
   }

   /**
    * Create a catalog exception for type-related errors.
    *
    * @param msgCode the error message code
    * @param type the PSTypeEnum that caused the error
    * @param arrayArgs additional arguments for message formatting
    * @return a new PSCatalogException with type context
    */
   public static PSCatalogException forType(int msgCode, PSTypeEnum type, Object... arrayArgs) {
      var exception = new PSCatalogException(msgCode, arrayArgs);
      if (type != null) {
         exception.addSuppressed(new RuntimeException("Type: " + type));
      }
      return exception;
   }

   /**
    * Create a catalog exception for GUID-related errors.
    *
    * @param msgCode the error message code
    * @param guid the GUID that caused the error
    * @param arrayArgs additional arguments for message formatting
    * @return a new PSCatalogException with GUID context
    */
   public static PSCatalogException forGuid(int msgCode, IPSGuid guid, Object... arrayArgs) {
      var exception = new PSCatalogException(msgCode, arrayArgs);
      if (guid != null) {
         exception.addSuppressed(new RuntimeException("GUID: " + guid));
      }
      return exception;
   }

   /**
    * Create a catalog exception for XML processing errors.
    *
    * @param msgCode the error message code
    * @param xmlOperation the XML operation that failed (e.g., "serialization", "deserialization")
    * @param cause the underlying XML processing exception
    * @param arrayArgs additional arguments for message formatting
    * @return a new PSCatalogException with XML context
    */
   public static PSCatalogException forXmlProcessing(int msgCode, String xmlOperation,
                                                    Throwable cause, Object... arrayArgs) {
      var exception = new PSCatalogException(msgCode, cause, arrayArgs);
      if (xmlOperation != null && !xmlOperation.trim().isEmpty()) {
         exception.addSuppressed(new RuntimeException("XML Operation: " + xmlOperation));
      }
      return exception;
   }

   /**
    * Create a catalog exception for object not found errors.
    *
    * @param msgCode the error message code
    * @param objectType the type of object that was not found
    * @param identifier the identifier that was used for lookup
    * @param arrayArgs additional arguments for message formatting
    * @return a new PSCatalogException with object context
    */
   public static PSCatalogException forObjectNotFound(int msgCode, PSTypeEnum objectType,
                                                     Object identifier, Object... arrayArgs) {
      var exception = new PSCatalogException(msgCode, arrayArgs);
      if (objectType != null) {
         exception.addSuppressed(new RuntimeException("Object Type: " + objectType));
      }
      if (identifier != null) {
         exception.addSuppressed(new RuntimeException("Identifier: " + identifier));
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

   /**
    * Check if this is an XML-related catalog exception.
    *
    * @return true if the exception is related to XML processing
    */
   public boolean isXmlRelated() {
      return hasCauseOfType(org.xml.sax.SAXException.class) ||
             hasCauseOfType(java.io.IOException.class) ||
             getSuppressed().length > 0 &&
             java.util.Arrays.stream(getSuppressed())
                .anyMatch(t -> t.getMessage() != null &&
                          t.getMessage().contains("XML Operation"));
   }

   /**
    * Check if this is a type-related catalog exception.
    *
    * @return true if the exception is related to type handling
    */
   public boolean isTypeRelated() {
      return getSuppressed().length > 0 &&
             java.util.Arrays.stream(getSuppressed())
                .anyMatch(t -> t.getMessage() != null &&
                          t.getMessage().startsWith("Type:"));
   }

   @Override
   protected String getResourceBundleBaseName() {
      return "com.percussion.services.catalog.PSCatalogErrorStringBundle";
   }
}
