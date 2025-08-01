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
package com.percussion.services.filestorage;

import java.util.Map;
import java.util.Optional;

/**
 * The properties of a file metadata using modern Java 11 patterns.
 *
 * <p>This interface extends Map to provide key-value access to file metadata
 * while offering Optional-based safe access methods for common properties like
 * MIME type, length, filename, encoding, and parse errors.</p>
 *
 * @author yubingchen
 * @since 6.0
 */
public interface IPSFileMeta extends Map<String, String> {

   /**
    * Gets the value of the specified property with Optional wrapper for safe access.
    *
    * @param propName the name of the property, may be {@code null} or empty
    * @return Optional containing the property value, or empty if doesn't exist
    */
   Optional<String> getPropertySafely(String propName);

   /**
    * Gets the value of the specified property.
    *
    * @param propName the name of the property, may be {@code null} or empty
    * @return the value of the property, may be {@code null} if doesn't exist
    * @deprecated Use {@link #getPropertySafely(String)} for null-safe access
    */
   @Deprecated
   @Override
   String get(Object propName);

   /**
    * Returns the MIME type from the metadata with Optional wrapper.
    *
    * @return Optional containing the MIME type, or empty if not available
    */
   Optional<String> getMimeTypeSafely();

   /**
    * Returns the MIME type from the metadata.
    *
    * @return the MIME type, may be {@code null}
    * @deprecated Use {@link #getMimeTypeSafely()} for null-safe access
    */
   @Deprecated
   String getMimeType();

   /**
    * Returns the length in bytes of the binary file.
    *
    * @return the file length in bytes, or 0 if unknown
    */
   long getLength();

   /**
    * Returns the filename when the file was originally uploaded or modified
    * from the Admin console with Optional wrapper.
    *
    * @return Optional containing the original filename, or empty if not available
    */
   Optional<String> getOriginalFilenameSafely();

   /**
    * Returns the filename when the file was originally uploaded or modified.
    *
    * @return the original filename, may be {@code null}
    * @deprecated Use {@link #getOriginalFilenameSafely()} for null-safe access
    */
   @Deprecated
   String getOriginalFilename();

   /**
    * Returns the encoding of the file if appropriate (e.g., textual documents)
    * with Optional wrapper.
    *
    * @return Optional containing the file encoding, or empty if not available
    */
   Optional<String> getEncodingSafely();

   /**
    * Returns the encoding of the file if appropriate (e.g., textual documents).
    *
    * @return the file encoding, {@code null} if not applicable
    * @deprecated Use {@link #getEncodingSafely()} for null-safe access
    */
   @Deprecated
   String getEncoding();

   /**
    * Returns any parse error that occurred during metadata extraction with Optional wrapper.
    *
    * @return Optional containing the parse error message, or empty if no error occurred
    */
   Optional<String> getParseErrorSafely();

   /**
    * Returns any parse error that occurred during metadata extraction.
    *
    * @return the parse error message, {@code null} if no error occurred
    * @deprecated Use {@link #getParseErrorSafely()} for null-safe access
    */
   @Deprecated
   String getParseError();

   /**
    * Checks if the metadata contains a parse error.
    *
    * @return {@code true} if there was a parse error, {@code false} otherwise
    */
   default boolean hasParseError() {
      return getParseErrorSafely().isPresent();
   }

   /**
    * Checks if the file has a known MIME type.
    *
    * @return {@code true} if MIME type is available, {@code false} otherwise
    */
   default boolean hasMimeType() {
      return getMimeTypeSafely().isPresent();
   }

   /**
    * Checks if the file has an original filename.
    *
    * @return {@code true} if original filename is available, {@code false} otherwise
    */
   default boolean hasOriginalFilename() {
      return getOriginalFilenameSafely().isPresent();
   }

   /**
    * Checks if the file has encoding information.
    *
    * @return {@code true} if encoding is available, {@code false} otherwise
    */
   default boolean hasEncoding() {
      return getEncodingSafely().isPresent();
   }
}
